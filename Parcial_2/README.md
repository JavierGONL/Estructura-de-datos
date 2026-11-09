# Entrega parcial #2 estructuras de datos grupo 4

## Integrantes:
- Kevin Javier Gonzalez Luna
- Iván Felipe Maluche Suarez
- Angélica Pascagaza Vega


## Instrucciones compilacion
### Version utilizada: Java 26
### Instrucciones: 
Para correr adecuadamente el programa se debe realizar el siguiente conjunto de comandos en la terminal dentro de la carpeta Parcial_2:
```console
rm -rf out
mkdir out
javac -d out Main.java src/*.java
java -cp out Main.java
```
Esto se hace para borrar el contenido la carpeta donde se guardaran las clases, crearla si no existe, crear las clases a partir de los archivos .java de la carpeta src y correr el main.
### Entorno e interaccion con el sistema
Al ingresar los comandos en la terminal sera posible escribir una serie de instrucciones para realizar los comandos de registro, consulta, procesamiento, cancelacion, envio de reporte, estadisticas del sistema y una opcion de salida. Todas estas opciones son ingresadas por terminal.
### Ejemplo de uso:
Se quiere añadir una orden para que un robot ensamble una horquilla para un apilador electrico, para esto debe primero identificar los componentes, rotar la base y acoplar el pivote. Cada una de estas ordenes se deben registrar y procesar de la siguiente manera:
```console
REGISTRAR 1 identificar 1 horquilla
REGISTRAR 2 construir 1 horquilla
REGISTRAR 3 rotar 2 construir
REGISTRAR 4 Acoplar 3 construir
```