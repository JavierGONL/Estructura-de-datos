package ecg.rendimiento;

/** Tiempo transcurrido de pared (incluye esperas y bloqueos; no equivale a tiempo de CPU). */
public class MedidorTiempo {

    private long inicio;
    private long duracion;

    public void iniciar() { inicio = System.nanoTime(); }

    public void finalizar() { duracion = System.nanoTime() - inicio; }

    /** Duración en nanosegundos. */
    public long getDuracion() { return duracion; }
}
