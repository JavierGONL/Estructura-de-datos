package ecg.utilidades;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Operaciones auxiliares sobre archivos y directorios. */
public final class GestorArchivos {
    private GestorArchivos() { }

    /** Crea el directorio padre del archivo si no existe. */
    public static void asegurarDirectorioPadre(Path archivo) throws IOException {
        Path padre = archivo.toAbsolutePath().getParent();
        if (padre != null) Files.createDirectories(padre);
    }

    public static boolean esCabeceraWFDB(Path p) {
        return p.getFileName().toString().toLowerCase().endsWith(ConstantesECG.EXTENSION_CABECERA);
    }
}
