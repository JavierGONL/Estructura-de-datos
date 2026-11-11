package ecg.procesamiento;

import java.util.ArrayList;
import java.util.List;

/**
 * Variante del algoritmo de Pan-Tompkins independiente de la frecuencia de muestreo:
 * filtrado paso banda (media móvil), derivada, cuadrado, integración en ventana móvil y
 * umbral adaptativo con periodo refractario y rechazo de ondas T por pendiente.
 * No incluye la búsqueda hacia atrás de latidos perdidos.
 */
public class DetectorPanTompkins implements DetectorQRS {

    private static final double VENTANA_LINEA_BASE = 0.20;   // s
    private static final double VENTANA_SUAVIZADO = 0.025;   // s
    private static final double VENTANA_INTEGRACION = 0.15;  // s
    private static final double REFRACTARIO = 0.20;          // s
    private static final double VENTANA_ONDA_T = 0.36;       // s
    private static final double RADIO_REFINADO = 0.08;       // s

    @Override
    public String getNombre() { return "Pan-Tompkins (media móvil)"; }

    @Override
    public List<Long> detectar(double[] x, double fs) {
        int n = x.length;
        List<Long> resultado = new ArrayList<>();
        if (n < (int) (fs * 1.0) || fs <= 0) return resultado;

        double[] limpio = reemplazarNaN(x);
        double[] filtrada = restar(limpio, mediaMovil(limpio, ventana(VENTANA_LINEA_BASE, fs)));
        filtrada = mediaMovil(filtrada, ventana(VENTANA_SUAVIZADO, fs));

        double[] energia = new double[n];
        double[] derivada = new double[n];
        for (int i = 2; i < n - 2; i++) {
            derivada[i] = (2 * filtrada[i + 2] + filtrada[i + 1] - filtrada[i - 1] - 2 * filtrada[i - 2]) / 8.0;
            energia[i] = derivada[i] * derivada[i];
        }
        double[] integrada = mediaMovil(energia, ventana(VENTANA_INTEGRACION, fs));

        List<Integer> picos = seleccionarPicos(integrada, derivada, fs);

        // Refinar la posición al máximo de |señal filtrada| alrededor del pico integrado.
        int radio = Math.max(1, (int) Math.round(RADIO_REFINADO * fs));
        List<Integer> refinados = new ArrayList<>();
        for (int p : picos) {
            int a = Math.max(0, p - radio), b = Math.min(n - 1, p + radio);
            int mejor = a;
            for (int i = a; i <= b; i++) if (Math.abs(filtrada[i]) > Math.abs(filtrada[mejor])) mejor = i;
            if (!refinados.isEmpty()
                    && mejor - refinados.get(refinados.size() - 1) < (int) (REFRACTARIO * fs)) {
                int previo = refinados.get(refinados.size() - 1);
                if (Math.abs(filtrada[mejor]) > Math.abs(filtrada[previo])) refinados.set(refinados.size() - 1, mejor);
            } else {
                refinados.add(mejor);
            }
        }
        for (int i : refinados) resultado.add((long) i);
        return resultado;
    }

    private List<Integer> seleccionarPicos(double[] mw, double[] d, double fs) {
        int n = mw.length;
        int inicial = (int) Math.min(n, Math.round(2 * fs));
        double max = 0, suma = 0;
        for (int i = 0; i < inicial; i++) { max = Math.max(max, mw[i]); suma += mw[i]; }
        double spki = 0.25 * max;
        double npki = 0.5 * suma / Math.max(1, inicial);
        double umbral = npki + 0.25 * (spki - npki);

        int refractario = (int) Math.round(REFRACTARIO * fs);
        int ventanaT = (int) Math.round(VENTANA_ONDA_T * fs);
        int radioPendiente = Math.max(1, (int) Math.round(VENTANA_INTEGRACION * fs / 2));

        // Máximo de |d| en la ventana [i-radio, i+radio] para todo i, en O(n) con una ventana
        // deslizante (antes se recalculaba con maxAbs en cada candidato, O(n·ventana)).
        double[] pendienteVentana = maxAbsVentana(d, radioPendiente);

        List<Integer> picos = new ArrayList<>();
        int ultimo = -1_000_000;
        double pendienteUltimo = 0;

        for (int i = 1; i < n - 1; i++) {
            if (!(mw[i] >= mw[i - 1] && mw[i] > mw[i + 1])) continue;
            double pendiente = pendienteVentana[i];

            if (mw[i] > umbral) {
                int distancia = i - ultimo;
                if (distancia < refractario) {
                    if (!picos.isEmpty() && mw[i] > mw[ultimo]) {   // el mismo latido, pico mayor
                        picos.set(picos.size() - 1, i);
                        ultimo = i;
                        pendienteUltimo = pendiente;
                    }
                } else if (distancia < ventanaT && pendiente < 0.5 * pendienteUltimo) {
                    npki = 0.125 * mw[i] + 0.875 * npki;            // onda T
                } else {
                    picos.add(i);
                    spki = 0.125 * mw[i] + 0.875 * spki;
                    ultimo = i;
                    pendienteUltimo = pendiente;
                }
            } else {
                npki = 0.125 * mw[i] + 0.875 * npki;
            }
            umbral = npki + 0.25 * (spki - npki);
        }
        return picos;
    }

    private static int ventana(double segundos, double fs) { return Math.max(1, (int) Math.round(segundos * fs)); }

    /**
     * Máximo de |v| en la ventana centrada [i-radio, i+radio] para cada i, en O(n) usando un deque
     * monótono decreciente (ventana deslizante). Sustituye al cálculo O(n·ventana) por candidato.
     */
    private static double[] maxAbsVentana(double[] v, int radio) {
        int n = v.length;
        double[] r = new double[n];
        int[] deque = new int[n];   // índices, con |v| decreciente desde el frente
        int cabeza = 0, cola = 0;   // [cabeza, cola)
        // Precarga la parte izquierda de la ventana del primer i (índices 0..radio-1); el índice
        // i+radio de cada paso lo añade el bucle.
        for (int j = 0; j < radio && j < n; j++) {
            double vj = Math.abs(v[j]);
            while (cola > cabeza && Math.abs(v[deque[cola - 1]]) <= vj) cola--;
            deque[cola++] = j;
        }
        for (int i = 0; i < n; i++) {
            // 1) incorpora el índice que entra por la derecha de la ventana centrada en i.
            int entra = i + radio;
            if (entra < n) {
                double ve = Math.abs(v[entra]);
                while (cola > cabeza && Math.abs(v[deque[cola - 1]]) <= ve) cola--;
                deque[cola++] = entra;
            }
            // 2) descarta por la izquierda lo que ya salió de la ventana [i-radio, i+radio].
            int limite = i - radio;
            while (cola > cabeza && deque[cabeza] < limite) cabeza++;
            r[i] = (cola > cabeza) ? Math.abs(v[deque[cabeza]]) : 0.0;
        }
        return r;
    }

    private static double[] reemplazarNaN(double[] x) {
        double[] r = x.clone();
        double previo = 0;
        for (int i = 0; i < r.length; i++) {
            if (Double.isNaN(r[i])) r[i] = previo; else previo = r[i];
        }
        return r;
    }

    private static double[] restar(double[] a, double[] b) {
        double[] r = new double[a.length];
        for (int i = 0; i < r.length; i++) r[i] = a[i] - b[i];
        return r;
    }

    /** Media móvil centrada con sumas acumuladas (O(n)); los extremos usan la ventana disponible. */
    static double[] mediaMovil(double[] x, int ventana) {
        int n = x.length;
        double[] acum = new double[n + 1];
        for (int i = 0; i < n; i++) acum[i + 1] = acum[i] + x[i];
        double[] r = new double[n];
        int h = ventana / 2;
        for (int i = 0; i < n; i++) {
            int a = Math.max(0, i - h), b = Math.min(n - 1, i + h);
            r[i] = (acum[b + 1] - acum[a]) / (b - a + 1);
        }
        return r;
    }
}
