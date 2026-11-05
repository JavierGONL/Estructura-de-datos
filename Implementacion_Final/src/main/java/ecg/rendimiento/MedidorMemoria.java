package ecg.rendimiento;

import ecg.modelo.MetricasEjecucion;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Memoria durante una operación:
 * <ul>
 *   <li><b>heap usado</b> = totalMemory − freeMemory (solo el heap de Java);</li>
 *   <li><b>heap máximo observado</b>: muestreado cada pocos ms por un hilo auxiliar (es una cota inferior
 *       del pico real: los picos más breves que el periodo de muestreo pueden no verse);</li>
 *   <li><b>memoria reservada por la JVM</b> = totalMemory;</li>
 *   <li><b>RSS</b> del proceso: solo se obtiene en Linux (/proc/self/status); en otros sistemas es "no disponible".</li>
 * </ul>
 * El heap y el RSS son magnitudes distintas y no deben presentarse como equivalentes.
 */
public class MedidorMemoria {

    private static final long PERIODO_MUESTREO_MS = 5;

    private long inicial, finalMem, maximo, reservada, rss = MetricasEjecucion.NO_DISPONIBLE;
    private volatile boolean activo;
    private volatile long maximoObservado;
    private Thread muestreador;

    public static long heapUsada() {
        Runtime r = Runtime.getRuntime();
        return r.totalMemory() - r.freeMemory();
    }

    public void iniciar() {
        inicial = heapUsada();
        maximoObservado = inicial;
        activo = true;
        muestreador = new Thread(() -> {
            while (activo) {
                long u = heapUsada();
                if (u > maximoObservado) maximoObservado = u;
                try { Thread.sleep(PERIODO_MUESTREO_MS); } catch (InterruptedException e) { return; }
            }
        }, "muestreador-memoria");
        muestreador.setDaemon(true);
        muestreador.start();
    }

    public void finalizar() {
        activo = false;
        if (muestreador != null) {
            muestreador.interrupt();
            try { muestreador.join(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        finalMem = heapUsada();
        maximo = Math.max(maximoObservado, Math.max(inicial, finalMem));
        reservada = Runtime.getRuntime().totalMemory();
        rss = leerRSS();
    }

    /** RSS en bytes (Linux) o {@link MetricasEjecucion#NO_DISPONIBLE}. */
    public static long leerRSS() {
        try {
            Path p = Path.of("/proc/self/status");
            if (!Files.isReadable(p)) return MetricasEjecucion.NO_DISPONIBLE;
            for (String l : Files.readAllLines(p)) {
                if (l.startsWith("VmRSS:")) {
                    String[] t = l.split("\\s+");
                    return Long.parseLong(t[1]) * 1024L;      // kB → bytes
                }
            }
        } catch (Exception ignorada) { /* se informa como no disponible */ }
        return MetricasEjecucion.NO_DISPONIBLE;
    }

    public long getInicial() { return inicial; }
    public long getFinal() { return finalMem; }
    public long getMaximo() { return maximo; }
    public long getReservada() { return reservada; }
    public long getRSS() { return rss; }
}
