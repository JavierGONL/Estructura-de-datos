package ecg.ml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ecg.lectura.CargadorRegistro;
import ecg.ml.estructuras.DynamicArray;
import ecg.modelo.AnotacionECG;
import ecg.modelo.CanalECG;
import ecg.modelo.RegistroECG;

/**
 * Acceso a MIT-BIH y construcción de los datasets DS1/DS2. Equivalente de datos.py + etapa4_dataset.py.
 *
 * <p>Lee los registros con {@link CargadorRegistro} (el mismo lector WFDB de la aplicación) y arma la
 * matriz de características con {@link ecg.ml.Caracteristicas}. Así el entrenamiento en Java no depende
 * de los archivos .npz generados en Python: parte directamente de los .hea/.dat/.atr.
 */
public final class Datos {

    public static final String[] CLASES = {"N", "S", "V", "F"};
    public static final double FS_MITBIH = 360.0;
    public static final String CANAL = "MLII";

    /** Partición por registro de de Chazal et al. (2004). */
    public static final String[] DS1 = {"101", "106", "108", "109", "112", "114", "115", "116", "118",
            "119", "122", "124", "201", "203", "205", "207", "208", "209", "215", "220", "223", "230"};
    public static final String[] DS2 = {"100", "103", "105", "111", "113", "117", "121", "123", "200",
            "202", "210", "212", "213", "214", "219", "221", "222", "228", "231", "232", "233", "234"};

    /** Símbolos que existen pero se excluyen de las 4 clases (AAMI "Q"). */
    private static final String SIMBOLOS_Q = "/fQ";

    /** Mapa AAMI símbolo WFDB -&gt; clase (N, S, V, F). */
    private static final Map<String, String> AAMI = new HashMap<>();

    static {
        for (char s : "NLRej".toCharArray()) {
            AAMI.put(String.valueOf(s), "N");
        }
        for (char s : "AaJS".toCharArray()) {
            AAMI.put(String.valueOf(s), "S");
        }
        for (char s : "VE".toCharArray()) {
            AAMI.put(String.valueOf(s), "V");
        }
        AAMI.put("F", "F");
    }

    private Datos() {
    }

    /** Índice de la clase (0..3) o -1 si el símbolo no es una de las 4 clases AAMI. */
    public static int indiceClaseAAMI(String simbolo) {
        String clase = AAMI.get(simbolo);
        if (clase == null) {
            return -1;
        }
        for (int i = 0; i < CLASES.length; i++) {
            if (CLASES[i].equals(clase)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean esLatido(String simbolo) {
        return AAMI.containsKey(simbolo) || SIMBOLOS_Q.contains(simbolo);
    }

    /** Un dataset ya en memoria: matriz de características, etiquetas, registro y posición de cada latido. */
    public static final class Dataset {
        public final double[][] x;
        public final int[] y;
        public final String[] registro;
        public final int[] posicion;
        public final int descartados;

        Dataset(double[][] x, int[] y, String[] registro, int[] posicion, int descartados) {
            this.x = x;
            this.y = y;
            this.registro = registro;
            this.posicion = posicion;
            this.descartados = descartados;
        }
    }

    /** Localiza el .hea de un registro dentro del directorio (y sus subcarpetas). */
    public static Path localizar(String nombre, Path directorio) throws IOException {
        Path directo = directorio.resolve(nombre + ".hea");
        if (Files.isRegularFile(directo)) {
            return directo;
        }
        try (var stream = Files.walk(directorio)) {
            return stream.filter(p -> p.getFileName().toString().equals(nombre + ".hea"))
                    .findFirst().orElse(null);
        }
    }

    private static int indiceCanal(RegistroECG reg, String nombre) {
        for (int i = 0; i < reg.getNumeroCanales(); i++) {
            if (reg.getCanal(i).getNombre().equals(nombre)) {
                return i;
            }
        }
        throw new IllegalArgumentException("el registro " + reg.getNombreRegistro()
                + " no tiene el canal " + nombre);
    }

    /**
     * Filas de características de un registro: solo latidos N/S/V/F con ventana completa. Los latidos
     * fuera de las 4 clases (/ f Q) no se etiquetan, pero SÍ cuentan para los intervalos RR.
     *
     * @return {@code {X, y, posiciones, descartados}} dentro de un {@link Dataset} parcial (registro = null)
     */
    static Dataset filasRegistro(RegistroECG reg) {
        CanalECG canal = reg.getCanal(indiceCanal(reg, CANAL));
        double[] x = canal.getMuestras();

        // Anotaciones de latido en orden, con su símbolo.
        List<AnotacionECG> anotaciones = reg.getAnotaciones(AnotacionECG.FUENTE_REFERENCIA);
        if (anotaciones.isEmpty()) {
            anotaciones = reg.getAnotaciones();
        }
        DynamicArray<Integer> posList = new DynamicArray<>(8);
        DynamicArray<String> simList = new DynamicArray<>(8);
        for (AnotacionECG a : anotaciones) {
            if (esLatido(a.getTipo())) {
                posList.add((int) a.getIndiceMuestra());
                simList.add(a.getTipo());
            }
        }
        int[] pos = new int[posList.length()];
        for (int i = 0; i < pos.length; i++) {
            pos[i] = posList.get(i);
        }

        // centros, indices y F (igual que extraer() en Python/Java).
        int[] centros = new int[pos.length];
        for (int i = 0; i < pos.length; i++) {
            centros[i] = Caracteristicas.recentrar(x, pos[i]);
        }
        double[][] filas = Caracteristicas.extraer(x, pos);   // filas[i] == null si la ventana no cabe

        DynamicArray<double[]> xOut = new DynamicArray<>(8);
        DynamicArray<Integer> yOut = new DynamicArray<>(8);
        DynamicArray<Integer> posOut = new DynamicArray<>(8);
        int conVentana = 0;
        for (int i = 0; i < pos.length; i++) {
            if (filas[i] == null) {
                continue;                                     // sin ventana completa
            }
            conVentana++;
            int clase = indiceClaseAAMI(simList.get(i));
            if (clase >= 0) {
                xOut.add(filas[i]);
                yOut.add(clase);
                posOut.add(centros[i]);
            }
        }
        int descartados = pos.length - conVentana;            // sin ventana (los sin etiqueta se cuentan aparte)

        double[][] xArr = new double[xOut.length()][];
        int[] yArr = new int[yOut.length()];
        int[] posArr = new int[posOut.length()];
        for (int i = 0; i < xArr.length; i++) {
            xArr[i] = xOut.get(i);
            yArr[i] = yOut.get(i);
            posArr[i] = posOut.get(i);
        }
        return new Dataset(xArr, yArr, null, posArr, descartados);
    }

    /** Construye el dataset de un conjunto de registros. */
    public static Dataset construir(String[] nombres, Path directorio) throws IOException {
        CargadorRegistro cargador = new CargadorRegistro();
        DynamicArray<double[]> x = new DynamicArray<>(8);
        DynamicArray<Integer> y = new DynamicArray<>(8);
        DynamicArray<String> r = new DynamicArray<>(8);
        DynamicArray<Integer> p = new DynamicArray<>(8);
        int descartados = 0;
        for (String nombre : nombres) {
            Path hea = localizar(nombre, directorio);
            if (hea == null) {
                throw new IOException("no se encontró " + nombre + ".hea en " + directorio);
            }
            RegistroECG reg = cargador.cargarRegistro(hea);
            Dataset d = filasRegistro(reg);
            descartados += d.descartados;
            for (int i = 0; i < d.y.length; i++) {
                x.add(d.x[i]);
                y.add(d.y[i]);
                r.add(nombre);
                p.add(d.posicion[i]);
            }
        }
        double[][] xArr = new double[x.length()][];
        int[] yArr = new int[y.length()];
        String[] rArr = new String[r.length()];
        int[] pArr = new int[p.length()];
        for (int i = 0; i < xArr.length; i++) {
            xArr[i] = x.get(i);
            yArr[i] = y.get(i);
            rArr[i] = r.get(i);
            pArr[i] = p.get(i);
        }
        return new Dataset(xArr, yArr, rArr, pArr, descartados);
    }

    /** Guarda un dataset en texto (cabecera + una fila por latido) para reutilizarlo sin releer WFDB. */
    public static void guardar(Dataset d, Path ruta) throws IOException {
        try (var w = Files.newBufferedWriter(ruta)) {
            int nCar = d.x.length > 0 ? d.x[0].length : Caracteristicas.N_CARACT;
            w.write("DSV1 " + d.x.length + " " + nCar + " " + d.descartados);
            w.newLine();
            for (int i = 0; i < d.x.length; i++) {
                StringBuilder sb = new StringBuilder();
                sb.append(d.y[i]).append(' ').append(d.registro[i]).append(' ').append(d.posicion[i]);
                for (double v : d.x[i]) {
                    sb.append(' ').append(Double.toString(v));
                }
                w.write(sb.toString());
                w.newLine();
            }
        }
    }

    /** Lee un dataset guardado con {@link #guardar}. */
    public static Dataset cargar(Path ruta) throws IOException {
        List<String> lineas = Files.readAllLines(ruta);
        String[] cab = lineas.get(0).trim().split("\\s+");
        int n = Integer.parseInt(cab[1]);
        int nCar = Integer.parseInt(cab[2]);
        int descartados = Integer.parseInt(cab[3]);
        double[][] x = new double[n][nCar];
        int[] y = new int[n];
        String[] r = new String[n];
        int[] p = new int[n];
        for (int i = 0; i < n; i++) {
            String[] f = lineas.get(i + 1).trim().split("\\s+");
            y[i] = Integer.parseInt(f[0]);
            r[i] = f[1];
            p[i] = Integer.parseInt(f[2]);
            for (int j = 0; j < nCar; j++) {
                x[i][j] = Double.parseDouble(f[3 + j]);
            }
        }
        return new Dataset(x, y, r, p, descartados);
    }
}
