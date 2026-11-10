package ecg.modelo;

/**
 * Metadatos de un canal tal como los declara una línea de señal del archivo .hea.
 * Cada canal conserva su propio archivo, formato, ganancia y línea de base.
 */
public class MetadatosCanal {

    private String archivoDatos;
    private int formatoWFDB;
    private int muestrasPorFrame = 1;
    private int skew;
    private int desplazamiento;          // bytes a saltar al inicio del archivo (+offset)
    private double gananciaADC;          // unidades ADC por unidad física; 0 = no declarada
    private double frecuenciaMuestreo;
    private int adcZero;
    private int baseline;
    private String unidades = "mV";
    private String descripcion = "";
    private int resolucionADC;
    private int valorInicial;
    private int checksum;
    private boolean checksumDeclarado;

    public String getArchivoDatos() { return archivoDatos; }
    public void setArchivoDatos(String v) { this.archivoDatos = v; }
    public int getFormatoWFDB() { return formatoWFDB; }
    public void setFormatoWFDB(int v) { this.formatoWFDB = v; }
    public int getMuestrasPorFrame() { return muestrasPorFrame; }
    public void setMuestrasPorFrame(int v) { this.muestrasPorFrame = v; }
    public int getSkew() { return skew; }
    public void setSkew(int v) { this.skew = v; }
    public int getDesplazamiento() { return desplazamiento; }
    public void setDesplazamiento(int v) { this.desplazamiento = v; }
    public double getGananciaADC() { return gananciaADC; }
    public void setGananciaADC(double v) { this.gananciaADC = v; }
    public boolean isGananciaDisponible() { return gananciaADC > 0; }
    public double getFrecuenciaMuestreo() { return frecuenciaMuestreo; }
    public void setFrecuenciaMuestreo(double v) { this.frecuenciaMuestreo = v; }
    public int getAdcZero() { return adcZero; }
    public void setAdcZero(int v) { this.adcZero = v; }
    public int getBaseline() { return baseline; }
    public void setBaseline(int v) { this.baseline = v; }
    public String getUnidades() { return unidades; }
    public void setUnidades(String v) { this.unidades = (v == null || v.isBlank()) ? "mV" : v; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String v) { this.descripcion = v == null ? "" : v; }
    public int getResolucionADC() { return resolucionADC; }
    public void setResolucionADC(int v) { this.resolucionADC = v; }
    public int getValorInicial() { return valorInicial; }
    public void setValorInicial(int v) { this.valorInicial = v; }
    public int getChecksum() { return checksum; }
    public void setChecksum(int v) { this.checksum = v; this.checksumDeclarado = true; }
    public boolean isChecksumDeclarado() { return checksumDeclarado; }
}
