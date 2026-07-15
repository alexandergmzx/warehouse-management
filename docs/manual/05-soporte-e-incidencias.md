# Manual de soporte e incidencias

**Público:** soporte de aplicación, supervisores, operadores y responsables de
incidencias\
**Nivel:** operativo y técnico\
**Estado:** borrador inicial; pendiente de simulacro de incidencia

## Objetivo

Este manual ayuda a responder una pregunta antes de cambiar el sistema:

> ¿Qué ocurrió realmente y qué evidencia lo demuestra?

La recuperación comienza después de entender el estado. Reiniciar, repetir una
preparación o ajustar existencias sin comprobar puede ocultar la causa o duplicar un
movimiento.

## Regla principal

1. Proteja a las personas y detenga el riesgo de duplicar trabajo.
2. Conserve la pantalla, la hora y los identificadores.
3. Compruebe salud, registros e historial de datos.
4. Determine la causa o declare que todavía es desconocida.
5. Ejecute únicamente una recuperación autorizada y registrada.
6. Compruebe el resultado antes de cerrar la incidencia.

## Lo que soporte puede hacer

- Comprobar salud local y remota.
- Consultar el tablero.
- Recopilar mensajes de terminales y herramientas administrativas.
- Leer registros estructurados.
- Ejecutar consultas SQL de solo lectura aprobadas.
- Correlacionar orden, tarea, usuario, terminal, artículo, ubicación y movimiento.
- Recomendar una acción administrativa auditada.
- Coordinar un cambio de configuración, rollback o corrección de software.

## Lo que soporte no debe hacer

- Cambiar tareas o existencias directamente con SQL.
- Actualizar o borrar movimientos o transiciones históricas.
- Editar una migración ya aplicada.
- Usar `flyway repair` para ocultar una diferencia.
- Eliminar un volumen para recuperar preproducción.
- Repetir un ajuste de existencias después de una respuesta incierta.
- Pedir al preparador que repita físicamente una tarea sin comprobar su estado.
- Copiar claves, sesiones de acceso o hashes en una incidencia.

## Clasificación inicial

La siguiente clasificación es una guía inicial. El responsable del ambiente puede
establecer tiempos de respuesta más estrictos.

| Severidad | Ejemplos |
|---|---|
| Crítica | Riesgo para personas; corrupción o pérdida amplia de datos; existencias no confiables en varias áreas; servicio completo detenido sin alternativa |
| Alta | Varios preparadores afectados; fallos repetidos de sincronización; proceso principal detenido; posible duplicación de movimientos |
| Media | Una tarea, terminal o persona afectada con el resto del sistema disponible |
| Baja | Consulta, problema visual, etiqueta aislada o defecto documental sin impacto inmediato |

Una incidencia puede cambiar de severidad conforme aparece nueva evidencia. Registre
el motivo del cambio.

## Estados de una incidencia

| Estado | Significado |
|---|---|
| Abierta | El problema fue reportado y todavía no está clasificado por completo |
| Diagnosticada | Existe evidencia suficiente de la causa o del estado exacto |
| Recuperada | El trabajo puede continuar, aunque falte una corrección definitiva |
| Cerrada | La causa, recuperación, evidencia y seguimiento quedaron registrados |

“Ya funciona” no es suficiente para cerrar una incidencia.

## 1. Recibir el reporte

Pida los siguientes datos:

- quién reporta;
- fecha y hora aproximada;
- usuario y terminal;
- número de orden y tarea;
- ubicación y artículo;
- cantidad solicitada y encontrada;
- mensaje exacto de la pantalla;
- estado `ON`, `OFF` o `Q:` de la terminal;
- acción que se intentaba realizar;
- acciones ya realizadas;
- identificador de correlación, cuando exista.

No retrase la atención si falta un dato. Registre “desconocido” y continúe con lo
disponible.

## 2. Evitar más impacto

Antes del diagnóstico:

1. Pida que no se repita físicamente la tarea.
2. Pida que no se cierre o reinicie una terminal con `Q:`.
3. Detenga ajustes relacionados con el mismo artículo y ubicación.
4. Si varias personas están afectadas, avise al supervisor para pausar trabajo
   nuevo de forma controlada.
5. Conserve capturas y registros antes de reiniciar servicios.

Una tarea puede bloquearse para liberar a una persona o terminal, pero primero debe
comprobarse si existe información pendiente de sincronización. El bloqueo puede
provocar el rechazo de esa información.

## 3. Determinar el alcance

Pregunte:

- ¿Afecta a una persona o a varias?
- ¿Afecta a una sola terminal o a todas?
- ¿Afecta a una ubicación, un artículo o todo el almacén?
- ¿La salud local responde?
- ¿La salud desde la red responde?
- ¿El tablero continúa actualizándose?
- ¿Ocurrió después de un despliegue, reinicio o cambio de red?

| Alcance observado | Primera área a revisar |
|---|---|
| Una lectura incorrecta | Etiqueta, artículo, ubicación y tarea |
| Una terminal sin conexión | Cobertura, configuración y estado del dispositivo |
| Varias terminales sin conexión | Salud remota, red, firewall o HTTPS |
| Tablero y terminales afectados | Aplicación o base de datos |
| Solo administración REST | Sesión, permisos o contrato de solicitud |
| Comienza tras un despliegue | Versión, configuración y migraciones |

## 4. Comprobar salud

1. Compruebe salud desde el propio servidor.
2. Si funciona, compruebe desde la red autorizada.
3. En `preprod`, use únicamente la dirección HTTPS aprobada para la comprobación no
   local.

Si salud local funciona y la remota falla, revise red, firewall, dirección y HTTPS.
No reinicie la aplicación como primera medida.

Si salud local falla, revise consola, configuración y PostgreSQL.

## 5. Encontrar el identificador de correlación

Cada solicitud recibe un `correlationId`. Puede aparecer:

- en la respuesta de error;
- en el encabezado `X-Correlation-Id`;
- en el registro de la terminal;
- en los logs de la aplicación;
- en movimientos y transiciones auditadas.

Este identificador une el mensaje del usuario, la solicitud, el log y el historial.
No es una clave y puede conservarse como evidencia.

Si no está disponible, utilice hora aproximada, tarea, orden, usuario y terminal
para reducir la búsqueda.

Para interpretar el código recibido, consulte el
[Catálogo de errores en español](anexos/codigos-de-error.md).

## 6. Leer los registros

La aplicación escribe un objeto JSON por línea en la salida estándar. Los eventos
más útiles son:

| Mensaje | Significado |
|---|---|
| `login succeeded` | Inicio de sesión correcto |
| `login rejected` | Inicio de sesión rechazado |
| `business rule violation` | Una regla de negocio rechazó la acción |
| `pick confirmed` | Preparación registrada correctamente |
| `task blocked` | Tarea bloqueada por administración |
| `task resumed` | Tarea reanudada |
| `stock adjusted` | Ajuste de existencias aplicado |
| `order created` | Orden creada |
| `Unhandled exception` | Falla inesperada del software |

Una `business rule violation` no siempre significa un defecto. Puede ser la
protección esperada ante una ubicación, artículo, cantidad o estado incorrectos.

Un `Unhandled exception` y `INTERNAL_ERROR` sí requieren conservar el error completo
y escalar a desarrollo.

Los comandos de filtrado están en la
[Referencia técnica de diagnóstico](anexos/referencia-tecnica-diagnostico.md).

## 7. Consultar el historial de datos

Utilice únicamente las consultas aprobadas de solo lectura:

1. tareas atascadas;
2. diferencias entre existencias y movimientos;
3. recorrido completo de una orden;
4. resumen de integridad para entrega de turno.

Las consultas oficiales están en [`docs/sql-diagnostics.md`](../sql-diagnostics.md).

Una diferencia entre existencias y movimientos es siempre una incidencia. Conserve
registros y resultados antes de cualquier recuperación.

## 8. Elegir una recuperación

| Situación comprobada | Recuperación permitida |
|---|---|
| Tarea activa que no puede continuar | Bloqueo administrativo con razón |
| Causa de una tarea bloqueada ya resuelta | Reanudación administrativa |
| Conteo físico verificado y sin sincronización pendiente | Ajuste auditado de existencias |
| Configuración incorrecta | Cambio aprobado y reinicio |
| Defecto de aplicación sin cambio incompatible de datos | Rollback a versión estable |
| Defecto de estructura | Nueva migración hacia adelante |
| Daño grave que requiere restauración | Restauración aprobada desde copia, con evaluación de pérdida de datos |

La acción debe citar la evidencia que la justifica. No elija una recuperación solo
porque es la más rápida.

## 9. Comprobar después de recuperar

1. Compruebe salud.
2. Compruebe el tablero.
3. Consulte el estado de la tarea u orden afectada.
4. Compruebe el movimiento de existencias cuando corresponda.
5. Confirme que la terminal no conserva `Q:` o `SYNC FAILED` sin atender.
6. Realice un caso de prueba controlado.
7. Confirme con el usuario que puede continuar.
8. Registre el resultado y cualquier riesgo pendiente.

Recuperar el servicio no elimina la necesidad de corregir la causa.

## 10. Cerrar y dar seguimiento

Antes de cerrar:

- la causa está indicada o declarada como desconocida con seguimiento;
- la línea de tiempo está completa;
- la recuperación identifica quién la autorizó y ejecutó;
- los logs y resultados SQL están conservados;
- no hay secretos en la evidencia;
- el estado final fue comprobado;
- existe una acción para evitar repetición cuando corresponde;
- las pruebas se actualizarán si apareció un caso no cubierto.

Utilice la [Plantilla de incidencia](plantillas/registro-de-incidencia.md).

## Procedimientos por síntoma

### No se puede iniciar sesión

1. Registre usuario, terminal, hora y código de error.
2. No pida la clave al usuario.
3. Determine si afecta a una cuenta o a varias.
4. Revise `login rejected` y el código exacto.
5. Compruebe si usuario o terminal están inactivos o no registrados.
6. Si aparece conflicto de terminal, revise si conserva otra tarea activa.

### Ubicación o artículo incorrectos

1. Confirme que las existencias no cambiaron.
2. Compare tarea, etiqueta y valor escaneado.
3. Revise `WRONG_LOCATION` o `WRONG_ARTICLE` por `correlationId`.
4. Sustituya la etiqueta únicamente si está incorrecta o dañada.
5. Permita continuar con el valor correcto.

### Diferencia de cantidad

1. Solicite segundo conteo.
2. Compruebe tareas y sincronizaciones pendientes.
3. Trace la orden y el historial de movimientos.
4. Bloquee la tarea si no puede continuar.
5. Ajuste únicamente después de demostrar la diferencia.

### Tarea atascada

1. Contacte al preparador antes de bloquear.
2. Revise terminal, `Q:` y último estado.
3. Ejecute la consulta de tareas atascadas.
4. Si no puede continuar, bloquee con una razón detallada.

### `SYNC FAILED`

1. No permita repetir la preparación.
2. Conserve tarea y código de pantalla.
3. Consulte el estado actual y el historial completo.
4. Compruebe si existe un movimiento `PICK`.
5. Determine recuperación con administrador y supervisor.
6. Permita reconocer la pantalla solo después de conservar la evidencia.

### Tablero sin actualizar

1. Compruebe salud.
2. Actualice una vez el navegador.
3. Revise la solicitud `/dashboard/api/tasks` y los logs.
4. Si salud funciona, investigue sesión, navegador o respuesta del tablero.
5. No use información congelada para autorizar una recuperación.

### `preprod` no inicia

1. Lea `Description` y `Action` en `APPLICATION FAILED TO START`.
2. Identifique únicamente el nombre de la variable ausente o insegura.
3. No copie valores secretos en la incidencia.
4. Corrija la configuración y reinicie.
5. Compruebe salud antes de abrir acceso remoto.

### Error interno

1. Conserve `correlationId`, hora y operación.
2. Conserve el `stack_trace` completo de `Unhandled exception`.
3. Determine si la acción pudo haber cambiado datos antes del error.
4. No repita una acción de escritura hasta comprobar el resultado.
5. Escale a desarrollo con una reproducción mínima y la evidencia.

## Limitación conocida de los logs

Cada línea de log no incluye por sí misma la versión o configuración desplegada.
Correlacione la hora del incidente con el registro de despliegue. Esta limitación
debe resolverse antes de considerar el sistema preparado para una operación más
formal.

Para el triaje inicial consulte la
[Guía rápida de soporte](referencia-rapida/guia-rapida-soporte.md).
