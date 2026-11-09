package ecg.ml.estructuras;
/**
 * DynamicArray<T>: lista que crece sola, hecha a mano con un arreglo normal.
 * len = cuántos elementos hay de verdad; capacity = tamaño real del arreglo interno.
 * Cuando se llena, se crea un arreglo del doble de tamaño y se copian los elementos.
 * Implementa Iterable para poder usar for (T x : lista).
 */

@SuppressWarnings("unchecked")
public class DynamicArray<T> implements Iterable<T> {
    private T[] array;
    private int len = 0;        // cantidad de elementos que el usuario ve
    private int capacity = 0;   // tamaño real del arreglo interno

    /**
     * Qué hace: crea la lista con una capacidad inicial.
     * Cómo: valida que no sea negativa y reserva un arreglo de ese tamaño.
     */
    public DynamicArray(int capacity) {
        if (capacity < 0) throw new IllegalArgumentException("Illegal Capacity: " + capacity);
        this.capacity = capacity;
        array = (T[]) new Object[capacity];
    }

    /*, 
     * Qué hace: agrega un elemento al final.
     * Cómo: si el arreglo está lleno (len == capacity) lo duplica (si la capacidad era 0 pasa a 1),
     * copia los elementos al arreglo nuevo y luego escribe el elemento en array[len] y suma 1 a len.
     * Costo: O(1) casi siempre; O(n) solo cuando hay que duplicar (O(1) amortizado).
     */
    public void add(T element) {
        if (len == capacity) {
            capacity = capacity == 0 ? 1 : capacity * 2;
            T[] new_arr = (T[]) new Object[capacity];
            for (int i = 0; i < len; i++) new_arr[i] = array[i];
            array = new_arr;
        }
        array[len++] = element;
    }

    /**
     * Qué hace: saca y devuelve el primer elemento (null si la lista está vacía).
     * Cómo: guarda array[0] y llama a removeIdx(0), que corre todo a la izquierda: O(n). (Queue no la usa: tiene su propia
     * forma de sacar en O(1).)
     */
    public T eliminarPrimero() {
        if (len == 0) return null;
        T first = array[0];
        removeIdx(0);
        return first;
    }

    // ---- Métodos de acceso "físico", solo para Queue (visibles en el paquete). ----
    // Tratan el arreglo interno como un buffer circular: no usan ni actualizan len, así que
    // en ese uso get(), length(), iterator() y toString() NO sirven (la lista "ve" 0 elementos).

    /** Qué hace: devuelve el tamaño real del arreglo interno. Cómo: devuelve capacity. */
    int capacity() {
        return capacity;
    }

    /**
     * Qué hace: si el buffer circular está lleno, lo agranda al doble sin perder el orden de la cola.
     * Cómo: si size < capacity no hace nada. Si no, crea un arreglo del doble y copia los size elementos en orden
     * lógico: el que sale primero (posición head) pasa a la posición 0, el siguiente a la 1, etc., leyendo con
     * (head + i) % capacity para respetar la vuelta. Queue pone después head en 0 y tail en size. O(n).
     */
    void ensureCircularCapacity(int head, int size) {
        if (size < capacity) return;

        int newCapacity = capacity == 0 ? 1 : capacity * 2;
        T[] newArray = (T[]) new Object[newCapacity];
        for (int i = 0; i < size; i++) {
            newArray[i] = array[(head + i) % capacity];
        }
        array = newArray;
        capacity = newCapacity;
    }

    /** Qué hace: lee la casilla index del arreglo interno, sin validar contra len. Cómo: devuelve array[index]. */
    T getPhysical(int index) {
        return array[index];
    }

    /** Qué hace: escribe element en la casilla index del arreglo interno. Cómo: asigna array[index]. */
    void setPhysical(int index, T element) {
        array[index] = element;
    }

    /** Qué hace: libera la casilla index. Cómo: pone null para soltar la referencia y poder reutilizarla. */
    void clearPhysical(int index) {
        array[index] = null;
    }

    /**
     * Qué hace: devuelve el elemento de la posición index (empezando en 0).
     * Cómo: si el índice no está entre 0 y len-1 lanza una excepción; si no, lee array[index]. O(1).
     */
    public T get(int index) {
        if (index < 0 || index >= len) throw new IndexOutOfBoundsException("Index: " + index);
        return array[index];
    }

    /**
     * Qué hace: borra TODAS las apariciones de element.
     * Cómo: recorre el arreglo; al encontrar una (con equals) corre a la izquierda todo lo que está
     * después, deja el último lugar en null con array[--len] y hace i-- para volver a revisar
     * la misma posición (por si el elemento que llegó ahí también coincide). O(n) por cada borrado.
     */
    public void remove(T element) {
        if (element == null || len == 0) return;
        for (int i = 0; i < len; i++) {
            if (array[i].equals(element)) {
                for (int j = i; j < len - 1; j++) {
                    array[j] = array[j + 1];
                }
                array[--len] = null;   // el último elemento real estaba en len-1: se resta ANTES de usar el índice
                i--;                   // para eliminar bien todas las coincidencias
            }
        }
    }

    /**
     * Qué hace: borra el elemento de la posición index.
     * Cómo: si el índice no es válido no hace nada; si lo es, corre a la izquierda lo que está
     * después y deja en null el último lugar con array[--len]. O(n).
     */
    public void removeIdx(int index) {
        if (index < 0 || index >= len || len == 0) return;
        for (int i = index; i < len - 1; i++) {
            array[i] = array[i + 1];
        }
        array[--len] = null;
    }

    /**
     * Qué hace: dice si element está en la lista.
     * Cómo: búsqueda lineal comparando con equals en las posiciones 0..len-1. O(n).
     */
    public boolean search(T element) {
        if (element == null || len == 0) return false;

        for (int i = 0; i < len; i++) {
            if (array[i].equals(element)) return true;
        }

        return false;
    }

    /** Qué hace: dice si la lista está vacía. Cómo: compara len con 0. */
    public boolean isEmpty() {
        return len == 0;
    }

    /**
     * Qué hace: vacía la lista (la capacidad se conserva).
     * Cómo: pone null en las posiciones usadas (para soltar las referencias) y deja len en 0.
     */
    public void reset() {
        for (int i = 0; i < len; i++) {
            array[i] = null;
        }
        len = 0;
    }

    /** Qué hace: devuelve cuántos elementos hay. Cómo: devuelve len. */
    public int length() {
        return len;
    }

    /**
     * Qué hace: reemplaza el elemento de la posición index.
     * Cómo: solo escribe si index está entre 0 y len-1 (una posición que la lista "ve"); si no, no hace nada.
     */
    public void set(int index, T element) {
        if (index < 0 || index >= len) return;

        array[index] = element;
    }

    /**
     * Qué hace: permite recorrer la lista con for-each.
     * Cómo: devuelve un iterador que lleva un índice: hasNext() es "index < len" y next() devuelve
     * array[index] y avanza. (Es la única clase de java.util que se usa, y lo exige Iterable.)
     */
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
                return array[index++];
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Qué hace: texto de la lista, por ejemplo [10, 20, 30]; vacía es [].
     * Cómo: un StringBuilder; escribe cada elemento con ", " entre ellos.
     */
    @Override
    public String toString() {
        if (len == 0) return "[]";
        else {
            StringBuilder sb = new StringBuilder(len).append("[");
            for (int i = 0; i < len - 1; i++) sb.append(array[i] + ", ");
            return sb.append(array[len - 1] + "]").toString();
        }
    }
}
