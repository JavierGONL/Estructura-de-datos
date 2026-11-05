package ecg.interfaz;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import java.awt.FlowLayout;

/** Desplazamiento, inicio/fin de la ventana y restablecimiento temporal. */
public class PanelControlTemporal extends JPanel {

    private final ControladorECG c;
    private final JTextField inicio = new JTextField(6), fin = new JTextField(6);
    private final JSlider deslizador = new JSlider(0, 1000, 0);
    private boolean actualizando;

    public PanelControlTemporal(ControladorECG c) {
        this.c = c;
        setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));
        setBorder(BorderFactory.createTitledBorder("Navegación temporal"));

        JButton atras = new JButton("◀"), adelante = new JButton("▶"), aplicar = new JButton("Aplicar"),
                reset = new JButton("Inicio");
        atras.setToolTipText("Retroceder media ventana (←)");
        adelante.setToolTipText("Avanzar media ventana (→)");
        atras.addActionListener(e -> c.desplazarAtras());
        adelante.addActionListener(e -> c.desplazarAdelante());
        reset.addActionListener(e -> c.desplazarA(0));
        aplicar.addActionListener(e -> aplicarCampos());
        inicio.addActionListener(e -> aplicarCampos());
        fin.addActionListener(e -> aplicarCampos());
        deslizador.setPreferredSize(new java.awt.Dimension(220, 24));
        deslizador.addChangeListener(e -> {
            if (actualizando || !c.hayRegistro()) return;
            double libre = Math.max(0, c.getRegistro().getDuracionSegundos() - c.getConfiguracion().getDuracionVentana());
            c.desplazarA(libre * deslizador.getValue() / 1000.0);
        });

        add(reset); add(atras); add(deslizador); add(adelante);
        add(new JLabel("Inicio (s)")); add(inicio);
        add(new JLabel("Fin (s)")); add(fin);
        add(aplicar);
    }

    private void aplicarCampos() {
        try {
            double a = Double.parseDouble(inicio.getText().trim().replace(',', '.'));
            double b = Double.parseDouble(fin.getText().trim().replace(',', '.'));
            if (b > a) c.establecerVentana(a, b);
        } catch (NumberFormatException ignorada) {
            refrescar();
        }
    }

    /** Actualiza campos y deslizador con la ventana actual. */
    public void refrescar() {
        if (!c.hayRegistro()) return;
        actualizando = true;
        var cfg = c.getConfiguracion();
        inicio.setText(String.format("%.2f", cfg.getTiempoInicio()));
        fin.setText(String.format("%.2f", cfg.getTiempoFin()));
        double libre = c.getRegistro().getDuracionSegundos() - cfg.getDuracionVentana();
        deslizador.setValue(libre <= 1e-9 ? 0 : (int) Math.round(1000 * cfg.getTiempoInicio() / libre));
        actualizando = false;
    }
}
