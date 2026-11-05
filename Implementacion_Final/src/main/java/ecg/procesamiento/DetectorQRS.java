package ecg.procesamiento;

import java.util.List;

/** Algoritmo de detección de complejos QRS sobre un único canal. */
public interface DetectorQRS {

    /** @return índices de muestra de los picos R detectados, en orden creciente */
    List<Long> detectar(double[] muestras, double frecuenciaMuestreo);

    String getNombre();
}
