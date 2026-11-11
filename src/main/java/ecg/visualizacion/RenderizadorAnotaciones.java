package ecg.visualizacion;

import ecg.modelo.AnotacionECG;
import ecg.modelo.ConfiguracionVista;
import ecg.modelo.RegistroECG;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/** Marcas de latidos: referencia (azul), manual (verde), detecciones del algoritmo (rojo) y
 * clasificación del Random Forest (color por clase AAMI, marcada en el borde inferior). */
public class RenderizadorAnotaciones {

    private static final Color REFERENCIA = new Color(0, 90, 200);
    private static final Color ALGORITMO = new Color(220, 50, 20);
    private static final Color MANUAL = new Color(0, 150, 60);

    // Colores por clase AAMI para las marcas del Random Forest (N, S, V, F).
    private static final Color CLASE_N = new Color(0, 150, 60);     // normal: verde
    private static final Color CLASE_S = new Color(230, 150, 0);    // supraventricular: ámbar
    private static final Color CLASE_V = new Color(200, 0, 160);    // ventricular: magenta
    private static final Color CLASE_F = new Color(120, 60, 200);   // fusión: violeta
    private static final Color CLASE_OTRA = new Color(90, 90, 90);  // cualquier otra etiqueta

    // Objetos reutilizados en cada repaint (antes se creaban por cada anotación visible).
    private static final float[] DISCONTINUO = {3f, 3f};
    private static final BasicStroke TRAZO_NORMAL =
            new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, null, 0f);
    private static final BasicStroke TRAZO_SEL =
            new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, null, 0f);
    private static final BasicStroke TRAZO_ALGORITMO =
            new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, DISCONTINUO, 0f);
    private static final BasicStroke TRAZO_ALGORITMO_SEL =
            new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, DISCONTINUO, 0f);

    // Versiones translúcidas (alfa 110 normal, 220 seleccionada) cacheadas por color.
    private static final Color REFERENCIA_T = conAlfa(REFERENCIA, 110);
    private static final Color ALGORITMO_T = conAlfa(ALGORITMO, 110);
    private static final Color MANUAL_T = conAlfa(MANUAL, 110);
    private static final Color REFERENCIA_TS = conAlfa(REFERENCIA, 220);
    private static final Color ALGORITMO_TS = conAlfa(ALGORITMO, 220);
    private static final Color MANUAL_TS = conAlfa(MANUAL, 220);

    private static Color conAlfa(Color c, int alfa) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alfa);
    }

    /** Color de una marca de clasificación según su etiqueta de clase AAMI. */
    private static Color colorClase(String tipo) {
        if (tipo == null || tipo.isEmpty()) return CLASE_OTRA;
        return switch (tipo.charAt(0)) {
            case 'N' -> CLASE_N;
            case 'S' -> CLASE_S;
            case 'V' -> CLASE_V;
            case 'F' -> CLASE_F;
            default -> CLASE_OTRA;
        };
    }

    public void dibujar(Graphics2D g, RegistroECG registro, Rectangle celda, ConfiguracionVista c) {
        boolean verRef = c.isMostrarAnotaciones();
        boolean verDet = c.isMostrarDetecciones();
        boolean verClas = c.isMostrarClasificacion();
        if (!verRef && !verDet && !verClas) return;
        double fs = registro.getFrecuenciaMuestreo();
        double t0 = c.getTiempoInicio(), t1 = c.getTiempoFin();
        double pps = RenderizadorECG.pixelesPorSegundo(c, celda);
        long m0 = (long) Math.floor(t0 * fs), m1 = (long) Math.ceil(t1 * fs);

        g.setFont(g.getFont().deriveFont(Font.BOLD, 10f));
        boolean denso = pps < 40;   // poco espacio horizontal: sin texto
        for (AnotacionECG a : registro.getAnotaciones()) {
            long i = a.getIndiceMuestra();
            if (i < m0 || i > m1) continue;
            String fuente = a.getFuente();
            boolean algoritmo = AnotacionECG.FUENTE_ALGORITMO.equals(fuente);
            boolean clasificacion = AnotacionECG.FUENTE_CLASIFICACION.equals(fuente);
            boolean manual = AnotacionECG.FUENTE_MANUAL.equals(fuente);

            boolean visible = clasificacion ? verClas : algoritmo ? verDet : verRef;
            if (!visible) continue;

            int x = (int) Math.round(celda.x + (i / fs - t0) * pps);
            boolean seleccionada = a == c.getAnotacionSeleccionada();

            Color color;
            Color trazoColor;
            if (clasificacion) {
                color = colorClase(a.getTipo());
                trazoColor = conAlfa(color, seleccionada ? 230 : 150);
            } else {
                color = algoritmo ? ALGORITMO : manual ? MANUAL : REFERENCIA;
                trazoColor = seleccionada
                        ? (algoritmo ? ALGORITMO_TS : manual ? MANUAL_TS : REFERENCIA_TS)
                        : (algoritmo ? ALGORITMO_T : manual ? MANUAL_T : REFERENCIA_T);
            }
            g.setColor(trazoColor);
            g.setStroke(algoritmo ? (seleccionada ? TRAZO_ALGORITMO_SEL : TRAZO_ALGORITMO)
                    : (seleccionada ? TRAZO_SEL : TRAZO_NORMAL));
            g.drawLine(x, celda.y, x, celda.y + celda.height);
            g.setColor(color);
            // Las de clasificación marcan abajo para no chocar con referencia/detección arriba.
            if (clasificacion) {
                g.fillPolygon(new int[]{x - 4, x + 4, x}, new int[]{celda.y + celda.height,
                        celda.y + celda.height, celda.y + celda.height - 7}, 3);
                if (!denso || seleccionada) g.drawString(a.getTipo(), x + 3, celda.y + celda.height - 10);
            } else {
                g.fillPolygon(new int[]{x - 4, x + 4, x}, new int[]{celda.y, celda.y, celda.y + 7}, 3);
                if (!denso || seleccionada) g.drawString(a.getTipo(), x + 3, celda.y + 18);
            }
        }
    }
}
