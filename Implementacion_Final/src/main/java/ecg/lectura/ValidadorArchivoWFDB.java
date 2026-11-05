package ecg.lectura;

import ecg.modelo.CabeceraECG;
import ecg.modelo.MetadatosCanal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Comprueba que una cabecera ya interpretada describe un registro que esta aplicación sabe leer. */
public class ValidadorArchivoWFDB {

    /** Resultado de la validación: errores impiden la carga, advertencias no. */
    public static class Informe {
        private final List<String> errores = new ArrayList<>();
        private final List<String> advertencias = new ArrayList<>();
        public boolean esValido() { return errores.isEmpty(); }
        public List<String> getErrores() { return errores; }
        public List<String> getAdvertencias() { return advertencias; }
        public String resumen() {
            StringBuilder sb = new StringBuilder();
            errores.forEach(e -> sb.append("ERROR: ").append(e).append('\n'));
            advertencias.forEach(a -> sb.append("Aviso: ").append(a).append('\n'));
            return sb.length() == 0 ? "Correcto" : sb.toString().trim();
        }
    }

    public Informe validar(CabeceraECG cab, Path directorio) {
        Informe inf = new Informe();
        List<MetadatosCanal> metas = cab.getMetadatosCanales();

        if (metas.size() != cab.getNumeroCanales()) {
            inf.errores.add("El número de señales declarado (" + cab.getNumeroCanales()
                    + ") no coincide con las líneas de señal (" + metas.size() + ")");
        }
        if (cab.getFrecuenciaMuestreo() <= 0) inf.errores.add("Frecuencia de muestreo inválida");

        for (int i = 0; i < metas.size(); i++) {
            MetadatosCanal m = metas.get(i);
            String id = "Señal " + i + " (" + m.getDescripcion() + "): ";
            if (!LectorSenalWFDB.formatoSoportado(m.getFormatoWFDB())) {
                inf.errores.add(id + "formato WFDB " + m.getFormatoWFDB() + " no soportado");
            }
            if (m.getMuestrasPorFrame() != 1) {
                inf.errores.add(id + "más de una muestra por frame (x" + m.getMuestrasPorFrame() + ") no soportado");
            }
            if (m.getSkew() != 0) inf.errores.add(id + "skew distinto de cero no soportado");
            if (!m.isGananciaDisponible()) {
                inf.errores.add(id + "no declara ganancia; no se aplican valores supuestos");
            }
            if (Double.isNaN(ConversorUnidades.factorAMilivoltios(m.getUnidades()))) {
                inf.advertencias.add(id + "unidades '" + m.getUnidades()
                        + "' no son de tensión; se conservan sin convertir a mV");
            }
            Path f = directorio.resolve(m.getArchivoDatos());
            if (!Files.isRegularFile(f)) inf.errores.add(id + "no existe el archivo " + m.getArchivoDatos());
        }

        // Un mismo archivo debe usar un solo formato y desplazamiento.
        for (int i = 0; i < metas.size(); i++) {
            for (int j = i + 1; j < metas.size(); j++) {
                MetadatosCanal a = metas.get(i), b = metas.get(j);
                if (a.getArchivoDatos().equals(b.getArchivoDatos())
                        && (a.getFormatoWFDB() != b.getFormatoWFDB() || a.getDesplazamiento() != b.getDesplazamiento())) {
                    inf.errores.add("Las señales " + i + " y " + j + " comparten archivo con formatos distintos");
                }
            }
        }

        // Longitud de los datos frente a lo declarado.
        if (inf.esValido()) {
            if (cab.getNumeroMuestras() <= 0) {
                inf.advertencias.add("La cabecera no declara el número de muestras; se deducirá del tamaño del archivo");
            } else {
                try {
                    LectorSenalWFDB lector = new LectorSenalWFDB();
                    for (var grupo : lector.agruparPorArchivo(metas).entrySet()) {
                        List<MetadatosCanal> sub = new ArrayList<>();
                        grupo.getValue().forEach(k -> sub.add(metas.get(k)));
                        long disponibles = LectorSenalWFDB.obtenerDecodificador(sub.get(0).getFormatoWFDB())
                                .contarFrames(directorio.resolve(grupo.getKey()), sub);
                        if (disponibles < cab.getNumeroMuestras()) {
                            inf.errores.add("El archivo " + grupo.getKey() + " contiene " + disponibles
                                    + " frames y la cabecera declara " + cab.getNumeroMuestras());
                        }
                    }
                } catch (IOException e) {
                    inf.errores.add("No se pudo comprobar el tamaño de los datos: " + e.getMessage());
                }
            }
        }
        return inf;
    }
}
