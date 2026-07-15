# Guía rápida de soporte

## Primero

1. No repita el trabajo ni el ajuste.
2. Conserve hora, tarea, orden, usuario, terminal y mensaje.
3. Compruebe `ON`/`OFF`/`Q:`.
4. Obtenga `correlationId` si existe.
5. Determine si afecta a uno o a varios usuarios.

## Comprobar

- Salud local.
- Salud remota; HTTPS obligatorio en `preprod`.
- Actualización del tablero.
- Logs por hora o `correlationId`.
- Tarea, transiciones y movimientos con SQL de solo lectura.

## Regla de decisión

| Evidencia | Acción |
|---|---|
| Salud local funciona, remota falla | Revisar red, firewall y HTTPS |
| Tarea atascada | Contactar preparador antes de bloquear |
| Terminal con `Q:` | Esperar/investigar sincronización antes de bloquear |
| `SYNC FAILED` | No repetir; comprobar tarea y movimiento |
| Diferencia de existencias | Preservar evidencia y escalar antes de ajustar |
| `INTERNAL_ERROR` | Conservar stack trace; no repetir escritura |

## Recuperaciones permitidas

- Bloquear o reanudar con autorización.
- Ajustar existencias con conteo e historial comprobados.
- Corregir configuración y reiniciar.
- Volver a una versión estable compatible.
- Crear una migración nueva hacia adelante.
- Restaurar una copia solo mediante decisión coordinada.

## Nunca haga esto

- No cambie datos con SQL.
- No borre movimientos o transiciones.
- No edite migraciones aplicadas.
- No use `flyway repair` para ocultar cambios.
- No elimine volúmenes de preproducción.
- No copie claves o sesiones de acceso.

## Para cerrar

Causa, línea de tiempo, recuperación, autorización, verificación, evidencia y
seguimiento deben quedar registrados.
