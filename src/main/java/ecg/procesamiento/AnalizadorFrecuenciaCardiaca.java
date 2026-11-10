package ecg.procesamiento;

/** Frecuencia cardíaca instantánea: {@code FC_i = 60 / RR_i} (latidos por minuto). */
public class AnalizadorFrecuenciaCardiaca {

    public double[] instantanea(double[] rrSegundos) {
        double[] fc = new double[rrSegundos.length];
        for (int i = 0; i < fc.length; i++) fc[i] = rrSegundos[i] > 0 ? 60.0 / rrSegundos[i] : Double.NaN;
        return fc;
    }

    /** FC media calculada a partir del RR medio (no como media de FC instantáneas). */
    public double media(double[] rrSegundos) {
        if (rrSegundos.length == 0) return Double.NaN;
        double suma = 0;
        for (double v : rrSegundos) suma += v;
        return 60.0 / (suma / rrSegundos.length);
    }
}
