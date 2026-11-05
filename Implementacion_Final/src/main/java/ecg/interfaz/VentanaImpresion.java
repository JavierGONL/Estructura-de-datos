package ecg.interfaz;

import ecg.exportacion.ConfiguracionExportacion;
import ecg.exportacion.SelectorArchivoExportacion;
import ecg.modelo.ConfiguracionVista;
import ecg.utilidades.GestorRutas;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Impresión estándar de las doce derivaciones, personalizada, o registro completo por páginas (PNG). */
public class VentanaImpresion extends JDialog {

    private final ControladorECG controlador;
    private final JRadioButton estandar = new JRadioButton("Estándar (todas las derivaciones, 25 mm/s, 10 mm/mV)");
    private final JRadioButton personalizada = new JRadioButton("Personalizada");
    private final JSpinner inicio = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1.0e6, 1.0));
    private final JSpinner duracion = new JSpinner(new SpinnerNumberModel(5.0, 0.5, 3600.0, 1.0));
    private final JSpinner ancho = new JSpinner(new SpinnerNumberModel(0, 0, 20000, 100));
    private final JSpinner alto = new JSpinner(new SpinnerNumberModel(0, 0, 20000, 100));
    private final JSpinner mmPorS = new JSpinner(new SpinnerNumberModel(25.0, 1.0, 200.0, 5.0));
    private final JSpinner mmPorMv = new JSpinner(new SpinnerNumberModel(10.0, 0.5, 100.0, 2.5));
    private final JCheckBox anotaciones = new JCheckBox("Incluir anotaciones", true);
    private final JCheckBox metadatos = new JCheckBox("Incluir metadatos", true);
    private final ConfiguracionVista vistaLocal;
    private final PanelSeleccionCanales canales;

    public VentanaImpresion(JFrame padre, ControladorECG controlador, boolean personalizadaInicial) {
        super(padre, "Imprimir / exportar PNG", true);
        this.controlador = controlador;
        var registro = controlador.getRegistro();
        vistaLocal = controlador.getConfiguracion().copiar();
        duracion.setValue(Math.min(registro.getDuracionSegundos(), 5.0));

        ButtonGroup g = new ButtonGroup();
        g.add(estandar); g.add(personalizada);
        (personalizadaInicial ? personalizada : estandar).setSelected(true);

        JPanel opciones = new JPanel(new GridLayout(0, 2, 6, 4));
        opciones.setBorder(BorderFactory.createTitledBorder("Opciones"));
        opciones.add(new JLabel("Inicio (s)")); opciones.add(inicio);
        opciones.add(new JLabel("Duración (s)")); opciones.add(duracion);
        opciones.add(new JLabel("Ancho px (0 = auto)")); opciones.add(ancho);
        opciones.add(new JLabel("Alto px (0 = auto)")); opciones.add(alto);
        opciones.add(new JLabel("Escala horizontal (mm/s)")); opciones.add(mmPorS);
        opciones.add(new JLabel("Escala vertical (mm/mV)")); opciones.add(mmPorMv);
        opciones.add(anotaciones); opciones.add(metadatos);

        canales = new PanelSeleccionCanales(registro, vistaLocal.getCanalesVisibles(),
                (i, v) -> { var l = new ArrayList<>(vistaLocal.getCanalesVisibles());
                    if (v) { if (!l.contains(i)) l.add(i); } else l.remove(Integer.valueOf(i));
                    vistaLocal.setCanalesVisibles(l); },
                () -> { var l = new ArrayList<Integer>(); for (int i = 0; i < registro.getNumeroCanales(); i++) l.add(i);
                    vistaLocal.setCanalesVisibles(l); sincronizar(); },
                () -> { vistaLocal.setCanalesVisibles(new ArrayList<>()); sincronizar(); },
                vistaLocal::setDistribucion);
        sincronizar();

        JPanel modo = new JPanel(new GridLayout(0, 1));
        modo.add(estandar); modo.add(personalizada);
        JPanel norte = new JPanel(new BorderLayout());
        norte.add(modo, BorderLayout.NORTH);
        norte.add(opciones, BorderLayout.CENTER);

        JButton exportar = new JButton("Exportar PNG…"), paginas = new JButton("Registro completo (páginas)…"),
                cerrar = new JButton("Cerrar");
        exportar.addActionListener(e -> exportar());
        paginas.addActionListener(e -> exportarPaginas());
        cerrar.addActionListener(e -> dispose());
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sur.add(paginas); sur.add(exportar); sur.add(cerrar);

        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(norte, BorderLayout.NORTH);
        add(canales, BorderLayout.CENTER);
        add(sur, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(padre);
    }

    private void sincronizar() { canales.sincronizar(vistaLocal.getCanalesVisibles(), vistaLocal.getDistribucion()); }

    private double num(JSpinner s) { return ((Number) s.getValue()).doubleValue(); }

    private void exportar() {
        var r = controlador.getRegistro();
        double t0 = num(inicio), dur = num(duracion);
        boolean std = estandar.isSelected();
        String nombre;
        if (std) {
            nombre = GestorRutas.nombreEstandar(r.getNombreRegistro(), r.getNumeroCanales());
        } else {
            List<String> n = new ArrayList<>();
            for (int i : vistaLocal.getCanalesVisibles()) n.add(r.getCanal(i).getNombre());
            if (n.isEmpty()) { JOptionPane.showMessageDialog(this, "Seleccione al menos una derivación."); return; }
            nombre = n.size() > 4 ? GestorRutas.nombreSegmento(r.getNombreRegistro(), t0, t0 + dur)
                    : GestorRutas.nombreCanales(r.getNombreRegistro(), n);
        }
        new SelectorArchivoExportacion().elegirDestino(this, GestorRutas.directorioPNG(), nombre).ifPresent(destino ->
                ejecutar(() -> {
                    if (std) {
                        controlador.exportarEstandar(destino, t0, dur);
                    } else {
                        ConfiguracionVista v = vistaLocal.copiar();
                        v.setTiempoInicio(Math.max(0, Math.min(t0, r.getDuracionSegundos() - 0.1)));
                        v.setTiempoFin(Math.min(r.getDuracionSegundos(), v.getTiempoInicio() + dur));
                        ConfiguracionExportacion o = new ConfiguracionExportacion();
                        o.setAncho((int) num(ancho)); o.setAlto((int) num(alto));
                        o.setEscalaHorizontal(num(mmPorS)); o.setEscalaVertical(num(mmPorMv));
                        o.setMostrarAnotaciones(anotaciones.isSelected());
                        o.setIncluirMetadatos(metadatos.isSelected());
                        controlador.exportarPersonalizada(destino, v, o);
                    }
                    return "Imagen guardada en:\n" + destino;
                }));
    }

    private void exportarPaginas() {
        new SelectorArchivoExportacion().elegirDirectorio(this, GestorRutas.directorioPNG()).ifPresent(dir ->
                ejecutar(() -> "Se crearon " + controlador.exportarRegistroCompleto(dir, num(duracion)).size()
                        + " imágenes en:\n" + dir));
    }

    private interface Tarea { String ejecutar() throws Exception; }

    private void ejecutar(Tarea t) {
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception { return t.ejecutar(); }
            @Override protected void done() {
                setCursor(java.awt.Cursor.getDefaultCursor());
                try {
                    JOptionPane.showMessageDialog(VentanaImpresion.this, get());
                } catch (Exception ex) {
                    Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(VentanaImpresion.this, c.getMessage(), "Error al exportar",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
}
