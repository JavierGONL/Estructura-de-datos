import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Programa principal: lee un comando por línea y se lo pasa a Sistema.
 *   java -cp out Main                  -> interactivo (muestra "> ")
 *   java -cp out Main < casoA.txt      -> lee los comandos de un archivo
 *   java -cp out Main -e < casoA.txt   -> igual, pero repite cada comando ("> CMD") antes de su respuesta
 * Se ignoran las líneas vacías y las que empiezan con #. SALIR termina el programa.
 */
public class Main {

    /**
     * Qué hace: ciclo de lectura de comandos.
     * Cómo: crea un Sistema nuevo (estructuras vacías) y un lector de la entrada estándar. En cada vuelta
     * lee una línea; si es null (se acabó el archivo) o SALIR, termina; si está vacía o es comentario,
     * la salta; si no, la ejecuta con Sistema.ejecutar e imprime la respuesta. Con -e imprime antes el
     * comando para que la salida quede legible cuando los comandos vienen de un archivo.
     */
    public static void main(String[] args) throws IOException {
        boolean eco = false;
        if (args.length > 0 && args[0].equals("-e")) {
            eco = true;
        }

        boolean interactivo = !eco && System.console() != null;
        Sistema sistema = new Sistema();
        BufferedReader lector = new BufferedReader(new InputStreamReader(System.in));

        if (interactivo) {
            System.out.println("Comandos: REGISTRAR id nombre prioridad padre | CONSULTAR id | SIGUIENTE");
            System.out.println("          CANCELAR id | REPORTE | ESTADISTICAS | SALIR");
        }

        while (true) {
            if (interactivo) {
                System.out.print("> ");
                System.out.flush();
            }

            String linea = lector.readLine();
            if (linea == null) {
                break;
            }

            linea = linea.trim();
            if (linea.isEmpty() || linea.startsWith("#")) {
                continue;
            }
            if (linea.equalsIgnoreCase("SALIR")) {
                break;
            }
            if (eco) {
                System.out.println("> " + linea);
            }

            System.out.println(sistema.ejecutar(linea));
        }
    }
}
