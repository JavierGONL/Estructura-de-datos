package ecg.ml;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del clasificador SIMPLE de {@code ecg.ml}: extracción de características, entropía, muestreo,
 * árbol, bosque y métricas. (Versión acorde a la implementación simple actual; la versión completa con
 * SplitMix64/Gini/árbol plano/serialización ya no forma parte del paquete.)
 */
class ModeloMLTest {

    // ---- Caracteristicas ----

    @Test
    void caracteristicasVectorDe257YVentanaCentrada() {
        // señal plana con un pico claro: la ventana debe tener N_CARACT valores y recentrarse al pico.
        int n = 2000;
        double[] x = new double[n];
        int pico = 500;
        x[pico] = 5.0;                                   // un único máximo
        int centro = Caracteristicas.recentrar(x, pico - 3);
        assertEquals(pico, centro, "recentrar lleva al mayor |x - mediana|");

        int[] pos = {pico, 900, 1300};
        double[][] filas = Caracteristicas.extraer(x, pos);
        assertEquals(pos.length, filas.length);
        for (double[] f : filas) {
            assertNotNull(f, "la ventana cabe en la señal");
            assertEquals(Caracteristicas.N_CARACT, f.length);
            assertEquals(257, Caracteristicas.N_CARACT);
        }
    }

    @Test
    void caracteristicasDescartaVentanaQueNoCabe() {
        double[] x = new double[300];
        x[5] = 3.0;                                       // pico pegado al inicio: la ventana no cabe
        double[][] filas = Caracteristicas.extraer(x, new int[]{5});
        assertEquals(1, filas.length);
        assertNull(filas[0], "sin ventana completa -> fila null");
    }

    @Test
    void caracteristicasRRenSegundos() {
        // dos latidos separados ~1 s (360 muestras a 360 Hz): rr_prev del segundo debe salir ~1,0 s.
        int n = 4000;
        double[] x = new double[n];
        int p1 = 1000;
        int p2 = p1 + 360;
        x[p1] = 4.0;
        x[p2] = 4.0;
        double[][] filas = Caracteristicas.extraer(x, new int[]{p1, p2});
        assertNotNull(filas[1]);
        double rrPrev = filas[1][Caracteristicas.N_VENTANA];   // [252] rr_prev en segundos
        assertEquals(1.0, rrPrev, 0.1, "rr_prev ~ 1 s");
    }

    // ---- Entropia ----

    @Test
    void entropiaPuraYMaxima() {
        List<Muestra> puras = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            puras.add(new Muestra(new double[]{0.0}, 0));
        }
        assertEquals(0.0, Entropia.entropia(puras, 4), 1e-12, "grupo puro -> entropía 0");

        List<Muestra> uniforme = new ArrayList<>();
        for (int c = 0; c < 4; c++) {
            uniforme.add(new Muestra(new double[]{0.0}, c));
        }
        assertEquals(2.0, Entropia.entropia(uniforme, 4), 1e-12, "4 clases equiprobables -> 2 bits");

        assertEquals(0.0, Entropia.entropia(new ArrayList<>(), 4), 1e-12, "vacío -> 0");
    }

    // ---- Muestreo ----

    @Test
    void bootstrapDevuelveNIndicesEnRango() {
        int n = 200;
        int[] idx = Muestreo.bootstrap(n, new Random(0));
        assertEquals(n, idx.length);
        for (int v : idx) {
            assertTrue(v >= 0 && v < n);
        }
    }

    @Test
    void muestreoSinReemplazoSinRepetidos() {
        int[] m = Muestreo.muestraSinReemplazo(50, 10, new Random(7));
        assertEquals(10, m.length);
        boolean[] visto = new boolean[50];
        for (int v : m) {
            assertFalse(visto[v], "sin índices repetidos");
            visto[v] = true;
            assertTrue(v >= 0 && v < 50);
        }
    }

    @Test
    void muestreoSinReemplazoRecortaK() {
        int[] m = Muestreo.muestraSinReemplazo(5, 100, new Random(0));
        assertEquals(5, m.length, "k se recorta al total disponible");
    }

    // ---- ArbolDesicion ----

    @Test
    void arbolMemorizaEntrenamiento() {
        Random rng = new Random(0);
        int n = 300;
        int m = 6;
        List<Muestra> muestras = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double[] f = new double[m];
            for (int j = 0; j < m; j++) {
                f[j] = rng.nextGaussian();
            }
            muestras.add(new Muestra(f, rng.nextInt(4)));
        }
        // sin límite de hoja (min=1) y mirando todas las características, un árbol memoriza su entrenamiento
        ArbolDesicion arbol = new ArbolDesicion(4, m, 1, new Random(0));
        arbol.entrenar(muestras);
        int iguales = 0;
        for (Muestra mu : muestras) {
            if (argmax(arbol.predecir(mu.caracteristicas())) == mu.etiqueta()) {
                iguales++;
            }
        }
        assertTrue(iguales >= (int) (0.95 * n), "un árbol profundo reproduce casi todo su entrenamiento");
    }

    // ---- BosqueSimple ----

    private List<Muestra> datos;

    private void generar() {
        Random rng = new Random(1);
        int n = 500;
        int m = 8;
        datos = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double[] f = new double[m];
            for (int j = 0; j < m; j++) {
                f[j] = rng.nextGaussian();
            }
            double s = f[0] + 0.5 * f[1];
            int y = s < -0.5 ? 0 : (s < 0 ? 1 : (s < 0.8 ? 2 : 3));
            datos.add(new Muestra(f, y));
        }
    }

    @Test
    void bosqueMismaSemillaMismasPredicciones() {
        generar();
        BosqueSimple a = new BosqueSimple(20, 4, 3, 5, 0);
        BosqueSimple b = new BosqueSimple(20, 4, 3, 5, 0);
        a.entrenar(datos);
        b.entrenar(datos);
        for (Muestra mu : datos) {
            assertEquals(a.predecir(mu.caracteristicas()), b.predecir(mu.caracteristicas()),
                    "misma semilla -> mismo bosque -> mismas predicciones");
        }
    }

    @Test
    void bosqueDistribucionEsProbabilidad() {
        generar();
        BosqueSimple rf = new BosqueSimple(15, 4, 3, 5, 0);
        rf.entrenar(datos);
        for (Muestra mu : datos) {
            double[] dist = rf.predecirDistribucion(mu.caracteristicas());
            assertEquals(4, dist.length);
            double suma = 0;
            for (double p : dist) {
                assertTrue(p >= -1e-9 && p <= 1.0 + 1e-9);
                suma += p;
            }
            assertEquals(1.0, suma, 1e-9, "la distribución suma 1");
            assertEquals(argmax(dist), rf.predecir(mu.caracteristicas()), "predecir = argmax de la distribución");
        }
    }

    @Test
    void bosqueAprendeSenal() {
        generar();
        BosqueSimple rf = new BosqueSimple(25, 4, 3, 5, 0);
        rf.entrenar(datos);
        int aciertos = 0;
        for (Muestra mu : datos) {
            if (rf.predecir(mu.caracteristicas()) == mu.etiqueta()) {
                aciertos++;
            }
        }
        double acc = (double) aciertos / datos.size();
        assertTrue(acc > 0.6, "con señal aprendible, el bosque supera claramente al azar (acc=" + acc + ")");
    }

    // ---- Metricas ----

    @Test
    void metricasMatrizYF1() {
        int[] real = {0, 0, 1, 1, 2, 2, 3, 3};
        int[] pred = {0, 1, 1, 1, 2, 2, 3, 0};
        int[][] m = Metricas.matrizConfusion(real, pred);
        assertEquals(1, m[0][0]);
        assertEquals(1, m[0][1]);
        assertEquals(2, m[1][1]);
        assertEquals(0.75, Metricas.accuracy(m), 1e-9);  // 6 de 8 aciertos
        assertTrue(Metricas.f1Macro(m) > 0 && Metricas.f1Macro(m) <= 1);
    }

    private static int argmax(double[] v) {
        int mejor = 0;
        for (int i = 1; i < v.length; i++) {
            if (v[i] > v[mejor]) {
                mejor = i;
            }
        }
        return mejor;
    }
}
