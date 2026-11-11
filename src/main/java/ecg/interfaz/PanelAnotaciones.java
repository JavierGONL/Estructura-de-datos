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
    private final JCheckBox clasificacion = new JCheckBox("Clasificación (Random Forest)", true);
    private final JCheckBox cuadricula = new JCheckBox("Cuadrícula", true);
    private final JCheckBox cursor = new JCheckBox("Cursor", true);
    private boolean actualizando;

    public PanelAnotaciones(ControladorECG c) {
        setLayout(new GridLayout(0, 1, 2, 2));
        setBorder(BorderFactory.createTitledBorder("Marcas"));
        referencia.addActionListener(e -> { if (!actualizando) c.mostrarAnotaciones(referencia.isSelected()); });
        detecciones.addActionListener(e -> { if (!actualizando) c.mostrarDetecciones(detecciones.isSelected()); });
        clasificacion.addActionListener(e -> { if (!actualizando) c.mostrarClasificacion(clasificacion.isSelected()); });
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

        JButton clasificar = new JButton("Clasificar (Random Forest)");
        clasificar.setToolTipText("Clasifica los latidos del registro con el bosque y dibuja su clase (N/S/V/F) en el visor");
        clasificar.addActionListener(e -> {
            clasificar.setEnabled(false);
            new SwingWorker<Integer, Void>() {
                private boolean sinModelo;
                @Override protected Integer doInBackground() throws Exception {
                    if (!c.asegurarModeloRF()) { sinModelo = true; return 0; }
                    return c.clasificarRegistroActual(true).latidos.size();
                }
                @Override protected void done() {
                    clasificar.setEnabled(true);
                    try {
                        get();
                        if (sinModelo) {
                            JOptionPane.showMessageDialog(PanelAnotaciones.this,
                                    "No hay ningún modelo Random Forest entrenado todavía.\n"
                                    + "Entrénalo una vez desde «Análisis ▸ Clasificar latidos (Random Forest)…»\n"
                                    + "(se guarda en disco y luego se usa directamente desde aquí).",
                                    "Clasificación", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            clasificacion.setSelected(true);
                            JOptionPane.showMessageDialog(PanelAnotaciones.this,
                                    get() + " latidos clasificados (ver marcas de color por clase).");
                        }
                    } catch (Exception ex) {
                        Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                        JOptionPane.showMessageDialog(PanelAnotaciones.this, "Error: " + causa.getMessage(),
                                "Clasificación", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });

        add(referencia); add(detecciones); add(clasificacion); add(cuadricula); add(cursor);
        add(detectar); add(clasificar);
    }

    public void sincronizar(ecg.modelo.ConfiguracionVista cfg) {
        actualizando = true;
        referencia.setSelected(cfg.isMostrarAnotaciones());
        detecciones.setSelected(cfg.isMostrarDetecciones());
        clasificacion.setSelected(cfg.isMostrarClasificacion());
        cuadricula.setSelected(cfg.isMostrarCuadricula());
        cursor.setSelected(cfg.isMostrarCursor());
        actualizando = false;
    }
}
