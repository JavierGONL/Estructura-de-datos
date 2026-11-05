"""Acceso a MIT-BIH (WFDB) y constantes compartidas por todas las etapas.

Todo lo que dependa de "cómo se lee un registro" vive aquí, para que entrenamiento
e inferencia usen exactamente el mismo código.
"""
from dataclasses import dataclass
from pathlib import Path

import os

import numpy as np
import wfdb

# Carpeta de MIT-BIH. Por defecto: <raíz del proyecto>/data/MIT-BIH Arrhythmia Database, con la raíz
# calculada desde la ubicación de este archivo (files/), así no depende de desde dónde se ejecute.
# Se puede cambiar con la variable de entorno ECG_DATOS_MITBIH o con --dir en los scripts.
RAIZ_PROYECTO = Path(__file__).resolve().parent.parent
DIR_DATOS = Path(os.environ.get("ECG_DATOS_MITBIH",
                                RAIZ_PROYECTO / "data" / "MIT-BIH Arrhythmia Database"))
FS_MITBIH = 360  # Hz

# Agrupación AAMI restringida a las 4 clases del proyecto (símbolo WFDB -> clase).
#   N: normal, bloqueo de rama izq./der., escape auricular, escape nodal
#   S: prematuro auricular, auricular aberrado, nodal prematuro, supraventricular prematuro
#   V: contracción ventricular prematura, escape ventricular
#   F: fusión ventricular + normal
AAMI = {**{s: "N" for s in "NLRej"},
        **{s: "S" for s in "AaJS"},
        **{s: "V" for s in "VE"},
        "F": "F"}
# Latidos que existen pero se excluyen de las 4 clases (AAMI "Q": marcapasos, fusión paced, no clasificable).
SIMBOLOS_Q = set("/fQ")
SIMBOLOS_LATIDO = set(AAMI) | SIMBOLOS_Q

# Registros con marcapasos: el estándar AAMI/de Chazal los excluye de la evaluación.
REGISTROS_MARCAPASOS = ("102", "104", "107", "217")


@dataclass
class Registro:
    nombre: str
    fs: float
    canales: list          # nombres de derivación, p. ej. ["MLII", "V1"]
    unidades: list
    senal: np.ndarray      # (n_muestras, n_canales) en unidades físicas (mV)
    ann_muestra: np.ndarray  # índice de muestra de cada anotación
    ann_simbolo: np.ndarray  # símbolo WFDB de cada anotación (array de str)

    @property
    def n_muestras(self):
        return self.senal.shape[0]

    @property
    def duracion_s(self):
        return self.n_muestras / self.fs

    def indice_canal(self, nombre):
        """Índice del canal por nombre. NO asumir que MLII es el canal 0 (en 102 y 104 no existe)."""
        return self.canales.index(nombre)

    def es_latido(self):
        """Máscara booleana: anotaciones que son latidos (excluye ritmo '+', ruido '~', '|', etc.)."""
        return np.isin(self.ann_simbolo, list(SIMBOLOS_LATIDO))


def localizar(nombre, directorio=DIR_DATOS):
    """Ruta base (sin extensión) del registro: busca <nombre>.hea en el directorio y sus subcarpetas."""
    directorio = Path(directorio)
    if (directorio / f"{nombre}.hea").exists():
        return directorio / nombre
    for hea in directorio.rglob(f"{nombre}.hea"):
        return hea.with_suffix("")
    return None


def descargar(nombres, destino=DIR_DATOS):
    """Descarga .hea/.dat/.atr de MIT-BIH solo para los registros que no están ya en disco."""
    destino = Path(destino)
    for n in nombres:
        base = localizar(n, destino)
        carpeta = base.parent if base else destino
        archivos = [f"{n}.{ext}" for ext in ("hea", "dat", "atr")]
        faltan = [a for a in archivos if not (carpeta / a).exists()]
        if faltan:
            carpeta.mkdir(parents=True, exist_ok=True)
            wfdb.dl_files("mitdb", str(carpeta), faltan)


def cargar_registro(nombre, directorio=DIR_DATOS):
    """Lee señal (en mV) y anotaciones de referencia (.atr) de un registro."""
    ruta = localizar(nombre, directorio)
    if ruta is None:
        raise FileNotFoundError(f"No se encontró {nombre}.hea en {directorio}")
    base = str(ruta)
    rec = wfdb.rdrecord(base)               # p_signal = (digital - baseline) / ganancia
    ann = wfdb.rdann(base, "atr")
    return Registro(
        nombre=nombre,
        fs=float(rec.fs),
        canales=list(rec.sig_name),
        unidades=list(rec.units),
        senal=np.asarray(rec.p_signal, dtype=np.float64),
        ann_muestra=np.asarray(ann.sample, dtype=np.int64),
        ann_simbolo=np.asarray(ann.symbol),
    )