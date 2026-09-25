package Varias;
import java.util.Random;

public class conway_GameOfLife {
    int[][] matriz;
    int generation;
    int maxGeneration;
    int len;
    Random x;

    public conway_GameOfLife(int maxGen, int len) {
        this.matriz = new int[len + 2][len + 2];
        this.generation = 0;
        this.maxGeneration = maxGen;
        this.len = len;
        this.x = new Random();

        for (int i = 1; i < len - 1; i++) {
            for (int j = 1; j < len - 1; j++) {
                // las siguientes 2 lineas la saque de chatgpt, no supe usar la libreria :c
                x = new Random();
                matriz[i][j] = x.nextInt(2);

                System.out.print(((matriz[i][j] == 1) ? "[x]" : "[ ]"));
            }
            System.out.println();
        }
    }

    public void NextGen() {
        if (generation >= maxGeneration)
            return;

        for (int i = 1; i < len - 1; i++) {
            for (int j = 1; j < len - 1; j++) {
                int nVecinos = 0;
                if (matriz[i - 1][j] == 1)
                    nVecinos++;
                if (matriz[i][j - 1] == 1)
                    nVecinos++;
                if (matriz[i - 1][j - 1] == 1)
                    nVecinos++;
                if (matriz[i + 1][j] == 1)
                    nVecinos++;
                if (matriz[i][j + 1] == 1)
                    nVecinos++;
                if (matriz[i + 1][j + 1] == 1)
                    nVecinos++;
                if (matriz[i + 1][j - 1] == 1)
                    nVecinos++;
                if (matriz[i - 1][j + 1] == 1)
                    nVecinos++;

                if (nVecinos < 2) {
                    matriz[i][j] = 0;
                } else if (nVecinos >= 2 && nVecinos <= 3) {
                    matriz[i][j] = 1;
                } else if (nVecinos > 3) {
                    matriz[i][j] = 0;
                }
            }
        }

        generation++;
    }

    public void dibujar() {
        System.out.println();
        System.out.println();
        for (int i = 1; i < len - 1; i++) {
            for (int j = 1; j < len - 1; j++) {
                System.out.print(((matriz[i][j] == 1) ? "[x]" : "[ ]"));
            }
            System.out.println();
        }
    }
}