package ecg.ml.estructuras;

import java.util.Iterator;

/**
 * Cola circular de capacidad fija. Equivalente de la clase CircularQueue de
 * estructuras/cola_circular.py.
 *
 * <p>Uso en el proyecto: ventana deslizante de los últimos k intervalos RR, tanto al construir el
 * dataset como al predecir (mismo código en las dos fases).
 *
 * @param <T> tipo de los elementos
 */
public class CircularQueue<T> implements Iterable<T> {

    private final Object[] data;
    private int head;       // índice del elemento más antiguo
    private int size;

    public CircularQueue(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("la capacidad debe ser >= 1");
        }
        this.data = new Object[capacity];
        this.head = 0;
        this.size = 0;
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return data.length;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean isFull() {
        return size == data.length;
    }

    /** Encola al final. O(1). Lanza si está llena. */
    public void enqueue(T x) {
        if (isFull()) {
            throw new IllegalStateException("cola llena");
        }
        data[(head + size) % data.length] = x;
        size++;
    }

    /** Saca y devuelve el más antiguo. O(1). */
    @SuppressWarnings("unchecked")
    public T dequeue() {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException("cola vacía");
        }
        T x = (T) data[head];
        data[head] = null;
        head = (head + 1) % data.length;
        size--;
        return x;
    }

    /** Encola; si está llena descarta el más antiguo y lo devuelve (si no, devuelve null). */
    public T enqueueOverwrite(T x) {
        T evicted = isFull() ? dequeue() : null;
        enqueue(x);
        return evicted;
    }

    @SuppressWarnings("unchecked")
    public T peek() {
        if (isEmpty()) {
            throw new IndexOutOfBoundsException("cola vacía");
        }
        return (T) data[head];
    }

    /** Elemento i contando desde el más antiguo (0) hasta el más reciente (size-1). */
    @SuppressWarnings("unchecked")
    public T get(int i) {
        if (i < 0 || i >= size) {
            throw new IndexOutOfBoundsException("índice " + i + " fuera de rango [0, " + size + ")");
        }
        return (T) data[(head + i) % data.length];
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < size;
            }

            @Override
            public T next() {
                return get(i++);
            }
        };
    }
}
