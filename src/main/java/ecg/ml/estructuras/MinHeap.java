package ecg.ml.estructuras;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Min-heap binario sobre un arreglo dinámico. Equivalente de la clase MinHeap de estructuras/heap.py.
 *
 * <p>Heap de mínimo según {@code key(item)}. A igual clave sale primero el que entró antes (FIFO),
 * usando un número de secuencia como desempate. Uso en el proyecto: reparto de registros en grupos
 * equilibrados (validación) y selección de los latidos más sospechosos.
 *
 * @param <T> tipo de los elementos
 * @param <K> tipo de la clave (comparable)
 */
public class MinHeap<T, K> {

    /** Entrada interna (clave, nº de inserción, item). */
    private final class Entry {
        final K clave;
        final long seq;
        final T item;

        Entry(K clave, long seq, T item) {
            this.clave = clave;
            this.seq = seq;
            this.item = item;
        }
    }

    private final DynamicArray<Entry> a = new DynamicArray<>(8);
    private final Function<T, K> key;
    private final Comparator<K> comparador;
    private long seqCounter;

    public MinHeap(Function<T, K> key, Comparator<K> comparador) {
        this.key = key;
        this.comparador = comparador;
    }

    /** Heap sobre claves que ya son {@link Comparable}. */
    public static <T, K extends Comparable<K>> MinHeap<T, K> of(Function<T, K> key) {
        return new MinHeap<>(key, Comparator.naturalOrder());
    }

    public int size() {
        return a.length();
    }

    public boolean isEmpty() {
        return a.isEmpty();
    }

    public T peek() {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException("heap vacío");
        }
        return a.get(0).item;
    }

    public K peekKey() {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException("heap vacío");
        }
        return a.get(0).clave;
    }

    /** O(log n). */
    public void push(T item) {
        a.add(new Entry(key.apply(item), seqCounter++, item));
        siftUp(a.length() - 1);
    }

    /** Extrae el mínimo. O(log n). */
    public T pop() {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException("heap vacío");
        }
        Entry top = a.get(0);
        int ultimo = a.length() - 1;
        Entry last = a.get(ultimo);
        a.removeIdx(ultimo);               // quita el último (emula pop() del arreglo)
        if (!isEmpty()) {
            a.set(0, last);
            siftDown(0);
        }
        return top.item;
    }

    /** Extrae el mínimo e inserta item en una sola pasada (más barato que pop + push). */
    public T replace(T item) {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException("heap vacío");
        }
        Entry top = a.get(0);
        a.set(0, new Entry(key.apply(item), seqCounter++, item));
        siftDown(0);
        return top.item;
    }

    // ---- internos ----
    private boolean less(int i, int j) {
        Entry ei = a.get(i);
        Entry ej = a.get(j);
        int c = comparador.compare(ei.clave, ej.clave);
        return c < 0 || (c == 0 && ei.seq < ej.seq);
    }

    private void swap(int i, int j) {
        Entry t = a.get(i);
        a.set(i, a.get(j));
        a.set(j, t);
    }

    private void siftUp(int i) {
        while (i > 0) {
            int padre = (i - 1) / 2;
            if (less(i, padre)) {
                swap(i, padre);
                i = padre;
            } else {
                break;
            }
        }
    }

    private void siftDown(int i) {
        int n = a.length();
        while (true) {
            int izq = 2 * i + 1;
            int der = 2 * i + 2;
            int menor = i;
            if (izq < n && less(izq, menor)) {
                menor = izq;
            }
            if (der < n && less(der, menor)) {
                menor = der;
            }
            if (menor == i) {
                break;
            }
            swap(i, menor);
            i = menor;
        }
    }

    /** Clave interna (clave externa, -posición de llegada) para romper empates por orden de llegada. */
    private record ClaveTopK<K extends Comparable<K>>(K k, long negPos) implements Comparable<ClaveTopK<K>> {
        @Override
        public int compareTo(ClaveTopK<K> o) {
            int c = k.compareTo(o.k);
            return c != 0 ? c : Long.compare(negPos, o.negPos);
        }
    }

    /** Par (clave interna, item) que viaja por el heap durante top-k. */
    private record ParTopK<T, K extends Comparable<K>>(ClaveTopK<K> clave, T item) {
    }

    /**
     * Los k elementos de mayor clave, de mayor a menor. O(n log k) y memoria O(k).
     * Equivalente de la función top_k de heap.py: mantiene un min-heap de tamaño k, de modo que su
     * raíz es el menor de los k mejores vistos; un elemento nuevo solo entra si supera a esa raíz.
     * En empate de clave se conservan (y salen primero) los que llegaron antes.
     */
    public static <T, K extends Comparable<K>> List<T> topK(List<T> items, int k, Function<T, K> key) {
        if (k <= 0) {
            return new ArrayList<>();
        }
        MinHeap<ParTopK<T, K>, ClaveTopK<K>> heap = MinHeap.of(ParTopK::clave);
        for (int pos = 0; pos < items.size(); pos++) {
            T x = items.get(pos);
            ClaveTopK<K> clave = new ClaveTopK<>(key.apply(x), -(long) pos);
            ParTopK<T, K> par = new ParTopK<>(clave, x);
            if (heap.size() < k) {
                heap.push(par);
            } else if (clave.compareTo(heap.peekKey()) > 0) {
                heap.replace(par);
            }
        }
        List<T> asc = new ArrayList<>();
        while (!heap.isEmpty()) {
            asc.add(heap.pop().item());
        }
        List<T> out = new ArrayList<>(asc.size());
        for (int i = asc.size() - 1; i >= 0; i--) {
            out.add(asc.get(i));
        }
        return out;
    }
}
