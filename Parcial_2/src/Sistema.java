/**
 * Sistema: junta las cuatro estructuras (árbol general, BST, AVL y heap) y ofrece las seis operaciones.
 * Contexto II.1 (planificador de robot): cada registro es una tarea y la prioridad es su urgencia.
 *
 * Coherencia entre estructuras:
 *  - El MISMO objeto Registro está en las cuatro; cambiar su estado se ve en todas.
 *  - BST y AVL guardan el historial: atendidos y cancelados NO se borran, por eso CONSULTAR los encuentra.
 *  - El heap contiene solo los pendientes y es el que decide cuál es la siguiente tarea.
 */
public class Sistema {
    // aca se declaran las 4 estructuras, 
    private final ArbolGeneral arbol = new ArbolGeneral();
    private final BST bst = new BST();
    private final AVL avl = new AVL();
    private final Max_heap_R heap = new Max_heap_R();

    /**
     * Qué hace: ejecuta una línea de comando y devuelve el texto de respuesta.
     * Cómo: separa la línea en palabras, mira la primera (el comando) y llama al método correspondiente
     * después de convertir los números con Integer.parseInt. Si el comando no existe, si faltan
     * argumentos o si un número no es válido, devuelve un mensaje de error y no cambia nada.
     */
    public String ejecutar(String linea) {
        String[] t = linea.trim().split("\\s+");
        String cmd = t[0].toUpperCase();
        try {
            return switch (cmd) {
                case "REGISTRAR" -> t.length == 5
                        ? registrar(Integer.parseInt(t[1]), t[2], Integer.parseInt(t[3]), Integer.parseInt(t[4]))
                        : "ERROR: uso: REGISTRAR id nombre prioridad padre";
                case "CONSULTAR" -> t.length == 2 ? consultar(Integer.parseInt(t[1])) : "ERROR: uso: CONSULTAR id";
                case "SIGUIENTE" -> siguiente();
                case "CANCELAR" -> t.length == 2 ? cancelar(Integer.parseInt(t[1])) : "ERROR: uso: CANCELAR id";
                case "REPORTE" -> reporte();
                case "ESTADISTICAS" -> estadisticas();
                default -> "ERROR: comando desconocido (" + t[0] + ")";
            };
        } catch (NumberFormatException e) {
            return "ERROR: los argumentos numericos no son validos";
        }
    }

    /**
     * O1. Qué hace: agrega un registro a las cuatro estructuras.
     * Cómo: primero VALIDA todo y solo si todo está bien inserta, así un error nunca deja las estructuras a medias.
     * Rechaza, en este orden: id no positivo, id repetido, prioridad fuera de 1..10, padre negativo,
     * segunda raíz (padre 0 cuando ya hay raíz) y padre inexistente. No se detectan ciclos: el id
     * es nuevo y el padre ya existe, así que nunca se puede formar uno.
     */
    private String registrar(int id, String nombre, int prioridad, int padre) {
        // edge cases
        if (id <= 0) { // el id no puede ser menor o igual a 0
            return "ERROR: id invalido (" + id + ")";
        }

        if (avl.search(id)) { // no se permiten duplicados
            return "ERROR: ID duplicado (" + id + ")";
        }

        if (prioridad < 1 || prioridad > 10) { // como maximo se permite hasta 10 de dificultad
            return "ERROR: prioridad invalida (" + prioridad + ")";
        }
        if (padre < 0) { // por lo mismo que id > 0, el padre debe ser >=0, 0 es la raiz
            return "ERROR: padre invalido (" + padre + ")";
        }

        if (padre == 0) { // solo hay una raiz
            if (arbol.idRaiz() != 0) {
                return "ERROR: ya existe una raiz (" + arbol.idRaiz() + ")";
            }
        } 
        else if (!avl.search(padre)) { // no existe el padre
            return "ERROR: el padre " + padre + " no existe";
        }

        // ahora el registro tiene que entrar en las 4 estructuras 
        Registro r = new Registro(id, nombre, prioridad, padre);
        arbol.insercion(r); // Representa la jerarquía de tareas: una tarea y sus subtareas
        bst.insercion(r); // el bst como que solo esta para comparaciones 
        avl.insercion(r); // el avl es para busqueda por que al estar balanceado siempre es log(n)
        heap.insert(r); // Contiene solo tareas PENDIENTES
        return "OK: registro " + id;
    }

    /**
     * O2. Qué hace: muestra los datos y el estado de un registro.
     * Cómo: lo busca en el AVL (O(log n)). Como el AVL guarda el historial, también encuentra
     * los atendidos y los cancelados. Si el id no existe, devuelve un error.
     */
    private String consultar(int id) {
        Registro r = avl.buscar(id);
        if (r == null) {
            return "ERROR: el id " + id + " no existe";
        }

        return "ID=" + r.id + " NOMBRE=" + r.nombre + " PRIORIDAD=" + r.prioridad + "\nPADRE=" + r.padre + " ESTADO=" + r.estadoActual();
    }

    /**
     * O3. Qué hace: entrega la tarea pendiente más urgente y la marca ATENDIDA.
     * Cómo: si el heap está vacío responde SIN PENDIENTES; si no, extractMax saca la raíz (mayor
     * prioridad; en empate, menor id) y se le cambia el estado con atendido().
     */
    private String siguiente() {
        if (heap.isEmpty()) {
            return "SIN PENDIENTES";
        }

        Registro r = heap.extractMax();
        r.atendido();
        return "ATENDIDO: " + r.id + " (" + r.nombre + ")";
    }

    /**
     * O4. Qué hace: cancela una tarea y todas sus subtareas pendientes; informa cuántas canceló.
     * Cómo: comprueba que el id exista; el árbol general cancela en cascada (cambia el estado de los
     * pendientes del subárbol); y después el heap se reconstruye con solo los registros que siguen
     * pendientes, para que ningún cancelado pueda salir con SIGUIENTE. Los atendidos conservan su estado.
     */
    private String cancelar(int id) {
        if (!avl.search(id)) {
            return "ERROR: el id " + id + " no existe";
        }

        int cancelados = arbol.cancelarSubarbol(id);
        heap.reconstruccion(); 
        return "CANCELADOS: " + cancelados;
    }

    /**
     * O5. Qué hace: lista los pendientes en orden de atención (mayor prioridad primero; en empate, menor id).
     * Cómo: pide al heap los registros ordenados con Heap-Sort sobre una COPIA (el heap no se altera, así
     * un SIGUIENTE posterior atiende lo mismo que si no hubiera habido reporte). Heap-Sort los deja de
     * menor a mayor, así que se imprimen de atrás hacia adelante.
     */
    private String reporte() {
        Registro[] o = heap.ordenados();
        if (o.length == 0) {
            return "REPORTE vacio: 0 pendientes";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = o.length - 1; i >= 0; i--) {
            sb.append(o[i].corto());
            if (i > 0) sb.append(' ');
        }
        return sb.toString();
    }

    /**
     * O6. Qué hace: muestra los tres recorridos, la altura y las hojas del árbol general, las alturas
     * del BST y del AVL, y la cantidad de pendientes.
     * Cómo: pide cada dato a su estructura y lo arma en líneas de texto. Pendientes = tamaño del heap.
     */
    private String estadisticas() {
        return "Preorden: " + unir(arbol.preOrder())
                + "\nPostorden: " + unir(arbol.postOrder())
                + "\nPor niveles: " + unir(arbol.levelOrder())
                + "\nAltura arbol general: " + arbol.altura()
                + "\nHojas: " + arbol.contarHojas()
                + "\nAltura BST: " + bst.altura()
                + "\nAltura AVL: " + avl.altura()
                + "\nPendientes: " + heap.size();
    }

    /** Qué hace: convierte una lista de ids en texto "10,20,30". Cómo: recorre la lista separando con comas. */
    private static String unir(DynamicArray<Integer> ids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.length(); i++) {
            if (i > 0) sb.append(',');
            sb.append(ids.get(i));
        }
        return sb.toString();
    }
}
