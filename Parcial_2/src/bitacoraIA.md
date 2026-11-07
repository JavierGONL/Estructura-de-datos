# Bitacora de IA - Examen 02

**Herramienta:** Claude (claude.ai).

## Tres solicitudes realizadas

1. "Con el enunciado del examen, que tipos de implementaciones podriamos hacer?" (alternativas para cada
   estructura: lista de hijos vs primer-hijo/siguiente-hermano, sucesor vs predecesor al eliminar,
   reconstruir el heap vs borrado perezoso al cancelar).
2. "Revisa mis clases (Registro, Nodo, ArbolGeneral, Max_heap_R, DynamicArray) y dime que errores de logica hay"
3. "Tengo que usar DynamicArray en la cola: corrigela sin cambiar eso." Despues se reviso la version que
   escribimos nosotros (Queue) con la misma herramienta.

## Respuestas que se verificaron o corrigieron

**Heap.** La revision de `Max_heap_R` encontro que `siftup` no subia los registros de mayor prioridad
(un else colgado) y que `buildheap` dejaba sin arreglar la raiz. Se comprobo ejecutando el caso B (salia el
30 antes que el 40) y el caso F (la raiz era 4 y no 10); despues de corregir, ambos dan lo esperado.

**Cola del recorrido por niveles.** La IA propuso primero una cola circular con arreglo propio, lo que
incumplia el requisito de usar `DynamicArray`; se pidio rehacerla. Al final el equipo escribio su propia
`Queue`: una cola circular (head, tail, size) que usa un `DynamicArray` como almacenamiento con metodos de
acceso fisico. Se verifico comparandola con una cola de referencia en 3000 sesiones aleatorias de `add` y
`poll`, incluyendo crecer cuando la cola esta dada la vuelta, y midiendo la memoria: con 1000 elementos vivos
y un millon de vueltas retiene 1024 casillas (la primera version con solo un indice `head` retenia 1.001.000).
Ademas `PruebasEstructuras` pasa 71 de 71 comprobaciones y los casos A a F coinciden con el enunciado.
