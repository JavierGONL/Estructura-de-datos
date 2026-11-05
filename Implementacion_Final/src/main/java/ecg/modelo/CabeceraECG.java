package ecg.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Información general del archivo .hea. */
public class CabeceraECG {

    private String nombreRegistro;
    private int numeroCanales;
    private double frecuenciaMuestreo;
    private long numeroMuestras;          // 0 = no declarado en la cabecera
    private String fecha = "";
    private String hora = "";
    private List<MetadatosCanal> canales = new ArrayList<>();
    private final List<String> comentarios = new ArrayList<>();

    public String getNombreRegistro() { return nombreRegistro; }
    public void setNombreRegistro(String v) { this.nombreRegistro = v; }
    public int getNumeroCanales() { return numeroCanales; }
    public void setNumeroCanales(int v) { this.numeroCanales = v; }
    public double getFrecuenciaMuestreo() { return frecuenciaMuestreo; }
    public void setFrecuenciaMuestreo(double v) { this.frecuenciaMuestreo = v; }
    public long getNumeroMuestras() { return numeroMuestras; }
    public void setNumeroMuestras(long v) { this.numeroMuestras = v; }
    public String getFecha() { return fecha; }
    public void setFecha(String v) { this.fecha = v == null ? "" : v; }
    public String getHora() { return hora; }
    public void setHora(String v) { this.hora = v == null ? "" : v; }
    public List<MetadatosCanal> getMetadatosCanales() { return canales; }
    public void setMetadatosCanales(List<MetadatosCanal> v) { this.canales = v; }
    public List<String> getComentarios() { return Collections.unmodifiableList(comentarios); }
    public void agregarComentario(String c) { comentarios.add(c); }
}
