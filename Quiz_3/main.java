public class main {
    public static void main(String[] args) {
        snake s = new snake(10, 5);

        for (int i = 0; i < 10; i++) {
            System.out.println("Turno " + (i + 1));
            s.actualizarPosiciones(s.LeerInput());
            s.actualizarCuadricula();

            for (int fila = 0; fila < s.cuadriculaSize; fila++) {
                for (int col = 0; col < s.cuadriculaSize; col++) {
                    System.out.print(s.cuadricula[fila][col] == null ? ". " : s.cuadricula[fila][col] + " ");
                }
                System.out.println();
            }
            System.out.println();
        }
    }
}
