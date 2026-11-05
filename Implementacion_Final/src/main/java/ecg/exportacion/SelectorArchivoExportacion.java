package ecg.exportacion;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Diálogo para elegir el destino de una exportación PNG con nombre sugerido y confirmación de sobrescritura. */
public class SelectorArchivoExportacion {

    public Optional<Path> elegirDestino(Component padre, Path directorioInicial, String nombreSugerido) {
        JFileChooser fc = new JFileChooser(directorioInicial == null ? null : directorioInicial.toFile());
        fc.setDialogTitle("Exportar imagen PNG");
        fc.setFileFilter(new FileNameExtensionFilter("Imagen PNG (*.png)", "png"));
        fc.setSelectedFile(new java.io.File(fc.getCurrentDirectory(), nombreSugerido));
        if (fc.showSaveDialog(padre) != JFileChooser.APPROVE_OPTION) return Optional.empty();

        Path destino = fc.getSelectedFile().toPath();
        if (!destino.getFileName().toString().toLowerCase().endsWith(".png")) {
            destino = destino.resolveSibling(destino.getFileName() + ".png");
        }
        if (Files.exists(destino) && JOptionPane.showConfirmDialog(padre,
                "El archivo ya existe. ¿Sobrescribirlo?", "Confirmar", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) {
            return Optional.empty();
        }
        return Optional.of(destino);
    }

    public Optional<Path> elegirDirectorio(Component padre, Path directorioInicial) {
        JFileChooser fc = new JFileChooser(directorioInicial == null ? null : directorioInicial.toFile());
        fc.setDialogTitle("Carpeta de destino");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        return fc.showSaveDialog(padre) == JFileChooser.APPROVE_OPTION
                ? Optional.of(fc.getSelectedFile().toPath()) : Optional.empty();
    }
}
