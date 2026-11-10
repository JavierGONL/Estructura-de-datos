package ecg.ml;

import ecg.ml.estructuras.CircularQueue;
import ecg.ml.estructuras.DynamicArray;
import ecg.ml.estructuras.MinHeap;
import ecg.ml.estructuras.MovingMean;
import ecg.ml.estructuras.TreeNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de las estructuras propias contra las colecciones estándar (espejo de test_estructuras.py). */
class EstructurasTest {

    @Test
    void dynamicArrayContraArrayList() {
        Random rng = new Random(1);
        DynamicArray<Double> da = new DynamicArray<>(1);
        List<Double> ref = new ArrayList<>();
        // operaciones soportadas por esta DynamicArray: add, set, removeIdx
        String ops = "aaas r".replace(" ", "");   // a=add, s=set, r=removeIdx
        for (int paso = 0; paso < 6000; paso++) {
            char op = ops.charAt(rng.nextInt(ops.length()));
            if (op == 'a') {
                double x = rng.nextDouble();
                da.add(x);
                ref.add(x);
            } else if (op == 's' && !ref.isEmpty()) {
                int i = rng.nextInt(ref.size());
                double x = rng.nextDouble();
                da.set(i, x);
                ref.set(i, x);
            } else if (op == 'r' && !ref.isEmpty()) {
                int i = rng.nextInt(ref.size());
                da.removeIdx(i);
                ref.remove(i);
            }
            assertEquals(ref.size(), da.length());
        }
        // misma secuencia de elementos
        for (int i = 0; i < ref.size(); i++) {
            assertEquals(ref.get(i), da.get(i));
        }
    }

    @Test
    void dynamicArrayAccesoYBusqueda() {
        DynamicArray<Integer> da = new DynamicArray<>(2);
        for (int i = 0; i < 1000; i++) {
            da.add(i);
        }
        assertEquals(1000, da.length());
        assertEquals(0, da.get(0));
        assertEquals(999, da.get(999));
        assertTrue(da.search(500));
        assertFalse(da.search(5000));
        da.set(0, -1);
        assertEquals(-1, da.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> da.get(1000));
    }

    @Test
    void dynamicArrayEliminarYReset() {
        DynamicArray<Integer> da = new DynamicArray<>(4);
        for (int i = 0; i < 10; i++) {
            da.add(i);
        }
        assertEquals(0, da.eliminarPrimero());   // saca el primero
        assertEquals(1, da.get(0));
        assertEquals(9, da.length());
        da.remove(5);                            // borra el valor 5
        assertFalse(da.search(5));
        assertEquals(8, da.length());
        da.reset();
        assertTrue(da.isEmpty());
        assertEquals(0, da.length());
    }

    @Test
    void circularQueueContraDeque() {
        Random rng = new Random(2);
        CircularQueue<Double> q = new CircularQueue<>(7);
        Deque<Double> ref = new ArrayDeque<>();
        for (int paso = 0; paso < 5000; paso++) {
            if (rng.nextDouble() < 0.55) {
                double x = rng.nextDouble();
                if (q.isFull()) {
                    assertThrows(IllegalStateException.class, () -> q.enqueue(0.0));
                } else {
                    q.enqueue(x);
                    ref.addLast(x);
                }
            } else if (!ref.isEmpty()) {
                assertEquals(ref.pollFirst(), q.dequeue());
            }
            assertEquals(ref.size(), q.size());
        }
    }

    @Test
    void mediaMovil() {
        Random rng = new Random(3);
        MovingMean m = new MovingMean(10);
        Deque<Double> ref = new ArrayDeque<>();
        assertTrue(Double.isNaN(m.mean()));
        for (int i = 0; i < 100_000; i++) {
            double x = 0.3 + rng.nextDouble() * 1.2;
            m.push(x);
            ref.addLast(x);
            if (ref.size() > 10) {
                ref.pollFirst();
            }
        }
        double suma = 0;
        for (double v : ref) {
            suma += v;
        }
        assertEquals(suma / ref.size(), m.mean(), 1e-9);
    }

    @Test
    void heapContraPriorityQueue() {
        Random rng = new Random(4);
        MinHeap<Double, Double> h = MinHeap.of(x -> x);
        PriorityQueue<Double> ref = new PriorityQueue<>();
        for (int i = 0; i < 2000; i++) {
            double x = rng.nextDouble();
            h.push(x);
            ref.add(x);
        }
        while (!ref.isEmpty()) {
            assertEquals(ref.poll(), h.pop());
        }
    }

    @Test
    void heapClaveYEmpatesFifo() {
        MinHeap<String[], Integer> h = new MinHeap<>(t -> Integer.parseInt(t[0]), Comparator.<Integer>naturalOrder());
        h.push(new String[]{"2", "a"});
        h.push(new String[]{"1", "b"});
        h.push(new String[]{"2", "c"});
        h.push(new String[]{"1", "d"});
        String salida = h.pop()[1] + h.pop()[1] + h.pop()[1] + h.pop()[1];
        assertEquals("bdac", salida);
    }

    @Test
    void heapReplace() {
        MinHeap<Integer, Integer> h = MinHeap.of(x -> x);
        for (int x : new int[]{5, 3, 8}) {
            h.push(x);
        }
        assertEquals(3, h.replace(10));
        assertEquals(5, h.pop());
        assertEquals(8, h.pop());
        assertEquals(10, h.pop());
    }

    @Test
    void topKContraOrden() {
        Random rng = new Random(6);
        List<int[]> datos = new ArrayList<>();
        for (int i = 0; i < 3000; i++) {
            datos.add(new int[]{rng.nextInt(101), i});
        }
        for (int k : new int[]{0, 1, 7, 50, 5000}) {
            List<int[]> esperado = new ArrayList<>(datos);
            // orden estable descendente por clave
            esperado.sort((a, b) -> Integer.compare(b[0], a[0]));
            List<int[]> obtenido = MinHeap.topK(datos, k, t -> t[0]);
            int lim = Math.min(k, datos.size());
            assertEquals(lim, obtenido.size());
            for (int i = 0; i < lim; i++) {
                assertEquals(esperado.get(i)[0], obtenido.get(i)[0]);
                assertEquals(esperado.get(i)[1], obtenido.get(i)[1], "mismo desempate por llegada");
            }
        }
    }

    @Test
    void metricasArbol() {
        TreeNode hojaA = new TreeNode(new int[]{1, 0, 0, 0});
        TreeNode hojaB = new TreeNode(new int[]{1, 0, 0, 0});
        TreeNode hojaC = new TreeNode(new int[]{1, 0, 0, 0});
        TreeNode interno = new TreeNode(7, 1.0, hojaA, hojaB);
        TreeNode raiz = new TreeNode(3, 0.5, interno, hojaC);
        assertEquals(3, TreeNode.height(raiz));
        assertEquals(5, TreeNode.countNodes(raiz));
        assertEquals(3, TreeNode.countLeaves(raiz));
        assertTrue(TreeNode.isFull(raiz));
        assertFalse(TreeNode.isFull(new TreeNode(0, 0.0, new TreeNode(new int[]{1, 0, 0, 0}), null)));
        assertEquals(0, TreeNode.height(null));
        assertEquals(0, TreeNode.countNodes(null));
    }
}
