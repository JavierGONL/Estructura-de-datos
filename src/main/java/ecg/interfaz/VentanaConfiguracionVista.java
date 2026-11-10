package ecg.interfaz;

import ecg.modelo.ConfiguracionVista;
import ecg.visualizacion.DistribucionCanales;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.FlowLayout;
import java.awt.GridLayout;

/** Preferencias de visualización y distribución. */
public class VentanaConfiguracionVista extends JDialog {

    public VentanaConfiguracionVista(JFrame padre, ControladorECG controlador) {
        super(padre, "Configuración de la vista", true);
        ConfiguracionVista c = controlador.getConfiguracion();

        JCheckBox cuadricula = new JCheckBox("Mostrar cuadrícula", c.isMostrarCuadricula());
        JCheckBox anot = new JCheckBox("Mostrar anotaciones de referencia", c.isMostrarAnotaciones());
        JCheckBox det = new JCheckBox("Mostrar detecciones del algoritmo", c.isMostrarDetecciones());
        JCheckBox cursor = new JCheckBox("Mostrar cursor", c.isMostrarCursor());
        JCheckBox nombres = new JCheckBox("Mostrar nombres de derivación", c.isMostrarNombres());
        JCheckBox encabezado = new JCheckBox("Mostrar encabezado con metadatos", c.isMostrarEncabezado());
        JComboBox<DistribucionCanales> dist = new JComboBox<>(DistribucionCanales.values());
        dist.setSelectedItem(DistribucionCanales.desdeNombre(c.getDistribucion()));
        JSpinner ppm = new JSpinner(new SpinnerNumberModel(c.getPixelesPorMm(), 1.0, 12.0, 0.5));
        JSpinner mmmv = new JSpinner(new SpinnerNumberModel(c.getEscalaVertical(), 0.5, 400.0, 1.0));

        JPanel centro = new JPanel(new GridLayout(0, 1, 2, 2));
        centro.setBorder(BorderFactory.createEmptyBorder(10, 12, 4, 12));
        for (var x : new JCheckBox[]{cuadricula, anot, det, cursor, nombres, encabezado}) centro.add(x);
        JPanel f1 = new JPanel(new FlowLayout(FlowLayout.LEFT)); f1.add(new JLabel("Distribución:")); f1.add(dist);
        JPanel f2 = new JPanel(new FlowLayout(FlowLayout.LEFT)); f2.add(new JLabel("Píxeles por mm:")); f2.add(ppm);
        JPanel f3 = new JPanel(new FlowLayout(FlowLayout.LEFT)); f3.add(new JLabel("Amplitud (mm/mV):")); f3.add(mmmv);
        centro.add(f1); centro.add(f2); centro.add(f3);

        JButton ok = new JButton("Aplicar"), cancelar = new JButton("Cancelar");
        ok.addActionListener(e -> {
            c.setMostrarCuadricula(cuadricula.isSelected());
            c.setMostrarAnotaciones(anot.isSelected());
            c.setMostrarDetecciones(det.isSelected());
            c.setMostrarCursor(cursor.isSelected());
            c.setMostrarNombres(nombres.isSelected());
            c.setMostrarEncabezado(encabezado.isSelected());
            c.setDistribucion(((DistribucionCanales) dist.getSelectedItem()).name());
            c.setPixelesPorMm(((Number) ppm.getValue()).doubleValue());
            c.setEscalaVertical(((Number) mmmv.getValue()).doubleValue());
            controlador.notificarCambio();
            dispose();
        });
        cancelar.addActionListener(e -> dispose());
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sur.add(ok); sur.add(cancelar);

        add(centro, java.awt.BorderLayout.CENTER);
        add(sur, java.awt.BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(padre);
    }
}
