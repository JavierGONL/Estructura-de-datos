package ecg.ml;

import java.util.Arrays;

/**
 * Características de cada latido para el clasificador. Equivalente exacto de caracteristicas.py:
 * ambas deben dar los mismos números (se comprueba con ParidadCaracteristicas).
 *
 * Vector de 257 valores por latido:
 *   [0..251] ventana de MLII en mV (90 muestras antes, 162 desde el latido) menos su mediana
 *   [252] rr_prev  [253] rr_next  [254] rr_media (últimos 10 RR)  [255] ratio_prev  [256] ratio_next
 */
public final class Caracteristicas {
    public static final double FS = 360.0;
    public static final int ANTES = 90, DESPUES = 162, N_VENTANA = ANTES + DESPUES;
    public static final int RADIO_RECENTRADO = 18, N_RR_MEDIA = 10, RR_DEFECTO = 288;
    public static final int N_CARACT = N_VENTANA + 5;

    private Caracteristicas() { }

    /** Mediana de x[a, b): ordena una copia y toma el centro (o el promedio de los dos centrales). */
    static double mediana(double[] x, int a, int b) {
        double[] c = Arrays.copyOfRange(x, a, b);
        Arrays.sort(c);
        int n = c.length;
        return (n % 2 == 1) ? c[n / 2] : (c[n / 2 - 1] + c[n / 2]) / 2.0;
    }

    /** Mueve el latido al mayor |x - mediana| en +-50 ms (el primero si hay empate). */
    public static int recentrar(double[] x, int pos) {
        double base = mediana(x, Math.max(0, pos - ANTES), Math.min(x.length, pos + DESPUES));
        int lo = Math.max(0, pos - RADIO_RECENTRADO), hi = Math.min(x.length - 1, pos + RADIO_RECENTRADO);
        int mejor = lo;
        double max = -1;
        for (int i = lo; i <= hi; i++) {
            double v = Math.abs(x[i] - base);
            if (v > max) { max = v; mejor = i; }
        }
        return mejor;
    }

    /**
     * Características de todos los latidos de x (señal MLII en mV) cuyas posiciones son `posiciones`
     * (ordenadas). Devuelve una fila por latido; es null si su ventana de 252 muestras no cabe en la señal.
     */
    public static double[][] extraer(double[] x, int[] posiciones) {
        int m = posiciones.length;
        int[] c = new int[m];
        for (int i = 0; i < m; i++) c[i] = recentrar(x, posiciones[i]);
        int[] d = new int[m];                       // d[i] = muestras entre el latido i-1 y el i
        for (int i = 1; i < m; i++) d[i] = c[i] - c[i - 1];

        double[][] filas = new double[m][];
        for (int i = 0; i < m; i++) {
            boolean hayPrev = i > 0, hayNext = i < m - 1;
            int dPrev = hayPrev ? d[i] : (hayNext ? d[i + 1] : RR_DEFECTO);
            int dNext = hayNext ? d[i + 1] : dPrev;
            long suma = 0;
            int cuenta = 0;
            for (int k = Math.max(1, i - N_RR_MEDIA + 1); k <= i; k++) { suma += d[k]; cuenta++; }
            double media = (cuenta == 0) ? dPrev : (double) suma / cuenta;

            if (c[i] - ANTES < 0 || c[i] + DESPUES > x.length) continue;
            double[] f = new double[N_CARACT];
            double med = mediana(x, c[i] - ANTES, c[i] + DESPUES);
            for (int j = 0; j < N_VENTANA; j++) f[j] = x[c[i] - ANTES + j] - med;
            f[N_VENTANA] = dPrev / FS;
            f[N_VENTANA + 1] = dNext / FS;
            f[N_VENTANA + 2] = media / FS;
            f[N_VENTANA + 3] = dPrev / media;
            f[N_VENTANA + 4] = dNext / media;
            filas[i] = f;
        }
        return filas;
    }
}
