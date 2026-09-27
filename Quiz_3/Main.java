public class Main {
    public static void main(String[] args) {
        
        Position[] frutas = {
            new Position(5, 8), // N2
            new Position(7, 7)  // N5
        };

        // Instancia directa con tu constructor existente
        Snake s = new Snake(10, frutas);

        Position[] movimientos = {
            new Position(0, 1),  // N1: Derecha (no come)
            new Position(0, 1),  // N2: Derecha (come fruta)
            new Position(1, 0),  // N3: Abajo (no come)
            new Position(1, 0),  // N4: Abajo (no come)
            new Position(0, -1), // N5: Izquierda (come fruta)
            new Position(0, -1)  // N6: Izquierda (no come)
        };

        String[] movs = {"N1", "N2", "N3", "N4", "N5", "N6"};
        String[] acciones = {"no come", "come", "no come", "no come", "come", "no come"};

        System.out.println("Mov. | Acción    | data                               | tail | size | cap. | ¿Resize?");
        System.out.println("----------------------------------------------------------------------------------------");

        // Impresión directa del estado inicial
        System.out.print("---  | inicial  | [");
        for (int j = 0; j < s.capacity(); j++) {
            if (s.data[j] == null) {
                System.out.print("null");
            } else if (s.data[j].x == 5 && s.data[j].y == 5) {
                System.out.print("A");
            } else if (s.data[j].x == 5 && s.data[j].y == 6) {
                System.out.print("B");
            }
            if (j < s.capacity() - 1) System.out.print(", ");
        }
        System.out.println("] | " + s.posTail + "    | " + s.size() + "    | " + s.capacity() + "    | No");

        // Ciclo de movimientos llamando a tu método actualizarPosiciones
        for (int i = 0; i < movimientos.length; i++) {
            int capAntes = s.capacity();

            // Ejecuta tu función
            s.actualizarPosiciones(movimientos[i]);

            boolean resize = (s.capacity() != capAntes);

            String acc = acciones[i];
            String espaciado = acc.equals("come") ? "    | [" : " | [";
            System.out.print(movs[i] + "   | " + acc + espaciado);

            // Mapeo directo de coordenadas a letras A-H
            for (int j = 0; j < s.capacity(); j++) {
                if (s.data[j] == null) {
                    System.out.print("null");
                } else {
                    int x = s.data[j].x;
                    int y = s.data[j].y;
                    if (x == 5 && y == 5) System.out.print("A");
                    else if (x == 5 && y == 6) System.out.print("B");
                    else if (x == 5 && y == 7) System.out.print("C");
                    else if (x == 5 && y == 8) System.out.print("D");
                    else if (x == 6 && y == 8) System.out.print("E");
                    else if (x == 7 && y == 8) System.out.print("F");
                    else if (x == 7 && y == 7) System.out.print("G");
                    else if (x == 7 && y == 6) System.out.print("H");
                    else System.out.print("(" + x + "," + y + ")");
                }
                if (j < s.capacity() - 1) System.out.print(", ");
            }
            System.out.println("] | " + s.posTail + "    | " + s.size() + "    | " + s.capacity() + "    | " + (resize ? "Sí" : "No"));
        }
    }
}