package ecg.rendimiento;

import ecg.modelo.MetricasEjecucion;
import ecg.utilidades.GeneradorRegistroSintetico;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RendimientoTest {

    @TempDir Path tmp;

    static long basura;

    private static void trabajar() {
        double[] a = new double[3_000_000];
        for (int i = 0; i < a.length; i++) a[i] = Math.sin(i);
        basura += (long) a[1234];
    }

    @Test
    void mideTiempoMemoriaYCpu() {
        RegistroMetricas reg = new RegistroMetricas();
        MonitorRendimiento mon = new MonitorRendimiento(reg);
        mon.iniciarMedicion("trabajo");
        trabajar();
        MetricasEjecucion m = mon.finalizarMedicion();

        assertEquals("trabajo", m.getNombreOperacion());
        assertTrue(m.getTiempoPared() > 0);
        assertTrue(m.getMemoriaMaxima() >= m.getMemoriaInicial());
        assertTrue(m.getMemoriaMaxima() >= m.getMemoriaFinal());
        assertTrue(m.getMemoriaReservadaJVM() > 0);
        // CPU de hilo: soportado por la JVM
        assertTrue(m.getTiempoCPUHilo() > 0);
        // en Linux hay usuario/sistema; en otros SO deben declararse no disponibles (nunca inventarse)
        if (Files.isReadable(Path.of("/proc/self/stat"))) {
            assertTrue(m.getTiempoCPUUsuario() >= 0 && m.getTiempoCPUSistema() >= 0);
            assertTrue(m.getMemoriaRSS() > 0);
        } else {
            assertEquals(MetricasEjecucion.NO_DISPONIBLE, m.getTiempoCPUUsuario());
        }
        assertEquals(1, reg.getMetricas().size());
        assertThrows(IllegalStateException.class, mon::finalizarMedicion);
    }

    @Test
    void medicionesAnidadasYCierreSiFalla() throws Exception {
        RegistroMetricas reg = new RegistroMetricas();
        MonitorRendimiento mon = new MonitorRendimiento(reg);
        mon.medir("externa", () -> { mon.medir("interna", RendimientoTest::trabajar); });
        assertThrows(IllegalStateException.class, () -> mon.medir("falla", () -> { throw new IllegalStateException("x"); }));
        List<MetricasEjecucion> l = reg.getMetricas();
        assertEquals(3, l.size());
        assertEquals("interna", l.get(0).getNombreOperacion());
        assertEquals(1, l.get(0).getProfundidad());
        assertEquals(0, l.get(1).getProfundidad());
        assertEquals("falla", l.get(2).getNombreOperacion());
        assertThrows(IllegalStateException.class, mon::finalizarMedicion, "la pila quedó vacía");

        ResultadoRendimiento r = reg.cerrarEjecucion("obs");
        assertEquals(1, reg.getHistorial().size());
        assertTrue(reg.getMetricas().isEmpty());
        assertEquals(3, r.resumirPorOperacion().size());
        long primerNivel = r.getMetricas().stream().filter(x -> x.getProfundidad() == 0)
                .mapToLong(MetricasEjecucion::getTiempoPared).sum();
        assertEquals(primerNivel, r.tiempoTotalPrimerNivelNs());
    }

    @Test
    void generaInformePdfConTablasYGraficos() throws IOException {
        RegistroMetricas reg = new RegistroMetricas();
        MonitorRendimiento mon = new MonitorRendimiento(reg);
        for (int i = 0; i < 3; i++) {
            mon.medir("Cargar registro", RendimientoTest::trabajar);
            mon.medir("Operación con un nombre excesivamente largo para provocar el recorte en la tabla", RendimientoTest::trabajar);
        }
        Path pdf = tmp.resolve("sub/informe.pdf");
        new GeneradorInformePDF().generarInforme(reg, pdf);
        byte[] b = Files.readAllBytes(pdf);
        assertEquals("%PDF", new String(b, 0, 4, StandardCharsets.ISO_8859_1));
        assertTrue(b.length > 20_000, "incluye tres gráficos: " + b.length);
        assertEquals(3, reg.getMetricas().size() - 3, "generar el informe no vacía el registro");
    }

    @Test
    void evaluacionExperimentalAgregaRepeticiones() throws IOException {
        Path hea = GeneradorRegistroSintetico.generar(tmp, "ev", 12, 500, 20, 16);
        ResultadoRendimiento r = new EvaluacionExperimental().ejecutar(hea, 3, true);
        var res = r.resumirPorOperacion();
        assertTrue(res.stream().anyMatch(x -> x.operacion.equals("Analizar QRS") && x.repeticiones == 3));
        assertTrue(res.stream().anyMatch(x -> x.operacion.equals("Decodificar señales") && x.repeticiones == 3));
        assertEquals(12, r.getNumeroCanales());
        Path pdf = tmp.resolve("eval.pdf");
        new GeneradorInformePDF().generarInforme(r, pdf);
        assertTrue(Files.size(pdf) > 20_000);
    }
}
