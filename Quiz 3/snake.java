import java.util.Scanner;

public class snake { // hacer un array circular, elimino el invariante de data[0] = Tail
    private Position[] data; // quiero guardar en data la pos del segmento de la serpiente, osea tengo que guardar otro array?
    private int size;
    private int capacity;
    private int posTail;
    private int posHead;
    public String[][] cuadricula;
    public int cuadriculaSize;

    class Position {
    int x;
    int y;

    Position(int x, int y) {
        this.x = x;
        this.y = y;
    }
}

    public snake(int cuadriculaSize) {
        this.data = new Position[2];
        this.capacity = 2;
        this.size = 2;
        this.posTail = 0;
        data[0] =  new Position(cuadriculaSize/2, cuadriculaSize/2);
        this.posHead = 1;
        data[1] =  new Position(cuadriculaSize/2, cuadriculaSize/2 + 1);
        this.cuadricula = new String[cuadriculaSize][cuadriculaSize];
        this.cuadriculaSize = cuadriculaSize;
    }

    public int size() {
        return size;
    }

    private int capacity() {
        return capacity;
    }

    public Position get(int index) { // complejidad O(1), aca como convencion es position desde el tail, dado que el tail es como la pos inicial de la serpiente
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("indice fuera del array");
        }
        return data[(posTail + index) % capacity];
    }

    public void set(int index, Position value) { // complejidad O(1)
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("indice fuera del array");
        }
        data[(posTail + index) % capacity] = value;
    }

    public void addHead(Position value) { // complejidad O(1) amortizada
        if (size == capacity) {
            resize(capacity * 2);
        }

        if (size == 0) {
            data[posTail] = value;
            posHead = posTail;
        } 
        else {
            posHead = (posHead + 1) % capacity;
            data[posHead] = value;
        }

        size++;
    }

    public void removeTail() {
        if (size == 0) {
            throw new IllegalArgumentException("La capacidad no puede ser menor o igual a cero");
        }

        data[posTail] = null; // esto es para limpiar memoria
        posTail = (posTail + 1) % capacity;
        size--;

        if (size == 0) { // manejar el caso vacio
            posHead = posTail;
        }
    }

    private void resize(int newCapacity) { // al hacer el resize debo copiar el orden de la snake como estaba
        Position[] temporalArray = new Position[newCapacity];
        for (int i = 0; i < size; i++) {
            temporalArray[i] = data[(posTail + i) % capacity];
        }

        data = temporalArray;
        posTail = 0;
        posHead = size - 1;
        capacity = newCapacity;
    }

    public void actualizarCuadricula() {
        for (int i = 0; i < cuadriculaSize; i++) { // primero limpio la cuadricula
            for (int j = 0; j < cuadriculaSize; j++) {
                cuadricula[i][j] = null;
            }
        }

        for (int i = 0; i < size; i++) { // pinto las serpiente
            Position segmento = data[(posTail + i) % capacity];
            if (cuadricula[segmento.x][segmento.y] != null) {  
                throw new IllegalArgumentException("Colision");
            }
             
            cuadricula[segmento.x][segmento.y] = "#";
        }
    }

    public Position LeerInput() {
        System.out.print("Movimiento (W/A/S/D): ");
        char c = Character.toLowerCase(new Scanner(System.in).next().charAt(0));

        if (c == 'a') {
            return new Position(-1, 0);
        } 
        else if (c == 'd') {
            return new Position(1, 0);
        } 
        else if (c == 'w') {
            return new Position(0, -1);
        } 
        else if (c == 's') {
            return new Position(0, 1);
        } 
        else {
            return new Position(0, 0);
        }
    }

    public void actualizarPosiciones(Position input) {
        Position newHead = new Position(data[posHead].x + input.x, data[posHead].y + input.y); // actualizo la cabeza al input
        addHead(newHead);
        removeTail();
    }

    public static void main(String[] args) {
        snake s = new snake(10);
        Scanner sc = new Scanner(System.in);

        for (int i = 0; i < 10; i++) {
            System.out.println("Turno " + (i + 1));
            s.actualizarPosiciones(s.LeerInput());
            s.actualizarCuadricula();

            for (int fila = 0; fila < s.cuadriculaSize; fila++) {
                for (int col = 0; col < s.cuadriculaSize; col++) {
                    System.out.print(s.cuadricula[fila][col] == null ? ". " : s.cuadricula[fila][col] + " ");
                }
                System.out.println();
            }
            System.out.println();
        }

        sc.close();
    }
}