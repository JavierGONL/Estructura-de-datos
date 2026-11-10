package ecg.lectura;

import ecg.modelo.CabeceraECG;
import ecg.modelo.MetadatosCanal;
import ecg.modelo.RegistroECG;
import ecg.utilidades.GeneradorRegistroSintetico;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LecturaWFDBTest {

    @TempDir Path tmp;

    private CabeceraECG cabecera(String texto) throws IOException {
        Path p = tmp.resolve("t.hea");
        Files.writeString(p, texto);
        return new LectorCabecera().leer(p);
    }

    @Test
    void parseaGananciaBaselineUnidadesYDescripcion() throws IOException {
        CabeceraECG c = cabecera("rec 2 360 650000 12:00:00 01/01/2000\n"
                + "# comentario\n"
                + "rec.dat 212 200(1024)/mV 11 1024 995 -22131 0 MLII\n"
                + "rec.dat 212 100/uV 11 1024 1011 20052 0 V5 derivacion\n");
        assertEquals(2, c.getNumeroCanales());
        assertEquals(360.0, c.getFrecuenciaMuestreo());
        assertEquals(650000, c.getNumeroMuestras());
        MetadatosCanal a = c.getMetadatosCanales().get(0), b = c.getMetadatosCanales().get(1);
        assertEquals(200.0, a.getGananciaADC());
        assertEquals(1024, a.getBaseline());
        assertEquals("mV", a.getUnidades());
        assertEquals("MLII", a.getDescripcion());
        assertEquals(1024, b.getBaseline(), "sin baseline explícito se usa adcZero");
        assertEquals("uV", b.getUnidades());
        assertEquals("V5 derivacion", b.getDescripcion());
        assertEquals(1, c.getComentarios().size());
    }

    @Test
    void archivoDeDatosPropioDeCadaCanalYOffset() throws IOException {
        CabeceraECG c = cabecera("x 2 500 10\nuno.dat 16+44 1000/mV\ndos.bin 16 1000/mV\n");
        assertEquals("uno.dat", c.getMetadatosCanales().get(0).getArchivoDatos());
        assertEquals(44, c.getMetadatosCanales().get(0).getDesplazamiento());
        assertEquals("dos.bin", c.getMetadatosCanales().get(1).getArchivoDatos());
    }

    @Test
    void rechazaMultisegmentoYCabecerasInconsistentes() {
        assertThrows(LecturaECGException.class, () -> cabecera("rec/3 2 250 100\n"));
        assertThrows(LecturaECGException.class, () -> cabecera("rec 2 250 100\na.dat 16 200\n"));
        assertThrows(LecturaECGException.class, () -> cabecera("rec 1 250 100\na.dat 99x 200\n"));
    }

    @Test
    void formato212() throws IOException {
        int[] v = {0, -1, 2047, -2048, 5, -5, 1000};   // 7 muestras: la última queda a medias
        byte[] d = new byte[(v.length / 2) * 3 + 2];
        for (int k = 0; k + 1 < v.length; k += 2) {
            int a = v[k] & 0xFFF, b = v[k + 1] & 0xFFF, o = (k / 2) * 3;
            d[o] = (byte) a; d[o + 1] = (byte) (((a >> 8) & 0xF) | ((b >> 4) & 0xF0)); d[o + 2] = (byte) b;
        }
        int o = (v.length / 2) * 3;
        d[o] = (byte) (v[6] & 0xFF); d[o + 1] = (byte) ((v[6] >> 8) & 0xF);
        Path f = tmp.resolve("a.dat");
        Files.write(f, d);

        // 7 muestras = 7 frames de 1 canal
        MetadatosCanal m = new MetadatosCanal();
        double[][] r = new DecodificadorFormato212().decodificar(f, List.of(m), 7);
        for (int i = 0; i < v.length; i++) assertEquals(v[i], r[0][i], "muestra " + i);

        // 7 muestras = 3 frames de 2 canales (+1 sobrante) → desintercalado
        double[][] r2 = new DecodificadorFormato212().decodificar(f, List.of(m, m), 3);
        assertEquals(0, r2[0][0]); assertEquals(-1, r2[1][0]);
        assertEquals(2047, r2[0][1]); assertEquals(-2048, r2[1][1]);
        assertEquals(5, r2[0][2]); assertEquals(-5, r2[1][2]);
        assertEquals(3, new DecodificadorFormato212().contarFrames(f, List.of(m, m)));
    }

    @Test
    void formato16Formato8YEnteros() throws IOException {
        Path f16 = tmp.resolve("s16.dat");
        Files.write(f16, new byte[]{(byte) 0xFF, (byte) 0xFF, 0x10, 0x00, 0x00, (byte) 0x80, 0x01, 0x00});
        MetadatosCanal m = new MetadatosCanal();
        double[][] r = new DecodificadorFormato16().decodificar(f16, List.of(m, m), 2);
        assertArrayEquals(new double[]{-1, -32768}, r[0]);     // canal 0: frames 0 y 1
        assertArrayEquals(new double[]{16, 1}, r[1]);

        Path f8 = tmp.resolve("d8.dat");
        Files.write(f8, new byte[]{1, 1, (byte) 0xFF, 2});     // 2 canales, 2 frames
        MetadatosCanal a = new MetadatosCanal(), b = new MetadatosCanal();
        a.setValorInicial(100); b.setValorInicial(-50);
        double[][] r8 = new DecodificadorFormato8().decodificar(f8, List.of(a, b), 2);
        assertArrayEquals(new double[]{101, 100}, r8[0]);
        assertArrayEquals(new double[]{-49, -47}, r8[1]);

        Path f24 = tmp.resolve("d24.dat");
        Files.write(f24, new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x01, 0x00, 0x00});
        assertArrayEquals(new double[]{-1, 1}, new DecodificadorFormatoEntero(3).decodificar(f24, List.of(m), 2)[0]);
        assertThrows(IOException.class, () -> new DecodificadorFormato16().decodificar(f16, List.of(m), 100));
    }

    @Test
    void conversionDeUnidades() {
        MetadatosCanal m = new MetadatosCanal();
        m.setGananciaADC(200); m.setBaseline(1024); m.setFormatoWFDB(212);
        assertEquals(1.0, ConversorUnidades.convertirMuestra(1224, m), 1e-12);
        assertEquals(-0.5, ConversorUnidades.convertirMuestra(924, m), 1e-12);
        m.setUnidades("uV"); m.setGananciaADC(1000);          // 1000 ADU por µV
        assertEquals(0.001, ConversorUnidades.convertirMuestra(2024, m), 1e-12);   // 1 µV = 0,001 mV
        m.setUnidades("V"); m.setGananciaADC(1);
        assertEquals(1000.0, ConversorUnidades.convertirMuestra(1025, m), 1e-9);
        m.setUnidades("mmHg");
        assertEquals(1.0, ConversorUnidades.convertirMuestra(1025, m), 1e-12, "unidades no eléctricas no se convierten");
        m.setFormatoWFDB(16);
        assertTrue(Double.isNaN(ConversorUnidades.convertirMuestra(-32768, m)));
        m.setGananciaADC(0);
        assertThrows(IllegalArgumentException.class, () -> ConversorUnidades.convertirMuestra(1, m));
    }

    @Test
    void rechazaRegistroSinGanancia() throws IOException {
        Files.write(tmp.resolve("g.dat"), new byte[8]);
        Files.writeString(tmp.resolve("g.hea"), "g 1 250 4\ng.dat 16\n");
        LecturaECGException e = assertThrows(LecturaECGException.class,
                () -> new CargadorRegistro().cargarRegistro(tmp.resolve("g.hea")));
        assertTrue(e.getMessage().contains("ganancia"));
    }

    @Test
    void registroSinteticoDoceDerivacionesEnFormato16Y212() throws IOException {
        for (int formato : new int[]{16, 212}) {
            Path hea = GeneradorRegistroSintetico.generar(tmp.resolve("f" + formato), "s", 12, 500, 20, formato);
            RegistroECG r = new CargadorRegistro().cargarRegistro(hea);
            assertEquals(12, r.getNumeroCanales());
            assertEquals(10000, r.getNumeroMuestras());
            assertEquals(20.0, r.getDuracionSegundos(), 1e-9);
            assertEquals("aVR", r.getCanal(3).getNombre());
            assertTrue(r.getAdvertencias().isEmpty(), "checksum debe coincidir: " + r.getAdvertencias());
            assertFalse(r.getAnotaciones().isEmpty(), "carga el .atr de referencia");
            // el pico R de la derivación II ronda 1 mV; en aVR es negativo
            double max2 = 0, minAvr = 0;
            for (double v : r.getCanal(1).getMuestras()) max2 = Math.max(max2, v);
            for (double v : r.getCanal(3).getMuestras()) minAvr = Math.min(minAvr, v);
            assertEquals(1.0, max2, 0.15);
            assertTrue(minAvr < -0.6);
        }
    }

    @Test
    void numeroDeCanalesNoEstaFijado() throws IOException {
        Path hea = GeneradorRegistroSintetico.generar(tmp.resolve("dos"), "mit", 2, 360, 10, 212);
        RegistroECG r = new CargadorRegistro().cargarRegistro(hea);
        assertEquals(2, r.getNumeroCanales());
        assertEquals(3600, r.getNumeroMuestras());
    }

    @Test
    void checksumIncorrectoGeneraAdvertencia() throws IOException {
        Path hea = GeneradorRegistroSintetico.generar(tmp.resolve("ck"), "ck", 2, 250, 5, 16);
        var cs = java.nio.charset.StandardCharsets.ISO_8859_1;
        String txt = Files.readString(hea, cs).replaceFirst("(\\S+) 0 I", "12345 0 I");
        Files.writeString(hea, txt, cs);
        RegistroECG r = new CargadorRegistro().cargarRegistro(hea);
        assertFalse(r.getAdvertencias().isEmpty());
    }

    @Test
    void anotacionesConSkip() throws IOException {
        // 3 latidos "N": en t=100, t=100+5000 (usa SKIP), t=5200
        byte[] d = {
                100, 4,                 // tipo 1 (N) en bits altos: (1<<10)|100 = 0x0464 → LE 0x64,0x04
                0, (byte) (59 << 2),    // SKIP: (59<<10) = 0xEC00 → LE 0x00,0xEC
                0, 0, (byte) 0x88, 0x13,// 32 bits: alta=0, baja=5000 (0x1388)
                0, 4,                   // N con intervalo 0
                (byte) 200, 4,          // N con intervalo 200
                0, 0};
        d[0] = 0x64; d[1] = 0x04; d[3] = (byte) 0xEC; d[9] = 0x04; d[10] = (byte) 200; d[11] = 0x04;
        Path f = tmp.resolve("a.atr");
        Files.write(f, d);
        var a = new LectorAnotacionesWFDB().leer(f);
        assertEquals(3, a.size());
        assertEquals(100, a.get(0).getIndiceMuestra());
        assertEquals(5100, a.get(1).getIndiceMuestra());
        assertEquals(5300, a.get(2).getIndiceMuestra());
        assertEquals("N", a.get(0).getTipo());
        assertTrue(a.get(0).esLatido());
    }
}
