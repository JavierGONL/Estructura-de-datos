package ecg.visualizacion;

import ecg.lectura.CargadorRegistro;
import ecg.modelo.ConfiguracionVista;
import ecg.modelo.RegistroECG;
import ecg.utilidades.GeneradorRegistroSintetico;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VisualizacionTest {

    @TempDir static Path tmp;
    static RegistroECG registro;

    @BeforeAll
    static void cargar() throws IOException {
        System.setProperty("java.awt.headless", "true");
        registro = new CargadorRegistro().cargarRegistro(
                GeneradorRegistroSintetico.generar(tmp, "v", 12, 500, 30, 16));
    }

    @Test
    void distribuciones() {
        Rectangle area = new Rectangle(0, 0, 900, 800);
        List<Rectangle> g = DistribucionCanales.CUADRICULA_3X4.calcular(12, area);
        assertEquals(12, g.size());
        assertEquals(300, g.get(0).width);
        assertEquals(200, g.get(0).height);
        assertEquals(300, g.get(1).x);
        assertEquals(200, g.get(3).y);
        assertEquals(12, DistribucionCanales.VERTICAL.calcular(12, area).size());
        assertEquals(1, DistribucionCanales.INDIVIDUAL.calcular(12, area).size());
        assertEquals(area, DistribucionCanales.INDIVIDUAL.calcular(5, area).get(0));
        assertEquals(2, DistribucionCanales.CUADRICULA_3X4.calcular(2, area).size());
        assertTrue(DistribucionCanales.AUTOMATICA.calcular(0, area).isEmpty());
    }

    @Test
    void renderizaTrazosYRespetaLaSeleccionDeCanales() {
        ConfiguracionVista c = ConfiguracionVista.paraRegistro(registro);
        RenderizadorECG r = new RenderizadorECG();
        Dimension dim = new Dimension(1200, 800);
        assertEquals(12, r.calcularCeldas(registro, c, dim).size());

        c.setCanalesVisibles(List.of(1, 3, 99, -1, 1));     // inválidos y repetidos se ignoran
        assertEquals(List.of(1, 3), r.canalesEfectivos(registro, c));
        c.setDistribucion("INDIVIDUAL");
        assertEquals(1, r.calcularCeldas(registro, c, dim).size());

        c.setDistribucion("CUADRICULA_3X4");
        c.setCanalesVisibles(List.of(0, 1, 2));
        c.setTiempoInicio(0); c.setTiempoFin(5);
        BufferedImage img = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        r.dibujar(g, registro, c, dim);
        g.dispose();
        long oscuros = 0;
        for (int y = 0; y < dim.height; y += 2) for (int x = 0; x < dim.width; x += 2) {
            int p = img.getRGB(x, y);
            if (((p >> 16) & 0xFF) < 60 && ((p >> 8) & 0xFF) < 60) oscuros++;
        }
        assertTrue(oscuros > 200, "debe haber trazo negro; píxeles oscuros: " + oscuros);
    }

    @Test
    void zoomDesplazamientoYLimites() {
        ConfiguracionVista c = ConfiguracionVista.paraRegistro(registro);   // 0–10 s de 30 s
        GestorZoom z = new GestorZoom(registro, c);
        GestorDesplazamiento d = new GestorDesplazamiento(registro, c);

        z.zoomHorizontal(2);
        assertEquals(5.0, c.getDuracionVentana(), 1e-9);
        assertEquals(2.5, c.getTiempoInicio(), 1e-9);               // centrado
        z.zoomHorizontal(100);
        assertTrue(c.getDuracionVentana() >= 0.2 - 1e-9);           // mínimo
        z.zoomHorizontal(0.001);
        assertEquals(30.0, c.getDuracionVentana(), 1e-9);           // máximo = registro
        assertEquals(0.0, c.getTiempoInicio(), 1e-9);

        z.restablecerZoom();
        d.desplazarAdelante();
        assertEquals(5.0, c.getTiempoInicio(), 1e-9);
        d.desplazarA(1000);
        assertEquals(20.0, c.getTiempoInicio(), 1e-9);
        assertEquals(30.0, c.getTiempoFin(), 1e-9);
        d.desplazarAtras();
        d.restablecerPosicion();
        assertEquals(0.0, c.getTiempoInicio(), 1e-9);

        double antes = registro.getCanal(0).getMuestra(100);
        z.zoomVertical(2);
        assertEquals(20.0, c.getEscalaVertical(), 1e-9);
        z.zoomVertical(1e9);
        assertEquals(GestorZoom.ESCALA_VERTICAL_MAX, c.getEscalaVertical());
        assertEquals(antes, registro.getCanal(0).getMuestra(100), "el zoom no altera los datos");
    }

    @Test
    void zoomEnElCursorMantieneElInstanteBajoElRaton() {
        ConfiguracionVista c = ConfiguracionVista.paraRegistro(registro);
        GestorZoom z = new GestorZoom(registro, c);
        z.zoomHorizontal(2, 2.0);                                     // 2,0 s está al 20 % de la ventana
        assertEquals(1.0, c.getTiempoInicio(), 1e-9);
        double rel = (4.0 - c.getTiempoInicio()) / c.getDuracionVentana();
        z.zoomHorizontal(2, 4.0);
        assertEquals(rel, (4.0 - c.getTiempoInicio()) / c.getDuracionVentana(), 1e-9);
    }

    @Test
    void seleccionDeCanalesIntervaloYCursor() {
        ConfiguracionVista c = ConfiguracionVista.paraRegistro(registro);
        RenderizadorECG r = new RenderizadorECG();
        GestoresVista gv = new GestoresVista(registro, c, r);

        gv.canales.limpiarSeleccion();
        assertTrue(c.getCanalesVisibles().isEmpty());
        gv.canales.seleccionarCanal(5); gv.canales.seleccionarCanal(2); gv.canales.seleccionarCanal(5);
        assertEquals(List.of(2, 5), c.getCanalesVisibles());
        gv.canales.deseleccionarCanal(2);
        assertEquals(List.of(5), c.getCanalesVisibles());
        gv.canales.seleccionarTodos();
        assertEquals(12, c.getCanalesVisibles().size());

        gv.intervalo.establecerInicio(4); gv.intervalo.establecerFin(2);
        assertEquals(2.0, c.getIntervaloSeleccionado().getTiempoInicio());
        assertEquals(2.0, gv.intervalo.getIntervaloSeleccionado().getDuracion());
        gv.intervalo.limpiarSeleccion();
        assertNull(c.getIntervaloSeleccionado());

        Dimension dim = new Dimension(1200, 800);
        Rectangle celda = r.calcularCeldas(registro, c, dim).get(1);       // derivación II
        Point p = new Point(celda.x + celda.width / 2, celda.y + celda.height / 2);
        var info = gv.cursor.actualizar(p, dim);
        assertNotNull(info);
        assertEquals("II", info.nombreCanal);
        assertEquals(5.0, info.tiempoSegundos, 0.02);                       // mitad de 0–10 s
        assertEquals(registro.getCanal(1).getMuestra((int) info.indiceMuestra), info.amplitudMv);
        assertNull(gv.cursor.actualizar(new Point(0, 0), dim), "fuera de toda celda");
    }
}
