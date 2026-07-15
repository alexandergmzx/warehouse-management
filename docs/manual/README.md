# Manual del sistema de gestión de almacén

**Estado:** borrador inicial\
**Idioma:** español\
**Sistema cubierto:** Miniature Warehouse Management System y terminal de mano
HandheldPi

Este manual explica cómo usar, supervisar y mantener el sistema según la
responsabilidad de cada persona. No es necesario leerlo completo: cada lector
puede comenzar por la guía correspondiente a su trabajo.

## ¿Por dónde empiezo?

| Si usted... | Empiece aquí | Estado |
|---|---|---|
| Prepara pedidos con la terminal de mano | [Manual del preparador de pedidos](01-preparador-de-pedidos.md) | Borrador inicial |
| Ya recibió capacitación y necesita recordar el flujo | [Guía rápida del preparador](referencia-rapida/guia-rapida-preparador.md) | Borrador inicial |
| Supervisa a los preparadores y resuelve incidencias | [Manual del supervisor](02-supervisor-de-almacen.md) | Borrador inicial |
| Ya recibió capacitación como supervisor y necesita recordar el flujo | [Guía rápida del supervisor](referencia-rapida/guia-rapida-supervisor.md) | Borrador inicial |
| Da de alta artículos, ubicaciones, existencias u órdenes | [Manual del administrador de almacén](03-administrador-de-almacen.md) | Borrador inicial |
| Ejecuta las acciones administrativas mediante la interfaz REST | [Referencia técnica de administración](anexos/referencia-tecnica-administracion.md) | Borrador inicial |
| Instala, inicia o detiene el sistema | [Manual de instalación y operación](04-instalacion-y-operacion.md) | Borrador inicial |
| Opera el sistema en Windows | [Anexo técnico de Windows](anexos/operacion-windows.md) | Borrador inicial |
| Opera el sistema en Linux Mint | [Anexo técnico de Linux](anexos/operacion-linux.md) | Borrador inicial |
| Atiende fallas del sistema | [Manual de soporte e incidencias](05-soporte-e-incidencias.md) | Borrador inicial |
| Analiza logs o ejecuta consultas de diagnóstico | [Referencia técnica de diagnóstico](anexos/referencia-tecnica-diagnostico.md) | Borrador inicial |
| Necesita interpretar un código de error | [Catálogo de errores en español](anexos/codigos-de-error.md) | Borrador inicial |
| Registra una incidencia | [Plantilla de incidencia](plantillas/registro-de-incidencia.md) | Borrador inicial |
| Ejecuta pruebas de aceptación | [Manual de pruebas y aceptación](06-pruebas-y-aceptacion.md) | Borrador inicial |
| Necesita consultar los casos FT-01 a FT-19 en español | [Matriz funcional en español](anexos/matriz-pruebas-funcionales.md) | Borrador inicial |
| Registra una ejecución de prueba | [Plantilla de ejecución](plantillas/registro-de-ejecucion-de-prueba.md) | Borrador inicial |
| Mantiene o amplía el software | [Guía de desarrollo y mantenimiento](07-desarrollo-y-mantenimiento.md) | Borrador inicial |
| Necesita ubicar un módulo, estado o fuente técnica | [Mapa técnico del proyecto](anexos/mapa-tecnico-del-proyecto.md) | Borrador inicial |
| Revisa si un cambio está completo | [Lista de verificación de cambio](plantillas/lista-de-verificacion-de-cambio.md) | Borrador inicial |
| Es responsable del proyecto, la seguridad o una auditoría | [Guía de gobierno, seguridad y auditoría](08-gobierno-seguridad-y-auditoria.md) | Borrador inicial |
| Necesita saber quién decide o ejecuta cada actividad | [Matriz de responsabilidades](anexos/matriz-de-responsabilidades.md) | Borrador inicial |
| Revisa riesgos antes de autorizar un uso | [Registro inicial de riesgos](anexos/registro-inicial-de-riesgos.md) | Requiere aprobación del responsable |
| Documenta una revisión o decisión | [Plantilla de revisión y autorización](plantillas/revision-y-autorizacion.md) | Borrador inicial |
| Integra una terminal u otro cliente con la API | [Guía de integración HHT, API y MFC](09-integracion-hht-api-y-mfc.md) | Borrador inicial |
| Ya conoce el contrato y necesita un recordatorio | [Guía rápida de integración](referencia-rapida/guia-rapida-integracion.md) | Borrador inicial |
| Implementa o diagnostica el cliente HHT | [Referencia técnica de integración HHT](anexos/referencia-integracion-hht.md) | Borrador inicial |
| Evalúa una conexión futura con MFC | [Guía para una futura integración MFC](anexos/guia-futura-integracion-mfc.md) | Preparación; MFC real no implementado |
| Registra una prueba de integración | [Lista de verificación de integración](plantillas/lista-verificacion-integracion.md) | Borrador inicial |
| Capacita al personal o mantiene el manual | [Guía de capacitación y publicación](10-capacitacion-y-publicacion.md) | Borrador inicial |
| Prepara una sesión para un perfil concreto | [Plan de capacitación por perfil](anexos/plan-de-capacitacion-por-perfil.md) | Propuesta inicial |
| Ya conoce el método y necesita un recordatorio para capacitar | [Guía rápida del capacitador](referencia-rapida/guia-rapida-capacitador.md) | Borrador inicial |
| Registra una sesión de capacitación | [Registro de capacitación](plantillas/registro-de-capacitacion.md) | Borrador inicial |
| Comprueba si las instrucciones funcionan para el público | [Validación de instrucciones con usuario](plantillas/validacion-de-instrucciones-con-usuario.md) | Borrador inicial |
| Revisa qué falta para publicar cada capítulo | [Matriz de validación del manual](anexos/matriz-de-validacion-del-manual.md) | Control inicial |
| Prepara o retira una edición del manual | [Control de versión y publicación](plantillas/control-de-version-y-publicacion.md) | Borrador inicial |

Para conocer primero qué hace el sistema y quién participa, consulte
[Cómo funciona el sistema](00-como-funciona-el-sistema.md).

Si encuentra una palabra desconocida, consulte el
[glosario sencillo](anexos/glosario-sencillo.md).

## Reglas comunes para todas las personas

1. Cada persona debe utilizar su propia cuenta, gafete y clave.
2. Nunca se deben compartir claves ni copiar datos reales de acceso en este
   manual.
3. Una preparación solo se completa con la cantidad exacta indicada por el
   sistema.
4. El preparador no puede saltar una tarea. Si hay un problema debe avisar al
   supervisor.
5. Nunca se deben corregir existencias o tareas cambiando directamente la base
   de datos.
6. Una pantalla de error no significa que deba repetirse físicamente el
   movimiento. Primero debe comprobarse si el sistema lo registró.
7. Las acciones de recuperación deben quedar registradas para poder revisarlas
   después.

## Cómo está escrito este manual

- Las instrucciones para personal operativo usan frases cortas y pasos
  numerados.
- Cuando la terminal muestra un texto en inglés, el manual indica su significado
  en español y la acción que debe realizarse.
- **Resultado esperado** indica qué debe aparecer si el paso salió bien.
- **Deténgase y avise** indica que no debe continuar sin ayuda.
- **No haga esto** identifica una acción que puede provocar errores o pérdida de
  información.
- Los detalles técnicos se mantienen en las guías de soporte, instalación y
  desarrollo; no se mezclan con las instrucciones del preparador.

Las reglas editoriales completas se encuentran en
[Criterios de redacción y accesibilidad](criterios-de-redaccion.md).

## Alcance actual

Este proyecto es una prueba de concepto. El flujo principal y la conexión de la
terminal con el sistema ya fueron verificados en un entorno local. La prueba
completa mediante la red inalámbrica del almacén y el dispositivo físico sigue
pendiente. Hasta completar esa prueba, los pasos relacionados con el uso real en
el almacén deben considerarse instrucciones provisionales.

La pantalla de supervisión permite consultar tareas, pero actualmente no ofrece
botones para crear órdenes, ajustar existencias o bloquear y reanudar tareas.
Esas acciones administrativas requieren herramientas y permisos especiales y se
documentarán por separado.

La terminal usa una API REST implementada y dispone de una cola local para
continuar una tarea ya obtenida durante una interrupción. No puede tomar una tarea
nueva sin conexión. La extensión MFC actual solo escribe un log: no existe una
conexión TCP ni entrega a un sistema externo.

## Control del manual

Cada publicación deberá indicar:

- versión del manual;
- versión del sistema a la que corresponde;
- fecha de revisión;
- persona responsable de la revisión;
- capítulos modificados;
- pruebas prácticas realizadas con las instrucciones.

No se publicará una instrucción como verificada si solo fue revisada en el texto.
Una persona del público correspondiente deberá seguirla en el sistema y confirmar
el resultado.
