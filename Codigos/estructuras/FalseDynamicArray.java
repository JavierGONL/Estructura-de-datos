package estructuras;

import java.util.NoSuchElementException;

@SuppressWarnings("unchecked")
public class FalseDynamicArray<T> implements Iterable<T> {

    // Directorio de bloques: cada bloque es un array fijo que NUNCA se copia.
    private T[][] chunks;
    private int[] chunkStart; // indice global donde empieza cada bloque
    private int numChunks = 0;
    private int chunksCap;    // capacidad del directorio (cuantos bloques caben)

    private int len = 0;      // cantidad de elementos que el usuario cree que hay
    private int capacity = 0; // capacidad total = suma de tamanios de los bloques

    public FalseDynamicArray(int capacity) {
        if (capacity < 0) throw new IllegalArgumentException("Illegal Capacity: " + capacity);

        chunksCap = 4;
        chunks = (T[][]) new Object[chunksCap][];
        chunkStart = new int[chunksCap];

        if (capacity > 0) addChunk(capacity);
    }

    // Crea un bloque nuevo de tamanio 'size' y lo agrega al directorio.
    // No copia ningun elemento existente, solo apila un array nuevo.
    private void addChunk(int size) {
        if (numChunks == chunksCap) growDirectory();
        chunkStart[numChunks] = capacity; // arranca donde termina la capacidad actual
        chunks[numChunks] = (T[]) new Object[size];
        numChunks++;
        capacity += size;
    }

    // Duplica el directorio de bloques. Esto si copia, pero solo copia
    // punteros (uno por bloque), nunca los elementos -> barato, O(log n) veces.
    private void growDirectory() {
        chunksCap *= 2;
        T[][] newChunks = (T[][]) new Object[chunksCap][];
        int[] newStart = new int[chunksCap];
        System.arraycopy(chunks, 0, newChunks, 0, numChunks);
        System.arraycopy(chunkStart, 0, newStart, 0, numChunks);
        chunks = newChunks;
        chunkStart = newStart;
    }

    // indice global -> bloque que lo contiene (busqueda binaria sobre chunkStart)
    private int chunkOf(int index) {
        int lo = 0, hi = numChunks - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (chunkStart[mid] <= index) lo = mid; else hi = mid - 1;
        }
        return lo;
    }

    private T getAt(int index) {
        int c = chunkOf(index);
        return chunks[c][index - chunkStart[c]];
    }

    private void setAt(int index, T value) {
        int c = chunkOf(index);
        chunks[c][index - chunkStart[c]] = value;
    }

    public void add(T element) {
        if (len == capacity) {
            // bloque nuevo del mismo tamanio que la capacidad actual:
            // duplica la capacidad total sin copiar nada existente
            addChunk(capacity == 0 ? 1 : capacity);
        }
        setAt(len, element);
        len++;
    }

    public void remove(T element) {
        if (element == null || len == 0) return;
        for (int i = 0; i < len; i++) {
            if (getAt(i).equals(element)) {
                for (int j = i; j < len - 1; j++) {
                    setAt(j, getAt(j + 1));
                }
                setAt(len - 1, null); // libera el ultimo elemento
                len--;
                i--; // para eliminar bien todas las coincidencias
            }
        }
    }

    public void removeIdx(int index) {
        if (index < 0 || index >= len || len == 0) return;
        for (int i = index; i < len - 1; i++) {
            setAt(i, getAt(i + 1));
        }
        setAt(len - 1, null);
        len--;
    }

    public boolean search(T element) {
        if (element == null || len == 0) return false;
        for (int i = 0; i < len; i++) {
            if (getAt(i).equals(element)) return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return len == 0;
    }

    public void reset() {
        for (int i = 0; i < len; i++) setAt(i, null);
        len = 0;
    }

    public int length() {
        return len;
    }

    public void set(int index, T element) {
        if (index < 0 || index >= len) return;
        setAt(index, element);
    }

    @Override
    public java.util.Iterator<T> iterator() {
        return new java.util.Iterator<T>() {
            int index = 0;

            @Override
            public boolean hasNext() {
                return index < len;
            }

            @Override
            public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                return getAt(index++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public String toString() {
        if (len == 0) return "[]";
        StringBuilder sb = new StringBuilder().append("[");
        for (int i = 0; i < len - 1; i++) sb.append(getAt(i)).append(", ");
        return sb.append(getAt(len - 1)).append("]").toString();
    }
}