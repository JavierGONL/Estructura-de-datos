package ecg.modelo;

import java.time.LocalDateTime;

/**
 * Mediciones de una operación. Memoria en bytes y tiempos en nanosegundos;
 * el valor {@link #NO_DISPONIBLE} indica que la plataforma no ofrece esa medida.
 */
public class MetricasEjecucion {

    public static final long NO_DISPONIBLE = -1;

    private String nombreOperacion;
    private LocalDateTime fechaEjecucion = LocalDateTime.now();
    private int profundidad;                       // 0 = operación de primer nivel (no anidada)

    private long memoriaInicial;
    private long memoriaFinal;
    private long memoriaMaxima;                    // heap usado máximo observado
    private long memoriaReservadaJVM = NO_DISPONIBLE;
    private long memoriaRSS = NO_DISPONIBLE;

    private long tiempoCPUUsuario = NO_DISPONIBLE; // proceso completo (según SO)
    private long tiempoCPUSistema = NO_DISPONIBLE;
    private long tiempoCPUTotal = NO_DISPONIBLE;   // proceso completo (portable)
    private long tiempoCPUHilo = NO_DISPONIBLE;    // hilo que realizó la medición

    private long tiempoPared;

    public String getNombreOperacion() { return nombreOperacion; }
    public void setNombreOperacion(String v) { this.nombreOperacion = v; }
    public LocalDateTime getFechaEjecucion() { return fechaEjecucion; }
    public void setFechaEjecucion(LocalDateTime v) { this.fechaEjecucion = v; }
    public int getProfundidad() { return profundidad; }
    public void setProfundidad(int v) { this.profundidad = v; }
    public long getMemoriaInicial() { return memoriaInicial; }
    public void setMemoriaInicial(long v) { this.memoriaInicial = v; }
    public long getMemoriaFinal() { return memoriaFinal; }
    public void setMemoriaFinal(long v) { this.memoriaFinal = v; }
    public long getMemoriaMaxima() { return memoriaMaxima; }
    public void setMemoriaMaxima(long v) { this.memoriaMaxima = v; }
    public long getMemoriaReservadaJVM() { return memoriaReservadaJVM; }
    public void setMemoriaReservadaJVM(long v) { this.memoriaReservadaJVM = v; }
    public long getMemoriaRSS() { return memoriaRSS; }
    public void setMemoriaRSS(long v) { this.memoriaRSS = v; }
    public long getTiempoCPUUsuario() { return tiempoCPUUsuario; }
    public void setTiempoCPUUsuario(long v) { this.tiempoCPUUsuario = v; }
    public long getTiempoCPUSistema() { return tiempoCPUSistema; }
    public void setTiempoCPUSistema(long v) { this.tiempoCPUSistema = v; }
    public long getTiempoCPUTotal() { return tiempoCPUTotal; }
    public void setTiempoCPUTotal(long v) { this.tiempoCPUTotal = v; }
    public long getTiempoCPUHilo() { return tiempoCPUHilo; }
    public void setTiempoCPUHilo(long v) { this.tiempoCPUHilo = v; }
    public long getTiempoPared() { return tiempoPared; }
    public void setTiempoPared(long v) { this.tiempoPared = v; }
}
