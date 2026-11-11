package ecg.visualizacion;

import ecg.lectura.CargadorRegistro;
import ecg.modelo.ConfiguracionVista;
import ecg.modelo.RegistroECG;
import ecg.utilidades.GeneradorRegistroSintetico;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica la caché de la escena base del visor: mover el cursor del ratón NO cambia la firma de la
 * escena (se reutiliza la imagen cacheada), mientras que cambiar el zoom/ventana SÍ la invalida.
 */
class CacheEscenaTest {

    @TempDir static Path tmp;
    static RegistroECG registro;

    @BeforeAll
    static void cargar() throws IOException {
        System.setProperty("java.awt.headless", "true");
        registro = new CargadorRegistro().cargarRegistro(
                GeneradorRegistroSintetico.generar(tmp, "cache", 2, 360, 20, 16));
    }

    private String firma(PanelECG panel, Dimension dim) throws Exception {
        Method mth = PanelECG.class.getDeclaredMethod("firmaEscena", Dimension.class);
        mth.setAccessible(true);
        return (String) mth.invoke(panel, dim);
    }

    @Test
    void cursorNoInvalidaPeroZoomSi() throws Exception {
        ConfiguracionVista c = ConfiguracionVista.paraRegistro(registro);
        PanelECG panel = new PanelECG(registro, c);
        Dimension dim = new Dimension(800, 600);

        String base = firma(panel, dim);

        // Mover el cursor del ratón: la firma de la escena base NO debe cambiar.
        c.setCursorTiempo(3.21);
        c.setCursorCanal(0);
        assertEquals(base, firma(panel, dim), "mover el cursor no invalida la escena cacheada");

        // Cambiar la ventana de tiempo (zoom/desplazamiento): la firma SÍ debe cambiar.
        c.setTiempoFin(c.getTiempoFin() / 2);
        assertNotEquals(base, firma(panel, dim), "cambiar el zoom invalida la escena cacheada");
    }

    @Test
    void escenaBaseYConCursorSeDibujanSinError() {
        ConfiguracionVista c = ConfiguracionVista.paraRegistro(registro);
        c.setMostrarCursor(true);
        c.setCursorTiempo(2.0);
        c.setCursorCanal(0);
        RenderizadorECG r = new RenderizadorECG();
        Dimension dim = new Dimension(640, 480);
        BufferedImage img = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
        var g = img.createGraphics();
        assertDoesNotThrow(() -> {
            r.dibujar(g, registro, c, dim, false);       // escena base sin cursor
            r.dibujarSoloCursor(g, registro, c, dim);    // solo cursor encima
        });
        g.dispose();
    }
}
