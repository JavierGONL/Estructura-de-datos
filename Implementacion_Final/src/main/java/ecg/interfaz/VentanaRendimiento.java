package ecg.interfaz;

import ecg.modelo.MetricasEjecucion;
import ecg.utilidades.FormateadorDatos;
import ecg.utilidades.GestorRutas;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.nio.file.Path;

/** Mediciones de la sesión y generación del informe PDF. */
public class VentanaRendimiento extends JDialog {

    private static final String[] COLUMNAS = {"Operación", "Nivel", "Tiempo (ms)", "CPU hilo (ms)", "CPU usuario (ms)",
            "CPU sistema (ms)", "Heap máx (MiB)", "RSS (MiB)"};

    private final ControladorECG controlador;
    private final DefaultTableModel modelo = new DefaultTableModel(COLUMNAS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JLabel estado = new JLabel(" ");
    private final JTextArea observaciones = new JTextArea(3, 40);

    public VentanaRendimiento(JFrame padre, ControladorECG controlador) {
        super(padre, "Rendimiento de la sesión", false);
        this.controlador = controlador;

        JTable tabla = new JTable(modelo);
        tabla.setAutoCreateRowSorter(true);
        JScrollPane sp = new JScrollPane(tabla);
        sp.setPreferredSize(new Dimension(820, 260));

        JPanel obs = new JPanel(new BorderLayout());
        obs.setBorder(BorderFactory.createTitledBorder("Observaciones para el informe"));
        obs.add(new JScrollPane(observaciones));

        JButton actualizar = new JButton("Actualizar");
        actualizar.addActionListener(e -> actualizar());
        JButton informe = new JButton("Finalizar ejecución y generar informe de rendimiento");
        informe.addActionListener(e -> generarInforme());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(actualizar); botones.add(informe);

        JPanel sur = new JPanel(new BorderLayout());
        sur.add(obs, BorderLayout.CENTER);
        sur.add(botones, BorderLayout.SOUTH);

        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(estado, BorderLayout.NORTH);
        add(sp, BorderLayout.CENTER);
        add(sur, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(padre);
        actualizar();
    }

    public final void actualizar() {
        modelo.setRowCount(0);
        for (MetricasEjecucion m : controlador.getRegistroMetricas().getMetricas()) {
            modelo.addRow(new Object[]{m.getNombreOperacion(), m.getProfundidad(),
                    FormateadorDatos.ms(m.getTiempoPared()), FormateadorDatos.ms(m.getTiempoCPUHilo()),
                    FormateadorDatos.ms(m.getTiempoCPUUsuario()), FormateadorDatos.ms(m.getTiempoCPUSistema()),
                    FormateadorDatos.mb(m.getMemoriaMaxima()), FormateadorDatos.mb(m.getMemoriaRSS())});
        }
        estado.setText(" " + modelo.getRowCount() + " mediciones en la ejecución actual · "
                + controlador.getRegistroMetricas().getHistorial().size() + " ejecuciones cerradas");
    }

    private void generarInforme() {
        if (modelo.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Todavía no hay mediciones.");
            return;
        }
        JFileChooser fc = new JFileChooser(GestorRutas.directorioPDF().toFile().isDirectory()
                ? GestorRutas.directorioPDF().toFile() : null);
        fc.setFileFilter(new FileNameExtensionFilter("Informe PDF (*.pdf)", "pdf"));
        fc.setSelectedFile(new java.io.File(fc.getCurrentDirectory(), GestorRutas.nombreInforme()));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path destino = fc.getSelectedFile().toPath();
        if (!destino.getFileName().toString().toLowerCase().endsWith(".pdf")) {
            destino = destino.resolveSibling(destino.getFileName() + ".pdf");
        }
        final Path d = destino;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                controlador.finalizarYGenerarInforme(d, observaciones.getText());
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    actualizar();
                    JOptionPane.showMessageDialog(VentanaRendimiento.this, "Informe generado:\n" + d);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(VentanaRendimiento.this, String.valueOf(ex.getCause()),
                            "Error al generar el informe", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
