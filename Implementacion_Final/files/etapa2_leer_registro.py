"""Etapa 2: leer registros MIT-BIH con WFDB y comprobar que se interpretan bien.

Uso:  python etapa2_leer_registro.py 100 119 207
Descarga los registros si no están en data/mitdb.
"""
import argparse
import sys
from collections import Counter

import numpy as np
import wfdb

from datos import AAMI, DIR_DATOS, FS_MITBIH, SIMBOLOS_Q, cargar_registro, descargar, localizar


def checksum16(d):
    """Suma de las muestras módulo 2^16 (campo 'checksum' de la cabecera)."""
    return int(d.sum()) % 65536


def verificar_integridad(nombre, directorio):
    """Compara las muestras DIGITALES con el valor inicial y el checksum que declara la cabecera."""
    rec = wfdb.rdrecord(str(localizar(nombre, directorio)), physical=False)
    ok = True
    for c, nom in enumerate(rec.sig_name):
        d = rec.d_signal[:, c]
        if int(d[0]) != rec.init_value[c]:
            print(f"   [FALLA] {nom}: primera muestra {d[0]} != init_value {rec.init_value[c]}")
            ok = False
        # Las cabeceras lo guardan con signo (-22131) o sin signo (43405): son el mismo valor módulo 2^16.
        if checksum16(d) != int(rec.checksum[c]) % 65536:
            print(f"   [FALLA] {nom}: checksum {checksum16(d)} != cabecera {rec.checksum[c]} (mod 2^16)")
            ok = False
    return ok


def resumen(nombre, directorio):
    reg = cargar_registro(nombre, directorio)
    print(f"\n=== Registro {nombre} ===")
    print(f" fs = {reg.fs:g} Hz | muestras = {reg.n_muestras} | duración = {reg.duracion_s/60:.2f} min")
    for i, (c, u) in enumerate(zip(reg.canales, reg.unidades)):
        x = reg.senal[:, i]
        print(f" canal {i}: {c:5s} [{u}]  min={x.min():+.3f}  max={x.max():+.3f}  media={x.mean():+.3f}  std={x.std():.3f}")

    ok = True
    if reg.fs != FS_MITBIH:
        print(f" [AVISO] fs distinta de {FS_MITBIH} Hz")
    if not np.all(np.isfinite(reg.senal)):
        print(" [FALLA] hay NaN/inf en la señal")
        ok = False
    ok &= verificar_integridad(nombre, directorio)
    print(" integridad (valor inicial + checksum):", "ok" if ok else "FALLA")

    # Anotaciones: cuántas hay de cada símbolo y cómo caen en las clases AAMI.
    cuenta = Counter(reg.ann_simbolo.tolist())
    print(" anotaciones por símbolo:", dict(sorted(cuenta.items(), key=lambda kv: -kv[1])))
    por_clase = Counter(AAMI.get(s) for s in reg.ann_simbolo if s in AAMI)
    n_q = sum(v for s, v in cuenta.items() if s in SIMBOLOS_Q)
    print(" clases AAMI:", dict(por_clase), "| latidos excluidos (/ f Q):", n_q)
    print(f" latidos totales: {int(reg.es_latido().sum())}")

    # Las anotaciones deben estar ordenadas y dentro de la señal.
    if np.any(np.diff(reg.ann_muestra) < 0) or reg.ann_muestra.max() >= reg.n_muestras:
        print(" [FALLA] anotaciones desordenadas o fuera de rango")
        ok = False
    return ok


def main():
    p = argparse.ArgumentParser()
    p.add_argument("registros", nargs="*", default=["100"])
    p.add_argument("--dir", default=str(DIR_DATOS))
    a = p.parse_args()
    descargar(a.registros, a.dir)
    todo_ok = all([resumen(r, a.dir) for r in a.registros])
    sys.exit(0 if todo_ok else 1)


if __name__ == "__main__":
    main()