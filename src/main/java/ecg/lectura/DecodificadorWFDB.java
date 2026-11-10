package ecg.lectura;

import ecg.modelo.MetadatosCanal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Decodifica un archivo de señales WFDB. Todos los canales recibidos comparten el mismo archivo
 * y el mismo formato, y se devuelven en el orden declarado en la cabecera.
 */
public interface DecodificadorWFDB {

    /** @return valores digitales (ADC) en {@code [canal][frame]} */
    double[][] decodificar(Path archivo, List<MetadatosCanal> canales, long numeroFrames) throws IOException;

    /** Número de frames completos que contiene el archivo (útil si la cabecera no declara la longitud). */
    long contarFrames(Path archivo, List<MetadatosCanal> canales) throws IOException;
}
