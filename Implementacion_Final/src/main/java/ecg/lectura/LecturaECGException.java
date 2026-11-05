package ecg.lectura;

import java.io.IOException;

/** Error al interpretar un registro WFDB (cabecera mal formada, formato no soportado, etc.). */
public class LecturaECGException extends IOException {
    public LecturaECGException(String mensaje) { super(mensaje); }
    public LecturaECGException(String mensaje, Throwable causa) { super(mensaje, causa); }
}
