package ecg;

import ecg.exportacion.ImpresionEstandar;
import ecg.interfaz.ControladorECG;
import ecg.interfaz.VentanaPrincipal;
import ecg.interfaz.VentanaVisorECG;
import ecg.lectura.CargadorRegistro;
import ecg.modelo.RegistroECG;
import ecg.rendimiento.EvaluacionExperimental;
import ecg.rendimiento.GeneradorInformePDF;
import ecg.rendimiento.ResultadoRendimiento;
import ecg.utilidades.GeneradorRegistroSintetico;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.file.Path;

/**
 * Punto de entrada.
 * <pre>
 *   (sin argumentos)                       abre la aplicación
 *   registro.hea                           abre la aplicación con ese registro en el visor
 *   --demo                                 genera un registro sintético de 12 derivaciones y lo abre
 *   --generar-demo [directorio]            solo genera el registro sintético
 *   --exportar registro.hea salida.png [inicio_s [duracion_s]]    ECG estándar sin interfaz gráfica
 *   --evaluar registro.hea repeticiones informe.pdf               evaluación experimental + informe
 * </pre>
 */
public final class Main {

    private Main() { }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) { iniciarInterfaz(null); return; }
        switch (args[0]) {
            case "--ayuda": case "-h": case "--help":
                System.out.println("Uso: ver comentario de la clase ecg.Main (README.md)");
                break;
            case "--generar-demo": {
                Path hea = generarDemo(args.length > 1 ? Path.of(args[1]) : Path.of("data/Sintetico"));
                System.out.println("Registro creado: " + hea);
                break;
            }
            case "--demo":
                iniciarInterfaz(generarDemo(Path.of("data/Sintetico")));
                break;
            case "--exportar":
                exportar(args);
                break;
            case "--evaluar":
                evaluar(args);
                break;
            default:
                iniciarInterfaz(Path.of(args[0]));
        }
    }

    private static Path generarDemo(Path directorio) throws Exception {
        return GeneradorRegistroSintetico.generar(directorio, "sintetico12", 12, 500, 60, 212);
    }

    private static void exportar(String[] a) throws Exception {
        if (a.length < 3) { System.err.println("Uso: --exportar registro.hea salida.png [inicio [duracion]]"); System.exit(2); }
        System.setProperty("java.awt.headless", "true");
        RegistroECG r = new CargadorRegistro().cargarRegistro(Path.of(a[1]));
        double ini = a.length > 3 ? Double.parseDouble(a[3]) : 0;
        double dur = a.length > 4 ? Double.parseDouble(a[4]) : ImpresionEstandar.DURACION_PREDETERMINADA_S;
        ImpresionEstandar imp = new ImpresionEstandar();
        imp.guardar(imp.generar(r, ini, dur), Path.of(a[2]));
        System.out.printf("%s: %d derivaciones, %.1f s → %s%n", r.getNombreRegistro(), r.getNumeroCanales(),
                r.getDuracionSegundos(), a[2]);
    }

    private static void evaluar(String[] a) throws Exception {
        if (a.length < 4) { System.err.println("Uso: --evaluar registro.hea repeticiones informe.pdf"); System.exit(2); }
        System.setProperty("java.awt.headless", "true");
        ResultadoRendimiento r = new EvaluacionExperimental().ejecutar(Path.of(a[1]), Integer.parseInt(a[2]), true);
        new GeneradorInformePDF().generarInforme(r, Path.of(a[3]));
        r.resumirPorOperacion().forEach(s -> System.out.printf("%-22s n=%d  t medio=%.2f ms  heap máx=%.1f MiB%n",
                s.operacion, s.repeticiones, s.tiempoMedioNs / 1e6, s.heapMaximoBytes / 1048576.0));
        System.out.println("Informe: " + a[3]);
    }

    private static void iniciarInterfaz(Path hea) throws Exception {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignorada) { /* por defecto */ }
        ControladorECG controlador = new ControladorECG();
        if (hea != null) controlador.cargarRegistro(hea);
        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal v = new VentanaPrincipal(controlador);
            v.setVisible(true);
            if (controlador.hayRegistro()) new VentanaVisorECG(controlador).setVisible(true);
        });
    }
}
