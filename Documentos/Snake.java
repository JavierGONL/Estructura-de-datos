import java.util.NoSuchElementException;

/**
 * Serpiente sobre un buffer circular.
 * Indice logico 0 = cola, size-1 = cabeza.
 * Indice fisico de i:  (start + i) mod capacity.
 */
public class Snake {
    // Invariantes:
    // S1. capacity >= 2 y 0 <= size <= capacity
    // S2. 0 <= start < capacity
    // S3. para 0 <= i < size: data[(start + i) % capacity] es p_i (cola -> cabeza)
    // S4. toda celda fisica que no es de la forma (start + i) % capacity, i < size, es null
    // S5. capacity solo cambia en resize(), que exige size == capacity y la duplica
    private Position[] data;
    private int start;    // indice fisico de la cola
    private int size;     // longitud actual (incluye el estado transitorio k+1)
    private int capacity; // == data.length

    public Snake(Position tail, Position head) {
        this.data = new Position[2];
        this.data[0] = tail;
        this.data[1] = head;
        this.start = 0;
        this.size = 2;
        this.capacity = 2;
    }

    public int size() { 
        return size; 
    }

    public int capacity() { 
        return capacity; 
    }

    public int start() { 
        return start; 
    }

    /** Solo para la traza: contenido crudo de la celda fisica i. */
    public Position physicalAt(int i) { 
        return data[i]; 
    }

    public Position get(int logicalIndex) { // O(1)
        if (logicalIndex < 0 || logicalIndex >= size) {
            throw new IndexOutOfBoundsException("Indice logico fuera de rango: " + logicalIndex);
        }
        return data[(start + logicalIndex) % capacity];
    }

    public void addHead(Position p) { // O(1) amortizado, O(size) worst-case
        if (size == capacity) {
            resize(2 * capacity);
        }
        data[(start + size) % capacity] = p; // primera celda libre, justo despues de la cabeza
        size++;
    }

    public Position removeTail() { // O(1) worst-case
        if (size == 0) {
            throw new NoSuchElementException("La serpiente esta vacia");
        }
        Position tail = data[start];
        data[start] = null;
        start = (start + 1) % capacity;
        size--;
        return tail;
    }

    private void resize(int newCapacity) { // Theta(size)
        Position[] newData = new Position[newCapacity];
        for (int i = 0; i < size; i++) {
            newData[i] = data[(start + i) % capacity]; // se lee en orden logico
        }
        data = newData;
        start = 0; // la cola pasa a la celda fisica 0
        capacity = newCapacity;
    }
}