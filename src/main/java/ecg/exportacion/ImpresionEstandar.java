package ecg.exportacion;

import ecg.modelo.ConfiguracionVista;
import ecg.modelo.RegistroECG;
import ecg.utilidades.ConstantesECG;
import ecg.visualizacion.RenderizadorECG;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Presentación estándar: todas las derivaciones, cuadrícula, 25 mm/s y 10 mm/mV, con encabezado.
 * Las dimensiones se calculan para que la calibración sea exacta a {@code pixelesPorMm}.
 */
public class ImpresionEstandar extends ExportadorECG {

    public static final double DURACION_PREDETERMINADA_S = 5.0;
    public static final double PIXELES_POR_MM = 5.0;   // ≈ 127 dpi

    public ConfiguracionVista crearConfiguracion(RegistroECG registro, double inicio, double duracion) {
        ConfiguracionVista c = ConfiguracionVista.paraRegistro(registro);
        List<Integer> todos = new ArrayList<>();
        for (int i = 0; i < registro.getNumeroCanales(); i++) todos.add(i);
        c.setCanalesVisibles(todos);
        double dur = Math.max(0.1, Math.min(duracion, registro.getDuracionSegundos()));
        double ini = Math.max(0, Math.min(inicio, registro.getDuracionSegundos() - dur));
        c.setTiempoInicio(ini);
        c.setTiempoFin(ini + dur);
        c.setEscalaHorizontal(ConstantesECG.MM_POR_SEGUNDO_ESTANDAR);
        c.setEscalaVertical(ConstantesECG.MM_POR_MV_ESTANDAR);
        c.setPixelesPorMm(PIXELES_POR_MM);
        c.setMostrarCuadricula(true);
        c.setMostrarAnotaciones(true);
        c.setMostrarDetecciones(true);
        c.setMostrarEncabezado(true);
        c.setMostrarCursor(false);
        return c;
    }

    public BufferedImage generar(RegistroECG registro, double inicio, double duracion) {
        ConfiguracionVista c = crearConfiguracion(registro, inicio, duracion);
        return renderizar(registro, c, calcularDimension(registro, c));
    }

    private BufferedImage renderizar(RegistroECG registro, ConfiguracionVista c, int[] wh) {
        return renderizar(registro, c, wh[0], wh[1]);
    }

    /** Tamaño en píxeles que da exactamente 25 mm/s horizontalmente y 10 mm/mV verticalmente. */
    public int[] calcularDimension(RegistroECG registro, ConfiguracionVista c) {
        int n = c.getCanalesVisibles().size();
        int[] filasCols = ecg.visualizacion.DistribucionCanales.desdeNombre(c.getDistribucion()).filasYColumnas(n);
        int celdaAncho = (int) Math.round(c.getDuracionVentana() * c.getEscalaHorizontal() * c.getPixelesPorMm());
        int celdaAlto = (int) Math.round(ConstantesECG.MM_ALTO_CANAL_ESTANDAR * c.getPixelesPorMm());
        int ancho = filasCols[1] * celdaAncho + 2 * RenderizadorECG.MARGEN;
        int alto = filasCols[0] * celdaAlto + 2 * RenderizadorECG.MARGEN + RenderizadorECG.ALTO_ENCABEZADO;
        return new int[]{ancho, alto};
    }
}
