package ecg.interfaz;

import ecg.visualizacion.GestorCursor;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;

/** Tiempo, muestra, derivación y amplitud bajo el cursor. */
public class PanelInformacionCursor extends JPanel {

    private final JLabel etiqueta = new JLabel(" ");

    public PanelInformacionCursor() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        etiqueta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        add(etiqueta, BorderLayout.CENTER);
        mostrar(null);
    }

    public void mostrar(GestorCursor.InfoCursor info) {
        etiqueta.setText(info == null
                ? "Mueva el ratón sobre una derivación · rueda: zoom · arrastrar: desplazar · Mayús+arrastrar: intervalo"
                : info.toString());
    }
}
