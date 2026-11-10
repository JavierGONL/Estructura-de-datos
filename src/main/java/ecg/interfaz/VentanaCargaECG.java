package ecg.interfaz;

import ecg.lectura.CargadorRegistro;
import ecg.lectura.ValidadorArchivoWFDB;
import ecg.modelo.CabeceraECG;
import ecg.modelo.RegistroECG;
import ecg.utilidades.ConversorTiempo;
import ecg.utilidades.GestorArchivos;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.nio.file.Path;

/** Selecciona un .hea, muestra su información y estado de validación, y carga el registro. */
public class VentanaCargaECG extends JDialog {

    private final ControladorECG controlador;
    private final JTextField ruta = new JTextField(40);
    private final JTextArea info = new JTextArea(12, 50);
    private final JButton cargar = new JButton("Cargar registro");
    private boolean cargado;

    public VentanaCargaECG(JFrame padre, ControladorECG controlador) {
        super(padre, "Abrir registro ECG", true);
        this.controlador = controlador;
        setLayout(new BorderLayout(6, 6));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel arriba = new JPanel(new BorderLayout(4, 4));
        arriba.add(new JLabel("Archivo de cabecera (.hea):"), BorderLayout.NORTH);
        ruta.setEditable(false);
        arriba.add(ruta, BorderLayout.CENTER);
        JButton examinar = new JButton("Examinar…");
        examinar.addActionListener(e -> examinar());
        arriba.add(examinar, BorderLayout.EAST);
        add(arriba, BorderLayout.NORTH);

        info.setEditable(false);
        info.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        info.setText("Seleccione un archivo .hea de WFDB (PTB-XL, MIT-BIH, …).\n"
                + "El número de derivaciones se toma de la cabecera.");
        add(new JScrollPane(info), BorderLayout.CENTER);

        JPanel abajo = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        cargar.setEnabled(false);
        cargar.addActionListener(e -> cargar());
        JButton cerrar = new JButton("Cerrar");
        cerrar.addActionListener(e -> dispose());
        abajo.add(cargar);
        abajo.add(cerrar);
        add(abajo, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(padre);
    }

    /** @return true si se cargó un registro */
    public boolean isCargado() { return cargado; }

    private void examinar() {
        JFileChooser fc = new JFileChooser(Path.of("data").toFile().isDirectory() ? Path.of("data").toFile() : null);
        fc.setFileFilter(new FileNameExtensionFilter("Cabecera WFDB (*.hea)", "hea"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path p = fc.getSelectedFile().toPath();
        if (!GestorArchivos.esCabeceraWFDB(p)) {
            JOptionPane.showMessageDialog(this, "Seleccione un archivo .hea", "Archivo no válido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ruta.setText(p.toString());
        mostrarInformacion(p);
    }

    private void mostrarInformacion(Path p) {
        StringBuilder sb = new StringBuilder();
        try {
            CargadorRegistro c = new CargadorRegistro();
            CabeceraECG cab = c.leerSoloCabecera(p);
            ValidadorArchivoWFDB.Informe inf = c.validar(p);
            double dur = cab.getNumeroMuestras() / cab.getFrecuenciaMuestreo();
            sb.append("Registro:      ").append(cab.getNombreRegistro()).append('\n')
              .append("Canales:       ").append(cab.getNumeroCanales()).append('\n')
              .append(String.format("Frecuencia:    %.1f Hz%n", cab.getFrecuenciaMuestreo()))
              .append("Muestras:      ").append(cab.getNumeroMuestras()).append('\n')
              .append("Duración:      ").append(cab.getNumeroMuestras() > 0 ? ConversorTiempo.formatoReloj(dur) : "(se deduce del .dat)").append('\n')
              .append("Derivaciones:  ");
            cab.getMetadatosCanales().forEach(m -> sb.append(m.getDescripcion().isBlank() ? "?" : m.getDescripcion()).append(' '));
            sb.append("\n\nValidación:\n").append(inf.resumen());
            cargar.setEnabled(inf.esValido());
        } catch (Exception e) {
            sb.append("No se pudo leer la cabecera:\n").append(e.getMessage());
            cargar.setEnabled(false);
        }
        info.setText(sb.toString());
        info.setCaretPosition(0);
    }

    private void cargar() {
        Path p = Path.of(ruta.getText());
        cargar.setEnabled(false);
        info.append("\n\nCargando…");
        new SwingWorker<RegistroECG, Void>() {
            @Override protected RegistroECG doInBackground() throws Exception { return controlador.cargarRegistro(p); }
            @Override protected void done() {
                try {
                    RegistroECG r = get();
                    cargado = true;
                    if (!r.getAdvertencias().isEmpty()) {
                        JOptionPane.showMessageDialog(VentanaCargaECG.this, String.join("\n", r.getAdvertencias()),
                                "Avisos", JOptionPane.INFORMATION_MESSAGE);
                    }
                    dispose();
                } catch (Exception ex) {
                    cargar.setEnabled(true);
                    Throwable t = ex.getCause() != null ? ex.getCause() : ex;
                    info.append("\nERROR: " + t.getMessage());
                }
            }
        }.execute();
    }
}
