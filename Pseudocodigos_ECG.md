# Pseudocódigos del proyecto ECG Viewer

**Convención:** `←` asignación · `//` comentario · `SI / SINO / PARA / MIENTRAS / RETORNAR` · `LANZAR error` · `[ ]` listas.
Cada subsección corresponde a un archivo (o a un grupo de archivos triviales) del proyecto Java.

## 1. Paquete modelo

Clases de datos sin lógica de negocio, salvo lo indicado.

### RegistroECG

```
CLASE RegistroECG
  ATRIBUTOS nombre, cabecera, canales[], anotaciones[], advertencias[]

  numeroMuestras()  ← canales[0].numeroMuestras
  duracion()        ← numeroMuestras / cabecera.frecuenciaMuestreo

  reemplazarAnotaciones(fuente, nuevas):
      ELIMINAR de anotaciones todas las de esa fuente
      AGREGAR nuevas
      ORDENAR anotaciones por indiceMuestra
```

### CanalECG, CabeceraECG, MetadatosCanal, IntervaloTemporal, ResultadoDeteccion, MetricasEjecucion

```
CLASES de solo datos con getters y setters
  CanalECG:        nombre, muestras[] (en mV), metadatos, unidades
  CabeceraECG:     nombre, numeroCanales, fs, numeroMuestras, fecha, hora, metadatos[], comentarios[]
  MetadatosCanal:  archivo, formato, muestrasPorFrame, skew, offsetBytes, ganancia, baseline,
                   adcZero, unidades, descripcion, resolucion, valorInicial, checksum
  MetricasEjecucion: memoria (inicial, final, maxima, reservada, RSS), CPU (usuario, sistema,
                   total, hilo), tiempoPared, profundidad;  valor -1 = NO_DISPONIBLE

IntervaloTemporal(a, b):
    inicio ← MIN(a, b);  fin ← MAX(a, b)
    muestraInicial(fs) ← PISO(inicio * fs);  muestraFinal(fs) ← TECHO(fin * fs)

ResultadoDeteccion(indices[], fs, algoritmo, canal):
    tiempos[i] ← indices[i] / fs
```

### AnotacionECG

```
esLatido():
    SI tipo tiene más de un carácter  →  RETORNAR verdadero      // p. ej. «QRS» del algoritmo
    RETORNAR tipo ∈ {N L R a V F J A S E j / Q e n x f r}         // símbolos WFDB de latido

tiempo(fs) ← indiceMuestra / fs
```

### ConfiguracionVista

```
paraRegistro(registro):
    canalesVisibles ← [0 .. registro.numeroCanales - 1]
    ventana ← [0, MIN(10 s, duracion)]
    escalaHorizontal ← 25 mm/s;  escalaVertical ← 10 mm/mV
    distribucion ← SI numeroCanales <= 3 ENTONCES VERTICAL SINO CUADRICULA_3X4

copiar():  RETORNAR copia profunda de todos los campos (la lista de canales se duplica)
```

## 2. Paquete lectura

### LectorCabecera

```
leer(ruta) → CabeceraECG
    lineas ← leer archivo .hea (ISO-8859-1)
    generalLeida ← falso;  canales ← []
    PARA cada linea (sin espacios en los extremos):
        SI vacía → continuar
        SI empieza con '#' → guardar como comentario; continuar
        SI NO generalLeida:
            leerDatosGenerales(linea);  generalLeida ← verdadero
        SINO SI canales.tamaño < numeroCanales:
            m ← leerMetadatosCanal(linea);  m.fs ← cabecera.fs;  canales.agregar(m)
    SI canales.tamaño ≠ numeroCanales → LANZAR «señales declaradas ≠ líneas de señal»

leerDatosGenerales(linea):
    t ← dividir linea por espacios       // nombre nsig [fs] [nmuestras] [hora] [fecha]
    SI nombre contiene '/' → LANZAR «multisegmento no soportado»
    numeroCanales ← entero(t[1]);  SI < 1 → LANZAR
    fs ← SI existe t[2] ENTONCES número antes de '/' o '(' SINO 250   // valor por defecto WFDB
    numeroMuestras ← SI existe t[3] ENTONCES entero SINO 0

leerMetadatosCanal(linea):
    t ← dividir linea por espacios;  SI tamaño < 2 → LANZAR
    archivo ← t[0]
    t[1] con patrón  formato[xN][:skew][+offset]  → formato, muestrasPorFrame, skew, offset
    SI existe t[2]:  patrón  ganancia[(baseline)][/unidades]
        ganancia ← valor o 0 (= no declarada);  baseline explícito ← SI aparece el paréntesis
        unidades ← valor o «mV»
    resolucion ← t[3] o valor por defecto según formato
    adcZero ← t[4] o 0
    SI no hubo baseline explícito → baseline ← adcZero
    valorInicial ← t[5];  checksum ← t[6];  descripcion ← unir t[8..]
```

### DecodificadorWFDB (interfaz) y DecodificadorBase

```
INTERFAZ DecodificadorWFDB
    decodificar(archivo, canales[], frames) → matriz [canal][frame]
    contarFrames(archivo, canales[]) → frames completos en el archivo

CLASE ABSTRACTA DecodificadorBase
    decodificar(archivo, canales, frames):
        bytes ← leer archivo completo;  offset ← canales[0].offsetBytes
        SI (bytes.tamaño - offset) < bytesNecesarios(frames, nCanales) → LANZAR «archivo truncado»
        RETORNAR decodificarBytes(bytes, offset, canales, frames)      // lo define cada formato
    contarFrames(...) ← framesEnBytes(tamañoArchivo - offset, nCanales)
```

### DecodificadorFormato16

```
bytesNecesarios ← frames * nCanales * 2
decodificarBytes:
    p ← offset
    PARA t DE 0 A frames-1:
        PARA c DE 0 A nCanales-1:
            r[c][t] ← entero16_con_signo( byte[p] | byte[p+1] << 8 )      // little-endian
            p ← p + 2
```

### DecodificadorFormato212

```
total ← frames * nCanales                       // muestras en orden intercalado
bytesNecesarios ← (total DIV 2) * 3  +  (SI total impar ENTONCES 2 SINO 0)
decodificarBytes:
    k ← 0                                       // índice global de muestra
    PARA t, c en orden intercalado:
        b ← offset + (k DIV 2) * 3
        SI k es par:   v ← byte[b]   | (byte[b+1] Y 0x0F) << 8
        SINO:          v ← byte[b+2] | (byte[b+1] Y 0xF0) << 4
        SI v Y 0x800  →  v ← v - 4096          // extensión de signo de 12 bits
        r[c][t] ← v;  k ← k + 1
```

### DecodificadorFormato8

```
acumulado[c] ← valorInicial del canal c
PARA t, c:
    acumulado[c] ← acumulado[c] + byte_con_signo(p)      // diferencias sucesivas
    r[c][t] ← acumulado[c];  p ← p + 1
```

### DecodificadorFormatoEntero (24 y 32 bits)

```
PARA t, c:
    v ← byte[p] | byte[p+1] << 8 | byte[p+2] << 16
    SI 24 bits:  v ← extender_signo_24(v)
    SINO:        v ← v | byte[p+3] << 24
    r[c][t] ← v;  p ← p + bytesPorMuestra
```

### ConversorUnidades (única implementación de la conversión)

```
convertirMuestra(D, m):
    SI m.ganancia <= 0 → LANZAR «ganancia no declarada»      // no se usan valores supuestos
    SI m.formato = 16 Y D = -32768 → RETORNAR NaN            // muestra inválida WFDB
    V ← (D - m.baseline) / m.ganancia
    factor ← SEGÚN unidades:  mV→1, uV→0.001, nV→1e-6, V→1000, otras→ninguno
    RETORNAR SI hay factor ENTONCES V * factor SINO V        // unidades no eléctricas no se convierten
```

### ValidadorArchivoWFDB

```
validar(cabecera, directorio) → Informe{errores[], advertencias[]}
    SI señales declaradas ≠ líneas → error;  SI fs <= 0 → error
    PARA cada canal i:
        SI formato no soportado → error
        SI muestrasPorFrame ≠ 1 → error;  SI skew ≠ 0 → error
        SI ganancia no declarada → error
        SI unidades no son de tensión → advertencia
        SI no existe el archivo de datos → error
    PARA cada par de canales que comparten archivo:
        SI formato u offset difieren → error
    SI no hay errores:
        SI numeroMuestras = 0 → advertencia «se deducirá del .dat»
        SINO PARA cada archivo: SI framesDisponibles < numeroMuestras → error
```

### LectorSenalWFDB

```
leerSenales(directorio, cabecera) → CanalECG[]
    grupos ← agrupar índices de canal por archivo, conservando el orden de la cabecera
    frames ← cabecera.numeroMuestras
    SI frames = 0 → frames ← MIN sobre archivos de contarFrames()
    PARA cada archivo del grupo:
        dec ← decodificador según formato
        datos ← dec.decodificar(ruta, canalesDelGrupo, frames)
        colocar datos[j] en la posición original de cada canal
    PARA cada canal i:
        comprobarChecksum(i)
        nombre ← descripcion  (o «Canal i+1» si está vacía)
        muestras ← ConversorUnidades.convertir(digital[i], meta)
        CREAR CanalECG;  liberar digital[i]

comprobarChecksum(i):
    suma ← Σ digital[i]   (enteros, desbordamiento permitido)
    SI ((suma - checksumCabecera) Y 0xFFFF) ≠ 0 → agregar advertencia
```

### LectorAnotacionesWFDB

```
leer(archivo) → AnotacionECG[]
    t ← 0;  i ← 0
    MIENTRAS quedan 2 bytes:
        palabra ← 16 bits little-endian en i;  i ← i + 2
        tipo ← palabra >> 10;  intervalo ← palabra Y 0x3FF
        SI tipo = 0 Y intervalo = 0 → SALIR               // fin del archivo
        SI tipo = 59 (SKIP):  leer dos palabras (alta, baja);  t ← t + (alta << 16 | baja);  continuar
        SI tipo ∈ {60, 61, 62}:  continuar                // NUM, SUB, CHN
        SI tipo = 63 (AUX):  i ← i + intervalo + (intervalo Y 1);  continuar
        t ← t + intervalo
        AGREGAR Anotacion(t, símbolo(tipo), fuente = REFERENCIA)
```

### CargadorRegistro

```
cargarRegistro(archivoHea):
    cabecera ← [medir «Leer cabecera»]  LectorCabecera.leer(archivoHea)
    informe ← ValidadorArchivoWFDB.validar(cabecera, directorio)
    SI informe tiene errores → LANZAR «registro no válido» + resumen
    canales ← [medir «Decodificar señales»]  LectorSenalWFDB.leerSenales(...)
    registro ← NUEVO RegistroECG(nombre, cabecera, canales)
    registro.advertencias ← informe.advertencias + advertencias del lector
    SI existe «nombre.atr» o «nombre.ann»:
        registro.anotaciones ← [medir «Leer anotaciones»] LectorAnotacionesWFDB.leer(...)
    RETORNAR registro
```

## 3. Paquete procesamiento

### DetectorPanTompkins

```
detectar(x[], fs) → índices[]
    SI x demasiado corta → RETORNAR []
    x ← reemplazar NaN por el último valor válido
    f ← x - mediaMovil(x, 0.20 s * fs)                 // quita línea base
    f ← mediaMovil(f, 0.025 s * fs)                    // suaviza ruido
    PARA i DE 2 A n-3:
        d[i] ← (2 f[i+2] + f[i+1] - f[i-1] - 2 f[i-2]) / 8     // derivada
        e[i] ← d[i]^2                                           // energía
    mw ← mediaMovil(e, 0.15 s * fs)                    // integración
    picos ← seleccionarPicos(mw, d, fs)
    PARA cada pico p:                                  // refinar a la R real
        buscar en [p - 0.08 s, p + 0.08 s] el índice con MAX |f|
        SI queda a menos de 0.2 s del anterior → conservar el de mayor |f|
    RETORNAR picos refinados

seleccionarPicos(mw, d, fs):
    spki ← 0.25 * MAX(mw en 2 s iniciales);  npki ← 0.5 * MEDIA(mw en 2 s iniciales)
    umbral ← npki + 0.25 (spki - npki);  ultimo ← -∞
    PARA cada máximo local i de mw:
        pend ← MAX |d| alrededor de i
        SI mw[i] > umbral:
            SI i - ultimo < 0.2 s:                    // periodo refractario
                SI mw[i] > mw[ultimo] → reemplazar el último pico
            SINO SI i - ultimo < 0.36 s Y pend < 0.5 * pendUltimo:
                npki ← 0.125 mw[i] + 0.875 npki       // onda T: se trata como ruido
            SINO:
                aceptar i;  spki ← 0.125 mw[i] + 0.875 spki;  ultimo ← i;  pendUltimo ← pend
        SINO:
            npki ← 0.125 mw[i] + 0.875 npki
        umbral ← npki + 0.25 (spki - npki)

mediaMovil(x, ventana):         // O(n) con sumas acumuladas
    acum[0] ← 0;  acum[i+1] ← acum[i] + x[i]
    PARA cada i:  a ← MAX(0, i - ventana/2);  b ← MIN(n-1, i + ventana/2)
                  r[i] ← (acum[b+1] - acum[a]) / (b - a + 1)
```

### AnalizadorRR y AnalizadorFrecuenciaCardiaca

```
calcularRR(indices[], fs):
    PARA i DE 0 A tamaño-2:  rr[i] ← (indices[i+1] - indices[i]) / fs

estadisticas(rr):
    media ← promedio(rr)
    sdnn  ← RAIZ( Σ (rr - media)^2 / (n - 1) )
    rmssd ← RAIZ( Σ (rr[i] - rr[i-1])^2 / (n - 1) )

instantanea(rr)[i] ← 60 / rr[i]
media(rr) ← 60 / promedio(rr)          // no es el promedio de FC instantáneas
```

### ValidadorDeteccion

```
validar(detecciones[], referencia[], fs, tolerancia):
    ORDENAR ambas;  tol ← tolerancia * fs;  i ← 0;  j ← 0;  TP ← FP ← FN ← 0
    MIENTRAS i < |detecciones| Y j < |referencia|:
        dif ← detecciones[i] - referencia[j]
        SI |dif| <= tol:  TP++;  acumular error |dif|/fs;  i++;  j++
        SINO SI dif < 0:  FP++;  i++          // detección sin referencia
        SINO:             FN++;  j++          // referencia sin detección
    FP ← FP + restantes de detecciones;  FN ← FN + restantes de referencia
    sensibilidad ← TP / (TP + FN);  precision ← TP / (TP + FP)
    F1 ← 2 * sens * prec / (sens + prec);  errorMedio ← acumulado / TP
```

### FusionDetecciones

```
fusionar(resultadosPorCanal[], ventana, fs, votosMinimos):
    candidatos ← todos los (indice, canal) de todos los canales, ORDENADOS por índice
    grupo ← []
    PARA cada candidato c:
        SI grupo no vacío Y c.indice - ultimo(grupo).indice > ventana * fs:
            cerrar(grupo);  grupo ← []
        grupo.agregar(c)
    cerrar(grupo)

cerrar(grupo):
    SI canalesDistintos(grupo) >= votosMinimos → salida.agregar( mediana del grupo )

votosPorMayoria(n) ← MAX(1, (n + 1) DIV 2)
```

### EvaluadorCanales

```
evaluar(registro, detector, tolerancia):
    referencia ← índices de latidos de referencia
    PARA cada canal i:
        det ← detector.detectar(canal.muestras, fs)
        val ← SI referencia no vacía ENTONCES validar(det, referencia, fs, tolerancia) SINO nulo
        resultados.agregar(i, nombre, det, val)
```

### AnalizadorECG

```
analizarRegistro(registro):
    PARA cada canal i:  porCanal[i] ← detector.detectar(muestras, fs)
    fusion ← FusionDetecciones.fusionar(porCanal, 0.10 s, fs, mayoría)
    registro.reemplazarAnotaciones(ALGORITMO, anotaciones «QRS» en cada índice de fusion)

analizarCanal(registro, c):  igual, pero solo con el canal c y sin fusión
```

## 4. Paquete visualizacion

### DistribucionCanales

```
filasYColumnas(n):
    CUADRICULA_3X4 → (TECHO(n / 3), 3)
    VERTICAL       → (n, 1)
    INDIVIDUAL     → (1, 1)
    AUTOMATICA     → SI n <= 3 ENTONCES (n, 1)
                     SINO cols ← TECHO(RAIZ(n));  (TECHO(n / cols), cols)

calcular(visibles, area) → rectángulos[]
    n ← SI INDIVIDUAL ENTONCES MIN(1, visibles) SINO visibles
    (filas, cols) ← filasYColumnas(n)
    PARA k DE 0 A n-1:
        f ← k DIV cols;  c ← k MOD cols                       // orden de lectura por filas
        x0 ← area.x + c * area.ancho DIV cols;   x1 ← area.x + (c+1) * area.ancho DIV cols
        y0 ← area.y + f * area.alto DIV filas;   y1 ← area.y + (f+1) * area.alto DIV filas
        celda[k] ← Rectángulo(x0, y0, x1 - x0, y1 - y0)       // el reparto entero evita huecos
```

### RenderizadorECG (motor de dibujo)

```
GEOMETRÍA (funciones estáticas compartidas con los gestores)
    pixelesPorSegundo(cfg, celda)  ← celda.ancho / (tiempoFin - tiempoInicio)
    pixelesPorMilivoltio(cfg)      ← escalaVertical(mm/mV) * pixelesPorMm
    xATiempo(x)        ← tiempoInicio + (x - celda.x) / pixelesPorSegundo
    yAMilivoltios(y)   ← (celda.y + celda.alto/2 - y) / pixelesPorMilivoltio

canalesEfectivos(registro, cfg):
    canales ← canalesVisibles sin repetidos y con índice válido, ORDENADOS
    RETORNAR primeros N, con N según la distribución (INDIVIDUAL → 1)

dibujar(g, registro, cfg, dimension):
    activar antialiasing;  rellenar fondo blanco
    SI cfg.mostrarEncabezado → dibujarEncabezado()      // nombre, fs, ventana, escalas
    canales ← canalesEfectivos;  celdas ← distribución.calcular(canales.tamaño, área)
    SI canales vacío → escribir «Ninguna derivación seleccionada»;  RETORNAR
    PARA k DE 0 A canales.tamaño-1:
        gc ← copia de g recortada a celdas[k]
        cuadrícula.dibujar(gc, celda, cfg)
        cursor.dibujarSeleccion(gc, celda, cfg)         // intervalo sombreado
        senal.dibujar(gc, canal, celda, cfg)
        anotaciones.dibujar(gc, registro, celda, cfg)
        cursor.dibujarCursor(gc, celda, cfg, canal, esCanalDelCursor)
        SI cfg.mostrarNombres → escribir canal.nombre en la esquina
        dibujar borde de la celda
```

### RenderizadorCuadricula

```
dibujar(g, celda, cfg):
    pps ← pixelesPorSegundo;  ppmv ← pixelesPorMilivoltio;  yc ← celda.y + celda.alto / 2
    SI cfg.mostrarCuadricula:
        líneasVerticales(paso 0.04 s, color suave, solo si 0.04 * pps >= 3 px)
        líneasHorizontales(paso 0.1 mV, color suave, solo si 0.1 * ppmv >= 3 px)
        líneasVerticales(paso 0.2 s, color fuerte, solo si >= 6 px)
        líneasHorizontales(paso 0.5 mV, color fuerte, solo si >= 6 px)
    dibujar línea discontinua de referencia en yc (0 mV)
    paso ← primer valor de [0.2, 0.5, 1, 2, 5, 10, 30, 60 ...] con paso * pps >= 70 px
    PARA cada múltiplo k * paso dentro de [tiempoInicio, tiempoFin]:  marca y texto «k s»

líneasVerticales(paso, ...):
    PARA k DE TECHO(t0 / paso) A PISO(t1 / paso):
        x ← celda.x + (k * paso - t0) * pps;  línea de arriba a abajo
líneasHorizontales(paso, ...):
    PARA k DE -kMax A kMax:  y ← yc - k * paso * ppmv;  SI y dentro de la celda → línea
```

### RenderizadorSenal

```
dibujar(g, canal, celda, cfg):
    i0 ← MAX(0, PISO(t0 * fs) - 1);  i1 ← MIN(n-1, TECHO(t1 * fs) + 1)
    SI i1 <= i0 → RETORNAR
    SI (i1 - i0) > 2 * celda.ancho:  envolvente()           // demasiadas muestras por píxel
    SINO:                            polilinea()

polilinea():
    lapizAbajo ← falso
    PARA i DE i0 A i1:
        SI x[i] es NaN → lapizAbajo ← falso;  continuar     // la muestra inválida abre un hueco
        px ← celda.x + (i / fs - t0) * pps;  py ← limitar(yc - x[i] * ppmv)
        SI lapizAbajo → lineTo(px, py) SINO moveTo(px, py);  lapizAbajo ← verdadero

envolvente():
    PARA cada columna de píxeles px:
        a ← PISO((t0 + px / pps) * fs);  b ← PISO((t0 + (px+1) / pps) * fs)
        min, max ← extremos de x[a..b] ignorando NaN
        SI hay datos → línea vertical en px entre yc - max*ppmv y yc - min*ppmv
```

### RenderizadorAnotaciones

```
dibujar(g, registro, celda, cfg):
    SI no se muestran anotaciones ni detecciones → RETORNAR
    PARA cada anotación a con indice dentro de [t0 * fs, t1 * fs]:
        esAlgoritmo ← (a.fuente = ALGORITMO)
        SI esAlgoritmo Y NO cfg.mostrarDetecciones → continuar
        SI NO esAlgoritmo Y NO cfg.mostrarAnotaciones → continuar
        color ← SEGÚN fuente: REFERENCIA azul, MANUAL verde, ALGORITMO rojo (línea discontinua)
        x ← celda.x + (indice / fs - t0) * pps
        dibujar línea vertical, triángulo superior (más gruesa si está seleccionada)
        SI pps >= 40 O seleccionada → escribir a.tipo
```

### RenderizadorCursor

```
dibujarSeleccion(g, celda, cfg):
    SI hay intervalo: rellenar de xa = x(inicio) a xb = x(fin) con color translúcido; bordes

dibujarCursor(g, celda, cfg, canal, esCeldaDelCursor):
    SI cursor oculto o cursorTiempo fuera de la ventana → RETORNAR
    línea vertical discontinua en x(cursorTiempo)
    SI esCeldaDelCursor:
        i ← REDONDEAR(cursorTiempo * fs);  v ← canal.muestra[i]        // SI no es NaN
        círculo en (x, yc - v * ppmv);  etiqueta «t s  v mV» sin salirse de la celda
```

### PanelECG y PanelECGInteractivo

```
PanelECG.paintComponent(g):  renderizador.dibujar(g, registro, configuracion, tamaño del panel)

PanelECGInteractivo (extiende PanelECG)
    mousePressed(e):
        SI botón izquierdo Y hay celdas:
            SI Mayús:  seleccionando ← verdadero;  intervalo.inicio ← intervalo.fin ← tiempoEn(e)
            SINO:      puntoArrastre ← e;  inicioVentana ← t0;  pxPorSegundo ← pps de la 1ª celda
    mouseDragged(e):
        SI seleccionando → intervalo.fin ← tiempoEn(e)
        SINO SI hay arrastre → desplazarA(inicioVentana - (e.x - puntoArrastre.x) / pxPorSegundo)
        actualizar cursor;  repintar y avisar del cambio
    mouseMoved(e):  actualizar cursor (GestorCursor);  repintar
    mouseExited():  ocultar cursor
    doble clic:     seleccionarMasCercana(tiempoEn(e), tolerancia)
    rueda:
        factor ← SI giro hacia arriba ENTONCES 1.25 SINO 0.8
        SI Ctrl → zoomVertical(factor)
        SINO zoomHorizontal(factor, tiempoEn(e))        // mantiene fijo el instante bajo el ratón
    teclas:  ← → desplazar · + − zoom · Inicio restablecer · Esc limpiar selección
```

### GestorZoom

```
zoomHorizontal(factor, tCentro):
    ventana ← t1 - t0;  minima ← MIN(duracion, MAX(0.2 s, 10 / fs))
    nueva ← LIMITAR(ventana / factor, minima, duracion)
    rel ← LIMITAR((tCentro - t0) / ventana, 0, 1)         // posición relativa del punto fijo
    fijarVentana(tCentro - rel * nueva, nueva)

fijarVentana(inicio, dur):
    dur ← MIN(dur, duracionRegistro);  inicio ← LIMITAR(inicio, 0, duracionRegistro - dur)
    t0 ← inicio;  t1 ← inicio + dur

zoomVertical(factor):  escalaVertical ← LIMITAR(escalaVertical * factor, 0.5, 400)
restablecerZoom():     ventana ← [0, MIN(10, duracion)];  escalaVertical ← 10
// ninguna operación modifica las muestras
```

### GestorDesplazamiento, GestorSeleccionCanales, GestorIntervalo, GestorAnotaciones

```
desplazarA(t):   ventana ← t1 - t0;  t0 ← LIMITAR(t, 0, duracion - ventana);  t1 ← t0 + ventana
desplazarAdelante() ← desplazarA(t0 + ventana / 2);   desplazarAtras() ← desplazarA(t0 - ventana / 2)

seleccionarCanal(i):    SI i válido Y no está → agregar y ORDENAR
deseleccionarCanal(i):  quitar i
seleccionarTodos():     canalesVisibles ← [0 .. n-1];   limpiarSeleccion(): lista vacía

establecerInicio(t) / establecerFin(t):
    guardar valor;  intervalo ← SI ambos existen ENTONCES IntervaloTemporal(inicio, fin) SINO nulo
limpiarSeleccion():  inicio ← fin ← NaN;  intervalo ← nulo

seleccionarMasCercana(tiempo, tolerancia):
    mejor ← nulo
    PARA cada anotación VISIBLE según los indicadores:
        SI |a.tiempo - tiempo| <= mejorDistancia → mejor ← a
    cfg.anotacionSeleccionada ← mejor
```

### GestorCursor

```
actualizar(punto, dimension) → InfoCursor o nulo
    PARA k, celda en celdas:
        SI celda contiene punto:
            t ← xATiempo(punto.x);  muestra ← LIMITAR(REDONDEAR(t * fs), 0, n-1)
            cfg.cursorTiempo ← muestra / fs;  cfg.cursorCanal ← canal k
            RETORNAR (tiempo, muestra, nombre del canal, amplitud en mV de esa muestra)
    RETORNAR nulo        // el ratón no está sobre ninguna derivación
```

## 5. Paquete exportacion

### ExportadorPNG y ExportadorECG (base)

```
ExportadorPNG.exportar(imagen, destino):
    crear directorios padre;  SI ImageIO no puede escribir PNG → LANZAR error

ExportadorECG.renderizar(registro, cfg, ancho, alto):
    SI ancho * alto > 120 millones de píxeles → LANZAR «imagen demasiado grande»
    imagen ← BufferedImage(ancho, alto);  RenderizadorECG.dibujar(imagen, registro, cfg, tamaño)
    RETORNAR imagen                                  // mismo motor que la pantalla

limpiaDeInspeccion(cfg) ← copia SIN cursor, intervalo ni anotación seleccionada
```

### ImpresionEstandar

```
crearConfiguracion(registro, inicio, duracion):
    cfg ← paraRegistro(registro);  todos los canales visibles
    dur ← LIMITAR(duracion, 0.1, duracionRegistro);  ini ← LIMITAR(inicio, 0, duracionRegistro - dur)
    ventana ← [ini, ini + dur];  escalas 25 mm/s y 10 mm/mV;  5 píxeles por mm
    cuadrícula, anotaciones y encabezado activos;  cursor oculto

calcularDimension(registro, cfg):
    (filas, cols) ← filasYColumnas(nCanales)
    anchoCelda ← REDONDEAR(duracionVentana * 25 * pxPorMm)      // 1 mm = 0.04 s exacto
    altoCelda  ← REDONDEAR(40 mm * pxPorMm)                       // ±2 mV a 10 mm/mV
    ancho ← cols * anchoCelda + 2 * margen
    alto  ← filas * altoCelda + 2 * margen + altoEncabezado

generar(registro, inicio, duracion) ← renderizar(registro, cfg, calcularDimension(...))
```

### ImpresionPersonalizada

```
generar(registro, vista, opciones):
    cfg ← limpiaDeInspeccion(vista)                 // la vista original no se modifica
    aplicar de opciones: cuadrícula, anotaciones/detecciones, encabezado (metadatos), escalas
    SI ancho <= 0 O alto <= 0:
        calcular tamaño automático a partir de escalas, ventana y número de canales
    RETORNAR renderizar(registro, cfg, ancho, alto)
```

### ExportadorVistaActual

```
generar(registro, cfgActual, ancho, alto) ← renderizar(registro, COPIA de cfgActual, ancho, alto)
exportar(...) ← guardar(generar(...), destino)       // incluye canales, zoom, intervalo y marcas
```

### ExportadorRegistroCompleto

```
exportar(registro, inicio, fin, segundosPorPagina, directorio):
    hasta ← SI fin <= 0 O fin > duracion ENTONCES duracion SINO fin
    paso ← MAX(0.5, segundosPorPagina);  paginas ← TECHO((hasta - inicio) / paso)
    PARA p DE 0 A paginas-1:
        t0 ← inicio + p * paso;  t1 ← MIN(hasta, t0 + paso)
        cfg ← ImpresionEstandar.crearConfiguracion(registro, t0, paso);  ventana ← [t0, t1]
        imagen ← renderizar(registro, cfg, calcularDimension(...))
        guardar(imagen, directorio / nombreSegmento + «_pNNN.png»)
    RETORNAR rutas creadas
```

### ConfiguracionExportacion y SelectorArchivoExportacion

```
ConfiguracionExportacion: ancho, alto, formato, cuadrícula, anotaciones, metadatos, escalas, segundosPorPagina

elegirDestino(padre, directorioInicial, nombreSugerido):
    mostrar JFileChooser con filtro PNG y nombre sugerido
    SI el usuario cancela → VACÍO
    SI falta la extensión → agregar «.png»
    SI el archivo existe Y el usuario no confirma sobrescribir → VACÍO
    RETORNAR ruta
```

## 6. Paquete utilidades

```
GestorRutas:
    limpiar(s)               ← s sin caracteres fuera de [A-Za-z0-9._-]
    nombreEstandar(reg, n)   ← «ECG_Registro» + reg + «_» + n + «Derivaciones.png»
    nombreCanales(reg, [..]) ← «ECG_Registro» + reg + «_Canales» + nombres unidos con «_» + «.png»
    nombreSegmento(reg, a, b)← «ECG_Registro» + reg + «_Segmento_» + a + «s_» + b + «s.png»
    nombreInforme()          ← «informe_rendimiento_» + fechaHora + «.pdf»

GestorArchivos.asegurarDirectorioPadre(ruta) ← crear directorios del padre si faltan
ConversorTiempo: muestra↔segundos (÷ y × fs); ns→ms (÷ 1e6); formatoReloj(s) → «mm:ss.mmm»
FormateadorDatos: bytes → B / KiB / MiB / GiB (−1 → «no disponible»); ms(ns), mb(bytes) → texto o «n/d»
ConstantesECG: nombre de la aplicación, extensión .hea, directorios de salida, 25 mm/s, 10 mm/mV
```

### GeneradorRegistroSintetico (datos de prueba)

```
generar(directorio, nombre, canales, fs, segundos, formato 16 o 212):
    latidos ← instantes con RR ≈ 0.8 s + variación sinusoidal + ruido gaussiano
    PARA cada canal c:
        mv[i] ← deriva lenta (0.25 Hz) + ruido
        PARA cada latido b:  sumar ondas P, Q, R, S, T (gaussianas) * factorAmplitud[c]
        adu[c][i] ← REDONDEAR(mv[i] * 200)             // ganancia 200 ADU/mV
    .dat ← SI 16: pares de bytes little-endian intercalados
           SI 212: empaquetar de dos en dos muestras de 12 bits en 3 bytes
    .hea ← línea de registro + una línea por canal (formato, 200(0)/mV, resolución, checksum, nombre)
    .atr ← palabras (tipo N << 10 | intervalo); SKIP + 32 bits si el intervalo > 1023; palabra 0 final
```

## 7. Paquete rendimiento

### MedidorTiempo

```
iniciar():    inicio ← nanoTime()
finalizar():  duracion ← nanoTime() - inicio          // tiempo de pared, no de CPU
```

### MedidorMemoria

```
iniciar():
    inicial ← heapUsada();  maximoObservado ← inicial
    LANZAR hilo daemon:  MIENTRAS activo: maximoObservado ← MAX(maximoObservado, heapUsada());  dormir 5 ms
finalizar():
    activo ← falso;  esperar al hilo
    final ← heapUsada();  maximo ← MAX(maximoObservado, inicial, final)
    reservada ← totalMemory();  rss ← leerRSS()
heapUsada() ← totalMemory - freeMemory
leerRSS():  SI existe /proc/self/status → valor de «VmRSS» (kB) * 1024  SINO NO_DISPONIBLE
```

### MedidorCPU

```
iniciar():   guardar cpuHilo, cpuProceso, (usuario, sistema) del proceso
finalizar(): hilo, proceso, usuario, sistema ← MAX(0, final - inicial)   // NO_DISPONIBLE si falta alguno

cpuHilo()     ← ThreadMXBean.getCurrentThreadCpuTime()  si está soportado
cpuProceso()  ← OperatingSystemMXBean.getProcessCpuTime()  si está soportado
usuarioYSistema():
    SI no se puede leer /proc/self/stat → (NO_DISPONIBLE, NO_DISPONIBLE)
    resto ← texto después del último «)»;  campos ← dividir resto
    RETORNAR (campos[11] * 10 ms, campos[12] * 10 ms)      // utime, stime con CLK_TCK = 100
// usuario y sistema NUNCA se obtienen restando fuentes distintas
```

### MonitorRendimiento

```
iniciarMedicion(operacion):
    m ← NUEVA Medicion(operacion, profundidad = pila.tamaño);  APILAR m
    m.memoria.iniciar();  m.cpu.iniciar();  m.tiempo.iniciar()

finalizarMedicion():
    SI pila vacía → LANZAR error
    m ← DESAPILAR;  m.tiempo.finalizar();  m.cpu.finalizar();  m.memoria.finalizar()
    metricas ← copiar los valores de los tres medidores;  registro.agregar(metricas)

medir(operacion, accion):
    iniciarMedicion(operacion)
    INTENTAR  RETORNAR accion()
    FINALMENTE  finalizarMedicion()                  // también si la acción falla
```

### RegistroMetricas y ResultadoRendimiento

```
RegistroMetricas:
    agregar(m) ← actuales.agregar(m)
    instantanea(obs) ← ResultadoRendimiento con copia de las métricas actuales (no vacía)
    cerrarEjecucion(obs): r ← instantanea(obs);  historial.agregar(r);  actuales.vaciar();  RETORNAR r

ResultadoRendimiento:
    resumirPorOperacion():
        agrupar métricas por nombre (orden de aparición)
        PARA cada grupo: n, tiempo medio/mín/máx, CPU medias (solo valores disponibles),
                         heap máximo, RSS máximo
    tiempoTotalPrimerNivel() ← Σ tiempoPared de las métricas con profundidad = 0   // sin doble conteo
```

### GraficadorMetricas

```
barras(titulo, unidad, etiquetas[], series[]):
    techo ← valor «redondo» >= 1.05 * MAX(valores)
    dibujar 6 líneas de rejilla con su valor, ejes y título
    PARA cada operación i:
        PARA cada serie s:
            SI valor < 0 → escribir «n/d»
            SINO barra de altura alto * valor / techo, con el valor encima
        etiqueta de la operación girada 35°
    leyenda de series

graficoMemoria ← barras(heap máximo en MiB)
graficoCPU     ← barras(CPU usuario y sistema)  SI ninguna está disponible ENTONCES (CPU total del proceso)
graficoTiempo  ← barras(tiempo medio de pared en ms)
```

### GeneradorInformePDF

```
generarInforme(resultado, destino):
    resumen ← resultado.resumirPorOperacion();  crear documento y primera página A4
    agregarPortada();  agregarDatosRegistro();  agregarMetricasGenerales()
    agregarTablaOperaciones();  agregarGraficos();  agregarObservaciones()
    cerrar página (con pie «Página N»);  guardar en destino

agregarDatosRegistro():  viñetas: registro, canales, fs, muestras y duración, fecha, Java, SO, heap máximo
agregarMetricasGenerales():
    tabla: operaciones medidas, tiempo total (nivel 0), CPU usuario/sistema/total (suma de nivel 0,
           «no disponible» si no hay datos), heap máximo, RSS máximo
agregarTablaOperaciones():  por cada resumen: nombre, n, T medio/mín/máx, CPU usr/sis/tot, heap MiB
agregarGraficos():
    asegurar espacio para título + un gráfico (evita título huérfano)
    insertar los tres gráficos a todo el ancho de la página
agregarObservaciones():  texto del usuario + notas fijas (tiempo de pared ≠ CPU, heap ≠ RSS, etc.)

utilidades de maquetación:
    asegurarEspacio(alto): SI y - alto < margen → nuevaPagina()
    ajustar(texto, ancho): partir por palabras según el ancho real de la fuente
    limpiar(texto): sustituir caracteres no codificables en WinAnsi (guiones largos, «…», > 255)
    tabla(): cabecera sombreada; filas alternas; repetir cabecera al cambiar de página
```

### EvaluacionExperimental

```
ejecutar(archivoHea, repeticiones, descartarPrimera):
    PARA i DE 0 A repeticiones (+1 si descartarPrimera):
        SI i = 0 Y descartarPrimera:  cargar, analizar, renderizar SIN medir;  continuar   // calentamiento JIT
        registro ← [medir «Cargar registro»] cargar (con sus fases anidadas)
        [medir «Analizar QRS»]   analizador.analizarRegistro(registro)
        imagen ← [medir «Renderizar ECG»]  impresionEstandar.generar(registro, 0, 5)
        [medir «Exportar PNG»]   guardar imagen en archivo temporal
    borrar archivo temporal
    RETORNAR registroMetricas.cerrarEjecucion(«Evaluación experimental…»)
```

## 8. Paquete interfaz

### ControladorECG

```
cargarRegistro(hea):  registro ← [medir «Cargar registro»] cargador.cargarRegistro(hea);  establecerRegistro(registro)
establecerRegistro(r):
    cfg ← paraRegistro(r);  gestores ← NUEVOS GestoresVista(r, cfg);  registrar r en métricas;  notificarCambio()

notificarCambio():  ejecutar todos los oyentes EN el hilo de Swing (invokeLater si hace falta)

acciones de vista (seleccionarCanal, zoom, desplazar, distribución, marcas...):
    delegar en el gestor correspondiente (modifica SOLO la ConfiguracionVista);  notificarCambio()

exportarEstandar / exportarPersonalizada / exportarVistaActual / exportarRegistroCompleto:
    [medir «Exportar PNG»]  llamar al exportador correspondiente

finalizarYGenerarInforme(destino, observaciones):
    r ← registroMetricas.cerrarEjecucion(observaciones)
    [medir «Generar informe PDF»]  GeneradorInformePDF.generarInforme(r, destino)
    // el coste del propio PDF queda en la ejecución siguiente
```

### VentanaPrincipal

```
construir menú:
    Archivo:      Abrir registro · Salir
    Ver:          Visualizar ECG · Configuración de la vista        (necesitan registro)
    Exportar:     Imprimir estándar · Imprimir canales seleccionados · PNG de la vista actual
    Análisis:     Analizar registro (detección QRS)
    Rendimiento:  Consultar rendimiento / generar informe PDF
actualizarEstado():  habilitar o no los ítems según hayRegistro();  mostrar resumen del registro
visualizar():        [medir «Abrir visor»] crear y mostrar VentanaVisorECG
analizar():          SwingWorker → controlador.analizarRegistro(); mostrar nº de latidos
```

### VentanaCargaECG

```
examinar():   elegir .hea;  SI no tiene extensión .hea → aviso
mostrarInformacion(ruta):
    cabecera ← leerSoloCabecera;  informe ← validar
    mostrar nombre, canales, fs, muestras, duración, derivaciones, resultado de la validación
    habilitar «Cargar» SOLO SI informe.esValido()
cargar():     SwingWorker → controlador.cargarRegistro();  al terminar: mostrar avisos y cerrar; si falla → mostrar error
```

### VentanaVisorECG y paneles de control

```
VentanaVisorECG(controlador):
    panel ← PanelECGInteractivo(registro, cfg, gestores);  enlazar cursor → PanelInformacionCursor
    oeste ← PanelSeleccionCanales + PanelAnotaciones;  sur ← PanelControlTemporal + PanelControlZoom + exportar
    oyente ← refrescar();  registrar en el controlador;  quitarlo al cerrar la ventana
    refrescar():  repintar panel;  sincronizar casillas, campos de tiempo y marcas con cfg

PanelSeleccionCanales:
    PARA cada canal del registro (no un número fijo): crear casilla con su nombre
    casilla → alCambiarCanal(indice, marcada);  botones Todas / Ninguna;  selector de distribución
    sincronizar(visibles): marcar casillas SIN disparar eventos (bandera «actualizando»)

PanelControlTemporal:
    botones ◀ ▶ Inicio;  deslizador 0..1000  →  desplazarA((duracion - ventana) * valor / 1000)
    campos Inicio/Fin → SI fin > inicio ENTONCES controlador.establecerVentana(inicio, fin)
    refrescar(): actualizar campos y posición del deslizador sin disparar eventos

PanelControlZoom:        botones H+ H− V+ V− Restablecer → controlador.zoom*
PanelInformacionCursor:  mostrar(info) → texto «derivación, t, muestra, amplitud» o ayuda si info es nulo
PanelAnotaciones:        casillas (referencia, detecciones, cuadrícula, cursor);  «Detectar QRS» → SwingWorker
```

### VentanaImpresion, VentanaConfiguracionVista y VentanaRendimiento

```
VentanaImpresion:
    vistaLocal ← copia de la configuración (las derivaciones elegidas aquí no afectan al visor)
    exportar():
        SI estándar → nombre sugerido «ECG_Registro…_NDerivaciones.png»;  controlador.exportarEstandar(...)
        SINO: SI no hay derivaciones → aviso;  ventana, escalas, tamaño y opciones → exportarPersonalizada(...)
        ejecutar en SwingWorker;  mostrar ruta guardada o el error
    exportarPaginas():  elegir carpeta → exportarRegistroCompleto(carpeta, segundosPorPágina)

VentanaConfiguracionVista:
    mostrar casillas y selectores con los valores actuales
    «Aplicar» → copiar valores a la ConfiguracionVista;  notificarCambio();  cerrar

VentanaRendimiento:
    actualizar():  llenar la tabla con las métricas actuales (n/d si no disponibles) y el nº de ejecuciones cerradas
    generarInforme():
        SI no hay mediciones → aviso
        elegir destino .pdf;  SwingWorker → controlador.finalizarYGenerarInforme(destino, observaciones)
        al terminar: actualizar tabla y mostrar la ruta
```

## 9. Main

```
main(args):
    SI sin argumentos → iniciarInterfaz(nulo)
    SEGÚN args[0]:
        --generar-demo [dir]      → generar registro sintético de 12 derivaciones (formato 212, 60 s, 500 Hz)
        --demo                    → generar y abrir en la interfaz
        --exportar hea png [ini [dur]]
                                  → (sin pantalla) cargar y guardar la impresión estándar
        --evaluar hea n pdf       → (sin pantalla) EvaluacionExperimental + informe PDF
        otro                      → tratarlo como ruta .hea y abrirlo en la interfaz

iniciarInterfaz(hea):
    aplicar el aspecto del sistema;  controlador ← NUEVO ControladorECG
    SI hea no es nulo → controlador.cargarRegistro(hea)
    EN el hilo de Swing:  mostrar VentanaPrincipal;  SI hay registro → mostrar VentanaVisorECG
```
