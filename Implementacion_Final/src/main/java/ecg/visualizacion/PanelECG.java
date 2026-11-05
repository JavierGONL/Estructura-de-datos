package ecg.visualizacion;

import ecg.modelo.ConfiguracionVista;
import ecg.modelo.RegistroECG;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

/** Panel que muestra una representación estática del ECG; delega todo el dibujo en {@link RenderizadorECG}. */
public class PanelECG extends JPanel {

    protected RegistroECG registro;
    protected ConfiguracionVista configuracion;
    protected final RenderizadorECG renderizador = new RenderizadorECG();

    public PanelECG(RegistroECG registro, ConfiguracionVista configuracion) {
        this.registro = registro;
        this.configuracion = configuracion;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(1100, 700));
    }

    public RegistroECG getRegistro() { return registro; }
    public ConfiguracionVista getConfiguracion() { return configuracion; }
    public RenderizadorECG getRenderizador() { return renderizador; }

    public void setConfiguracion(ConfiguracionVista c) { this.configuracion = c; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            renderizador.dibujar(g2, registro, configuracion, getSize());
        } finally {
            g2.dispose();
        }
    }
}
