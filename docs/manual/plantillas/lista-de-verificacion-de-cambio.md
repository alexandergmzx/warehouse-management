# Lista de verificación de cambio

## Identificación

| Campo | Valor |
|---|---|
| Cambio | |
| Responsable | |
| Versión/rama | |
| Criterio de aceptación | |
| Riesgo | Bajo / Medio / Alto |

## Antes de implementar

- [ ] Revisé cambios existentes y no descarté trabajo ajeno.
- [ ] Identifiqué requisito y público afectado.
- [ ] Revisé ADR, arquitectura y API relacionados.
- [ ] Confirmé si cambia una regla protegida.
- [ ] Confirmé si requiere autorización o ADR nuevo.
- [ ] Identifiqué clientes y ambientes afectados.
- [ ] Diseñé migración, transacción, seguridad e idempotencia.
- [ ] Definí pruebas positivas, negativas y de recuperación.

## Implementación

- [ ] Controladores usan servicios.
- [ ] Transacción está en el servicio correcto.
- [ ] Bloqueos siguen el orden aprobado.
- [ ] Migración es nueva; no edité una aplicada.
- [ ] `devdata` no puede llegar a `preprod`.
- [ ] Validación y permisos están definidos.
- [ ] Error usa código estable y `correlationId`.
- [ ] Logs usan campos permitidos y no contienen secretos.
- [ ] Configuración tiene propietario, valor, sensibilidad y reinicio.
- [ ] No añadí alcance excluido de forma implícita.

## Pruebas

- [ ] Camino correcto.
- [ ] Entrada no válida.
- [ ] Permiso insuficiente.
- [ ] Concurrencia/bloqueo cuando corresponde.
- [ ] Idempotencia/reintento cuando corresponde.
- [ ] Rollback sin cambio parcial.
- [ ] Migración e integridad.
- [ ] Logs y redacción de secretos.
- [ ] Navegador, red o hardware cuando corresponde.
- [ ] `mvnw verify` correcto.
- [ ] CI correcto.

## Documentación

- [ ] `README.md` refleja el estado.
- [ ] `API.md` está actualizado.
- [ ] Arquitectura/ADR actualizado cuando corresponde.
- [ ] Matriz de configuración actualizada.
- [ ] Pruebas y trazabilidad actualizadas.
- [ ] Diagnóstico y runbook actualizados.
- [ ] Manuales por audiencia actualizados.
- [ ] Evidencia conservada con versión/configuración.

## Revisión final

- [ ] Revisé el diff completo.
- [ ] No hay secretos, dumps, `.env` o archivos de IDE.
- [ ] No hay reformateo no relacionado.
- [ ] No hay atribuciones de asistentes.
- [ ] Los riesgos pendientes están escritos.
- [ ] La definición de terminado se cumple.

## Resultado

| Campo | Valor |
|---|---|
| Verificación automática | |
| Verificación manual | |
| Evidencia | |
| Revisor | |
| Decisión | Aceptado / Requiere cambios / Bloqueado |
