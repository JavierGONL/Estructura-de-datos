package ecg.rendimiento;

import ecg.modelo.MetricasEjecucion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Resultados de una ejecución (sesión o evaluación experimental); se conservan en el historial. */
public class ResultadoRendimiento {

    /** Estadísticas agregadas de todas las mediciones con el mismo nombre de operación. */
    public static class ResumenOperacion {
        public final String operacion;
        public final int repeticiones;
        public final double tiempoMedioNs;
        public final long tiempoMinNs, tiempoMaxNs;
        public final double cpuUsuarioMedioNs, cpuSistemaMedioNs, cpuTotalMedioNs;   // -1 = no disponible
        public final long heapMaximoBytes;
        public final long rssMaximoBytes;                                            // -1 = no disponible

        ResumenOperacion(String op, List<MetricasEjecucion> m) {
            operacion = op;
            repeticiones = m.size();
            tiempoMedioNs = m.stream().mapToLong(MetricasEjecucion::getTiempoPared).average().orElse(0);
            tiempoMinNs = m.stream().mapToLong(MetricasEjecucion::getTiempoPared).min().orElse(0);
            tiempoMaxNs = m.stream().mapToLong(MetricasEjecucion::getTiempoPared).max().orElse(0);
            cpuUsuarioMedioNs = mediaDisponible(m, MetricasEjecucion::getTiempoCPUUsuario);
            cpuSistemaMedioNs = mediaDisponible(m, MetricasEjecucion::getTiempoCPUSistema);
            cpuTotalMedioNs = mediaDisponible(m, MetricasEjecucion::getTiempoCPUTotal);
            heapMaximoBytes = m.stream().mapToLong(MetricasEjecucion::getMemoriaMaxima).max().orElse(0);
            rssMaximoBytes = m.stream().mapToLong(MetricasEjecucion::getMemoriaRSS).max().orElse(-1);
        }

        private static double mediaDisponible(List<MetricasEjecucion> m,
                                              java.util.function.ToLongFunction<MetricasEjecucion> f) {
            return m.stream().mapToLong(f).filter(v -> v >= 0).average().orElse(-1);
        }
    }

    private final LocalDateTime fecha = LocalDateTime.now();
    private String nombreRegistro = "—";
    private int numeroCanales;
    private double frecuenciaMuestreo;
    private long numeroMuestras;
    private String observaciones = "";
    private String resumenClasificacion = "";   // texto opcional del RF (matriz/accuracy); vacío si no hubo
    private final List<MetricasEjecucion> metricas = new ArrayList<>();

    private final String versionJava = System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")";
    private final String sistemaOperativo = System.getProperty("os.name") + " " + System.getProperty("os.version")
            + " (" + System.getProperty("os.arch") + ")";
    private final int procesadores = Runtime.getRuntime().availableProcessors();
    private final long heapMaximoJVM = Runtime.getRuntime().maxMemory();

    public void setInformacionRegistro(String nombre, int canales, double fs, long muestras) {
        this.nombreRegistro = nombre; this.numeroCanales = canales;
        this.frecuenciaMuestreo = fs; this.numeroMuestras = muestras;
    }

    public void setObservaciones(String o) { this.observaciones = o == null ? "" : o; }
    public void setResumenClasificacion(String s) { this.resumenClasificacion = s == null ? "" : s; }
    public void agregar(MetricasEjecucion m) { metricas.add(m); }

    public LocalDateTime getFecha() { return fecha; }
    public String getNombreRegistro() { return nombreRegistro; }
    public int getNumeroCanales() { return numeroCanales; }
    public double getFrecuenciaMuestreo() { return frecuenciaMuestreo; }
    public long getNumeroMuestras() { return numeroMuestras; }
    public String getObservaciones() { return observaciones; }
    public String getResumenClasificacion() { return resumenClasificacion; }
    public boolean tieneClasificacion() { return !resumenClasificacion.isBlank(); }
    public List<MetricasEjecucion> getMetricas() { return metricas; }
    public String getVersionJava() { return versionJava; }
    public String getSistemaOperativo() { return sistemaOperativo; }
    public int getProcesadores() { return procesadores; }
    public long getHeapMaximoJVM() { return heapMaximoJVM; }

    /** Resumen por operación, en orden de primera aparición. */
    public List<ResumenOperacion> resumirPorOperacion() {
        Map<String, List<MetricasEjecucion>> g = new LinkedHashMap<>();
        for (MetricasEjecucion m : metricas) g.computeIfAbsent(m.getNombreOperacion(), k -> new ArrayList<>()).add(m);
        List<ResumenOperacion> r = new ArrayList<>();
        g.forEach((k, v) -> r.add(new ResumenOperacion(k, v)));
        return r;
    }

    /** Suma de tiempos de pared de las operaciones de primer nivel (las anidadas no se cuentan dos veces). */
    public long tiempoTotalPrimerNivelNs() {
        return metricas.stream().filter(m -> m.getProfundidad() == 0).mapToLong(MetricasEjecucion::getTiempoPared).sum();
    }

    public long heapMaximoObservado() {
        return metricas.stream().mapToLong(MetricasEjecucion::getMemoriaMaxima).max().orElse(0);
    }

    public long rssMaximoObservado() {
        return metricas.stream().mapToLong(MetricasEjecucion::getMemoriaRSS).max().orElse(MetricasEjecucion.NO_DISPONIBLE);
    }
}
