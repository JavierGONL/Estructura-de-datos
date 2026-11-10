package ecg.modelo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Registro completo: cabecera, canales y anotaciones. El resto de la aplicación solo usa esta clase. */
public class RegistroECG {

    private final String nombreRegistro;
    private final CabeceraECG cabecera;
    private final List<CanalECG> canales;
    private final List<AnotacionECG> anotaciones = new ArrayList<>();
    private final List<String> advertencias = new ArrayList<>();
    private Path origen;

    public RegistroECG(String nombreRegistro, CabeceraECG cabecera, List<CanalECG> canales) {
        this.nombreRegistro = nombreRegistro;
        this.cabecera = cabecera;
        this.canales = new ArrayList<>(canales);
    }

    public String getNombreRegistro() { return nombreRegistro; }
    public CabeceraECG getCabecera() { return cabecera; }
    public int getNumeroCanales() { return canales.size(); }
    public CanalECG getCanal(int indice) { return canales.get(indice); }
    public List<CanalECG> getCanales() { return Collections.unmodifiableList(canales); }
    public Path getOrigen() { return origen; }
    public void setOrigen(Path origen) { this.origen = origen; }

    public double getFrecuenciaMuestreo() { return cabecera.getFrecuenciaMuestreo(); }

    public long getNumeroMuestras() { return canales.isEmpty() ? 0 : canales.get(0).getNumeroMuestras(); }

    public double getDuracionSegundos() {
        double fs = getFrecuenciaMuestreo();
        return fs > 0 ? getNumeroMuestras() / fs : 0;
    }

    // ---- anotaciones ----

    public synchronized List<AnotacionECG> getAnotaciones() { return new ArrayList<>(anotaciones); }

    public synchronized List<AnotacionECG> getAnotaciones(String fuente) {
        return anotaciones.stream().filter(a -> a.getFuente().equals(fuente)).collect(Collectors.toList());
    }

    public synchronized void agregarAnotaciones(List<AnotacionECG> nuevas) {
        anotaciones.addAll(nuevas);
        anotaciones.sort((a, b) -> Long.compare(a.getIndiceMuestra(), b.getIndiceMuestra()));
    }

    /** Sustituye todas las anotaciones de una fuente (p. ej. al repetir la detección). */
    public synchronized void reemplazarAnotaciones(String fuente, List<AnotacionECG> nuevas) {
        anotaciones.removeIf(a -> a.getFuente().equals(fuente));
        agregarAnotaciones(nuevas);
    }

    public List<String> getAdvertencias() { return Collections.unmodifiableList(advertencias); }
    public void agregarAdvertencias(List<String> a) { advertencias.addAll(a); }
}
