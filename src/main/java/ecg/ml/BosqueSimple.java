package ecg.ml;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Random Forest SIMPLE.
 *
 * <p>QUÉ hace: entrena muchos {@link ArbolDesicion} y, para clasificar un latido, promedia las
 * predicciones de todos (voto "blando" por probabilidades). La clase final es la de mayor probabilidad
 * media.
 *
 * <p>CÓMO: cada árbol se entrena con un bootstrap distinto de los datos (filas al azar con repetición)
 * y, en cada nodo, mira solo un subconjunto aleatorio de características. Esa doble aleatoriedad hace a
 * los árboles diferentes entre sí.
 *
 * <p>POR QUÉ promediar: un solo árbol tiende a sobreajustar (memoriza el entrenamiento). Al promediar
 * muchos árboles poco correlacionados, el bosque generaliza mejor y es más estable.
 */
public class BosqueSimple {

    private final int nArboles;
    private final int nClases;
    private final int maxCaracteristicas;
    private final int minMuestrasHoja;
    private final List<ArbolDesicion> arboles = new ArrayList<>();
    private final Random rng;

    /**
     * @param nArboles          cuántos árboles tendrá el bosque
     * @param nClases           número de clases (4 para ECG: N, S, V, F)
     * @param maxCaracteristicas características consideradas por nodo (típico: raíz del total)
     * @param minMuestrasHoja   tamaño mínimo de un grupo para seguir dividiéndolo
     * @param semilla           semilla del azar (misma semilla -> mismo bosque)
     */
    public BosqueSimple(int nArboles, int nClases, int maxCaracteristicas, int minMuestrasHoja, long semilla) {
        this.nArboles = nArboles;
        this.nClases = nClases;
        this.maxCaracteristicas = maxCaracteristicas;
        this.minMuestrasHoja = minMuestrasHoja;
        this.rng = new Random(semilla);
    }

    /** Entrena el bosque: por cada árbol, saca un bootstrap de las muestras y lo construye con él. */
    public void entrenar(List<Muestra> muestras) {
        for (int t = 0; t < nArboles; t++) {
            int[] indices = Muestreo.bootstrap(muestras.size(), rng);
            List<Muestra> bootstrap = new ArrayList<>(indices.length);
            for (int idx : indices) {
                bootstrap.add(muestras.get(idx));
            }
            ArbolDesicion arbol = new ArbolDesicion(nClases, maxCaracteristicas, minMuestrasHoja, rng);
            arbol.entrenar(bootstrap);
            arboles.add(arbol);
        }
    }

    /** Promedio de las distribuciones de todos los árboles para un latido (voto blando). */
    public double[] predecirDistribucion(double[] caracteristicas) {
        double[] acumulado = new double[nClases];
        for (ArbolDesicion arbol : arboles) {
            double[] dist = arbol.predecir(caracteristicas);
            for (int c = 0; c < nClases; c++) {
                acumulado[c] += dist[c];
            }
        }
        for (int c = 0; c < nClases; c++) {
            acumulado[c] /= arboles.size();
        }
        return acumulado;
    }

    /** Clase predicha: la de mayor probabilidad media (empate -> la de menor índice). */
    public int predecir(double[] caracteristicas) {
        double[] dist = predecirDistribucion(caracteristicas);
        int mejor = 0;
        for (int c = 1; c < dist.length; c++) {
            if (dist[c] > dist[mejor]) {
                mejor = c;
            }
        }
        return mejor;
    }
}
