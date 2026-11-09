/**
 * Árbol binario de búsqueda (sin balanceo). La clave es registro.id: a la izquierda van los ids
 * menores y a la derecha los mayores. No admite ids repetidos.
 * Si los ids llegan ordenados, el árbol se vuelve una cadena (altura n-1): por eso existe el AVL.
 * Convención: árbol vacío = altura -1; hoja = altura 0.
 */
public class BST {
    private Nodo root;
    private int contador;   // cantidad de nodos

    /** Qué hace: devuelve cuántos registros hay. Cómo: devuelve el contador. */
    public int size() {
        return contador;
    }

    /**
     * Qué hace: devuelve el registro con ese id, o null si no existe.
     * Cómo: parte de la raíz; si el id buscado es menor baja a la izquierda y si es mayor a la derecha,
     * hasta encontrarlo o llegar a null. Un camino por el árbol: O(altura) (O(log n) si está equilibrado,
     * O(n) en el peor caso).
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
     * Cómo: primero comprueba que no esté repetido y luego llama a la versión recursiva,
     * que devuelve la nueva raíz del árbol.
     */
    public boolean insercion(Registro r) {
        if (search(r.id)) return false;
        root = insercion(root, r);
        contador++;
        return true;
    }

    /**
     * Qué hace: inserta r en el subárbol de v y devuelve la raíz de ese subárbol.
     * Cómo: si v es null, aquí va el nodo nuevo. Si no, baja a la izquierda (id menor) o a la derecha
     * (id mayor) y vuelve a conectar el hijo con lo que devuelve la llamada. O(altura).
     */
    private Nodo insercion(Nodo v, Registro r) {
        if (v == null) return new Nodo(r);
        if (r.id < v.registro.id) v.L = insercion(v.L, r);
        else v.R = insercion(v.R, r);
        return v;
    }

    /**
     * Qué hace: elimina el registro con ese id; devuelve false si no existe.
     * Cómo: comprueba que exista y llama a la versión recursiva.
     */
    public boolean eliminar(int id) {
        if (!search(id)) return false;
        root = eliminar(root, id);
        contador--;
        return true;
    }

    /**
     * Qué hace: elimina el id del subárbol de v y devuelve la nueva raíz de ese subárbol.
     * Cómo: baja hasta el nodo. Hay tres casos al encontrarlo:
     *  - sin hijo izquierdo: lo reemplaza su hijo derecho (o null si es hoja);
     *  - sin hijo derecho: lo reemplaza su hijo izquierdo;
     *  - con dos hijos: busca el SUCESOR (el menor del subárbol derecho), copia su registro en este
     *    nodo y luego elimina el sucesor del subárbol derecho (que tiene a lo sumo un hijo).
     * El árbol sigue ordenado porque el sucesor es el siguiente id en el inorden.
     */
    private Nodo eliminar(Nodo v, int id) {
        if (id < v.registro.id) {
            v.L = eliminar(v.L, id);
        } else if (id > v.registro.id) {
            v.R = eliminar(v.R, id);
        } else {
            if (v.L == null) return v.R;
            if (v.R == null) return v.L;
            
            Nodo suc = minimo(v.R);
            v.registro = suc.registro;
            v.R = eliminar(v.R, suc.registro.id);
        }
        return v;
    }

    /** Qué hace: devuelve el nodo con el menor id del subárbol de v. Cómo: baja siempre por la izquierda. */
    private Nodo minimo(Nodo v) {
        while (v.L != null) v = v.L;
        return v;
    }

    /** Qué hace: ids en inorden (de menor a mayor). Cómo: crea la lista y llama a la recursiva. */
    public DynamicArray<Integer> inOrder() {
        DynamicArray<Integer> res = new DynamicArray<>(8);
        inOrder(root, res);
        return res;
    }

    /**
     * Qué hace: agrega a res los ids del subárbol de v en inorden.
     * Cómo: izquierda, luego el nodo, luego derecha. En un BST sale ordenado de menor a mayor. O(n).
     */
    private void inOrder(Nodo v, DynamicArray<Integer> res) {
        if (v == null) return;
        inOrder(v.L, res);
        res.add(v.registro.id);
        inOrder(v.R, res);
    }

    /** Qué hace: altura del árbol (vacío = -1, hoja = 0). Cómo: llama a la recursiva desde la raíz. */
    public int altura() {
        return altura(root);
    }

    /**
     * Qué hace: altura del subárbol de v.
     * Cómo: 1 + la mayor de las alturas de sus dos hijos; un null vale -1. O(n): aquí se recorre todo
     * porque el BST no guarda la altura en el nodo (el AVL sí).
     */
    private int altura(Nodo v) {
        if (v == null) return -1;
        return 1 + Math.max(altura(v.L), altura(v.R));
    }

    /** Qué hace: forma del árbol en texto, por ejemplo 8(3(1,4),9); "_" es un hijo vacío. Sirve para las pruebas. */
    public String estructura() {
        return estructura(root);
    }

    /**
     * Qué hace: texto del subárbol de v.
     * Cómo: null = "_"; una hoja es solo su id; si no, id(izquierda,derecha) con la misma función.
     */
    private String estructura(Nodo v) {
        if (v == null) return "_";
        if (v.L == null && v.R == null) return String.valueOf(v.registro.id);
        return v.registro.id + "(" + estructura(v.L) + "," + estructura(v.R) + ")";
    }
}
