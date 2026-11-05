package ecg.visualizacion;

import ecg.modelo.ConfiguracionVista;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/** Cuadrícula de papel ECG: 0,04 s / 0,1 mV por cuadro pequeño y 0,2 s / 0,5 mV por cuadro grande. */
public class RenderizadorCuadricula {

    private static final Color MENOR = new Color(250, 225, 225);
    private static final Color MAYOR = new Color(240, 170, 170);
    private static final Color CERO = new Color(150, 150, 150);
    private static final Color TEXTO = new Color(90, 90, 90);
    private static final double[] PASOS_ETIQUETA = {0.2, 0.5, 1, 2, 5, 10, 30, 60, 120, 300, 600, 1800, 3600};

    public void dibujar(Graphics2D g, Rectangle celda, ConfiguracionVista c) {
        double t0 = c.getTiempoInicio(), t1 = c.getTiempoFin();
        double pps = RenderizadorECG.pixelesPorSegundo(c, celda);
        double ppmv = RenderizadorECG.pixelesPorMilivoltio(c);
        double yc = celda.y + celda.height / 2.0;

        if (c.isMostrarCuadricula()) {
            lineasVerticales(g, celda, t0, t1, pps, 0.04, MENOR, 3);
            lineasHorizontales(g, celda, yc, ppmv, 0.1, MENOR, 3);
            lineasVerticales(g, celda, t0, t1, pps, 0.2, MAYOR, 6);
            lineasHorizontales(g, celda, yc, ppmv, 0.5, MAYOR, 6);
        }
        g.setColor(CERO);
        g.setStroke(new BasicStroke(0.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 4f}, 0f));
        g.drawLine(celda.x, (int) Math.round(yc), celda.x + celda.width, (int) Math.round(yc));

        etiquetasTiempo(g, celda, t0, t1, pps);
    }

    private void lineasVerticales(Graphics2D g, Rectangle r, double t0, double t1, double pps,
                                  double paso, Color color, double minPx) {
        if (paso * pps < minPx) return;
        g.setColor(color);
        g.setStroke(new BasicStroke(paso >= 0.2 ? 0.9f : 0.5f));
        long k0 = (long) Math.ceil(t0 / paso - 1e-9), k1 = (long) Math.floor(t1 / paso + 1e-9);
        for (long k = k0; k <= k1; k++) {
            int x = (int) Math.round(r.x + (k * paso - t0) * pps);
            g.drawLine(x, r.y, x, r.y + r.height);
        }
    }

    private void lineasHorizontales(Graphics2D g, Rectangle r, double yc, double ppmv, double pasoMv,
                                    Color color, double minPx) {
        if (pasoMv * ppmv < minPx) return;
        g.setColor(color);
        g.setStroke(new BasicStroke(pasoMv >= 0.5 ? 0.9f : 0.5f));
        long kMax = (long) Math.ceil((r.height / 2.0) / (pasoMv * ppmv)) + 1;
        for (long k = -kMax; k <= kMax; k++) {
            int y = (int) Math.round(yc - k * pasoMv * ppmv);
            if (y >= r.y && y <= r.y + r.height) g.drawLine(r.x, y, r.x + r.width, y);
        }
    }

    private void etiquetasTiempo(Graphics2D g, Rectangle r, double t0, double t1, double pps) {
        double paso = PASOS_ETIQUETA[PASOS_ETIQUETA.length - 1];
        for (double p : PASOS_ETIQUETA) if (p * pps >= 70) { paso = p; break; }
        g.setFont(g.getFont().deriveFont(Font.PLAIN, 9f));
        g.setColor(TEXTO);
        long k0 = (long) Math.ceil(t0 / paso - 1e-9), k1 = (long) Math.floor(t1 / paso + 1e-9);
        for (long k = k0; k <= k1; k++) {
            int x = (int) Math.round(r.x + (k * paso - t0) * pps);
            g.drawLine(x, r.y + r.height - 4, x, r.y + r.height);
            g.drawString(formato(k * paso), x + 2, r.y + r.height - 3);
        }
    }

    private static String formato(double t) {
        return (t == Math.rint(t)) ? String.format("%.0f s", t) : String.format("%.1f s", t);
    }
}
