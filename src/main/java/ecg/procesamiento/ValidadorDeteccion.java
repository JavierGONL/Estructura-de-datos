package ecg.procesamiento;

import ecg.modelo.AnotacionECG;
import ecg.modelo.RegistroECG;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Compara las detecciones del algoritmo con las anotaciones de referencia (emparejamiento uno a uno). */
public class ValidadorDeteccion {

    public static final double TOLERANCIA_PREDETERMINADA_S = 0.15;

    public ResultadoValidacion validar(List<Long> detecciones, List<Long> referencia, double fs,
                                       double toleranciaSegundos) {
        List<Long> d = new ArrayList<>(detecciones), r = new ArrayList<>(referencia);
        Collections.sort(d);
        Collections.sort(r);
        double tol = toleranciaSegundos * fs;
        int tp = 0, fp = 0, fn = 0, i = 0, j = 0;
        double sumaErr = 0, maxErr = 0;
        while (i < d.size() && j < r.size()) {
            long dif = d.get(i) - r.get(j);
            if (Math.abs(dif) <= tol) {
                tp++;
                double e = Math.abs(dif) / fs * 1000.0;
                sumaErr += e;
                maxErr = Math.max(maxErr, e);
                i++; j++;
            } else if (dif < 0) { fp++; i++; }
            else { fn++; j++; }
        }
        fp += d.size() - i;
        fn += r.size() - j;
        return new ResultadoValidacion(tp, fp, fn, tp == 0 ? Double.NaN : sumaErr / tp, maxErr);
    }

    /** Índices de los latidos de referencia (excluye anotaciones de ritmo, ruido, etc.). */
    public static List<Long> indicesReferencia(RegistroECG registro) {
        List<Long> r = new ArrayList<>();
        for (AnotacionECG a : registro.getAnotaciones(AnotacionECG.FUENTE_REFERENCIA)) {
            if (a.esLatido()) r.add(a.getIndiceMuestra());
        }
        return r;
    }
}
