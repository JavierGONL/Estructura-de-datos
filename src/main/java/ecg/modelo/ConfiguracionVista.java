package ecg.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * Estado visual del visor. El visor y los exportadores comparten esta clase para que
 * lo que se ve en pantalla sea lo que se exporta.
 */
public class ConfiguracionVista {

    public static final double VELOCIDAD_ESTANDAR_MM_S = 25.0;
    public static final double GANANCIA_ESTANDAR_MM_MV = 10.0;
    public static final double PIXELES_POR_MM_PANTALLA = 4.0;

    private List<Integer> canalesVisibles = new ArrayList<>();

    private double tiempoInicio;
    private double tiempoFin = 10;

    /** mm/s nominales (usados por los exportadores para dimensionar la imagen). */
    private double escalaHorizontal = VELOCIDAD_ESTANDAR_MM_S;
    /** mm/mV: define la amplitud dibujada. */
    private double escalaVertical = GANANCIA_ESTANDAR_MM_MV;
    private double pixelesPorMm = PIXELES_POR_MM_PANTALLA;

    private String distribucion = "CUADRICULA_3X4";

    private boolean mostrarCuadricula = true;
    private boolean mostrarAnotaciones = true;
    private boolean mostrarDetecciones = true;
    private boolean mostrarClasificacion = true;
    private boolean mostrarCursor = true;
    private boolean mostrarNombres = true;
    private boolean mostrarEncabezado = false;

    // estado de inspección
    private double cursorTiempo = Double.NaN;
    private int cursorCanal = -1;
    private IntervaloTemporal intervaloSeleccionado;
    private AnotacionECG anotacionSeleccionada;

    /** Configuración inicial: todos los canales visibles y los primeros 10 s. */
    public static ConfiguracionVista paraRegistro(RegistroECG r) {
        ConfiguracionVista c = new ConfiguracionVista();
        for (int i = 0; i < r.getNumeroCanales(); i++) c.canalesVisibles.add(i);
        c.tiempoInicio = 0;
        c.tiempoFin = Math.max(0.1, Math.min(10.0, r.getDuracionSegundos()));
        c.distribucion = r.getNumeroCanales() <= 3 ? "VERTICAL" : "CUADRICULA_3X4";
        return c;
    }

    public ConfiguracionVista copiar() {
        ConfiguracionVista c = new ConfiguracionVista();
        c.canalesVisibles = new ArrayList<>(canalesVisibles);
        c.tiempoInicio = tiempoInicio; c.tiempoFin = tiempoFin;
        c.escalaHorizontal = escalaHorizontal; c.escalaVertical = escalaVertical;
        c.pixelesPorMm = pixelesPorMm; c.distribucion = distribucion;
        c.mostrarCuadricula = mostrarCuadricula; c.mostrarAnotaciones = mostrarAnotaciones;
        c.mostrarDetecciones = mostrarDetecciones; c.mostrarCursor = mostrarCursor;
        c.mostrarClasificacion = mostrarClasificacion;
        c.mostrarNombres = mostrarNombres; c.mostrarEncabezado = mostrarEncabezado;
        c.cursorTiempo = cursorTiempo; c.cursorCanal = cursorCanal;
        c.intervaloSeleccionado = intervaloSeleccionado; c.anotacionSeleccionada = anotacionSeleccionada;
        return c;
    }

    public List<Integer> getCanalesVisibles() { return canalesVisibles; }
    public void setCanalesVisibles(List<Integer> v) { this.canalesVisibles = new ArrayList<>(v); }
    public double getTiempoInicio() { return tiempoInicio; }
    public void setTiempoInicio(double v) { this.tiempoInicio = v; }
    public double getTiempoFin() { return tiempoFin; }
    public void setTiempoFin(double v) { this.tiempoFin = v; }
    public double getDuracionVentana() { return tiempoFin - tiempoInicio; }
    public double getEscalaHorizontal() { return escalaHorizontal; }
    public void setEscalaHorizontal(double v) { this.escalaHorizontal = v; }
    public double getEscalaVertical() { return escalaVertical; }
    public void setEscalaVertical(double v) { this.escalaVertical = v; }
    public double getPixelesPorMm() { return pixelesPorMm; }
    public void setPixelesPorMm(double v) { this.pixelesPorMm = v; }
    public String getDistribucion() { return distribucion; }
    public void setDistribucion(String v) { this.distribucion = v; }
    public boolean isMostrarCuadricula() { return mostrarCuadricula; }
    public void setMostrarCuadricula(boolean v) { this.mostrarCuadricula = v; }
    public boolean isMostrarAnotaciones() { return mostrarAnotaciones; }
    public void setMostrarAnotaciones(boolean v) { this.mostrarAnotaciones = v; }
    public boolean isMostrarDetecciones() { return mostrarDetecciones; }
    public void setMostrarDetecciones(boolean v) { this.mostrarDetecciones = v; }
    public boolean isMostrarClasificacion() { return mostrarClasificacion; }
    public void setMostrarClasificacion(boolean v) { this.mostrarClasificacion = v; }
    public boolean isMostrarCursor() { return mostrarCursor; }
    public void setMostrarCursor(boolean v) { this.mostrarCursor = v; }
    public boolean isMostrarNombres() { return mostrarNombres; }
    public void setMostrarNombres(boolean v) { this.mostrarNombres = v; }
    public boolean isMostrarEncabezado() { return mostrarEncabezado; }
    public void setMostrarEncabezado(boolean v) { this.mostrarEncabezado = v; }
    public double getCursorTiempo() { return cursorTiempo; }
    public void setCursorTiempo(double v) { this.cursorTiempo = v; }
    public int getCursorCanal() { return cursorCanal; }
    public void setCursorCanal(int v) { this.cursorCanal = v; }
    public IntervaloTemporal getIntervaloSeleccionado() { return intervaloSeleccionado; }
    public void setIntervaloSeleccionado(IntervaloTemporal v) { this.intervaloSeleccionado = v; }
    public AnotacionECG getAnotacionSeleccionada() { return anotacionSeleccionada; }
    public void setAnotacionSeleccionada(AnotacionECG v) { this.anotacionSeleccionada = v; }
}
