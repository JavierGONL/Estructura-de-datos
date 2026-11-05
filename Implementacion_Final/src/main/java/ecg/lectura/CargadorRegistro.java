package ecg.lectura;

import ecg.modelo.AnotacionECG;
import ecg.modelo.CabeceraECG;
import ecg.modelo.CanalECG;
import ecg.modelo.RegistroECG;
import ecg.rendimiento.MonitorRendimiento;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Punto de entrada para cargar un registro:
 * .hea → LectorCabecera → ValidadorArchivoWFDB → LectorSenalWFDB → ConversorUnidades → RegistroECG.
 * Si hay un archivo de anotaciones de referencia (.atr) junto a la cabecera, también se carga.
 */
public class CargadorRegistro {

    private final LectorCabecera lectorCabecera = new LectorCabecera();
    private final ValidadorArchivoWFDB validador = new ValidadorArchivoWFDB();
    private final LectorSenalWFDB lectorSenal = new LectorSenalWFDB();
    private final LectorAnotacionesWFDB lectorAnotaciones = new LectorAnotacionesWFDB();
    private MonitorRendimiento monitor;

    /** Si se indica, cada fase de la carga se mide por separado. */
    public void setMonitor(MonitorRendimiento monitor) { this.monitor = monitor; }

    public CabeceraECG leerSoloCabecera(Path archivoHea) throws IOException {
        return lectorCabecera.leer(archivoHea);
    }

    public ValidadorArchivoWFDB.Informe validar(Path archivoHea) throws IOException {
        return validador.validar(lectorCabecera.leer(archivoHea), directorioDe(archivoHea));
    }

    public RegistroECG cargarRegistro(Path archivoHea) throws IOException {
        Path dir = directorioDe(archivoHea);
        CabeceraECG cab = medir("Leer cabecera", () -> lectorCabecera.leer(archivoHea));

        ValidadorArchivoWFDB.Informe informe = validador.validar(cab, dir);
        if (!informe.esValido()) {
            throw new LecturaECGException("El registro no es válido:\n" + informe.resumen());
        }

        List<CanalECG> canales = medir("Decodificar señales", () -> lectorSenal.leerSenales(dir, cab));
        RegistroECG registro = new RegistroECG(cab.getNombreRegistro(), cab, canales);
        registro.setOrigen(archivoHea);
        registro.agregarAdvertencias(informe.getAdvertencias());
        registro.agregarAdvertencias(lectorSenal.getAdvertencias());

        cargarAnotacionesDeReferencia(archivoHea, registro);
        return registro;
    }

    private void cargarAnotacionesDeReferencia(Path hea, RegistroECG registro) {
        String base = nombreBase(hea);
        for (String ext : new String[]{".atr", ".ann"}) {
            Path f = directorioDe(hea).resolve(base + ext);
            if (Files.isRegularFile(f)) {
                try {
                    List<AnotacionECG> a = medir("Leer anotaciones", () -> lectorAnotaciones.leer(f));
                    registro.agregarAnotaciones(new ArrayList<>(a));
                } catch (IOException e) {
                    registro.agregarAdvertencias(List.of("No se pudieron leer las anotaciones " + f.getFileName()
                            + ": " + e.getMessage()));
                }
                return;
            }
        }
    }

    private <T> T medir(String operacion, Callable<T> accion) throws IOException {
        try {
            return monitor == null ? accion.call() : monitor.medir(operacion, accion);
        } catch (IOException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static Path directorioDe(Path archivo) {
        Path p = archivo.toAbsolutePath().getParent();
        return p == null ? Path.of(".") : p;
    }

    public static String nombreBase(Path archivo) {
        String n = archivo.getFileName().toString();
        int p = n.lastIndexOf('.');
        return p > 0 ? n.substring(0, p) : n;
    }
}
