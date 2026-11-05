"""Etapa 1: comprobar que el entorno Python tiene todo lo necesario.

Uso:  python etapa1_verificar.py
"""
import importlib
import sys

MINIMOS = {"numpy": "1.26", "scipy": "1.11", "matplotlib": "3.8",
           "wfdb": "4.1", "sklearn": "1.4", "joblib": "1.3"}


def version_tuple(v):
    return tuple(int(p) for p in v.split(".")[:2] if p.isdigit())


def main():
    ok = True
    if sys.version_info < (3, 10):
        print(f"[FALLA] Python {sys.version.split()[0]}: se requiere >= 3.10")
        ok = False
    else:
        print(f"[ok] Python {sys.version.split()[0]}")

    for modulo, minimo in MINIMOS.items():
        try:
            m = importlib.import_module(modulo)
            v = getattr(m, "__version__", "?")
            bien = version_tuple(v) >= version_tuple(minimo)
            print(f"[{'ok' if bien else 'FALLA'}] {modulo} {v} (mínimo {minimo})")
            ok &= bien
        except ImportError:
            print(f"[FALLA] {modulo} no está instalado  ->  pip install -r requirements.txt")
            ok = False

    if ok:
        # Prueba mínima de matplotlib sin ventana (backend Agg), para que los scripts funcionen en servidores.
        import matplotlib
        matplotlib.use("Agg")
        import matplotlib.pyplot as plt
        plt.figure().add_subplot().plot([0, 1], [0, 1])
        plt.close("all")
        print("[ok] matplotlib puede dibujar")
    print("\nEntorno listo." if ok else "\nCorrige los puntos marcados y vuelve a ejecutar.")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()