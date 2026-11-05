package ecg.visualizacion;

import ecg.modelo.ConfiguracionVista;
import ecg.modelo.RegistroECG;

/**
 * Escalas horizontal y vertical. El zoom horizontal cambia el intervalo temporal visible; el vertical
 * cambia mm/mV. Ninguno modifica los valores originales de la señal.
 */
public class GestorZoom {

    public static final double VENTANA_INICIAL_S = 10.0;
    public static final double ESCALA_VERTICAL_MIN = 0.5, ESCALA_VERTICAL_MAX = 400;

    private final RegistroECG registro;
    private final ConfiguracionVista c;

    public GestorZoom(RegistroECG registro, ConfiguracionVista c) { this.registro = registro; this.c = c; }

    /** factor &gt; 1 acerca (ventana menor), factor &lt; 1 aleja; el centro es el de la ventana actual. */
    public void zoomHorizontal(double factor) {
        zoomHorizontal(factor, (c.getTiempoInicio() + c.getTiempoFin()) / 2);
    }

    /** Zoom manteniendo fijo el instante {@code tCentro} en su posición relativa de la ventana. */
    public void zoomHorizontal(double factor, double tCentro) {
        double dur = registro.getDuracionSegundos();
        double ventana = c.getDuracionVentana();
        double minima = Math.min(dur, Math.max(0.2, 10.0 / registro.getFrecuenciaMuestreo()));
        double nueva = Math.max(minima, Math.min(dur, ventana / factor));
        double rel = ventana > 0 ? (tCentro - c.getTiempoInicio()) / ventana : 0.5;
        rel = Math.max(0, Math.min(1, rel));
        double inicio = tCentro - rel * nueva;
        fijarVentana(inicio, nueva);
    }

    public void zoomVertical(double factor) {
        c.setEscalaVertical(Math.max(ESCALA_VERTICAL_MIN, Math.min(ESCALA_VERTICAL_MAX, c.getEscalaVertical() * factor)));
    }

    public void restablecerZoom() {
        double dur = registro.getDuracionSegundos();
        c.setTiempoInicio(0);
        c.setTiempoFin(Math.max(0.1, Math.min(VENTANA_INICIAL_S, dur)));
        c.setEscalaVertical(ConfiguracionVista.GANANCIA_ESTANDAR_MM_MV);
    }

    /** Fija una ventana respetando los límites del registro. */
    void fijarVentana(double inicio, double duracion) {
        double dur = registro.getDuracionSegundos();
        duracion = Math.min(duracion, dur);
        inicio = Math.max(0, Math.min(inicio, dur - duracion));
        c.setTiempoInicio(inicio);
        c.setTiempoFin(inicio + duracion);
    }
}
