package ecg.interfaz;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.FlowLayout;

/** Ampliar y reducir horizontal y verticalmente. */
public class PanelControlZoom extends JPanel {

    public PanelControlZoom(ControladorECG c) {
        setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));
        setBorder(BorderFactory.createTitledBorder("Zoom"));
        add(boton("H +", "Ampliar el tiempo (rueda / +)", () -> c.zoomHorizontal(1.25)));
        add(boton("H −", "Reducir el tiempo (rueda / −)", () -> c.zoomHorizontal(0.8)));
        add(boton("V +", "Ampliar la amplitud (Ctrl + rueda)", () -> c.zoomVertical(1.25)));
        add(boton("V −", "Reducir la amplitud (Ctrl + rueda)", () -> c.zoomVertical(0.8)));
        add(boton("Restablecer", "10 s y 10 mm/mV (Inicio)", c::restablecerZoom));
    }

    private static JButton boton(String texto, String ayuda, Runnable r) {
        JButton b = new JButton(texto);
        b.setToolTipText(ayuda);
        b.addActionListener(e -> r.run());
        return b;
    }
}
