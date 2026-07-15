# Manual de pruebas y aceptación

**Público:** responsable de pruebas, usuario de aceptación, desarrollo, soporte y
responsable del proyecto\
**Nivel:** operativo y técnico\
**Estado:** borrador inicial; pendiente de ensayo siguiendo este manual

## Objetivo

Las pruebas deben demostrar que una versión concreta cumple las reglas del sistema
en una configuración concreta.

Compilar, ejecutar una prueba sin conservar evidencia o afirmar “funcionó” no es
aceptación.

## Responsabilidades

| Persona | Responsabilidad |
|---|---|
| Responsable de pruebas | Preparar alcance, datos, ambiente y reporte |
| Ejecutor | Seguir los pasos y conservar resultados sin alterarlos |
| Usuario de aceptación | Confirmar que el flujo permite realizar el trabajo esperado |
| Responsable técnico | Revisar resultados automáticos, logs, SQL y defectos |
| Responsable del proyecto | Aprobar excepciones, riesgos y decisión final |

Una misma persona puede ejecutar varias funciones, pero el reporte debe indicar cuál
desempeñó en cada momento.

## Estados permitidos

| Estado | Significado |
|---|---|
| Aprobada | Cumple el resultado esperado y existe evidencia conservada |
| Fallida | El resultado observado no cumple lo esperado |
| Bloqueada | No pudo ejecutarse por falta de ambiente, equipo, datos o acceso |
| No aplica | El caso no corresponde al alcance, con justificación aprobada |

Una prueba bloqueada nunca debe registrarse como aprobada. Una prueba fallida no se
borra cuando se corrige: se conserva y se añade una nueva ejecución.

## Capas de prueba

| Capa | Qué comprueba |
|---|---|
| Integración WMS | API, PostgreSQL, migraciones, seguridad y reglas de negocio |
| Calidad estática | Estilo y defectos detectables por Checkstyle y SpotBugs |
| Diagnóstico SQL | Coherencia entre estados, tareas y movimientos |
| Navegador | Login, tablero, actualización y recursos visuales reales |
| Etiquetas | Contenido QR, PDF/PNG, repetibilidad y lectura |
| HandheldPi automática | Pantallas, cliente HTTP, cola sin conexión y reglas del dispositivo |
| Integración local | Cliente HandheldPi real contra una aplicación WMS en la misma máquina |
| Dispositivo y red | GamePi20, cámara, botones, WiFi, firewall y operación física |
| Inspección de alcance | Confirmar que funciones excluidas no aparecieron por accidente |

Ninguna capa sustituye automáticamente a otra. Una prueba HTTP puede aprobar y aun
así existir un defecto visible en un navegador o dispositivo físico.

## Estado de referencia actual

Los resultados históricos aplican únicamente a las versiones y configuraciones
citadas en su evidencia.

| Alcance | Resultado registrado | Pendiente |
|---|---|---|
| WMS FT-01 a FT-19 | 19 aprobadas, 0 fallidas, 0 bloqueadas | Repetir ante una nueva versión candidata |
| Verificación WMS | 33 pruebas de integración, Checkstyle y SpotBugs correctos en el pase citado | No representa automáticamente el árbol de trabajo actual |
| HandheldPi automática | 100 pruebas aprobadas en el repositorio del dispositivo | Repetir con la versión candidata |
| HandheldPi contra WMS local | 44 de 44 comprobaciones aprobadas | Confirmar nuevamente tras cambios de contrato |
| GamePi20 mediante WiFi y firewall | No ejecutada en la integración actual | Prueba física Stage 3 |

La fuente de resultados ejecutados es
[`docs/executed-test-report.md`](../executed-test-report.md) y su evidencia asociada.

La cabecera de `docs/functional-test-specification.md` todavía afirma que FT-19 está
bloqueada, pero el reporte ejecutado posterior demuestra su aprobación. Hasta
corregir esa cabecera, utilice el reporte ejecutado como fuente del estado real y la
especificación como fuente de los casos.

## 1. Definir el alcance

Antes de probar, registre:

- versión o confirmación exacta de cada repositorio;
- cambios incluidos;
- perfil y configuración;
- sistema operativo;
- Java, Maven, Docker y PostgreSQL;
- terminal HandheldPi, cuando corresponda;
- casos requeridos y casos excluidos;
- riesgos que determinan pruebas adicionales;
- persona que decidirá la aceptación.

No utilice “última versión” como identificador. Debe poder recuperarse exactamente
lo probado.

## 2. Preparar el ambiente

1. Utilice datos deterministas de desarrollo.
2. Confirme que Docker está activo para las pruebas WMS.
3. Compruebe que no quedó `SPRING_PROFILES_ACTIVE=preprod` en la terminal de pruebas.
4. Compruebe espacio para resultados y capturas.
5. Identifique si la prueba cambiará o eliminará datos.
6. Prepare una base local desechable cuando el caso lo requiera.
7. No conecte las pruebas destructivas a una base conservada.
8. Registre el estado inicial.

Las pruebas de integración WMS usan instancias PostgreSQL desechables mediante
Testcontainers. Docker debe estar disponible.

## 3. Ejecutar la verificación automática WMS

En Linux:

```bash
env -u SPRING_PROFILES_ACTIVE ./mvnw -B verify
```

En Windows, abra una nueva ventana de PowerShell sin variables de `preprod`:

```powershell
.\mvnw.cmd -B verify
```

Utilice `verify`, no `test`. Las pruebas del proyecto terminan en `*IT` y se ejecutan
mediante Failsafe durante la fase de integración. `mvn test` ejecuta cero pruebas y
no constituye una verificación.

`verify` debe:

1. compilar;
2. ejecutar todas las pruebas de integración;
3. completar la verificación de Failsafe;
4. terminar Checkstyle sin infracciones;
5. terminar SpotBugs sin hallazgos bloqueantes;
6. mostrar `BUILD SUCCESS`.

Conserve:

- comando exacto;
- hora de inicio y fin;
- resumen de pruebas;
- resultado de cada herramienta;
- reportes bajo `target/failsafe-reports/` cuando exista una falla;
- versión de herramientas y código.

## 4. Ejecutar las pruebas funcionales

La matriz en español se encuentra en
[Casos funcionales FT-01 a FT-19](anexos/matriz-pruebas-funcionales.md).

Para una regresión completa:

1. Ejecute los casos automáticos con `verify`.
2. Revise que cada FT tenga una prueba o evidencia correspondiente.
3. Ejecute manualmente los casos que requieren navegador, dispositivo o inspección.
4. Registre aprobado, fallido, bloqueado o no aplica para cada caso.
5. Cite la evidencia concreta, no solo el nombre de la prueba.

## 5. Ejecutar diagnóstico SQL

Después de una ejecución integrada o manual:

1. Ejecute el resumen de integridad.
2. Compruebe que existencias y movimientos coinciden.
3. Compruebe que cada tarea completada tenga el movimiento esperado.
4. Trace las órdenes utilizadas en los escenarios principales.
5. Conserve resultados y hora.

Utilice las consultas oficiales en
[`docs/sql-diagnostics.md`](../sql-diagnostics.md).

El resultado normal de la reconciliación de existencias es cero diferencias.

## 6. Probar el tablero en un navegador real

La prueba automática no sustituye esta comprobación.

1. Inicie el ambiente autorizado.
2. Abra `/login` en un navegador real.
3. Compruebe rechazo de una cuenta sin permiso.
4. Inicie sesión con una cuenta `ADMIN`.
5. Abra `/dashboard`.
6. Compruebe columnas, datos y tarea atascada.
7. Espere más de un intervalo de actualización.
8. Confirme que `Last refreshed` cambia sin recargar la página.
9. Revise la consola y la red del navegador para detectar recursos fallidos.
10. Conserve una captura sin secretos.

Esta comprobación es obligatoria porque una prueba previa encontró un defecto real de
estilos que la prueba HTTP no detectó.

## 7. Probar etiquetas

1. Genere dos veces la misma etiqueta de ubicación en PNG y PDF.
2. Genere dos veces la misma etiqueta de artículo en PNG y PDF.
3. Compruebe que cada par sea idéntico byte por byte.
4. Revise el texto visible del PDF.
5. Escanee los códigos con la terminal o lector aprobado.
6. Confirme contenido exacto `LOC:<codigo>` y `ART:<sku>`.
7. Conserve archivos o sus hashes.

No valide únicamente que el archivo abre. Debe comprobarse el contenido escaneado.

## 8. Ejecutar pruebas automáticas HandheldPi

En el repositorio separado HandheldPi, con su ambiente virtual preparado:

```bash
.venv/bin/python -m pytest
```

Registre versión del repositorio, Python, dependencias, cantidad de pruebas y
resultado. El pase histórico citado tuvo 100 pruebas aprobadas; una ejecución nueva
debe registrar su propia cantidad.

Estas pruebas incluyen lógica, cliente HTTP, cola persistente, servidor falso y
escenarios funcionales. No demuestran cámara, botones, WiFi o energía reales.

## 9. Ejecutar integración local HandheldPi–WMS

El cliente real debe comunicarse con una instancia WMS `dev` controlada.

Incluya como mínimo:

- gafete y PIN correctos;
- escaneos correctos e incorrectos;
- cantidad exacta y discrepancia;
- confirmación repetida sin doble movimiento;
- trabajo sin conexión y envío posterior en orden;
- rechazo de una repetición y `SYNC FAILED`;
- sesión vencida o revocada y nuevo login;
- conflicto de terminal;
- relación de `correlationId` entre dispositivo y servidor;
- reconciliación SQL final.

La evidencia histórica de 44 comprobaciones está en
[`docs/evidence/2026-07-15-hht-loopback-integration.md`](../evidence/2026-07-15-hht-loopback-integration.md).

## 10. Ejecutar la aceptación física pendiente

Esta prueba requiere el GamePi20 y una red de ensayo autorizada. Debe utilizar `dev`
mediante HTTP en una red confiable y limitada. No utilice `preprod` sin HTTPS.

Antes de comenzar:

1. Registre identificador y versión del dispositivo.
2. Compruebe batería, pantalla, botones, cámara y audio.
3. Prepare gafete y etiquetas de prueba.
4. Inicie el WMS y confirme salud local.
5. Aplique la regla de firewall limitada al segmento de ensayo.
6. Compruebe salud desde el dispositivo.

Escenarios mínimos:

1. Preparación completa con gafete y PIN.
2. Pérdida real de WiFi después de tomar una tarea.
3. Finalización sin conexión y sincronización al volver.
4. Bloqueo administrativo durante la falta de conexión para provocar
   `SYNC FAILED` de forma controlada.
5. Recuperación posterior con supervisor y administrador.
6. Captura de pantallas y logs del dispositivo.
7. Correlación con logs del servidor.
8. Reconciliación SQL sin diferencias.
9. Retirada de la regla de firewall al terminar.

Hasta que estos pasos se ejecuten y documenten, el uso físico por WiFi sigue
bloqueado para aceptación final.

## 11. Probar instalación y configuración

Incluya:

- construcción desde una copia limpia;
- inicio `preprod` contra una base vacía autorizada;
- rechazo de variables faltantes;
- rechazo de la clave pública de desarrollo;
- confirmación de que no se cargan usuarios o datos de demostración;
- salud local;
- detención ordenada;
- copia de seguridad y rollback documentados;
- HTTPS para todo acceso no local a `preprod`.

Si la infraestructura HTTPS todavía no existe, registre el caso remoto de
preproducción como bloqueado, no aprobado.

## 12. Gestionar una falla

1. No cambie el resultado observado.
2. Marque la prueba como fallida.
3. Conserve logs, captura, SQL y datos de entrada seguros.
4. Abra una referencia de defecto o incidencia.
5. Registre impacto y reproducción.
6. Corrija en una versión nueva.
7. Repita el caso fallido.
8. Ejecute la regresión proporcional al cambio.
9. Mantenga ambas ejecuciones en el historial.

Una prueba que pasa después de una corrección no elimina el fallo anterior.

## 13. Gestionar un bloqueo

Registre:

- recurso faltante;
- quién puede proporcionarlo;
- fecha de próxima revisión;
- casos afectados;
- riesgo para la decisión final.

Ejemplo actual: la prueba física por WiFi está bloqueada hasta disponer del GamePi20,
la red y la participación del propietario.

## 14. Reglas de evidencia

Cada ejecución debe indicar:

- ID de prueba;
- versión y configuración;
- fecha y hora UTC;
- persona ejecutora;
- datos de prueba;
- pasos realizados;
- resultado esperado;
- resultado observado;
- estado;
- rutas de evidencia;
- defecto relacionado;
- revisión o aprobación.

Nunca conserve claves, sesiones de acceso, hashes o un archivo `.env`.

Las capturas deben mostrar el resultado relevante y no una pantalla preparada que no
provenga de la ejecución.

## 15. Decidir la aceptación

Antes de aprobar una versión:

- todos los casos obligatorios están aprobados;
- no hay casos críticos bloqueados;
- cada falla tiene disposición y riesgo aprobado;
- la reconciliación de existencias es correcta;
- la prueba manual de navegador está aprobada;
- la prueba física está aprobada si el alcance incluye dispositivo y WiFi;
- no se incluyeron funciones excluidas;
- la instalación y recuperación fueron ensayadas;
- toda evidencia cita la versión exacta;
- el responsable del proyecto firma la decisión.

Si una condición falta, la decisión debe ser “no aceptada” o “aceptación limitada”
con alcance y riesgo explícitos.

Utilice la
[Plantilla de ejecución de prueba](plantillas/registro-de-ejecucion-de-prueba.md)
y la [Guía rápida de pruebas](referencia-rapida/guia-rapida-pruebas.md).
