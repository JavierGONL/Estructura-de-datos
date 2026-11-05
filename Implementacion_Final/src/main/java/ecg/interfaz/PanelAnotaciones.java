package ecg.interfaz;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.GridLayout;

/** Visibilidad de anotaciones, detecciones, cuadrícula y cursor; ejecución del detector. */
public class PanelAnotaciones extends JPanel {

    private final JCheckBox referencia = new JCheckBox("Anotaciones de referencia", true);
    private final JCheckBox detecciones = new JCheckBox("Detecciones del algoritmo", true);
    private final JCheckBox cuadricula = new JCheckBox("Cuadrícula", true);
    private final JCheckBox cursor = new JCheckBox("Cursor", true);
    private boolean actualizando;

    public PanelAnotaciones(ControladorECG c) {
        setLayout(new GridLayout(0, 1, 2, 2));
        setBorder(BorderFactory.createTitledBorder("Marcas"));
        referencia.addActionListener(e -> { if (!actualizando) c.mostrarAnotaciones(referencia.isSelected()); });
        detecciones.addActionListener(e -> { if (!actualizando) c.mostrarDetecciones(detecciones.isSelected()); });
        cuadricula.addActionListener(e -> { if (!actualizando) c.mostrarCuadricula(cuadricula.isSelected()); });
        cursor.addActionListener(e -> { if (!actualizando) c.mostrarCursor(cursor.isSelected()); });
        JButton detectar = new JButton("Detectar QRS");
        detectar.addActionListener(e -> {
            detectar.setEnabled(false);
            new SwingWorker<Integer, Void>() {
                @Override protected Integer doInBackground() throws Exception {
                    return c.analizarRegistro().getIndicesDetectados().size();
                }
                @Override protected void done() {
                    detectar.setEnabled(true);
                    try {
                        JOptionPane.showMessageDialog(PanelAnotaciones.this, get() + " latidos detectados.");
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(PanelAnotaciones.this, "Error: " + ex.getCause(),
                                "Detección", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });
        add(referencia); add(detecciones); add(cuadricula); add(cursor); add(detectar);
    }

    public void sincronizar(ecg.modelo.ConfiguracionVista cfg) {
        actualizando = true;
        referencia.setSelected(cfg.isMostrarAnotaciones());
        detecciones.setSelected(cfg.isMostrarDetecciones());
        cuadricula.setSelected(cfg.isMostrarCuadricula());
        cursor.setSelected(cfg.isMostrarCursor());
        actualizando = false;
    }
}
