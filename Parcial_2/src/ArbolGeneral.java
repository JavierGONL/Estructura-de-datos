/**
 * Árbol general: cada nodo guarda un Registro y una lista con TODOS sus hijos (puede tener muchos).
 * Representa la jerarquía de tareas: una tarea y sus subtareas.
 * Los recorridos devuelven los ids en una lista para que Sistema decida cómo imprimirlos.
 * Convención de altura: número de aristas del camino más largo; árbol vacío = -1; hoja = 0.
 */
public class ArbolGeneral {

    /** Nodo del árbol general, tiene una variable registro y la lista de los hijos */
    private static class NodoGeneral {
        Registro registro;
        DynamicArray<NodoGeneral> hijos = new DynamicArray<>(2);

        NodoGeneral(Registro registro) {
            this.registro = registro;
        }
    }

    private NodoGeneral root;
    private int contador;          // cantidad de nodos

    public ArbolGeneral() {
        this.root = null;
    }

    public int size() {
        return contador;
    }

    /**
     * Qué hace: devuelve el id de la raíz, o 0 si todavía no hay raíz (los ids reales son positivos).
     * Cómo: mira root. Sistema lo usa para rechazar una segunda raíz y avisar cuál ya existe.
     */
    public int idRaiz() {
        return root == null ? 0 : root.registro.id;
    }

    // ---------- búsqueda e inserción ----------

    /** Qué hace: busca el nodo con ese id en todo el árbol. Cómo: llama a la versión recursiva desde la raíz. */
    private NodoGeneral buscar(int id) {
        return buscar(root, id);
    }

    /**
     * Qué hace: busca el id dentro del subárbol de v; devuelve el nodo o null si no está.
     * Cómo: si v tiene ese id, lo devuelve; si no, pregunta recursivamente a cada hijo y devuelve
     * el primer resultado que no sea null. Como no hay orden entre hermanos, puede tener que mirar
     * todos los nodos: O(n).
     */
    private NodoGeneral buscar(NodoGeneral v, int id) {
        if (v == null) return null;
        if (v.registro.id == id) return v;

        
        /** va entrando en cada subarbol hasta que llega a un valor nulo o el que se busca,
         *  si es el valor buscado sale del bucle for
         */
        for (NodoGeneral h : v.hijos) {
            NodoGeneral r = buscar(h, id);
            if (r != null) return r;
        }
        return null;
    }

    /** Qué hace: dice si existe un registro con ese id. Cómo: true si buscar no devuelve null. */
    public boolean search(int id) {
        return buscar(id) != null;
    }

    /**
     * Qué hace: agrega un registro al árbol. Si padre == 0 será la raíz; si no, queda como último hijo de su padre. 
     * Devuelve false (y no cambia nada) si ya hay raíz o si el padre no existe.
     * Cómo: para la raíz mira que root sea null; para un hijo busca el nodo padre y le agrega el
     * nuevo nodo a su lista de hijos. Los hijos quedan en el orden en que se registran.
     */
    public boolean insercion(Registro r) {
        if (r.padre == 0) {
            if (root != null) return false;
            root = new NodoGeneral(r);
            contador++;
            return true;
        }
        
        NodoGeneral p = buscar(r.padre);
        if (p == null) return false;
        p.hijos.add(new NodoGeneral(r));
        contador++;
        return true;
    }

    // ---------- recorridos (devuelven los ids en orden) ----------

    /** Qué hace: ids en preorden (el nodo y luego sus hijos). Cómo: crea la lista y llama a la recursiva. */
    public DynamicArray<Integer> preOrder() {
        DynamicArray<Integer> res = new DynamicArray<>(8);
        preOrder(root, res);
        return res;
    }

    /**
     * Qué hace: agrega a res los ids del subárbol de v en preorden.
     * Cómo: primero guarda el id de v y después recorre, de izquierda a derecha, cada hijo
     * con la misma función. Cada nodo se visita una vez: O(n).
     */
    private void preOrder(NodoGeneral v, DynamicArray<Integer> res) {
        if (v == null) return;
        res.add(v.registro.id);                              // primero el nodo
        for (NodoGeneral h : v.hijos) preOrder(h, res);      // luego los hijos
    }

    /** Qué hace: ids en postorden (los hijos y luego el nodo). Cómo: crea la lista y llama a la recursiva. */
    public DynamicArray<Integer> postOrder() {
        DynamicArray<Integer> res = new DynamicArray<>(8);
        postOrder(root, res);
        return res;
    }

    /**
     * Qué hace: agrega a res los ids del subárbol de v en postorden.
     * Cómo: primero recorre cada hijo con la misma función y solo al final guarda el id de v;
     * por eso cada nodo aparece después de todos sus descendientes y la raíz queda de última. O(n).
     */
    private void postOrder(NodoGeneral v, DynamicArray<Integer> res) {
        if (v == null) return;
        for (NodoGeneral h : v.hijos) postOrder(h, res);     // primero los hijos
        res.add(v.registro.id);                              // luego el nodo
    }

    /**
     * Qué hace: devuelve los ids por niveles: primero la raíz, luego sus hijos de izquierda a derecha,
     * luego los nietos, y así hasta el último nivel.
     * Cómo: uso UNA sola cola (FIFO). Meto la raíz. Mientras la cola no esté vacía: saco el primero
     * (poll), guardo su id en el resultado y meto al final de la cola a todos sus hijos de izquierda
     * a derecha. Como la cola respeta el orden de llegada, todos los nodos de un nivel salen antes
     * que los del nivel siguiente. Termina cuando la cola queda vacía, o sea cuando ya salió el
     * último nodo (una hoja del último nivel). Cada nodo entra y sale una vez: O(n).
     */
    public DynamicArray<Integer> levelOrder() {
        DynamicArray<Integer> res = new DynamicArray<>(8);
        if (root == null) return res;

        Queue<NodoGeneral> cola = new Queue<>();
        cola.add(root);
        while (!cola.isEmpty()) {
            NodoGeneral v = cola.poll();
            res.add(v.registro.id);
            for (NodoGeneral h : v.hijos) cola.add(h);
        }
        return res;
    }

    // ---------- altura y hojas ----------

    /** Qué hace: altura del árbol en aristas (vacío = -1, hoja = 0). Cómo: llama a la recursiva desde la raíz. */
    public int altura() {
        return altura(root);
    }

    /**
     * Qué hace: altura del subárbol de v.
     * Cómo: calcula la altura de cada hijo y se queda con la mayor; la de v es esa mayor + 1.
     * Si no tiene hijos, max queda en -1 y la altura es 0 (hoja). Si v es null devuelve -1. O(n).
     */
    private int altura(NodoGeneral v) {
        if (v == null) return -1;
        int max = -1;
        for (NodoGeneral h : v.hijos) {
            max = Math.max(max, altura(h));
        }
        return max + 1;
    }

    /** Qué hace: cuenta las hojas (nodos sin hijos). Cómo: llama a la recursiva desde la raíz. */
    public int contarHojas() {
        return contarHojas(root);
    }

    /**
     * Qué hace: cuenta las hojas del subárbol de v.
     * Cómo: si v no tiene hijos vale 1; si tiene, suma las hojas de cada hijo. O(n).
     */
    private int contarHojas(NodoGeneral v) {
        if (v == null) return 0;
        if (v.hijos.isEmpty()) return 1;

        int total = 0;
        for (NodoGeneral h : v.hijos) total += contarHojas(h);
        return total;
    }

    // ---------- cancelación en cascada ----------

    /**
     * Qué hace: cancela la tarea con ese id y todas sus subtareas que sigan PENDIENTE; devuelve cuántas canceló
     * (0 si el id no existe o si no había pendientes).
     * Cómo: busca el nodo y llama a cancelar. Los nodos NO se borran del árbol: solo cambian de estado.
     */
    public int cancelarSubarbol(int id) {
        NodoGeneral v = buscar(id);
        return v == null ? 0 : cancelar(v);
    }

    /**
     * Qué hace: cancela v (si está pendiente) y sus descendientes pendientes; devuelve cuántos canceló.
     * Cómo: si v está PENDIENTE lo pasa a CANCELADO y cuenta 1; después baja por TODOS los hijos
     * sumando lo que cancelen. Se baja siempre, aunque v esté ATENDIDO (que conserva su estado),
     * porque debajo de un nodo atendido puede haber descendientes todavía pendientes.
     */
    private int cancelar(NodoGeneral v) {
        int cancelados = 0;
        if (v.registro.estadoActual() == Registro.Estado.PENDIENTE) {
            v.registro.cancelado();
            cancelados++;
        }
        for (NodoGeneral h : v.hijos) cancelados += cancelar(h);
        return cancelados;
    }
}
