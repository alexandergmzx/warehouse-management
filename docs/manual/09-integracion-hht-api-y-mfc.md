# Guía de integración de terminal, API y MFC

**Público:** integración, desarrollo de la terminal, soporte de red y revisión
técnica\
**Nivel:** técnico explicado en lenguaje directo\
**Estado:** borrador inicial; la conexión local HHT–WMS está verificada y la
prueba con dispositivo y red inalámbrica reales está pendiente

## Objetivo

Esta guía explica cómo conectar otro cliente con el WMS sin romper el flujo de
preparación. También delimita la extensión MFC para evitar que una posibilidad
futura se confunda con una conexión ya disponible.

Los términos técnicos necesarios se explican en el
[glosario sencillo](anexos/glosario-sencillo.md).

## Estado real de las integraciones

| Integración | Estado actual | Consecuencia |
|---|---|---|
| HandheldPi con la API `v1` | Implementada y probada en el mismo equipo | Puede usarse para desarrollo y pruebas controladas |
| HandheldPi por la red inalámbrica y dispositivo físico | Pendiente de prueba completa | No debe declararse aceptada para el almacén |
| Herramientas administrativas con la API `v1` | Implementadas como interfaz REST | Requieren cuenta `ADMIN`; no forman parte del flujo HHT |
| Tablero web | Solo consulta | No es una interfaz de integración ni permite recuperar tareas |
| MFC | Existe el punto de extensión y un adaptador que solo registra un log | No existe conexión TCP, telegrama, cola ni entrega a un MFC |

## Fuentes que debe consultar

Use estas fuentes en este orden:

1. [`README.md`](../../README.md), para conocer el estado entregado.
2. Evidencia en [`docs/evidence/`](../evidence/) y el
   [reporte de pruebas ejecutadas](../executed-test-report.md), para saber qué se
   comprobó realmente.
3. [`API.md`](../../API.md), contrato oficial del WMS.
4. [`HandheldPi/API.md`](../../../HandheldPi/API.md), comportamiento particular
   del cliente HHT.
5. [Arquitectura](../architecture.md) y
   [ADR 0007](../decisions/0007-dashboard-label-and-mfc-contracts.md), para el
   límite MFC.

Si el contrato oficial y una nota del cliente no coinciden, detenga el cambio y
resuelva la diferencia. No haga que el cliente “adivine” la respuesta.

## Límites que no se deben cruzar

- La terminal se comunica únicamente por la API REST.
- La terminal nunca lee ni cambia PostgreSQL directamente.
- Un cliente HHT no debe utilizar los endpoints administrativos.
- El tablero es para consulta humana; no debe analizarse como si fuera una API.
- Los códigos QR de ubicación y artículo son exactos y distinguen mayúsculas de
  minúsculas.
- Una respuesta `409` nunca debe interpretarse de forma general como éxito.
- No se debe introducir transporte MFC dentro de la transacción actual de
  confirmación.

## Flujo completo de una terminal

```text
Gafete y PIN
      |
      v
Inicio de sesión ──> Obtener o recuperar tarea activa
                           |
                           v
                    Escanear ubicación
                           |
                           v
                     Escanear artículo
                           |
                           v
                    Confirmar cantidad
                           |
                           v
                WMS descuenta existencias
                y completa tarea/orden
```

El gafete usa la forma `OP:<usuario>`, pero esta es una convención de la
terminal. El WMS recibe `username`, `password` y `deviceCode`; nunca recibe la
imagen ni el contenido completo del gafete.

Los códigos operativos admitidos son:

- ubicación: `LOC:<código>`, por ejemplo `LOC:A-01-01`;
- artículo: `ART:<sku>`, por ejemplo `ART:ART-001`;
- no se admite un EAN sin el prefijo y la identidad aprobada.

## Configuración mínima de HandheldPi

Antes de conectar una terminal, confirme:

| Dato | Ejemplo | Regla |
|---|---|---|
| Identificador del equipo | `HHT-PI-01` | Debe existir y estar activo en el WMS |
| Sitio | `MAD-01` | Identifica la instalación de la terminal |
| Cliente | `http` | `mock` es solo para pruebas sin WMS real |
| Dirección del WMS | `http://192.168.1.50:8080` | No incluya `/api/v1`; el cliente lo añade |
| Tiempo de espera | `5.0` segundos | Debe aprobarse según la red real |
| Reintento | `30.0` segundos | Controla cuándo vuelve a intentar enviar pendientes |
| Longitud del PIN | `4` | Debe coincidir con la capacitación y las cuentas usadas |
| Archivo de cola | `/var/lib/hht/queue.db` | Debe conservarse y tener permisos correctos |
| Archivo de logs | `/var/log/hht/hht.jsonl` | Debe rotarse y protegerse |

No copie la configuración de bucle local a un dispositivo real: el ejemplo local
usa `localhost`, terminal simulada y rutas de prueba.

## Autenticación y sesión

1. La terminal lee el usuario del gafete.
2. La persona introduce su PIN.
3. La terminal envía esos datos y su `deviceCode` al WMS.
4. El WMS devuelve una sesión vinculada a esa persona y terminal.
5. La terminal envía el token en las solicitudes posteriores.
6. Al cerrar sesión, el WMS revoca el token.

Nunca registre el PIN, la clave ni el encabezado `Authorization`. La terminal no
permite cerrar sesión mientras haya operaciones pendientes, porque necesita la
sesión para enviarlas.

Cuando el WMS responde `INVALID_TOKEN`, `TOKEN_EXPIRED` o `TOKEN_REVOKED`, la
terminal vuelve al inicio de sesión y conserva la cola. Después de una nueva
autenticación puede continuar el envío.

## Endpoints utilizados por la terminal

| Acción | Método y ruta | Respuesta normal |
|---|---|---|
| Iniciar sesión | `POST /api/v1/auth/login` | `200` con sesión |
| Cerrar sesión | `POST /api/v1/auth/logout` | `204` |
| Obtener tarea | `GET /api/v1/hht/tasks/next` | `200` con tarea o `204` sin tarea |
| Confirmar ubicación | `POST /api/v1/hht/tasks/{id}/scan-location` | `200` |
| Confirmar artículo | `POST /api/v1/hht/tasks/{id}/scan-article` | `200` |
| Confirmar cantidad | `POST /api/v1/hht/tasks/{id}/confirm` | `200` |
| Comprobar disponibilidad | `GET /actuator/health` | `200` si aplicación y base están disponibles |

La [referencia técnica de HHT](anexos/referencia-integracion-hht.md) contiene
ejemplos de solicitudes y el tratamiento de errores.

## Recuperación de una tarea interrumpida

`GET /hht/tasks/next` devuelve primero la tarea activa de la misma persona. La
terminal debe mostrar la pantalla correspondiente al estado recibido:

| Estado recibido | Acción que falta |
|---|---|
| `ASSIGNED` | Ir a la ubicación y escanearla |
| `LOCATION_CONFIRMED` | Escanear el artículo |
| `ARTICLE_CONFIRMED` | Contar y confirmar la cantidad |

La terminal no debe inventar un paso ya realizado ni hacer retroceder el estado.

## Trabajo sin conexión

La terminal solo puede obtener una tarea cuando el WMS está disponible. Después
de obtenerla, puede validar localmente los dos códigos y guardar estas acciones en
orden:

```text
scan-location → scan-article → confirm
```

Al recuperar la conexión, envía la cola de más antiguo a más reciente.

| Respuesta durante el envío | Tratamiento correcto |
|---|---|
| Error de red, espera agotada o `5xx` | Conservar como pendiente y reintentar después |
| Token inválido, vencido o revocado | Conservar como pendiente y pedir inicio de sesión |
| Otro error `4xx` | Marcar la acción y las siguientes de esa tarea como rechazadas; mostrar `SYNC FAILED` |
| Éxito | Marcar como enviada y conservar el registro local para auditoría |

Mientras exista algo pendiente, la terminal no debe pedir otra tarea ni cerrar la
sesión. Un rechazo no se elimina automáticamente: se conserva para investigación
y debe atenderlo el supervisor.

No borre manualmente `queue.db` para resolver una incidencia. Esa acción destruye
evidencia y puede ocultar una preparación no sincronizada.

## Repetición segura e idempotencia

Los escaneos correctos pueden repetirse sin hacer retroceder la tarea. El WMS
responde como reproducción segura.

Para la confirmación final:

1. genere un UUID una sola vez;
2. guárdelo con la cantidad antes de enviar;
3. use el mismo UUID y la misma cantidad en todos los reintentos;
4. considere éxito únicamente una respuesta de éxito;
5. trate `409 CONFIRMATION_ID_REUSED` como un conflicto real.

Generar un nuevo UUID después de una espera agotada puede provocar una segunda
intención distinta. Cambiar la cantidad manteniendo el UUID también es inválido.

## Seguimiento por identificador de correlación

Cada solicitud HHT genera un UUID nuevo en `X-Correlation-Id`. El WMS lo devuelve
en el encabezado de respuesta y lo incluye en sus logs. El cliente actual conserva
ese valor explícitamente en su log `wms_rejected` cuando recibe un rechazo. Soporte
puede usarlo para unir:

- el rechazo mostrado por la terminal;
- el evento `wms_rejected` del log HHT;
- la solicitud registrada por el WMS;
- las transiciones o movimientos relacionados.

En respuestas exitosas, el cliente actual no registra el identificador de
correlación. Es posible investigar la operación con tarea, orden,
`confirmationId` y hora, pero la unión completa por correlación está pendiente de
una mejora específica. No afirme que existe trazabilidad HHT–WMS completa para
éxitos.

El identificador de correlación sigue una solicitud; el `confirmationId` identifica
una confirmación y sus reintentos. No son intercambiables.

## Red y ambientes

HTTP sin cifrado se admite únicamente en desarrollo controlado y en una red de
confianza con alcance limitado. Para acceso no local en preproducción, la decisión
de seguridad exige HTTPS.

El proyecto actual no incluye terminación TLS. Por eso, antes de probar una
terminal física contra preproducción se debe proporcionar y verificar un punto
HTTPS, certificados, nombre de host, firewall y ruta de red. La ausencia de esa
capa bloquea la aceptación, no justifica rebajar la regla.

## Pruebas mínimas de una integración HHT

1. Pruebas unitarias del cliente y de la cola.
2. Pruebas HTTP contra respuestas normales y cada familia de error.
3. Integración de bucle local con WMS y PostgreSQL reales.
4. Interrupción y recuperación de red sin duplicar existencias.
5. Vencimiento o revocación de sesión con cola pendiente.
6. Rechazo de una acción almacenada y aparición de `SYNC FAILED`.
7. Prueba con dispositivo, escáner y red inalámbrica reales.
8. Revisión conjunta de un rechazo mediante `correlationId` y de un éxito mediante
   los identificadores actualmente disponibles.

Una prueba local no sustituye los pasos 7 y 8. Registre cada ejecución con la
[plantilla de integración](plantillas/lista-verificacion-integracion.md).

## Cambiar el contrato HHT

Antes de cambiar una ruta, campo, estado o código:

1. identifique las versiones del WMS y de HandheldPi afectadas;
2. determine si el cambio es compatible dentro de `v1`;
3. actualice el contrato oficial antes o junto con el código;
4. actualice servidor, cliente, cola, pruebas y manuales como una unidad;
5. pruebe una actualización con operaciones locales pendientes;
6. conserve evidencia de integración real;
7. publique la combinación de versiones autorizada.

No cambie el significado de un campo o código existente dentro de `v1`. Prefiera
añadir campos opcionales cuando la compatibilidad lo permita.

## Extensión MFC: qué existe hoy

Cuando una orden cambia a `COMPLETED`, el dominio crea un evento con:

- `eventId`;
- `orderId`;
- `orderNumber`;
- `completedAt`.

El puerto `OrderCompletionPublisher` recibe ese evento. El único adaptador actual,
`NoopOrderCompletionPublisher`, escribe un log con `eventId` y `orderNumber` y
termina. La propiedad `WMS_MFC_ADAPTER` solo admite `noop`.

Esto demuestra dónde conectar una integración futura, no que exista entrega.

## Condiciones antes de implementar MFC real

El responsable debe aprobar, como mínimo:

- propósito, receptor y propietario del sistema MFC;
- formato, versión y codificación del mensaje;
- autenticación, cifrado y reglas de red;
- confirmación positiva, rechazo y tiempo de espera;
- idempotencia mediante `eventId`;
- reintentos, límite, pausa y recuperación;
- orden de entrega y tratamiento de mensajes atrasados;
- métricas, logs, alertas y retención;
- comportamiento si el WMS confirma la orden pero MFC no responde;
- decisión entre publicación posterior al commit y patrón outbox.

La llamada actual ocurre dentro de la transacción que completa la orden y es segura
porque el adaptador no hace red. Un adaptador real no debe bloquear esa transacción.
Use la [guía de preparación MFC](anexos/guia-futura-integracion-mfc.md) antes de
iniciar el diseño.

## Criterio de aceptación

Una integración puede aceptarse solo cuando:

- existe contrato versionado y responsable;
- las reglas de autenticación, repetición y recuperación están probadas;
- no se registran secretos;
- cliente y servidor son compatibles en la combinación publicada;
- se probó el ambiente y hardware que se pretende autorizar;
- soporte puede seguir un caso de extremo a extremo;
- riesgos y limitaciones pendientes están escritos y aceptados.
