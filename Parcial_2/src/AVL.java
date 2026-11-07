/**
 * Árbol AVL: un BST que se mantiene equilibrado. Cada nodo guarda su altura y se cumple siempre que
 * el factor de balance = altura(izquierda) - altura(derecha) está entre -1 y 1.
 * Si al insertar o eliminar algún nodo llega a +2 o -2, se corrige con rotaciones (LL, RR, LR, RL).
 * Así la altura es O(log n) aunque los ids lleguen ordenados.
 * Convención: árbol vacío = altura -1; hoja = altura 0.
 */
public class AVL {
    private Nodo root;
    private int contador;   // cantidad de nodos
    /** Bitácora de las rotaciones hechas (para el caso D y para explicar qué pasó). */
    private DynamicArray<String> rotaciones = new DynamicArray<>(4);

    public int size() {
        return contador;
    }

    public DynamicArray<String> rotaciones() {
        return rotaciones;
    }

    public void limpiarRotaciones() {
        rotaciones.reset();
    }

    public int idRaiz() {
        return root == null ? 0 : root.registro.id;
    }

    // ---------- altura y balance de un nodo ----------

    /** Qué hace: altura de un nodo; null vale -1. Cómo: lee el campo altura que cada nodo guarda. O(1). */
    private int alt(Nodo v) {
        return v == null ? -1 : v.altura;
    }

    /** Qué hace: recalcula la altura de v. Cómo: 1 + la mayor altura de sus hijos (que ya están bien). O(1). */
    private void actualizar(Nodo v) {
        v.altura = 1 + Math.max(alt(v.L), alt(v.R));
    }

    /** Qué hace: factor de balance de v. Cómo: altura izquierda menos altura derecha. Positivo = pesa a la izquierda. */
    private int balance(Nodo v) {
        return v == null ? 0 : alt(v.L) - alt(v.R);
    }

    // ---------- rotaciones ----------

    /**
     * Qué hace: rotación simple a la derecha en y; devuelve el nodo que queda arriba.
     * Cómo: el hijo izquierdo x sube; y pasa a ser hijo derecho de x, y el hijo derecho que tenía x
     * pasa a ser hijo izquierdo de y. Se actualiza primero la altura de y (quedó abajo) y luego la de x.
     * El orden inorden no cambia. O(1).
     */
    private Nodo rotarDerecha(Nodo y) {
        Nodo x = y.L;
        y.L = x.R;
        x.R = y;
        actualizar(y);
        actualizar(x);
        return x;
    }

    /**
     * Qué hace: rotación simple a la izquierda en x; devuelve el nodo que queda arriba.
     * Cómo: es el espejo de rotarDerecha: el hijo derecho y sube, x pasa a ser su hijo izquierdo y
     * el hijo izquierdo que tenía y pasa a ser hijo derecho de x. Luego se actualizan alturas (x y después y).
     */
    private Nodo rotarIzquierda(Nodo x) {
        Nodo y = x.R;
        x.R = y.L;
        y.L = x;
        actualizar(x);
        actualizar(y);
        return y;
    }

    /**
     * Qué hace: deja v con balance entre -1 y 1 y devuelve la nueva raíz de ese subárbol.
     * Cómo: recalcula la altura de v y mira su balance:
     *  - balance > 1 (pesa a la izquierda): si el hijo izquierdo no pesa a la derecha (balance >= 0)
     *    es LL; una rotación a la derecha en v. Si el hijo izquierdo pesa a la derecha es LR:
     *    primero rotación a la izquierda en el hijo y luego a la derecha en v.
     *  - balance < -1 (pesa a la derecha): si el hijo derecho no pesa a la izquierda (balance <= 0)
     *    es RR: una rotación a la izquierda en v. Si pesa a la izquierda es RL: derecha en el hijo
     *    y luego izquierda en v.
     * Se usa >= 0 (y <= 0) porque al ELIMINAR el hijo puede tener balance 0 y entonces la rotación simple es la correcta.
     * Cada rotación queda anotada en la bitácora.
     */
    private Nodo rebalancear(Nodo v) {
        actualizar(v);
        int b = balance(v);
        if (b > 1) {
            if (balance(v.L) >= 0) {
                rotaciones.add("LL (derecha en " + v.registro.id + ")");
                return rotarDerecha(v);
            }
            rotaciones.add("LR (izquierda en " + v.L.registro.id + ", derecha en " + v.registro.id + ")");
            v.L = rotarIzquierda(v.L);
            return rotarDerecha(v);
        }
        if (b < -1) {
            if (balance(v.R) <= 0) {
                rotaciones.add("RR (izquierda en " + v.registro.id + ")");
                return rotarIzquierda(v);
            }
            rotaciones.add("RL (derecha en " + v.R.registro.id + ", izquierda en " + v.registro.id + ")");
            v.R = rotarDerecha(v.R);
            return rotarIzquierda(v);
        }
        return v;
    }

    // ---------- búsqueda, inserción y eliminación ----------

    /**
     * Qué hace: devuelve el registro con ese id, o null. Cómo: igual que en el BST (baja a la izquierda
     * o a la derecha según el id). Como el AVL está equilibrado, son O(log n) pasos incluso en el peor caso.
     */
    public Registro buscar(int id) {
        Nodo v = root;
        while (v != null) {
            if (id == v.registro.id) return v.registro;
            v = (id < v.registro.id) ? v.L : v.R;
        }
        return null;
    }

    /** Qué hace: dice si existe ese id. Cómo: true si buscar no devuelve null. */
    public boolean search(int id) {
        return buscar(id) != null;
    }

    /**
     * Qué hace: inserta un registro; devuelve false (sin cambiar nada) si el id ya existe.
     * Cómo: comprueba que no esté repetido y llama a la recursiva. O(log n).
     */
    public boolean insercion(Registro r) {
        if (search(r.id)) return false;
        root = insercion(root, r);
        contador++;
        return true;
    }

    /**
     * Qué hace: inserta r en el subárbol de v y devuelve su nueva raíz.
     * Cómo: baja como en el BST; al volver de la recursión (camino de regreso) llama a rebalancear en
     * cada nodo del camino, que actualiza su altura y rota si hace falta. Con una inserción basta
     * una rotación (simple o doble) para dejar todo equilibrado.
     */
    private Nodo insercion(Nodo v, Registro r) {
        if (v == null) return new Nodo(r);
        if (r.id < v.registro.id) v.L = insercion(v.L, r);
        else v.R = insercion(v.R, r);
        return rebalancear(v);
    }

    /** Qué hace: elimina el id; devuelve false si no existe. Cómo: comprueba que exista y llama a la recursiva. O(log n). */
    public boolean eliminar(int id) {
        if (!search(id)) return false;
        root = eliminar(root, id);
        contador--;
        return true;
    }

    /**
     * Qué hace: elimina el id del subárbol de v y devuelve su nueva raíz.
     * Cómo: igual que el BST (con dos hijos se copia el registro del SUCESOR, el menor de la derecha,
     * y se elimina el sucesor de la derecha). La diferencia es que al volver de la recursión se llama
     * a rebalancear en cada nodo del camino, porque eliminar puede desequilibrar varios niveles.
     */
    private Nodo eliminar(Nodo v, int id) {
        if (id < v.registro.id) {
            v.L = eliminar(v.L, id);
        } else if (id > v.registro.id) {
            v.R = eliminar(v.R, id);
        } else {
            if (v.L == null) return v.R;
            if (v.R == null) return v.L;
            Nodo suc = v.R;
            while (suc.L != null) suc = suc.L;
            v.registro = suc.registro;
            v.R = eliminar(v.R, suc.registro.id);
        }
        return rebalancear(v);
    }

    // ---------- inorden, altura y verificación ----------

    /** Qué hace: ids en inorden (de menor a mayor). Cómo: crea la lista y llama a la recursiva. */
    public DynamicArray<Integer> inOrder() {
        DynamicArray<Integer> res = new DynamicArray<>(8);
        inOrder(root, res);
        return res;
    }

    /** Qué hace: agrega a res los ids del subárbol de v en inorden. Cómo: izquierda, nodo, derecha. O(n). */
    private void inOrder(Nodo v, DynamicArray<Integer> res) {
        if (v == null) return;
        inOrder(v.L, res);
        res.add(v.registro.id);
        inOrder(v.R, res);
    }

    /** Qué hace: altura del árbol (vacío = -1, hoja = 0). Cómo: lee la altura guardada en la raíz. O(1). */
    public int altura() {
        return alt(root);
    }

    /**
     * Qué hace: comprueba que TODOS los nodos tengan factor de balance entre -1 y 1 y que la altura
     * guardada sea la real. Sirve para las pruebas.
     * Cómo: llama a verificar, que recalcula las alturas desde las hojas sin fiarse de las guardadas.
     */
    public boolean balanceado() {
        return verificar(root) != MALO;
    }

    private static final int MALO = -100;   // marca "este subárbol está mal"

    /**
     * Qué hace: devuelve la altura REAL del subárbol de v, o MALO si algún nodo está desbalanceado o
     * tiene guardada una altura distinta de la real.
     * Cómo: calcula la altura real de los dos hijos, compara su diferencia (debe ser <= 1) y compara
     * 1 + max con la altura guardada en v.
     */
    private int verificar(Nodo v) {
        if (v == null) return -1;
        int hi = verificar(v.L);
        int hd = verificar(v.R);
        if (hi == MALO || hd == MALO || Math.abs(hi - hd) > 1) return MALO;
        int h = 1 + Math.max(hi, hd);
        return (h == v.altura) ? h : MALO;
    }

    /** Qué hace: forma del árbol en texto, por ejemplo 40(20,50); "_" es un hijo vacío. Cómo: igual que en el BST. */
    public String estructura() {
        return estructura(root);
    }

    /** Qué hace: texto del subárbol de v. Cómo: null = "_"; hoja = su id; si no, id(izquierda,derecha). */
    private String estructura(Nodo v) {
        if (v == null) return "_";
        if (v.L == null && v.R == null) return String.valueOf(v.registro.id);
        return v.registro.id + "(" + estructura(v.L) + "," + estructura(v.R) + ")";
    }
}
