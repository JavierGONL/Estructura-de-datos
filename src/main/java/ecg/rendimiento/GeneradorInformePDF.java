package ecg.rendimiento;

import ecg.modelo.MetricasEjecucion;
import ecg.rendimiento.ResultadoRendimiento.ResumenOperacion;
import ecg.utilidades.FormateadorDatos;
import ecg.utilidades.GestorArchivos;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;                      // PDFBOX3
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Genera el informe técnico de rendimiento con Apache PDFBox 3.x: información de la ejecución,
 * métricas globales, tabla por operación, tres gráficos y observaciones.
 * No es reentrante: use una instancia por documento.
 */
public class GeneradorInformePDF {

    private static final float MARGEN = 50f;
    private static final float ANCHO_UTIL = PDRectangle.A4.getWidth() - 2 * MARGEN;
    private static final float ALTO_FILA = 15f;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final GraficadorMetricas graficador = new GraficadorMetricas();

    // estado de la generación en curso
    private PDDocument doc;
    private PDPageContentStream cs;
    private PDFont normal, negrita;
    private PDFont monoespaciada;
    private float y;
    private int numeroPagina;
    private ResultadoRendimiento datos;
    private List<ResumenOperacion> resumen;

    /** Informe de lo medido hasta ahora (no vacía el registro). */
    public void generarInforme(RegistroMetricas metricas, Path destino) throws IOException {
        generarInforme(metricas.instantanea(""), destino);
    }

    public void generarInforme(ResultadoRendimiento resultado, Path destino) throws IOException {
        GestorArchivos.asegurarDirectorioPadre(destino);
        this.datos = resultado;
        this.resumen = resultado.resumirPorOperacion();
        this.numeroPagina = 0;
        try (PDDocument d = new PDDocument()) {
            this.doc = d;
            normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);                // PDFBOX3
            negrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);          // PDFBOX3
            monoespaciada = new PDType1Font(Standard14Fonts.FontName.COURIER);           // PDFBOX3
            nuevaPagina();
            try {
                agregarPortada();
                agregarDatosRegistro();
                agregarMetricasGenerales();
                agregarTablaOperaciones();
                agregarGraficos();
                agregarClasificacion();
                agregarObservaciones();
            } finally {
                cerrarPagina();
            }
            d.save(destino.toFile());
        } finally {
            doc = null; cs = null;
        }
    }

    // ------------------------------------------------------------------ secciones

    private void agregarPortada() throws IOException {
        regla();
        y -= 8;
        centrado("INFORME DE RENDIMIENTO", negrita, 22);
        centrado("Sistema de análisis ECG", normal, 13);
        centrado("Generado el " + java.time.LocalDateTime.now().format(FECHA), normal, 10);
        y -= 4;
        regla();
        y -= 14;
    }

    private void agregarDatosRegistro() throws IOException {
        titulo("1. Información de la ejecución");
        double duracion = datos.getFrecuenciaMuestreo() > 0 ? datos.getNumeroMuestras() / datos.getFrecuenciaMuestreo() : 0;
        viñeta("Registro: " + datos.getNombreRegistro());
        viñeta("Canales: " + datos.getNumeroCanales());
        viñeta(String.format(Locale.ROOT, "Frecuencia de muestreo: %.0f Hz", datos.getFrecuenciaMuestreo()));
        viñeta(String.format(Locale.ROOT, "Número de muestras: %d (%.1f s)", datos.getNumeroMuestras(), duracion));
        viñeta("Fecha y hora de la ejecución: " + datos.getFecha().format(FECHA));
        viñeta("Versión de Java: " + datos.getVersionJava());
        viñeta("Sistema operativo: " + datos.getSistemaOperativo() + ", " + datos.getProcesadores() + " procesadores");
        viñeta("Heap máximo permitido a la JVM: " + FormateadorDatos.bytes(datos.getHeapMaximoJVM()));
        y -= 8;
    }

    private void agregarMetricasGenerales() throws IOException {
        titulo("2. Métricas de rendimiento (globales)");
        List<MetricasEjecucion> primero = new ArrayList<>();
        for (MetricasEjecucion m : datos.getMetricas()) if (m.getProfundidad() == 0) primero.add(m);

        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"Operaciones medidas", String.valueOf(datos.getMetricas().size())});
        filas.add(new String[]{"Tiempo transcurrido (suma de operaciones de primer nivel)",
                ms(datos.tiempoTotalPrimerNivelNs()) + " ms"});
        filas.add(new String[]{"CPU de usuario (proceso)", sumaDisponible(primero, MetricasEjecucion::getTiempoCPUUsuario)});
        filas.add(new String[]{"CPU de sistema (proceso)", sumaDisponible(primero, MetricasEjecucion::getTiempoCPUSistema)});
        filas.add(new String[]{"CPU total del proceso", sumaDisponible(primero, MetricasEjecucion::getTiempoCPUTotal)});
        filas.add(new String[]{"Memoria heap máxima observada", FormateadorDatos.bytes(datos.heapMaximoObservado())});
        filas.add(new String[]{"Memoria física residente (RSS) máxima", FormateadorDatos.bytes(datos.rssMaximoObservado())});
        tabla(new String[]{"Métrica", "Resultado"}, filas, new float[]{ANCHO_UTIL * 0.62f, ANCHO_UTIL * 0.38f}, 1);
        y -= 10;
    }

    private void agregarTablaOperaciones() throws IOException {
        titulo("3. Rendimiento por operación");
        List<String[]> filas = new ArrayList<>();
        for (ResumenOperacion r : resumen) {
            filas.add(new String[]{r.operacion, String.valueOf(r.repeticiones),
                    ms(r.tiempoMedioNs), ms(r.tiempoMinNs), ms(r.tiempoMaxNs),
                    msD(r.cpuUsuarioMedioNs), msD(r.cpuSistemaMedioNs), msD(r.cpuTotalMedioNs),
                    FormateadorDatos.mb(r.heapMaximoBytes)});
        }
        float u = ANCHO_UTIL;
        tabla(new String[]{"Operación", "n", "T medio", "T mín", "T máx", "CPU usr", "CPU sis", "CPU tot", "Heap MiB"},
                filas, new float[]{u * .25f, u * .05f, u * .10f, u * .09f, u * .09f, u * .10f, u * .10f, u * .10f, u * .12f}, 1);
        escribir("Tiempos en milisegundos (media, mínimo y máximo); n/d = no disponible en esta plataforma.",
                normal, 8, 0);
        y -= 12;
    }

    private void agregarGraficos() throws IOException {
        asegurarEspacio(ANCHO_UTIL * 520f / 1200f + 50);   // el título no debe quedar solo al final de página
        titulo("4. Gráficos");
        if (resumen.isEmpty()) { escribir("No hay mediciones que graficar.", normal, 10, 0); y -= 8; return; }
        imagen(graficador.graficoMemoria(resumen));
        imagen(graficador.graficoCPU(resumen));
        imagen(graficador.graficoTiempo(resumen));
    }

    private void agregarClasificacion() throws IOException {
        if (datos == null || !datos.tieneClasificacion()) return;   // solo si hubo clasificación RF
        titulo("5. Clasificación de latidos (Random Forest)");
        // El resumen viene preformateado (matriz de confusión + métricas); se imprime en monoespaciada
        // para conservar la alineación de columnas.
        for (String linea : datos.getResumenClasificacion().split("\n", -1)) {
            asegurarEspacio(12);
            y -= 11;
            texto(linea, monoespaciada, 8.5f, MARGEN, y);
        }
        y -= 10;
    }

    private void agregarObservaciones() throws IOException {
        titulo((datos != null && datos.tieneClasificacion() ? "6." : "5.") + " Observaciones");
        if (!datos.getObservaciones().isBlank()) {
            parrafo(datos.getObservaciones());
            y -= 4;
        }
        parrafo("Notas sobre la medición:");
        viñeta("El tiempo transcurrido es de pared: incluye esperas y bloqueos, y no equivale al tiempo de CPU.");
        viñeta("La memoria heap (totalMemory − freeMemory) no es la memoria física del proceso (RSS); son magnitudes distintas.");
        viñeta("El máximo de heap se muestrea cada pocos milisegundos: es una cota inferior del pico real.");
        viñeta("CPU de usuario y de sistema del proceso solo se obtienen en Linux (/proc/self/stat, resolución de 10 ms); "
                + "en otros sistemas se muestra la CPU total del proceso.");
        viñeta("Las operaciones anidadas (p. ej. «Decodificar señales» dentro de «Cargar registro») se incluyen en la "
                + "tabla pero no se suman dos veces en las métricas globales.");
        viñeta("Los resultados dependen del estado del JIT y del recolector de basura; para comparar, repita la medición.");
    }

    // ------------------------------------------------------------------ utilidades de maquetación

    private void nuevaPagina() throws IOException {
        cerrarPagina();
        PDPage p = new PDPage(PDRectangle.A4);
        doc.addPage(p);
        cs = new PDPageContentStream(doc, p);
        numeroPagina++;
        y = PDRectangle.A4.getHeight() - MARGEN;
    }

    private void cerrarPagina() throws IOException {
        if (cs == null) return;
        String pie = "Informe de rendimiento · ECG Viewer · Página " + numeroPagina;
        cs.beginText();
        cs.setFont(normal, 8);
        cs.setNonStrokingColor(new Color(110, 110, 110));
        cs.newLineAtOffset(MARGEN, 28);
        cs.showText(limpiar(pie));
        cs.endText();
        cs.setNonStrokingColor(new Color(0, 0, 0));
        cs.close();
        cs = null;
    }

    private void asegurarEspacio(float alto) throws IOException {
        if (y - alto < MARGEN) nuevaPagina();
    }

    private void regla() throws IOException {
        cs.setStrokingColor(new Color(80, 80, 80));
        cs.setLineWidth(0.8f);
        cs.moveTo(MARGEN, y);
        cs.lineTo(MARGEN + ANCHO_UTIL, y);
        cs.stroke();
        y -= 6;
    }

    private void titulo(String t) throws IOException {
        asegurarEspacio(40);
        y -= 4;
        escribir(t, negrita, 13, 0);
        y -= 4;
    }

    private void centrado(String t, PDFont f, float tam) throws IOException {
        asegurarEspacio(tam + 8);
        float w = ancho(t, f, tam);
        y -= tam + 2;
        texto(t, f, tam, MARGEN + (ANCHO_UTIL - w) / 2, y);
    }

    private void viñeta(String t) throws IOException {
        boolean primera = true;
        for (String l : ajustar(t, normal, 10, ANCHO_UTIL - 14)) {
            asegurarEspacio(14);
            y -= 13;
            if (primera) {                       // marca de viñeta
                cs.addRect(MARGEN + 3, y + 3, 3, 3);
                cs.fill();
                primera = false;
            }
            texto(l, normal, 10, MARGEN + 14, y);
        }
    }

    private void parrafo(String t) throws IOException {
        for (String l : ajustar(t, normal, 10, ANCHO_UTIL)) {
            asegurarEspacio(14);
            y -= 13;
            texto(l, normal, 10, MARGEN, y);
        }
    }

    private void escribir(String t, PDFont f, float tam, float sangria) throws IOException {
        asegurarEspacio(tam + 4);
        y -= tam + 2;
        texto(t, f, tam, MARGEN + sangria, y);
    }

    private void texto(String t, PDFont f, float tam, float x, float yy) throws IOException {
        cs.beginText();
        cs.setFont(f, tam);
        cs.newLineAtOffset(x, yy);
        cs.showText(limpiar(t));
        cs.endText();
    }

    private void imagen(BufferedImage img) throws IOException {
        float w = ANCHO_UTIL, h = w * img.getHeight() / img.getWidth();
        asegurarEspacio(h + 8);
        PDImageXObject px = LosslessFactory.createFromImage(doc, img);
        cs.drawImage(px, MARGEN, y - h, w, h);
        y -= h + 10;
    }

    /** Tabla con cabecera sombreada; las columnas desde {@code primeraNumerica} se alinean a la derecha. */
    private void tabla(String[] cab, List<String[]> filas, float[] anchos, int primeraNumerica) throws IOException {
        dibujarFila(cab, anchos, true, false, primeraNumerica);
        boolean alterna = false;
        for (String[] f : filas) {
            if (y - ALTO_FILA < MARGEN) { nuevaPagina(); dibujarFila(cab, anchos, true, false, primeraNumerica); }
            dibujarFila(f, anchos, false, alterna, primeraNumerica);
            alterna = !alterna;
        }
        cs.setStrokingColor(new Color(80, 80, 80));
        cs.setLineWidth(0.6f);
        cs.moveTo(MARGEN, y);
        cs.lineTo(MARGEN + ANCHO_UTIL, y);
        cs.stroke();
    }

    private void dibujarFila(String[] celdas, float[] anchos, boolean cabecera, boolean sombreada,
                             int primeraNumerica) throws IOException {
        asegurarEspacio(ALTO_FILA);
        float yTop = y;
        if (cabecera || sombreada) {
            cs.setNonStrokingColor(new Color(cabecera ? 215 : 240, cabecera ? 224 : 240, cabecera ? 238 : 240));
            cs.addRect(MARGEN, yTop - ALTO_FILA, ANCHO_UTIL, ALTO_FILA);
            cs.fill();
            cs.setNonStrokingColor(new Color(0, 0, 0));
        }
        float tam = anchos.length > 4 ? 8f : 9.5f;
        PDFont f = cabecera ? negrita : normal;
        float x = MARGEN;
        for (int i = 0; i < celdas.length; i++) {
            String t = recortar(celdas[i], f, tam, anchos[i] - 6);
            float tx = (i >= primeraNumerica && anchos.length > 2) ? x + anchos[i] - 3 - ancho(t, f, tam) : x + 3;
            texto(t, f, tam, tx, yTop - ALTO_FILA + 4.5f);
            x += anchos[i];
        }
        y -= ALTO_FILA;
    }

    // ------------------------------------------------------------------ texto

    private float ancho(String t, PDFont f, float tam) throws IOException {
        return f.getStringWidth(limpiar(t)) / 1000f * tam;
    }

    private String recortar(String t, PDFont f, float tam, float max) throws IOException {
        t = limpiar(t);
        if (ancho(t, f, tam) <= max) return t;
        while (t.length() > 1 && ancho(t + "…", f, tam) > max) t = t.substring(0, t.length() - 1);
        return t + "...";
    }

    private List<String> ajustar(String texto, PDFont f, float tam, float max) throws IOException {
        List<String> lineas = new ArrayList<>();
        for (String parrafo : limpiar(texto).split("\n")) {
            StringBuilder actual = new StringBuilder();
            for (String palabra : parrafo.split(" ")) {
                String prueba = actual.length() == 0 ? palabra : actual + " " + palabra;
                if (ancho(prueba, f, tam) <= max || actual.length() == 0) actual = new StringBuilder(prueba);
                else { lineas.add(actual.toString()); actual = new StringBuilder(palabra); }
            }
            lineas.add(actual.toString());
        }
        return lineas;
    }

    /** Las fuentes estándar usan WinAnsi: se sustituye lo que no puedan codificar. */
    private static String limpiar(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c == '\u2013' || c == '\u2014' || c == '\u2212') sb.append('-');
            else if (c == '\u2026') sb.append("...");
            else if (c == '\n' || c == '\t' || c < 32) sb.append(' ');
            else if (c > 255) sb.append('?');
            else sb.append(c);
        }
        return sb.toString();
    }

    private static String ms(double ns) { return String.format(Locale.ROOT, "%.2f", ns / 1e6); }

    private static String msD(double ns) { return ns < 0 ? "n/d" : String.format(Locale.ROOT, "%.2f", ns / 1e6); }

    private static String sumaDisponible(List<MetricasEjecucion> m,
                                         java.util.function.ToLongFunction<MetricasEjecucion> f) {
        long suma = 0; boolean alguno = false;
        for (MetricasEjecucion x : m) { long v = f.applyAsLong(x); if (v >= 0) { suma += v; alguno = true; } }
        return alguno ? ms(suma) + " ms" : "no disponible";
    }
}
