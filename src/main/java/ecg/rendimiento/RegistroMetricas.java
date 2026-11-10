package ecg.rendimiento;

import ecg.modelo.MetricasEjecucion;
import ecg.modelo.RegistroECG;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Almacena las mediciones de la ejecución en curso y el historial de ejecuciones cerradas. */
public class RegistroMetricas {

    private final List<MetricasEjecucion> actuales = new ArrayList<>();
    private final List<ResultadoRendimiento> historial = new ArrayList<>();
    private String nombreRegistro = "—";
    private int canales;
    private double fs;
    private long muestras;

    public synchronized void agregar(MetricasEjecucion m) { actuales.add(m); }

    public synchronized List<MetricasEjecucion> getMetricas() { return new ArrayList<>(actuales); }

    public synchronized List<ResultadoRendimiento> getHistorial() { return Collections.unmodifiableList(historial); }

    public synchronized void setInformacionRegistro(RegistroECG r) {
        nombreRegistro = r.getNombreRegistro(); canales = r.getNumeroCanales();
        fs = r.getFrecuenciaMuestreo(); muestras = r.getNumeroMuestras();
    }

    /** Copia de lo medido hasta ahora, sin vaciar el registro. */
    public synchronized ResultadoRendimiento instantanea(String observaciones) {
        ResultadoRendimiento r = new ResultadoRendimiento();
        r.setInformacionRegistro(nombreRegistro, canales, fs, muestras);
        r.setObservaciones(observaciones);
        actuales.forEach(r::agregar);
        return r;
    }

    /** Consolida la ejecución: la guarda en el historial y empieza una nueva. */
    public synchronized ResultadoRendimiento cerrarEjecucion(String observaciones) {
        ResultadoRendimiento r = instantanea(observaciones);
        historial.add(r);
        actuales.clear();
        return r;
    }

    public synchronized void vaciar() { actuales.clear(); }
}
