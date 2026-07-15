# Plantilla de registro de incidencia

Copie esta plantilla para cada incidencia. Conserve la copia y sus anexos en
`docs/evidence/` durante ensayos, o en el sistema de incidencias autorizado para el
ambiente.

---

## Incidencia `<INC-AAAA-MM-DD-NN>`

| Campo | Valor |
|---|---|
| Detectada en UTC | |
| Reportada por | |
| Severidad | Crítica / Alta / Media / Baja |
| Versión y configuración | `<confirmacion-git>` + `dev/preprod` |
| Estado | Abierta / Diagnosticada / Recuperada / Cerrada |
| Responsable actual | |

### Resumen e impacto

Describa en una o dos frases qué se observó, quién fue afectado y qué trabajo se
detuvo o quedó en duda.

### Identificadores

| Identificador | Valor |
|---|---|
| `correlationId` | |
| Orden | |
| Tarea | |
| Usuario | |
| Terminal | |
| Artículo | |
| Ubicación | |
| Movimiento de existencias | |

No registre claves ni sesiones de acceso.

### Línea de tiempo

| Hora UTC | Evento | Fuente |
|---|---|---|
| | | Terminal / tablero / log / SQL / persona |

### Evidencia conservada

- [ ] Captura de la terminal o tablero, si corresponde.
- [ ] Líneas de log relacionadas.
- [ ] Consulta SQL de solo lectura y resultado.
- [ ] Estado de salud local y remoto.
- [ ] Versión/configuración obtenida del registro de despliegue.
- [ ] Evidencia revisada para excluir secretos.

### Diagnóstico

Explique qué demuestra la evidencia. Separe hechos, interpretación y datos todavía
desconocidos.

### Causa raíz

Indique la causa demostrada. Si sigue desconocida, escriba “desconocida” y asigne
una acción de seguimiento; no deje el campo vacío.

### Recuperación

| Campo | Valor |
|---|---|
| Acción ejecutada | |
| Interfaz utilizada | |
| Autorizada por | |
| Ejecutada por | |
| Hora UTC | |
| Identificador de transición/movimiento/cambio | |
| Resultado | |

Nunca registre una modificación SQL directa como recuperación permitida. Las tareas
y existencias deben cambiar mediante acciones auditadas o una migración nueva.

### Verificación posterior

- [ ] Salud correcta.
- [ ] Tablero actualizado.
- [ ] Tarea u orden en estado esperado.
- [ ] Movimiento de existencias comprobado.
- [ ] Terminal sin información pendiente sin atender.
- [ ] Usuario confirmó que puede continuar.

### Seguimiento

- [ ] Causa corregida o riesgo aceptado por el responsable.
- [ ] Acción preventiva asignada.
- [ ] Prueba nueva o actualizada, si apareció una brecha.
- [ ] Documentación actualizada.
- [ ] Evidencia conservada en la ubicación autorizada.

### Cierre

| Campo | Valor |
|---|---|
| Cerrada por | |
| Fecha y hora UTC | |
| Resultado final | |
| Riesgo pendiente | |
