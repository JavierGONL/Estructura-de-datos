package ecg.lectura;

import ecg.modelo.MetadatosCanal;

import java.util.List;

/** Formatos 24 y 32: enteros con signo little-endian de 3 o 4 bytes. */
public class DecodificadorFormatoEntero extends DecodificadorBase {

    private final int bytesPorMuestra;

    public DecodificadorFormatoEntero(int bytesPorMuestra) {
        if (bytesPorMuestra != 3 && bytesPorMuestra != 4) throw new IllegalArgumentException();
        this.bytesPorMuestra = bytesPorMuestra;
    }

    @Override protected long bytesNecesarios(long frames, int n) { return frames * n * bytesPorMuestra; }
    @Override protected long framesEnBytes(long bytes, int n) { return bytes / ((long) bytesPorMuestra * n); }

    @Override
    protected double[][] decodificarBytes(byte[] d, int inicio, List<MetadatosCanal> canales, int frames) {
        int n = canales.size();
        double[][] r = new double[n][frames];
        int p = inicio;
        for (int t = 0; t < frames; t++) {
            for (int c = 0; c < n; c++) {
                int v = (d[p] & 0xFF) | ((d[p + 1] & 0xFF) << 8) | ((d[p + 2] & 0xFF) << 16);
                v = (bytesPorMuestra == 3) ? (v << 8) >> 8 : (v | (d[p + 3] << 24));
                r[c][t] = v;
                p += bytesPorMuestra;
            }
        }
        return r;
    }
}
