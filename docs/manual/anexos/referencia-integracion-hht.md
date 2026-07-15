# Referencia técnica de integración HHT

**Público:** desarrollo de clientes HHT, pruebas y soporte avanzado\
**Contrato:** WMS REST `v1`\
**Estado:** borrador inicial

Esta referencia resume el subconjunto que utiliza HandheldPi. La fuente oficial
sigue siendo [`API.md`](../../../API.md). Las decisiones particulares del cliente
están en [`HandheldPi/API.md`](../../../../HandheldPi/API.md).

## Dirección y encabezados

Configure la dirección sin añadir `/api/v1`:

```text
http://192.168.1.50:8080
```

En todas las solicitudes JSON:

```http
Accept: application/json
Content-Type: application/json
X-Correlation-Id: <UUID nuevo para esta solicitud>
```

Después de iniciar sesión:

```http
Authorization: Bearer <token opaco>
```

No guarde el token en logs, capturas, incidencias ni ejemplos reales.

El WMS devuelve `X-Correlation-Id` en el encabezado. HandheldPi registra el UUID
en el evento `wms_rejected`, pero actualmente no lo registra para respuestas
exitosas.

## Secuencia de solicitudes

### 1. Iniciar sesión

```http
POST /api/v1/auth/login
```

```json
{
  "username": "picker02",
  "password": "<PIN>",
  "deviceCode": "HHT-PI-01"
}
```

La terminal debe existir y estar activa. El usuario también debe estar activo y
tener una clave compatible con la entrada de la terminal.

### 2. Obtener o recuperar tarea

```http
GET /api/v1/hht/tasks/next
```

Un `200` devuelve una tarea. Un `204` significa que no hay trabajo. El servidor
devuelve primero la tarea activa del usuario; si no existe, toma la siguiente por
orden FIFO global.

Ejemplo abreviado:

```json
{
  "id": 101,
  "state": "ASSIGNED",
  "orderNumber": "DEMO-1001",
  "location": {"code": "A-01-01"},
  "article": {"sku": "ART-001", "description": "Camiseta negra"},
  "quantity": 20
}
```

### 3. Confirmar ubicación

```http
POST /api/v1/hht/tasks/101/scan-location
```

```json
{"qrValue": "LOC:A-01-01"}
```

### 4. Confirmar artículo

```http
POST /api/v1/hht/tasks/101/scan-article
```

```json
{"qrValue": "ART:ART-001"}
```

### 5. Confirmar cantidad

```http
POST /api/v1/hht/tasks/101/confirm
```

```json
{
  "confirmationId": "7a3d389f-9150-43ef-90e6-0955ea37d2a7",
  "quantity": 20
}
```

La cantidad debe ser exactamente la solicitada. El UUID se genera una vez, se
guarda antes del primer envío y se reutiliza sin cambiar la cantidad.

### 6. Cerrar sesión

```http
POST /api/v1/auth/logout
```

No lleva cuerpo y devuelve `204`. HandheldPi lo impide mientras existan acciones
`pending` que todavía necesiten esa sesión.

## Formato de error

Los errores de aplicación usan `application/problem+json`:

```json
{
  "title": "Wrong location",
  "status": 409,
  "code": "WRONG_LOCATION",
  "detail": "La ubicación no corresponde a la tarea.",
  "correlationId": "450ad1c5-6006-4c98-b48f-3e92a9db6ae7"
}
```

El cliente debe decidir con `status` y `code`, no comparando el texto traducible
de `detail`.

## Mapeo de errores del cliente actual

| Respuesta | Clasificación HHT | Acción |
|---|---|---|
| Conexión rechazada o espera agotada | WMS no disponible | Pasar a `OFF`; conservar pendientes |
| Cualquier `5xx` | WMS no disponible | Pasar a `OFF`; conservar pendientes |
| `401 INVALID_TOKEN` | Reautenticación | Volver al login; conservar pendientes |
| `401 TOKEN_EXPIRED` | Reautenticación | Volver al login; conservar pendientes |
| `401 TOKEN_REVOKED` | Reautenticación | Volver al login; conservar pendientes |
| Otro `4xx` en una operación en vivo | Rechazo | Mostrar el problema; no repetir a ciegas |
| Otro `4xx` al vaciar la cola | Rechazo definitivo de esa cadena | Pasar la operación y sus sucesoras de la tarea a `dead`; mostrar `SYNC FAILED` |

Un cuerpo que no sea JSON se limita al diagnosticar; nunca debe mostrarse una
excepción interna ni registrarse un cuerpo arbitrario completo.

## Códigos importantes por acción

| Acción | Códigos que debe manejar |
|---|---|
| Login | `INVALID_CREDENTIALS`, `USER_INACTIVE`, `DEVICE_INACTIVE`, `DEVICE_NOT_REGISTERED`, `DEVICE_ASSIGNMENT_CONFLICT` |
| Pedir tarea | `TASK_ASSIGNMENT_CONFLICT` |
| Ubicación | `TASK_NOT_FOUND`, `TASK_NOT_ASSIGNED_TO_USER`, `INVALID_TASK_STATE`, `WRONG_LOCATION` |
| Artículo | `TASK_NOT_FOUND`, `TASK_NOT_ASSIGNED_TO_USER`, `INVALID_TASK_STATE`, `WRONG_ARTICLE` |
| Confirmación | `TASK_NOT_FOUND`, `TASK_NOT_ASSIGNED_TO_USER`, `INVALID_TASK_STATE`, `CONFIRMATION_ID_REUSED`, `INSUFFICIENT_STOCK`, `QUANTITY_MISMATCH` |

Consulte el [catálogo de errores](codigos-de-error.md) para la acción operativa.

## Reglas de reproducción

- Un escaneo correcto repetido devuelve éxito y puede incluir `replayed: true`.
- Una confirmación repetida con el mismo `confirmationId` y cantidad devuelve el
  resultado original sin descontar existencias otra vez.
- El estado de orden incluido puede haber avanzado desde la primera respuesta.
- Una confirmación con el mismo UUID y otra cantidad devuelve
  `CONFIRMATION_ID_REUSED`.
- En `v1`, cualquier respuesta `409` a la confirmación es un error real.

## Cola local HandheldPi

La cola usa SQLite en modo WAL y conserva una fila por acción. La clave de
operación es única.

```text
pending ──éxito──> sent
   |
   └──rechazo──> dead ──acuse del operador──> acknowledged
```

Las filas `sent` se conservan como rastro local. Si una acción queda `dead`, las
acciones posteriores pendientes de esa misma tarea también quedan `dead` para no
romper el orden. Una falta de red o autenticación deja la fila en `pending`.

Una base antigua de contrato `v0` se migra a esquema `v2`; sus confirmaciones no
enviadas se archivan como `dead` porque no son entregables a `v1`.

El método que borra toda la cola existe solo para pruebas guionizadas. No es una
herramienta de recuperación operativa.

## Configuración de referencia

```toml
[device]
id = "HHT-PI-01"
site = "MAD-01"

[wms]
backend = "http"
base_url = "http://192.168.1.50:8080"
timeout_s = 5.0
retry_interval_s = 30.0

[workflow]
pin_length = 4

[logging]
file = "/var/log/hht/hht.jsonl"

[queue]
db_path = "/var/lib/hht/queue.db"
```

Proteja los directorios, use un usuario de servicio limitado y configure la
rotación definida por HandheldPi. No coloque secretos en el archivo si el cliente
no los necesita.

## Comprobación de salud

```http
GET /actuator/health
```

No requiere token. Un `200` indica que la aplicación y la base de datos están
disponibles; no demuestra que un usuario, terminal o tarea particulares sean
válidos.

## Compatibilidad de cliente

Un cliente alternativo debe demostrar:

- el mismo orden de estados;
- códigos QR exactos;
- token Bearer protegido;
- correlación por solicitud;
- confirmación idempotente persistida antes del envío;
- cola ordenada si declara funcionamiento sin conexión;
- manejo específico de cada error;
- bloqueo de nueva tarea mientras queden acciones pendientes;
- evidencia contra un WMS y PostgreSQL reales.

La expresión “correlación por solicitud” exige enviar un UUID y procesar el eco del
servidor. Si además se requiere trazabilidad completa de éxitos entre ambos logs,
el cliente debe añadirla de forma segura y con pruebas; hoy esa parte está
pendiente.
