
package estructuras;

@SuppressWarnings("unchecked")
public class DynamicArray_1<T> implements Iterable<T> {
    private T[] array;
    private int len = 0; // length user thinks array is
    private int capacity = 0; // Actual array size


    public DynamicArray_1(int capacity) {
        if (capacity < 0) throw new IllegalArgumentException("Illegal Capacity: " + capacity);
        this.capacity = capacity;
        array = (T[]) new Object[capacity];
    }

    public void add(T element) {
        if (len == capacity - 1) {
            capacity = capacity == 0 ? 1 : capacity*2;
            T[] new_arr = (T[]) new Object[capacity];
            for (int i = 0; i < len; i++) new_arr[i] = array[i];
            array = new_arr;
        }
        array[len++] = element;
    }

    public void remove(T element) {
        if (element == null || len == 0) return;
        for (int i = 0; i < len; i++) {
            if (array[i].equals(element)) {
                for (int j = i; j < len - 1; j++) {
                    array[j] = array[j+1];
                }
                array[len--] = null; // debo actualizar el ultimo elemtento como null
                i--; // esto para eliminar bien todas las coincidencias
            }
        }        
    }

    public void removeIdx(int index) {
        if (index < 0 || index >= len || len == 0) return;
        for (int i = index; i < len - 1; i++) {
            array[i] = array[i + 1];
        }
        array[len--] = null; // debo actualizar el ultimo elemtento como null
    }

    public boolean search(T element) {
        if (element == null || len == 0) return false;
        
        for (int i = 0; i < len; i++) {
            if (array[i].equals(element)) return true;
        }

        return false;
    }

    public boolean isEmpty() {
        return len == 0;
    }

    public void reset() {
        for (int i = 0; i < len; i++) {
            array[i] = null;
        }
        len = 0;
    }

    public int length() {
        return len;
    }

    public void set(int index, T element) {
        if ((index < 0 || index > len) || len == 0) return;

        array[index] = element;
    }
    
  // Iterator is still fast but not as fast as iterative for loop
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