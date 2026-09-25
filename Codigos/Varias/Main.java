package Varias;
public class Main {
    public static void main(String[] args) {
        int maxGen = 10;
        int len = 10;
        conway_GameOfLife conway = new conway_GameOfLife(maxGen, len);

        for (int i = 0; i < maxGen; i++) {
            conway.NextGen();
            conway.dibujar();
        }
    }
}
