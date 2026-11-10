package ecg.modelo;

/** Segmento temporal [inicio, fin] en segundos. */
public class IntervaloTemporal {

    private final double tiempoInicio;
    private final double tiempoFin;

    public IntervaloTemporal(double a, double b) {
        this.tiempoInicio = Math.min(a, b);
        this.tiempoFin = Math.max(a, b);
    }

    public double getTiempoInicio() { return tiempoInicio; }
    public double getTiempoFin() { return tiempoFin; }
    public double getDuracion() { return tiempoFin - tiempoInicio; }
    public long getMuestraInicial(double frecuencia) { return (long) Math.floor(tiempoInicio * frecuencia); }
    public long getMuestraFinal(double frecuencia) { return (long) Math.ceil(tiempoFin * frecuencia); }

    @Override
    public String toString() { return String.format("[%.3f s, %.3f s]", tiempoInicio, tiempoFin); }
}
