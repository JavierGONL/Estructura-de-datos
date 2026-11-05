package ecg.exportacion;

import ecg.modelo.ConfiguracionVista;
import ecg.modelo.RegistroECG;
import ecg.utilidades.GestorRutas;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Exporta todo el registro (o un intervalo) dividido en páginas de duración fija: una imagen por página. */
public class ExportadorRegistroCompleto extends ExportadorECG {

    private final ImpresionEstandar impresion = new ImpresionEstandar();

    /**
     * @param inicio,fin       intervalo a exportar en segundos (fin &lt;= 0 → hasta el final)
     * @param segundosPorPagina duración de cada imagen
     * @return rutas de los archivos creados
     */
    public List<Path> exportar(RegistroECG registro, double inicio, double fin, double segundosPorPagina,
                               Path directorioDestino) throws IOException {
        double total = registro.getDuracionSegundos();
        double hasta = (fin <= 0 || fin > total) ? total : fin;
        double paso = Math.max(0.5, segundosPorPagina);
        int paginas = (int) Math.ceil((hasta - inicio) / paso);

        List<Path> creados = new ArrayList<>();
        for (int p = 0; p < paginas; p++) {
            double t0 = inicio + p * paso, t1 = Math.min(hasta, t0 + paso);
            ConfiguracionVista c = impresion.crearConfiguracion(registro, t0, paso);
            c.setTiempoInicio(t0);
            c.setTiempoFin(Math.max(t1, t0 + 0.1));
            BufferedImage img = renderizar(registro, c, impresion.calcularDimension(registro, c)[0],
                    impresion.calcularDimension(registro, c)[1]);
            String nombre = GestorRutas.nombreSegmento(registro.getNombreRegistro(), t0, t1)
                    .replace(".png", String.format("_p%03d.png", p + 1));
            Path destino = directorioDestino.resolve(nombre);
            guardar(img, destino);
            creados.add(destino);
        }
        return creados;
    }
}
