package ecg.utilidades;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/** Construcción de rutas y nombres de archivos de salida. */
public final class GestorRutas {
    private GestorRutas() { }

    private static final DateTimeFormatter MARCA = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public static String marcaDeTiempo() { return LocalDateTime.now().format(MARCA); }

    public static Path directorioPNG() { return Path.of(ConstantesECG.DIRECTORIO_PNG); }
    public static Path directorioPDF() { return Path.of(ConstantesECG.DIRECTORIO_PDF); }

    public static String nombreEstandar(String registro, int canales) {
        return "ECG_Registro" + limpiar(registro) + "_" + canales + "Derivaciones.png";
    }

    public static String nombreCanales(String registro, List<String> nombres) {
        return "ECG_Registro" + limpiar(registro) + "_Canales"
                + nombres.stream().map(GestorRutas::limpiar).collect(Collectors.joining("_")) + ".png";
    }

    public static String nombreSegmento(String registro, double inicio, double fin) {
        return String.format("ECG_Registro%s_Segmento_%ds_%ds.png", limpiar(registro),
                (int) Math.round(inicio), (int) Math.round(fin));
    }

    public static String nombreInforme() { return "informe_rendimiento_" + marcaDeTiempo() + ".pdf"; }

    /** Deja solo caracteres seguros en nombres de archivo. */
    public static String limpiar(String s) { return s.replaceAll("[^A-Za-z0-9._-]", ""); }
}
