package ecg.exportacion;

import ecg.modelo.ConfiguracionVista;
import ecg.modelo.RegistroECG;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/** Exporta exactamente lo que muestra el visor: mismos canales, zoom, intervalo y marcadores. */
public class ExportadorVistaActual extends ExportadorECG {

    /**
     * @param configuracion configuración vigente del visor (no se modifica)
     * @param ancho,alto    tamaño del componente en pantalla (para conservar las proporciones) o el deseado
     */
    public BufferedImage generar(RegistroECG registro, ConfiguracionVista configuracion, int ancho, int alto) {
        return renderizar(registro, configuracion.copiar(), ancho, alto);
    }

    public void exportar(RegistroECG registro, ConfiguracionVista configuracion, int ancho, int alto,
                         Path destino) throws IOException {
        guardar(generar(registro, configuracion, ancho, alto), destino);
    }
}
