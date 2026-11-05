package ecg.rendimiento;

import ecg.modelo.MetricasEjecucion;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tiempo de CPU en dos niveles:
 * <ol>
 *   <li><b>Portable</b>: CPU del hilo que mide ({@link ThreadMXBean}) y CPU total del proceso
 *       ({@code com.sun.management.OperatingSystemMXBean}), si la JVM los soporta.</li>
 *   <li><b>Específico del SO</b>: CPU de usuario y de sistema del proceso completo, leídos de
 *       {@code /proc/self/stat} (Linux). En otros sistemas quedan como "no disponible"; no se
 *       estiman restando magnitudes de fuentes distintas.</li>
 * </ol>
 * Los contadores de /proc están en "ticks" (CLK_TCK); Java no puede consultarlo, se asume el valor
 * habitual de 100 Hz.
 * <p>
 * {@link #iniciar()} y {@link #finalizar()} deben llamarse desde el mismo hilo.
 */
public class MedidorCPU {

    private static final long NS_POR_TICK = 1_000_000_000L / 100;
    private static final long ND = MetricasEjecucion.NO_DISPONIBLE;

    private long hiloIni = ND, procesoIni = ND, usuarioIni = ND, sistemaIni = ND;
    private long hilo = ND, proceso = ND, usuario = ND, sistema = ND;

    public void iniciar() {
        hiloIni = cpuHilo();
        procesoIni = cpuProceso();
        long[] us = usuarioYSistema();
        usuarioIni = us[0];
        sistemaIni = us[1];
    }

    public void finalizar() {
        hilo = delta(hiloIni, cpuHilo());
        proceso = delta(procesoIni, cpuProceso());
        long[] us = usuarioYSistema();
        usuario = delta(usuarioIni, us[0]);
        sistema = delta(sistemaIni, us[1]);
    }

    private static long delta(long a, long b) { return (a == ND || b == ND) ? ND : Math.max(0, b - a); }

    public static long cpuHilo() {
        ThreadMXBean t = ManagementFactory.getThreadMXBean();
        try {
            if (t.isCurrentThreadCpuTimeSupported() && t.isThreadCpuTimeEnabled()) return t.getCurrentThreadCpuTime();
        } catch (UnsupportedOperationException | SecurityException ignorada) { /* no disponible */ }
        return ND;
    }

    public static long cpuProceso() {
        var os = ManagementFactory.getOperatingSystemMXBean();
        if (os instanceof com.sun.management.OperatingSystemMXBean) {
            long v = ((com.sun.management.OperatingSystemMXBean) os).getProcessCpuTime();
            return v >= 0 ? v : ND;
        }
        return ND;
    }

    /** {usuario, sistema} en ns, o {ND, ND}. */
    public static long[] usuarioYSistema() {
        try {
            Path p = Path.of("/proc/self/stat");
            if (!Files.isReadable(p)) return new long[]{ND, ND};
            String s = Files.readString(p);
            // El nombre del proceso (campo 2) va entre paréntesis y puede contener espacios.
            String resto = s.substring(s.lastIndexOf(')') + 2);
            String[] t = resto.split("\\s+");           // t[0] = campo 3 (estado)
            long utime = Long.parseLong(t[11]);         // campo 14
            long stime = Long.parseLong(t[12]);         // campo 15
            return new long[]{utime * NS_POR_TICK, stime * NS_POR_TICK};
        } catch (Exception e) {
            return new long[]{ND, ND};
        }
    }

    public long getHilo() { return hilo; }
    public long getProceso() { return proceso; }
    public long getUsuario() { return usuario; }
    public long getSistema() { return sistema; }
}
