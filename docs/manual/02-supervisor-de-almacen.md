# Manual del supervisor de almacén

**Público:** supervisores de preparadores de pedidos\
**Nivel:** operativo y de supervisión\
**Estado:** borrador inicial; pendiente de validación con un supervisor

## Su responsabilidad

El supervisor vigila que el trabajo avance de forma segura y ayuda cuando una
ubicación, un artículo, una cantidad o una terminal presentan un problema.

Su responsabilidad principal es decidir si el preparador puede continuar, debe
volver a comprobar el trabajo o necesita detenerse mientras una persona autorizada
investiga la tarea.

El supervisor no debe pedir que se confirme una cantidad incorrecta ni que se
repita físicamente una preparación sin comprobar primero su estado en el sistema.

## Herramientas disponibles

El sistema dispone de una pantalla de supervisión llamada tablero. Esta pantalla
permite consultar las tareas y se actualiza aproximadamente cada dos segundos.

El tablero es únicamente de consulta. Actualmente no contiene botones para:

- bloquear o reanudar una tarea;
- ajustar existencias;
- crear órdenes;
- crear artículos o ubicaciones.

Esas acciones requieren una cuenta con permiso `ADMIN` y una herramienta
administrativa aprobada. Si el supervisor no dispone de esa herramienta, debe
solicitar la acción al administrador del almacén o a soporte.

## 1. Abrir el tablero

1. Abra en el navegador la dirección del sistema proporcionada por el responsable
   de instalación y añada `/dashboard`.
2. Introduzca su usuario y clave de administrador.
3. Espere a que aparezca la tabla de tareas.
4. Compruebe que la hora de `Last refreshed` cambia cada pocos segundos.

`Last refreshed` significa “última actualización”.

**Resultado esperado:** puede ver las tareas y la hora continúa actualizándose sin
recargar la página manualmente.

![Tablero de tareas con una tarea atascada resaltada en rojo](../evidence/2026-07-14-final-acceptance-sweep/dashboard.png)

Si la hora no cambia:

1. Actualice la página una vez con el navegador.
2. Si continúa detenida, no tome decisiones basadas en esa pantalla.
3. Avise a soporte e indique la hora de la última actualización visible.

**No haga esto:** no comparta su clave ni deje abierta la sesión en un equipo de
uso público.

## 2. Entender las columnas

| Columna | Significado |
|---|---|
| `Task #` | Número único de la tarea |
| `State` | Paso actual de la tarea |
| `Order` | Orden a la que pertenece |
| `Line` | Renglón de la orden |
| `Location` | Ubicación donde debe recogerse el artículo |
| `Article` | Código del artículo |
| `Qty` | Cantidad solicitada |
| `User` | Preparador que tiene asignada la tarea |
| `Device` | Terminal utilizada por el preparador |
| `Last transition` | Fecha y hora del último avance registrado |
| `Stuck` | Indica que una tarea activa lleva demasiado tiempo sin avanzar |

El tablero muestra primero las tareas con cambios más recientes. Puede mostrar
como máximo 500 tareas, por lo que no sustituye un informe histórico.

## 3. Entender los estados

| Estado visible | Significado | Acción habitual |
|---|---|---|
| `AVAILABLE` | Disponible para ser tomada | Esperar a que el sistema la asigne |
| `ASSIGNED` | Asignada a un preparador y una terminal | Confirmar que el preparador comenzó el recorrido |
| `LOCATION_CONFIRMED` | La ubicación ya fue escaneada | El preparador debe comprobar el artículo |
| `ARTICLE_CONFIRMED` | El artículo ya fue escaneado | El preparador debe contar y confirmar |
| `BLOCKED` | Detenida administrativamente | Revisar la razón y no asignarla hasta resolverla |
| `COMPLETED` | Preparación terminada y registrada | No volver a preparar esta tarea |

Una tarea disponible o bloqueada normalmente no muestra usuario ni terminal.

## 4. Revisar una tarea marcada como atascada

El tablero marca `STUCK` y resalta en rojo una tarea activa que no ha avanzado
durante más tiempo que el límite configurado. En el entorno de demostración, el
límite es de 30 minutos.

La marca no demuestra por sí sola que exista una falla. El preparador podría estar
contando, atendiendo una instrucción de seguridad o trabajando en una zona sin
conexión.

1. Anote `Task #`, `User`, `Device`, `State` y `Last transition`.
2. Localice al preparador.
3. Pregunte si conserva la tarea en la terminal.
4. Revise si la terminal muestra `ON`, `OFF`, `Q:` o un mensaje de error.
5. Determine si el preparador continúa trabajando o necesita ayuda.
6. Si el trabajo es válido y está avanzando, permita que continúe y vuelva a
   revisar el tablero.
7. Si la tarea no puede continuar, solicite su bloqueo con una razón concreta.

**No haga esto:** no bloquee automáticamente una tarea solo porque aparece en
rojo. Si la terminal tiene información pendiente, el bloqueo puede hacer que esa
información sea rechazada al recuperar la conexión.

## 5. Atender una ubicación incorrecta

Cuando el preparador informe una ubicación incorrecta:

1. Pídale que no retire ningún artículo.
2. Compare la ubicación de la terminal con el rótulo físico.
3. Indique que vaya a la ubicación mostrada por la terminal.
4. Si la ubicación existe pero la etiqueta no puede leerse, marque la etiqueta
   para reposición y solicite una nueva.
5. Si la ubicación no existe o su contenido no corresponde, detenga la tarea y
   registre la diferencia.

Un escaneo incorrecto no cambia las existencias. Normalmente el preparador puede
corregirlo escaneando la ubicación correcta.

## 6. Atender un artículo incorrecto

1. Pida al preparador que devuelva el artículo incorrecto al lugar donde estaba.
2. Compare el código de la terminal con la etiqueta del artículo físico.
3. Busque el artículo correcto dentro de la ubicación confirmada.
4. Si se encuentra, permita que el preparador lo escanee y continúe.
5. Si no se encuentra, detenga la tarea y registre artículo, ubicación y cantidad.

Un escaneo incorrecto no cambia las existencias. No autorice sustituciones por
artículos parecidos.

## 7. Atender una diferencia de cantidad

Cuando aparezca `COUNT MISMATCH`:

1. Pida al preparador que pulse `B` y vuelva a contar.
2. Realice o presencie un segundo conteo físico.
3. Compare el resultado con `Qty` en el tablero o con la cantidad mostrada en la
   terminal.
4. Si ahora coincide, permita confirmar la cantidad exacta.
5. Si no coincide, no permita la confirmación.
6. Anote tarea, artículo, ubicación, cantidad solicitada y cantidad encontrada.
7. Solicite el bloqueo de la tarea mientras se investiga la diferencia.
8. Entregue los datos al administrador o a soporte para decidir si corresponde un
   ajuste de existencias.

**No haga esto:** no solicite un ajuste de existencias de forma automática. Primero
debe comprobarse el historial, porque una preparación pendiente de sincronizar
puede explicar la diferencia.

## 8. Atender una terminal sin conexión

### Antes de tomar una tarea

Si la terminal muestra `OFF`, el preparador no podrá recibir una tarea nueva.

1. Lleve la terminal a una zona donde normalmente existe cobertura.
2. Espere a que aparezca `ON`.
3. Si continúa sin conexión, pruebe otra terminal autorizada o avise a soporte.

### Durante una tarea

Si la terminal pierde la conexión después de recibir la tarea:

1. Permita que el preparador termine los escaneos y el conteo.
2. Indique que mantenga encendida la terminal.
3. Lleve la terminal a una zona con cobertura.
4. Espere a que `OFF` cambie a `ON`.
5. Espere a que desaparezca `Q:`.
6. Compruebe en el tablero que la tarea avanzó o terminó.

**No haga esto:** no bloquee la tarea ni pida repetir la preparación mientras la
terminal muestre `Q:` sin comprobar primero la sincronización.

## 9. Atender `SYNC FAILED`

`SYNC FAILED` significa que el sistema rechazó información guardada por la
terminal. La preparación física pudo haberse realizado, pero el movimiento puede
no estar registrado.

1. Pida al preparador que no repita el trabajo y que no pulse `A` todavía.
2. Anote o fotografíe el número de tarea y el código mostrado.
3. Registre usuario, terminal, ubicación, artículo y hora aproximada.
4. Busque la tarea en el tablero y anote su estado actual.
5. Avise al administrador o a soporte para que compruebe el historial de la tarea
   y de las existencias.
6. Espere una decisión de recuperación antes de mover artículos o ajustar
   existencias.
7. Cuando los datos hayan sido conservados y soporte lo indique, permita que el
   preparador pulse `A` para salir de la pantalla.

**Deténgase y avise:** nunca deduzca únicamente por la pantalla si las existencias
deben aumentar o disminuir.

## 10. Solicitar el bloqueo de una tarea

El bloqueo se utiliza cuando una tarea no puede continuar de forma segura. Algunos
ejemplos son:

- artículo faltante después de un segundo conteo;
- ubicación o etiqueta dañada que impide comprobar el trabajo;
- terminal perdida o dañada con una tarea activa;
- riesgo de duplicar una preparación después de un fallo de sincronización;
- investigación de una diferencia física.

Antes de solicitar el bloqueo:

1. Compruebe si la terminal conserva información pendiente en `Q:`.
2. Anote el estado actual de la tarea.
3. Describa el problema de forma concreta.
4. Incluya ubicación, artículo y cantidad cuando sean relevantes.

Una razón adecuada sería:

> Faltan 3 unidades de ART-001 en A-01-01 después de un segundo conteo a las
> 14:35; tarea detenida para revisión de existencias.

Una razón inadecuada sería:

> No funciona.

Al bloquear una tarea, el sistema:

- libera al preparador y a la terminal;
- borra las confirmaciones de ubicación y artículo de esa tarea;
- conserva la asignación prevista del artículo y la ubicación;
- no cambia las existencias;
- registra quién realizó el bloqueo y la razón.

El tablero no ejecuta el bloqueo. La acción debe realizarla una persona autorizada
mediante la herramienta administrativa.

## 11. Solicitar la reanudación de una tarea

Una tarea solo debe reanudarse cuando la causa del bloqueo ya fue resuelta.

1. Lea la razón original del bloqueo en el registro de la incidencia.
2. Confirme qué acción resolvió el problema.
3. Compruebe con el administrador o soporte que no queda una sincronización
   pendiente de la terminal anterior.
4. Solicite la reanudación.
5. Compruebe que el estado cambia de `BLOCKED` a `AVAILABLE`.
6. Informe al equipo que la tarea volverá a asignarse siguiendo el orden normal del
   sistema.

Reanudar no cambia las existencias y no devuelve la tarea directamente al mismo
preparador. La tarea queda disponible para ser tomada nuevamente.

## 12. Inicio de turno

Al comenzar el turno:

1. Abra el tablero y compruebe `Last refreshed`.
2. Revise tareas `STUCK` o resaltadas en rojo.
3. Revise tareas `BLOCKED` y sus incidencias abiertas.
4. Confirme que las terminales necesarias están encendidas y cargadas.
5. Confirme que los preparadores tienen su propio gafete.
6. Informe al equipo sobre ubicaciones, etiquetas o artículos con problemas
   conocidos.

## 13. Entrega de turno

Antes de retirarse:

1. Revise las tareas `STUCK`, `BLOCKED` y asignadas que siguen abiertas.
2. Compruebe que ninguna terminal entregada muestre `Q:`.
3. Registre diferencias de cantidad y fallos de sincronización pendientes.
4. Entregue al siguiente supervisor los números de tarea y el estado de cada
   incidencia.
5. Confirme quién dará seguimiento y cuándo se revisará nuevamente.

No describa una incidencia solamente como “pendiente”. Indique qué ocurrió, qué se
comprobó y cuál es la siguiente acción.

## Cuándo avisar a soporte inmediatamente

- El tablero dejó de actualizarse.
- Varias terminales perdieron la conexión al mismo tiempo.
- Aparecen fallos de sincronización repetidos.
- Una tarea figura como completada, pero el movimiento físico está en duda.
- Las existencias del sistema no coinciden con un conteo confirmado.
- Una tarea cambia de estado sin una acción conocida.
- No se puede iniciar sesión con varias cuentas autorizadas.
- El sistema o la pantalla de salud no responden.

## Datos que debe conservar

Para cualquier incidente, registre:

- fecha y hora aproximada;
- número de tarea y orden;
- usuario y terminal;
- ubicación y artículo;
- cantidad solicitada y cantidad encontrada;
- estado del tablero;
- mensaje exacto de la terminal;
- estado `ON`, `OFF` o `Q:`;
- fotografías necesarias, sin mostrar claves;
- acciones realizadas y persona que las autorizó.

Para un recordatorio de una sola página, consulte la
[Guía rápida del supervisor](referencia-rapida/guia-rapida-supervisor.md).
