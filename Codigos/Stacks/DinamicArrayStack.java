public class DinamicArrayStack {
    // una implementacion de stack usando dynamic arrays (stack es una cola lifo)
    String[] stackArray = new String[2];
    int wR = 0; // write and read

    public void push(String key) {
        if (wR == stackArray.length) {
            String[] newArray = new String[stackArray.length * 2];
            for (int idx = 0; idx < stackArray.length; idx++) {
                newArray[idx] = stackArray[idx];
            }
            
            stackArray = newArray;
        }
        
        stackArray[wR] = key;
        wR++;
    }

    public String pop() {
        if (wR == 0) {
            throw new IllegalStateException("La cola está vacía");
        }

        wR--;
        String value = stackArray[wR];
        stackArray[wR] = null;
        return value;
    }

    public boolean isEmpty() {
        return wR == 0;
    }

    public boolean search(String key) {
        for (int idx = 0; idx < stackArray.length; idx++) {
            if (stackArray[idx] == key) {
                return true;
            }
        }

        return false;
    }
}
