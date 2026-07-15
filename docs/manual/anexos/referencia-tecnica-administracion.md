# Referencia técnica de administración

**Público:** persona autorizada que ejecuta las acciones administrativas mediante
una herramienta REST\
**Nivel:** técnico\
**Contrato oficial:** [`API.md`](../../../API.md)

Este anexo traduce al español la parte administrativa del contrato. El archivo
`API.md` continúa siendo la fuente técnica oficial si existe una diferencia.

## Reglas de seguridad

- Utilice únicamente una cuenta con función `ADMIN`.
- No registre claves, sesiones de acceso ni sus valores cifrados.
- No pegue una sesión de acceso en un documento, chat o captura.
- Utilice un identificador UUID nuevo en `X-Correlation-Id` para cada solicitud y
  consérvela en el registro de la acción.
- Cierre la sesión al terminar.
- No repita una solicitud que cambia datos cuando no conozca su resultado.

La sesión del tablero del navegador y la sesión de la interfaz REST son distintas.
Entrar a `/dashboard` no proporciona acceso automático a `/api/v1/admin/**`.

## Dirección base y encabezados

Ejemplo de dirección base:

```text
http://<servidor>:8080/api/v1
```

Excepto para iniciar sesión, envíe:

```http
Authorization: Bearer <sesion-de-acceso>
Accept: application/json
X-Correlation-Id: <uuid-nuevo>
```

Para solicitudes con contenido JSON, añada:

```http
Content-Type: application/json
```

## Iniciar sesión

```http
POST /api/v1/auth/login
```

```json
{
  "username": "<usuario-admin>",
  "password": "<clave>",
  "deviceCode": "<terminal-registrada>"
}
```

La respuesta correcta es `200` e incluye `token`, `expiresAt`, usuario y
terminal. Compruebe que `user.role` sea `ADMIN`.

Aunque el administrador no prepare pedidos, la autenticación REST actual exige una
terminal registrada. El prototipo no ofrece funciones REST para crear usuarios o
terminales; deben prepararse mediante el procedimiento controlado del ambiente.

## Cerrar sesión

```http
POST /api/v1/auth/logout
Authorization: Bearer <sesion-de-acceso>
```

La respuesta correcta es `204` sin contenido. Repetir el cierre también devuelve
`204`.

## Crear un artículo

```http
POST /api/v1/admin/articles
```

```json
{
  "sku": "ART-005",
  "description": "Camiseta negra talla mediana"
}
```

Resultado correcto: `201`. Conserve `id`, `sku` y `qrValue`.

Restricciones:

- `sku`: 1 a 50 caracteres, patrón `^[A-Z0-9_-]{1,50}$`;
- `description`: 1 a 200 caracteres, no vacía.

Errores principales: `ARTICLE_ALREADY_EXISTS` y `VALIDATION_FAILED`.

## Crear una ubicación

```http
POST /api/v1/admin/locations
```

```json
{
  "code": "C-01-01",
  "pickSequence": 30101
}
```

Resultado correcto: `201`. Conserve `id`, `code`, `qrValue` y `pickSequence`.

Restricciones:

- `code`: patrón `^[A-Z]+-[0-9]{2}-[0-9]{2}$`;
- `pickSequence`: entero positivo y único.

Errores principales: `LOCATION_ALREADY_EXISTS`,
`PICK_SEQUENCE_ALREADY_EXISTS` y `VALIDATION_FAILED`.

## Ajustar existencias

```http
POST /api/v1/admin/stock/adjustments
```

```json
{
  "articleSku": "ART-001",
  "locationCode": "A-01-01",
  "quantityDelta": -3,
  "reason": "Conteo cíclico CC-2026-07-15; segundo conteo confirmado"
}
```

Resultado correcto: `201`. Registre `movementId`, `quantityDelta` y
`resultingQuantity`.

`quantityDelta` no puede ser cero. El resultado no puede ser negativo.

Errores principales: `ARTICLE_NOT_FOUND`, `LOCATION_NOT_FOUND`,
`NEGATIVE_RESULTING_STOCK` y `VALIDATION_FAILED`.

**Solicitud no idempotente:** si la conexión se interrumpe después de enviar el
ajuste, no lo repita hasta comprobar el historial. Una repetición podría aplicar el
mismo cambio dos veces.

## Crear una orden

```http
POST /api/v1/admin/orders
```

```json
{
  "orderNumber": "WEB-2026-00042",
  "lines": [
    {"lineNumber": 1, "articleSku": "ART-001", "quantity": 25},
    {"lineNumber": 2, "articleSku": "ART-003", "quantity": 2}
  ]
}
```

Resultado correcto: `201`. Compruebe `orderNumber`, `state`, `lineCount` y
`taskCount`.

Restricciones:

- `orderNumber`: 1 a 50 caracteres, letras mayúsculas, números, `-` o `_`;
- `lines`: al menos una;
- `lineNumber`: entero mayor o igual que 1 y único dentro de la orden;
- `articleSku`: artículo existente;
- `quantity`: entero mayor o igual que 1.

Errores principales: `ORDER_ALREADY_EXISTS`, `ARTICLE_NOT_FOUND`,
`INSUFFICIENT_AVAILABLE_STOCK` y `VALIDATION_FAILED`.

La operación es completa: ante cualquier error no se conserva una orden parcial.

## Consultar una orden

```http
GET /api/v1/admin/orders/{orderNumber}
```

Resultado correcto: `200`. La respuesta contiene orden, líneas, cantidades
solicitadas y preparadas, y tareas. `ORDER_NOT_FOUND` devuelve `404`.

## Consultar tareas

```http
GET /api/v1/admin/tasks
```

Filtros opcionales:

```text
state=AVAILABLE
state=ASSIGNED
orderNumber=<numero>
assignedUsername=<usuario>
stuckOnly=true
```

Los filtros `state` pueden repetirse. Los estados admitidos son `AVAILABLE`,
`ASSIGNED`, `LOCATION_CONFIRMED`, `ARTICLE_CONFIRMED`, `BLOCKED` y `COMPLETED`.
La respuesta contiene como máximo 500 tareas.

## Bloquear una tarea

```http
POST /api/v1/admin/tasks/{taskId}/block
```

```json
{
  "reason": "Faltan 3 unidades de ART-001 en A-01-01; segundo conteo confirmado"
}
```

Resultado correcto: `200` con estado `BLOCKED`. La razón no puede estar vacía.

Errores principales: `TASK_NOT_FOUND`, `INVALID_TASK_STATE` y
`VALIDATION_FAILED`.

## Reanudar una tarea

```http
POST /api/v1/admin/tasks/{taskId}/resume
```

No lleva contenido. Resultado correcto: `200` con estado `AVAILABLE`.

Errores principales: `TASK_NOT_FOUND` e `INVALID_TASK_STATE`.

## Obtener etiquetas

```http
GET /api/v1/admin/labels/locations/{code}/pdf
GET /api/v1/admin/labels/locations/{code}/png
GET /api/v1/admin/labels/articles/{sku}/pdf
GET /api/v1/admin/labels/articles/{sku}/png
```

Guarde la respuesta como archivo binario; no como texto. Los tipos de contenido son
`application/pdf` e `image/png`.

Errores principales: `LOCATION_NOT_FOUND` y `ARTICLE_NOT_FOUND`.

## Interpretar errores

Los errores de aplicación usan `application/problem+json` e incluyen:

- `status`: código HTTP;
- `code`: código estable del problema;
- `detail`: explicación segura cuando corresponde;
- `correlationId`: identificador para soporte;
- `fields`: campos incorrectos cuando existe un error de validación.

| Estado | Significado general | Acción |
|---:|---|---|
| `400` | Solicitud mal formada | Revise la estructura JSON |
| `401` | Sesión inválida, vencida o cerrada | Inicie sesión nuevamente |
| `403` | Cuenta sin permiso | Deténgase; no cambie de cuenta sin autorización |
| `404` | Registro no encontrado | Compruebe el identificador |
| `409` | Conflicto con el estado o un registro existente | Consulte el estado actual antes de decidir |
| `422` | Datos no válidos | Revise `fields` y corrija únicamente esos datos |
| `500` | Falla no esperada | Conserve `correlationId` y avise a soporte |

## Evidencia mínima

Conserve sin secretos:

- fecha y hora UTC;
- ambiente y versión del sistema;
- usuario que ejecutó la acción;
- método y ruta solicitados;
- `X-Correlation-Id` o `correlationId` devuelto;
- datos de negocio no sensibles;
- estado HTTP y código de resultado;
- identificadores creados o modificados.
