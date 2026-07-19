# Manual de instalación y operación

**Público:** responsable de despliegue, operador del sistema, administrador de
base de datos y responsable de red\
**Nivel:** operativo y técnico\
**Estado:** borrador inicial; pendiente de ensayo completo en la red y el equipo
destino

## Objetivo

Este capítulo explica cómo preparar, iniciar, comprobar, detener y recuperar el
sistema de forma controlada.

Los comandos específicos están separados por sistema operativo:

- [Operación en Windows](anexos/operacion-windows.md)
- [Operación en Linux Mint](anexos/operacion-linux.md)

## Responsabilidades

| Persona | Responsabilidad |
|---|---|
| Propietario del equipo | Instalar y actualizar Java, Docker y las herramientas del sistema |
| Operador de despliegue | Preparar la versión, configurar el ambiente, iniciar, detener y comprobar salud |
| Administrador de base de datos | Preparar PostgreSQL, credenciales, copias de seguridad y restauración |
| Responsable de red | Definir dirección, segmento autorizado y regla de firewall |
| Soporte de aplicación | Revisar registros, diagnosticar fallas y coordinar recuperación |
| Responsable del proyecto | Autorizar versiones, ventanas de cambio y excepciones |

Una persona puede cubrir varias responsabilidades, pero las decisiones deben quedar
registradas por separado.

## Ambientes disponibles

El proyecto tiene dos perfiles:

| Perfil | Uso | Datos incluidos |
|---|---|---|
| `dev` | Desarrollo y demostración local | Incluye usuarios, terminales y datos de ejemplo |
| `preprod` | Ensayo operativo con configuración externa | No incluye usuarios ni datos de demostración |

No existe un perfil llamado `prod`. El sistema es una prueba de concepto. No debe
presentarse como una instalación productiva sin una revisión adicional de seguridad,
copias de seguridad, alta disponibilidad, gestión de usuarios, cifrado y soporte.

El acceso no local a `preprod` debe utilizar HTTPS. La aplicación no incluye todavía
un certificado, proxy inverso o terminador HTTPS. Por lo tanto, un despliegue de
preproducción accesible desde terminales u otros equipos queda pendiente de que la
infraestructura proporcione y pruebe esa capa segura.

**No haga esto:** nunca utilice el perfil `dev`, sus usuarios o su clave de base de
datos como si fueran datos reales del almacén.

## Herramientas necesarias

El propietario del equipo debe instalar y mantener:

- Java OpenJDK 21 LTS;
- Docker, cuando PostgreSQL o las pruebas utilicen contenedores;
- acceso a PostgreSQL 17.10;
- Git o un paquete aprobado del código fuente.

Maven no requiere una instalación separada. El proyecto incluye `mvnw` para Linux y
`mvnw.cmd` para Windows, que utilizan Maven 3.9.16.

Este manual comprueba las herramientas, pero no autoriza ni automatiza su
instalación.

## 1. Preparar un despliegue

Antes de iniciar:

1. Identifique la versión o confirmación exacta que se desplegará.
2. Registre el perfil y el ambiente destino.
3. Confirme una ventana de cambio y una persona responsable.
4. Compruebe Java y, si corresponde, Docker.
5. Confirme que PostgreSQL está disponible.
6. Confirme que la base de datos destino es la correcta.
7. Prepare las variables sin escribir secretos en el repositorio.
8. Tome una copia de seguridad o instantánea antes de aplicar una migración nueva a
   una base de datos conservada.
9. Identifique la versión anterior conocida como estable.
10. Si `preprod` tendrá acceso desde otro equipo, confirme la solución HTTPS
    aprobada antes de abrir la red.
11. Avise a supervisores antes de interrumpir el servicio.

**Deténgase:** no continúe si no puede identificar la base de datos, la versión, la
copia de seguridad o la forma de volver a la versión anterior.

## 2. Preparar PostgreSQL

El sistema necesita una instancia accesible de PostgreSQL 17.10.

Para `preprod`:

- utilice una base de datos dedicada;
- utilice una cuenta y clave propias del ambiente;
- no utilice `wms_dev_password`;
- limite el acceso de red según la política del ambiente;
- prepare una copia de seguridad o instantánea mediante la plataforma de base de
  datos;
- confirme que los usuarios y terminales operativos se aprovisionarán mediante un
  procedimiento controlado, porque el perfil no incluye datos de demostración.

La aplicación no crea copias de seguridad y no configura cifrado de PostgreSQL.
Esas tareas pertenecen al administrador de base de datos y a la infraestructura.

## 3. Configurar `preprod`

Las tres variables mínimas son:

| Variable | Contenido | Sensibilidad |
|---|---|---|
| `WMS_DB_URL` | Dirección JDBC de PostgreSQL | Media; puede revelar infraestructura |
| `WMS_DB_USERNAME` | Usuario de PostgreSQL | Media |
| `WMS_DB_PASSWORD` | Clave de PostgreSQL | Alta; secreto |

También debe establecerse:

```text
SPRING_PROFILES_ACTIVE=preprod
```

Si falta una variable o la clave coincide con la clave pública de desarrollo, el
sistema se detiene antes de conectarse.

![Falla segura al iniciar preproducción sin la configuración requerida](../evidence/2026-07-14-final-acceptance-sweep/preprod-failfast.png)

**Resultado esperado ante una configuración incorrecta:** aparece
`APPLICATION FAILED TO START` con el nombre de la variable que falta, pero sin
mostrar su valor.

## 4. Construir la aplicación

1. Abra una terminal dentro del repositorio.
2. Ejecute el empaquetado correspondiente a su sistema operativo.
3. Compruebe que termine con `BUILD SUCCESS`.
4. Registre la versión de Java, Maven y la aplicación.
5. Compruebe que exista:

```text
target/warehouse-management-0.1.0-SNAPSHOT.jar
```

El nombre contiene `SNAPSHOT`, lo que confirma que esta versión todavía es una
versión de desarrollo. Para un despliegue formal futuro deberá definirse un esquema
de versiones publicadas.

## 5. Iniciar la aplicación

1. Confirme que PostgreSQL está activo.
2. Confirme que las variables pertenecen al ambiente correcto.
3. Inicie la aplicación desde el paquete o mediante Maven.
4. Observe la consola durante el arranque.
5. Espere a que termine la aplicación de migraciones y quede escuchando en el
   puerto configurado.
6. Compruebe la salud local antes de abrir acceso desde la red.

Al iniciar, Flyway aplica las migraciones pendientes. En `preprod` solo se aplican
las migraciones de estructura; no se cargan los datos de demostración.

**No haga esto:** no interrumpa deliberadamente el proceso mientras se está
aplicando una migración.

## 6. Comprobar la salud

La dirección local de salud es:

```text
http://localhost:<puerto>/actuator/health
```

El puerto predeterminado es `8080`.

Una respuesta correcta incluye:

```json
{"status":"UP"}
```

Realice dos comprobaciones cuando el ambiente permita acceso remoto:

1. Desde el propio servidor usando `localhost`.
2. Desde otro equipo en el mismo segmento que las terminales y usuarios del tablero.
   En `preprod`, use la dirección HTTPS aprobada; no el puerto HTTP directo.

| Resultado local | Resultado desde la red | Interpretación inicial |
|---|---|---|
| Correcto | Correcto | Aplicación y ruta de red disponibles |
| Correcto | Falla | Revisar HTTPS, firewall, dirección o ruta de red; no reiniciar la aplicación como primera acción |
| Falla | No probado | Revisar aplicación, configuración y base de datos |
| Intermitente | Intermitente | Conservar horas y registros; escalar a soporte y red |

La salud correcta demuestra que la aplicación y la base de datos responden. No
demuestra por sí sola que todos los usuarios, etiquetas o flujos estén preparados.

## 7. Abrir el acceso de red

En desarrollo controlado, puede exponerse a la red de ensayo el puerto de la
aplicación, normalmente `8080`, limitado al segmento autorizado.

En `preprod`, no exponga directamente HTTP en el puerto `8080`. Utilice la
terminación HTTPS aprobada por infraestructura. El diseño del certificado, el proxy
inverso y sus reglas todavía no forma parte de este repositorio, por lo que debe
existir un procedimiento adicional antes de habilitar el acceso remoto.

Toda regla debe limitarse al segmento concreto donde se encuentran:

- terminales de mano;
- equipos autorizados para el tablero;
- herramientas administrativas aprobadas.

PostgreSQL nunca debe abrirse directamente a la red de las terminales. Debe permanecer
en el propio servidor o en una red privada de base de datos.

La decisión de activar un firewall completo es distinta de añadir una regla. Debe
coordinarse con el responsable de red para no interrumpir otros servicios.

## 8. Comprobaciones después de iniciar

1. Confirme salud local y, cuando corresponda, desde la dirección HTTPS remota.
2. Abra `/dashboard` con una cuenta autorizada.
3. Compruebe que `Last refreshed` cambia.
4. Compruebe que no se cargaron datos de demostración en `preprod`.
5. Confirme que existen los usuarios y terminales preparados para el ensayo.
6. Realice una prueba controlada de inicio de sesión de una terminal.
7. Revise que los registros se estén conservando.
8. Registre el resultado del despliegue.

No utilice una preparación real como primera prueba después de un cambio. Use el
caso de aceptación autorizado para el ambiente.

## 9. Operar el servicio

Durante la operación:

- compruebe periódicamente la salud;
- vigile que el tablero siga actualizándose;
- conserve la salida estructurada de la aplicación;
- controle el espacio disponible para registros y base de datos;
- registre cambios de configuración y reinicios;
- coordine incidencias con supervisión y soporte.

La aplicación escribe registros en la salida de la consola. No incluye rotación,
retención ni envío de registros. El proceso que aloja la aplicación debe capturarlos
y aplicar la política del ambiente.

El proyecto tampoco instala un servicio automático de Windows o `systemd`. Si se
requiere inicio al encender, supervisión del proceso o reinicio automático, debe
configurarse y probarse mediante la plataforma de alojamiento como alcance
adicional.

Tampoco proporciona terminación HTTPS. Hasta documentarla y verificarla, el acceso
remoto a `preprod` no debe marcarse como listo.

## 10. Detener el servicio

Para una detención planificada:

1. Avise al supervisor y detenga la asignación de trabajo nuevo.
2. Pida que las terminales terminen de sincronizar y compruebe que no muestran
   `Q:`.
3. Registre la hora de inicio de la ventana.
4. Detenga el proceso con `Ctrl+C` o mediante el gestor que lo aloja.
5. Espere la salida completa del proceso.
6. Compruebe que la dirección de salud ya no responde.
7. No detenga PostgreSQL si otra aplicación lo utiliza.
8. Registre la hora y el resultado.

La aplicación utiliza una detención ordenada. Evite terminar el proceso por fuerza
salvo que esté bloqueado y soporte haya conservado la evidencia.

## 11. Reiniciar después de una configuración

Todas las variables se leen al iniciar. Ningún cambio se aplica en caliente.

1. Registre el valor anterior sin copiar secretos.
2. Apruebe y prepare el valor nuevo.
3. Detenga ordenadamente la aplicación.
4. Cambie la configuración en el mecanismo del ambiente.
5. Inicie la aplicación.
6. Repita las comprobaciones locales y de red.
7. Registre el resultado y la forma de volver al valor anterior.

## Configuración principal

| Variable | Valor predeterminado | Uso |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | Selecciona `dev` o `preprod` |
| `WMS_SERVER_ADDRESS` | `0.0.0.0` | Dirección donde escucha la aplicación |
| `WMS_SERVER_PORT` | `8080` | Puerto de API y tablero |
| `WMS_TASK_STUCK_THRESHOLD` | `PT30M` | Tiempo para marcar una tarea atascada |
| `WMS_AUTH_TOKEN_TTL` | `PT8H` | Duración de una sesión de terminal |
| `WMS_DASHBOARD_POLL_INTERVAL` | `PT2S` | Frecuencia de actualización del tablero |
| `WMS_MFC_ADAPTER` | `noop` | Adaptador de finalización: `noop` (sin efecto) o `telegram` (envío real de misiones MFC, ADR 0011) |
| `WMS_MFC_TELEGRAM_BASE_URL` | vacío | URL del WCS que recibe los telegramas; obligatoria con `telegram` (la aplicación se niega a iniciar sin ella) |
| `WMS_MFC_TELEGRAM_RETRY_INTERVAL` | `PT30S` | Intervalo del despachador y de los reintentos; solo con `telegram` |
| `WMS_MFC_TELEGRAM_MAX_ATTEMPTS` | `5` | Intentos de envío antes de marcar la misión `FAILED`; solo con `telegram` |
| `WMS_MFC_TRANSPORT_SOURCE_LOCATION` | vacío | Código de ubicación origen de las misiones TRANSPORT; debe existir; obligatoria con `telegram` |
| `WMS_MFC_TRANSPORT_DESTINATION_LOCATION` | vacío | Código de ubicación destino de las misiones TRANSPORT; debe existir; obligatoria con `telegram` |

`PT30M`, `PT8H`, `PT2S` y `PT30S` son duraciones: 30 minutos, 8 horas, 2
segundos y 30 segundos. Las variables `WMS_MFC_*` adicionales solo importan
cuando `WMS_MFC_ADAPTER=telegram`; con el valor predeterminado `noop` la
integración MFC está desactivada y ninguna se lee.

Todas requieren reiniciar la aplicación. La referencia completa sigue siendo
[`docs/configuration-matrix.md`](../configuration-matrix.md).

`WMS_SERVER_PORT` no agrega HTTPS por sí solo. Cambiar el número del puerto no cifra
la conexión.

## 12. Copias de seguridad y restauración

La aplicación no crea ni restaura copias de PostgreSQL.

Antes de una migración nueva:

1. El administrador de base de datos crea una copia o instantánea.
2. Registra la hora, base de datos, versión y ubicación protegida de la copia.
3. Comprueba que el procedimiento de restauración está definido.
4. Confirma cuánto tiempo de datos podría perderse.
5. Autoriza al operador a iniciar la nueva versión.

Una copia que nunca fue probada no debe considerarse una recuperación confirmada.
El ensayo de restauración debe realizarse según la plataforma de PostgreSQL sin
sobrescribir una base de datos conservada.

## 13. Volver a una versión anterior

La recuperación rutinaria de aplicación consiste en:

1. Detener la versión actual.
2. Desplegar el paquete anterior conocido como estable.
3. Utilizar la misma base de datos.
4. Iniciar y comprobar salud.
5. Ejecutar el caso de aceptación acordado.
6. Registrar motivo, versión anterior, versión restaurada y resultado.

Esto solo es seguro cuando las migraciones intermedias son compatibles y aditivas.
Si una migración hizo un cambio incompatible, deténgase y solicite una decisión del
responsable técnico y del administrador de base de datos.

## 14. Recuperación de estructura de base de datos

No existe una migración automática hacia atrás.

- No edite una migración ya aplicada.
- No utilice `flyway repair` para ocultar un cambio.
- Corrija el problema mediante una migración nueva hacia adelante.
- Si es imprescindible volver al estado anterior, utilice la instantánea aprobada y
  un procedimiento coordinado de restauración.

Restaurar una base de datos puede perder cambios posteriores a la copia. Requiere
una decisión explícita, una ventana de servicio y un plan para las terminales que
puedan conservar información pendiente.

## 15. Reinicio destructivo de desarrollo

Solo en una base local de desarrollo se permite eliminar el volumen y recrearlo.

```text
docker compose down -v
docker compose up -d
```

**Peligro:** `-v` elimina permanentemente los datos del volumen local. Nunca use
este procedimiento en preproducción ni como recuperación de una incidencia.

## 16. Evidencia del despliegue

Registre como mínimo:

| Dato | Valor |
|---|---|
| Fecha y hora UTC | |
| Operador | |
| Ambiente y perfil | |
| Versión o confirmación | |
| Java y Maven | |
| PostgreSQL | |
| Identificador de configuración | |
| Copia de seguridad | |
| Resultado de construcción | |
| Salud local | |
| Salud desde la red | |
| Tablero | |
| Prueba de terminal | |
| Registros conservados en | |
| Resultado final | Aprobado / Fallido / Revertido |
| Incidencias | |

Una ejecución sin evidencia conservada no cuenta como ensayo aprobado.

Para operaciones rutinarias, consulte la
[Guía rápida de operación](referencia-rapida/guia-rapida-operacion.md).
