# Matriz de validación del manual

**Estado:** control inicial de borradores\
**Fecha de revisión documental:** 2026-07-15\
**Regla:** “pendiente” no significa fallado; significa que todavía no existe la
evidencia requerida

## Capítulos principales

| Documento | Público validador | Validación necesaria | Estado actual |
|---|---|---|---|
| `00` Cómo funciona | Persona nueva en el proyecto | Explicar flujo y funciones sin ayuda adicional | Pendiente |
| `01` Preparador | Preparador con poca experiencia tecnológica | Flujo completo en GamePi20 y Wi-Fi, incluidos errores | Pendiente; faltan prueba física e imágenes |
| `02` Supervisor | Supervisor de almacén | Turno, tarea atascada, diferencia y sincronización fallida | Pendiente |
| `03` Administrador | Administrador autorizado | Altas, orden, ajuste, bloqueo y reanudación en `dev` | Pendiente |
| `04` Instalación y operación | Operador Windows y operador Linux | Instalación, inicio, salud, parada y recuperación | Pendiente de ensayo completo |
| `05` Soporte | Soporte y supervisor | Simulacro desde aviso hasta cierre | Pendiente |
| `06` Pruebas | Responsable de pruebas | Ejecutar una campaña siguiendo solo el capítulo | Pendiente |
| `07` Desarrollo | Desarrollo/revisión técnica | Aplicar checklist a un cambio real | Pendiente |
| `08` Gobierno y seguridad | Propietario, seguridad y auditoría | Aprobar responsables, riesgos y límites | Pendiente de decisión |
| `09` Integración | Integración HHT y red | Local, errores, Wi-Fi física y trazabilidad | Parcial; local evidenciado, físico pendiente |
| `10` Capacitación y publicación | Capacitador y propietario del manual | Ejecutar piloto, validar y preparar una publicación | Pendiente |

## Anexos y referencias

| Grupo | Revisor | Comprobación | Estado actual |
|---|---|---|---|
| Glosario y códigos de error | Soporte y público operativo | Significado comprensible y acción correcta | Revisión documental realizada; práctica pendiente |
| Administración técnica | Administrador/desarrollo | Solicitudes coinciden con `API.md` | Revisión técnica pendiente |
| Operación Windows/Linux | Operador de cada plataforma | Comandos y resultados reales | Pendiente |
| Diagnóstico | Soporte/DBA | Consultas seguras y resultados interpretables | Pendiente |
| Matriz FT-01 a FT-19 | Pruebas | Correspondencia con especificación y evidencia | Revisión técnica pendiente |
| Mapa técnico | Desarrollo | Paquetes, estados y fuentes vigentes | Revisión técnica pendiente |
| Responsabilidades | Propietario y responsables de área | Asignación nominal y aprobación RACI | Pendiente |
| Riesgos | Propietario y seguridad | Prioridad, propietario, fecha y decisión | Pendiente |
| Integración HHT | Desarrollo HandheldPi/WMS | Contrato, cola y errores coinciden con código | Revisión documental realizada; aceptación pendiente |
| Integración MFC futura | Arquitectura y proveedor futuro | Puerta de decisión completa | No aplicable hasta autorizar alcance; conservar como preparación |

## Guías rápidas

Cada guía rápida debe comprobarse después de validar su manual completo:

- [ ] preparador;
- [ ] supervisor;
- [ ] administrador;
- [ ] operación;
- [ ] soporte;
- [ ] pruebas;
- [ ] cambios;
- [ ] responsable del proyecto;
- [ ] integración;
- [ ] capacitador.

Una guía rápida no se valida de forma aislada ni sustituye capacitación.

## Imágenes operativas requeridas

Los siguientes recursos deben capturarse desde la versión que se publicará:

| Pantalla | Estado |
|---|---|
| Inicio y `SCAN BADGE` | Pendiente |
| Entrada de `PIN` | Pendiente |
| `READY` y `GO TO` | Pendiente |
| `PICK` | Pendiente |
| `COUNT` y `COUNT MISMATCH` | Pendiente |
| `PICK OK` en línea y sin conexión | Pendiente |
| `STATUS` con `ON`, `OFF` y `Q:` | Pendiente |
| `SYNC FAILED` | Pendiente |
| Tablero de supervisor | Disponible como evidencia; validar contra versión candidata |
| Etiquetas de ubicación y artículo | Disponibles como evidencia; validar impresión real |

Cada imagen debe usar datos de demostración, ocultar credenciales, tener texto
alternativo en español y ser legible impresa.

## Controles documentales ya comprobados

- [x] Manuales redactados en español.
- [x] Guías del preparador sin jerga técnica, salvo textos reales de pantalla.
- [x] Contenido técnico separado por público.
- [x] Índice por función disponible.
- [x] Enlaces locales comprobados en la revisión documental citada.
- [x] Limitaciones de Wi-Fi física, HTTPS y MFC visibles.
- [x] Plantillas para pruebas, incidencias, cambios, integración y autorizaciones.
- [ ] Identificador de versión de publicación asignado.
- [ ] Propietarios nominales asignados.
- [ ] Validación práctica por público completada.
- [ ] Imágenes de terminal incorporadas.
- [ ] Primera publicación autorizada.

## Registro de validaciones

| Documento/versión | Fecha | Persona y función | Ambiente/hardware | Resultado | Evidencia | Cambios requeridos |
|---|---|---|---|---|---|---|
| | | | | | | |
