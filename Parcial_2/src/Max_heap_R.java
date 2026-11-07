/**
 * Max-heap binario de Registros, guardado en un arreglo (monton[0..size-1]).
 * La raíz monton[0] es siempre la tarea que se atiende primero según Registro.desempate
 * (mayor prioridad; si empatan, menor id). Contiene solo tareas PENDIENTES.
 * Posiciones: padre de i = (i-1)/2; hijo izquierdo = 2i+1; hijo derecho = 2i+2.
 * Toda comparación de orden se hace con Registro.desempate: no hay reglas de desempate repetidas aquí.
 */
public class Max_heap_R {
    private Registro[] monton = new Registro[8];   // arreglo interno (puede tener espacio libre al final)
    private int size = 0;                          // cuántos registros hay realmente

    /** Qué hace: posición del padre de i. Cómo: (i-1)/2 con división entera. */
    static int padre(int i) { return (i - 1) / 2; }

    /** Qué hace: posición del hijo izquierdo de i. Cómo: 2i+1. */
    static int left(int i) { return 2 * i + 1; }

    /** Qué hace: posición del hijo derecho de i. Cómo: 2i+2. */
    static int right(int i) { return 2 * i + 2; }

    /** Qué hace: intercambia dos posiciones del arreglo. Cómo: usa una variable temporal. */
    static void swap(Registro[] a, int i, int j) {
        Registro t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    /** Qué hace: cuántos registros hay en el heap. Cómo: devuelve size. */
    public int size() { return size; }

    /** Qué hace: dice si el heap está vacío. Cómo: compara size con 0. Sistema lo usa antes de extraer. */
    public boolean isEmpty() { return size == 0; }

    /** Qué hace: devuelve la raíz (la tarea más urgente) SIN sacarla. Cómo: lee monton[0]; falla si está vacío. O(1). */
    public Registro max() {
        if (size == 0) throw new RuntimeException("no hay registros disponibles");
        return monton[0];
    }

    /**
     * Qué hace: busca un registro por id y devuelve su posición en el arreglo (o -1 si no está).
     * Cómo: recorre el arreglo comparando ids. (La prioridad no sirve para buscar: se repite entre registros.) O(n).
     */
    public int busqueda(int id) {
        for (int i = 0; i < size; i++) {
            if (monton[i].id == id) return i;
        }
        return -1;
    }

    /**
     * Qué hace: agrega un registro al heap.
     * Cómo: si el arreglo está lleno lo duplica (copiando a mano); pone el registro en la primera
     * posición libre (al final) y llama a siftup para que suba hasta su lugar. O(log n).
     */
    public void insert(Registro v) {
        if (size == monton.length) {
            Registro[] nuevo = new Registro[2 * size];
            for (int i = 0; i < size; i++) nuevo[i] = monton[i];
            monton = nuevo;
        }
        monton[size] = v;
        size++;
        siftup(monton, size - 1);
    }

    /**
     * Qué hace: hace subir el elemento de la posición i hasta que su padre vaya antes que él.
     * Cómo: mientras i no sea la raíz y el elemento vaya ANTES que su padre (Registro.desempate),
     * los intercambia y continúa desde la posición del padre. Como mucho sube la altura del heap: O(log n).
     */
    static void siftup(Registro[] a, int i) {
        while (i > 0 && Registro.desempate(a[i], a[padre(i)])) {
            swap(a, i, padre(i));
            i = padre(i);
        }
    }

    /**
     * Qué hace: saca y devuelve la raíz (la tarea más urgente).
     * Cómo: guarda la raíz; el ÚLTIMO elemento del arreglo pasa a la raíz (y su lugar queda en null);
     * y siftDown lo baja hasta su sitio. Falla si el heap está vacío. O(log n).
     */
    public Registro extractMax() {
        if (size == 0) throw new RuntimeException("no hay registros disponibles");
        Registro top = monton[0];
        size--;
        monton[0] = monton[size];
        monton[size] = null;
        siftDown(monton, 0, size);
        return top;
    }

    /**
     * Qué hace: hace bajar el elemento de la posición i dentro de a[0..n-1] hasta que ya vaya antes que sus hijos.
     * Cómo: mientras tenga hijo izquierdo (left(i) < n): escoge el MEJOR de sus hijos (el derecho solo
     * si va antes que el izquierdo, así en un empate de prioridad gana el menor id); si ese hijo no va
     * antes que el nodo, ya está en su lugar y se detiene (break); si va antes, los intercambia y
     * sigue desde la posición del hijo. Como mucho baja la altura del heap: O(log n).
     */
    static void siftDown(Registro[] a, int i, int n) {
        while (left(i) < n) {
            int c = left(i);
            if (right(i) < n && Registro.desempate(a[right(i)], a[c])) c = right(i);
            if (!Registro.desempate(a[c], a[i])) break;
            swap(a, i, c);
            i = c;
        }
    }

    /**
     * Qué hace: convierte a[0..n-1] en un heap.
     * Cómo (Floyd): recorre de la última posición que tiene hijos (n/2 - 1) hasta la 0, INCLUYENDO la raíz,
     * llamando a siftDown en cada una. Las hojas ya son heaps de un elemento, así que se parte de abajo hacia
     * arriba. Cuesta O(n), mejor que insertar uno por uno (O(n log n)).
     */
    static void buildheap(Registro[] a, int n) {
        for (int i = n / 2 - 1; i >= 0; i--) siftDown(a, i, n);
    }

    /**
     * Qué hace: carga en el heap los registros de un arreglo y lo deja listo (por ejemplo, el caso F).
     * Cómo: copia el arreglo al interno (con un margen de espacio libre) y llama a buildheap. O(n).
     */
    public void construir(Registro[] a) {
        monton = new Registro[a.length + 8];
        for (int i = 0; i < a.length; i++) monton[i] = a[i];
        size = a.length;
        buildheap(monton, size);
    }

    /**
     * Qué hace: después de CANCELAR, deja en el heap solo los registros que siguen PENDIENTES.
     * Cómo: recorre el arreglo y va pasando hacia el principio (compactando) los que están PENDIENTES;
     * pone null en lo que sobra, ajusta size y llama a buildheap para recuperar el orden de heap. O(n).
     * (Así los cancelados nunca podrán salir con SIGUIENTE.)
     */
    public void reconstruccion() {
        int k = 0;
        for (int i = 0; i < size; i++) {
            if (monton[i].estadoActual() == Registro.Estado.PENDIENTE) monton[k++] = monton[i];
        }
        for (int i = k; i < size; i++) monton[i] = null;
        size = k;
        buildheap(monton, size);
    }

    /**
     * Qué hace: devuelve los registros del heap ordenados SIN alterar el heap (lo usa REPORTE).
     * Cómo: le pasa el arreglo a heapSort, que trabaja sobre una copia. Queda de "peor a mejor":
     * el que se atiende primero está en la ÚLTIMA posición.
     */
    public Registro[] ordenados() {
        return heapSort(monton, size);
    }

    /**
     * Qué hace: Heap-Sort. Devuelve una COPIA de a[0..n-1] ordenada de menor a mayor según Registro.desempate
     * (o sea, el que se atiende primero queda al final). El arreglo original no se modifica.
     * Cómo: copia el arreglo (Heap-Sort desarma lo que ordena, por eso no se puede usar el heap
     * original); construye un heap con buildheap; y repite: intercambia la raíz con la última
     * posición de la parte activa (esa raíz ya queda en su lugar definitivo), reduce la parte activa
     * en uno y llama a siftDown para restaurar el heap. O(n log n) en todos los casos.
     */
    public static Registro[] heapSort(Registro[] a, int n) {
        Registro[] o = new Registro[n];
        for (int i = 0; i < n; i++) o[i] = a[i];
        buildheap(o, n);
        for (int end = n - 1; end > 0; end--) {
            swap(o, 0, end);
            siftDown(o, 0, end);
        }
        return o;
    }

    /** Qué hace: texto del arreglo interno, por ejemplo [20(8), 50(6)]. Cómo: concatena corto() de cada posición. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < size; i++) {
            if (i > 0) sb.append(", ");
            sb.append(monton[i].corto());
        }
        return sb.append("]").toString();
    }
}
