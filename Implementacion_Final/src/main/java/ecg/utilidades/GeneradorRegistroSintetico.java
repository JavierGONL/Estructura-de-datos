package ecg.utilidades;

import ecg.lectura.LectorAnotacionesWFDB;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Genera registros WFDB sintéticos (cabecera .hea, datos .dat en formato 16 o 212 y anotaciones .atr)
 * para probar la aplicación sin descargar PTB-XL ni MIT-BIH.
 */
public final class GeneradorRegistroSintetico {
    private GeneradorRegistroSintetico() { }

    public static final String[] DERIVACIONES_12 =
            {"I", "II", "III", "aVR", "aVL", "aVF", "V1", "V2", "V3", "V4", "V5", "V6"};
    private static final double[] FACTOR_AMPLITUD =
            {0.70, 1.00, 0.45, -0.80, 0.30, 0.75, 0.35, 0.60, 0.90, 1.10, 1.00, 0.80};
    private static final double GANANCIA = 200.0;   // ADU/mV

    /**
     * @param formato 16 o 212
     * @return ruta del archivo .hea creado
     */
    public static Path generar(Path directorio, String nombre, int canales, double fs, double segundos,
                               int formato) throws IOException {
        if (formato != 16 && formato != 212) throw new IllegalArgumentException("Formato 16 o 212");
        Files.createDirectories(directorio);
        int n = (int) Math.round(segundos * fs);
        Random rnd = new Random(42);

        // instantes de los latidos (RR ≈ 0,8 s con variabilidad)
        List<Integer> latidos = new ArrayList<>();
        double t = 0.6;
        while (t < segundos - 0.5) {
            latidos.add((int) Math.round(t * fs));
            t += 0.8 + 0.06 * Math.sin(t * 0.7) + 0.02 * rnd.nextGaussian();
        }

        int[][] adu = new int[canales][n];
        for (int c = 0; c < canales; c++) {
            double factor = FACTOR_AMPLITUD[c % FACTOR_AMPLITUD.length];
            double[] mv = new double[n];
            for (int i = 0; i < n; i++) {
                double ti = i / fs;
                mv[i] = 0.05 * Math.sin(2 * Math.PI * 0.25 * ti + c) + 0.01 * rnd.nextGaussian();
            }
            for (int b : latidos) agregarLatido(mv, b, fs, factor);
            for (int i = 0; i < n; i++) {
                int v = (int) Math.round(mv[i] * GANANCIA);
                adu[c][i] = formato == 212 ? Math.max(-2047, Math.min(2047, v)) : v;
            }
        }

        // .dat
        ByteArrayOutputStream datos = new ByteArrayOutputStream();
        if (formato == 16) {
            for (int i = 0; i < n; i++) for (int c = 0; c < canales; c++) {
                datos.write(adu[c][i] & 0xFF);
                datos.write((adu[c][i] >> 8) & 0xFF);
            }
        } else {
            int total = n * canales, k = 0;
            int[] plano = new int[total];
            for (int i = 0; i < n; i++) for (int c = 0; c < canales; c++) plano[k++] = adu[c][i] & 0xFFF;
            for (int j = 0; j + 1 < total; j += 2) {
                datos.write(plano[j] & 0xFF);
                datos.write(((plano[j] >> 8) & 0x0F) | ((plano[j + 1] >> 4) & 0xF0));
                datos.write(plano[j + 1] & 0xFF);
            }
            if (total % 2 == 1) {
                datos.write(plano[total - 1] & 0xFF);
                datos.write((plano[total - 1] >> 8) & 0x0F);
            }
        }
        Files.write(directorio.resolve(nombre + ".dat"), datos.toByteArray());

        // .hea
        StringBuilder h = new StringBuilder();
        h.append(nombre).append(' ').append(canales).append(' ').append((int) fs).append(' ').append(n).append('\n');
        for (int c = 0; c < canales; c++) {
            int checksum = 0;
            for (int v : adu[c]) checksum += v;
            checksum &= 0xFFFF;
            if (checksum >= 32768) checksum -= 65536;
            String nombreCanal = c < DERIVACIONES_12.length ? DERIVACIONES_12[c] : "C" + (c + 1);
            h.append(nombre).append(".dat ").append(formato).append(" 200(0)/mV ").append(formato == 212 ? 12 : 16)
                    .append(" 0 ").append(adu[c][0]).append(' ').append(checksum).append(" 0 ")
                    .append(nombreCanal).append('\n');
        }
        h.append("# Registro sintético generado por ECG Viewer (no es un registro clínico)\n");
        Path hea = directorio.resolve(nombre + ".hea");
        Files.writeString(hea, h.toString(), java.nio.charset.StandardCharsets.ISO_8859_1);

        escribirAnotaciones(directorio.resolve(nombre + ".atr"), latidos);
        return hea;
    }

    /** Latido P-QRS-T como suma de gaussianas (amplitudes en mV). */
    private static void agregarLatido(double[] mv, int centro, double fs, double factor) {
        double[][] ondas = {            // {desplazamiento s, amplitud mV, sigma s}
                {-0.20, 0.12, 0.025}, {-0.035, -0.12, 0.010}, {0.0, 1.00, 0.011},
                {0.035, -0.25, 0.012}, {0.26, 0.30, 0.045}};
        int radio = (int) Math.round(0.5 * fs);
        for (int i = Math.max(0, centro - radio); i < Math.min(mv.length, centro + radio); i++) {
            double ti = (i - centro) / fs;
            for (double[] o : ondas) {
                double z = (ti - o[0]) / o[2];
                mv[i] += factor * o[1] * Math.exp(-0.5 * z * z);
            }
        }
    }

    private static void escribirAnotaciones(Path archivo, List<Integer> latidos) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int previo = 0;
        int codigoN = LectorAnotacionesWFDB.codigoDeSimbolo("N");
        for (int b : latidos) {
            int dt = b - previo;
            if (dt > 1023) {                        // SKIP + 32 bits (palabra alta primero)
                palabra(out, 59 << 10);
                palabra(out, (dt >>> 16) & 0xFFFF);
                palabra(out, dt & 0xFFFF);
                dt = 0;
            }
            palabra(out, (codigoN << 10) | dt);
            previo = b;
        }
        palabra(out, 0);
        Files.write(archivo, out.toByteArray());
    }

    private static void palabra(ByteArrayOutputStream o, int w) { o.write(w & 0xFF); o.write((w >> 8) & 0xFF); }
}
