package ecg.interfaz;

import ecg.exportacion.SelectorArchivoExportacion;
import ecg.utilidades.ConstantesECG;
import ecg.utilidades.GestorRutas;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Menú principal de la aplicación. */
public class VentanaPrincipal extends JFrame {

    private final ControladorECG controlador;
    private final JLabel resumen = new JLabel("", SwingConstants.CENTER);
    private final List<JMenuItem> requierenRegistro = new ArrayList<>();
    private VentanaRendimiento ventanaRendimiento;
    private VentanaClasificacion ventanaClasificacion;

    public VentanaPrincipal(ControladorECG controlador) {
        super(ConstantesECG.NOMBRE_APLICACION);
        this.controlador = controlador;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(620, 280));
        add(resumen);
        setJMenuBar(crearMenu());
        controlador.addOyente(this::actualizarEstado);
        actualizarEstado();
        pack();
        setLocationByPlatform(true);
    }

    private JMenuBar crearMenu() {
        JMenuBar barra = new JMenuBar();

        JMenu archivo = new JMenu("Archivo");
        archivo.add(item("Abrir registro ECG…", false, this::abrir));
        archivo.addSeparator();
        archivo.add(item("Salir", false, () -> System.exit(0)));

        JMenu ver = new JMenu("Ver");
        ver.add(item("Visualizar ECG", true, this::visualizar));
        ver.add(item("Configuración de la vista…", true, () -> new VentanaConfiguracionVista(this, controlador).setVisible(true)));

        JMenu exportar = new JMenu("Exportar");
        exportar.add(item("Imprimir ECG estándar (PNG)…", true, () -> new VentanaImpresion(this, controlador, false).setVisible(true)));
        exportar.add(item("Imprimir canales seleccionados (PNG)…", true, () -> new VentanaImpresion(this, controlador, true).setVisible(true)));
        exportar.add(item("Exportar PNG de la vista actual…", true, this::exportarVistaActual));

        JMenu analisis = new JMenu("Análisis");
        analisis.add(item("Analizar registro (detección QRS)", true, this::analizar));
        analisis.add(item("Clasificar latidos (Random Forest)…", true, this::clasificar));

        JMenu rend = new JMenu("Rendimiento");
        rend.add(item("Consultar rendimiento / generar informe PDF…", false, this::mostrarRendimiento));

        for (JMenu m : new JMenu[]{archivo, ver, exportar, analisis, rend}) barra.add(m);
        return barra;
    }

    private JMenuItem item(String texto, boolean requiereRegistro, Runnable accion) {
        JMenuItem i = new JMenuItem(texto);
        i.addActionListener(e -> accion.run());
        if (requiereRegistro) requierenRegistro.add(i);
        return i;
    }

    private void actualizarEstado() {
        boolean hay = controlador.hayRegistro();
        requierenRegistro.forEach(i -> i.setEnabled(hay));
        if (!hay) {
            resumen.setText("<html><center><h2>ECG Viewer</h2>Archivo ▸ Abrir registro ECG…</center></html>");
            return;
        }
        var r = controlador.getRegistro();
        resumen.setText(String.format("<html><center><h2>%s</h2>%d derivaciones · %.0f Hz · %.1f s · %d anotaciones</center></html>",
                r.getNombreRegistro(), r.getNumeroCanales(), r.getFrecuenciaMuestreo(), r.getDuracionSegundos(),
                r.getAnotaciones().size()));
    }

    private void abrir() { new VentanaCargaECG(this, controlador).setVisible(true); }

    private void visualizar() {
        controlador.getMonitor().medir("Abrir visor", () -> new VentanaVisorECG(controlador).setVisible(true));
    }

    private void exportarVistaActual() {
        var r = controlador.getRegistro();
        new SelectorArchivoExportacion().elegirDestino(this, GestorRutas.directorioPNG(),
                GestorRutas.nombreEstandar(r.getNombreRegistro(), controlador.getConfiguracion().getCanalesVisibles().size())
                        .replace(".png", "_vista.png")).ifPresent(destino -> {
            try {
                controlador.exportarVistaActual(destino, 1600, 900);
                JOptionPane.showMessageDialog(this, "Imagen guardada en:\n" + destino);
            } catch (IOException | RuntimeException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void analizar() {
        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() throws Exception {
                return controlador.analizarRegistro().getIndicesDetectados().size();
            }
            @Override protected void done() {
                try {
                    JOptionPane.showMessageDialog(VentanaPrincipal.this, get() + " latidos detectados (fusión de canales).");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(VentanaPrincipal.this, String.valueOf(ex.getCause()), "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void mostrarRendimiento() {
        if (ventanaRendimiento == null || !ventanaRendimiento.isDisplayable()) {
            ventanaRendimiento = new VentanaRendimiento(this, controlador);
        }
        ventanaRendimiento.actualizar();
        ventanaRendimiento.setVisible(true);
    }

    private void clasificar() {
        if (ventanaClasificacion == null || !ventanaClasificacion.isDisplayable()) {
            ventanaClasificacion = new VentanaClasificacion(this, controlador);
        }
        ventanaClasificacion.setVisible(true);
        ventanaClasificacion.toFront();
    }
}
