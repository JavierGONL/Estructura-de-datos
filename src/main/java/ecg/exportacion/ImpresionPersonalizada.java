package ecg.exportacion;

import ecg.modelo.ConfiguracionVista;
import ecg.modelo.RegistroECG;

import java.awt.image.BufferedImage;

/** Imagen con las derivaciones, el intervalo, las escalas y las dimensiones elegidas por el usuario. */
public class ImpresionPersonalizada extends ExportadorECG {

    /**
     * @param vista base (canales, distribución, intervalo); no se modifica
     * @param opciones dimensiones y opciones de salida; ancho/alto &lt;= 0 → tamaño automático
     */
    public BufferedImage generar(RegistroECG registro, ConfiguracionVista vista, ConfiguracionExportacion opciones) {
        ConfiguracionVista c = limpiaDeInspeccion(vista);
        c.setMostrarCuadricula(opciones.isMostrarCuadricula());
        c.setMostrarAnotaciones(opciones.isMostrarAnotaciones());
        c.setMostrarDetecciones(opciones.isMostrarAnotaciones());
        c.setMostrarEncabezado(opciones.isIncluirMetadatos());
        c.setMostrarCursor(false);
        c.setEscalaHorizontal(opciones.getEscalaHorizontal());
        c.setEscalaVertical(opciones.getEscalaVertical());
        c.setPixelesPorMm(ImpresionEstandar.PIXELES_POR_MM);

        int ancho = opciones.getAncho(), alto = opciones.getAlto();
        if (ancho <= 0 || alto <= 0) {
            int n = Math.max(1, c.getCanalesVisibles().size());
            int[] fc = ecg.visualizacion.DistribucionCanales.desdeNombre(c.getDistribucion()).filasYColumnas(n);
            if (ancho <= 0) ancho = (int) Math.round(fc[1] * c.getDuracionVentana() * c.getEscalaHorizontal()
                    * c.getPixelesPorMm()) + 2 * ecg.visualizacion.RenderizadorECG.MARGEN;
            if (alto <= 0) alto = fc[0] * (int) Math.round(40 * c.getPixelesPorMm())
                    + 2 * ecg.visualizacion.RenderizadorECG.MARGEN
                    + (c.isMostrarEncabezado() ? ecg.visualizacion.RenderizadorECG.ALTO_ENCABEZADO : 0);
        }
        return renderizar(registro, c, ancho, alto);
    }
}
