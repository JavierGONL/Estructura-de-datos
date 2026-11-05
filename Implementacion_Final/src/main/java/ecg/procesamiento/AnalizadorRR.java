package ecg.procesamiento;

import java.util.List;

/** Intervalos RR: {@code RR_i = (n_{i+1} - n_i) / fs}. */
public class AnalizadorRR {

    public static class Estadisticas {
        public final double mediaSegundos, sdnnSegundos, rmssdSegundos;
        Estadisticas(double m, double s, double r) { mediaSegundos = m; sdnnSegundos = s; rmssdSegundos = r; }
    }

    public double[] calcularRR(List<Long> indices, double fs) {
        if (indices.size() < 2) return new double[0];
        double[] rr = new double[indices.size() - 1];
        for (int i = 0; i < rr.length; i++) rr[i] = (indices.get(i + 1) - indices.get(i)) / fs;
        return rr;
    }

    public Estadisticas estadisticas(double[] rr) {
        if (rr.length == 0) return new Estadisticas(Double.NaN, Double.NaN, Double.NaN);
        double media = 0;
        for (double v : rr) media += v;
        media /= rr.length;
        double var = 0;
        for (double v : rr) var += (v - media) * (v - media);
        double sdnn = rr.length > 1 ? Math.sqrt(var / (rr.length - 1)) : 0;
        double dif = 0;
        for (int i = 1; i < rr.length; i++) dif += (rr[i] - rr[i - 1]) * (rr[i] - rr[i - 1]);
        double rmssd = rr.length > 1 ? Math.sqrt(dif / (rr.length - 1)) : 0;
        return new Estadisticas(media, sdnn, rmssd);
    }
}
