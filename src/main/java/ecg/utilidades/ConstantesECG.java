package ecg.utilidades;

/** Constantes compartidas por varios paquetes. */
public final class ConstantesECG {
    private ConstantesECG() { }

    public static final String NOMBRE_APLICACION = "ECG Viewer";
    public static final String EXTENSION_CABECERA = ".hea";
    public static final String DIRECTORIO_PNG = "exports/png";
    public static final String DIRECTORIO_PDF = "exports/pdf";
    public static final double MM_POR_SEGUNDO_ESTANDAR = 25.0;
    public static final double MM_POR_MV_ESTANDAR = 10.0;
    public static final double MM_ALTO_CANAL_ESTANDAR = 40.0;  // ±2 mV a 10 mm/mV
}
