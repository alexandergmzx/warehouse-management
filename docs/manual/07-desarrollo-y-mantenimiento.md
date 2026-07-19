# Guía de desarrollo y mantenimiento

**Público:** desarrollo, mantenimiento, revisión técnica y soporte avanzado\
**Nivel:** técnico\
**Estado:** borrador inicial; pendiente de revisión técnica

## Objetivo

Esta guía explica cómo cambiar el sistema sin romper sus reglas de almacén,
integridad, seguridad, operación o evidencia.

El proyecto es un monolito modular Java/Spring Boot. La simplicidad del despliegue no
elimina los límites entre módulos ni autoriza accesos directos a los datos.

## Fuentes de verdad

Consulte en este orden:

1. [`README.md`](../../README.md): estado actual y alcance entregado.
2. [`docs/executed-test-report.md`](../executed-test-report.md) y
   [`docs/evidence/`](../evidence/): lo realmente ejecutado y verificado.
3. [`CLAUDE.md`](../../CLAUDE.md): reglas de trabajo del repositorio.
4. [`API.md`](../../API.md): contrato REST implementado.
5. [`docs/architecture.md`](../architecture.md): límites y transacciones.
6. [`docs/decisions/`](../decisions/): decisiones de arquitectura.
7. Especificación funcional y trazabilidad: requisitos y pruebas esperadas.

Algunos ADR antiguos conservan en su cabecera “delivery pending”, aunque el README y
la evidencia posterior confirman la implementación. Utilice el ADR para conocer la
decisión y la evidencia para conocer el estado. No reabra una decisión solo por esa
cabecera desactualizada.

## Tecnología aprobada

| Componente | Regla actual |
|---|---|
| Lenguaje | Java solamente |
| Java | Cualquier OpenJDK 21 LTS actualizado; Temurin recomendado y usado por CI |
| Construcción | Maven 3.9.16 mediante el wrapper |
| Aplicación | Spring Boot 4.0.7 |
| Base de datos | PostgreSQL 17.10 |
| Migraciones | Flyway; propietario único de la estructura |
| Persistencia | JPA para operaciones comunes; JDBC/SQL enfocado para bloqueo y consultas especiales |
| Pruebas | JUnit, Failsafe y PostgreSQL Testcontainers |
| Calidad | Checkstyle y SpotBugs durante `verify` |

No cambie una versión fijada sin actualizar o crear el ADR correspondiente. No
introduzca Kotlin sin una decisión de arquitectura.

## Preparar el ambiente de desarrollo

El propietario instala Java y Docker. El proyecto proporciona Maven Wrapper.

En Linux:

```bash
java -version
./mvnw -v
docker info --format '{{.ServerVersion}}'
docker compose up -d
./mvnw spring-boot:run
```

En Windows:

```powershell
java -version
.\mvnw.cmd -v
docker info --format "{{.ServerVersion}}"
docker compose up -d
.\mvnw.cmd spring-boot:run
```

Compruebe:

```text
http://localhost:8080/actuator/health
```

El perfil predeterminado es `dev` e incluye datos de demostración. No utilice esos
usuarios ni claves fuera de desarrollo.

## Uso desde un IDE

- Importe el `pom.xml` como proyecto Maven existente.
- Use Maven y Git como fuentes portátiles de verdad.
- No genere un segundo proyecto dentro del repositorio.
- No dependa de una configuración exclusiva del IDE para construir o probar.
- No confirme `.idea/`, configuraciones locales, rutas de equipo o secretos sin una
  decisión explícita.
- Mantenga el proyecto utilizable desde IntelliJ IDEA y VS Code.

## Estructura general

```text
src/main/java/          aplicación
src/main/resources/     configuración, migraciones, plantilla y fuente de etiquetas
src/test/java/          pruebas de integración
docs/                   arquitectura, decisiones, diagnóstico, pruebas y evidencia
.github/workflows/      verificación continua
compose.yaml            PostgreSQL local
```

El [Mapa técnico del proyecto](anexos/mapa-tecnico-del-proyecto.md) describe cada
paquete y estado.

## Reglas de dependencia

- Los controladores llaman servicios de aplicación, no repositorios directamente.
- Los límites de transacción pertenecen a servicios.
- El dominio no depende de DTO HTTP, detalles de PostgreSQL o transporte MFC.
- Use JPA para persistencia ordinaria.
- Use JDBC o SQL nativo únicamente para necesidades explícitas de bloqueo,
  asignación, FIFO, reconciliación o consultas especializadas.
- Mantenga una dirección clara de dependencias entre catálogo, inventario, órdenes,
  preparación, identidad, operaciones y MFC.

Una implementación más corta no justifica romper estos límites.

## Reglas de negocio que no deben cambiarse implícitamente

1. La confirmación exige la cantidad exacta.
2. La terminal no permite saltar tareas.
3. La asignación sigue FIFO global por orden, línea y secuencia.
4. Una persona o terminal tiene como máximo una tarea activa.
5. Las líneas se dividen entre ubicaciones en orden ascendente por código.
6. Las existencias se reducen solo al confirmar correctamente.
7. Existencias, movimiento, tarea, línea y orden cambian en una transacción.
8. Los movimientos y transiciones son históricos e inmutables.
9. La terminal usa REST y nunca accede directamente a PostgreSQL.
10. La integración MFC vive detrás del puerto de finalización; el transporte
    lo fija un ADR (hoy: outbox + HTTP, ADR 0011) y un socket TCP crudo
    sigue fuera de alcance.

Cambiar una de estas reglas exige autorización del responsable, actualización de
requisitos, ADR cuando corresponda, contrato, pruebas y manuales.

## Transacciones y concurrencia

### Creación de orden

- Bloquea las filas de existencias candidatas en un orden estable.
- Calcula disponibilidad restando reservas de tareas sin terminar.
- Crea orden, líneas y tareas completas o no crea nada.
- Divide cantidades por código de ubicación ascendente.

### Asignación de tarea

- Usa PostgreSQL `READ COMMITTED` y bloqueo explícito.
- Selecciona FIFO con `FOR UPDATE ... SKIP LOCKED`.
- La tarea más antigua bloqueada puede ser omitida temporalmente; por eso existe la
  observación de tareas atascadas.
- Las restricciones de base de datos protegen una tarea activa por persona/terminal.

### Confirmación

- Bloquea la tarea y comprueba propietario, estado e idempotencia.
- Bloquea las existencias.
- Actualiza existencias, tarea, línea y orden.
- Inserta exactamente un movimiento.
- Todo confirma o todo se revierte.

No añada reintentos generales. Un reintento solo puede introducirse para una causa
transitoria conocida, limitada y demostrada.

## Base de datos y migraciones

- Flyway crea y modifica la estructura.
- Hibernate usa `ddl-auto: validate`; nunca `create` o `update`.
- Una migración aplicada a cualquier ambiente conservado es inmutable.
- Corrija hacia adelante con una versión nueva.
- No utilice `flyway repair` para ocultar un checksum diferente.
- Mantenga estructura común en `db/migration`.
- Mantenga datos exclusivamente de desarrollo en `db/devdata`.
- `preprod` nunca debe leer `db/devdata`.
- Una migración de datos de desarrollo también debe tratarse como inmutable una vez
  aplicada.

Cuando cambie una entidad persistida:

1. diseñe primero la migración y sus restricciones;
2. revise compatibilidad hacia atrás;
3. cambie la entidad;
4. añada pruebas de migración e integridad;
5. compruebe un inicio contra base vacía y, cuando aplique, una base actualizada;
6. documente copia de seguridad y recuperación.

No modifique datos operativos mediante una migración improvisada para resolver una
incidencia.

## Seguridad

La aplicación tiene dos mecanismos separados:

- `/api/v1`: sesión opaca en encabezado Bearer, sin sesión de navegador;
- tablero: cookie de sesión, formulario y protección CSRF.

Reglas:

- `/api/v1/admin/**` requiere función `ADMIN`.
- Las claves usan Argon2id.
- Solo se guarda el hash SHA-256 de las sesiones opacas.
- La sesión está vinculada a usuario y terminal, vence y puede revocarse.
- Nunca registre claves, hashes, encabezados de autorización o cuerpos arbitrarios.
- Los errores usan RFC 9457, código estable y `correlationId`.
- No cambie el significado de un código dentro de `v1`.
- Todo acceso no local a `preprod` requiere HTTPS.

Un endpoint nuevo debe definir autorización, validación, error estable, idempotencia,
log seguro, prueba negativa y documentación.

## API y compatibilidad

`API.md` es el contrato de `/api/v1`.

Antes de cambiar una solicitud o respuesta:

1. identifique todos los clientes, incluido HandheldPi;
2. determine si el cambio es compatible;
3. preserve nombres, tipos y códigos existentes cuando sea posible;
4. diseñe idempotencia para acciones que cambian datos;
5. actualice cliente, servidor, pruebas y documentación como una unidad;
6. ejecute integración real, no solo una prueba de controlador.

Una respuesta `409` no significa automáticamente duplicado aceptable. Cada código
tiene una semántica propia.

## Configuración

- Las propiedades de operación se leen al iniciar.
- Todo cambio requiere reinicio.
- Las duraciones principales están enlazadas a propiedades validadas.
- `preprod` valida URL, usuario y clave antes de crear la conexión.
- La clave pública de desarrollo se rechaza en `preprod`.
- Cada parámetro debe documentar propietario, valor predeterminado, ambiente,
  sensibilidad y reinicio.
- No agregue un valor de producción inseguro como predeterminado.

Actualice [`docs/configuration-matrix.md`](../configuration-matrix.md) al añadir o
cambiar una propiedad.

## Logs y observabilidad

- Emita una línea JSON por evento.
- Use `correlationId` durante toda la solicitud.
- Añada únicamente campos permitidos.
- Incluya identificadores de orden, tarea, usuario/terminal, artículo, ubicación y
  movimiento cuando correspondan.
- Nunca incluya secretos.
- Distinga rechazo esperado de regla de negocio y excepción inesperada.
- Actualice la guía de análisis con cada evento nuevo.

Limitación actual: cada línea no incluye la versión/configuración desplegada. Un
cambio futuro para resolverlo debe tener prueba y documentación.

## Tablero y etiquetas

El tablero usa Thymeleaf y JavaScript pequeño dentro de una plantilla. No introduzca
una aplicación web separada sin una decisión de alcance.

Las etiquetas deben ser deterministas:

- mismo contenido produce los mismos bytes;
- QR negro sobre blanco y contenido exacto;
- PDF utiliza la fuente autorizada incluida;
- pruebas comparan generación repetida y lectura.

No cambie librería, geometría o fuente sin revisar ADR 0007, licencias y evidencia.

## Extensión MFC

`OrderCompletionPublisher` es el puerto del dominio. Existen dos adaptadores
(`WMS_MFC_ADAPTER`): `noop`, el predeterminado, que solo escribe un log, y
`telegram` (ADR 0011), que implementa el envío real de misiones MFC. Cada
decisión que antes estaba abierta quedó registrada:

- serialización y contrato: `TELEGRAMS.md`, propiedad de este repositorio;
- transacción: patrón outbox — `publish()` solo inserta la fila
  `mfc_mission` `PENDING` dentro de la transacción que completa la orden;
  la red ocurre después, en el despachador programado;
- reintentos e idempotencia: intervalo fijo con límite de intentos y
  `eventId` como clave de repetición; agotados los intentos, la misión queda
  `FAILED` auditada;
- observabilidad: eventos estructurados con `missionId`/`eventId` (guía de
  logs) y libro de transiciones inmutable `mfc_mission_transition`;
- confirmaciones: REST entrante con rol `WCS` y repetición idempotente.

No amplíe estos límites como una implementación “pequeña”: cambiar el
transporte, añadir un socket TCP crudo o un intermediario de mensajes exige
un ADR nuevo. El dominio sigue sin depender de `mfc`: el puerto y el evento
son su única superficie.

Consulte la [guía de integración](09-integracion-hht-api-y-mfc.md) para los
contratos HHT y MFC vigentes; la
[preparación de MFC](anexos/guia-futura-integracion-mfc.md) queda como
registro histórico de las preguntas que ADR 0011 respondió.

## Pruebas

En Linux:

```bash
env -u SPRING_PROFILES_ACTIVE ./mvnw -B verify
```

En Windows, desde una PowerShell sin variables `preprod`:

```powershell
.\mvnw.cmd -B verify
```

La suite WMS actual utiliza pruebas de integración `*IT`. `mvn test` ejecuta cero
pruebas existentes; utilice siempre `verify`.

Para lógica nueva aislada se pueden añadir pruebas unitarias `*Test`, pero no deben
reemplazar la evidencia PostgreSQL/API necesaria.

Todo cambio debe considerar:

- camino correcto;
- errores y validación;
- permisos;
- concurrencia y bloqueo;
- idempotencia;
- rollback de la transacción;
- migración e integridad;
- logs y redacción de secretos;
- navegador o dispositivo real cuando corresponda.

## Integración continua

El flujo CI ejecuta Java 21 Temurin y `mvn verify` en `push`, solicitudes de cambio y
ejecución manual. Conserva reportes Surefire/Failsafe durante 14 días.

Un resultado local no sustituye CI, y CI no sustituye pruebas manuales de navegador,
red o hardware.

## Procedimiento para realizar un cambio

1. Revise el estado de Git y preserve cambios existentes.
2. Defina problema, alcance y criterio de aceptación.
3. Identifique requisitos, ADR, API, configuración y manuales afectados.
4. Decida si necesita autorización o una nueva decisión.
5. Diseñe migración, transacción, seguridad e idempotencia antes de escribir código.
6. Implemente el cambio más pequeño que cumple el objetivo.
7. Añada pruebas proporcionales al riesgo.
8. Actualice contrato, diagnóstico, configuración y manuales.
9. Ejecute `verify`.
10. Ejecute comprobaciones manuales necesarias.
11. Conserve evidencia con versión y configuración.
12. Revise diferencias, secretos, archivos locales y alcance accidental.

Use la [Lista de verificación de cambio](plantillas/lista-de-verificacion-de-cambio.md).

## Disciplina del repositorio

- No descarte cambios ajenos para facilitar una modificación.
- No confirme `.env`, claves, sesiones, dumps o rutas personales.
- No añada atribuciones de asistentes ni `Co-Authored-By`; Alexander Gomez es el
  único autor del proyecto.
- Evite reformateos no relacionados.
- Registre experimentos fallidos y riesgos sin ocultarlos.
- No declare terminado por compilación o por salida generada previamente.

## Definición de terminado

Un cambio está terminado cuando:

- cumple un criterio verificable;
- conserva todas las reglas no modificadas;
- tiene decisión aprobada para cambios de arquitectura o alcance;
- API y configuración están documentadas;
- migraciones son nuevas e inmutables;
- seguridad e idempotencia están revisadas;
- pruebas automáticas pasan;
- pruebas manuales necesarias pasan;
- diagnóstico y logs permiten operar el cambio;
- manuales por audiencia están actualizados;
- evidencia cita la versión exacta;
- no quedan secretos ni archivos locales;
- riesgos residuales están declarados.

## Limitaciones técnicas conocidas

- No existe perfil productivo.
- No existe terminación HTTPS incluida.
- No hay pantalla administrativa completa.
- No hay funciones API para administrar usuarios y terminales.
- No hay instalación de servicio Windows o `systemd` para el WMS.
- La aplicación no administra retención de logs.
- Las líneas de log no incorporan versión/configuración.
- La aceptación física HandheldPi por WiFi sigue pendiente.
- El bucle MFC se probó contra un sustituto del WCS
  (`scripts/wcs-standin/`), no contra `agv-fleet-controller` real; las
  misiones SORT están especificadas pero no implementadas.
- No hay funciones de lotes, series, caducidad, reposición o preparación parcial.

No resuelva una limitación implícitamente dentro de otro cambio. Trátela como alcance
nuevo con decisión, pruebas, operación y manuales.

Para una revisión rápida consulte la
[Guía rápida para cambios](referencia-rapida/guia-rapida-cambios.md).
