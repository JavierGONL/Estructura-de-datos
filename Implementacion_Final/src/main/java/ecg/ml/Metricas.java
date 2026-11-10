package ecg.ml;

/**
 * Métricas de clasificación a partir de una matriz de confusión 4x4 (N, S, V, F).
 * Equivalente de metricas.py.
 *
 * <p>Convención: {@code M[real][predicho]}. Para cada clase c se trata como "c contra el resto":
 * <pre>
 *   TP = M[c][c]   FN = fila c - TP   FP = columna c - TP   TN = total - TP - FN - FP
 *   precision = TP/(TP+FP)   recall = TP/(TP+FN)   especificidad = TN/(TN+FP)   F1 = 2PR/(P+R)
 * </pre>
 * Si un denominador es 0 la métrica vale 0.0.
 */
public final class Metricas {

    public static final String[] CLASES = {"N", "S", "V", "F"};

    private Metricas() {
    }

    /** Métricas por clase: soporte, precision, recall, especificidad, f1. */
    public static final class PorClase {
        public final int soporte;
        public final double precision;
        public final double recall;
        public final double especificidad;
        public final double f1;

        PorClase(int soporte, double precision, double recall, double especificidad, double f1) {
            this.soporte = soporte;
            this.precision = precision;
            this.recall = recall;
            this.especificidad = especificidad;
            this.f1 = f1;
        }
    }

    public static int[][] matrizConfusion(int[] yReal, int[] yPred) {
        return matrizConfusion(yReal, yPred, CLASES.length);
    }

    public static int[][] matrizConfusion(int[] yReal, int[] yPred, int k) {
        int[][] m = new int[k][k];
        for (int i = 0; i < yReal.length; i++) {
            m[yReal[i]][yPred[i]]++;
        }
        return m;
    }

    private static double div(double a, double b) {
        return b > 0 ? a / b : 0.0;
    }

    public static PorClase[] porClase(int[][] m) {
        int total = 0;
        for (int[] fila : m) {
            for (int v : fila) {
                total += v;
            }
        }
        int k = m.length;
        PorClase[] out = new PorClase[k];
        for (int c = 0; c < k; c++) {
            int tp = m[c][c];
            int filaSum = 0;
            int colSum = 0;
            for (int j = 0; j < k; j++) {
                filaSum += m[c][j];
                colSum += m[j][c];
            }
            int fn = filaSum - tp;
            int fp = colSum - tp;
            int tn = total - tp - fn - fp;
            double p = div(tp, tp + fp);
            double rc = div(tp, tp + fn);
            out[c] = new PorClase(tp + fn, p, rc, div(tn, tn + fp), div(2 * p * rc, p + rc));
        }
        return out;
    }

    public static double accuracy(int[][] m) {
        int traza = 0;
        int total = 0;
        for (int i = 0; i < m.length; i++) {
            traza += m[i][i];
            for (int v : m[i]) {
                total += v;
            }
        }
        return div(traza, total);
    }

    public static double f1Macro(int[][] m) {
        PorClase[] pc = porClase(m);
        double s = 0;
        for (PorClase f : pc) {
            s += f.f1;
        }
        return s / pc.length;
    }

    /** Texto con la matriz de confusión y la tabla de métricas por clase. */
    public static String reporte(int[][] m) {
        StringBuilder sb = new StringBuilder();
        sb.append("matriz de confusión (filas = real, columnas = predicho)\n");
        sb.append("      ");
        for (String c : CLASES) {
            sb.append(String.format("%8s", c));
        }
        sb.append('\n');
        for (int i = 0; i < CLASES.length; i++) {
            sb.append(String.format("%5s ", CLASES[i]));
            for (int v : m[i]) {
                sb.append(String.format("%8d", v));
            }
            sb.append('\n');
        }
        sb.append('\n');
        sb.append(String.format("%5s %8s %10s %8s %9s %7s%n",
                "clase", "soporte", "precision", "recall", "especif.", "F1"));
        PorClase[] pc = porClase(m);
        for (int i = 0; i < CLASES.length; i++) {
            PorClase f = pc[i];
            sb.append(String.format("%5s %8d %10.3f %8.3f %9.4f %7.3f%n",
                    CLASES[i], f.soporte, f.precision, f.recall, f.especificidad, f.f1));
        }
        sb.append(String.format("accuracy = %.4f   F1 macro = %.3f", accuracy(m), f1Macro(m)));
        return sb.toString();
    }
}
