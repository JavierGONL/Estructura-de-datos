package ecg.ml;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Árbol de decisión SIMPLE.
 *
 * <p>QUÉ hace: aprende una serie de preguntas "¿la característica j es menor que un umbral?" que, paso
 * a paso, separan los latidos por su clase (N, S, V, F). Al final, cada hoja guarda la proporción de
 * clases de las muestras que cayeron en ella.
 *
 * <p>CÓMO: en cada nodo elige al azar un subconjunto de características, y de ellas se queda con la que
 * más reduce la ENTROPÍA (el desorden de clases). El umbral de corte es la MEDIA de esa característica
 * en las muestras del nodo. Repite de forma recursiva hasta que el grupo es puro o demasiado pequeño.
 *
 * <p>POR QUÉ así: se usa entropía + corte por la media y azar con
 * {@link java.util.Random}. Es más fácil de leer y explicar; a cambio, no es reproducible bit a bit ni
 * se guarda en archivo.
 */
public class ArbolDesicion {

    /** Nodo del árbol. Si {@code hoja != null} es una hoja; si no, es una pregunta con dos hijos. */
    private static final class Nodo {
        // ---- solo en nodos internos (preguntas) ----
        int caracteristica;     // índice de la característica por la que se pregunta
        double umbral;          // valor de corte: izquierda si x[caracteristica] < umbral
        Nodo izquierda;
        Nodo derecha;
        // ---- solo en hojas ----
        double[] distribucion;  // proporción de cada clase en la hoja (suma 1); null en los internos
    }

    private final int nClases;
    private final int maxCaracteristicas;   // cuántas características se miran por nodo
    private final int minMuestrasHoja;      // si quedan menos muestras que esto, se crea una hoja
    private final Random rng;
    private Nodo raiz;

    public ArbolDesicion(int nClases, int maxCaracteristicas, int minMuestrasHoja, Random rng) {
        this.nClases = nClases;
        this.maxCaracteristicas = maxCaracteristicas;
        this.minMuestrasHoja = minMuestrasHoja;
        this.rng = rng;
    }

    /** Entrena el árbol con las muestras indicadas (ya sorteadas por bootstrap desde el bosque). */
    public void entrenar(List<Muestra> muestras) {
        raiz = construir(muestras);
    }

    /**
     * Construye recursivamente un subárbol a partir de un grupo de muestras.
     * Condiciones de parada (se crea una hoja): el grupo es pequeño o todas las muestras son de la
     * misma clase; también si ninguna partición logra separar las muestras.
     */
    private Nodo construir(List<Muestra> muestras) {
        Nodo nodo = new Nodo();
        if (muestras.size() < minMuestrasHoja || mismaClase(muestras)) {
            nodo.distribucion = distribucion(muestras);
            return nodo;
        }

        // 1) elegir al azar qué características se consideran en este nodo.
        int nCaract = muestras.get(0).caracteristicas().length;
        int[] candidatas = Muestreo.muestraSinReemplazo(nCaract, maxCaracteristicas, rng);

        // 2) de las candidatas, quedarse con la que da mayor ganancia de información.
        double entropiaPadre = Entropia.entropia(muestras, nClases);
        int mejorCaract = -1;
        double mejorUmbral = 0.0;
        double mejorGanancia = Double.NEGATIVE_INFINITY;
        for (int j : candidatas) {
            double umbral = media(muestras, j);                 // corte = media de esa característica
            double ganancia = gananciaInformacion(muestras, j, umbral, entropiaPadre);
            if (ganancia > mejorGanancia) {
                mejorGanancia = ganancia;
                mejorCaract = j;
                mejorUmbral = umbral;
            }
        }

        // 3) partir las muestras por el mejor corte.
        List<Muestra> izq = new ArrayList<>();
        List<Muestra> der = new ArrayList<>();
        repartir(muestras, mejorCaract, mejorUmbral, izq, der);

        // si todas caen de un lado, el corte no separa nada -> hoja con la distribución actual.
        if (izq.isEmpty() || der.isEmpty()) {
            nodo.distribucion = distribucion(muestras);
            return nodo;
        }

        nodo.caracteristica = mejorCaract;
        nodo.umbral = mejorUmbral;
        nodo.izquierda = construir(izq);
        nodo.derecha = construir(der);
        return nodo;
    }

    /**
     * Predice la distribución de clases para un vector de características, bajando por el árbol hasta
     * una hoja (izquierda si {@code valor < umbral}, derecha en caso contrario).
     */
    public double[] predecir(double[] caracteristicas) {
        Nodo nodo = raiz;
        while (nodo.distribucion == null) {     // mientras sea un nodo interno
            nodo = caracteristicas[nodo.caracteristica] < nodo.umbral ? nodo.izquierda : nodo.derecha;
        }
        return nodo.distribucion;
    }

    // ---------------------------------------------------------------------------------------------
    // Ayudantes (todos operan sobre la lista de muestras del nodo actual).
    // ---------------------------------------------------------------------------------------------

    /** Media de la característica j en las muestras (sirve de umbral de corte). */
    private static double media(List<Muestra> muestras, int j) {
        double suma = 0;
        for (Muestra m : muestras) {
            suma += m.caracteristicas()[j];
        }
        return suma / muestras.size();
    }

    /** Reparte las muestras en dos listas según {@code x[j] < umbral}. */
    private static void repartir(List<Muestra> muestras, int j, double umbral,
                                 List<Muestra> izq, List<Muestra> der) {
        for (Muestra m : muestras) {
            if (m.caracteristicas()[j] < umbral) {
                izq.add(m);
            } else {
                der.add(m);
            }
        }
    }

    /**
     * Ganancia de información de un corte: entropía del padre menos la media ponderada de las entropías
     * de los dos hijos. Cuanto mayor, mejor separa el corte las clases.
     */
    private double gananciaInformacion(List<Muestra> muestras, int j, double umbral, double entropiaPadre) {
        List<Muestra> izq = new ArrayList<>();
        List<Muestra> der = new ArrayList<>();
        repartir(muestras, j, umbral, izq, der);
        double n = muestras.size();
        double ePadreMenosHijos = entropiaPadre
                - (izq.size() / n) * Entropia.entropia(izq, nClases)
                - (der.size() / n) * Entropia.entropia(der, nClases);
        return ePadreMenosHijos;
    }

    /** Proporción de cada clase en las muestras (histograma normalizado); es lo que guarda una hoja. */
    private double[] distribucion(List<Muestra> muestras) {
        double[] dist = new double[nClases];
        for (Muestra m : muestras) {
            dist[m.etiqueta()] += 1.0 / muestras.size();
        }
        return dist;
    }

    /** True si todas las muestras tienen la misma etiqueta (el grupo ya es "puro"). */
    private static boolean mismaClase(List<Muestra> muestras) {
        int primera = muestras.get(0).etiqueta();
        for (Muestra m : muestras) {
            if (m.etiqueta() != primera) {
                return false;
            }
        }
        return true;
    }
}
