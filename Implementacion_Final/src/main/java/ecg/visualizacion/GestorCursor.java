package ecg.visualizacion;

import ecg.modelo.CanalECG;
import ecg.modelo.ConfiguracionVista;
import ecg.modelo.RegistroECG;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

/** Convierte la posición del ratón en tiempo, muestra, derivación y amplitud. */
public class GestorCursor {

    /** Lectura del cursor en un instante. */
    public static class InfoCursor {
        public final double tiempoSegundos;
        public final long indiceMuestra;
        public final int indiceCanal;
        public final String nombreCanal;
        public final double amplitudMv;
        public final String unidades;

        InfoCursor(double t, long i, int c, String n, double a, String u) {
            tiempoSegundos = t; indiceMuestra = i; indiceCanal = c; nombreCanal = n; amplitudMv = a; unidades = u;
        }

        @Override public String toString() {
            return String.format("Derivación %s   t = %.3f s   muestra %d   amplitud = %.3f %s",
                    nombreCanal, tiempoSegundos, indiceMuestra, amplitudMv, unidades);
        }
    }

    private final RegistroECG registro;
    private final ConfiguracionVista c;
    private final RenderizadorECG renderizador;

    public GestorCursor(RegistroECG registro, ConfiguracionVista c, RenderizadorECG renderizador) {
        this.registro = registro; this.c = c; this.renderizador = renderizador;
    }

    /** Actualiza el cursor de la configuración; devuelve null si el ratón no está sobre una derivación. */
    public InfoCursor actualizar(Point p, Dimension dim) {
        List<Integer> canales = renderizador.canalesEfectivos(registro, c);
        List<Rectangle> celdas = renderizador.calcularCeldas(registro, c, dim);
        for (int k = 0; k < celdas.size(); k++) {
            Rectangle r = celdas.get(k);
            if (!r.contains(p)) continue;
            int idx = canales.get(k);
            CanalECG canal = registro.getCanal(idx);
            double t = RenderizadorECG.xATiempo(c, r, p.x);
            long muestra = Math.max(0, Math.min(canal.getNumeroMuestras() - 1,
                    Math.round(t * canal.getFrecuenciaMuestreo())));
            c.setCursorTiempo(muestra / canal.getFrecuenciaMuestreo());
            c.setCursorCanal(idx);
            return new InfoCursor(c.getCursorTiempo(), muestra, idx, canal.getNombre(),
                    canal.getAmplitudEnMilivoltios((int) muestra), canal.getUnidades());
        }
        return null;
    }

    public void ocultar() { c.setCursorTiempo(Double.NaN); c.setCursorCanal(-1); }
}
