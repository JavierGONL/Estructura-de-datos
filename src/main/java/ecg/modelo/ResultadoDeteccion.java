package ecg.modelo;

import java.util.ArrayList;
import java.util.List;

/** Resultado de un algoritmo de detección de QRS. */
public class ResultadoDeteccion {

    private final List<Long> indicesDetectados;
    private final List<Double> tiemposDetectados;
    private final String nombreAlgoritmo;
    private final int canalUtilizado;   // -1 = fusión de varios canales

    public ResultadoDeteccion(List<Long> indices, double frecuenciaMuestreo, String algoritmo, int canal) {
        this.indicesDetectados = new ArrayList<>(indices);
        this.tiemposDetectados = new ArrayList<>(indices.size());
        for (long i : indices) tiemposDetectados.add(i / frecuenciaMuestreo);
        this.nombreAlgoritmo = algoritmo;
        this.canalUtilizado = canal;
    }

    public List<Long> getIndicesDetectados() { return indicesDetectados; }
    public List<Double> getTiemposDetectados() { return tiemposDetectados; }
    public String getNombreAlgoritmo() { return nombreAlgoritmo; }
    public int getCanalUtilizado() { return canalUtilizado; }
}
