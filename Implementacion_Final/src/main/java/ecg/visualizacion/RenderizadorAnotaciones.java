package ecg.visualizacion;

import ecg.modelo.AnotacionECG;
import ecg.modelo.ConfiguracionVista;
import ecg.modelo.RegistroECG;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/** Marcas de latidos: referencia (azul), manual (verde) y detecciones del algoritmo (rojo). */
public class RenderizadorAnotaciones {

    private static final Color REFERENCIA = new Color(0, 90, 200);
    private static final Color ALGORITMO = new Color(220, 50, 20);
    private static final Color MANUAL = new Color(0, 150, 60);

    public void dibujar(Graphics2D g, RegistroECG registro, Rectangle celda, ConfiguracionVista c) {
        if (!c.isMostrarAnotaciones() && !c.isMostrarDetecciones()) return;
        double fs = registro.getFrecuenciaMuestreo();
        double t0 = c.getTiempoInicio(), t1 = c.getTiempoFin();
        double pps = RenderizadorECG.pixelesPorSegundo(c, celda);
        long m0 = (long) Math.floor(t0 * fs), m1 = (long) Math.ceil(t1 * fs);

        g.setFont(g.getFont().deriveFont(Font.BOLD, 10f));
        boolean denso = pps < 40;   // poco espacio horizontal: sin texto
        for (AnotacionECG a : registro.getAnotaciones()) {
            long i = a.getIndiceMuestra();
            if (i < m0 || i > m1) continue;
            boolean algoritmo = AnotacionECG.FUENTE_ALGORITMO.equals(a.getFuente());
            if (algoritmo ? !c.isMostrarDetecciones() : !c.isMostrarAnotaciones()) continue;

            Color color = algoritmo ? ALGORITMO
                    : AnotacionECG.FUENTE_MANUAL.equals(a.getFuente()) ? MANUAL : REFERENCIA;
            int x = (int) Math.round(celda.x + (i / fs - t0) * pps);
            boolean seleccionada = a == c.getAnotacionSeleccionada();

            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), seleccionada ? 220 : 110));
            g.setStroke(new BasicStroke(seleccionada ? 2f : 1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10f, algoritmo ? new float[]{3f, 3f} : null, 0f));
            g.drawLine(x, celda.y, x, celda.y + celda.height);
            g.setColor(color);
            g.fillPolygon(new int[]{x - 4, x + 4, x}, new int[]{celda.y, celda.y, celda.y + 7}, 3);
            if (!denso || seleccionada) g.drawString(a.getTipo(), x + 3, celda.y + 18);
        }
    }
}
