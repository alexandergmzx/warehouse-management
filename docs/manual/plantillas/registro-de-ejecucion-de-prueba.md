# Plantilla de ejecución de prueba

Copie esta plantilla para una prueba individual o adapte la tabla final para un pase
completo.

---

## Ejecución `<ID>-<AAAA-MM-DD>-<NN>`

| Campo | Valor |
|---|---|
| ID de prueba | FT- / HHT-TC- / Operación- |
| Fecha y hora UTC | |
| Persona ejecutora | |
| Persona testigo/revisora | |
| Repositorio y confirmación | |
| Cambios sin confirmar | Sí / No; detalle |
| Perfil y ambiente | |
| Sistema operativo | |
| Java / Maven / Docker / PostgreSQL | |
| HandheldPi / Python / dispositivo | Cuando corresponda |

### Objetivo

Indique la regla o riesgo que se comprobará.

### Condiciones previas

- [ ] Ambiente identificado.
- [ ] Datos preparados.
- [ ] Copia o base desechable cuando corresponde.
- [ ] Herramientas comprobadas.
- [ ] Evidencia y reloj UTC disponibles.
- [ ] No hay secretos en los datos que se conservarán.

### Datos de prueba

| Dato | Valor seguro |
|---|---|
| Orden | |
| Tarea | |
| Usuario de demostración | |
| Terminal | |
| Artículo | |
| Ubicación | |
| Cantidad | |
| Otros | |

No registre claves ni sesiones de acceso.

### Pasos y resultados

| # | Acción | Resultado esperado | Resultado observado | Evidencia |
|---:|---|---|---|---|
| 1 | | | | |

### Resultado

| Campo | Valor |
|---|---|
| Estado | Aprobada / Fallida / Bloqueada / No aplica |
| Inicio y fin UTC | |
| `correlationId` | |
| Defecto o incidencia | |
| Riesgo observado | |

### Evidencia

- [ ] Salida automática.
- [ ] Logs relacionados.
- [ ] Consulta SQL y resultado.
- [ ] Capturas reales.
- [ ] Archivos o hashes.
- [ ] Evidencia revisada para excluir secretos.

### Si falló

| Campo | Valor |
|---|---|
| Paso exacto | |
| Reproducción | |
| Impacto | |
| Defecto abierto | |
| Versión de corrección | |
| Nueva ejecución | |

### Si quedó bloqueada

| Campo | Valor |
|---|---|
| Recurso faltante | |
| Responsable de resolver | |
| Próxima revisión | |
| Riesgo para aceptación | |

### Revisión

| Campo | Valor |
|---|---|
| Revisada por | |
| Fecha UTC | |
| Decisión | Aceptada / Rechazada / Repetir |
| Comentarios | |

---

## Resumen de un pase completo

| ID | Estado | Evidencia | Defecto/nota |
|---|---|---|---|
| FT-01 | | | |

**Totales:** aprobadas ___, fallidas ___, bloqueadas ___, no aplican ___.

**Decisión:** aceptada / aceptación limitada / no aceptada.

**Aprobada por:**\
**Fecha UTC:**
