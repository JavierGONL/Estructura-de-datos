package ecg.visualizacion;

import ecg.modelo.ConfiguracionVista;
import ecg.modelo.RegistroECG;

/** Agrupa los gestores que operan sobre un mismo registro y una misma configuración. */
public class GestoresVista {

    public final GestorZoom zoom;
    public final GestorDesplazamiento desplazamiento;
    public final GestorSeleccionCanales canales;
    public final GestorIntervalo intervalo;
    public final GestorCursor cursor;
    public final GestorAnotaciones anotaciones;

    public GestoresVista(RegistroECG registro, ConfiguracionVista config, RenderizadorECG renderizador) {
        zoom = new GestorZoom(registro, config);
        desplazamiento = new GestorDesplazamiento(registro, config);
        canales = new GestorSeleccionCanales(registro, config);
        intervalo = new GestorIntervalo(config);
        cursor = new GestorCursor(registro, config, renderizador);
        anotaciones = new GestorAnotaciones(registro, config);
    }
}
