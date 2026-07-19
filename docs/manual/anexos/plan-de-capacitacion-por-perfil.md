# Plan de capacitación por perfil

**Estado:** propuesta inicial; duración y tamaño de grupo deben ajustarse después
del piloto\
**Regla:** toda práctica utiliza datos de demostración y el ambiente autorizado

## Resumen

| Perfil | Duración inicial | Modalidad | Evidencia mínima |
|---|---:|---|---|
| Preparador de pedidos | 75 minutos | Demostración y práctica individual | Lista práctica completa |
| Supervisor | 90 minutos | Escenarios y decisiones | Dos incidencias resueltas correctamente |
| Administrador de almacén | 120 minutos | Taller con revisión por pares | Creación, ajuste y recuperación registrados |
| Instalación y operación | 120 minutos | Laboratorio técnico | Inicio, salud, parada y recuperación controlada |
| Soporte | 120 minutos | Simulacro de incidencia | Triaje, correlación y escalamiento registrados |
| Pruebas y aceptación | 90 minutos | Taller de evidencia | Ejecución con resultado aprobado, fallido o bloqueado |
| Desarrollo y mantenimiento | 120 minutos | Revisión técnica | Cambio de ejemplo revisado con checklist |
| Gobierno, seguridad y auditoría | 90 minutos | Mesa de revisión | Decisión limitada con riesgos y responsables |
| Integración HHT/API | 120 minutos | Laboratorio técnico | Flujo normal, sin conexión y rechazo trazados |
| Integración MFC | Sesión de 90 minutos | Laboratorio técnico | Contrato `TELEGRAMS.md`, ciclo de misión y operación (ADR 0011) explicados con el sustituto del WCS |
| Capacitador y propietario del manual | 90 minutos | Taller de facilitación | Validación con usuario y control de versión simulados |

Las duraciones son puntos de partida, no límites. Añada tiempo o sesiones
individuales cuando sea necesario.

## Preparador de pedidos

### Material

- manual del preparador;
- guía rápida;
- terminal física cargada;
- gafete de demostración;
- etiquetas de ubicación y artículo;
- unidades de demostración para contar;
- acceso a un supervisor de práctica.

### Ejercicios

1. Identificar botones, `ON`, `OFF` y `Q:`.
2. Iniciar sesión sin comunicar la clave.
3. Completar una tarea correcta.
4. Escanear de forma controlada una ubicación equivocada.
5. Escanear de forma controlada un artículo equivocado.
6. Encontrar una cantidad diferente y pedir ayuda.
7. Perder conexión después de recibir la tarea y sincronizar al regresar.
8. Interpretar un `SYNC FAILED` simulado sin repetir el movimiento.
9. Cerrar sesión y entregar la terminal.

### Criterio

Debe demostrar todos los puntos críticos de la guía de capacitación. Si necesita
ayuda en uno, repite ese escenario antes de trabajar sin acompañamiento.

## Supervisor

### Material

- manual y guía rápida de supervisor;
- tablero con datos de demostración;
- una terminal con tarea activa;
- formatos de incidencia;
- acceso coordinado a un administrador de práctica.

### Ejercicios

1. Explicar cada estado de tarea.
2. Revisar una tarea `STUCK` sin bloquearla automáticamente.
3. Resolver ubicación o artículo incorrectos.
4. Atender una diferencia de cantidad.
5. Atender `Q:` y evitar un bloqueo prematuro.
6. Registrar `SYNC FAILED` y escalar con los datos necesarios.
7. Preparar una entrega de turno completa.

## Administrador de almacén

### Ejercicios

1. Crear artículo y ubicación con segunda revisión.
2. Generar y comprobar una etiqueta.
3. Crear una orden válida y consultar sus tareas.
4. Rechazar una orden inválida sin improvisar otro identificador.
5. Preparar un ajuste con segundo conteo y razón concreta.
6. Simular una respuesta dudosa y comprobar antes de repetir.
7. Bloquear y reanudar una tarea con autorización.

Toda acción debe realizarse en desarrollo. El ejercicio no autoriza acceso de
administración a otro ambiente.

## Instalación y operación

### Ejercicios

1. Comprobar versiones y requisitos.
2. Iniciar PostgreSQL y WMS con el perfil correcto.
3. Interpretar salud correcta e incorrecta.
4. Revisar logs sin mostrar secretos.
5. Detener y volver a iniciar de forma controlada.
6. Aplicar un escenario de recuperación permitido.
7. Explicar por qué preproducción remota queda bloqueada sin HTTPS.

Ejecute la variante de Windows o Linux que corresponda al puesto.

## Soporte

### Ejercicios

1. Recibir una incidencia incompleta y solicitar datos seguros.
2. Clasificar alcance y gravedad.
3. Seguir un rechazo mediante `correlationId`.
4. Seguir una confirmación mediante tarea y `confirmationId`.
5. Comprobar estado antes de recomendar repetir una acción.
6. Conservar evidencia y escalar sin copiar secretos.
7. Cerrar un simulacro con causa, recuperación y prevención.

## Pruebas y aceptación

### Ejercicios

1. Identificar versión, ambiente y casos.
2. Registrar un caso aprobado con evidencia.
3. Registrar un caso fallido sin eliminarlo después de corregir.
4. Registrar como bloqueada una prueba sin dispositivo.
5. Explicar por qué `verify` no sustituye navegador, Wi-Fi o hardware.
6. Preparar una decisión de aceptación limitada.

## Desarrollo y mantenimiento

### Ejercicios

1. Identificar módulo y regla protegida de un cambio propuesto.
2. Revisar transacción, migración, seguridad e idempotencia.
3. Actualizar contrato y manual afectado.
4. Seleccionar pruebas proporcionales al riesgo.
5. Revisar un diff sin descartar cambios ajenos.
6. Demostrar la definición de terminado.

## Gobierno, seguridad y auditoría

### Ejercicios

1. Distinguir control implementado, parcial y pendiente.
2. Asignar propietario y fecha a un riesgo.
3. Redactar una aceptación limitada.
4. Rechazar una afirmación sin versión o evidencia.
5. Revisar permisos, retención, privacidad y continuidad.
6. Decidir qué bloquea preproducción remota y producción.

## Integración HHT/API

### Ejercicios

1. Configurar terminal registrada contra desarrollo.
2. Ejecutar el flujo REST completo.
3. Probar reintento con el mismo `confirmationId`.
4. Perder conexión y vaciar la cola en orden.
5. Revocar sesión con acciones pendientes.
6. Provocar un rechazo controlado y conservar el registro rechazado.
7. Unir logs de un rechazo con `X-Correlation-Id`.
8. Registrar la limitación de correlación de respuestas exitosas.

## Integración MFC

La integración está implementada (ADR 0011, `TELEGRAMS.md`); la sesión
recorre lo existente en lugar de decidirlo. El grupo debe completar:

- contrato `TELEGRAMS.md`: misiones TRANSPORT, estados y confirmaciones;
- outbox transaccional: por qué la red nunca ocurre en la transacción;
- reintentos, idempotencia por `eventId` y misiones `FAILED`;
- observabilidad: guía de logs y rastro SQL de misiones (§5);
- ejercicio con el sustituto del WCS (`scripts/wcs-standin/`);
- límites vigentes: SORT responde `501`; TCP crudo fuera de alcance.

## Capacitador y propietario del manual

### Ejercicios

1. Preparar una sesión sin datos reales.
2. Explicar un flujo con lenguaje adecuado al perfil.
3. Observar una práctica sin intervenir innecesariamente.
4. Registrar una duda como posible defecto del manual.
5. Validar una instrucción con usuario.
6. Preparar una nueva versión y retirar una copia anterior.

## Seguimiento

Repita la capacitación cuando:

- la persona todavía requiere ayuda en un punto crítico;
- cambia su función;
- cambia un paso o pantalla que utiliza;
- una incidencia demuestra una comprensión incorrecta extendida;
- no ha utilizado el proceso durante el periodo definido por el negocio.

El responsable del área debe definir la vigencia de la capacitación. Este plan no
impone por sí mismo un periodo laboral o legal.
