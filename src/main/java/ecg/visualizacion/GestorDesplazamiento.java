package ecg.visualizacion;

import ecg.modelo.ConfiguracionVista;
import ecg.modelo.RegistroECG;

/** Navegación temporal por el registro. */
public class GestorDesplazamiento {

    private final RegistroECG registro;
    private final ConfiguracionVista c;

    public GestorDesplazamiento(RegistroECG registro, ConfiguracionVista c) { this.registro = registro; this.c = c; }

    /** Avanza media ventana. */
    public void desplazarAdelante() { desplazarPorSegundos(c.getDuracionVentana() / 2); }

    public void desplazarAtras() { desplazarPorSegundos(-c.getDuracionVentana() / 2); }

    public void desplazarPorSegundos(double dt) { desplazarA(c.getTiempoInicio() + dt); }

    /** Coloca el inicio de la ventana en {@code tiempo}, conservando su duración. */
    public void desplazarA(double tiempo) {
        double ventana = c.getDuracionVentana();
        double inicio = Math.max(0, Math.min(tiempo, registro.getDuracionSegundos() - ventana));
        c.setTiempoInicio(inicio);
        c.setTiempoFin(inicio + ventana);
    }

    public void restablecerPosicion() { desplazarA(0); }
}
