# HHT and administration REST contract

**Status: the `/api/v1` HHT/admin surface (Phase 7) is implemented and evidenced; the label and dashboard endpoints (Phase 8) below are implementation in progress.**
Contract version: `v1`  
Base URL on the LAN: `http://<wms-host>:8080/api/v1`  
Media type: `application/json`  
Timestamps: UTC ISO-8601 strings

This contract is intentionally small. After `v1` is implemented and published,
additive response fields may be introduced, but existing fields, paths, state
names, and error codes must not change within that version. The approved design
defaults are recorded in `docs/research/phase-3-mvp-recommendation.md`.

## Common rules

Except for login and health checks, send:

```http
Authorization: Bearer <opaque-token>
Accept: application/json
```

A client may send `X-Correlation-Id` as a UUID. The server echoes it; if omitted, the server creates one. Tokens, passwords, and password hashes must never be logged.

QR payloads are exact and case-sensitive:

- Location: `LOC:A-01-02`
- Article: `ART:ART-001`

### Error body

All application errors use RFC 9457 `application/problem+json`. The required
members are `type`, `title`, `status`, `code`, and `correlationId`; `detail`,
`instance`, and endpoint-approved safe extensions are optional.

```json
{
  "type": "https://warehouse.example/problems/wrong-location",
  "title": "Wrong location",
  "status": 409,
  "code": "WRONG_LOCATION",
  "detail": "Scanned location does not match the assigned task.",
  "correlationId": "450ad1c5-6006-4c98-b48f-3e92a9db6ae7",
  "taskId": 101,
  "expectedLocationCode": "A-01-02",
  "scannedQrValue": "LOC:A-01-01"
}
```

Endpoint-approved extensions must not expose credentials, tokens, password
hashes, arbitrary request bodies, or internal exception details. Validation
errors use status `422`, code `VALIDATION_FAILED`, and a `fields` object.
Malformed JSON uses `400` and `MALFORMED_REQUEST`.

Common bearer-token errors are `401 INVALID_TOKEN`, `401 TOKEN_EXPIRED`, and
`401 TOKEN_REVOKED`. An authenticated user without the required role receives
`403 FORBIDDEN`.

## Authentication

### `POST /auth/login`

Authenticates one active picker or administrator and registers which known HHT is being used.

Request:

```json
{
  "username": "picker01",
  "password": "picker123",
  "deviceCode": "HHT-PI-01"
}
```

Response `200 OK`:

```json
{
  "token": "wms_5bnWl7N9uHkJv3...",
  "tokenType": "Bearer",
  "expiresAt": "2026-07-11T22:23:08Z",
  "user": {
    "id": 2,
    "username": "picker01",
    "role": "PICKER"
  },
  "device": {
    "id": 1,
    "code": "HHT-PI-01"
  }
}
```

Errors:

- `401 INVALID_CREDENTIALS`
- `403 USER_INACTIVE`
- `403 DEVICE_INACTIVE`
- `404 DEVICE_NOT_REGISTERED`
- `409 DEVICE_ASSIGNMENT_CONFLICT` when the device currently has an active task for another user

### `POST /auth/logout`

Revokes the current token. The request has no body. Returns `204 No Content`. Repeating logout with the same token also returns `204`.

## HHT picking workflow

Valid progression:

`AVAILABLE → ASSIGNED → LOCATION_CONFIRMED → ARTICLE_CONFIRMED → COMPLETED`

Orders and lines use the states `OPEN`, `IN_PROGRESS`, and `COMPLETED`. An
order or line becomes `IN_PROGRESS` when its first task is claimed; a line
completes only when all its tasks complete, and an order completes only when
all its lines complete.

An administrator may move an uncompleted task to `BLOCKED` with a required
reason and later resume it to `AVAILABLE` through the administration recovery
endpoints. The HHT has no skip, block, or resume operation.

Wrong scans and rejected quantities leave the task in their prior state. A
repeated correct scan returns the current task representation with
`replayed: true` and never regresses state. Final confirmation requires a
client-generated UUID; repeating that UUID with the same canonical payload
returns the original result, while a different payload returns
`409 CONFIRMATION_ID_REUSED`.

### `GET /hht/tasks/next`

Returns the caller's current active task. If none exists, atomically claims the next `AVAILABLE` task in global FIFO order: order creation time, order-line number, then task sequence.

Response `200 OK`:

```json
{
  "id": 101,
  "state": "ASSIGNED",
  "orderNumber": "DEMO-1001",
  "lineNumber": 1,
  "taskSequence": 1,
  "location": {
    "code": "A-01-01"
  },
  "article": {
    "sku": "ART-001",
    "description": "Black basic T-shirt"
  },
  "quantity": 20,
  "assignedAt": "2026-07-11T14:24:00Z"
}
```

If no work is available, returns `204 No Content`. Concurrent requests never assign the same task, and one user never receives a second active task. A detected uniqueness/ownership race that cannot be safely retried by the server returns `409 TASK_ASSIGNMENT_CONFLICT`; the HHT should request the next task again.

### `POST /hht/tasks/{taskId}/scan-location`

Request:

```json
{
  "qrValue": "LOC:A-01-01"
}
```

Response `200 OK`:

```json
{
  "taskId": 101,
  "state": "LOCATION_CONFIRMED",
  "locationCode": "A-01-01",
  "confirmedAt": "2026-07-11T14:25:12Z"
}
```

Errors:

- `404 TASK_NOT_FOUND`
- `409 TASK_NOT_ASSIGNED_TO_USER`
- `409 INVALID_TASK_STATE`
- `409 WRONG_LOCATION`

### `POST /hht/tasks/{taskId}/scan-article`

Request:

```json
{
  "qrValue": "ART:ART-001"
}
```

Response `200 OK`:

```json
{
  "taskId": 101,
  "state": "ARTICLE_CONFIRMED",
  "articleSku": "ART-001",
  "confirmedAt": "2026-07-11T14:25:28Z"
}
```

Errors:

- `404 TASK_NOT_FOUND`
- `409 TASK_NOT_ASSIGNED_TO_USER`
- `409 INVALID_TASK_STATE`
- `409 WRONG_ARTICLE`

### `POST /hht/tasks/{taskId}/confirm`

`confirmationId` is generated once by the HHT and retained across network retries. A retry with the same ID and payload returns the same task, movement, quantity, stock balance, and completion timestamp without decrementing stock again. The nested order state is read at retry time and may have advanced from `IN_PROGRESS` to `COMPLETED`.

Request:

```json
{
  "confirmationId": "7a3d389f-9150-43ef-90e6-0955ea37d2a7",
  "quantity": 20
}
```

Response `200 OK`:

```json
{
  "taskId": 101,
  "state": "COMPLETED",
  "confirmedQuantity": 20,
  "movementId": 18,
  "remainingStock": 0,
  "order": {
    "number": "DEMO-1001",
    "state": "IN_PROGRESS"
  },
  "completedAt": "2026-07-11T14:26:03Z"
}
```

When this is the final task, `order.state` is `COMPLETED`.

Errors:

- `404 TASK_NOT_FOUND`
- `409 TASK_NOT_ASSIGNED_TO_USER`
- `409 INVALID_TASK_STATE`
- `409 CONFIRMATION_ID_REUSED` when the ID belongs to a different confirmation
- `409 INSUFFICIENT_STOCK`
- `422 QUANTITY_MISMATCH` when quantity differs from the task quantity

## Administration endpoints

Administration endpoints require an `ADMIN` token.

### `POST /admin/orders`

Creates the order, allocates all lines, and creates tasks in one transaction. An order is either fully accepted or not created. Allocation accounts for stock already allocated to unfinished tasks and splits across locations in ascending code order.

Request:

```json
{
  "orderNumber": "WEB-2026-00042",
  "lines": [
    { "lineNumber": 1, "articleSku": "ART-001", "quantity": 25 },
    { "lineNumber": 2, "articleSku": "ART-003", "quantity": 2 }
  ]
}
```

Response `201 Created` with `Location: /api/v1/admin/orders/WEB-2026-00042`:

```json
{
  "orderNumber": "WEB-2026-00042",
  "state": "OPEN",
  "createdAt": "2026-07-11T14:20:00Z",
  "lineCount": 2,
  "taskCount": 3
}
```

Errors: `409 ORDER_ALREADY_EXISTS`, `409 INSUFFICIENT_AVAILABLE_STOCK`, `404 ARTICLE_NOT_FOUND`, and `422 VALIDATION_FAILED`.

### `GET /admin/orders/{orderNumber}`

Response `200 OK`:

```json
{
  "orderNumber": "WEB-2026-00042",
  "state": "IN_PROGRESS",
  "createdAt": "2026-07-11T14:20:00Z",
  "completedAt": null,
  "lines": [
    {
      "lineNumber": 1,
      "articleSku": "ART-001",
      "requestedQuantity": 25,
      "pickedQuantity": 20,
      "state": "IN_PROGRESS",
      "tasks": [
        { "id": 101, "locationCode": "A-01-01", "quantity": 20, "state": "COMPLETED" },
        { "id": 102, "locationCode": "A-01-02", "quantity": 5, "state": "AVAILABLE" }
      ]
    }
  ]
}
```

Returns `404 ORDER_NOT_FOUND` when absent.

### `GET /admin/tasks?state=AVAILABLE&state=ASSIGNED`

Returns a JSON array of task summaries for the dashboard. Optional filters are repeated `state` values, `orderNumber`, `assignedUsername`, and `stuckOnly`. Allowed states are `AVAILABLE`, `ASSIGNED`, `LOCATION_CONFIRMED`, `ARTICLE_CONFIRMED`, `BLOCKED`, and `COMPLETED`. This PoC returns at most 500 rows ordered by last transition time descending; it returns `200 OK` with an empty array when no task matches.

Response `200 OK`:

```json
[
  {
    "id": 101,
    "taskNumber": "WEB-2026-00042-001-01",
    "state": "ASSIGNED",
    "orderNumber": "WEB-2026-00042",
    "lineNumber": 1,
    "locationCode": "A-01-01",
    "articleSku": "ART-001",
    "quantity": 20,
    "assignedUsername": "picker01",
    "deviceCode": "HHT-PI-01",
    "lastTransitionAt": "2026-07-11T14:24:00Z",
    "stuck": false
  }
]
```

### `POST /admin/tasks/{taskId}/block`

Administrative recovery per ADR 0004. Moves an `AVAILABLE`, `ASSIGNED`, `LOCATION_CONFIRMED`, or `ARTICLE_CONFIRMED` task to `BLOCKED`. Blocking preserves the task's allocation and physical stock, releases any user/device assignment, clears scan confirmations, and appends one audit transition with the required reason.

Request:

```json
{ "reason": "Damaged carton at A-01-01; awaiting supervisor recount" }
```

Response `200 OK`:

```json
{
  "taskId": 102,
  "state": "BLOCKED",
  "reason": "Damaged carton at A-01-01; awaiting supervisor recount",
  "blockedAt": "2026-07-11T15:02:44Z"
}
```

Errors:

- `404 TASK_NOT_FOUND`
- `409 INVALID_TASK_STATE` when the task is already `BLOCKED` or `COMPLETED`
- `422 VALIDATION_FAILED` when `reason` is missing or blank

### `POST /admin/tasks/{taskId}/resume`

Returns a `BLOCKED` task to `AVAILABLE`. Resume preserves the FIFO and allocation fields, leaves stock unchanged, and appends one audit transition; the task becomes claimable again in normal global FIFO order. The request has no body.

Response `200 OK`:

```json
{
  "taskId": 102,
  "state": "AVAILABLE",
  "resumedAt": "2026-07-11T15:40:10Z"
}
```

Errors:

- `404 TASK_NOT_FOUND`
- `409 INVALID_TASK_STATE` when the task is not `BLOCKED`

### `POST /admin/articles`

Creates article master data. `sku` is 1–50 uppercase letters, digits, hyphens, or underscores; `description` is 1–200 characters. The server derives the QR value as `ART:<sku>`.

Request:

```json
{ "sku": "ART-005", "description": "Red knitted cardigan" }
```

Response `201 Created`:

```json
{
  "id": 5,
  "sku": "ART-005",
  "description": "Red knitted cardigan",
  "qrValue": "ART:ART-005",
  "active": true
}
```

Errors include `409 ARTICLE_ALREADY_EXISTS` and `422 VALIDATION_FAILED`.

### `POST /admin/locations`

Creates a pick location. `code` must match `^[A-Z]+-[0-9]{2}-[0-9]{2}$`; `pickSequence` is a unique positive integer reserved for future route optimization. The proposed allocator deliberately uses location-code order, not `pickSequence`. The server derives the QR value as `LOC:<code>`.

Request:

```json
{ "code": "C-01-01", "pickSequence": 30101 }
```

Response `201 Created`:

```json
{
  "id": 6,
  "code": "C-01-01",
  "qrValue": "LOC:C-01-01",
  "pickSequence": 30101,
  "active": true
}
```

Errors include `409 LOCATION_ALREADY_EXISTS`, `409 PICK_SEQUENCE_ALREADY_EXISTS`, and `422 VALIDATION_FAILED`.

### `POST /admin/stock/adjustments`

Posts a signed stock delta and appends an `ADJUSTMENT` movement in the same transaction.

Request:

```json
{
  "articleSku": "ART-001",
  "locationCode": "A-01-01",
  "quantityDelta": 4,
  "reason": "Cycle count correction CC-2026-07-11"
}
```

Response `201 Created`:

```json
{
  "movementId": 19,
  "articleSku": "ART-001",
  "locationCode": "A-01-01",
  "quantityDelta": 4,
  "resultingQuantity": 24
}
```

Errors include `404 ARTICLE_NOT_FOUND`, `404 LOCATION_NOT_FOUND`, `409 NEGATIVE_RESULTING_STOCK`, and `422 VALIDATION_FAILED`.

### `GET /admin/labels/locations/{code}/png`, `GET /admin/labels/locations/{code}/pdf`

### `GET /admin/labels/articles/{sku}/png`, `GET /admin/labels/articles/{sku}/pdf`

Printable QR labels per ADR 0007. The PNG is a deterministic 300×300 image (error correction M, four-module quiet zone, black-on-white) encoding the exact `LOC:<code>` or `ART:<sku>` payload the HHT scan endpoints accept. The PDF is a single-label A4 sheet with the same QR code plus human-readable text ("Location \<code\>" / "Article \<sku\>") in an embedded, licence-vendored font (`src/main/resources/fonts/liberation-sans/`). Both formats are byte-identical across repeated generation for the same code/SKU. Responses are `image/png` or `application/pdf`; errors are `404 LOCATION_NOT_FOUND` or `404 ARTICLE_NOT_FOUND`.

## Admin dashboard (not part of `/api/v1`)

`GET /dashboard` is a server-rendered, session-authenticated page (form login at `GET`/`POST /login`, `ADMIN` role only) showing live task state: task number, state, order, line, location, article, quantity, assigned user/device, last transition time, and a stuck flag (per `wms.task.stuck-threshold`). It embeds a small script that polls `GET /dashboard/api/tasks` (same JSON shape as `GET /admin/tasks`, no filters) every `wms.dashboard.poll-interval` (default 2 s) and replaces the table body without a full page reload. This session/cookie/CSRF-protected surface is separate from the stateless `/api/v1` bearer-token API (ADR 0007); unauthenticated requests redirect to `/login`, and an authenticated non-admin user receives `403`.

## Health endpoint

`GET /actuator/health` is outside `/api/v1`, needs no token, and returns `200` only when the application and database are healthy. It must not expose credentials or detailed internals.
