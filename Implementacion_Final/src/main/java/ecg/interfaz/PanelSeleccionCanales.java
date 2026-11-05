package ecg.interfaz;

import ecg.modelo.RegistroECG;
import ecg.visualizacion.DistribucionCanales;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Casillas de las derivaciones, generadas a partir de los canales del registro (nunca doce fijas). */
public class PanelSeleccionCanales extends JPanel {

    private final List<JCheckBox> casillas = new ArrayList<>();
    private final JComboBox<DistribucionCanales> distribucion;
    private boolean actualizando;

    /**
     * @param alCambiarCanal  (indice, visible) cada vez que el usuario marca o desmarca una casilla
     * @param alCambiarDistribucion nombre de la distribución elegida; null si no se desea el selector
     */
    public PanelSeleccionCanales(RegistroECG registro, List<Integer> visibles,
                                 BiConsumer<Integer, Boolean> alCambiarCanal, Runnable todos, Runnable ninguno,
                                 Consumer<String> alCambiarDistribucion) {
        setLayout(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder("Derivaciones (" + registro.getNumeroCanales() + ")"));

        JPanel lista = new JPanel(new GridLayout(0, registro.getNumeroCanales() > 12 ? 2 : 1));
        for (int i = 0; i < registro.getNumeroCanales(); i++) {
            final int idx = i;
            JCheckBox cb = new JCheckBox(registro.getCanal(i).getNombre(), visibles.contains(i));
            cb.addActionListener(e -> { if (!actualizando) alCambiarCanal.accept(idx, cb.isSelected()); });
            casillas.add(cb);
            lista.add(cb);
        }
        add(new JScrollPane(lista), BorderLayout.CENTER);

        JPanel abajo = new JPanel();
        abajo.setLayout(new BoxLayout(abajo, BoxLayout.Y_AXIS));
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        JButton bt = new JButton("Todas"), bn = new JButton("Ninguna");
        bt.addActionListener(e -> todos.run());
        bn.addActionListener(e -> ninguno.run());
        botones.add(bt);
        botones.add(bn);
        abajo.add(botones);

        if (alCambiarDistribucion != null) {
            distribucion = new JComboBox<>(DistribucionCanales.values());
            distribucion.addActionListener(e -> {
                if (!actualizando) alCambiarDistribucion.accept(((DistribucionCanales) distribucion.getSelectedItem()).name());
            });
            JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
            fila.add(distribucion);
            abajo.add(fila);
        } else {
            distribucion = null;
        }
        add(abajo, BorderLayout.SOUTH);
    }

    /** Refleja en las casillas un cambio hecho desde otro lugar (sin disparar eventos). */
    public void sincronizar(List<Integer> visibles, String nombreDistribucion) {
        actualizando = true;
        for (int i = 0; i < casillas.size(); i++) casillas.get(i).setSelected(visibles.contains(i));
        if (distribucion != null && nombreDistribucion != null) {
            distribucion.setSelectedItem(DistribucionCanales.desdeNombre(nombreDistribucion));
        }
        actualizando = false;
    }

    public List<Integer> getSeleccion() {
        List<Integer> l = new ArrayList<>();
        for (int i = 0; i < casillas.size(); i++) if (casillas.get(i).isSelected()) l.add(i);
        return l;
    }
}
