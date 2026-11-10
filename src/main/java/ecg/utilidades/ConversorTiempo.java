package ecg.utilidades;

/** Conversiones entre muestras, segundos y nanosegundos. */
public final class ConversorTiempo {
    private ConversorTiempo() { }

    public static double muestraASegundos(long muestra, double fs) { return muestra / fs; }
    public static long segundosAMuestra(double segundos, double fs) { return Math.round(segundos * fs); }
    public static double nsAMs(long ns) { return ns / 1_000_000.0; }
    public static double nsASegundos(long ns) { return ns / 1_000_000_000.0; }

    /** Formato mm:ss.mmm */
    public static String formatoReloj(double segundos) {
        long ms = Math.round(segundos * 1000);
        return String.format("%02d:%02d.%03d", ms / 60000, (ms / 1000) % 60, ms % 1000);
    }
}
