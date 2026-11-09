package ecg.ml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Programa de ejemplo del {@link BosqueSimple}: entrena con DS1 y evalúa con DS2 los latidos ECG.
 *
 * <p>Reutiliza el dataset ya preparado ({@link Datos}) y las métricas ({@link Metricas}) de la
 * implementación completa, pero clasifica con la versión simple (entropía + corte por la media).
 *
 * <p>Uso:  java ecg.ml.simple.EjemploSimple [--arboles N] [--min-hoja L] [--semilla S] [--dataset dir]
 *
 * <p>Si existen ml/dataset/ds1.txt y ds2.txt (de {@code ecg.ml.ConstruirDataset}) se usan directamente;
 * si no, se construyen leyendo los registros MIT-BIH de data/.
 */
public final class EjemploSimple {

    private EjemploSimple() {
    }

    public static void main(String[] args) throws Exception {
        int nArboles = 50;
        int minHoja = 10;
        long semilla = 0;
        String datasetDir = "ml/dataset";

        for (int i = 0; i < args.length - 1; i++) {
            switch (args[i]) {
                case "--arboles" -> nArboles = Integer.parseInt(args[++i]);
                case "--min-hoja" -> minHoja = Integer.parseInt(args[++i]);
                case "--semilla" -> semilla = Long.parseLong(args[++i]);
                case "--dataset" -> datasetDir = args[++i];
                default -> { }
            }
        }

        // 1) cargar DS1 (entrenamiento) y DS2 (prueba).
        Datos.Dataset ds1 = cargar("ds1", Datos.DS1, datasetDir);
        Datos.Dataset ds2 = cargar("ds2", Datos.DS2, datasetDir);
        List<Muestra> entrenamiento = aMuestras(ds1);
        int nClases = Datos.CLASES.length;                       // 4: N, S, V, F
        int maxCaract = (int) Math.sqrt(ds1.x[0].length);        // raíz(257) ~ 16 características por nodo

        System.out.printf("entrenamiento (DS1): %d latidos x %d características%n", ds1.x.length, ds1.x[0].length);
        System.out.printf("prueba (DS2): %d latidos%n", ds2.x.length);
        System.out.printf("%d árboles, %d características por nodo, min_hoja=%d%n", nArboles, maxCaract, minHoja);

        // 2) entrenar el bosque simple.
        long t0 = System.nanoTime();
        BosqueSimple bosque = new BosqueSimple(nArboles, nClases, maxCaract, minHoja, semilla);
        bosque.entrenar(entrenamiento);
        System.out.printf("entrenado en %.0f s%n", (System.nanoTime() - t0) / 1e9);

        // 3) predecir DS2 y medir con la matriz de confusión.
        int[] pred = new int[ds2.x.length];
        for (int i = 0; i < ds2.x.length; i++) {
            pred[i] = bosque.predecir(ds2.x[i]);
        }
        int[][] matriz = Metricas.matrizConfusion(ds2.y, pred);
        System.out.println("\n=== bosque simple (entropía + corte por la media) ===");
        System.out.println(Metricas.reporte(matriz));
    }

    /** Convierte un {@link Datos.Dataset} (arreglos paralelos) a la lista de {@link Muestra}. */
    private static List<Muestra> aMuestras(Datos.Dataset ds) {
        List<Muestra> muestras = new ArrayList<>(ds.x.length);
        for (int i = 0; i < ds.x.length; i++) {
            muestras.add(new Muestra(ds.x[i], ds.y[i]));
        }
        return muestras;
    }

    private static Datos.Dataset cargar(String nombre, String[] regs, String dir) throws Exception {
        Path txt = Path.of(dir, nombre + ".txt");
        if (Files.isRegularFile(txt)) {
            System.out.println("usando " + txt);
            return Datos.cargar(txt);
        }
        System.out.println("construyendo " + nombre + " desde los registros MIT-BIH...");
        return Datos.construir(regs, Path.of("data/MIT-BIH Arrhythmia Database"));
    }
}
