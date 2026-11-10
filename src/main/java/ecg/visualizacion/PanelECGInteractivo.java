package ecg.visualizacion;

import ecg.modelo.ConfiguracionVista;
import ecg.modelo.RegistroECG;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * Componente central del visor. Los eventos se traducen en cambios sobre {@link ConfiguracionVista}
 * (a través de los gestores) y a continuación se repinta.
 *
 * Ratón: rueda = zoom horizontal en el cursor; Ctrl+rueda = zoom vertical; arrastrar = desplazar;
 * Mayús+arrastrar = seleccionar intervalo; doble clic = seleccionar anotación cercana.
 * Teclado: ←/→ desplazar, +/− zoom horizontal, Inicio restablecer, Esc limpiar selección.
 */
public class PanelECGInteractivo extends PanelECG {

    private final GestoresVista gestores;
    private Consumer<GestorCursor.InfoCursor> oyenteCursor = i -> { };
    private Runnable oyenteCambio = () -> { };

    private Point puntoArrastre;
    private double inicioVentanaArrastre;
    private double pxPorSegundoArrastre;
    private boolean seleccionando;

    public PanelECGInteractivo(RegistroECG registro, ConfiguracionVista configuracion, GestoresVista gestores) {
        super(registro, configuracion);
        this.gestores = gestores;
        setFocusable(true);
        instalarRaton();
        instalarTeclado();
    }

    public void setOyenteCursor(Consumer<GestorCursor.InfoCursor> o) { this.oyenteCursor = o; }

    /** Se invoca cuando una interacción del panel cambia la configuración (para refrescar otros controles). */
    public void setOyenteCambio(Runnable r) { this.oyenteCambio = r; }

    private void cambio() { repaint(); oyenteCambio.run(); }

    private Rectangle primeraCelda() {
        List<Rectangle> celdas = renderizador.calcularCeldas(registro, configuracion, getSize());
        return celdas.isEmpty() ? null : celdas.get(0);
    }

    private double tiempoEn(Point p) {
        Rectangle r = primeraCelda();
        return r == null ? Double.NaN : RenderizadorECG.xATiempo(configuracion, r, p.x);
    }

    private void instalarRaton() {
        MouseAdapter m = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (e.getButton() != MouseEvent.BUTTON1) return;
                Rectangle r = primeraCelda();
                if (r == null) return;
                if (e.isShiftDown()) {
                    seleccionando = true;
                    double t = tiempoEn(e.getPoint());
                    gestores.intervalo.establecerInicio(t);
                    gestores.intervalo.establecerFin(t);
                } else {
                    seleccionando = false;
                    puntoArrastre = e.getPoint();
                    inicioVentanaArrastre = configuracion.getTiempoInicio();
                    pxPorSegundoArrastre = RenderizadorECG.pixelesPorSegundo(configuracion, r);
                }
            }

            @Override public void mouseDragged(MouseEvent e) {
                if (seleccionando) {
                    gestores.intervalo.establecerFin(tiempoEn(e.getPoint()));
                } else if (puntoArrastre != null) {
                    double dt = -(e.getX() - puntoArrastre.x) / pxPorSegundoArrastre;
                    gestores.desplazamiento.desplazarA(inicioVentanaArrastre + dt);
                }
                actualizarCursor(e.getPoint());
                cambio();
            }

            @Override public void mouseReleased(MouseEvent e) { puntoArrastre = null; seleccionando = false; }

            @Override public void mouseMoved(MouseEvent e) { actualizarCursor(e.getPoint()); cambio(); }

            @Override public void mouseExited(MouseEvent e) {
                gestores.cursor.ocultar();
                oyenteCursor.accept(null);
                cambio();
            }

            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    double t = tiempoEn(e.getPoint());
                    double tol = 0.15 * configuracion.getDuracionVentana() / 10.0 + 0.05;
                    if (!Double.isNaN(t)) gestores.anotaciones.seleccionarMasCercana(t, tol);
                    cambio();
                }
            }

            @Override public void mouseWheelMoved(MouseWheelEvent e) {
                double factor = e.getPreciseWheelRotation() < 0 ? 1.25 : 0.8;
                if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
                    gestores.zoom.zoomVertical(factor);
                } else {
                    double t = tiempoEn(e.getPoint());
                    if (Double.isNaN(t)) gestores.zoom.zoomHorizontal(factor);
                    else gestores.zoom.zoomHorizontal(factor, t);
                }
                actualizarCursor(e.getPoint());
                cambio();
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
        addMouseWheelListener(m);
    }

    private void actualizarCursor(Point p) {
        oyenteCursor.accept(gestores.cursor.actualizar(p, getSize()));
    }

    private void instalarTeclado() {
        accion("izq", KeyEvent.VK_LEFT, () -> gestores.desplazamiento.desplazarAtras());
        accion("der", KeyEvent.VK_RIGHT, () -> gestores.desplazamiento.desplazarAdelante());
        accion("mas", KeyEvent.VK_ADD, () -> gestores.zoom.zoomHorizontal(1.25));
        accion("mas2", KeyEvent.VK_EQUALS, () -> gestores.zoom.zoomHorizontal(1.25));
        accion("menos", KeyEvent.VK_SUBTRACT, () -> gestores.zoom.zoomHorizontal(0.8));
        accion("menos2", KeyEvent.VK_MINUS, () -> gestores.zoom.zoomHorizontal(0.8));
        accion("inicio", KeyEvent.VK_HOME, () -> gestores.zoom.restablecerZoom());
        accion("esc", KeyEvent.VK_ESCAPE, () -> { gestores.intervalo.limpiarSeleccion();
            gestores.anotaciones.seleccionarAnotacion(null); });
    }

    private void accion(String nombre, int tecla, Runnable r) {
        getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(tecla, 0), nombre);
        getActionMap().put(nombre, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { r.run(); cambio(); }
        });
    }
}
