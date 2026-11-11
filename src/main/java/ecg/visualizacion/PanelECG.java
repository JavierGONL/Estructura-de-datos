package ecg.visualizacion;

import ecg.modelo.ConfiguracionVista;
import ecg.modelo.IntervaloTemporal;
import ecg.modelo.RegistroECG;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Panel que muestra una representación del ECG; delega el dibujo en {@link RenderizadorECG}.
 *
 * <p>Para que el visor vaya fluido, la "escena base" (cuadrícula, señal, selección y anotaciones) se
 * cachea en una imagen y solo se regenera cuando cambia algo que la afecta (tamaño, zoom, ventana de
 * tiempo, canales, anotaciones visibles, intervalo…). Mover el ratón solo repinta el cursor encima de
 * esa imagen, sin recalcular la señal ni recorrer todas las anotaciones.
 */
public class PanelECG extends JPanel {

    protected RegistroECG registro;
    protected ConfiguracionVista configuracion;
    protected final RenderizadorECG renderizador = new RenderizadorECG();

    private BufferedImage cacheEscena;
    private String firmaCache;   // identifica la escena base cacheada (sin el cursor del ratón)

    public PanelECG(RegistroECG registro, ConfiguracionVista configuracion) {
        this.registro = registro;
        this.configuracion = configuracion;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(1100, 700));
    }

    public RegistroECG getRegistro() { return registro; }
    public ConfiguracionVista getConfiguracion() { return configuracion; }
    public RenderizadorECG getRenderizador() { return renderizador; }

    public void setConfiguracion(ConfiguracionVista c) { this.configuracion = c; invalidarCache(); repaint(); }

    /** Fuerza regenerar la escena base en el próximo repaint (p. ej. si cambian las anotaciones). */
    public void invalidarCache() { firmaCache = null; }

    /**
     * Firma de todo lo que afecta a la escena base. El cursor del ratón (cursorTiempo/cursorCanal) se
     * excluye a propósito: su cambio no debe invalidar la imagen cacheada.
     */
    private String firmaEscena(Dimension dim) {
        ConfiguracionVista c = configuracion;
        IntervaloTemporal iv = c.getIntervaloSeleccionado();
        long anotaciones = registro == null ? 0 : registro.getAnotaciones().size();
        return new StringBuilder(96)
                .append(dim.width).append('x').append(dim.height)
                .append('|').append(c.getTiempoInicio()).append(',').append(c.getTiempoFin())
                .append('|').append(c.getEscalaVertical()).append(',').append(c.getPixelesPorMm())
                .append('|').append(c.getDistribucion())
                .append('|').append(c.getCanalesVisibles())
                .append('|').append(c.isMostrarCuadricula()).append(c.isMostrarAnotaciones())
                .append(c.isMostrarDetecciones()).append(c.isMostrarClasificacion())
                .append(c.isMostrarNombres()).append(c.isMostrarEncabezado())
                .append('|').append(iv == null ? "-" : iv.getTiempoInicio() + "," + iv.getTiempoFin())
                .append('|').append(c.getAnotacionSeleccionada() == null ? "-"
                        : System.identityHashCode(c.getAnotacionSeleccionada()))
                .append('|').append(anotaciones)
                .toString();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Dimension dim = getSize();
        if (dim.width <= 0 || dim.height <= 0) return;

        String firma = firmaEscena(dim);
        if (cacheEscena == null || cacheEscena.getWidth() != dim.width
                || cacheEscena.getHeight() != dim.height || !firma.equals(firmaCache)) {
            cacheEscena = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gc = cacheEscena.createGraphics();
            try {
                renderizador.dibujar(gc, registro, configuracion, dim, false);   // escena sin el cursor
            } finally {
                gc.dispose();
            }
            firmaCache = firma;
        }

        g.drawImage(cacheEscena, 0, 0, null);

        // El cursor del ratón se pinta en vivo sobre la imagen cacheada (barato).
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            renderizador.dibujarSoloCursor(g2, registro, configuracion, dim);
        } finally {
            g2.dispose();
        }
    }
}
