package ecg.lectura;

import ecg.modelo.MetadatosCanal;

import java.util.List;

/**
 * Formato 8: diferencias sucesivas de 8 bits con signo. Cada señal acumula a partir de su
 * valor inicial declarado en la cabecera.
 */
public class DecodificadorFormato8 extends DecodificadorBase {

    @Override protected long bytesNecesarios(long frames, int n) { return frames * n; }
    @Override protected long framesEnBytes(long bytes, int n) { return bytes / n; }

    @Override
    protected double[][] decodificarBytes(byte[] d, int inicio, List<MetadatosCanal> canales, int frames) {
        int n = canales.size();
        double[][] r = new double[n][frames];
        int[] acumulado = new int[n];
        for (int c = 0; c < n; c++) acumulado[c] = canales.get(c).getValorInicial();
        int p = inicio;
        for (int t = 0; t < frames; t++) {
            for (int c = 0; c < n; c++) {
                acumulado[c] += d[p++];          // byte con signo
                r[c][t] = acumulado[c];
            }
        }
        return r;
    }
}
