/**
 * Registro = una tarea del planificador de robot (contexto II.1).
 *
 * Es el MISMO objeto el que vive en el árbol general, el BST, el AVL y el heap:
 * por eso, cuando cambio su estado, el cambio se ve en las cuatro estructuras a la vez.
 *
 * Estados: PENDIENTE (aún no se entrega al robot), ATENDIDO (ya se entregó) y CANCELADO.
 * El estado es privado: solo se consulta con estadoActual() y solo se cambia con
 * pendiente(), atendido() y cancelado(). 
 */
public class Registro {

    /** Los tres estados posibles. Es public para poder compararlo desde otras clases. */
    public enum Estado { PENDIENTE, ATENDIDO, CANCELADO }

    public final int id;          // entero positivo y único; es la clave del BST y del AVL
    public final String nombre;   // texto corto sin espacios
    public final int prioridad;   // de 1 a 10; 10 es la más urgente
    public final int padre;       // id del registro padre; 0 significa que es la raíz
    private Estado estado = Estado.PENDIENTE;   // toda tarea nace pendiente

    /**
     * Qué hace: crea una tarea nueva.
     * Cómo: guarda los cuatro datos en campos final (no cambian después) y deja el estado en PENDIENTE.
     */
    public Registro(int id, String nombre, int prioridad, int padre) {
        this.id = id;
        this.nombre = nombre;
        this.prioridad = prioridad;
        this.padre = padre;
    }

    public Estado estadoActual() {
        return this.estado;
    }

    public void pendiente() {
        this.estado = Estado.PENDIENTE;
    }

    public void cancelado() {
        this.estado = Estado.CANCELADO;
    }

    public void atendido() {
        this.estado = Estado.ATENDIDO;
    }

    /**
     * Qué hace: decide cuál de dos tareas se atiende primero. Es el ÚNICO lugar donde está esta regla.
     * Cómo: si las prioridades son distintas gana la mayor; si empatan, gana la de menor id.
     * Lo usan el heap (para subir y bajar) y Heap-Sort (para ordenar), así nunca se contradicen.
     * Devuelve true si a va antes que b.
     */
    public static boolean desempate(Registro a, Registro b) {
        if (a.prioridad != b.prioridad) return a.prioridad > b.prioridad;
        return a.id < b.id;   // desempate: menor ID
    }

    /** Qué hace: texto corto "id(prioridad)" para reportes y trazas. Cómo: concatena los dos números. */
    public String corto() {
        return id + "(" + prioridad + ")";
    }
}
