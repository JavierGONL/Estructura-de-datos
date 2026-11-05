package ecg.exportacion;

import ecg.utilidades.GestorArchivos;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/** Componente de bajo nivel que escribe un archivo PNG. */
public class ExportadorPNG {

    public void exportar(BufferedImage imagen, Path destino) throws IOException {
        GestorArchivos.asegurarDirectorioPadre(destino);
        if (!ImageIO.write(imagen, "png", destino.toFile())) {
            throw new IOException("No hay escritor PNG disponible");
        }
    }
}
