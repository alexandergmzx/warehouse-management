# Cómo funciona el sistema

## Para qué sirve

El sistema ayuda a preparar pedidos dentro de un almacén. Indica al preparador:

1. a qué ubicación debe ir;
2. qué artículo debe recoger;
3. qué cantidad debe recoger;
4. cuándo la preparación quedó registrada.

También conserva un historial de los movimientos de existencias y de los cambios
de cada tarea. Ese historial permite investigar errores sin depender únicamente
de la memoria de las personas.

## Personas que participan

| Persona | Responsabilidad principal |
|---|---|
| Preparador de pedidos | Recoger el artículo correcto, en la ubicación correcta y por la cantidad indicada |
| Supervisor | Vigilar el trabajo, ayudar con discrepancias y recuperar tareas bloqueadas |
| Administrador de almacén | Mantener artículos, ubicaciones, existencias, órdenes y etiquetas |
| Operador del sistema | Mantener el servicio disponible y correctamente configurado |
| Soporte | Investigar fallas y reunir evidencia antes de una recuperación |
| Responsable del proyecto | Aprobar reglas, accesos, cambios y resultados de aceptación |
| Equipo de desarrollo y pruebas | Mantener el software y comprobar que las reglas continúan funcionando |

En instalaciones pequeñas una persona puede tener más de una responsabilidad. Aun
así, debe seguir la guía correspondiente a la acción que está realizando.

## Recorrido normal de un pedido

1. El administrador registra artículos, ubicaciones y existencias.
2. El administrador crea una orden.
3. El sistema divide la orden en tareas de preparación.
4. El preparador inicia sesión en su terminal de mano.
5. La terminal entrega la siguiente tarea disponible.
6. El preparador escanea la ubicación.
7. El preparador escanea el artículo.
8. El preparador cuenta y confirma la cantidad exacta.
9. El sistema reduce las existencias y registra el movimiento.
10. Cuando todas las tareas terminan, la orden queda completada.

## Reglas que protegen el trabajo

- La ubicación debe confirmarse antes que el artículo.
- Un artículo incorrecto no permite avanzar.
- Solo se acepta la cantidad exacta indicada.
- La misma tarea no puede asignarse al mismo tiempo a dos personas.
- Una persona o una terminal solo puede tener una tarea activa.
- El preparador no puede saltar, bloquear ni reanudar tareas.
- Una tarea con problemas debe ser atendida por un supervisor.
- Las existencias cambian únicamente cuando la preparación se confirma
  correctamente.
- Repetir el envío de una confirmación por un fallo de red no debe descontar dos
  veces las existencias.

## Qué ocurre si se pierde la conexión

Para tomar una tarea nueva, la terminal necesita conexión con el sistema central.
Si la conexión se pierde después de tomar la tarea, la terminal puede guardar los
pasos realizados y enviarlos cuando vuelva la conexión.

Mientras haya información pendiente de enviar:

- la terminal muestra `OFF` o un contador `Q:` en la parte superior;
- no se debe apagar ni cerrar la sesión;
- no se podrá tomar otra tarea hasta terminar la sincronización;
- si el sistema rechaza lo guardado, el preparador debe avisar al supervisor.

## Límites actuales

Este sistema es una prueba de concepto, no un producto completo de almacén. Entre
otras limitaciones:

- no admite preparaciones parciales;
- no permite saltar tareas desde la terminal;
- no administra lotes, números de serie ni fechas de caducidad;
- no incluye control directo de transportadores o robots;
- la pantalla de supervisión permite consultar tareas, pero las acciones
  administrativas todavía se realizan mediante herramientas separadas;
- la validación completa de la terminal física a través de la red inalámbrica del
  almacén está pendiente.

Estas limitaciones deben explicarse durante la capacitación para evitar que una
persona espere funciones que el prototipo no ofrece.
