# `ecg.ml` — Random Forest para clasificar latidos ECG

Clasificador propio en Java, **sin librerías de machine learning**, que asigna a cada latido una de las
4 clases AAMI (N, S, V, F). Está pensado para **leerse y defenderse**: usa entropía + corte por la media
y estructuras de datos propias, con el mínimo de piezas necesarias para entender el algoritmo completo.

Todo el aprendizaje (árbol + bosque) vive en este paquete. Las piezas de soporte son `Datos` (lee los
registros MIT-BIH con el lector WFDB de la aplicación y arma los datasets DS1/DS2), `Caracteristicas`
(convierte cada latido en su vector de 257 valores) y `Metricas` (mide los resultados).

---

## Idea en una frase

Un **Random Forest** es un conjunto de **árboles de decisión**; cada árbol aprende, a base de preguntas
"¿la característica *j* es menor que un umbral?", a separar los latidos por clase. Para clasificar un
latido nuevo se pregunta a todos los árboles y se promedia su voto.

```
           +-----------------------------------------------+
  latido → |  árbol 1   árbol 2   árbol 3  ...   árbol N    |
           |    ↓         ↓         ↓             ↓         |
           |  dist1     dist2     dist3   ...   distN       |   dist = [pN, pS, pV, pF]
           +-----------------------------------------------+
                                ↓  (promedio)
                       distribución final → clase de mayor probabilidad
```

---

## Los archivos

| Archivo | Qué es | Responsabilidad |
|---|---|---|
| `Caracteristicas.java` | utilidad | Convierte un latido (señal + posición del pico R) en su vector de 257 valores (252 de forma + 5 de ritmo RR). |
| `Datos.java` | utilidad | Lee los registros WFDB (.hea/.dat/.atr), extrae los latidos y arma los datasets DS1/DS2. |
| `Muestra.java` | `record` | Un latido: su vector de 257 características + su clase real (0=N, 1=S, 2=V, 3=F). |
| `Entropia.java` | utilidad | Mide el "desorden" de clases de un grupo. Sirve para elegir el mejor corte. |
| `Muestreo.java` | utilidad | El azar del bosque: bootstrap (filas) y selección de características por nodo. |
| `ArbolDesicion.java` | clase | Un árbol de decisión: se entrena y predice una distribución de clases. |
| `BosqueSimple.java` | clase | El bosque: entrena N árboles y promedia sus predicciones. |
| `Metricas.java` | utilidad | Matriz de confusión, accuracy y F1 macro a partir de las predicciones. |
| `EjemploSimple.java` | `main` | Programa que entrena con DS1, evalúa con DS2 e imprime las métricas. |

---

## Las características de un latido (`Caracteristicas`)

Cada latido se describe con **257 números**:

* **[0..251] forma del latido (252 valores).** Ventana de 252 muestras del canal MLII alrededor del pico
  R (90 antes y 162 desde el pico), a la que se le resta su mediana para quitar el nivel base.
* **[252..256] ritmo (5 valores).** Intervalos RR: `rr_prev`, `rr_next`, `rr_media` (promedio de los
  últimos 10 RR), y las razones `rr_prev/rr_media` y `rr_next/rr_media`. Capturan latidos que llegan
  antes o después de lo normal.

`Datos` recorta esa ventana para cada latido anotado en el `.atr` y empareja el vector con su clase real.

---

## Cómo aprende un árbol (`ArbolDesicion`)

Un árbol se construye de forma **recursiva**, partiendo el grupo de muestras en dos una y otra vez:

1. **¿Parar?** Si al grupo le quedan pocas muestras (`< minMuestrasHoja`) o todas son de la misma clase,
   se crea una **hoja** que guarda la proporción de clases del grupo (su "distribución").
2. **Elegir características al azar.** De las 257, se toman solo unas pocas (por defecto √257 ≈ 16). Esto
   es parte del azar que diferencia a los árboles.
3. **Elegir el mejor corte.** Para cada característica candidata se prueba a cortar por su **media** y se
   mide la **ganancia de información** (cuánta entropía se reduce). Gana la que más reduce el desorden.
4. **Partir y repetir.** Las muestras con `valor < umbral` van a la izquierda y el resto a la derecha; se
   repiten los pasos 1-4 en cada lado.

**Predecir** es bajar por el árbol siguiendo las preguntas (`izquierda si valor < umbral`) hasta llegar a
una hoja, y devolver su distribución de clases.

### ¿Qué es la entropía y por qué sirve? (`Entropia`)

La entropía mide cuán mezcladas están las clases en un grupo:

- Todas iguales (p. ej. 100% N) → entropía **0** (grupo "puro", no hay nada que decidir).
- Muy repartidas (25% de cada clase) → entropía **alta** (mucho desorden).

Fórmula (entropía de Shannon, en bits): `H = - Σ p_c · log2(p_c)`, con `p_c` = proporción de la clase *c*.

La **ganancia de información** de un corte es la entropía del grupo padre menos la media ponderada de las
entropías de los dos hijos. El árbol se queda con el corte de **mayor ganancia**: el que deja los dos
lados lo más puros posible.

---

## Cómo funciona el bosque (`BosqueSimple`)

Entrena **N árboles independientes**. Cada árbol recibe un **bootstrap**: `n` filas sorteadas al azar
**con repetición** de las `n` originales (unas se repiten, otras se omiten). Esto, junto con la selección
aleatoria de características en cada nodo, hace que **ningún árbol sea igual a otro**.

Para clasificar, el bosque promedia las distribuciones de todos los árboles (**voto blando**) y devuelve
la clase con mayor probabilidad media.

**¿Por qué muchos árboles ganan a uno solo?** Un árbol profundo memoriza el entrenamiento (sobreajuste).
Pero si cada árbol se equivoca en cosas distintas, al promediar muchos esos errores se cancelan y el
resultado generaliza mejor y es más estable. Es la idea del *bagging* (Breiman, 2001).

---

## Las dos fuentes de azar (`Muestreo`)

| Método | Qué hace | Para qué |
|---|---|---|
| `bootstrap(n, rng)` | `n` índices al azar **con** repetición | Datos distintos para cada árbol. |
| `muestraSinReemplazo(total, k, rng)` | `k` índices distintos (Fisher-Yates parcial) | Preguntas distintas en cada nodo. |

Ambos usan un único `java.util.Random` con **semilla**: misma semilla → mismo bosque (reproducible).

---

## Estructuras de datos propias (`ecg.ml.estructuras`)

El clasificador se apoya en estructuras implementadas a mano, sin usar las colecciones de `java.util`
para el núcleo del problema:

| Estructura | Dónde se usa |
|---|---|
| `DynamicArray` | Acumular latidos de un registro sin conocer de antemano cuántos hay (`Datos`). |
| `CircularQueue` + `MovingMean` | Media de los últimos 10 intervalos RR al extraer características. |
| `TreeNode` | Análisis del árbol de decisión (altura, nº de nodos y hojas). |
| `MinHeap` + `topK` | Selección de los *k* mejores elementos sin ordenar todo. |

---

## Cómo ejecutarlo

Necesita las clases de `ecg.ml` ya compiladas. Desde la raíz del proyecto:

```bash
# compilar el paquete ml (y sus estructuras)
javac --release 25 -cp target/classes -d target/classes $(find src/main/java/ecg/ml -name '*.java')

# entrenar con DS1 y evaluar con DS2 (usa ml/dataset/ds1.txt y ds2.txt si existen;
# si no, los construye leyendo los registros MIT-BIH de data/)
java -Xmx2g -cp target/classes ecg.ml.EjemploSimple --arboles 25 --min-hoja 20
```

Opciones: `--arboles N` (nº de árboles), `--min-hoja L` (tamaño mínimo de hoja), `--semilla S`,
`--dataset dir`.

Las clases raras S y F son difíciles (hay muy pocos latidos de ellas); N y V se predicen bien.

---

## Referencias

- Breiman, L. (2001). *Random Forests*. Machine Learning, 45(1), 5-32.
- Quinlan, J. R. (1986). *Induction of Decision Trees*. Machine Learning, 1(1), 81-106.
- de Chazal, P., O'Dwyer, M., Reilly, R. B. (2004). *Automatic Classification of Heartbeats Using ECG
  Morphology and Heartbeat Interval Features*. IEEE Trans. Biomedical Engineering, 51(7), 1196-1206.
