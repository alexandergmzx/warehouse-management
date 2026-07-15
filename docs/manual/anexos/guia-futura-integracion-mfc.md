# Guía para una futura integración MFC

**Público:** responsable del proyecto, arquitectura, desarrollo, operación y
proveedor MFC\
**Estado:** documento de preparación; no describe una integración implementada

## Advertencia de alcance

El WMS no tiene hoy transporte TCP ni otro envío real a MFC. Solo existen:

- el evento `OrderCompletionEvent`;
- el puerto `OrderCompletionPublisher`;
- el adaptador `NoopOrderCompletionPublisher`, que escribe un log;
- la configuración `WMS_MFC_ADAPTER=noop`.

No configure `tcp`: ese valor no está implementado ni soportado.

## Evento disponible

| Campo | Tipo | Uso previsto |
|---|---|---|
| `eventId` | UUID | Identificar el evento y detectar reenvíos |
| `orderId` | Número interno | Referencia técnica dentro del WMS |
| `orderNumber` | Texto | Referencia operativa de la orden |
| `completedAt` | Instante UTC | Momento de finalización |

El evento se crea una vez cuando la última línea de la orden queda completa. La
evidencia actual demuestra que el puerto se invoca una vez por orden completada.
No demuestra recepción por un sistema externo.

## Riesgo de la llamada actual

La llamada a `publish` ocurre de forma síncrona dentro de la transacción que
confirma la última tarea. El adaptador actual regresa de inmediato y no produce un
efecto externo.

Añadir una conexión de red en ese punto podría:

- mantener bloqueadas filas mientras espera al MFC;
- revertir una confirmación por una falla externa;
- producir un envío externo aunque luego falle el commit;
- degradar todas las confirmaciones cuando el MFC esté caído.

Por ello, una conexión real exige una decisión nueva sobre la frontera de
transacción.

## Decisión obligatoria de entrega

Compare las dos alternativas principales:

| Alternativa | Ventaja | Riesgo/costo |
|---|---|---|
| Publicar después del commit | Menor complejidad y sin red dentro de la transacción | Puede existir una ventana en la que la orden se confirma pero no se intenta publicar |
| Outbox transaccional | Guarda la orden y el mensaje de forma atómica; permite reintentos controlados | Requiere tabla, despachador, estados, limpieza, alertas y operación adicional |

La elección debe registrarse en un ADR. No se debe ocultar en el código de un
adaptador.

## Contrato que debe acordarse con MFC

### Transporte

- protocolo y versión;
- dirección, puertos y sentido de conexión;
- TCP, HTTP, mensajería u otra opción aprobada;
- cifrado, autenticación y certificados;
- codificación, longitud y delimitación de mensajes;
- latidos, reconexión y cierre.

### Mensaje

- nombre y versión del evento;
- campos obligatorios y opcionales;
- formato de fecha y zona horaria;
- límites de longitud y caracteres;
- significado de cada identificador;
- ejemplo válido y ejemplos rechazados;
- reglas de evolución compatible.

### Resultado

- qué significa “recibido”, “aceptado” y “procesado”;
- respuesta positiva y negativa;
- códigos de rechazo;
- tiempo máximo de espera;
- qué hacer con una respuesta desconocida;
- método de conciliación posterior.

## Idempotencia y reintentos

`eventId` debe permanecer igual en todos los intentos del mismo evento. El MFC debe
declarar cómo reconoce y responde a un duplicado.

Defina antes de implementar:

- intervalo y aumento entre reintentos;
- máximo de intentos o duración;
- errores recuperables y definitivos;
- estado de mensaje detenido;
- quién puede reanudar o volver a enviar;
- orden entre eventos;
- tratamiento de mensajes antiguos;
- protección contra una cola sin límite.

Nunca genere un `eventId` nuevo solo para lograr que un mensaje rechazado parezca
nuevo.

## Seguridad

- Use una identidad técnica exclusiva y de mínimo privilegio.
- Cifre el transporte fuera de una conexión local controlada.
- Restrinja origen, destino y puerto en el firewall.
- Proteja claves y certificados fuera del repositorio.
- No registre telegramas completos si contienen información sensible.
- Defina rotación, vencimiento y revocación de credenciales.
- Pruebe certificado inválido, identidad revocada y conexión no autorizada.

## Observabilidad necesaria

Cada intento debe permitir conocer:

- `eventId` y número de orden;
- versión del contrato;
- destino lógico, sin exponer secretos;
- número de intento;
- inicio, duración y resultado;
- código de rechazo seguro;
- estado final de entrega.

Defina métricas y alertas para cola pendiente, edad del evento más antiguo,
rechazos, tiempos de espera y destino no disponible. Debe existir una vista o
procedimiento de conciliación entre órdenes completas y eventos entregados.

## Pruebas mínimas

1. Una orden completa produce exactamente un evento lógico.
2. Un reenvío conserva `eventId` y el receptor no duplica su efecto.
3. Mensaje inválido queda rechazado con causa útil.
4. Tiempo de espera no bloquea la confirmación del WMS.
5. Reinicio del WMS conserva eventos pendientes si se elige outbox.
6. Caída prolongada no pierde mensajes ni crea una cola sin control.
7. Recuperación respeta el orden acordado.
8. Credencial o certificado inválido se rechaza de forma segura.
9. Logs y métricas permiten seguir un evento completo.
10. Una prueba de extremo a extremo usa el MFC o simulador contractual aprobado.

También deben ejecutarse las pruebas completas del WMS para demostrar que la nueva
integración no cambia existencias, idempotencia ni respuesta HHT.

## Puerta de autorización

No se inicia una implementación real hasta contar con:

- [ ] propietario funcional y técnico en ambos sistemas;
- [ ] alcance y criterio de aceptación firmados;
- [ ] contrato versionado;
- [ ] ADR de transacción y entrega;
- [ ] análisis de amenazas y reglas de red;
- [ ] estrategia de idempotencia y recuperación;
- [ ] ambientes o simulador contractual disponibles;
- [ ] plan de pruebas, operación, monitoreo y rollback;
- [ ] política de retención y conciliación;
- [ ] autorización para ampliar el alcance actual del proyecto.

## Fuentes técnicas

- [Arquitectura: punto de extensión MFC](../../architecture.md#mfc-extension-seam)
- [ADR 0007](../../decisions/0007-dashboard-label-and-mfc-contracts.md)
- [Matriz de configuración](../../configuration-matrix.md)
- [Guía de desarrollo](../07-desarrollo-y-mantenimiento.md)
