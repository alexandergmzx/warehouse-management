# Guía rápida para cambios

## Antes

1. Revise Git y preserve cambios existentes.
2. Defina criterio de aceptación.
3. Revise README, CLAUDE, API, arquitectura y ADR.
4. Identifique reglas, datos, clientes y manuales afectados.
5. Obtenga autorización si cambia alcance, versión fijada o regla protegida.

## Implemente

- Controlador → servicio → repositorio.
- Transacciones en servicios.
- Migración nueva; nunca editar una aplicada.
- Validación, permisos e idempotencia explícitos.
- Logs correlacionables sin secretos.
- Cambio mínimo y sin reformateo ajeno.

## Verifique

```text
Linux:   env -u SPRING_PROFILES_ACTIVE ./mvnw -B verify
Windows: .\mvnw.cmd -B verify desde PowerShell limpia
```

Además pruebe navegador, red, SQL o hardware cuando corresponda.

## Documente

- API y errores.
- Configuración.
- Migración y recuperación.
- Pruebas y trazabilidad.
- Diagnóstico y operación.
- Manuales por audiencia.
- Evidencia con versión exacta.

## Nunca haga esto

- No cambie una regla protegida implícitamente.
- No use Hibernate para modificar la estructura.
- No use `flyway repair` para ocultar cambios.
- No registre claves o sesiones.
- No añada TCP/MFC real dentro de la transacción actual.
- No declare terminado por compilación.
- No añada atribución de asistentes.
