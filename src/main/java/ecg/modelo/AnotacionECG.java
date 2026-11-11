package ecg.modelo;

/** Marca sobre la señal: latido de referencia, detección del algoritmo o anotación manual. */
public class AnotacionECG {

    public static final String FUENTE_REFERENCIA = "REFERENCIA";
    public static final String FUENTE_ALGORITMO = "ALGORITMO";
    public static final String FUENTE_MANUAL = "MANUAL";
    /** Clasificación de latidos por el Random Forest (clase AAMI: N, S, V, F). */
    public static final String FUENTE_CLASIFICACION = "CLASIFICACION";

    /** Símbolos WFDB que corresponden a latidos (N, L, R, a, V, F, J, A, S, E, j, /, Q, e, n, x, f, r). */
    private static final String SIMBOLOS_LATIDO = "NLRaVFJASEj/Qenxfr";

    private final long indiceMuestra;
    private final String tipo;
    private final String descripcion;
    private final String fuente;

    public AnotacionECG(long indiceMuestra, String tipo, String descripcion, String fuente) {
        this.indiceMuestra = indiceMuestra;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.fuente = fuente;
    }

    public long getIndiceMuestra() { return indiceMuestra; }
    public String getTipo() { return tipo; }
    public String getDescripcion() { return descripcion; }
    public String getFuente() { return fuente; }

    public double getTiempo(double frecuenciaMuestreo) { return indiceMuestra / frecuenciaMuestreo; }

    /** Indica si la anotación representa un latido (las de otro tipo son ritmo, ruido, etc.). */
    public boolean esLatido() {
        return tipo != null && (tipo.length() != 1 || SIMBOLOS_LATIDO.indexOf(tipo.charAt(0)) >= 0);
    }
}
