# Bitacora de IA - Examen 02

**Herramienta:** Claude (claude.ai).

## solicitudes realizadas y resultado

1. "Revisa mis clases (Registro, Nodo, ArbolGeneral, Max_heap_R, DynamicArray) y dime que errores de logica hay"
   -> Errores en las cinco clases (detalle en la seccion siguiente).
2. "quiero usar DynamicArray como una estructura circular para la queue puedes hacer el cambio pls"
   -> `Queue` circular (`head`, `tail`, `size`) que usa el `DynamicArray` como almacenamiento. La version anterior sacaba con `removeIdx(0)` (O(n) por `poll`, `levelOrder` O(n^2)).
3. "quiero que revises esta logica esta rara?"
   -> La eliminacion del BST con sucesor inorden y el avance circular de la cola (`(head + 1) % capacidad`) eran correctos.
4. "segun el pdf que metodos me faltan por implementar?"
   -> Faltaban BST, AVL (rotaciones LL, RR, LR, RL y balance), `Sistema` con las seis operaciones, `Main` y las pruebas de los casos A a F.
5. "que te parece esta implementacion de avl y bst? si ves algo raro corrigelo"
   -> Sin errores de logica: el BST elimina con el sucesor inorden y el AVL guarda la altura en cada nodo, aplica LL, RR, LR y RL y rebalancea al insertar y al eliminar (`>= 0` y `<= 0` para elegir entre rotacion simple y doble).
6. "revisa"
   -> Sin errores de logica en las estructuras; observaciones: `len` queda en 0 en el `DynamicArray` de la cola, `eliminarPrimero()` sin uso y comentarios faltantes.
7. "ayudame a implementar un archivo de pruebas, y de paso corrige errores de logica si los ves"
   -> `PruebasEstructuras`: casos D, E y F, preguntas del Anexo B, sesion del Anexo A y pruebas aleatorias. Con ellas se vieron los fallos del heap en los casos B y F.
8. "arregla los errores de logica del max_heap_R"
   -> Corregido el heap (detalle en la seccion siguiente).
9. "comenta de manera consiza los archivos, explicar el como y porque estan, y de paso genera una tabla con la complejidad O(n) de cada estructura relevante"
   -> Comentarios "que hace / como" en cada funcion (se corrigio el de `levelOrder`, que describia varias colas en vez de una) y tabla de complejidades en el informe.
10. "revisa sistema y main si ves algo mal corrigelo y comentalo los 2 archivos"
   -> Reviso solo `Sistema` y `Main` (los demas archivos no se tocaron). Agrego validaciones y casos borde que no habia considerado (detalle en la seccion siguiente) y comento esos dos archivos con el formato "que hace / como".

## Respuestas que se verificaron o corrigieron

La IA corrigio estos errores y cada correccion se verifico ejecutando pruebas:

- **Heap (solicitudes 1 y 8).** Corrigio `siftup` (`else` colgado), `buildheap` (omitia la raiz y usaba el largo del arreglo), `siftDown` (sin desempate entre hijos), `heapSort` (modificaba su entrada), `reconstruccion()` (copiaba `i-1` sin cambiar `size`) y `busqueda` (por prioridad en vez de id). Verificacion: antes el caso B sacaba el 30 antes que el 40 y el caso F daba raiz 4 en vez de 10; despues ambos dan lo esperado, igual que las trazas del heap del Anexo A y 500 registros aleatorios contra Heap-Sort.
- **DynamicArray (solicitud 1).** Corrigio `add` (`capacity - 1`), `remove` y `removeIdx` (`array[len--]`) y `set` (`index > len`), y agrego `get`. Verificacion: todas las estructuras y las pruebas lo usan y pasan.
- **Registro, Nodo y ArbolGeneral (solicitud 1).** Hizo publico el enum `Estado`, cambio las letras "P", "A", "C" por metodos con nombre, dio altura al nodo binario, renombro el nodo interno a `NodoGeneral` y rehizo `ArbolGeneral` (insercion con padre, sin `inOrder`, `cancelarSubarbol`). Verificacion: la sesion completa del Anexo A (recorridos, altura 2, 5 hojas, `cancelarSubarbol(20)` = 1).
- **Cola (solicitud 2).** Su primera version usaba un arreglo propio y no `DynamicArray`, lo que incumplia el requisito; se pidio rehacerla. Verificacion: 3000 sesiones aleatorias de `add` y `poll` contra una cola de referencia y medicion de memoria: con 1000 elementos vivos y un millon de vueltas retiene 1024 casillas, frente a 1.001.000 de una version que solo avanzaba `head`.
- **BST y AVL (solicitudes 4 y 5).** Implemento BST y AVL (LL, RR, LR, RL; `>= 0` al eliminar) y reviso la version del equipo sin encontrar errores de logica. Verificacion: en las solicitudes 4 y 5 la IA los probo con pruebas internas propias, que no forman parte del proyecto. El archivo de pruebas llego recien en la solicitud 7 (`PruebasEstructuras`) y con el se comprobaron los casos D y E (alturas 6 y 2 con 1..7, eliminar el 4, AVL balanceado) y 4000 operaciones aleatorias contra un oraculo simple con el balance comprobado en cada paso.
- **Pruebas (solicitud 7).** Implemento `PruebasEstructuras`: 71 de 71 comprobaciones, incluidas las preguntas del Anexo B y la sesion del Anexo A.
- **Sistema y Main (solicitud 10, solo estos dos archivos).** La IA agrego casos borde que no habia considerado:
  - REGISTRAR: faltaban tres casos borde, que la IA agrego: id repetido, padre negativo y segunda raiz. Ahora valida todo antes de insertar, para que un error no deje las cuatro estructuras a medias.
  - `ejecutar` responde con un mensaje de error, sin cambiar nada, cuando el comando no existe, faltan argumentos o un numero no es valido (`NumberFormatException`).


  Verificacion: `Main` comparado con una implementacion de referencia en 300 sesiones aleatorias, sin diferencias; ademas `PruebasEstructuras` reproduce la sesion del Anexo A (duplicado, segunda raiz, padre inexistente, prioridad invalida, id inexistente en CONSULTAR y CANCELAR, SIGUIENTE sin pendientes) y las preguntas P4 a P7 del Anexo B.