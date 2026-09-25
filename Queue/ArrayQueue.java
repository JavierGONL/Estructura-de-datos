public class ArrayQueue {
    // una implementacion de queue usando static arrays (queue es una cola fifo) y una lectura circular
    String[] queue = new String[5];
    int write = 0;
    int read = 0;

    public void enqueue(String key) {
        if (queue[write] != null) {
            throw new IllegalStateException("La cola está llena");
        }

        queue[write] = key;
        write = (write + 1) % queue.length; // write = 0 -> 1 -> 2 -> 3 -> 4 -> 0
    }

    public String dequeue() {
        if (queue[read] == null) {
            throw new IllegalStateException("La cola está vacía");
        }

        String value = queue[read];
        queue[read] = null;
        read = (read + 1) % queue.length;
        return value;
    }

    public boolean isEmpty() {
        return queue[read] == null;
    }
}