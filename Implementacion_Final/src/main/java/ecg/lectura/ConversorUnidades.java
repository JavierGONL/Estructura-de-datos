package ecg.lectura;

import ecg.modelo.MetadatosCanal;

/**
 * Convierte valores digitales a unidades físicas con la calibración de cada canal:
 * {@code V = (D - baseline) / ganancia}. Para canales de tensión el resultado se expresa en mV.
 * Esta es la única implementación de la conversión en todo el proyecto.
 */
public final class ConversorUnidades {

    /** Valor reservado por WFDB para "muestra inválida" en formato 16. */
    public static final int MUESTRA_INVALIDA_FORMATO16 = -32768;

    private ConversorUnidades() { }

    /** Factor para pasar de las unidades de la cabecera a mV, o NaN si no son unidades de tensión. */
    public static double factorAMilivoltios(String unidades) {
        if (unidades == null) return Double.NaN;
        switch (unidades.trim().toLowerCase()) {
            case "mv": return 1.0;
            case "uv": case "\u00b5v": case "\u03bcv": return 0.001;
            case "nv": return 0.000001;
            case "v":  return 1000.0;
            default:   return Double.NaN;
        }
    }

    /** Unidades en las que quedan los valores tras {@link #convertir}. */
    public static String unidadesResultado(MetadatosCanal m) {
        return Double.isNaN(factorAMilivoltios(m.getUnidades())) ? m.getUnidades() : "mV";
    }

    public static double convertirMuestra(double digital, MetadatosCanal m) {
        if (!m.isGananciaDisponible()) {
            throw new IllegalArgumentException("El canal '" + m.getDescripcion() + "' no declara ganancia");
        }
        if (m.getFormatoWFDB() == 16 && digital == MUESTRA_INVALIDA_FORMATO16) return Double.NaN;
        double fisico = (digital - m.getBaseline()) / m.getGananciaADC();
        double factor = factorAMilivoltios(m.getUnidades());
        return Double.isNaN(factor) ? fisico : fisico * factor;
    }

    public static double[] convertir(double[] digital, MetadatosCanal m) {
        double[] r = new double[digital.length];
        for (int i = 0; i < r.length; i++) r[i] = convertirMuestra(digital[i], m);
        return r;
    }
}
