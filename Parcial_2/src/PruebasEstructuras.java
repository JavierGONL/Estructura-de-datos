/**
 * Pruebas de las estructuras sueltas: casos D (rotaciones AVL), E (BST frente a AVL) y F (heap y Heap-Sort)
 * de la sección 5, más pruebas adicionales (preguntas del Anexo B y pruebas aleatorias).
 *   java -cp out PruebasEstructuras
 * Cada comprobación imprime [OK] o [FALLA]; si alguna falla, el programa termina con código 1.
 * Solo usa las estructuras hechas a mano (no hay nada de java.util).
 */
public class PruebasEstructuras {
    static int total = 0, fallas = 0;
    static long semilla = 42;   // semilla del generador de números pseudoaleatorios propio

    /** Qué hace: compara el resultado obtenido con el esperado y lo imprime. Cómo: equals sobre texto; cuenta fallas. */
    static void check(String desc, String esperado, String obtenido) {
        total++;
        boolean ok = esperado.equals(obtenido);
        if (!ok) fallas++;
        System.out.println("  [" + (ok ? "OK" : "FALLA") + "] " + desc + ": " + obtenido
                + (ok ? "" : "   (esperado " + esperado + ")"));
    }

    /** Qué hace: versión de check para enteros. Cómo: los convierte a texto. */
    static void check(String desc, int esperado, int obtenido) {
        check(desc, String.valueOf(esperado), String.valueOf(obtenido));
    }

    /** Qué hace: versión de check para booleanos. Cómo: los convierte a texto. */
    static void check(String desc, boolean esperado, boolean obtenido) {
        check(desc, String.valueOf(esperado), String.valueOf(obtenido));
    }

    /** Qué hace: crea un registro de prueba (nombre "R<id>", sin padre). */
    static Registro reg(int id, int prio) {
        return new Registro(id, "R" + id, prio, 0);
    }

    /** Qué hace: crea registros con ids 1..n y esas prioridades (ids distintos, como pide el caso F). */
    static Registro[] desdePrioridades(int[] p) {
        Registro[] res = new Registro[p.length];
        for (int i = 0; i < p.length; i++) res[i] = reg(i + 1, p[i]);
        return res;
    }

    /** Qué hace: texto "[p1, p2, ...]" con las prioridades de los primeros n registros. */
    static String prios(Registro[] a, int n) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(", ");
            sb.append(a[i].prioridad);
        }
        return sb.append("]").toString();
    }

    /** Qué hace: número pseudoaleatorio en [0, n). Cómo: generador congruencial lineal (sin java.util.Random). */
    static int aleatorio(int n) {
        semilla = (semilla * 1103515245L + 12345L) & 0x7fffffffL;
        return (int) ((semilla >> 8) % n);
    }

    // ------------------------------------------------------------------ Caso D
    /** Caso D: cuatro inserciones en un AVL vacío; cada una debe dejar la raíz 20 con UNA rotación del tipo esperado. */
    static void casoD() {
        System.out.println("=== CASO D: Rotaciones AVL ===");
        int[][] datos = {{30, 20, 10}, {10, 20, 30}, {30, 10, 20}, {10, 30, 20}};
        String[] tipo = {"LL", "RR", "LR", "RL"};
        for (int k = 0; k < 4; k++) {
            AVL avl = new AVL();
            for (int id : datos[k]) avl.insercion(reg(id, 1));
            System.out.println("Caso D-" + tipo[k] + ": inserciones " + datos[k][0] + ", " + datos[k][1] + ", " + datos[k][2]);
            System.out.println("  rotaciones realizadas: " + avl.rotaciones());
            check("raiz final", 20, avl.idRaiz());
            check("inorden", "[10, 20, 30]", avl.inOrder().toString());
            check("altura", 1, avl.altura());
            check("balanceado (todos los factores en [-1,1])", true, avl.balanceado());
            check("tipo de rotacion", tipo[k], avl.rotaciones().get(0).substring(0, 2));
        }
    }

    // ------------------------------------------------------------------ Caso E
    /** Caso E: claves 1..7 en BST y AVL (alturas 6 y 2), eliminar el 4, y comparación opcional con 1..1000. */
    static void casoE() {
        System.out.println("=== CASO E: BST frente a AVL ===");
        BST bst = new BST();
        AVL avl = new AVL();
        for (int i = 1; i <= 7; i++) { bst.insercion(reg(i, 1)); avl.insercion(reg(i, 1)); }
        System.out.println("  BST: " + bst.estructura());
        System.out.println("  AVL: " + avl.estructura());
        check("altura BST con 1..7", 6, bst.altura());
        check("altura AVL con 1..7", 2, avl.altura());
        check("inorden BST", "[1, 2, 3, 4, 5, 6, 7]", bst.inOrder().toString());
        check("inorden AVL", "[1, 2, 3, 4, 5, 6, 7]", avl.inOrder().toString());
        bst.eliminar(4);
        avl.eliminar(4);
        System.out.println("  Tras eliminar 4 -> BST: " + bst.estructura() + " | AVL: " + avl.estructura());
        check("search(4) en BST", false, bst.search(4));
        check("search(4) en AVL", false, avl.search(4));
        check("inorden BST sin 4", "[1, 2, 3, 5, 6, 7]", bst.inOrder().toString());
        check("inorden AVL sin 4", "[1, 2, 3, 5, 6, 7]", avl.inOrder().toString());
        check("AVL balanceado tras eliminar", true, avl.balanceado());

        BST b2 = new BST();
        AVL a2 = new AVL();
        for (int i = 1; i <= 1000; i++) { b2.insercion(reg(i, 1)); a2.insercion(reg(i, 1)); }
        System.out.println("  (opcional) 1..1000");
        check("altura BST con 1..1000", 999, b2.altura());
        check("altura AVL con 1..1000", 9, a2.altura());
        check("AVL 1..1000 balanceado", true, a2.balanceado());
    }

    // ------------------------------------------------------------------ Caso F
    /** Caso F: heap con las prioridades [4,10,3,5,1,8,9,2], extraer la raíz y Heap-Sort ascendente. */
    static void casoF() {
        System.out.println("=== CASO F: Heap y Heap-Sort ===");
        Registro[] regs = desdePrioridades(new int[]{4, 10, 3, 5, 1, 8, 9, 2});
        Max_heap_R heap = new Max_heap_R();
        heap.construir(regs);
        System.out.println("  heap (id(prioridad)) tras buildheap: " + heap);
        check("prioridad de la raiz", 10, heap.max().prioridad);
        check("prioridad extraida", 10, heap.extractMax().prioridad);
        check("nueva raiz tras extraer", 9, heap.max().prioridad);
        Registro[] asc = Max_heap_R.heapSort(regs, regs.length);
        check("Heap-Sort ascendente", "[1, 2, 3, 4, 5, 8, 9, 10]", prios(asc, asc.length));
        check("la entrada no se modifica", "[4, 10, 3, 5, 1, 8, 9, 2]", prios(regs, regs.length));
    }

    // ------------------------------------------- Anexo B (preguntas de ejemplo)
    /** Reproduce las respuestas esperadas de las preguntas de sustentación 1 a 8 del Anexo B. */
    static void extrasAnexoB() {
        System.out.println("=== PRUEBAS ADICIONALES: preguntas de sustentacion del Anexo B ===");
        AVL avl = new AVL();
        avl.insercion(reg(50, 1)); avl.insercion(reg(20, 1)); avl.insercion(reg(40, 1));
        System.out.println("P1 AVL 50,20,40: " + avl.rotaciones());
        check("P1 forma", "40(20,50)", avl.estructura());
        check("P1 rotacion", "LR (izquierda en 20, derecha en 50)", avl.rotaciones().get(0));

        Max_heap_R h = new Max_heap_R();
        h.construir(desdePrioridades(new int[]{10, 5, 9, 4, 1, 8, 3, 2}));
        check("P2 extraer", 10, h.extractMax().prioridad);
        check("P2 heap resultante (id(prioridad))", "[3(9), 2(5), 6(8), 4(4), 5(1), 8(2), 7(3)]", h.toString());
        h.insert(reg(99, 7));
        check("P3 tras insertar 7", "[3(9), 99(7), 6(8), 2(5), 5(1), 8(2), 7(3), 4(4)]", h.toString());

        BST bst = new BST();
        int[] claves = {5, 3, 8, 1, 4, 9};
        for (int id : claves) bst.insercion(reg(id, 1));
        bst.eliminar(5);
        check("P8 BST eliminar 5 (sucesor)", "8(3(1,4),9)", bst.estructura());

        Sistema s = new Sistema();
        s.ejecutar("REGISTRAR 50 A 7 0");
        s.ejecutar("REGISTRAR 30 B 9 50");
        s.ejecutar("REGISTRAR 40 C 9 50");
        check("P4 reporte", "30(9) 40(9) 50(7)", s.ejecutar("REPORTE"));
        check("P4 primer SIGUIENTE", "ATENDIDO: 30 (B)", s.ejecutar("SIGUIENTE"));

        s = new Sistema();
        s.ejecutar("REGISTRAR 10 A 2 0");
        s.ejecutar("REGISTRAR 20 B 5 10");
        s.ejecutar("REGISTRAR 30 C 3 10");
        s.ejecutar("SIGUIENTE");
        check("P6 CANCELAR 20", "CANCELADOS: 0", s.ejecutar("CANCELAR 20"));
        check("P6 reporte", "30(3) 10(2)", s.ejecutar("REPORTE"));
        check("P5 CANCELAR 10", "CANCELADOS: 2", s.ejecutar("CANCELAR 10"));
        check("P5 CONSULTAR 20", "ID=20 NOMBRE=B PRIORIDAD=5\nPADRE=10 ESTADO=ATENDIDO", s.ejecutar("CONSULTAR 20"));

        s = new Sistema();
        s.ejecutar("REGISTRAR 10 A 2 0");
        s.ejecutar("REGISTRAR 20 B 5 10");
        s.ejecutar("REGISTRAR 30 C 3 10");
        s.ejecutar("REGISTRAR 40 D 1 20");
        check("P7 estadisticas", "Preorden: 10,20,40,30\nPostorden: 40,20,30,10\nPor niveles: 10,20,30,40\n"
                + "Altura arbol general: 2\nHojas: 2\nAltura BST: 3\nAltura AVL: 2\nPendientes: 4", s.ejecutar("ESTADISTICAS"));
    }

    // ------------------------------------------- Anexo A (sesión del ejemplo)
    /** Reproduce la sesión del ejemplo del Anexo A (errores, reporte, cancelación y estadísticas). */
    static void extrasAnexoA() {
        System.out.println("=== PRUEBAS ADICIONALES: sesion del ejemplo del Anexo A ===");
        Sistema s = new Sistema();
        String[] datos = {"10 RevisionGeneral 3 0", "20 Frenos 8 10", "30 Transmision 5 10", "40 Pastillas 9 20",
                "50 CableFreno 6 20", "60 Cadena 8 30", "70 Pinon 4 30", "80 Lavado 2 10"};
        for (String d : datos) s.ejecutar("REGISTRAR " + d);
        check("duplicado", "ERROR: ID duplicado (20)", s.ejecutar("REGISTRAR 20 Duplicado 5 10"));
        check("segunda raiz", "ERROR: ya existe una raiz (10)", s.ejecutar("REGISTRAR 90 Llantas 7 0"));
        check("padre inexistente", "ERROR: el padre 77 no existe", s.ejecutar("REGISTRAR 95 Timbre 4 77"));
        check("prioridad invalida", "ERROR: prioridad invalida (15)", s.ejecutar("REGISTRAR 96 Sillin 15 10"));
        check("reporte inicial", "40(9) 20(8) 60(8) 50(6) 30(5) 70(4) 10(3) 80(2)", s.ejecutar("REPORTE"));
        check("SIGUIENTE", "ATENDIDO: 40 (Pastillas)", s.ejecutar("SIGUIENTE"));
        check("CANCELAR 30", "CANCELADOS: 3", s.ejecutar("CANCELAR 30"));
        check("reporte", "20(8) 50(6) 10(3) 80(2)", s.ejecutar("REPORTE"));
        check("SIGUIENTE", "ATENDIDO: 20 (Frenos)", s.ejecutar("SIGUIENTE"));
        check("CANCELAR 20", "CANCELADOS: 1", s.ejecutar("CANCELAR 20"));
        check("CONSULTAR 60 cancelado", "ID=60 NOMBRE=Cadena PRIORIDAD=8\nPADRE=30 ESTADO=CANCELADO", s.ejecutar("CONSULTAR 60"));
        check("ESTADISTICAS", "Preorden: 10,20,40,50,30,60,70,80\nPostorden: 40,50,20,60,70,30,80,10\n"
                + "Por niveles: 10,20,30,80,40,50,60,70\nAltura arbol general: 2\nHojas: 5\n"
                + "Altura BST: 7\nAltura AVL: 3\nPendientes: 2", s.ejecutar("ESTADISTICAS"));
        check("CONSULTAR id inexistente", "ERROR: el id 77 no existe", s.ejecutar("CONSULTAR 77"));
        check("CANCELAR id inexistente", "ERROR: el id 77 no existe", s.ejecutar("CANCELAR 77"));
        // el 50 quedó cancelado con CANCELAR 20: solo quedan pendientes el 10 y el 80
        check("SIGUIENTE (queda el 10)", "ATENDIDO: 10 (RevisionGeneral)", s.ejecutar("SIGUIENTE"));
        check("SIGUIENTE (queda el 80)", "ATENDIDO: 80 (Lavado)", s.ejecutar("SIGUIENTE"));
        check("SIGUIENTE sin pendientes", "SIN PENDIENTES", s.ejecutar("SIGUIENTE"));
    }

    // ------------------------------------------------------------ Aleatorias
    /** 4000 operaciones aleatorias en BST y AVL contra un oráculo simple (arreglo de booleanos) y Heap-Sort de 500 registros. */
    static void aleatorias() {
        System.out.println("=== PRUEBAS ADICIONALES: aleatorias contra un oraculo simple ===");
        final int N = 200;
        boolean[] presente = new boolean[N + 1];
        BST bst = new BST();
        AVL avl = new AVL();
        boolean todoOk = true;
        for (int op = 0; op < 4000; op++) {
            int id = 1 + aleatorio(N);
            if (aleatorio(100) < 60) {
                boolean nuevo = !presente[id];
                todoOk &= (bst.insercion(reg(id, 1)) == nuevo) && (avl.insercion(reg(id, 1)) == nuevo);
                presente[id] = true;
            } else {
                boolean existe = presente[id];
                todoOk &= (bst.eliminar(id) == existe) && (avl.eliminar(id) == existe);
                presente[id] = false;
            }
            if (!avl.balanceado()) todoOk = false;
        }
        StringBuilder esperado = new StringBuilder("[");
        boolean primero = true;
        for (int i = 1; i <= N; i++) {
            if (presente[i]) {
                if (!primero) esperado.append(", ");
                esperado.append(i);
                primero = false;
            }
        }
        esperado.append("]");
        check("4000 operaciones: resultados de insertar/eliminar y balance AVL en cada paso", true, todoOk);
        check("inorden BST = oraculo", esperado.toString(), bst.inOrder().toString());
        check("inorden AVL = oraculo", esperado.toString(), avl.inOrder().toString());

        Max_heap_R g = new Max_heap_R();
        for (int i = 1; i <= 500; i++) g.insert(reg(i, 1 + aleatorio(10)));
        Registro[] hs = g.ordenados();
        boolean ok = true;
        int k = hs.length - 1;
        Registro prev = null;
        while (!g.isEmpty()) {
            Registro t = g.extractMax();
            if (t != hs[k--]) ok = false;
            if (prev != null && !Registro.desempate(prev, t)) ok = false;
            prev = t;
        }
        check("500 registros: extraer todo el heap sale en orden y coincide con Heap-Sort al reves", true, ok);
    }

    /** Ejecuta todas las pruebas e imprime el resumen; termina con código 1 si alguna falló. */
    public static void main(String[] args) {
        casoD();
        casoE();
        casoF();
        extrasAnexoB();
        extrasAnexoA();
        aleatorias();
        System.out.println();
        System.out.println("RESUMEN: " + (total - fallas) + "/" + total + " comprobaciones correctas");
        if (fallas > 0) System.exit(1);
    }
}
