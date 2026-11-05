package ecg.rendimiento;

import ecg.exportacion.ImpresionEstandar;
import ecg.lectura.CargadorRegistro;
import ecg.modelo.RegistroECG;
import ecg.procesamiento.AnalizadorECG;
import ecg.procesamiento.DetectorPanTompkins;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Modo de evaluación experimental: repite el flujo completo (cargar, detectar QRS, renderizar y
 * exportar PNG) varias veces sobre un registro y agrega los resultados para compararlos.
 */
public class EvaluacionExperimental {

    /** @param descartarPrimera si es true, la primera repetición (calentamiento del JIT) no se registra */
    public ResultadoRendimiento ejecutar(Path archivoHea, int repeticiones, boolean descartarPrimera)
            throws IOException {
        RegistroMetricas registro = new RegistroMetricas();
        MonitorRendimiento monitor = new MonitorRendimiento(registro);
        CargadorRegistro cargador = new CargadorRegistro();
        AnalizadorECG analizador = new AnalizadorECG(new DetectorPanTompkins());
        ImpresionEstandar impresion = new ImpresionEstandar();
        Path temporal = Files.createTempFile("ecg_eval_", ".png");

        try {
            int total = repeticiones + (descartarPrimera ? 1 : 0);
            for (int i = 0; i < total; i++) {
                final boolean calentamiento = descartarPrimera && i == 0;
                if (calentamiento) {
                    RegistroECG r = cargador.cargarRegistro(archivoHea);
                    analizador.analizarRegistro(r);
                    impresion.generar(r, 0, 5);
                    continue;
                }
                cargador.setMonitor(monitor);
                RegistroECG r = ejecutarMedido(monitor, () -> cargador.cargarRegistro(archivoHea), "Cargar registro");
                cargador.setMonitor(null);
                registro.setInformacionRegistro(r);
                ejecutarMedido(monitor, () -> analizador.analizarRegistro(r), "Analizar QRS");
                BufferedImage img = ejecutarMedido(monitor, () -> impresion.generar(r, 0, 5), "Renderizar ECG");
                ejecutarMedido(monitor, () -> { impresion.guardar(img, temporal); return null; }, "Exportar PNG");
            }
        } finally {
            Files.deleteIfExists(temporal);
        }
        return registro.cerrarEjecucion("Evaluación experimental: " + repeticiones
                + " repeticiones" + (descartarPrimera ? " (tras una de calentamiento descartada)." : "."));
    }

    private static <T> T ejecutarMedido(MonitorRendimiento m, java.util.concurrent.Callable<T> c, String op)
            throws IOException {
        try {
            return m.medir(op, c);
        } catch (IOException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
