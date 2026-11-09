package ecg.ml.estructuras;

/**
 * Media de los últimos k valores en O(1) por actualización (suma acumulada sobre una cola circular).
 * Equivalente de la clase MovingMean de estructuras/cola_circular.py.
 *
 * <p>Uso en el proyecto: media de los últimos 10 intervalos RR al extraer características.
 */
public class MovingMean {

    private final CircularQueue<Double> q;
    private double sum;

    public MovingMean(int k) {
        this.q = new CircularQueue<>(k);
        this.sum = 0.0;
    }

    public void push(double x) {
        Double evicted = q.enqueueOverwrite(x);
        if (evicted != null) {
            sum -= evicted;
        }
        sum += x;
    }

    public int count() {
        return q.size();
    }

    /** Media de los valores presentes. {@link Double#NaN} si aún no hay ninguno. */
    public double mean() {
        int n = q.size();
        return n == 0 ? Double.NaN : sum / n;
    }

    public boolean hasValue() {
        return q.size() > 0;
    }
}
