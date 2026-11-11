package ecg.visualizacion;

import ecg.modelo.AnotacionECG;
import ecg.modelo.ConfiguracionVista;
import ecg.modelo.RegistroECG;

/** Visibilidad y selección de anotaciones. */
public class GestorAnotaciones {

    private final RegistroECG registro;
    private final ConfiguracionVista c;

    public GestorAnotaciones(RegistroECG registro, ConfiguracionVista c) { this.registro = registro; this.c = c; }

    public void mostrarAnotaciones(boolean visible) { c.setMostrarAnotaciones(visible); }

    public void mostrarDetecciones(boolean visible) { c.setMostrarDetecciones(visible); }

    public void mostrarClasificacion(boolean visible) { c.setMostrarClasificacion(visible); }

    public void seleccionarAnotacion(AnotacionECG anotacion) { c.setAnotacionSeleccionada(anotacion); }

    /** Selecciona la anotación visible más cercana a {@code tiempo} dentro de la tolerancia (null si no hay). */
    public AnotacionECG seleccionarMasCercana(double tiempo, double toleranciaSegundos) {
        double fs = registro.getFrecuenciaMuestreo();
        AnotacionECG mejor = null;
        double mejorDist = toleranciaSegundos;
        for (AnotacionECG a : registro.getAnotaciones()) {
            boolean algoritmo = AnotacionECG.FUENTE_ALGORITMO.equals(a.getFuente());
            boolean clasificacion = AnotacionECG.FUENTE_CLASIFICACION.equals(a.getFuente());
            boolean visible = clasificacion ? c.isMostrarClasificacion()
                    : algoritmo ? c.isMostrarDetecciones() : c.isMostrarAnotaciones();
            if (!visible) continue;
            double d = Math.abs(a.getTiempo(fs) - tiempo);
            if (d <= mejorDist) { mejorDist = d; mejor = a; }
        }
        c.setAnotacionSeleccionada(mejor);
        return mejor;
    }
}
