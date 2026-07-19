# Mapa técnico del proyecto

## Paquetes de aplicación

| Paquete | Responsabilidad |
|---|---|
| `admin` | Órdenes, catálogo, ajustes, consulta de tareas y recuperación administrativa |
| `api` | Correlación, códigos de problema y tratamiento común de errores |
| `auth` | Login y logout de la API |
| `catalog` | Artículos y ubicaciones |
| `configuration` | Propiedades y validación segura de `preprod` |
| `dashboard` | Tablero HTML y consulta para actualización |
| `identity` | Usuarios, terminales, sesiones opacas e identidad autenticada |
| `inventory` | Existencias y movimientos inmutables |
| `label` | QR PNG y PDF deterministas |
| `mfc` | Adaptadores de finalización (`noop` y `telegram`), outbox de misiones, despachador y confirmaciones del WCS (ADR 0011) |
| `orders` | Órdenes, líneas y puerto de finalización |
| `picking` | Asignación, escaneos, confirmación y recuperación de tareas |
| `security` | Cadenas de seguridad API y tablero |

## Flujo de una solicitud

```text
Cliente
  → filtro de correlación
  → seguridad
  → controlador
  → servicio de aplicación y transacción
  → repositorios JPA/JDBC
  → PostgreSQL
  → respuesta o problema RFC 9457
```

El controlador no debe saltar el servicio para llamar un repositorio.

## Estados de tarea

```text
AVAILABLE
  → ASSIGNED
  → LOCATION_CONFIRMED
  → ARTICLE_CONFIRMED
  → COMPLETED
```

Recuperación:

```text
AVAILABLE / ASSIGNED / LOCATION_CONFIRMED / ARTICLE_CONFIRMED
  → BLOCKED
  → AVAILABLE
```

El bloqueo libera usuario/terminal, limpia escaneos y no cambia existencias. La
reanudación devuelve la tarea al orden normal.

## Estados de línea y orden

```text
OPEN → IN_PROGRESS → COMPLETED
```

Una línea termina al terminar todas sus tareas. Una orden termina al terminar todas
sus líneas.

## Datos principales

```text
customer_order
  └─ order_line
       └─ picking_task
            └─ task_transition  (histórico inmutable)

article + location
  └─ stock
       └─ stock_movement        (histórico inmutable)

app_user + device
  └─ auth_token
```

## Ubicaciones relevantes

| Elemento | Ruta |
|---|---|
| Inicio Spring Boot | `src/main/java/com/alexandergomez/wms/WarehouseManagementApplication.java` |
| Configuración común | `src/main/resources/application.yml` |
| Desarrollo | `src/main/resources/application-dev.yml` |
| Preproducción | `src/main/resources/application-preprod.yml` |
| Estructura Flyway | `src/main/resources/db/migration/` |
| Datos de demostración | `src/main/resources/db/devdata/` |
| Tablero | `src/main/resources/templates/dashboard.html` |
| Pruebas | `src/test/java/com/alexandergomez/wms/` |
| Contrato | `API.md` |
| Arquitectura | `docs/architecture.md` |
| Decisiones | `docs/decisions/` |
| Evidencia | `docs/evidence/` |

## Mapa de pruebas

| Área | Prueba principal |
|---|---|
| Migración y restricciones | `FlywayMigrationIT` |
| Persistencia/reconciliación | `PersistenceLayerIT` |
| Autenticación | `AuthApiIT` |
| Flujo correcto | `PickingApiIT` |
| Errores, concurrencia e idempotencia | `PickingNegativePathApiIT` |
| Asignación de orden | `OrderAllocationApiIT` |
| Ciclo de orden | `OrderLifecycleApiIT` |
| Bloqueo/reanudación | `TaskRecoveryApiIT` |
| Logs | `StructuredLoggingApiIT` |
| Configuración `preprod` | `PreprodConfigurationValidatorIT` |
| Etiquetas | `LabelApiIT` |
| Tablero | `DashboardApiIT` |
| Puerto MFC | `OrderCompletionSeamApiIT` |
| Misiones MFC (emisión, despacho, confirmación, arranque fallido) | `MfcTelegramLifecycleIT`, `TelegramFixturesIT`, `MissionDispatcherFailFastIT` |

## Documentos por tipo de cambio

| Cambio | Revisar y actualizar |
|---|---|
| Regla de almacén | Requisitos, ADR, API, pruebas, manual operativo |
| Endpoint o error | API, cliente, seguridad, pruebas, catálogo de errores |
| Base de datos | ADR, migración nueva, entidad, pruebas, diagnóstico y recuperación |
| Configuración | YAML, propiedades, matriz, validación, runbook y manual de operación |
| Log | evento, campos permitidos, prueba y guía de diagnóstico |
| Tablero | plantilla, seguridad, navegador real y manual de supervisor |
| Etiqueta | ADR/licencia, generación, determinismo, escaneo y manual administrativo |
| HandheldPi | contrato en ambos repositorios, pruebas automáticas, loopback y hardware |
| MFC | ADR 0011 y `TELEGRAMS.md` vigentes; revisar contrato versionado, migración, adaptador/despachador, pruebas, matriz de configuración, runbooks y manuales |
