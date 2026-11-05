package ecg.visualizacion;

import ecg.modelo.ConfiguracionVista;
import ecg.modelo.IntervaloTemporal;

/** Selección de un segmento mediante dos posiciones temporales. */
public class GestorIntervalo {

    private final ConfiguracionVista c;
    private double inicio = Double.NaN, fin = Double.NaN;

    public GestorIntervalo(ConfiguracionVista c) { this.c = c; }

    public void establecerInicio(double tiempo) { inicio = tiempo; actualizar(); }

    public void establecerFin(double tiempo) { fin = tiempo; actualizar(); }

    public IntervaloTemporal getIntervaloSeleccionado() { return c.getIntervaloSeleccionado(); }

    public void limpiarSeleccion() { inicio = fin = Double.NaN; c.setIntervaloSeleccionado(null); }

    private void actualizar() {
        c.setIntervaloSeleccionado(Double.isNaN(inicio) || Double.isNaN(fin) ? null : new IntervaloTemporal(inicio, fin));
    }
}
