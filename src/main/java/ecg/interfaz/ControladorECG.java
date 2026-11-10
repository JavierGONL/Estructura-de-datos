package ecg.interfaz;

import ecg.exportacion.ConfiguracionExportacion;
import ecg.exportacion.ExportadorRegistroCompleto;
import ecg.exportacion.ExportadorVistaActual;
import ecg.exportacion.ImpresionEstandar;
import ecg.exportacion.ImpresionPersonalizada;
import ecg.lectura.CargadorRegistro;
import ecg.modelo.ConfiguracionVista;
import ecg.modelo.RegistroECG;
import ecg.modelo.ResultadoDeteccion;
import ecg.procesamiento.AnalizadorECG;
import ecg.procesamiento.DetectorPanTompkins;
import ecg.rendimiento.GeneradorInformePDF;
import ecg.rendimiento.MonitorRendimiento;
import ecg.rendimiento.RegistroMetricas;
import ecg.rendimiento.ResultadoRendimiento;
import ecg.visualizacion.GestoresVista;
import ecg.visualizacion.RenderizadorECG;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Coordina las acciones del usuario: los paneles piden cambios aquí, el controlador modifica la
 * {@link ConfiguracionVista} (nunca los datos del registro) y avisa a los oyentes para que se repinten.
 */
public class ControladorECG {

    private final RegistroMetricas registroMetricas = new RegistroMetricas();
    private final MonitorRendimiento monitor = new MonitorRendimiento(registroMetricas);
    private final CargadorRegistro cargador = new CargadorRegistro();
    private final AnalizadorECG analizador = new AnalizadorECG(new DetectorPanTompkins());
    private final ImpresionEstandar impresionEstandar = new ImpresionEstandar();
    private final ImpresionPersonalizada impresionPersonalizada = new ImpresionPersonalizada();
    private final ExportadorVistaActual exportadorVista = new ExportadorVistaActual();
    private final ExportadorRegistroCompleto exportadorCompleto = new ExportadorRegistroCompleto();
    private final RenderizadorECG renderizador = new RenderizadorECG();
    private final List<Runnable> oyentes = new ArrayList<>();

    private RegistroECG registro;
    private ConfiguracionVista config;
    private GestoresVista gestores;

    // ---------------- estado ----------------

    public RegistroECG getRegistro() { return registro; }
    public ConfiguracionVista getConfiguracion() { return config; }
    public GestoresVista getGestores() { return gestores; }
    public RegistroMetricas getRegistroMetricas() { return registroMetricas; }
    public MonitorRendimiento getMonitor() { return monitor; }
    public RenderizadorECG getRenderizador() { return renderizador; }
    public boolean hayRegistro() { return registro != null; }

    public void addOyente(Runnable r) { oyentes.add(r); }
    public void removeOyente(Runnable r) { oyentes.remove(r); }

    /** Avisa a los oyentes siempre desde el hilo de Swing (la carga puede ocurrir en un hilo de fondo). */
    public void notificarCambio() {
        Runnable aviso = () -> new ArrayList<>(oyentes).forEach(Runnable::run);
        if (java.awt.GraphicsEnvironment.isHeadless() || javax.swing.SwingUtilities.isEventDispatchThread()) aviso.run();
        else javax.swing.SwingUtilities.invokeLater(aviso);
    }

    // ---------------- carga y análisis ----------------

    /** Carga un registro midiendo cada fase. Puede ejecutarse fuera del hilo de Swing. */
    public RegistroECG cargarRegistro(Path archivoHea) throws IOException {
        cargador.setMonitor(monitor);
        try {
            RegistroECG r = medir("Cargar registro", () -> cargador.cargarRegistro(archivoHea));
            establecerRegistro(r);
            return r;
        } finally {
            cargador.setMonitor(null);
        }
    }

    public void establecerRegistro(RegistroECG r) {
        this.registro = r;
        this.config = ConfiguracionVista.paraRegistro(r);
        this.gestores = new GestoresVista(r, config, renderizador);
        registroMetricas.setInformacionRegistro(r);
        notificarCambio();
    }

    public ResultadoDeteccion analizarRegistro() throws IOException {
        exigirRegistro();
        ResultadoDeteccion r = medir("Analizar QRS", () -> analizador.analizarRegistro(registro));
        notificarCambio();
        return r;
    }

    // ---------------- acciones de visualización ----------------

    public void seleccionarCanal(int i, boolean visible) {
        if (visible) gestores.canales.seleccionarCanal(i); else gestores.canales.deseleccionarCanal(i);
        notificarCambio();
    }

    public void seleccionarTodosLosCanales() { gestores.canales.seleccionarTodos(); notificarCambio(); }
    public void limpiarSeleccionDeCanales() { gestores.canales.limpiarSeleccion(); notificarCambio(); }
    public void establecerDistribucion(String d) { config.setDistribucion(d); notificarCambio(); }
    public void zoomHorizontal(double f) { gestores.zoom.zoomHorizontal(f); notificarCambio(); }
    public void zoomVertical(double f) { gestores.zoom.zoomVertical(f); notificarCambio(); }
    public void restablecerZoom() { gestores.zoom.restablecerZoom(); notificarCambio(); }
    public void desplazarAdelante() { gestores.desplazamiento.desplazarAdelante(); notificarCambio(); }
    public void desplazarAtras() { gestores.desplazamiento.desplazarAtras(); notificarCambio(); }
    public void desplazarA(double t) { gestores.desplazamiento.desplazarA(t); notificarCambio(); }
    public void establecerVentana(double inicio, double fin) {
        double dur = Math.max(0.1, Math.min(fin - inicio, registro.getDuracionSegundos()));
        gestores.desplazamiento.desplazarA(inicio);
        config.setTiempoFin(config.getTiempoInicio() + dur);
        gestores.desplazamiento.desplazarA(config.getTiempoInicio());
        notificarCambio();
    }
    public void mostrarAnotaciones(boolean v) { gestores.anotaciones.mostrarAnotaciones(v); notificarCambio(); }
    public void mostrarDetecciones(boolean v) { gestores.anotaciones.mostrarDetecciones(v); notificarCambio(); }
    public void mostrarCuadricula(boolean v) { config.setMostrarCuadricula(v); notificarCambio(); }
    public void mostrarCursor(boolean v) { config.setMostrarCursor(v); notificarCambio(); }
    public void limpiarIntervalo() { gestores.intervalo.limpiarSeleccion(); notificarCambio(); }

    // ---------------- exportación (todas medidas como «Exportar PNG») ----------------

    public void exportarEstandar(Path destino, double inicio, double duracion) throws IOException {
        exigirRegistro();
        medir("Exportar PNG", () -> {
            impresionEstandar.guardar(impresionEstandar.generar(registro, inicio, duracion), destino);
            return null;
        });
    }

    public void exportarPersonalizada(Path destino, ConfiguracionVista vista, ConfiguracionExportacion opciones)
            throws IOException {
        exigirRegistro();
        medir("Exportar PNG", () -> {
            impresionPersonalizada.guardar(impresionPersonalizada.generar(registro, vista, opciones), destino);
            return null;
        });
    }

    public void exportarVistaActual(Path destino, int ancho, int alto) throws IOException {
        exigirRegistro();
        medir("Exportar PNG", () -> {
            exportadorVista.exportar(registro, config, ancho, alto, destino);
            return null;
        });
    }

    public List<Path> exportarRegistroCompleto(Path directorio, double segundosPorPagina) throws IOException {
        exigirRegistro();
        return medir("Exportar PNG", () -> exportadorCompleto.exportar(registro, 0, 0, segundosPorPagina, directorio));
    }

    /** Renderiza una imagen de la vista actual solo para medir el coste de dibujo. */
    public BufferedImage renderizarPrueba(int ancho, int alto) throws IOException {
        exigirRegistro();
        return medir("Renderizar ECG", () -> exportadorVista.generar(registro, config, ancho, alto));
    }

    // ---------------- rendimiento ----------------

    /**
     * Consolida las mediciones acumuladas, genera el informe PDF y empieza una ejecución nueva.
     * El coste del propio PDF se mide después y queda en la ejecución siguiente.
     */
    public ResultadoRendimiento finalizarYGenerarInforme(Path destino, String observaciones) throws IOException {
        ResultadoRendimiento r = registroMetricas.cerrarEjecucion(observaciones);
        medir("Generar informe PDF", () -> {
            new GeneradorInformePDF().generarInforme(r, destino);
            return null;
        });
        return r;
    }

    // ---------------- auxiliares ----------------

    private void exigirRegistro() {
        if (registro == null) throw new IllegalStateException("No hay ningún registro cargado");
    }

    private <T> T medir(String operacion, Callable<T> accion) throws IOException {
        try {
            return monitor.medir(operacion, accion);
        } catch (IOException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
