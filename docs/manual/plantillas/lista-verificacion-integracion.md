# Lista de verificación de integración

Use una copia por combinación de cliente, WMS, dispositivo, red y ambiente.

## Identificación

| Campo | Valor |
|---|---|
| Integración | |
| Fecha y hora | |
| Responsable | |
| Revisor | |
| Ambiente | dev / preprod / otro |
| Versión o commit del WMS | |
| Versión o commit del cliente | |
| Terminal y sistema operativo | |
| Red utilizada | bucle local / cable / Wi-Fi |
| Contrato y versión | |

## Preparación

- [ ] La combinación de versiones está identificada.
- [ ] El contrato oficial y las notas del cliente coinciden.
- [ ] La terminal está registrada, activa y tiene identificador único.
- [ ] Las cuentas de prueba están activas y no son credenciales públicas.
- [ ] La dirección del WMS es correcta.
- [ ] HTTPS está disponible cuando el acceso no es local en preproducción.
- [ ] Firewall, nombre de host, fecha/hora y certificados están comprobados.
- [ ] La ruta de cola es persistente y tiene permisos limitados.
- [ ] Los logs tienen espacio, permisos y rotación.
- [ ] Existe forma segura de interrumpir y recuperar la red para la prueba.

## Flujo normal

- [ ] Inicio de sesión correcto.
- [ ] Usuario inactivo rechazado.
- [ ] Terminal desconocida o inactiva rechazada.
- [ ] Se obtiene una sola tarea activa.
- [ ] `204` muestra que no hay tareas sin crear una falsa incidencia.
- [ ] Ubicación correcta aceptada y equivocada rechazada.
- [ ] Artículo correcto aceptado y equivocado rechazado.
- [ ] Cantidad exacta aceptada y distinta rechazada.
- [ ] Existencias se descuentan una sola vez.
- [ ] Tarea, línea y orden alcanzan el estado esperado.
- [ ] Cierre de sesión revoca la sesión.

## Repetición y recuperación

- [ ] Un escaneo correcto repetido no hace retroceder la tarea.
- [ ] Una confirmación repetida usa el mismo `confirmationId`.
- [ ] La repetición no descuenta existencias nuevamente.
- [ ] Mismo UUID con cantidad distinta se rechaza.
- [ ] Ningún `409` se convierte automáticamente en éxito.
- [ ] Una tarea interrumpida vuelve a la pantalla de su estado actual.

## Trabajo sin conexión

- [ ] No se puede obtener una tarea nueva sin conexión.
- [ ] Una tarea ya obtenida permite guardar ubicación, artículo y confirmación.
- [ ] La cola mantiene el orden correcto.
- [ ] `Q:` muestra la cantidad pendiente.
- [ ] Con pendientes no se toma otra tarea ni se cierra sesión.
- [ ] Al volver la red, los datos se envían una sola vez de forma efectiva.
- [ ] Un `5xx` o espera agotada conserva los datos pendientes.
- [ ] Una sesión inválida conserva la cola y solicita autenticación.
- [ ] Un rechazo deja evidencia y muestra `SYNC FAILED`.
- [ ] No se borró la cola para conseguir una prueba aprobada.

## Correlación y seguridad

- [ ] Cada solicitud usa un `X-Correlation-Id` UUID válido.
- [ ] El mismo valor aparece en la respuesta y en los logs del WMS.
- [ ] Un rechazo puede unirse entre el evento `wms_rejected` y el WMS.
- [ ] La falta actual de correlación HHT en respuestas exitosas quedó anotada o fue
      resuelta y probada.
- [ ] No aparecen PIN, claves, token Bearer ni hashes en logs.
- [ ] Los errores no exponen excepciones ni cuerpos arbitrarios.
- [ ] Los archivos de configuración, cola y log tienen acceso restringido.

## Hardware y uso real

- [ ] Se utilizó el modelo físico que se pretende autorizar.
- [ ] Se probaron escáner, botones, pantalla y alimentación.
- [ ] Se recorrieron las zonas reales de cobertura inalámbrica.
- [ ] Se probó pérdida de señal y recuperación durante una tarea.
- [ ] Hora, batería, reinicio y almacenamiento persistente son correctos.
- [ ] Una persona del público objetivo siguió las instrucciones del manual.

## Evidencia

| Evidencia | Ubicación o referencia |
|---|---|
| Comandos y resultado | |
| Logs HHT | |
| Logs WMS | |
| Correlation IDs de ejemplo | |
| Consultas de verificación | |
| Fotografías/capturas permitidas | |
| Incidencias encontradas | |

No copie secretos ni datos personales innecesarios en la evidencia.

## Resultado

| Campo | Valor |
|---|---|
| Casos aprobados | |
| Casos fallidos | |
| Casos bloqueados | |
| Riesgos pendientes | |
| Alcance aceptado | |
| Decisión | Aceptado / Aceptación limitada / Requiere corrección / Bloqueado |
| Firma o aprobación | |
