package ecg.ml;

import ecg.modelo.AnotacionECG;
import ecg.modelo.CanalECG;
import ecg.modelo.RegistroECG;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Pegamento entre el Random Forest de {@code ecg.ml} y la aplicación: entrena un {@link BosqueSimple}
 * con la partición DS1 de MIT-BIH y clasifica los latidos de un {@link RegistroECG} ya cargado.
 *
 * <p>El modelo vive en memoria durante la sesión (el bosque no es serializable). El dataset de
 * características sí se cachea en texto ({@link Datos#guardar}) para no releer los WFDB cada vez.
 */
public final class ServicioClasificacion {

    /** Hiperparámetros del bosque. */
    public static final class Config {
        public int arboles = 25;
        public int minHoja = 20;
        public long semilla = 0;
        public Path dirDatos = Path.of("data", "MIT-BIH Arrhythmia Database");
        public Path cacheDataset = Path.of("ml", "dataset", "ds1.txt");
        public Path modelo = Path.of("ml", "modelo", "bosque.rf");
    }

    /** Clasificación de un latido concreto del registro abierto. */
    public static final class LatidoClasificado {
        public final int indiceMuestra;   // posición R (recentrada)
        public final int clase;           // 0..3 (N,S,V,F)
        public final double[] probabilidades;  // una por clase, suma ~1
        public final int claseReal;       // 0..3 si hay anotación de referencia; -1 si no
        public final int canalesVotantes; // nº de derivaciones que aportaron a la decisión

        LatidoClasificado(int indiceMuestra, int clase, double[] probabilidades, int claseReal,
                          int canalesVotantes) {
            this.indiceMuestra = indiceMuestra;
            this.clase = clase;
            this.probabilidades = probabilidades;
            this.claseReal = claseReal;
            this.canalesVotantes = canalesVotantes;
        }

        public String etiqueta() { return Datos.CLASES[clase]; }
        public double probabilidadMaxima() { return probabilidades[clase]; }
    }

    /** Resultado completo de clasificar un registro. */
    public static final class ResultadoClasificacion {
        public final List<LatidoClasificado> latidos;
        public final int[][] matrizConfusion;   // null si no hay etiquetas de referencia
        public final int sinVentana;            // latidos descartados por ventana incompleta
        public final String canalUsado;         // descripción del/los canal(es) usados

        ResultadoClasificacion(List<LatidoClasificado> latidos, int[][] matriz, int sinVentana, String canal) {
            this.latidos = latidos;
            this.matrizConfusion = matriz;
            this.sinVentana = sinVentana;
            this.canalUsado = canal;
        }

        public boolean tieneReferencia() { return matrizConfusion != null; }
    }

    private BosqueSimple bosque;
    private int latidosEntrenamiento;
    private int[] soportePorClase;

    public boolean hayModelo() { return bosque != null; }
    public int getLatidosEntrenamiento() { return latidosEntrenamiento; }
    public int[] getSoportePorClase() { return soportePorClase; }

    /**
     * Carga un modelo previamente entrenado y guardado en disco, si existe. Permite reutilizar el
     * bosque entre arranques sin volver a entrenar.
     *
     * @return true si se cargó un modelo; false si el archivo no existe
     */
    public boolean cargarModelo(Path ruta) throws IOException {
        if (ruta == null || !Files.isRegularFile(ruta)) {
            return false;
        }
        this.bosque = BosqueSimple.cargar(ruta);
        return true;
    }

    /**
     * Entrena el bosque con DS1. Operación pesada (segundos); conviene ejecutarla fuera del EDT.
     *
     * @param progreso callback opcional (0..1) tras cada árbol; puede ser {@code null}
     * @return texto con el resumen del entrenamiento
     */
    public String entrenar(Config cfg, java.util.function.DoubleConsumer progreso) throws IOException {
        Datos.Dataset ds1 = cargarDatasetDS1(cfg);
        if (ds1.x.length == 0) {
            throw new IOException("DS1 no produjo ningún latido; revisa los registros de MIT-BIH en "
                    + cfg.dirDatos);
        }
        latidosEntrenamiento = ds1.x.length;
        int nClases = Datos.CLASES.length;
        soportePorClase = new int[nClases];
        for (int y : ds1.y) soportePorClase[y]++;

        List<Muestra> muestras = new ArrayList<>(ds1.x.length);
        for (int i = 0; i < ds1.x.length; i++) muestras.add(new Muestra(ds1.x[i], ds1.y[i]));

        int maxCaract = (int) Math.sqrt(ds1.x[0].length);   // ~16 de 257
        BosqueSimple nuevo = new BosqueSimple(cfg.arboles, nClases, maxCaract, cfg.minHoja, cfg.semilla);
        nuevo.entrenar(muestras, progreso);
        this.bosque = nuevo;

        boolean guardado = false;
        if (cfg.modelo != null) {
            try {
                if (cfg.modelo.getParent() != null) Files.createDirectories(cfg.modelo.getParent());
                nuevo.guardar(cfg.modelo);
                guardado = true;
            } catch (IOException ignorado) {
                // guardar es una conveniencia; si falla, el modelo sigue en memoria para esta sesión.
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Entrenado con DS1: %d latidos x %d características%n",
                ds1.x.length, ds1.x[0].length));
        sb.append(String.format("%d árboles · %d características por nodo · min_hoja=%d · semilla=%d%n",
                cfg.arboles, maxCaract, cfg.minHoja, cfg.semilla));
        sb.append("latidos por clase: ");
        for (int c = 0; c < nClases; c++) {
            sb.append(Datos.CLASES[c]).append('=').append(soportePorClase[c]).append("  ");
        }
        if (guardado) {
            sb.append(String.format("%nModelo guardado en %s", cfg.modelo));
        }
        return sb.toString();
    }

    /** Carga DS1 de la caché de texto si existe; si no, lo construye leyendo los WFDB y lo guarda. */
    private Datos.Dataset cargarDatasetDS1(Config cfg) throws IOException {
        if (cfg.cacheDataset != null && Files.isRegularFile(cfg.cacheDataset)) {
            return Datos.cargar(cfg.cacheDataset);
        }
        if (!Files.isDirectory(cfg.dirDatos)) {
            throw new IOException("no existe el directorio de MIT-BIH: " + cfg.dirDatos
                    + " (necesario para entrenar el Random Forest)");
        }
        Datos.Dataset ds1 = Datos.construir(Datos.DS1, cfg.dirDatos);
        if (cfg.cacheDataset != null) {
            try {
                Files.createDirectories(cfg.cacheDataset.getParent());
                Datos.guardar(ds1, cfg.cacheDataset);
            } catch (IOException ignorado) {
                // la caché es una optimización; si falla, el entrenamiento ya está hecho.
            }
        }
        return ds1;
    }

    /**
     * Clasifica los latidos del registro abierto sobre el canal preferido (MLII, o el primero).
     * Equivale a {@link #clasificar(RegistroECG, int[], boolean)} con una sola derivación.
     */
    public ResultadoClasificacion clasificar(RegistroECG registro, int[] posiciones) {
        return clasificar(registro, posiciones, false);
    }

    /**
     * Clasifica los latidos del registro abierto. Para cada latido con ventana completa predice su
     * clase; si hay anotación de referencia en esa posición, también guarda la clase real.
     *
     * @param registro         registro cargado
     * @param posiciones       índices de muestra de los latidos (R), en orden ascendente
     * @param todosLosCanales  si es true, clasifica el latido en TODAS las derivaciones y promedia
     *                         sus distribuciones (voto blando entre canales), igual que la fusión de
     *                         Pan-Tompkins. Si es false, usa solo el canal preferido (MLII/primero).
     */
    public ResultadoClasificacion clasificar(RegistroECG registro, int[] posiciones, boolean todosLosCanales) {
        exigirModelo();
        int nCanales = registro.getNumeroCanales();
        int nClases = Datos.CLASES.length;

        // Canales a usar: todos, o solo el preferido.
        int[] canales;
        if (todosLosCanales && nCanales > 1) {
            canales = new int[nCanales];
            for (int i = 0; i < nCanales; i++) canales[i] = i;
        } else {
            canales = new int[]{indiceCanalPreferido(registro)};
        }

        // Señal, características y centros (posiciones R recentradas) por canal.
        double[][] senalPorCanal = new double[canales.length][];
        double[][][] filasPorCanal = new double[canales.length][][];
        for (int k = 0; k < canales.length; k++) {
            double[] x = registro.getCanal(canales[k]).getMuestras();
            senalPorCanal[k] = x;
            filasPorCanal[k] = Caracteristicas.extraer(x, posiciones);
        }
        // El centro de referencia para posicionar la marca sale del canal preferido (el primero de la lista).
        double[] senalRef = senalPorCanal[0];

        // Mapa posición de referencia -> clase real (si el registro trae .atr).
        java.util.Map<Integer, Integer> real = clasesReales(registro);
        boolean hayReferencia = !real.isEmpty();

        List<LatidoClasificado> latidos = new ArrayList<>();
        List<Integer> yReal = new ArrayList<>();
        List<Integer> yPred = new ArrayList<>();
        int sinVentana = 0;
        for (int i = 0; i < posiciones.length; i++) {
            // Promedia las distribuciones de todos los canales con ventana completa para este latido.
            double[] acum = new double[nClases];
            int votantes = 0;
            for (int k = 0; k < canales.length; k++) {
                double[] fila = filasPorCanal[k][i];
                if (fila == null) continue;
                double[] dist = bosque.predecirDistribucion(fila);
                for (int c = 0; c < nClases; c++) acum[c] += dist[c];
                votantes++;
            }
            if (votantes == 0) { sinVentana++; continue; }   // ninguna derivación tenía ventana completa
            for (int c = 0; c < nClases; c++) acum[c] /= votantes;

            int clase = argmax(acum);
            int centro = Caracteristicas.recentrar(senalRef, posiciones[i]);
            int cReal = hayReferencia ? real.getOrDefault(centro, cercano(real, centro)) : -1;
            latidos.add(new LatidoClasificado(centro, clase, acum, cReal, votantes));
            if (cReal >= 0) { yReal.add(cReal); yPred.add(clase); }
        }

        int[][] matriz = null;
        if (!yReal.isEmpty()) {
            int[] r = new int[yReal.size()];
            int[] p = new int[yPred.size()];
            for (int i = 0; i < r.length; i++) { r[i] = yReal.get(i); p[i] = yPred.get(i); }
            matriz = Metricas.matrizConfusion(r, p);
        }

        String descripcionCanal;
        if (canales.length == 1) {
            descripcionCanal = registro.getCanal(canales[0]).getNombre();
        } else {
            descripcionCanal = nCanales + " derivaciones (voto entre canales)";
        }
        return new ResultadoClasificacion(latidos, matriz, sinVentana, descripcionCanal);
    }

    // ---- ayudantes ----

    /** Prefiere el canal MLII (como el entrenamiento); si no existe, usa el primero. */
    private static int indiceCanalPreferido(RegistroECG reg) {
        for (int i = 0; i < reg.getNumeroCanales(); i++) {
            if (Datos.CANAL.equalsIgnoreCase(reg.getCanal(i).getNombre())) return i;
        }
        return 0;
    }

    /** Posiciones de referencia (.atr) -> índice de clase AAMI (0..3); vacío si no hay referencia. */
    private static java.util.Map<Integer, Integer> clasesReales(RegistroECG reg) {
        java.util.Map<Integer, Integer> m = new java.util.HashMap<>();
        List<AnotacionECG> ref = reg.getAnotaciones(AnotacionECG.FUENTE_REFERENCIA);
        for (AnotacionECG a : ref) {
            int clase = Datos.indiceClaseAAMI(a.getTipo());
            if (clase >= 0) m.put((int) a.getIndiceMuestra(), clase);
        }
        return m;
    }

    /** Clase real de la anotación de referencia más próxima (±25 ms a 360 Hz); -1 si ninguna. */
    private static int cercano(java.util.Map<Integer, Integer> real, int centro) {
        int tol = 9;   // ~25 ms a 360 Hz: cubre el recentrado de ±50 ms de forma conservadora
        for (int d = 0; d <= tol; d++) {
            Integer c = real.get(centro - d);
            if (c != null) return c;
            c = real.get(centro + d);
            if (c != null) return c;
        }
        return -1;
    }

    private static int argmax(double[] v) {
        int mejor = 0;
        for (int i = 1; i < v.length; i++) if (v[i] > v[mejor]) mejor = i;
        return mejor;
    }

    private void exigirModelo() {
        if (bosque == null) throw new IllegalStateException("el Random Forest no está entrenado todavía");
    }
}
