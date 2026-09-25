public class snake<T> { // hacer un array circular, elimino el invariante de data[0] = Tail
    private T[] data;
    private int size;
    private int capacity;
    private int posTail;
    private int posHead;

    public snake() {
        this.data = (T[]) new Object[2];
        this.capacity = 2;
        this.size = 0;
        this.posHead = 0;
        this.posTail = 0;
    }

    public int size() {
        return size;
    }

    private int capacity() {
        return capacity;
    }

    public int longSnake() {
        return posHead - posTail;
    }

    public T get(int index) { // complejidad O(1)
        if (index < size && index >= 0) {
            return data[index];
        } else {
            throw new IndexOutOfBoundsException("indice fuera del array");
        }
    }

    public void set(int index, T value) { // complejidad O(1)
        if (index < size && index >= 0) {
            data[index] = value;
        } else {
            throw new IndexOutOfBoundsException("indice fuera del array");
        }
    }

    public void addHead(T value) { // complejidad O(n), porque al crear un nuevo array, debo asignar 1 por 1 los valores del anterior array al sigui
        if (size == capacity) {
            resize(capacity * 2);
        }
        data[++posHead] = value;
        size++;
    }

    public T removeTail() {
        if (size == 0) {
            throw new IllegalArgumentException("La capacidad no puede ser menor o igual a cero");
        }

        T datoDevolver = data[posTail];
        data[posTail++] = null;
        size--;
        return datoDevolver;
    }

    private void resize(int newCapacity) { // al hacer el resize debo
        T[] temporalArray = (T[]) new Object[newCapacity];
        for (int i = 0; i < size; i++) {
            temporalArray[i] = data[posTail + i];
        }

        this.posHead = longSnake();
        this.posTail = 0;

        data = null;
        data = temporalArray;
        this.capacity = newCapacity;
    }
}