package ecg.lectura;

import ecg.modelo.MetadatosCanal;

import java.util.List;

/**
 * Formato 212: dos muestras de 12 bits (complemento a dos) empaquetadas en tres bytes.
 * Las muestras se numeran siguiendo el orden intercalado (frame 0 señal 0, frame 0 señal 1, ...).
 */
public class DecodificadorFormato212 extends DecodificadorBase {

    @Override
    protected long bytesNecesarios(long frames, int n) {
        long total = frames * n;
        return (total / 2) * 3 + ((total % 2) == 1 ? 2 : 0);
    }

    @Override
    protected long framesEnBytes(long bytes, int n) {
        long total = (bytes / 3) * 2 + ((bytes % 3) >= 2 ? 1 : 0);
        return total / n;
    }

    @Override
    protected double[][] decodificarBytes(byte[] d, int inicio, List<MetadatosCanal> canales, int frames) {
        int n = canales.size();
        double[][] r = new double[n][frames];
        long k = 0;
        for (int t = 0; t < frames; t++) {
            for (int c = 0; c < n; c++, k++) {
                int b = inicio + (int) (k / 2) * 3;
                int v;
                if ((k & 1) == 0) v = (d[b] & 0xFF) | ((d[b + 1] & 0x0F) << 8);
                else              v = (d[b + 2] & 0xFF) | ((d[b + 1] & 0xF0) << 4);
                if ((v & 0x800) != 0) v -= 4096;
                r[c][t] = v;
            }
        }
        return r;
    }
}
