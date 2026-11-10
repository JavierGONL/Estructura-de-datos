package ecg.exportacion;

import ecg.lectura.CargadorRegistro;
import ecg.modelo.ConfiguracionVista;
import ecg.modelo.RegistroECG;
import ecg.utilidades.GeneradorRegistroSintetico;
import ecg.visualizacion.RenderizadorECG;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExportacionTest {

    @TempDir static Path tmp;
    static RegistroECG registro;

    @BeforeAll
    static void cargar() throws IOException {
        System.setProperty("java.awt.headless", "true");
        registro = new CargadorRegistro().cargarRegistro(
                GeneradorRegistroSintetico.generar(tmp, "e", 12, 500, 25, 212));
    }

    @Test
    void impresionEstandarTieneCalibracionExacta() throws IOException {
        ImpresionEstandar imp = new ImpresionEstandar();
        BufferedImage img = imp.generar(registro, 0, 5);
        // 3 columnas × (5 s × 25 mm/s × 5 px/mm) + márgenes ; 4 filas × (40 mm × 5 px/mm) + márgenes + encabezado
        assertEquals(3 * 625 + 2 * RenderizadorECG.MARGEN, img.getWidth());
        assertEquals(4 * 200 + 2 * RenderizadorECG.MARGEN + RenderizadorECG.ALTO_ENCABEZADO, img.getHeight());
        Path destino = tmp.resolve("sub/estandar.png");
        imp.guardar(img, destino);
        BufferedImage leida = ImageIO.read(destino.toFile());
        assertEquals(img.getWidth(), leida.getWidth());
    }

    @Test
    void personalizadaUsaLosCanalesElegidosYElTamanoPedido() throws IOException {
        ConfiguracionVista v = ConfiguracionVista.paraRegistro(registro);
        v.setCanalesVisibles(List.of(1, 6));
        v.setTiempoInicio(2); v.setTiempoFin(6);
        ConfiguracionExportacion o = new ConfiguracionExportacion();
        o.setAncho(900); o.setAlto(500);
        BufferedImage img = new ImpresionPersonalizada().generar(registro, v, o);
        assertEquals(900, img.getWidth());
        assertEquals(500, img.getHeight());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11).size(), v.getCanalesVisibles().size() + 10,
                "no modifica la configuración original");
        // tamaño automático
        o.setAncho(0); o.setAlto(0);
        BufferedImage auto = new ImpresionPersonalizada().generar(registro, v, o);
        assertTrue(auto.getWidth() > 500 && auto.getHeight() > 100);
    }

    @Test
    void vistaActualCoincidePixelAPixelConElRenderizadorDeLaPantalla() {
        ConfiguracionVista v = ConfiguracionVista.paraRegistro(registro);
        v.setCanalesVisibles(List.of(0, 1, 2, 3));
        v.setCursorTiempo(3.0); v.setCursorCanal(1);
        BufferedImage a = new ExportadorVistaActual().generar(registro, v, 1000, 700);
        BufferedImage b = new BufferedImage(1000, 700, BufferedImage.TYPE_INT_RGB);
        var g = b.createGraphics();
        new RenderizadorECG().dibujar(g, registro, v, new java.awt.Dimension(1000, 700));
        g.dispose();
        for (int y = 0; y < 700; y += 7) for (int x = 0; x < 1000; x += 5) assertEquals(b.getRGB(x, y), a.getRGB(x, y));
    }

    @Test
    void registroCompletoSeDivideEnPaginas() throws IOException {
        List<Path> p = new ExportadorRegistroCompleto().exportar(registro, 0, 0, 10, tmp.resolve("pag"));
        assertEquals(3, p.size());                                   // 25 s en páginas de 10 s
        for (Path f : p) assertTrue(Files.size(f) > 5000, f.toString());
        assertTrue(p.get(2).getFileName().toString().endsWith("_p003.png"));
    }

    @Test
    void rechazaImagenesGigantes() {
        assertThrows(IllegalArgumentException.class,
                () -> new ExportadorECG().renderizar(registro, ConfiguracionVista.paraRegistro(registro), 20000, 20000));
    }
}
