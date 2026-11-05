"""Etapa 3: dibujar el ECG con sus anotaciones para comprobarlas a ojo.

Uso:
  python etapa3_visualizar.py 100                       # primeros 10 s -> figuras/100_t0_10s.png
  python etapa3_visualizar.py 119 --inicio 60 --duracion 6
  python etapa3_visualizar.py 100 --mostrar             # ventana interactiva
"""
import argparse
from pathlib import Path

import numpy as np

from datos import AAMI, DIR_DATOS, SIMBOLOS_Q, cargar_registro, descargar

COLORES = {"N": "tab:green", "S": "tab:orange", "V": "tab:red", "F": "tab:purple", "Q": "tab:gray"}


def clase_de(simbolo):
    if simbolo in AAMI:
        return AAMI[simbolo]
    return "Q" if simbolo in SIMBOLOS_Q else None   # None = no es latido (ritmo, ruido, ...)


def desplazamiento_al_maximo(reg, canal, radio_ms=60):
    """Diferencia (ms) entre cada anotación de latido y el máximo de |señal| en +-radio_ms.

    Sirve para saber cuánto "se aparta" la marca de referencia del pico real de la R. Es la misma
    incertidumbre que tendrá la posición que entregue Pan-Tompkins en la inferencia.
    """
    x = reg.senal[:, canal] - np.median(reg.senal[:, canal])
    r = int(round(radio_ms / 1000 * reg.fs))
    dif = []
    for m in reg.ann_muestra[reg.es_latido()]:
        a, b = max(0, m - r), min(len(x), m + r + 1)
        if b - a > 1:
            dif.append((a + int(np.argmax(np.abs(x[a:b]))) - m) / reg.fs * 1000)
    return np.array(dif)


def dibujar(reg, inicio_s, duracion_s):
    import matplotlib.pyplot as plt

    i0 = int(inicio_s * reg.fs)
    i1 = min(reg.n_muestras, int((inicio_s + duracion_s) * reg.fs))
    t = np.arange(i0, i1) / reg.fs
    n = len(reg.canales)
    fig, ejes = plt.subplots(n, 1, sharex=True, figsize=(14, 3.2 * n), squeeze=False)
    ejes = ejes[:, 0]

    en_ventana = (reg.ann_muestra >= i0) & (reg.ann_muestra < i1)
    for c, ax in enumerate(ejes):
        y = reg.senal[i0:i1, c]
        ax.plot(t, y, lw=0.8, color="k")
        ax.set_ylabel(f"{reg.canales[c]} [{reg.unidades[c]}]")
        ax.grid(alpha=0.3)
        for m, s in zip(reg.ann_muestra[en_ventana], reg.ann_simbolo[en_ventana]):
            cl = clase_de(s)
            tm = m / reg.fs
            if cl is None:   # no es latido: línea discontinua con su símbolo
                ax.axvline(tm, ls="--", lw=0.8, color="tab:blue", alpha=0.7)
                if c == 0:
                    ax.text(tm, ax.get_ylim()[1], s, color="tab:blue", va="top", fontsize=9)
            else:
                ax.plot(tm, reg.senal[m, c], "o", ms=5, color=COLORES[cl], mfc="none", mew=1.6)
                if c == 0:
                    ax.annotate(s, (tm, reg.senal[m, c]), textcoords="offset points", xytext=(0, 8),
                                ha="center", fontsize=9, color=COLORES[cl], fontweight="bold")
    ejes[-1].set_xlabel("tiempo [s]")
    ejes[0].set_title(f"Registro {reg.nombre} | fs = {reg.fs:g} Hz | "
                      f"{inicio_s:g}-{inicio_s + duracion_s:g} s | "
                      "círculos: N verde, S naranja, V rojo, F morado, Q gris; azul discontinuo: no es latido")
    fig.tight_layout()
    return fig


def main():
    p = argparse.ArgumentParser()
    p.add_argument("registro")
    p.add_argument("--inicio", type=float, default=0.0, help="segundo inicial")
    p.add_argument("--duracion", type=float, default=10.0, help="segundos a dibujar")
    p.add_argument("--dir", default=str(DIR_DATOS))
    p.add_argument("--salida", default=None)
    p.add_argument("--mostrar", action="store_true")
    a = p.parse_args()

    import matplotlib
    if not a.mostrar:
        matplotlib.use("Agg")
    import matplotlib.pyplot as plt

    descargar([a.registro], a.dir)
    reg = cargar_registro(a.registro, a.dir)

    for c, nom in enumerate(reg.canales):
        d = desplazamiento_al_maximo(reg, c)
        print(f"canal {c} ({nom}): marca -> máximo |x|: mediana {np.median(d):+.1f} ms, "
              f"p5..p95 = {np.percentile(d, 5):+.0f}..{np.percentile(d, 95):+.0f} ms")

    fig = dibujar(reg, a.inicio, a.duracion)
    salida = Path(a.salida or f"figuras/{a.registro}_t{a.inicio:g}_{a.duracion:g}s.png")
    salida.parent.mkdir(parents=True, exist_ok=True)
    fig.savefig(salida, dpi=130)
    print("figura guardada en", salida)
    if a.mostrar:
        plt.show()


if __name__ == "__main__":
    main()