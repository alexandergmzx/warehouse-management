# Registro inicial de riesgos

**Estado:** evaluación inicial para revisión del propietario.\
**Escala:** impacto y probabilidad Baja / Media / Alta.\
**Regla:** la prioridad final y la aceptación pertenecen al responsable del proyecto.

| ID | Riesgo | Impacto | Prob. | Tratamiento recomendado | Estado/gate |
|---|---|---:|---:|---|---|
| RSK-01 | Acceso no local a `preprod` sin HTTPS | Alta | Alta | Implementar y probar terminación HTTPS | Bloquea acceso remoto |
| RSK-02 | No existe gestión operativa de usuarios y terminales | Alta | Alta | Diseñar alta, cambio, baja, funciones y evidencia | Bloquea uso operativo |
| RSK-03 | No hay revocación global; un login nuevo no invalida sesiones anteriores | Alta | Media | Diseñar sesiones activas y revocación por usuario/dispositivo | Bloquea producción |
| RSK-04 | No hay limitación de intentos; PIN de demostración corto | Alta | Alta | Política de credenciales, rate limiting y monitoreo | Bloquea producción |
| RSK-05 | Cuenta PostgreSQL de mínimo privilegio no documentada | Alta | Media | Separar migración/ejecución y documentar grants | Bloquea producción |
| RSK-06 | Copia y restauración completas no ensayadas | Alta | Media | Definir RPO/RTO y probar restauración aislada | Bloquea datos conservados críticos |
| RSK-07 | Logs sin retención gestionada ni versión por línea | Media | Alta | Plataforma de logs, retención y build/config ID | Abierto |
| RSK-08 | No hay servicio supervisado, métricas ni alertas | Alta | Media | Definir gestor, salud, métricas y alertas | Bloquea producción |
| RSK-09 | Prueba GamePi20 por WiFi pendiente | Alta | Alta | Ejecutar Stage 3 con evidencia | Bloquea aceptación física |
| RSK-10 | No existe pantalla administrativa completa | Media | Alta | Herramienta controlada o UI con pruebas y autorización | Aceptable solo para PoC técnico |
| RSK-11 | Acciones CI referenciadas por etiquetas mutables | Media | Media | Fijar acciones por SHA y documentar actualización | Abierto |
| RSK-12 | No hay análisis de vulnerabilidades, SBOM o firma | Alta | Media | Añadir proceso y herramientas aprobadas | Bloquea producción |
| RSK-13 | No existe política de datos personales y retención | Alta | Media | Evaluación legal/privacidad y política aprobada | Bloquea uso con personal real |
| RSK-14 | PostgreSQL remoto sin TLS configurado por la aplicación | Alta | Media | Política de DB privada/TLS y evidencia | Bloquea DB remota insegura |
| RSK-15 | Un ajuste negativo puede reducir por debajo de reservas abiertas | Alta | Media | Validación/reserva o control operativo probado | Requiere revisión antes de cada ajuste |
| RSK-16 | Ajuste de existencias no idempotente ante respuesta ambigua | Alta | Media | Idempotency key o verificación obligatoria antes de repetir | Control operativo actual |
| RSK-17 | Estado de algunos documentos antiguos está desactualizado | Media | Alta | Reconciliar cabeceras con evidencia actual | Abierto |
| RSK-18 | MFC real podría introducir red dentro de la transacción | Alta | Media | ADR y outbox/post-commit antes de transporte | Mitigado: ADR 0011 adoptó outbox transaccional; el adaptador `telegram` no hace red en la transacción, verificado por pruebas |
| RSK-19 | No existe perfil ni arquitectura de producción | Alta | Alta | Diseño productivo completo y aceptación | Bloquea producción |
| RSK-20 | El HHT no registra `X-Correlation-Id` en respuestas exitosas | Media | Alta | Registrar de forma segura solicitud y eco en todos los resultados; añadir pruebas de trazabilidad | Limita diagnóstico de éxitos |
| RSK-21 | El manual operativo de la terminal no contiene todavía las imágenes mínimas requeridas | Media | Alta | Capturar pantallas reales de la versión candidata, revisar secretos y validar legibilidad | Bloquea publicación operativa |
| RSK-22 | Los capítulos no han sido validados todavía por todos sus públicos objetivo | Alta | Alta | Ejecutar pilotos por perfil, corregir y repetir antes de publicar | Bloquea edición `1.0` para almacén |

## Seguimiento

| ID | Responsable | Fecha objetivo | Acción | Evidencia | Riesgo residual | Decisión |
|---|---|---|---|---|---|---|
| | | | | | | |

## Reglas

- No cierre un riesgo porque “no ocurrió todavía”.
- Cite evidencia para reducir impacto o probabilidad.
- Una aceptación debe indicar alcance, duración, responsable y controles
  compensatorios.
- Un riesgo que bloquea producción puede aceptarse para una demostración local si el
  alcance y las restricciones quedan explícitos.
