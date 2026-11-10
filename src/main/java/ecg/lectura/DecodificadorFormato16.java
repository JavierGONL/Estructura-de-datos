package ecg.lectura;

import ecg.modelo.MetadatosCanal;

import java.util.List;

/** Formato 16: enteros de 16 bits con signo, little-endian, señales intercaladas. */
public class DecodificadorFormato16 extends DecodificadorBase {

    @Override protected long bytesNecesarios(long frames, int n) { return frames * n * 2L; }
    @Override protected long framesEnBytes(long bytes, int n) { return bytes / (2L * n); }

    @Override
    protected double[][] decodificarBytes(byte[] d, int inicio, List<MetadatosCanal> canales, int frames) {
        int n = canales.size();
        double[][] r = new double[n][frames];
        int p = inicio;
        for (int t = 0; t < frames; t++) {
            for (int c = 0; c < n; c++) {
                r[c][t] = (short) ((d[p] & 0xFF) | (d[p + 1] << 8));
                p += 2;
            }
        }
        return r;
    }
}
