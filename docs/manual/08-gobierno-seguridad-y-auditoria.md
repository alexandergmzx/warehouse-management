# Guía de gobierno, seguridad y auditoría

**Público:** responsable del proyecto, responsable de seguridad, auditoría,
propietario de negocio y responsables técnicos\
**Nivel:** gobierno y técnico\
**Estado:** borrador inicial; los riesgos y responsables requieren aprobación del
propietario

## Propósito

Esta guía permite responder con evidencia:

- qué alcance fue aprobado;
- qué controles están implementados;
- qué controles siguen pendientes;
- quién puede decidir y ejecutar cambios;
- qué riesgos impiden un uso más amplio;
- qué evidencia respalda una aceptación.

No es una certificación de seguridad o cumplimiento.

## Clasificación actual del sistema

El sistema es una prueba de concepto de gestión de almacén.

| Uso | Estado actual |
|---|---|
| Desarrollo y demostración local | Implementado y evidenciado para la versión citada |
| Integración HandheldPi local | Implementada y evidenciada |
| GamePi20 real mediante WiFi | Pendiente de aceptación física completa |
| Preproducción local sin acceso remoto | Ensayo técnico parcial evidenciado |
| Preproducción accesible desde otros equipos | Bloqueada hasta disponer de HTTPS y controles operativos faltantes |
| Producción | No autorizada; no existe perfil ni preparación productiva |

Ningún resultado debe presentarse fuera de este alcance. “Todas las pruebas pasan”
no significa “preparado para producción”.

## Autoridades documentales

| Pregunta | Fuente principal |
|---|---|
| ¿Qué está entregado? | `README.md` |
| ¿Qué se ejecutó realmente? | `docs/executed-test-report.md` y `docs/evidence/` |
| ¿Qué decisión se tomó y por qué? | `docs/decisions/` |
| ¿Qué comportamiento ofrece la API? | `API.md` |
| ¿Qué regla se prueba? | Requisitos, trazabilidad y especificación funcional |
| ¿Cómo se instala y recupera? | Runbooks y manual de operación |
| ¿Cómo se investiga un problema? | Guías de logs, SQL e incidencias |
| ¿Cómo actúa cada persona? | `docs/manual/` |

Una afirmación de control debe citar versión, configuración y evidencia; no solo el
documento que describe la intención.

## Decisiones que requieren aprobación

El responsable del proyecto debe aprobar antes de:

- cambiar una regla de preparación protegida;
- ampliar el alcance funcional;
- cambiar una versión tecnológica fijada;
- modificar el contrato API de forma incompatible;
- añadir una integración externa o transporte MFC;
- abrir acceso de red;
- aceptar un riesgo de seguridad alto;
- desplegar una migración incompatible;
- restaurar una base conservada;
- declarar una versión candidata aceptada;
- autorizar un uso distinto de desarrollo o demostración.

Una decisión arquitectónica duradera debe registrarse en un ADR. Una autorización
operativa debe conservar fecha, alcance, persona y evidencia.

Consulte la [Matriz de responsabilidades](anexos/matriz-de-responsabilidades.md).

## Controles implementados y evidenciados

### Identidad y sesiones

- Claves almacenadas con Argon2id.
- Sesiones REST opacas con 256 bits aleatorios.
- Solo se conserva SHA-256 de la sesión, no su valor reutilizable.
- Cada sesión se vincula a una persona y terminal.
- Vencimiento absoluto configurable, ocho horas por defecto.
- Logout revoca la sesión presentada.
- Usuario o terminal inactivos hacen fallar el acceso.
- Dos funciones de aplicación: `ADMIN` y `PICKER`.

### Autorización web

- `/api/v1/admin/**` exige `ADMIN`.
- El tablero exige `ADMIN`.
- API REST sin sesión de navegador.
- Tablero con cookie de sesión y protección CSRF.
- Respuestas `401` y `403` usan códigos estables.

### Datos e integridad

- Restricciones de base de datos para estados y cantidades.
- Una tarea activa como máximo por persona y terminal.
- Confirmación exacta e idempotente.
- Cambios de existencias y tarea dentro de una transacción.
- Movimientos y transiciones inmutables mediante triggers.
- Un movimiento `PICK` único por tarea.
- Flyway como único propietario de la estructura.

### Configuración

- `dev` y `preprod` separados.
- `preprod` no carga datos de demostración.
- `preprod` exige credenciales externas.
- La clave pública de desarrollo se rechaza en `preprod`.
- PostgreSQL de Compose solo escucha en loopback.

### Observabilidad y pruebas

- `correlationId` en solicitudes, errores y registros relacionados.
- Logs JSON con campos permitidos.
- Exclusión deliberada de claves, sesiones y hashes.
- Pruebas de autenticación, autorización, concurrencia, idempotencia e integridad.
- Imagen PostgreSQL fijada por digest.
- CI con permiso de contenido de solo lectura.

Cada control sigue limitado a la versión y evidencia que lo probó.

## Controles parciales o pendientes

### Gestión de cuentas y terminales

La estructura permite usuarios, funciones, terminales y estados activos, pero no
existe una interfaz administrativa para:

- crear o modificar cuentas;
- cambiar o restablecer claves;
- asignar funciones;
- registrar o retirar terminales;
- revocar todas las sesiones de una persona o terminal;
- revisar sesiones activas.

Los datos de desarrollo se crean mediante migraciones de demostración. Eso no es un
procedimiento válido para preproducción. El uso operativo queda bloqueado hasta
definir aprovisionamiento, autorización, baja y evidencia.

### Sesiones y acceso

- Un nuevo login no revoca automáticamente sesiones anteriores.
- `last_used_at` existe, pero no se actualiza en cada solicitud.
- No existe limitación de intentos, bloqueo temporal o protección específica contra
  fuerza bruta.
- El PIN numérico de cuatro dígitos es solo una credencial de demostración.
- No hay autenticación multifactor.
- No existe una política propia documentada para duración de sesión del tablero.

### Transporte

- ADR 0005 exige HTTPS para todo acceso no local a `preprod`.
- El proyecto no instala certificados ni terminación HTTPS.
- La conexión de PostgreSQL no configura TLS por sí misma.
- La prueba física de firewall y WiFi sigue pendiente.

### Base de datos

- No existe un script documentado de privilegio mínimo para la cuenta de ejecución.
- No se separan formalmente la cuenta de migración y la cuenta de aplicación.
- Copias, restauración, cifrado, disponibilidad y retención pertenecen a la
  plataforma externa.
- No existe evidencia de un ensayo completo de restauración.

### Operación y auditoría

- La aplicación no administra retención ni envío de logs.
- Cada línea de log no incluye versión/configuración.
- No existe servicio Windows o `systemd` entregado para el WMS.
- No existe tablero de métricas, alertas o alta disponibilidad.
- No existe política aprobada de conservación de datos operativos o personales.

### Cadena de suministro

- CI ejecuta pruebas y análisis estático.
- Las acciones de GitHub están referenciadas por etiquetas de versión mayor, no por
  SHA inmutable, aunque ADR 0006 solicita referencias inmutables.
- No existe análisis dedicado de vulnerabilidades de dependencias, SBOM o firma de
  artefactos.
- Las dependencias y la fuente incluida para PDF requieren revisión periódica de
  seguridad y licencias.

Estos puntos están detallados en el
[Registro inicial de riesgos](anexos/registro-inicial-de-riesgos.md).

## Gestión de acceso requerida

Antes de un ambiente operativo debe aprobarse un procedimiento que cubra:

1. solicitud de alta;
2. aprobación del responsable de negocio;
3. función mínima necesaria;
4. creación segura de clave;
5. entrega de credencial sin exponerla;
6. vinculación y etiquetado de terminal;
7. prueba de acceso;
8. revisión periódica;
9. cambio de responsabilidad;
10. baja inmediata;
11. revocación de sesiones;
12. conservación de evidencia.

No utilice SQL manual informal como sustituto permanente de este proceso.

## Gestión de secretos

- Las claves reales nunca se confirman en Git.
- `.env` contiene únicamente valores locales de desarrollo y permanece sin
  confirmar.
- `preprod` recibe secretos desde el mecanismo aprobado del ambiente.
- No se incluyen secretos en comandos conservados, tickets, capturas o logs.
- Toda exposición real o sospechada requiere rotación e incidencia.
- Deben definirse propietario, fecha de rotación, consumidores y procedimiento de
  emergencia para cada secreto.

Las variables de entorno son el mecanismo soportado por la aplicación, pero no son
por sí mismas un gestor de secretos.

## Red y transporte

### Desarrollo

HTTP puede utilizarse en una red de ensayo confiable, limitada y temporal para la
terminal física. Solo se abre el puerto de aplicación al segmento autorizado.

### Preproducción

Todo acceso no local requiere HTTPS. La infraestructura debe definir:

- certificado y nombre DNS;
- terminación TLS;
- protocolos y cifrados permitidos;
- redirección o rechazo de HTTP;
- encabezados reenviados;
- acceso al endpoint de salud;
- renovación y monitoreo del certificado;
- evidencia desde la red de terminales.

PostgreSQL nunca se expone a la red de las terminales.

## Integridad y auditoría de datos

Los históricos `stock_movement` y `task_transition` rechazan actualización y
eliminación mediante triggers. Esto protege el uso normal, pero no sustituye:

- privilegios mínimos de base de datos;
- monitoreo de administradores de base de datos;
- copias protegidas;
- auditoría externa cuando la regulación lo exija;
- control de acceso al servidor.

Una cuenta con capacidad de alterar estructura podría modificar o deshabilitar los
triggers. El responsable de base de datos debe limitar y auditar ese privilegio.

## Datos personales y retención

El sistema maneja nombres de usuario, terminales, actividad y marcas de tiempo que
pueden relacionarse con una persona. Antes de un uso real debe definirse:

- propósito y base autorizada del seguimiento;
- quién puede consultar la actividad;
- cuánto tiempo se conservan logs, sesiones e históricos;
- cómo se atienden obligaciones legales aplicables;
- cómo se protege una exportación de evidencia;
- cómo se elimina información cuando la ley lo permita sin destruir históricos que
  deban conservarse.

Este manual no declara cumplimiento con una ley o norma concreta.

## Copias y continuidad

Antes de una migración o uso conservado:

- defina frecuencia y retención de copias;
- proteja las copias con acceso y cifrado apropiados;
- defina objetivo de pérdida de datos y tiempo de recuperación;
- ensaye restauración en un ambiente aislado;
- registre versión de aplicación compatible;
- considere terminales con operaciones pendientes durante una recuperación.

Una copia no probada no cuenta como recuperación demostrada.

## Gestión de vulnerabilidades y dependencias

El responsable debe establecer:

- revisión periódica de Java, Spring, PostgreSQL, pgJDBC y librerías;
- seguimiento de avisos de seguridad;
- actualización controlada con ADR cuando cambia una versión fijada;
- análisis de dependencias;
- inventario o SBOM;
- revisión de licencias y archivos incluidos;
- respuesta a vulnerabilidades urgentes;
- evidencia de pruebas después de actualizar.

SpotBugs detecta defectos de código; no sustituye un escáner de vulnerabilidades de
dependencias.

## Evidencia para una auditoría

Prepare un paquete que incluya:

1. alcance y versión;
2. diagrama y mapa de módulos;
3. ADR aplicables;
4. configuración sin secretos;
5. matriz de responsabilidades;
6. resultados de pruebas y trazabilidad;
7. evidencia de instalación y salud;
8. revisión de accesos;
9. revisión de logs y reconciliación;
10. copias y ensayo de restauración;
11. riesgos y excepciones aceptadas;
12. incidentes y seguimiento;
13. licencias e inventario de dependencias;
14. decisión de aceptación.

No entregue un volcado completo de base de datos o logs sin revisar información
sensible.

## Revisión antes de una liberación

El responsable debe confirmar:

- alcance aprobado;
- riesgos altos resueltos o aceptación explícita;
- versión exacta;
- `verify` y CI correctos;
- pruebas manuales necesarias;
- reconciliación sin diferencias;
- documentación y manuales actualizados;
- copia de seguridad y rollback;
- acceso y secretos revisados;
- HTTPS si existe acceso no local a `preprod`;
- prueba física si el alcance incluye HandheldPi;
- evidencia conservada;
- decisión firmada.

Utilice la [Plantilla de revisión y autorización](plantillas/revision-y-autorizacion.md).

## Criterios que bloquean producción

La producción no debe autorizarse mientras falten, como mínimo:

- arquitectura y perfil productivos;
- HTTPS implementado y probado;
- gestión completa de identidades, terminales y sesiones;
- protección contra intentos de acceso abusivos;
- privilegios mínimos de base de datos;
- copias y restauración probadas;
- servicio supervisado, logs, alertas y retención;
- gestión de vulnerabilidades y artefactos;
- política de datos personales;
- aceptación física y de red;
- proceso de soporte y responsables con tiempos acordados.

Para una revisión ejecutiva consulte la
[Guía rápida del responsable](referencia-rapida/guia-rapida-responsable.md).
