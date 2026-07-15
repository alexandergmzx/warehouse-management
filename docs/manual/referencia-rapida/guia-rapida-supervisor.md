# Guía rápida del supervisor

**Esta hoja sirve como recordatorio. No sustituye la capacitación ni el registro
de incidencias.**

## Al comenzar el turno

- Abra `/dashboard`.
- Compruebe que `Last refreshed` cambia cada pocos segundos.
- Revise tareas `STUCK`, `BLOCKED` y tareas asignadas que siguen abiertas.
- Confirme que las terminales están cargadas y disponibles.

## Estados principales

| Estado | Significado |
|---|---|
| `AVAILABLE` | Disponible |
| `ASSIGNED` | Asignada a un preparador y una terminal |
| `LOCATION_CONFIRMED` | Ubicación confirmada |
| `ARTICLE_CONFIRMED` | Artículo confirmado; falta contar y confirmar |
| `BLOCKED` | Detenida administrativamente |
| `COMPLETED` | Terminada; no repetir |

## Qué hacer ante un problema

| Situación | Acción inmediata |
|---|---|
| Ubicación incorrecta | No recoger; comprobar rótulo y ubicación mostrada |
| Artículo incorrecto | Devolverlo y buscar el artículo indicado |
| Cantidad diferente | Segundo conteo; si continúa diferente, detener y solicitar bloqueo |
| Tarea `STUCK` | Contactar al preparador antes de bloquear |
| Terminal `OFF` | Buscar cobertura y mantenerla encendida |
| Terminal con `Q:` | Esperar sincronización; no bloquear ni repetir sin comprobar |
| `SYNC FAILED` | No repetir; anotar tarea/código y avisar a soporte |
| Tablero sin actualizar | No confiar en la pantalla; actualizar una vez y avisar a soporte |

## Antes de bloquear

1. Compruebe si la terminal muestra `Q:`.
2. Anote tarea, estado, usuario, terminal, ubicación, artículo y cantidad.
3. Escriba una razón concreta.
4. Solicite la acción a una persona autorizada.

Bloquear libera al preparador y la terminal, pero no cambia las existencias.

## Antes de reanudar

1. Confirme que la causa fue resuelta.
2. Confirme que no queda información pendiente en la terminal anterior.
3. Registre la solución.
4. Solicite la reanudación.
5. Compruebe que la tarea queda `AVAILABLE`.

## Nunca haga esto

- No autorice una cantidad incorrecta.
- No permita sustituir un artículo por otro parecido.
- No bloquee automáticamente una tarea solo porque está en rojo.
- No repita físicamente una tarea con `SYNC FAILED`.
- No ajuste existencias sin revisar el historial.
- No cambie tareas o existencias directamente en la base de datos.
- No comparta una cuenta administrativa.

## Datos para soporte

Fecha/hora, tarea, orden, usuario, terminal, estado, ubicación, artículo, cantidades,
mensaje de pantalla, `ON`/`OFF`/`Q:` y acciones ya realizadas.
