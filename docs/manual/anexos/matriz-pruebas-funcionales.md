# Matriz de pruebas funcionales FT-01 a FT-19

**Fuente oficial:**
[`docs/functional-test-specification.md`](../../functional-test-specification.md)\
**Estado ejecutado:**
[`docs/executed-test-report.md`](../../executed-test-report.md)

Esta traducción ayuda a ejecutar y revisar los casos. La especificación oficial y
las evidencias conservadas determinan el resultado final.

| ID | Caso en español | Resultado esperado |
|---|---|---|
| FT-01 | Iniciar sesión con preparador y terminal válidos; cerrar sesión | Se crea una sesión opaca, no se registra su valor y el cierre repetido es seguro |
| FT-02 | Crear una orden que necesita existencias de dos ubicaciones | La asignación completa usa ubicaciones en orden; si no alcanza, no queda una orden parcial |
| FT-03 | Enviar contenido mal formado, sin sesión y con permiso insuficiente | Se devuelven códigos estables y `correlationId`; ningún dato cambia |
| FT-04 | Dos personas solicitan trabajo al mismo tiempo; una persona o terminal intenta tomar otra tarea | Cada tarea se asigna una sola vez y solo existe una tarea activa por persona/terminal |
| FT-05 | Escanear ubicación y artículo incorrectos | Se devuelve el error correcto y no avanzan tarea, existencias, movimientos ni historial |
| FT-06 | Escanear ubicación y artículo correctos en orden | La tarea queda lista para confirmar; repetir escaneos correctos no retrocede ni duplica |
| FT-07 | Confirmar cero, más cantidad o cantidad parcial | Toda cantidad diferente se rechaza sin cambiar existencias o movimientos |
| FT-08 | Confirmar la cantidad exacta y repetir el mismo identificador | Existe un solo descuento y un solo movimiento `PICK`; la repetición devuelve el resultado original |
| FT-09 | Bloquear una tarea asignada y reanudarla | La razón queda auditada, se libera la asignación, vuelve a `AVAILABLE` y no cambian las existencias |
| FT-10 | Terminar todas las tareas de una línea y luego de una orden | Línea y orden terminan únicamente cuando todo su trabajo aprobado terminó |
| FT-11 | Intentar modificar o eliminar un movimiento de existencias | La base y la aplicación rechazan el cambio; el historial permanece intacto |
| FT-12 | Reutilizar un identificador de confirmación con contenido diferente | Se devuelve `CONFIRMATION_ID_REUSED` y no existe un segundo cambio de existencias |
| FT-13 | Comparar existencias contra movimientos | La consulta coincide en datos correctos y detecta una diferencia introducida para la prueba |
| FT-14 | Probar sesión vencida/cerrada y usuario/terminal inactivos | Todo acceso falla de forma segura con códigos estables |
| FT-15 | Iniciar `preprod` sin configuración o con clave insegura | El inicio se detiene con explicación segura y sin revelar secretos |
| FT-16 | Cambiar existencias e inspeccionar logs | Los logs permiten correlacionar la operación y no incluyen secretos |
| FT-17 | Generar dos veces etiquetas de ubicación y artículo | Contenido exacto, archivos repetibles y códigos legibles |
| FT-18 | Abrir el tablero y esperar un intervalo | Solo administración entra; la tabla se actualiza sin recargar toda la página |
| FT-19 | Revisar que no aparezcan funciones excluidas | No hay salto en terminal, preparación parcial, acceso directo a DB, TCP, planificador ni reintento de transporte |

## Agrupación

- FT-01 a FT-14: API, base de datos y reglas funcionales.
- FT-15 y FT-16: configuración y observabilidad.
- FT-17 y FT-18: etiquetas, navegador y operación.
- FT-19: inspección de alcance y arquitectura.

## Reglas de ejecución

- Use datos deterministas de desarrollo.
- Registre versión, configuración, migración, fecha, persona y evidencia.
- No registre claves, sesiones de acceso ni hashes.
- Un caso bloqueado no es aprobado.
- Una compilación correcta no demuestra un caso funcional.
- Una falla permanece en el historial después de corregirse.
