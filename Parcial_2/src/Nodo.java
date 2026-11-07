/**
 * Nodo binario: lo usan el BST y el AVL.
 * Guarda un Registro, el hijo izquierdo (L), el hijo derecho (R) y la altura del nodo.
 * La clave de orden es registro.id (nunca la prioridad).
 * Campos sin modificador (visibles en el paquete) para poder escribir v.L y v.R directamente.
 * registro NO es final: al eliminar un nodo con dos hijos se le copia el registro del sucesor.
 */
public class Nodo {
    Registro registro;
    int altura;     // solo la usa el AVL; un nodo recién creado es hoja, y una hoja tiene altura 0
    Nodo L;         // hijo izquierdo (ids menores)
    Nodo R;         // hijo derecho (ids mayores)

    /** Qué hace: crea un nodo hoja con ese registro. Cómo: guarda el registro; L y R quedan en null y altura en 0. */
    Nodo(Registro registro) {
        this.registro = registro;
    }
}
