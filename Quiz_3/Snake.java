import java.util.Scanner;
import java.util.Random;

public class Snake { // hacer un array circular, elimino el invariante de data[0] = Tail
    private final Scanner scanner = new Scanner(System.in);
    private final Random random = new Random();
    private Position[] data; // quiero guardar en data la pos de los segmentos de la serpiente
    private int size;
    private int capacity;
    private int posTail;
    private int posHead;
    public String[][] cuadricula;
    public int cuadriculaSize;
    public Position[] fruits;

    public Snake(int cuadriculaSize, int amountfruits) {
        this.data = new Position[2];
        this.capacity = 2;
        this.size = 2;
        this.posTail = 0;
        data[0] =  new Position(cuadriculaSize/2, cuadriculaSize/2);
        this.posHead = 1;
        data[1] =  new Position(cuadriculaSize/2, cuadriculaSize/2 + 1);
        this.cuadricula = new String[cuadriculaSize][cuadriculaSize];
        this.cuadriculaSize = cuadriculaSize;
        this.fruits = new Position[amountfruits];
        frutas();
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
        for (int i = 0; i < cuadriculaSize; i++) {
            for (int j = 0; j < cuadriculaSize; j++) {
                cuadricula[i][j] = null;
            }
        }

        for (Position fruit : fruits) {
            if (fruit != null) {
                cuadricula[fruit.x][fruit.y] = "f";
            }
        }

        for (int i = 0; i < size; i++) {
            Position segmento = data[(posTail + i) % capacity];
            if (cuadricula[segmento.x][segmento.y] == null || "f" == cuadricula[segmento.x][segmento.y]) {
                cuadricula[segmento.x][segmento.y] = "#";
            }
            else {
                throw new IllegalArgumentException("colision");
            }
        }
    }

    public Position LeerInput() {
        System.out.print("Movimiento (W/A/S/D): ");
        char c = Character.toLowerCase(scanner.next().charAt(0));

        if (c == 'a') {
            return new Position(0, -1);
        } 
        else if (c == 'd') {
            return new Position(0, 1);
        } 
        else if (c == 'w') {
            return new Position(-1, 0);
        } 
        else if (c == 's') {
            return new Position(1, 0);
        } 
        else {
            return new Position(0, 0);
        }
    }

    public void actualizarPosiciones(Position input) {
        Position newHead = new Position(data[posHead].x + input.x, data[posHead].y + input.y);
        if (newHead.x < 0 || newHead.x >= cuadriculaSize || newHead.y < 0 || newHead.y >= cuadriculaSize) {
            throw new IllegalArgumentException("La serpiente salio de la cuadricula");
        }

        boolean ateFruit = removeFruit(newHead);
        addHead(newHead);
        if (!ateFruit) {
            removeTail();
        }
    }

    public void frutas() {
        for (int i = 0; i < fruits.length; i++) {
            Position fruit;
            do {
                fruit = new Position(random.nextInt(cuadriculaSize), random.nextInt(cuadriculaSize));
            } while (occupiedBySnake(fruit) || occupiedByFruit(fruit, i));
            fruits[i] = fruit;
        }
    }

    private boolean occupiedBySnake(Position position) {
        for (int i = 0; i < size; i++) {
            Position segmento = data[(posTail + i) % capacity];
            if (segmento.x == position.x && segmento.y == position.y) {
                return true;
            }
        }
        return false;
    }

    private boolean occupiedByFruit(Position position, int limit) {
        for (int i = 0; i < limit; i++) {
            Position currentFruit = fruits[i];
            if (currentFruit != null && currentFruit.x == position.x && currentFruit.y == position.y) {
                return true;
            }
        }
        return false;
    }

    private boolean removeFruit(Position position) {
        for (int i = 0; i < fruits.length; i++) {
            Position currentFruit = fruits[i];
            if (currentFruit != null && currentFruit.x == position.x && currentFruit.y == position.y) {
                fruits[i] = null;
                return true;
            }
        }
        return false;
    }
}