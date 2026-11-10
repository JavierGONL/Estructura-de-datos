package ecg.ml;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

/**
 * Compara las características de Java con las de Python, bit a bit.
 * Uso:  java -cp <clases> ecg.ml.ParidadCaracteristicas <carpeta paridad/ generada por etapa4_paridad.py>
 */
public final class ParidadCaracteristicas {
    public static void main(String[] args) throws IOException {
        Path dir = Paths.get(args.length > 0 ? args[0] : "paridad");
        List<String> ls = Files.readAllLines(dir.resolve("senal.txt"));
        double[] x = ls.stream().filter(s -> !s.isBlank()).mapToDouble(Double::parseDouble).toArray();
        int[] pos = Files.readAllLines(dir.resolve("posiciones.txt")).stream()
                .filter(s -> !s.isBlank()).mapToInt(Integer::parseInt).toArray();
        double[][] java = Caracteristicas.extraer(x, pos);

        int filas = 0, distintos = 0;
        double maxDif = 0;
        for (String linea : Files.readAllLines(dir.resolve("esperado.txt"))) {
            if (linea.isBlank()) continue;
            String[] t = linea.split(" ");
            double[] f = java[Integer.parseInt(t[0])];
            filas++;
            if (f == null) { distintos++; continue; }          // Python la tiene y Java no
            for (int j = 0; j < Caracteristicas.N_CARACT; j++) {
                double esperado = Double.parseDouble(t[j + 1]);
                if (Double.compare(esperado, f[j]) != 0) {
                    distintos++;
                    maxDif = Math.max(maxDif, Math.abs(esperado - f[j]));
                    break;
                }
            }
        }
        int validasJava = 0;
        for (double[] f : java) if (f != null) validasJava++;
        System.out.printf("latidos comparados: %d | con ventana en Java: %d | filas distintas: %d | max |dif|: %.3g%n",
                filas, validasJava, distintos, maxDif);
        System.out.println(distintos == 0 && filas == validasJava ? "PARIDAD EXACTA" : "NO COINCIDE");
        System.exit(distintos == 0 && filas == validasJava ? 0 : 1);
    }
}
