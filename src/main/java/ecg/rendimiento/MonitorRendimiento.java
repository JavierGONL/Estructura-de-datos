package ecg.rendimiento;

import ecg.modelo.MetricasEjecucion;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Callable;

/**
 * Coordina las mediciones: se inicia antes de la operación y se cierra después. Admite operaciones
 * anidadas (cada una con su {@code profundidad}). Cada medición se guarda en el {@link RegistroMetricas}.
 * <p>
 * Inicio y fin de una misma medición deben ocurrir en el mismo hilo (por el CPU de hilo).
 */
public class MonitorRendimiento {

    private static final class Medicion {
        final String operacion;
        final int profundidad;
        final LocalDateTime fecha = LocalDateTime.now();
        final MedidorMemoria memoria = new MedidorMemoria();
        final MedidorCPU cpu = new MedidorCPU();
        final MedidorTiempo tiempo = new MedidorTiempo();
        Medicion(String op, int prof) { operacion = op; profundidad = prof; }
    }

    private final RegistroMetricas registro;
    private final Deque<Medicion> activas = new ArrayDeque<>();

    public MonitorRendimiento(RegistroMetricas registro) { this.registro = registro; }

    public RegistroMetricas getRegistro() { return registro; }

    public synchronized void iniciarMedicion(String operacion) {
        Medicion m = new Medicion(operacion, activas.size());
        activas.push(m);
        m.memoria.iniciar();
        m.cpu.iniciar();
        m.tiempo.iniciar();
    }

    public synchronized MetricasEjecucion finalizarMedicion() {
        if (activas.isEmpty()) throw new IllegalStateException("No hay ninguna medición en curso");
        Medicion m = activas.pop();
        m.tiempo.finalizar();
        m.cpu.finalizar();
        m.memoria.finalizar();

        MetricasEjecucion r = new MetricasEjecucion();
        r.setNombreOperacion(m.operacion);
        r.setFechaEjecucion(m.fecha);
        r.setProfundidad(m.profundidad);
        r.setTiempoPared(m.tiempo.getDuracion());
        r.setTiempoCPUHilo(m.cpu.getHilo());
        r.setTiempoCPUTotal(m.cpu.getProceso());
        r.setTiempoCPUUsuario(m.cpu.getUsuario());
        r.setTiempoCPUSistema(m.cpu.getSistema());
        r.setMemoriaInicial(m.memoria.getInicial());
        r.setMemoriaFinal(m.memoria.getFinal());
        r.setMemoriaMaxima(m.memoria.getMaximo());
        r.setMemoriaReservadaJVM(m.memoria.getReservada());
        r.setMemoriaRSS(m.memoria.getRSS());
        registro.agregar(r);
        return r;
    }

    /** Mide una operación que devuelve un valor; la medición se cierra también si la operación falla. */
    public <T> T medir(String operacion, Callable<T> accion) throws Exception {
        iniciarMedicion(operacion);
        try {
            return accion.call();
        } finally {
            finalizarMedicion();
        }
    }

    public void medir(String operacion, Runnable accion) {
        iniciarMedicion(operacion);
        try {
            accion.run();
        } finally {
            finalizarMedicion();
        }
    }
}
