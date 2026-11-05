package ecg.exportacion;

/** Opciones de salida de una exportación. Valores &lt;= 0 en ancho/alto significan "calcular automáticamente". */
public class ConfiguracionExportacion {

    private int ancho;
    private int alto;
    private String formato = "png";
    private boolean mostrarCuadricula = true;
    private boolean mostrarAnotaciones = true;
    private boolean incluirMetadatos = true;
    private double escalaHorizontal = 25.0;   // mm/s
    private double escalaVertical = 10.0;     // mm/mV
    private double segundosPorPagina = 10.0;  // registro completo

    public int getAncho() { return ancho; }
    public void setAncho(int v) { this.ancho = v; }
    public int getAlto() { return alto; }
    public void setAlto(int v) { this.alto = v; }
    public String getFormato() { return formato; }
    public void setFormato(String v) { this.formato = v; }
    public boolean isMostrarCuadricula() { return mostrarCuadricula; }
    public void setMostrarCuadricula(boolean v) { this.mostrarCuadricula = v; }
    public boolean isMostrarAnotaciones() { return mostrarAnotaciones; }
    public void setMostrarAnotaciones(boolean v) { this.mostrarAnotaciones = v; }
    public boolean isIncluirMetadatos() { return incluirMetadatos; }
    public void setIncluirMetadatos(boolean v) { this.incluirMetadatos = v; }
    public double getEscalaHorizontal() { return escalaHorizontal; }
    public void setEscalaHorizontal(double v) { this.escalaHorizontal = v; }
    public double getEscalaVertical() { return escalaVertical; }
    public void setEscalaVertical(double v) { this.escalaVertical = v; }
    public double getSegundosPorPagina() { return segundosPorPagina; }
    public void setSegundosPorPagina(double v) { this.segundosPorPagina = v; }
}
