package ecg.interfaz;

import ecg.ml.Datos;
import ecg.ml.Metricas;
import ecg.ml.ServicioClasificacion;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

/**
 * Clasificación de latidos con el Random Forest propio ({@code ecg.ml}). Permite entrenar el bosque
 * con DS1 de MIT-BIH y clasificar los latidos del registro abierto, mostrando clase y probabilidad
 * por latido y, si el registro tiene anotaciones de referencia, las métricas de acierto.
 */
public class VentanaClasificacion extends JDialog {

    private static final String[] COLUMNAS = {"#", "Muestra", "Clase (RF)", "Prob.", "Real", "¿Acierta?"};

    private final ControladorECG controlador;

    private final JSpinner spArboles = new JSpinner(new SpinnerNumberModel(25, 1, 500, 5));
    private final JSpinner spMinHoja = new JSpinner(new SpinnerNumberModel(20, 1, 1000, 5));
    private final JSpinner spSemilla = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
    private final JCheckBox chkVisor = new JCheckBox("Dibujar las clases sobre el visor", true);

    private final JButton btnEntrenar = new JButton("Entrenar modelo (DS1)");
    private final JButton btnCargar = new JButton("Cargar modelo guardado");
    private final JButton btnClasificar = new JButton("Clasificar latidos del registro");
    private final JProgressBar progreso = new JProgressBar(0, 100);
    private final JLabel estado = new JLabel(" ");

    private final DefaultTableModel modelo = new DefaultTableModel(COLUMNAS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTextArea reporte = new JTextArea(12, 50);

    public VentanaClasificacion(JFrame padre, ControladorECG controlador) {
        super(padre, "Clasificación de latidos — Random Forest", false);
        this.controlador = controlador;

        reporte.setEditable(false);
        reporte.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        add(construirNorte(), BorderLayout.NORTH);
        add(construirCentro(), BorderLayout.CENTER);
        add(construirSur(), BorderLayout.SOUTH);

        btnEntrenar.addActionListener(e -> entrenar());
        btnCargar.addActionListener(e -> cargar());
        btnClasificar.addActionListener(e -> clasificar());
        intentarCargaInicial();
        actualizarBotones();

        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        pack();
        setLocationRelativeTo(padre);
    }

    private JPanel construirNorte() {
        JPanel params = new JPanel(new GridLayout(2, 4, 8, 4));
        params.setBorder(BorderFactory.createTitledBorder("Hiperparámetros del bosque"));
        params.add(new JLabel("Árboles:"));
        params.add(spArboles);
        params.add(new JLabel("Mín. por hoja:"));
        params.add(spMinHoja);
        params.add(new JLabel("Semilla:"));
        params.add(spSemilla);
        params.add(new JLabel(""));
        params.add(new JLabel(""));

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        acciones.add(btnEntrenar);
        acciones.add(btnCargar);
        acciones.add(btnClasificar);
        acciones.add(chkVisor);

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(params, BorderLayout.NORTH);
        norte.add(acciones, BorderLayout.CENTER);
        norte.add(progreso, BorderLayout.SOUTH);
        return norte;
    }

    private JPanel construirCentro() {
        JTable tabla = new JTable(modelo);
        tabla.setAutoCreateRowSorter(true);
        JScrollPane spTabla = new JScrollPane(tabla);
        spTabla.setPreferredSize(new Dimension(520, 320));

        JScrollPane spReporte = new JScrollPane(reporte);
        spReporte.setBorder(BorderFactory.createTitledBorder("Resumen / métricas"));
        spReporte.setPreferredSize(new Dimension(420, 320));

        JPanel centro = new JPanel(new BorderLayout(8, 0));
        JPanel izq = new JPanel(new BorderLayout());
        izq.setBorder(BorderFactory.createTitledBorder("Latidos clasificados"));
        izq.add(spTabla, BorderLayout.CENTER);
        centro.add(izq, BorderLayout.CENTER);
        centro.add(spReporte, BorderLayout.EAST);
        return centro;
    }

    private JPanel construirSur() {
        JPanel sur = new JPanel(new BorderLayout());
        sur.add(estado, BorderLayout.WEST);
        return sur;
    }

    private void actualizarBotones() {
        boolean hayModelo = controlador.hayModeloRF();
        boolean hayRegistro = controlador.hayRegistro();
        btnClasificar.setEnabled(hayModelo && hayRegistro);
        if (hayModelo) {
            int n = controlador.getClasificador().getLatidosEntrenamiento();
            estado.setText(n > 0
                    ? " Modelo entrenado (" + n + " latidos DS1)."
                    : " Modelo cargado desde disco.");
        } else {
            estado.setText(" Sin modelo. Pulsa «Entrenar modelo» o «Cargar modelo guardado».");
        }
    }

    /** Intenta cargar un modelo guardado al abrir la ventana (silencioso si no hay). */
    private void intentarCargaInicial() {
        if (controlador.hayModeloRF()) return;
        try {
            if (controlador.cargarModeloRF(leerConfig())) {
                reporte.setText("Modelo cargado desde disco (" + leerConfig().modelo + ").\n"
                        + "Puedes clasificar directamente o re-entrenar con otros hiperparámetros.");
            }
        } catch (Exception ex) {
            // archivo corrupto o incompatible: se ignora, el usuario puede entrenar.
        }
    }

    private void cargar() {
        try {
            if (controlador.cargarModeloRF(leerConfig())) {
                reporte.setText("Modelo cargado desde disco (" + leerConfig().modelo + ").");
                estado.setText(" Modelo cargado.");
            } else {
                JOptionPane.showMessageDialog(this,
                        "No hay ningún modelo guardado en " + leerConfig().modelo
                                + "\nEntrena uno primero.",
                        "Sin modelo guardado", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            mostrarError("No se pudo cargar el modelo", ex);
        }
        actualizarBotones();
    }

    private ServicioClasificacion.Config leerConfig() {
        ServicioClasificacion.Config cfg = new ServicioClasificacion.Config();
        cfg.arboles = (Integer) spArboles.getValue();
        cfg.minHoja = (Integer) spMinHoja.getValue();
        cfg.semilla = ((Number) spSemilla.getValue()).longValue();
        return cfg;
    }

    private void entrenar() {
        ServicioClasificacion.Config cfg = leerConfig();
        btnEntrenar.setEnabled(false);
        btnClasificar.setEnabled(false);
        progreso.setIndeterminate(false);
        progreso.setValue(0);
        estado.setText(" Entrenando… (puede tardar varios segundos)");

        new SwingWorker<String, Integer>() {
            @Override protected String doInBackground() throws Exception {
                return controlador.entrenarModeloRF(cfg, p -> publish((int) Math.round(p * 100)));
            }
            @Override protected void process(List<Integer> chunks) {
                if (!chunks.isEmpty()) progreso.setValue(chunks.get(chunks.size() - 1));
            }
            @Override protected void done() {
                btnEntrenar.setEnabled(true);
                try {
                    reporte.setText(get());
                    progreso.setValue(100);
                } catch (Exception ex) {
                    progreso.setValue(0);
                    mostrarError("No se pudo entrenar el modelo", ex);
                }
                actualizarBotones();
            }
        }.execute();
    }

    private void clasificar() {
        boolean dibujar = chkVisor.isSelected();
        btnClasificar.setEnabled(false);
        progreso.setIndeterminate(true);
        estado.setText(" Clasificando latidos…");

        new SwingWorker<ServicioClasificacion.ResultadoClasificacion, Void>() {
            @Override protected ServicioClasificacion.ResultadoClasificacion doInBackground() throws Exception {
                return controlador.clasificarRegistroActual(dibujar);
            }
            @Override protected void done() {
                progreso.setIndeterminate(false);
                try {
                    mostrarResultado(get());
                } catch (Exception ex) {
                    mostrarError("No se pudieron clasificar los latidos", ex);
                }
                actualizarBotones();
            }
        }.execute();
    }

    private void mostrarResultado(ServicioClasificacion.ResultadoClasificacion r) {
        modelo.setRowCount(0);
        int n = 0;
        for (ServicioClasificacion.LatidoClasificado l : r.latidos) {
            String real = l.claseReal >= 0 ? Datos.CLASES[l.claseReal] : "—";
            String acierta = l.claseReal < 0 ? "—" : (l.claseReal == l.clase ? "sí" : "no");
            modelo.addRow(new Object[]{++n, l.indiceMuestra, l.etiqueta(),
                    String.format("%.0f%%", 100 * l.probabilidadMaxima()), real, acierta});
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Canal usado: ").append(r.canalUsado).append('\n');
        sb.append("Latidos clasificados: ").append(r.latidos.size()).append('\n');
        if (r.sinVentana > 0) {
            sb.append("Descartados (ventana incompleta): ").append(r.sinVentana).append('\n');
        }
        sb.append('\n');
        if (r.tieneReferencia()) {
            sb.append(Metricas.reporte(r.matrizConfusion));
        } else {
            sb.append("El registro no tiene anotaciones de referencia (.atr),\n");
            sb.append("así que no se pueden calcular métricas de acierto.\n");
            sb.append("Se muestran solo las clases predichas por el bosque.");
        }
        reporte.setText(sb.toString());
        reporte.setCaretPosition(0);
        estado.setText(" " + r.latidos.size() + " latidos clasificados.");
    }

    private void mostrarError(String titulo, Exception ex) {
        Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
        estado.setText(" Error: " + causa.getMessage());
        JOptionPane.showMessageDialog(this, String.valueOf(causa.getMessage()), titulo,
                JOptionPane.ERROR_MESSAGE);
    }
}
