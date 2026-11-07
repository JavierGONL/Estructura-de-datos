/**
 * Cola (FIFO: el primero que entra es el primero que sale) CIRCULAR, guardada en un DynamicArray.
 * Los elementos viven en array[head], array[head+1], ... dando la vuelta al final (por eso el % capacidad):
 *  - head = posición del siguiente que debe salir;
 *  - tail = posición donde se escribirá el próximo que entre;
 *  - size = cuántos hay en la cola.
 * Siempre se cumple tail == (head + size) % capacidad.
 * Al sacar un elemento su casilla se libera y se reutiliza, así que la memoria depende de cuántos elementos
 * hay a la vez y no de cuántos han pasado por la cola. add y poll cuestan O(1) (O(1) amortizado al crecer).
 * Se usa en el recorrido por niveles del árbol general. El DynamicArray se usa como almacenamiento con sus
 * métodos "físicos" (getPhysical, setPhysical, clearPhysical, ensureCircularCapacity).
 */
public class Queue<T> {
    private DynamicArray<T> array = new DynamicArray<>(8);
    private int head = 0;   // posición del siguiente elemento que debe salir
    private int tail = 0;   // casilla libre donde entrará el próximo elemento
    private int size = 0;   // cuántos elementos hay en la cola

    /**
     * Qué hace: mete un elemento al final de la cola.
     * Cómo: si el arreglo está lleno (size == capacidad) lo agranda al doble copiando los elementos en orden
     * lógico (ensureCircularCapacity), lo que deja head en 0 y tail en size. Después escribe el elemento en
     * tail, avanza tail una posición dando la vuelta (% capacidad) y suma 1 a size. O(1) amortizado.
     */
    public void add(T element) {
        if (size == array.capacity()) {
            array.ensureCircularCapacity(head, size);
            head = 0;
            tail = size;
        }

        array.setPhysical(tail, element);
        tail = (tail + 1) % array.capacity();
        size++;
    }

    /**
     * Qué hace: saca y devuelve el elemento que lleva más tiempo en la cola (null si está vacía).
     * Cómo: lee la casilla head, la libera (null), avanza head una posición dando la vuelta y resta 1 a size.
     * Si la cola queda vacía, head y tail vuelven a 0 para empezar limpio. O(1).
     */
    public T poll() {
        if (isEmpty()) return null;

        T element = array.getPhysical(head);
        array.clearPhysical(head);
        head = (head + 1) % array.capacity();
        size--;
        if (size == 0) {
            head = 0;
            tail = 0;
        }
        return element;
    }

    /** Qué hace: dice si no queda nadie por salir. Cómo: compara size con 0. */
    public boolean isEmpty() {
        return size == 0;
    }
}
