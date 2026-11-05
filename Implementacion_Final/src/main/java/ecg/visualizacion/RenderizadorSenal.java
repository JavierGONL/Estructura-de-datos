package ecg.visualizacion;

import ecg.modelo.CanalECG;
import ecg.modelo.ConfiguracionVista;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Path2D;

/** Dibuja la onda de un canal. Con muchas muestras por píxel usa la envolvente mínimo/máximo. */
public class RenderizadorSenal {

    private static final Color COLOR = new Color(15, 15, 15);
    private static final double LIMITE_Y = 100_000;

    public void dibujar(Graphics2D g, CanalECG canal, Rectangle celda, ConfiguracionVista c) {
        double fs = canal.getFrecuenciaMuestreo();
        double t0 = c.getTiempoInicio(), t1 = c.getTiempoFin();
        double[] x = canal.getMuestras();
        int i0 = (int) Math.max(0, Math.floor(t0 * fs) - 1);
        int i1 = (int) Math.min(x.length - 1, Math.ceil(t1 * fs) + 1);
        if (i1 <= i0 || t1 <= t0) return;

        double pps = RenderizadorECG.pixelesPorSegundo(c, celda);
        double ppmv = RenderizadorECG.pixelesPorMilivoltio(c);
        double yc = celda.y + celda.height / 2.0;

        g.setColor(COLOR);
        g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        if (i1 - i0 > 2 * celda.width) envolvente(g, x, fs, t0, pps, ppmv, yc, celda, i0, i1);
        else polilinea(g, x, fs, t0, pps, ppmv, yc, celda, i0, i1);
    }

    private void polilinea(Graphics2D g, double[] x, double fs, double t0, double pps, double ppmv,
                           double yc, Rectangle celda, int i0, int i1) {
        Path2D.Double path = new Path2D.Double();
        boolean lapizAbajo = false;
        for (int i = i0; i <= i1; i++) {
            if (Double.isNaN(x[i])) { lapizAbajo = false; continue; }
            double px = celda.x + (i / fs - t0) * pps;
            double py = limitar(yc - x[i] * ppmv);
            if (lapizAbajo) path.lineTo(px, py); else { path.moveTo(px, py); lapizAbajo = true; }
        }
        g.draw(path);
    }

    private void envolvente(Graphics2D g, double[] x, double fs, double t0, double pps, double ppmv,
                            double yc, Rectangle celda, int i0, int i1) {
        for (int px = 0; px < celda.width; px++) {
            int a = (int) Math.max(i0, Math.floor((t0 + px / pps) * fs));
            int b = (int) Math.min(i1, Math.floor((t0 + (px + 1) / pps) * fs));
            double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
            for (int i = a; i <= b; i++) {
                if (Double.isNaN(x[i])) continue;
                if (x[i] < min) min = x[i];
                if (x[i] > max) max = x[i];
            }
            if (min > max) continue;
            int xx = celda.x + px;
            g.drawLine(xx, (int) Math.round(limitar(yc - max * ppmv)), xx, (int) Math.round(limitar(yc - min * ppmv)));
        }
    }

    private static double limitar(double y) { return Math.max(-LIMITE_Y, Math.min(LIMITE_Y, y)); }
}
