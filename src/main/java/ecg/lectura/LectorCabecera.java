package ecg.lectura;

import ecg.modelo.CabeceraECG;
import ecg.modelo.MetadatosCanal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lee y analiza un archivo .hea de WFDB.
 *
 * <pre>
 * Línea de registro: nombre nsig [fs[/contador[(base)]]] [nmuestras] [hora] [fecha]
 * Línea de señal:    archivo formato[xN][:skew][+offset] ganancia[(baseline)][/unidades]
 *                    [resolución [adcZero [valorInicial [checksum [bloque [descripción]]]]]]
 * </pre>
 */
public class LectorCabecera {

    private static final double FRECUENCIA_PREDETERMINADA = 250.0;
    private static final String NUM = "[-+]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?";
    private static final Pattern FORMATO =
            Pattern.compile("^(\\d+)(?:x(\\d+))?(?::(\\d+))?(?:\\+(\\d+))?$");
    private static final Pattern GANANCIA =
            Pattern.compile("^(" + NUM + ")?(?:\\(([-+]?\\d+)\\))?(?:/(\\S+))?$");

    public CabeceraECG leer(Path ruta) throws IOException {
        List<String> lineas = Files.readAllLines(ruta, StandardCharsets.ISO_8859_1);
        CabeceraECG cab = new CabeceraECG();
        boolean generalLeida = false;
        List<MetadatosCanal> canales = new ArrayList<>();

        for (String bruta : lineas) {
            String linea = bruta.trim();
            if (linea.isEmpty()) continue;
            if (linea.startsWith("#")) { cab.agregarComentario(linea.substring(1).trim()); continue; }

            if (!generalLeida) {
                leerDatosGenerales(linea, cab);
                generalLeida = true;
            } else if (canales.size() < cab.getNumeroCanales()) {
                MetadatosCanal m = leerMetadatosCanal(linea);
                m.setFrecuenciaMuestreo(cab.getFrecuenciaMuestreo());
                canales.add(m);
            }
        }
        if (!generalLeida) throw new LecturaECGException("Cabecera vacía: " + ruta);
        if (canales.size() != cab.getNumeroCanales()) {
            throw new LecturaECGException("La cabecera declara " + cab.getNumeroCanales()
                    + " señales pero contiene " + canales.size() + " líneas de señal");
        }
        cab.setMetadatosCanales(canales);
        return cab;
    }

    private void leerDatosGenerales(String linea, CabeceraECG cab) throws LecturaECGException {
        String[] t = linea.split("\\s+");
        if (t.length < 2) throw new LecturaECGException("Línea de registro inválida: " + linea);
        if (t[0].contains("/")) {
            throw new LecturaECGException("Registro multisegmento ('" + t[0]
                    + "'): no soportado. Indique uno de los segmentos.");
        }
        cab.setNombreRegistro(t[0]);
        try {
            cab.setNumeroCanales(Integer.parseInt(t[1]));
            double fs = FRECUENCIA_PREDETERMINADA;
            if (t.length > 2) {
                String f = t[2].split("[/(]")[0];
                fs = Double.parseDouble(f);
                if (fs <= 0) fs = FRECUENCIA_PREDETERMINADA;
            }
            cab.setFrecuenciaMuestreo(fs);
            if (t.length > 3) cab.setNumeroMuestras(Long.parseLong(t[3]));
            if (t.length > 4) cab.setHora(t[4]);
            if (t.length > 5) cab.setFecha(t[5]);
        } catch (NumberFormatException e) {
            throw new LecturaECGException("Línea de registro inválida: " + linea, e);
        }
        if (cab.getNumeroCanales() < 1) throw new LecturaECGException("El registro no declara señales");
    }

    private MetadatosCanal leerMetadatosCanal(String linea) throws LecturaECGException {
        String[] t = linea.split("\\s+");
        if (t.length < 2) throw new LecturaECGException("Línea de señal inválida: " + linea);
        MetadatosCanal m = new MetadatosCanal();
        m.setArchivoDatos(t[0]);

        Matcher mf = FORMATO.matcher(t[1]);
        if (!mf.matches()) throw new LecturaECGException("Formato de señal inválido: " + t[1]);
        m.setFormatoWFDB(Integer.parseInt(mf.group(1)));
        if (mf.group(2) != null) m.setMuestrasPorFrame(Integer.parseInt(mf.group(2)));
        if (mf.group(3) != null) m.setSkew(Integer.parseInt(mf.group(3)));
        if (mf.group(4) != null) m.setDesplazamiento(Integer.parseInt(mf.group(4)));

        boolean baselineExplicito = false;
        if (t.length > 2) {
            Matcher mg = GANANCIA.matcher(t[2]);
            if (!mg.matches()) throw new LecturaECGException("Ganancia inválida: " + t[2]);
            if (mg.group(1) != null) m.setGananciaADC(Double.parseDouble(mg.group(1)));
            baselineExplicito = mg.group(2) != null;
            if (baselineExplicito) m.setBaseline(Integer.parseInt(mg.group(2)));
            m.setUnidades(mg.group(3));
        }

        try {
            m.setResolucionADC(t.length > 3 ? Integer.parseInt(t[3]) : resolucionPorDefecto(m.getFormatoWFDB()));
            m.setAdcZero(t.length > 4 ? Integer.parseInt(t[4]) : 0);
            if (!baselineExplicito) m.setBaseline(m.getAdcZero());
            if (t.length > 5) m.setValorInicial(Integer.parseInt(t[5]));
            if (t.length > 6) m.setChecksum(Integer.parseInt(t[6]));
        } catch (NumberFormatException e) {
            throw new LecturaECGException("Línea de señal inválida: " + linea, e);
        }
        if (t.length > 8) {
            m.setDescripcion(String.join(" ", java.util.Arrays.copyOfRange(t, 8, t.length)));
        }
        return m;
    }

    private static int resolucionPorDefecto(int formato) {
        switch (formato) {
            case 8:  return 8;
            case 16: return 16;
            case 24: return 24;
            case 32: return 32;
            default: return 12;
        }
    }
}
