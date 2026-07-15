# Matriz de responsabilidades

**Estado:** propuesta inicial; el propietario debe asignar nombres y aprobarla.

Abreviaturas:

- **A:** aprueba y responde por el resultado.
- **R:** realiza la actividad.
- **C:** debe ser consultado.
- **I:** debe ser informado.

| Actividad | Propietario proyecto | Negocio almacén | Desarrollo | Operación | DBA | Red/seguridad | Soporte | Pruebas |
|---|---|---|---|---|---|---|---|---|
| Aprobar alcance y reglas | A | C | C | I | I | C | I | C |
| Diseñar cambio técnico | C | C | A/R | C | C | C | C | C |
| Aprobar ADR | A | C | R | C | C | C | I | I |
| Crear versión candidata | A | I | R | C | I | I | I | C |
| Preparar PostgreSQL | I | I | C | C | A/R | C | I | C |
| Gestionar secretos | A | I | C | R | C | R | I | I |
| Abrir red/HTTPS | A | I | C | C | C | R | I | C |
| Desplegar | A | I | C | R | C | C | I | C |
| Ejecutar pruebas | I | C | C | C | C | I | C | A/R |
| Aceptación de usuario | A | R | I | I | I | I | C | C |
| Gestionar cuentas | A | C | C | R | C | C | C | I |
| Monitorear servicio | I | I | C | A/R | C | C | R | I |
| Diagnosticar incidencia | I | C | C | C | C | C | A/R | I |
| Autorizar ajuste | A | R | I | I | C | I | C | I |
| Ejecutar recuperación | A | C | C | R | R cuando aplique | C | R | I |
| Restaurar base | A | I | C | C | R | C | C | C |
| Aceptar riesgo alto | A | C | C | C | C | C | I | I |
| Autorizar liberación | A | C | C | C | C | C | C | R |

## Responsabilidades de integración

Hasta que se asigne un equipo separado, desarrollo realiza la integración y el
propietario del proyecto responde por su aceptación.

| Actividad | Aprueba | Realiza | Consulta | Informa |
|---|---|---|---|---|
| Mantener contrato HHT `v1` | Propietario del proyecto | Desarrollo/integración | Soporte, pruebas y operación | Negocio almacén |
| Publicar combinación WMS–HHT compatible | Propietario del proyecto | Desarrollo/integración y pruebas | Operación y soporte | Negocio almacén |
| Configurar terminal y cola | Operación | Integración/operación | Red, seguridad y soporte | Pruebas |
| Autorizar prueba por Wi-Fi | Propietario del proyecto | Red/seguridad y pruebas | Integración y operación | Negocio almacén |
| Resolver rechazo de sincronización | Operación | Soporte | Integración, administración y negocio | Propietario del proyecto |
| Aprobar contrato MFC futuro | Propietario del proyecto | Desarrollo/integración | Proveedor MFC, seguridad, operación y negocio | Soporte y pruebas |
| Autorizar transporte MFC real | Propietario del proyecto | Desarrollo/integración y operación | Seguridad, proveedor MFC y pruebas | Negocio almacén |
| Mantener la copia oficial del manual | Propietario del proyecto | Propietario del manual | Responsables de cada contenido | Todas las personas usuarias |
| Validar instrucciones con usuarios | Responsable de negocio | Capacitador y pruebas | Supervisor y responsable técnico | Propietario del manual |
| Autorizar una publicación | Propietario del proyecto | Propietario del manual | Negocio, desarrollo, operación, seguridad y pruebas | Todas las personas afectadas |
| Retirar una versión anterior | Propietario del proyecto | Propietario del manual y supervisores | Capacitadores | Todas las personas afectadas |

## Asignación nominal

| Función | Persona/equipo | Sustituto | Contacto | Fecha de revisión |
|---|---|---|---|---|
| Propietario del proyecto | | | | |
| Responsable de negocio | | | | |
| Desarrollo | | | | |
| Operación | | | | |
| Base de datos | | | | |
| Red y seguridad | | | | |
| Soporte | | | | |
| Pruebas | | | | |
| Integración HHT/API | | | | |
| Integración MFC futura | | | | |
| Propietario del manual | | | | |
| Capacitación | | | | |

La matriz no concede permisos técnicos por sí misma. Las cuentas y funciones deben
coincidir con las responsabilidades aprobadas.
