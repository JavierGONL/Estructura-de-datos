package ecg.lectura;

import ecg.modelo.AnotacionECG;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee archivos de anotaciones en formato MIT (.atr, .qrs, ...): palabras de 16 bits little-endian con
 * el tipo en los 6 bits altos y el intervalo desde la anotación anterior en los 10 bajos.
 */
public class LectorAnotacionesWFDB {

    private static final int SKIP = 59, NUM = 60, SUB = 61, CHN = 62, AUX = 63;

    /** Símbolos de los códigos 1..41 (ecgcodes.h). */
    private static final String[] SIMBOLO_POR_CODIGO = new String[42];
    static {
        String tabla = "NLRaVFJASEj/Q~"; // códigos 1..14
        for (int i = 0; i < tabla.length(); i++) SIMBOLO_POR_CODIGO[i + 1] = String.valueOf(tabla.charAt(i));
        SIMBOLO_POR_CODIGO[16] = "|";  SIMBOLO_POR_CODIGO[18] = "s"; SIMBOLO_POR_CODIGO[19] = "T";
        SIMBOLO_POR_CODIGO[20] = "*";  SIMBOLO_POR_CODIGO[21] = "D"; SIMBOLO_POR_CODIGO[22] = "\"";
        SIMBOLO_POR_CODIGO[23] = "=";  SIMBOLO_POR_CODIGO[24] = "p"; SIMBOLO_POR_CODIGO[25] = "B";
        SIMBOLO_POR_CODIGO[26] = "^";  SIMBOLO_POR_CODIGO[27] = "t"; SIMBOLO_POR_CODIGO[28] = "+";
        SIMBOLO_POR_CODIGO[29] = "u";  SIMBOLO_POR_CODIGO[30] = "?"; SIMBOLO_POR_CODIGO[31] = "!";
        SIMBOLO_POR_CODIGO[32] = "[";  SIMBOLO_POR_CODIGO[33] = "]"; SIMBOLO_POR_CODIGO[34] = "e";
        SIMBOLO_POR_CODIGO[35] = "n";  SIMBOLO_POR_CODIGO[36] = "@"; SIMBOLO_POR_CODIGO[37] = "x";
        SIMBOLO_POR_CODIGO[38] = "f";  SIMBOLO_POR_CODIGO[39] = "("; SIMBOLO_POR_CODIGO[40] = ")";
        SIMBOLO_POR_CODIGO[41] = "r";
    }

    public List<AnotacionECG> leer(Path archivo) throws IOException {
        byte[] d = Files.readAllBytes(archivo);
        List<AnotacionECG> r = new ArrayList<>();
        long t = 0;
        int i = 0;
        while (i + 1 < d.length) {
            int w = palabra(d, i);
            i += 2;
            int tipo = w >>> 10, intervalo = w & 0x3FF;
            if (tipo == 0 && intervalo == 0) break;                  // fin de archivo
            if (tipo == SKIP) {
                if (i + 3 >= d.length) break;
                long alta = palabra(d, i), baja = palabra(d, i + 2);
                i += 4;
                t += (int) ((alta << 16) | baja);
                continue;
            }
            if (tipo == NUM || tipo == SUB || tipo == CHN) continue;  // modifican la anotación previa
            if (tipo == AUX) { i += intervalo + (intervalo & 1); continue; }
            t += intervalo;
            String simbolo = tipo < SIMBOLO_POR_CODIGO.length ? SIMBOLO_POR_CODIGO[tipo] : null;
            if (simbolo == null) simbolo = "?";
            r.add(new AnotacionECG(t, simbolo, "Anotación de referencia (" + simbolo + ")",
                    AnotacionECG.FUENTE_REFERENCIA));
        }
        return r;
    }

    private static int palabra(byte[] d, int i) { return (d[i] & 0xFF) | ((d[i + 1] & 0xFF) << 8); }

    /** Código numérico WFDB de un símbolo (para escribir archivos de prueba); 0 si no existe. */
    public static int codigoDeSimbolo(String simbolo) {
        for (int i = 1; i < SIMBOLO_POR_CODIGO.length; i++) {
            if (simbolo.equals(SIMBOLO_POR_CODIGO[i])) return i;
        }
        return 0;
    }
}
