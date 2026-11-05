package ecg.visualizacion;

import ecg.modelo.CanalECG;
import ecg.modelo.ConfiguracionVista;
import ecg.modelo.IntervaloTemporal;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/** Cursor de inspección (línea vertical y lectura de amplitud) e intervalo seleccionado. */
public class RenderizadorCursor {

    private static final Color CURSOR = new Color(0, 110, 90);
    private static final Color SELECCION = new Color(60, 120, 255, 45);

    public void dibujarSeleccion(Graphics2D g, Rectangle celda, ConfiguracionVista c) {
        IntervaloTemporal iv = c.getIntervaloSeleccionado();
        if (iv == null) return;
        double pps = RenderizadorECG.pixelesPorSegundo(c, celda);
        int xa = (int) Math.round(celda.x + (iv.getTiempoInicio() - c.getTiempoInicio()) * pps);
        int xb = (int) Math.round(celda.x + (iv.getTiempoFin() - c.getTiempoInicio()) * pps);
        g.setColor(SELECCION);
        g.fillRect(xa, celda.y, Math.max(1, xb - xa), celda.height);
        g.setColor(new Color(60, 120, 255, 160));
        g.setStroke(new BasicStroke(1f));
        g.drawLine(xa, celda.y, xa, celda.y + celda.height);
        g.drawLine(xb, celda.y, xb, celda.y + celda.height);
    }

    /** @param canal canal dibujado en la celda (para marcar la amplitud); puede ser null */
    public void dibujarCursor(Graphics2D g, Rectangle celda, ConfiguracionVista c, CanalECG canal,
                              boolean celdaDelCursor) {
        double t = c.getCursorTiempo();
        if (!c.isMostrarCursor() || Double.isNaN(t) || t < c.getTiempoInicio() || t > c.getTiempoFin()) return;
        double pps = RenderizadorECG.pixelesPorSegundo(c, celda);
        int x = (int) Math.round(celda.x + (t - c.getTiempoInicio()) * pps);
        g.setColor(CURSOR);
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{5f, 3f}, 0f));
        g.drawLine(x, celda.y, x, celda.y + celda.height);

        if (celdaDelCursor && canal != null) {
            int i = (int) Math.round(t * canal.getFrecuenciaMuestreo());
            if (i >= 0 && i < canal.getNumeroMuestras() && !Double.isNaN(canal.getMuestra(i))) {
                double v = canal.getAmplitudEnMilivoltios(i);
                int y = (int) Math.round(celda.y + celda.height / 2.0 - v * RenderizadorECG.pixelesPorMilivoltio(c));
                g.setStroke(new BasicStroke(1.5f));
                g.drawOval(x - 4, y - 4, 8, 8);
                String txt = String.format("%.3f s  %.3f %s", t, v, canal.getUnidades());
                g.setFont(g.getFont().deriveFont(Font.BOLD, 11f));
                FontMetrics fm = g.getFontMetrics();
                int w = fm.stringWidth(txt) + 8;
                int bx = Math.min(x + 8, celda.x + celda.width - w - 2), by = celda.y + celda.height - fm.getHeight() - 14;
                g.setColor(new Color(255, 255, 255, 220));
                g.fillRect(bx, by, w, fm.getHeight() + 2);
                g.setColor(CURSOR);
                g.drawRect(bx, by, w, fm.getHeight() + 2);
                g.drawString(txt, bx + 4, by + fm.getAscent() + 1);
            }
        }
    }
}
