package ecg.visualizacion;

import ecg.modelo.ConfiguracionVista;
import ecg.modelo.RegistroECG;

import java.util.ArrayList;
import java.util.List;

/** Controla qué derivaciones están visibles. */
public class GestorSeleccionCanales {

    private final RegistroECG registro;
    private final ConfiguracionVista c;

    public GestorSeleccionCanales(RegistroECG registro, ConfiguracionVista c) { this.registro = registro; this.c = c; }

    public void seleccionarCanal(int indice) {
        if (indice < 0 || indice >= registro.getNumeroCanales()) return;
        if (!c.getCanalesVisibles().contains(indice)) {
            List<Integer> l = new ArrayList<>(c.getCanalesVisibles());
            l.add(indice);
            l.sort(Integer::compare);
            c.setCanalesVisibles(l);
        }
    }

    public void deseleccionarCanal(int indice) { c.getCanalesVisibles().remove(Integer.valueOf(indice)); }

    public void seleccionarTodos() {
        List<Integer> l = new ArrayList<>();
        for (int i = 0; i < registro.getNumeroCanales(); i++) l.add(i);
        c.setCanalesVisibles(l);
    }

    public void limpiarSeleccion() { c.setCanalesVisibles(new ArrayList<>()); }
}
