package ecg.procesamiento;

/** Métricas de una comparación entre detecciones y anotaciones de referencia. */
public class ResultadoValidacion {

    private final int tp, fp, fn;
    private final double errorMedioMs, errorMaximoMs;

    public ResultadoValidacion(int tp, int fp, int fn, double errorMedioMs, double errorMaximoMs) {
        this.tp = tp; this.fp = fp; this.fn = fn;
        this.errorMedioMs = errorMedioMs; this.errorMaximoMs = errorMaximoMs;
    }

    public int getVerdaderosPositivos() { return tp; }
    public int getFalsosPositivos() { return fp; }
    public int getFalsosNegativos() { return fn; }
    public double getSensibilidad() { return tp + fn == 0 ? Double.NaN : (double) tp / (tp + fn); }
    public double getPrecision() { return tp + fp == 0 ? Double.NaN : (double) tp / (tp + fp); }
    public double getF1() {
        double s = getSensibilidad(), p = getPrecision();
        return (Double.isNaN(s) || Double.isNaN(p) || s + p == 0) ? Double.NaN : 2 * s * p / (s + p);
    }
    /** Error temporal medio absoluto de los latidos emparejados, en milisegundos. */
    public double getErrorTemporalMedioMs() { return errorMedioMs; }
    public double getErrorTemporalMaximoMs() { return errorMaximoMs; }

    @Override
    public String toString() {
        return String.format("TP=%d FP=%d FN=%d Se=%.3f P+=%.3f F1=%.3f err=%.1f ms",
                tp, fp, fn, getSensibilidad(), getPrecision(), getF1(), errorMedioMs);
    }
}
