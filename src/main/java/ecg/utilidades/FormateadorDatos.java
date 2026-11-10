package ecg.utilidades;

import java.util.Locale;

/** Formato de cantidades para pantalla e informes. */
public final class FormateadorDatos {
    private FormateadorDatos() { }

    public static String bytes(long b) {
        if (b < 0) return "no disponible";
        if (b < 1024) return b + " B";
        double v = b / 1024.0;
        if (v < 1024) return String.format(Locale.ROOT, "%.1f KiB", v);
        v /= 1024;
        if (v < 1024) return String.format(Locale.ROOT, "%.1f MiB", v);
        return String.format(Locale.ROOT, "%.2f GiB", v / 1024);
    }

    /** Milisegundos con tres decimales a partir de nanosegundos; "n/d" si no está disponible. */
    public static String ms(long ns) { return ns < 0 ? "n/d" : String.format(Locale.ROOT, "%.3f", ns / 1e6); }

    public static String mb(long bytes) { return bytes < 0 ? "n/d" : String.format(Locale.ROOT, "%.2f", bytes / 1048576.0); }
}
