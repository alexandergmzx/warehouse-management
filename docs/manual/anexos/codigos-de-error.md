# Catálogo de errores en español

**Público:** supervisión, administración, soporte e integración\
**Fuente oficial:** `ProblemCode.java` y [`API.md`](../../../API.md)

El código estable identifica la regla que rechazó una acción. Conserve también el
`correlationId`.

## Solicitud

| Código | Significado | Acción inicial |
|---|---|---|
| `VALIDATION_FAILED` | Uno o más datos no cumplen las reglas | Revise los campos indicados y corrija solo esos datos |
| `MALFORMED_REQUEST` | El contenido enviado no tiene un formato JSON válido | Corrija la estructura; no repita el mismo contenido |

## Cuenta y sesión

| Código | Significado | Acción inicial |
|---|---|---|
| `INVALID_CREDENTIALS` | Usuario o clave incorrectos | Reintentar una vez; después avisar sin comunicar la clave |
| `INVALID_TOKEN` | Sesión de acceso no reconocida | Iniciar sesión nuevamente |
| `TOKEN_EXPIRED` | La sesión terminó por tiempo | Iniciar sesión; la terminal conserva su cola |
| `TOKEN_REVOKED` | La sesión fue cerrada o reemplazada | Iniciar sesión y revisar por qué fue revocada |
| `FORBIDDEN` | La cuenta no tiene permiso para la acción | Detenerse; no utilizar otra cuenta sin autorización |
| `USER_INACTIVE` | Usuario desactivado | Escalar al responsable de accesos |
| `DEVICE_INACTIVE` | Terminal desactivada | Retirar la terminal y escalar |
| `DEVICE_NOT_REGISTERED` | La terminal no existe en el sistema | Revisar identificador y aprovisionamiento |
| `DEVICE_ASSIGNMENT_CONFLICT` | La terminal está vinculada a trabajo activo de otra persona | Consultar tarea y usuario actuales; no compartir cuentas |

## Tareas de preparación

| Código | Significado | Acción inicial |
|---|---|---|
| `TASK_NOT_FOUND` | La tarea indicada no existe | Comprobar el número y el ambiente |
| `TASK_NOT_ASSIGNED_TO_USER` | La tarea pertenece a otra sesión | Comprobar usuario, terminal y estado; no forzar |
| `INVALID_TASK_STATE` | La acción no corresponde al paso actual | Consultar historial antes de recuperar |
| `WRONG_LOCATION` | La ubicación escaneada no coincide | Ir a la ubicación indicada; no cambia existencias |
| `WRONG_ARTICLE` | El artículo escaneado no coincide | Devolver el artículo y buscar el correcto |
| `TASK_ASSIGNMENT_CONFLICT` | Conflicto al intentar asignar trabajo simultáneamente | Solicitar nuevamente la siguiente tarea; si se repite, escalar |
| `CONFIRMATION_ID_REUSED` | Un identificador de confirmación se reutilizó con datos distintos | No tratar como éxito; conservar evidencia y escalar a integración |
| `INSUFFICIENT_STOCK` | Las existencias ya no alcanzan al confirmar | Detener tarea y reconciliar antes de ajustar |
| `QUANTITY_MISMATCH` | La cantidad no es exactamente la solicitada | Volver a contar; no confirmar una cantidad diferente |

## Órdenes, artículos, ubicaciones y existencias

| Código | Significado | Acción inicial |
|---|---|---|
| `ORDER_ALREADY_EXISTS` | El número de orden ya existe | Consultar la orden; no inventar otro número |
| `ORDER_NOT_FOUND` | La orden no existe | Comprobar el identificador y el ambiente |
| `INSUFFICIENT_AVAILABLE_STOCK` | No alcanza la cantidad libre para crear toda la orden | La orden no fue creada; revisar existencias y tareas abiertas |
| `ARTICLE_NOT_FOUND` | El artículo no existe | Comprobar catálogo y código |
| `ARTICLE_ALREADY_EXISTS` | El código de artículo ya existe | Consultar el registro; no crear una variante improvisada |
| `LOCATION_NOT_FOUND` | La ubicación no existe | Comprobar catálogo y código |
| `LOCATION_ALREADY_EXISTS` | El código de ubicación ya existe | Consultar el registro existente |
| `PICK_SEQUENCE_ALREADY_EXISTS` | La secuencia pertenece a otra ubicación | Seleccionar una secuencia autorizada y única |
| `NEGATIVE_RESULTING_STOCK` | El ajuste dejaría existencias menores que cero | Revisar conteo, signo y cantidad; no forzar |

## Sistema

| Código | Significado | Acción inicial |
|---|---|---|
| `INTERNAL_ERROR` | Falla inesperada del software | No repetir una escritura; conservar `correlationId` y `stack_trace`; escalar a desarrollo |

## Cuando aparece en `SYNC FAILED`

1. No repita físicamente la preparación.
2. Conserve tarea, código, usuario, terminal y hora.
3. Consulte estado, transiciones y movimientos.
4. Decida la recuperación con soporte y administración.
5. Permita reconocer la pantalla después de conservar la evidencia.
