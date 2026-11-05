package ecg.exportacion;

import ecg.modelo.ConfiguracionVista;
import ecg.modelo.RegistroECG;
import ecg.visualizacion.RenderizadorECG;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Base común de todas las exportaciones: dibuja siempre con {@link RenderizadorECG} para que la
 * imagen exportada coincida con lo que se ve en pantalla.
 */
public class ExportadorECG {

    private static final long MAX_PIXELES = 120_000_000L;
    protected final RenderizadorECG renderizador = new RenderizadorECG();
    protected final ExportadorPNG exportadorPNG = new ExportadorPNG();

    public BufferedImage renderizar(RegistroECG registro, ConfiguracionVista config, int ancho, int alto) {
        if ((long) ancho * alto > MAX_PIXELES) {
            throw new IllegalArgumentException("La imagen solicitada (" + ancho + "×" + alto
                    + " px) es demasiado grande; reduzca el intervalo o las escalas");
        }
        BufferedImage img = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            renderizador.dibujar(g, registro, config, new Dimension(ancho, alto));
        } finally {
            g.dispose();
        }
        return img;
    }

    public void guardar(BufferedImage imagen, Path destino) throws IOException {
        exportadorPNG.exportar(imagen, destino);
    }

    /** Copia de la configuración sin estado de inspección (el cursor no debe aparecer en las impresiones). */
    protected static ConfiguracionVista limpiaDeInspeccion(ConfiguracionVista origen) {
        ConfiguracionVista c = origen.copiar();
        c.setCursorTiempo(Double.NaN);
        c.setCursorCanal(-1);
        c.setIntervaloSeleccionado(null);
        c.setAnotacionSeleccionada(null);
        return c;
    }
}
