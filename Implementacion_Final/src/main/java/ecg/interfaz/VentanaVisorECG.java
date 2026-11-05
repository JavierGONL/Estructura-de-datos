package ecg.interfaz;

import ecg.exportacion.SelectorArchivoExportacion;
import ecg.utilidades.GestorRutas;
import ecg.visualizacion.PanelECGInteractivo;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

/** Ventana del visor: panel interactivo más todos los controles de visualización. */
public class VentanaVisorECG extends JFrame {

    private final ControladorECG controlador;
    private final PanelECGInteractivo panel;
    private final PanelSeleccionCanales seleccion;
    private final PanelControlTemporal temporal;
    private final PanelAnotaciones anotaciones;
    private final PanelInformacionCursor cursor = new PanelInformacionCursor();
    private final Runnable oyente;

    public VentanaVisorECG(ControladorECG controlador) {
        super("ECG Viewer — " + controlador.getRegistro().getNombreRegistro());
        this.controlador = controlador;
        var registro = controlador.getRegistro();
        var cfg = controlador.getConfiguracion();

        panel = new PanelECGInteractivo(registro, cfg, controlador.getGestores());
        panel.setOyenteCursor(cursor::mostrar);
        panel.setOyenteCambio(controlador::notificarCambio);

        seleccion = new PanelSeleccionCanales(registro, cfg.getCanalesVisibles(),
                controlador::seleccionarCanal, controlador::seleccionarTodosLosCanales,
                controlador::limpiarSeleccionDeCanales, controlador::establecerDistribucion);
        seleccion.sincronizar(cfg.getCanalesVisibles(), cfg.getDistribucion());
        temporal = new PanelControlTemporal(controlador);
        anotaciones = new PanelAnotaciones(controlador);

        JPanel oeste = new JPanel(new BorderLayout(4, 4));
        oeste.add(seleccion, BorderLayout.CENTER);
        oeste.add(anotaciones, BorderLayout.SOUTH);
        oeste.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 0));

        JButton exportar = new JButton("Exportar PNG (vista actual)");
        exportar.addActionListener(e -> exportarVista());
        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        controles.add(temporal);
        controles.add(new PanelControlZoom(controlador));
        controles.add(exportar);

        JPanel sur = new JPanel();
        sur.setLayout(new BoxLayout(sur, BoxLayout.Y_AXIS));
        sur.add(controles);
        sur.add(cursor);

        add(oeste, BorderLayout.WEST);
        add(panel, BorderLayout.CENTER);
        add(sur, BorderLayout.SOUTH);

        oyente = this::refrescar;
        controlador.addOyente(oyente);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) { controlador.removeOyente(oyente); }
        });
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationByPlatform(true);
        refrescar();
    }

    private void refrescar() {
        var cfg = controlador.getConfiguracion();
        panel.setConfiguracion(cfg);
        seleccion.sincronizar(cfg.getCanalesVisibles(), cfg.getDistribucion());
        temporal.refrescar();
        anotaciones.sincronizar(cfg);
    }

    private void exportarVista() {
        var r = controlador.getRegistro();
        new SelectorArchivoExportacion().elegirDestino(this, GestorRutas.directorioPNG(),
                GestorRutas.nombreEstandar(r.getNombreRegistro(), controlador.getConfiguracion().getCanalesVisibles().size())
                        .replace(".png", "_vista.png")).ifPresent(destino -> {
            try {
                controlador.exportarVistaActual(destino, Math.max(400, panel.getWidth()), Math.max(300, panel.getHeight()));
                JOptionPane.showMessageDialog(this, "Imagen guardada en:\n" + destino);
            } catch (IOException | RuntimeException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error al exportar", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
