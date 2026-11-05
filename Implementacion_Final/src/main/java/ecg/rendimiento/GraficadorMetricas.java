package ecg.rendimiento;

import ecg.rendimiento.ResultadoRendimiento.ResumenOperacion;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Locale;

/** Gráficos de barras (Java2D) de las métricas por operación; se insertan como PNG en el informe PDF. */
public class GraficadorMetricas {

    private static final Color[] PALETA = {new Color(40, 100, 180), new Color(230, 130, 30), new Color(90, 160, 90)};
    private static final int W = 1200, H = 520;

    private static class Serie {
        final String nombre; final double[] valores;     // valor < 0 = no disponible
        Serie(String n, double[] v) { nombre = n; valores = v; }
    }

    public BufferedImage graficoMemoria(List<ResumenOperacion> r) {
        double[] heap = r.stream().mapToDouble(x -> x.heapMaximoBytes / 1048576.0).toArray();
        return barras("Memoria heap máxima por operación", "MiB", etiquetas(r), List.of(new Serie("Heap máximo", heap)));
    }

    public BufferedImage graficoCPU(List<ResumenOperacion> r) {
        double[] usuario = r.stream().mapToDouble(x -> x.cpuUsuarioMedioNs < 0 ? -1 : x.cpuUsuarioMedioNs / 1e6).toArray();
        double[] sistema = r.stream().mapToDouble(x -> x.cpuSistemaMedioNs < 0 ? -1 : x.cpuSistemaMedioNs / 1e6).toArray();
        double[] total = r.stream().mapToDouble(x -> x.cpuTotalMedioNs < 0 ? -1 : x.cpuTotalMedioNs / 1e6).toArray();
        boolean haySO = java.util.Arrays.stream(usuario).anyMatch(v -> v >= 0);
        List<Serie> series = haySO
                ? List.of(new Serie("CPU usuario", usuario), new Serie("CPU sistema", sistema))
                : List.of(new Serie("CPU total del proceso", total));
        return barras("Tiempo de CPU medio por operación"
                + (haySO ? "" : " (usuario/sistema no disponible en este SO)"), "ms", etiquetas(r), series);
    }

    public BufferedImage graficoTiempo(List<ResumenOperacion> r) {
        double[] t = r.stream().mapToDouble(x -> x.tiempoMedioNs / 1e6).toArray();
        return barras("Tiempo transcurrido medio por operación", "ms", etiquetas(r), List.of(new Serie("Tiempo de pared", t)));
    }

    private static String[] etiquetas(List<ResumenOperacion> r) {
        return r.stream().map(x -> x.operacion).toArray(String[]::new);
    }

    private BufferedImage barras(String titulo, String unidad, String[] etiquetas, List<Serie> series) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);

        int izq = 90, der = 30, arriba = 70, abajo = 130;
        int ancho = W - izq - der, alto = H - arriba - abajo;

        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        g.drawString(titulo, izq, 36);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        g.drawString("[" + unidad + "]", 20, arriba - 14);

        double max = 0;
        for (Serie s : series) for (double v : s.valores) max = Math.max(max, v);
        double techo = max <= 0 ? 1 : techoAgradable(max);

        FontMetrics fm = g.getFontMetrics();
        for (int k = 0; k <= 5; k++) {
            int y = arriba + alto - (int) Math.round(alto * k / 5.0);
            g.setColor(new Color(225, 225, 225));
            g.drawLine(izq, y, izq + ancho, y);
            g.setColor(Color.DARK_GRAY);
            String txt = formato(techo * k / 5.0);
            g.drawString(txt, izq - 8 - fm.stringWidth(txt), y + 5);
        }
        g.setColor(Color.DARK_GRAY);
        g.drawLine(izq, arriba, izq, arriba + alto);
        g.drawLine(izq, arriba + alto, izq + ancho, arriba + alto);

        int grupos = etiquetas.length;
        if (grupos == 0) { g.dispose(); return img; }
        double anchoGrupo = (double) ancho / grupos;
        double anchoBarra = Math.min(70, anchoGrupo * 0.7 / series.size());
        for (int i = 0; i < grupos; i++) {
            double x0 = izq + i * anchoGrupo + (anchoGrupo - anchoBarra * series.size()) / 2;
            for (int s = 0; s < series.size(); s++) {
                double v = series.get(s).valores[i];
                int bx = (int) Math.round(x0 + s * anchoBarra);
                if (v < 0) {
                    g.setColor(Color.GRAY);
                    g.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
                    g.drawString("n/d", bx + 4, arriba + alto - 6);
                    continue;
                }
                int h = (int) Math.round(alto * v / techo);
                g.setColor(PALETA[s % PALETA.length]);
                g.fillRect(bx, arriba + alto - h, (int) Math.round(anchoBarra) - 2, h);
                g.setColor(Color.BLACK);
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                g.drawString(formato(v), bx, arriba + alto - h - 4);
            }
            // etiqueta de la operación, girada para que quepa
            Graphics2D gr = (Graphics2D) g.create();
            gr.setColor(Color.BLACK);
            gr.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            gr.translate(izq + (i + 0.5) * anchoGrupo, arriba + alto + 14);
            gr.rotate(Math.toRadians(35));
            gr.drawString(etiquetas[i], 0, 0);
            gr.dispose();
        }

        // leyenda
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        int lx = izq + ancho - 200;
        for (int s = 0; s < series.size(); s++) {
            g.setColor(PALETA[s % PALETA.length]);
            g.fillRect(lx, 20 + s * 20, 14, 14);
            g.setColor(Color.BLACK);
            g.drawString(series.get(s).nombre, lx + 20, 32 + s * 20);
        }
        g.dispose();
        return img;
    }

    private static double techoAgradable(double v) {
        double exp = Math.pow(10, Math.floor(Math.log10(v)));
        for (double m : new double[]{1, 1.5, 2, 2.5, 3, 4, 5, 7.5, 10}) if (m * exp >= v * 1.05) return m * exp;
        return 10 * exp;
    }

    private static String formato(double v) {
        return v >= 100 ? String.format(Locale.ROOT, "%.0f", v)
                : v >= 10 ? String.format(Locale.ROOT, "%.1f", v) : String.format(Locale.ROOT, "%.2f", v);
    }
}
