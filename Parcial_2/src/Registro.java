/**
 * clase registro
 * tiene 3 estados -> PENDIENTE, ATENDIDO y CANCELADO, segun su estado
 * estado solo se puede consultar con el metodo estadoActual() 
 * y para actualizarlo solamente es usar el metodo cambiar estado:
 *      -> "P" es pendiente
 *      -> "A" es atendido
 *      -> "C" es cancelado
 */
public class Registro {
    
    public  enum Estado { PENDIENTE, ATENDIDO, CANCELADO }

    public final int id;          // entero positivo y unico 
    public final String nombre;
    public final int prioridad;   // el 1 al 10 con 10 el mas urgente
    public final int padre;       // id del padre si 0 = raíz
    private Estado estado = Estado.PENDIENTE;

    public Registro(int id, String nombre, int prioridad, int padre) {
        this.id = id;
        this.nombre = nombre;
        this.prioridad = prioridad;
        this.padre = padre;
    }

    public Estado estadoActual() {
        return this.estado;
    }

    public void pendiente() {
        this.estado = Estado.PENDIENTE;
    }    
    public void cancelado() {
        this.estado = Estado.CANCELADO;
    }    
    public void atendido() {
        this.estado = Estado.ATENDIDO;
    }
}