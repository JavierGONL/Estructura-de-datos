package ecg.lectura;

import ecg.modelo.MetadatosCanal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Lógica común: lectura del archivo, salto de offset y comprobación de longitud. */
abstract class DecodificadorBase implements DecodificadorWFDB {

    /** Bytes que ocupan {@code frames} frames con {@code n} señales intercaladas. */
    protected abstract long bytesNecesarios(long frames, int n);

    /** Frames completos que caben en {@code bytes} bytes. */
    protected abstract long framesEnBytes(long bytes, int n);

    protected abstract double[][] decodificarBytes(byte[] d, int inicio, List<MetadatosCanal> canales,
                                                   int frames) throws IOException;

    @Override
    public final double[][] decodificar(Path archivo, List<MetadatosCanal> canales, long numeroFrames)
            throws IOException {
        if (numeroFrames > Integer.MAX_VALUE - 8) throw new LecturaECGException("Registro demasiado largo");
        byte[] d = Files.readAllBytes(archivo);
        int offset = canales.get(0).getDesplazamiento();
        long necesarios = bytesNecesarios(numeroFrames, canales.size());
        if (d.length - (long) offset < necesarios) {
            throw new LecturaECGException("El archivo " + archivo.getFileName() + " tiene "
                    + (d.length - offset) + " bytes de datos y se necesitan " + necesarios);
        }
        return decodificarBytes(d, offset, canales, (int) numeroFrames);
    }

    @Override
    public final long contarFrames(Path archivo, List<MetadatosCanal> canales) throws IOException {
        long bytes = Files.size(archivo) - canales.get(0).getDesplazamiento();
        return Math.max(0, framesEnBytes(bytes, canales.size()));
    }
}
