package ecg.visualizacion;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/** Forma de repartir las derivaciones visibles dentro del área de dibujo. */
public enum DistribucionCanales {

    /** Tres columnas; las filas necesarias (cuatro para doce derivaciones), en orden de lectura. */
    CUADRICULA_3X4("Cuadrícula 3×4"),
    /** Canales apilados verticalmente. */
    VERTICAL("Vertical"),
    /** Una sola derivación ocupa toda el área. */
    INDIVIDUAL("Individual"),
    /** Filas y columnas calculadas según el número de canales. */
    AUTOMATICA("Automática");

    private final String etiqueta;

    DistribucionCanales(String etiqueta) { this.etiqueta = etiqueta; }

    public String getEtiqueta() { return etiqueta; }

    @Override public String toString() { return etiqueta; }

    public static DistribucionCanales desdeNombre(String nombre) {
        try { return valueOf(nombre); } catch (RuntimeException e) { return AUTOMATICA; }
    }

    /** Número de canales que realmente se dibujan con esta distribución. */
    public int canalesDibujados(int visibles) { return this == INDIVIDUAL ? Math.min(1, visibles) : visibles; }

    public int[] filasYColumnas(int n) {
        switch (this) {
            case CUADRICULA_3X4: return new int[]{(int) Math.ceil(n / 3.0), 3};
            case VERTICAL:       return new int[]{n, 1};
            case INDIVIDUAL:     return new int[]{1, 1};
            default:
                if (n <= 3) return new int[]{n, 1};
                int cols = (int) Math.ceil(Math.sqrt(n));
                return new int[]{(int) Math.ceil(n / (double) cols), cols};
        }
    }

    /** Un rectángulo por canal dibujado, en orden de lectura (por filas). */
    public List<Rectangle> calcular(int visibles, Rectangle area) {
        int n = canalesDibujados(visibles);
        List<Rectangle> celdas = new ArrayList<>(n);
        if (n == 0) return celdas;
        int[] fc = filasYColumnas(n);
        int filas = fc[0], cols = fc[1];
        for (int k = 0; k < n; k++) {
            int f = k / cols, c = k % cols;
            int x0 = area.x + c * area.width / cols, x1 = area.x + (c + 1) * area.width / cols;
            int y0 = area.y + f * area.height / filas, y1 = area.y + (f + 1) * area.height / filas;
            celdas.add(new Rectangle(x0, y0, x1 - x0, y1 - y0));
        }
        return celdas;
    }
}
