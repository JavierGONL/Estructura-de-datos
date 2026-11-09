package ecg.ml;

import java.util.List;

/**
 * Cálculo de la ENTROPÍA de un grupo de muestras. La entropía mide el "desorden" de clases: vale 0 si
 * todas las muestras son de la misma clase y crece cuanto más mezcladas están entre varias clases.
 *
 * <p>El árbol la usa para elegir cómo partir: la mejor pregunta es la que más REDUCE la entropía, es
 * decir, la que deja los dos grupos resultantes lo más puros posible.
 */
public final class Entropia {

    private Entropia() {
    }

    /**
     * Entropía de Shannon: {@code -suma( p_c * log2(p_c) )}, donde {@code p_c} es la proporción de
     * muestras de la clase c.
     *
     * <p>Cómo: cuenta cuántas muestras hay de cada clase, las pasa a proporciones y suma
     * {@code -p*log2(p)} por cada clase presente (las clases con 0 muestras no aportan). Se usa log2
     * para medir la entropía en bits.
     */
    public static double entropia(List<Muestra> muestras, int nClases) {
        if (muestras.isEmpty()) {
            return 0.0;
        }
        int[] conteo = new int[nClases];
        for (Muestra m : muestras) {
            conteo[m.etiqueta()]++;
        }
        double total = muestras.size();
        double entropia = 0.0;
        for (int c : conteo) {
            if (c > 0) {
                double p = c / total;
                entropia -= p * (Math.log(p) / Math.log(2));   // log2(p) = ln(p) / ln(2)
            }
        }
        return entropia;
    }
}
