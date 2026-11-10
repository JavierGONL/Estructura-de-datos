package ecg.ml.estructuras;

/**
 * Nodo de árbol binario. Equivalente de la clase TreeNode de estructuras/arbol.py.
 *
 * <p>Uso en el proyecto (árbol de decisión):
 * <ul>
 *   <li>interno: {@link #caracteristica} = índice de característica, {@link #umbral} = umbral;
 *       {@link #left} es la rama "&lt;= umbral" y {@link #right} la rama "&gt; umbral".
 *   <li>hoja: {@link #conteos} = conteo de latidos por clase [N, S, V, F].
 * </ul>
 * No hay insert/balanceo: la forma la decide el algoritmo de entrenamiento.
 */
public class TreeNode {

    /** Índice de característica del nodo interno; -1 en las hojas. */
    public int caracteristica;
    /** Umbral del nodo interno. */
    public double umbral;
    /** Conteos por clase [N, S, V, F] en las hojas; null en los internos. */
    public int[] conteos;

    public TreeNode left;
    public TreeNode right;

    /** Crea un nodo interno (característica, umbral) con sus dos hijos. */
    public TreeNode(int caracteristica, double umbral, TreeNode left, TreeNode right) {
        this.caracteristica = caracteristica;
        this.umbral = umbral;
        this.conteos = null;
        this.left = left;
        this.right = right;
    }

    /** Crea una hoja con los conteos por clase. */
    public TreeNode(int[] conteos) {
        this.caracteristica = -1;
        this.umbral = 0.0;
        this.conteos = conteos;
        this.left = null;
        this.right = null;
    }

    public boolean isLeaf() {
        return left == null && right == null;
    }

    // ---- utilidades estáticas (equivalentes a las funciones de arbol.py) ----

    /** Número de niveles (árbol vacío = 0, una hoja = 1). */
    public static int height(TreeNode node) {
        if (node == null) {
            return 0;
        }
        return 1 + Math.max(height(node.left), height(node.right));
    }

    public static int countNodes(TreeNode node) {
        if (node == null) {
            return 0;
        }
        return 1 + countNodes(node.left) + countNodes(node.right);
    }

    public static int countLeaves(TreeNode node) {
        if (node == null) {
            return 0;
        }
        if (node.isLeaf()) {
            return 1;
        }
        return countLeaves(node.left) + countLeaves(node.right);
    }

    /** Cierto si todo nodo tiene 0 o 2 hijos (árbol de decisión bien construido). */
    public static boolean isFull(TreeNode node) {
        if (node == null || node.isLeaf()) {
            return true;
        }
        if (node.left == null || node.right == null) {
            return false;
        }
        return isFull(node.left) && isFull(node.right);
    }
}
