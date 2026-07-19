# Guía rápida de integración

## Antes de conectar una terminal

1. Identifique versiones exactas de WMS y HandheldPi.
2. Registre y active un `deviceCode` único.
3. Configure `backend = "http"` y la dirección base sin `/api/v1`.
4. Verifique salud, red, hora, permisos de cola y logs.
5. Use HTTPS para acceso no local en preproducción.

## Flujo HHT

```text
login → next → scan-location → scan-article → confirm → logout
```

- Ubicación: `LOC:<código>`.
- Artículo: `ART:<sku>`.
- Cantidad: exactamente la solicitada.
- `confirmationId`: generar una vez, guardar y reutilizar.

## Si falla

| Situación | Acción |
|---|---|
| Red, espera agotada o `5xx` | Conservar pendiente y reintentar |
| Token inválido/vencido/revocado | Volver al login sin borrar la cola |
| Otro `4xx` | Tratar como rechazo; no repetir a ciegas |
| Rechazo durante sincronización | Mostrar `SYNC FAILED` y escalar |

Con cola pendiente: no tomar otra tarea, no cerrar sesión y no borrar
`queue.db`.

## Para investigar

- Use `X-Correlation-Id` para unir un rechazo del HHT con el WMS.
- Use `confirmationId` para comprobar los reintentos de una confirmación.
- Nunca copie PIN, clave o token Bearer.

El HHT actual no registra `X-Correlation-Id` en respuestas exitosas; use tarea,
orden, `confirmationId` y hora, y deje anotada esta limitación.

## MFC

Desactivado por defecto (`WMS_MFC_ADAPTER=noop`). Con `telegram` (ADR 0011)
el WMS emite misiones TRANSPORT al WCS por HTTP (contrato `TELEGRAMS.md`) y
recibe confirmaciones en `POST /api/v1/mfc/missions/{id}/confirmations` con
el rol `WCS`. La red nunca ocurre dentro de la transacción de confirmación:
el adaptador solo inserta la fila outbox. No existe `tcp` y las misiones
SORT responden `501`.

## Aceptación

Una prueba local no autoriza el dispositivo en almacén. Complete la prueba con
hardware y Wi-Fi reales y registre el resultado en la
[lista de verificación](../plantillas/lista-verificacion-integracion.md).
