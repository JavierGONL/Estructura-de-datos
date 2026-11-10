package ecg.ml;

import java.util.Random;

/**
 * Las dos fuentes de azar de un Random Forest:
 * <ul>
 *   <li>{@link #bootstrap(int, Random)}: elige CON repetición las filas con que se entrena cada árbol;
 *   <li>{@link #muestraSinReemplazo(int, int, Random)}: elige SIN repetición qué características se
 *       miran en un nodo.
 * </ul>
 *
 * <p>POR QUÉ: que cada árbol vea datos y preguntas distintas hace que sus errores sean independientes;
 * al votar entre muchos árboles, esos errores se compensan y el bosque acierta más que un solo árbol.
 */
public final class Muestreo {

    private Muestreo() {
    }

    /**
     * Bootstrap: {@code n} índices en [0, n) elegidos al azar CON repetición. Algunas filas saldrán
     * varias veces y otras ninguna, así cada árbol entrena con una variación del dataset.
     */
    public static int[] bootstrap(int n, Random rng) {
        int[] indices = new int[n];
        for (int i = 0; i < n; i++) {
            indices[i] = rng.nextInt(n);
        }
        return indices;
    }

    /**
     * {@code k} índices DISTINTOS en [0, total) mediante un barajado parcial de Fisher-Yates: se
     * intercambian las primeras k posiciones con otra posición aleatoria y se devuelven esas k.
     * Si {@code k >= total} se devuelven todas las características.
     */
    public static int[] muestraSinReemplazo(int total, int k, Random rng) {
        k = Math.min(k, total);
        int[] a = new int[total];
        for (int i = 0; i < total; i++) {
            a[i] = i;
        }
        for (int i = 0; i < k; i++) {
            int j = i + rng.nextInt(total - i);
            int tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
        int[] seleccion = new int[k];
        System.arraycopy(a, 0, seleccion, 0, k);
        return seleccion;
    }
}
