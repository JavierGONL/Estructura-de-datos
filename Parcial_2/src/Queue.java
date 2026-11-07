public class Queue<T> {
    private DynamicArray<T> array = new DynamicArray<>(8);
    private int head = 0;
    private int tail = 0;
    private int size = 0;

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

    public boolean isEmpty() { 
        return size == 0;
    }
}
