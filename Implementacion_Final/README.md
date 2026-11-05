# ECG Viewer — ECG de 12 derivaciones, visor interactivo e informe de rendimiento

Implementación en Java de la arquitectura propuesta: lectura de registros WFDB (`.hea` + `.dat`),
visor interactivo Swing, exportación PNG e informe PDF de rendimiento (PDFBox).

## Requisitos
Java 25+ y Maven. Dependencias: Apache PDFBox 3.0.x (PDF) y JUnit 5 (pruebas).

## Uso rápido
```bash
mvn package                                   # compila, prueba y crea el JAR ejecutable
java -jar target/ProyectoECG-1.0.0-jar-with-dependencies.jar --demo   # registro sintético de 12 derivaciones
java -jar ...jar data/PTBXL/00001/registro.hea                        # abrir un registro real
```
Sin argumentos se abre la aplicación y se usa *Archivo ▸ Abrir registro ECG…*.

Modos sin interfaz gráfica:
```bash
... --generar-demo [dir]                         # crea sintetico12.hea/.dat/.atr
... --exportar registro.hea salida.png [ini [dur]]   # ECG estándar 3×4, 25 mm/s, 10 mm/mV
... --evaluar registro.hea 5 informe.pdf         # 5 repeticiones medidas + informe PDF
```

## Controles del visor
| Acción | Efecto |
|---|---|
| Rueda | Zoom horizontal centrado en el cursor |
| Ctrl + rueda | Zoom vertical (mm/mV) |
| Arrastrar | Desplazar en el tiempo |
| Mayús + arrastrar | Seleccionar un intervalo |
| Doble clic | Seleccionar la anotación más cercana |
| ← / → / + / − / Inicio / Esc | Desplazar, zoom, restablecer, limpiar selección |

## Estructura (igual a la arquitectura)
`modelo` · `lectura` · `procesamiento` · `visualizacion` · `exportacion` · `rendimiento` · `interfaz` · `utilidades`
(`src/main/java/ecg/...`), pruebas en `src/test/java/ecg/...`.

Reglas de diseño que se cumplen:
* El número de canales sale de la cabecera (2 en MIT-BIH, 12 en PTB-XL); no hay arrays ni casillas fijas de 12.
* Cada canal conserva su archivo, formato, ganancia y baseline; se convierte con `V = (D − baseline)/ganancia`
  (única implementación: `lectura.ConversorUnidades`). Sin ganancia declarada el registro se rechaza.
* Pantalla y exportaciones dibujan siempre con `RenderizadorECG` y comparten `ConfiguracionVista`
  (una prueba compara píxel a píxel la vista exportada con la de pantalla).
* El zoom/desplazamiento solo cambian la configuración, nunca las muestras.

## Formatos y límites conocidos
* Formatos WFDB soportados: **8, 16, 212, 24, 32**. Otros se rechazan con un mensaje claro.
* **No** soportados (se detectan y se rechazan): registros multisegmento, varias muestras por frame (`x`N), *skew* ≠ 0.
* Si la cabecera no declara el nº de muestras, se deduce del tamaño del `.dat`.
* Se comprueba el *checksum* de cada señal; si no coincide se muestra una advertencia (no bloquea).
* Anotaciones de referencia: se lee `<registro>.atr` / `.ann` (formato MIT) si existe.
* El detector es Pan-Tompkins con media móvil, sin búsqueda hacia atrás de latidos perdidos.

## Rendimiento: qué mide y qué no
* **Tiempo**: de pared (`nanoTime`), no de CPU.
* **Memoria**: heap usado (`totalMemory − freeMemory`), máximo muestreado cada 5 ms (cota inferior del pico real);
  RSS solo en Linux (`/proc/self/status`). Se informan como magnitudes distintas.
* **CPU**: portable = CPU del hilo (`ThreadMXBean`) y CPU total del proceso (`OperatingSystemMXBean`);
  usuario/sistema del proceso solo en Linux (`/proc/self/stat`, asumiendo CLK_TCK = 100). En otros SO se
  muestran como «n/d»; nunca se calculan por resta de fuentes distintas.
* Las operaciones anidadas se registran con su `profundidad` y no se suman dos veces en las métricas globales.
* El informe se genera con el botón *Finalizar ejecución y generar informe de rendimiento*
  (*Rendimiento ▸ Consultar rendimiento…*). El coste del propio PDF queda en la ejecución siguiente.

## Notas de verificación
* 31 pruebas JUnit (lectura 16/212/8/24, cabeceras, conversión, anotaciones, detección, fusión, geometría,
  zoom, exportación, medidores y PDF) pasan con JDK 25.
* Se verificó el flujo completo con registros sintéticos de 12 derivaciones (formatos 16 y 212) y la GUI bajo Xvfb.
* **Sin probar con datos reales de PhysioNet** (no había acceso de red): conviene validar con un registro de PTB-XL
  y otro de MIT-BIH. `mvn` tampoco pudo ejecutarse allí (sin acceso a Maven Central); la compilación y las pruebas se
  hicieron con `javac` y la consola de JUnit. El código PDF está escrito para PDFBox **3.x**; se probó con 2.0.29
  cambiando solo la creación de las fuentes (líneas marcadas `// PDFBOX3`).
* Los datos de `data/` no se incluyen: descargue PTB-XL y/o MIT-BIH de PhysioNet.
