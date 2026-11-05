package ecg.lectura;

import ecg.modelo.CabeceraECG;
import ecg.modelo.CanalECG;
import ecg.modelo.MetadatosCanal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Coordina la lectura binaria: agrupa señales por archivo, decodifica y convierte a unidades físicas. */
public class LectorSenalWFDB {

    private final List<String> advertencias = new ArrayList<>();

    public static boolean formatoSoportado(int formato) {
        return formato == 8 || formato == 16 || formato == 212 || formato == 24 || formato == 32;
    }

    public static DecodificadorWFDB obtenerDecodificador(int formato) throws LecturaECGException {
        switch (formato) {
            case 8:   return new DecodificadorFormato8();
            case 16:  return new DecodificadorFormato16();
            case 212: return new DecodificadorFormato212();
            case 24:  return new DecodificadorFormatoEntero(3);
            case 32:  return new DecodificadorFormatoEntero(4);
            default:  throw new LecturaECGException("Formato WFDB " + formato + " no soportado");
        }
    }

    /** Índices de señal agrupados por archivo, respetando el orden de la cabecera. */
    Map<String, List<Integer>> agruparPorArchivo(List<MetadatosCanal> metas) {
        Map<String, List<Integer>> grupos = new LinkedHashMap<>();
        for (int i = 0; i < metas.size(); i++) {
            grupos.computeIfAbsent(metas.get(i).getArchivoDatos(), k -> new ArrayList<>()).add(i);
        }
        return grupos;
    }

    public List<CanalECG> leerSenales(Path directorio, CabeceraECG cabecera) throws IOException {
        advertencias.clear();
        List<MetadatosCanal> metas = cabecera.getMetadatosCanales();
        int n = metas.size();
        double[][] digital = new double[n][];

        Map<String, List<Integer>> grupos = agruparPorArchivo(metas);
        long frames = cabecera.getNumeroMuestras();
        if (frames <= 0) frames = deducirFrames(directorio, metas, grupos);

        for (Map.Entry<String, List<Integer>> g : grupos.entrySet()) {
            List<MetadatosCanal> sub = new ArrayList<>();
            g.getValue().forEach(k -> sub.add(metas.get(k)));
            DecodificadorWFDB dec = obtenerDecodificador(sub.get(0).getFormatoWFDB());
            double[][] dato = dec.decodificar(directorio.resolve(g.getKey()), sub, frames);
            for (int j = 0; j < g.getValue().size(); j++) digital[g.getValue().get(j)] = dato[j];
        }

        List<CanalECG> canales = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            MetadatosCanal m = metas.get(i);
            comprobarChecksum(i, m, digital[i]);
            String nombre = m.getDescripcion().isBlank() ? "Canal " + (i + 1) : m.getDescripcion();
            canales.add(new CanalECG(nombre, ConversorUnidades.convertir(digital[i], m), m,
                    ConversorUnidades.unidadesResultado(m)));
            digital[i] = null;   // libera la copia digital cuanto antes
        }
        return canales;
    }

    private long deducirFrames(Path dir, List<MetadatosCanal> metas, Map<String, List<Integer>> grupos)
            throws IOException {
        long min = Long.MAX_VALUE;
        for (Map.Entry<String, List<Integer>> g : grupos.entrySet()) {
            List<MetadatosCanal> sub = new ArrayList<>();
            g.getValue().forEach(k -> sub.add(metas.get(k)));
            min = Math.min(min, obtenerDecodificador(sub.get(0).getFormatoWFDB())
                    .contarFrames(dir.resolve(g.getKey()), sub));
        }
        return min;
    }

    /** El checksum de la cabecera es la suma de las muestras módulo 2^16: detecta errores de decodificación. */
    private void comprobarChecksum(int indice, MetadatosCanal m, double[] digital) {
        if (!m.isChecksumDeclarado()) return;
        int suma = 0;
        for (double v : digital) suma += (int) v;
        if (((suma - m.getChecksum()) & 0xFFFF) != 0) {
            advertencias.add("Señal " + indice + " (" + m.getDescripcion()
                    + "): el checksum no coincide con el de la cabecera");
        }
    }

    public List<String> getAdvertencias() { return advertencias; }
}
