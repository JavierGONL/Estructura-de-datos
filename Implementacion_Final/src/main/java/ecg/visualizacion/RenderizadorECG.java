package ecg.visualizacion;

import ecg.modelo.CanalECG;
import ecg.modelo.ConfiguracionVista;
import ecg.modelo.RegistroECG;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * Motor principal de dibujo. No conoce ventanas ni controles: recibe un registro, una configuración y
 * una superficie. Tanto la pantalla como todas las exportaciones usan esta misma clase.
 *
 * Geometría: el intervalo [tiempoInicio, tiempoFin] se ajusta al ancho de cada celda; la amplitud
 * es {@code escalaVertical (mm/mV) × pixelesPorMm} píxeles por mV.
 */
public class RenderizadorECG {

    public static final int MARGEN = 8;
    public static final int ALTO_ENCABEZADO = 36;

    private final RenderizadorCuadricula cuadricula = new RenderizadorCuadricula();
    private final RenderizadorSenal senal = new RenderizadorSenal();
    private final RenderizadorAnotaciones anotaciones = new RenderizadorAnotaciones();
    private final RenderizadorCursor cursor = new RenderizadorCursor();

    // ---------- geometría compartida con los gestores ----------

    public static double pixelesPorSegundo(ConfiguracionVista c, Rectangle celda) {
        return celda.width / Math.max(1e-9, c.getTiempoFin() - c.getTiempoInicio());
    }

    public static double pixelesPorMilivoltio(ConfiguracionVista c) {
        return c.getEscalaVertical() * c.getPixelesPorMm();
    }

    public static double xATiempo(ConfiguracionVista c, Rectangle celda, double x) {
        return c.getTiempoInicio() + (x - celda.x) / pixelesPorSegundo(c, celda);
    }

    public static double yAMilivoltios(ConfiguracionVista c, Rectangle celda, double y) {
        return (celda.y + celda.height / 2.0 - y) / pixelesPorMilivoltio(c);
    }

    /** Índices (válidos) de los canales que se dibujan, según selección y distribución. */
    public List<Integer> canalesEfectivos(RegistroECG registro, ConfiguracionVista c) {
        List<Integer> r = new ArrayList<>();
        for (int i : c.getCanalesVisibles()) {
            if (i >= 0 && i < registro.getNumeroCanales() && !r.contains(i)) r.add(i);
        }
        r.sort(Integer::compare);
        int max = DistribucionCanales.desdeNombre(c.getDistribucion()).canalesDibujados(r.size());
        return new ArrayList<>(r.subList(0, max));
    }

    public Rectangle areaDeDibujo(ConfiguracionVista c, Dimension dim) {
        int cab = c.isMostrarEncabezado() ? ALTO_ENCABEZADO : 0;
        return new Rectangle(MARGEN, MARGEN + cab, Math.max(1, dim.width - 2 * MARGEN),
                Math.max(1, dim.height - 2 * MARGEN - cab));
    }

    /** Una celda por canal efectivo, en el mismo orden que {@link #canalesEfectivos}. */
    public List<Rectangle> calcularCeldas(RegistroECG registro, ConfiguracionVista c, Dimension dim) {
        return DistribucionCanales.desdeNombre(c.getDistribucion())
                .calcular(canalesEfectivos(registro, c).size(), areaDeDibujo(c, dim));
    }

    // ---------- dibujo ----------

    public void dibujar(Graphics2D g2, RegistroECG registro, ConfiguracionVista c, Dimension dim) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, dim.width, dim.height);

        if (registro == null) return;
        if (c.isMostrarEncabezado()) dibujarEncabezado(g2, registro, c, dim);

        List<Integer> canales = canalesEfectivos(registro, c);
        List<Rectangle> celdas = calcularCeldas(registro, c, dim);
        if (canales.isEmpty()) {
            g2.setColor(Color.GRAY);
            g2.drawString("Ninguna derivación seleccionada", MARGEN + 10, MARGEN + 30);
            return;
        }

        for (int k = 0; k < canales.size(); k++) {
            CanalECG canal = registro.getCanal(canales.get(k));
            Rectangle r = celdas.get(k);
            Graphics2D g = (Graphics2D) g2.create();
            g.clipRect(r.x, r.y, r.width + 1, r.height + 1);

            cuadricula.dibujar(g, r, c);
            cursor.dibujarSeleccion(g, r, c);
            senal.dibujar(g, canal, r, c);
            anotaciones.dibujar(g, registro, r, c);
            cursor.dibujarCursor(g, r, c, canal, canales.get(k) == c.getCursorCanal());

            if (c.isMostrarNombres()) {
                g.setFont(g.getFont().deriveFont(Font.BOLD, 13f));
                g.setColor(new Color(20, 40, 110));
                g.drawString(canal.getNombre(), r.x + 6, r.y + 16);
            }
            g.dispose();

            g2.setColor(new Color(120, 120, 120));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRect(r.x, r.y, r.width, r.height);
        }
    }

    private void dibujarEncabezado(Graphics2D g, RegistroECG r, ConfiguracionVista c, Dimension dim) {
        g.setColor(Color.BLACK);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 14f));
        g.drawString("Registro: " + r.getNombreRegistro(), MARGEN, MARGEN + 14);
        g.setFont(g.getFont().deriveFont(Font.PLAIN, 11f));
        String linea = String.format("Fs = %.0f Hz   |   %d derivaciones   |   %.2f s – %.2f s   |   %.0f mm/s, %.0f mm/mV",
                r.getFrecuenciaMuestreo(), r.getNumeroCanales(), c.getTiempoInicio(), c.getTiempoFin(),
                c.getEscalaHorizontal(), c.getEscalaVertical());
        g.drawString(linea, MARGEN, MARGEN + 30);
        FontMetrics fm = g.getFontMetrics();
        String info = "ECG Viewer";
        g.drawString(info, dim.width - MARGEN - fm.stringWidth(info), MARGEN + 14);
    }
}
