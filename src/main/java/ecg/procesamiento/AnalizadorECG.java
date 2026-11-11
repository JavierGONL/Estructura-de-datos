package ecg.procesamiento;

import ecg.modelo.AnotacionECG;
import ecg.modelo.RegistroECG;
import ecg.modelo.ResultadoDeteccion;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordina los análisis sobre un registro. Las detecciones se guardan en el propio registro como
 * anotaciones con fuente {@link AnotacionECG#FUENTE_ALGORITMO}.
 */
public class AnalizadorECG {

    public static final double VENTANA_FUSION_S = 0.10;

    private final DetectorQRS detector;
    private final FusionDetecciones fusion = new FusionDetecciones();
    private List<ResultadoDeteccion> ultimosPorCanal = new ArrayList<>();

    public AnalizadorECG(DetectorQRS detector) { this.detector = detector; }

    public DetectorQRS getDetector() { return detector; }
    public List<ResultadoDeteccion> getUltimosResultadosPorCanal() { return ultimosPorCanal; }

    /** Detecta en todos los canales y fusiona por mayoría. */
    public ResultadoDeteccion analizarRegistro(RegistroECG registro) {
        double fs = registro.getFrecuenciaMuestreo();
        int nCanales = registro.getNumeroCanales();

        // La detección de cada canal es independiente: se paraleliza para aprovechar los núcleos
        // disponibles (clave con 12 derivaciones). El resultado por índice es determinista.
        ResultadoDeteccion[] resultados = new ResultadoDeteccion[nCanales];
        java.util.stream.IntStream.range(0, nCanales).parallel().forEach(i ->
                resultados[i] = new ResultadoDeteccion(
                        detector.detectar(registro.getCanal(i).getMuestras(), fs), fs, detector.getNombre(), i));
        List<ResultadoDeteccion> porCanal = new ArrayList<>(nCanales);
        for (ResultadoDeteccion r : resultados) porCanal.add(r);
        ultimosPorCanal = porCanal;
        int votos = FusionDetecciones.votosPorMayoria(registro.getNumeroCanales());
        List<Long> fusionados = fusion.fusionar(porCanal, VENTANA_FUSION_S, fs, votos);
        ResultadoDeteccion resultado = new ResultadoDeteccion(fusionados, fs,
                detector.getNombre() + " + fusión", -1);
        publicar(registro, resultado);
        return resultado;
    }

    /** Detecta solo en un canal. */
    public ResultadoDeteccion analizarCanal(RegistroECG registro, int canal) {
        double fs = registro.getFrecuenciaMuestreo();
        ResultadoDeteccion r = new ResultadoDeteccion(
                detector.detectar(registro.getCanal(canal).getMuestras(), fs), fs, detector.getNombre(), canal);
        ultimosPorCanal = List.of(r);
        publicar(registro, r);
        return r;
    }

    private void publicar(RegistroECG registro, ResultadoDeteccion r) {
        List<AnotacionECG> anotaciones = new ArrayList<>();
        for (long i : r.getIndicesDetectados()) {
            anotaciones.add(new AnotacionECG(i, "QRS", "Detección: " + r.getNombreAlgoritmo(),
                    AnotacionECG.FUENTE_ALGORITMO));
        }
        registro.reemplazarAnotaciones(AnotacionECG.FUENTE_ALGORITMO, anotaciones);
    }
}
