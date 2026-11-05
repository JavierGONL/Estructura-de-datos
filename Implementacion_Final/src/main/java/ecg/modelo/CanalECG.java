package ecg.modelo;

/** Una derivación individual con sus muestras ya convertidas a unidades físicas. */
public class CanalECG {

    private final String nombre;
    private final double[] muestras;
    private final MetadatosCanal metadatos;
    private final String unidades;

    /**
     * @param muestras  valores en unidades físicas (mV para ECG). Un valor NaN indica muestra inválida.
     * @param unidades  unidades de {@code muestras}
     */
    public CanalECG(String nombre, double[] muestras, MetadatosCanal metadatos, String unidades) {
        this.nombre = nombre;
        this.muestras = muestras;
        this.metadatos = metadatos;
        this.unidades = unidades;
    }

    public String getNombre() { return nombre; }
    public double[] getMuestras() { return muestras; }
    public double getMuestra(int indice) { return muestras[indice]; }
    public int getNumeroMuestras() { return muestras.length; }
    public MetadatosCanal getMetadatos() { return metadatos; }
    public String getUnidades() { return unidades; }
    public double getFrecuenciaMuestreo() { return metadatos.getFrecuenciaMuestreo(); }

    /** Amplitud de la muestra indicada, en mV (las muestras ya se almacenan en mV). */
    public double getAmplitudEnMilivoltios(int indice) { return muestras[indice]; }
}
