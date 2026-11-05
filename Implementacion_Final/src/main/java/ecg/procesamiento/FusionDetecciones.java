package ecg.procesamiento;

import ecg.modelo.ResultadoDeteccion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Combina las detecciones de varios canales en una única lista de latidos mediante consenso. */
public class FusionDetecciones {

    private static final class Candidato {
        final long indice; final int canal;
        Candidato(long i, int c) { indice = i; canal = c; }
    }

    /**
     * @param ventanaSegundos separación máxima entre candidatos consecutivos de un mismo latido
     * @param votosMinimos    canales distintos que deben coincidir para aceptar el latido
     * @return índice mediano de cada grupo aceptado
     */
    public List<Long> fusionar(List<ResultadoDeteccion> porCanal, double ventanaSegundos, double fs,
                               int votosMinimos) {
        List<Candidato> todos = new ArrayList<>();
        for (ResultadoDeteccion r : porCanal) {
            for (long i : r.getIndicesDetectados()) todos.add(new Candidato(i, r.getCanalUtilizado()));
        }
        todos.sort(Comparator.comparingLong(c -> c.indice));

        long ventana = Math.round(ventanaSegundos * fs);
        List<Long> fusion = new ArrayList<>();
        List<Candidato> grupo = new ArrayList<>();
        for (Candidato c : todos) {
            if (!grupo.isEmpty() && c.indice - grupo.get(grupo.size() - 1).indice > ventana) {
                cerrar(grupo, votosMinimos, fusion);
                grupo.clear();
            }
            grupo.add(c);
        }
        cerrar(grupo, votosMinimos, fusion);
        return fusion;
    }

    /** Mayoría simple de los canales disponibles. */
    public static int votosPorMayoria(int numeroCanales) { return Math.max(1, (numeroCanales + 1) / 2); }

    private void cerrar(List<Candidato> grupo, int votosMinimos, List<Long> salida) {
        if (grupo.isEmpty()) return;
        Set<Integer> canales = new HashSet<>();
        for (Candidato c : grupo) canales.add(c.canal);
        if (canales.size() >= votosMinimos) salida.add(grupo.get(grupo.size() / 2).indice);
    }
}
