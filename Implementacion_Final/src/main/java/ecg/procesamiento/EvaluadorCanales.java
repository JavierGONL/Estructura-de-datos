package ecg.procesamiento;

import ecg.modelo.CanalECG;
import ecg.modelo.RegistroECG;
import ecg.modelo.ResultadoDeteccion;

import java.util.ArrayList;
import java.util.List;

/** Ejecuta el detector en cada canal por separado y, si hay referencia, compara su desempeño. */
public class EvaluadorCanales {

    public static class ResultadoCanal {
        public final int indice;
        public final String nombre;
        public final ResultadoDeteccion deteccion;
        /** null si el registro no tiene anotaciones de referencia */
        public final ResultadoValidacion validacion;
        ResultadoCanal(int i, String n, ResultadoDeteccion d, ResultadoValidacion v) {
            indice = i; nombre = n; deteccion = d; validacion = v;
        }
    }

    public List<ResultadoCanal> evaluar(RegistroECG registro, DetectorQRS detector, double toleranciaSegundos) {
        List<Long> referencia = ValidadorDeteccion.indicesReferencia(registro);
        ValidadorDeteccion validador = new ValidadorDeteccion();
        double fs = registro.getFrecuenciaMuestreo();

        List<ResultadoCanal> r = new ArrayList<>();
        for (int i = 0; i < registro.getNumeroCanales(); i++) {
            CanalECG c = registro.getCanal(i);
            List<Long> det = detector.detectar(c.getMuestras(), fs);
            ResultadoDeteccion rd = new ResultadoDeteccion(det, fs, detector.getNombre(), i);
            ResultadoValidacion rv = referencia.isEmpty() ? null
                    : validador.validar(det, referencia, fs, toleranciaSegundos);
            r.add(new ResultadoCanal(i, c.getNombre(), rd, rv));
        }
        return r;
    }
}
