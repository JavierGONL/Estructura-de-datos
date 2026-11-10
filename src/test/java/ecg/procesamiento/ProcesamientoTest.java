package ecg.procesamiento;

import ecg.lectura.CargadorRegistro;
import ecg.modelo.AnotacionECG;
import ecg.modelo.RegistroECG;
import ecg.modelo.ResultadoDeteccion;
import ecg.utilidades.GeneradorRegistroSintetico;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProcesamientoTest {

    @TempDir static Path tmp;
    static RegistroECG registro;

    @BeforeAll
    static void cargar() throws IOException {
        Path hea = GeneradorRegistroSintetico.generar(tmp, "p", 12, 500, 60, 16);
        registro = new CargadorRegistro().cargarRegistro(hea);
    }

    @Test
    void panTompkinsEncuentraLosLatidosDeReferencia() {
        List<Long> ref = ValidadorDeteccion.indicesReferencia(registro);
        assertTrue(ref.size() > 60);
        DetectorQRS det = new DetectorPanTompkins();
        int bien = 0;
        for (int c : new int[]{0, 1, 3, 6, 11}) {       // incluye aVR (R negativa) y V1
            List<Long> d = det.detectar(registro.getCanal(c).getMuestras(), 500);
            ResultadoValidacion v = new ValidadorDeteccion().validar(d, ref, 500, 0.10);
            assertTrue(v.getSensibilidad() > 0.97, registro.getCanal(c).getNombre() + ": " + v);
            assertTrue(v.getPrecision() > 0.97, registro.getCanal(c).getNombre() + ": " + v);
            assertTrue(v.getErrorTemporalMedioMs() < 30);
            bien++;
        }
        assertEquals(5, bien);
    }

    @Test
    void analizadorFusionaYPublicaAnotaciones() {
        AnalizadorECG a = new AnalizadorECG(new DetectorPanTompkins());
        ResultadoDeteccion r = a.analizarRegistro(registro);
        ResultadoValidacion v = new ValidadorDeteccion().validar(r.getIndicesDetectados(),
                ValidadorDeteccion.indicesReferencia(registro), 500, 0.10);
        assertTrue(v.getF1() > 0.98, v.toString());
        assertEquals(r.getIndicesDetectados().size(), registro.getAnotaciones(AnotacionECG.FUENTE_ALGORITMO).size());
        // repetir no duplica
        a.analizarRegistro(registro);
        assertEquals(r.getIndicesDetectados().size(), registro.getAnotaciones(AnotacionECG.FUENTE_ALGORITMO).size());
    }

    @Test
    void evaluadorDeCanalesDevuelveUnResultadoPorDerivacion() {
        var res = new EvaluadorCanales().evaluar(registro, new DetectorPanTompkins(), 0.1);
        assertEquals(12, res.size());
        assertNotNull(res.get(0).validacion);
    }

    @Test
    void validadorContaTpFpFn() {
        ResultadoValidacion v = new ValidadorDeteccion().validar(
                List.of(100L, 200L, 305L, 900L), List.of(100L, 205L, 300L, 600L), 100, 0.1);
        // tolerancia 10 muestras: 100✓, 200~205✓, 305~300✓, 900 FP, 600 FN
        assertEquals(3, v.getVerdaderosPositivos());
        assertEquals(1, v.getFalsosPositivos());
        assertEquals(1, v.getFalsosNegativos());
        assertEquals(0.75, v.getSensibilidad(), 1e-12);
        assertEquals(0.75, v.getPrecision(), 1e-12);
        assertEquals(0.75, v.getF1(), 1e-12);
        assertEquals(1000.0 * (0 + 5 + 5) / 3 / 100, v.getErrorTemporalMedioMs(), 1e-9);
    }

    @Test
    void rrYFrecuenciaCardiaca() {
        AnalizadorRR rr = new AnalizadorRR();
        double[] d = rr.calcularRR(List.of(0L, 500L, 1000L, 1600L), 500);
        assertArrayEquals(new double[]{1.0, 1.0, 1.2}, d, 1e-12);
        double[] fc = new AnalizadorFrecuenciaCardiaca().instantanea(d);
        assertEquals(60.0, fc[0], 1e-12);
        assertEquals(50.0, fc[2], 1e-12);
        assertEquals(60.0 / (3.2 / 3), new AnalizadorFrecuenciaCardiaca().media(d), 1e-12);
        assertEquals(0, rr.calcularRR(List.of(5L), 500).length);
    }

    @Test
    void fusionExigeConsenso() {
        double fs = 100;
        var a = new ResultadoDeteccion(List.of(100L, 300L), fs, "x", 0);
        var b = new ResultadoDeteccion(List.of(102L, 500L), fs, "x", 1);
        var c = new ResultadoDeteccion(List.of(99L), fs, "x", 2);
        List<Long> f = new FusionDetecciones().fusionar(List.of(a, b, c), 0.1, fs, 2);
        assertEquals(1, f.size());
        assertTrue(Math.abs(f.get(0) - 100) <= 2);
    }
}
