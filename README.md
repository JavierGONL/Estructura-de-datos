# Entrega propuesta de solucion quiz 3 estructuras de datos grupo 4

## Integrantes:
- Kevin Javier Gonzalez Luna
- Iván Felipe Maluche Suarez
- Angélica Pascagaza Vega


## Instrucciones compilacion quiz 3
### Version utilizada: Java 26
### Instrucciones: 
Para ejecutar adecuadamente la solucion propuesta para el snake y la implementacion del arreglo dinamico es necesario compilar los archivos en formato .java usando el comando javac, posteriormente iniciandolo con el comando java en una terminal

```console
$ java --version
openjdk 26.0.2.1 2026-08-18
OpenJDK Runtime Environment (build 26.0.2.1)
OpenJDK 64-Bit Server VM (build 26.0.2.1, mixed mode, sharing)

$ javac -d out src/*.java
$ java -cp out Main

N1 -> no come
Tablero:
. . . . . . . . . . 
. . . . . . . . . . 
. . . . . . . . . . 
. . . . . . . . . . 
. . . . . . . . . . 
. . . . . . # # f . 
. . . . . . . . . . 
. . . . . . . f . . 
. . . . . . . . . . 
. . . . . . . . . . 
Después del movimiento: data = [null, B, N1, null]
posTail = 1
size = 2
capacity = 4
resize = Sí

N2 -> come
Tablero:
. . . . . . . . . . 
. . . . . . . . . . 
. . . . . . . . . . 
. . . . . . . . . . 
. . . . . . . . . . 
. . . . . . # # # . 
. . . . . . . . . . 
. . . . . . . f . . 
. . . . . . . . . . 
. . . . . . . . . . 
Después del movimiento: data = [null, B, N1, N2]
posTail = 1
size = 3
capacity = 4
resize = No

N3 -> no come
Tablero:
. . . . . . . . . . 
. . . . . . . . . . 
. . . . . . . . . . 
. . . . . . . . . . 
. . . . . . . . . . 
. . . . . . . # # . 
. . . . . . . . # . 
. . . . . . . f . . 
. . . . . . . . . . 
. . . . . . . . . . 
Después del movimiento: data = [N3, null, N1, N2]
posTail = 2
size = 3
capacity = 4
resize = No

N4 -> no come
Tablero:
. . . . . . . . . . 
. . . . . . . . . . 
. . . . . . . . . . 
. . . . . . . . . . 
. . . . . . . . . . 
. . . . . . . . # . 
. . . . . . . . # . 
. . . . . . . f # . 
. . . . . . . . . . 
. . . . . . . . . . 
Después del movimiento: data = [N3, N4, null, N2]
posTail = 3
size = 3
capacity = 4
resize = No

N5 -> come
Tablero:
. . . . . . . . . . 
. . . . . . . . . . 
. . . . . . . . . . 
. . . . . . . . . . 
. . . . . . . . . . 
. . . . . . . . # . 
. . . . . . . . # . 
. . . . . . . # # . 
. . . . . . . . . . 
. . . . . . . . . . 
Después del movimiento: data = [N3, N4, N5, N2]
posTail = 3
size = 4
capacity = 4
resize = No

N6 -> no come
Tablero:
. . . . . . . . . . 
. . . . . . . . . . 
. . . . . . . . . . 
. . . . . . . . . . 
. . . . . . . . . . 
. . . . . . . . . . 
. . . . . . . . # . 
. . . . . . # # # . 
. . . . . . . . . . 
. . . . . . . . . . 
Después del movimiento: data = [null, N3, N4, N5, N6, null, null, null]
posTail = 1
size = 4
capacity = 8
resize = Sí
```