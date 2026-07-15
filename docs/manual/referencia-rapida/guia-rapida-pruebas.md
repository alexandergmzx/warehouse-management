# Guía rápida de pruebas

## Antes

- Identifique versión, cambios, perfil y herramientas.
- Use datos de desarrollo y una base desechable.
- Confirme Docker activo.
- Retire `SPRING_PROFILES_ACTIVE=preprod` de la terminal de pruebas.
- Prepare la ubicación de evidencia.

## WMS

```text
Linux:   env -u SPRING_PROFILES_ACTIVE ./mvnw -B verify
Windows: .\mvnw.cmd -B verify desde una PowerShell limpia
```

Use `verify`; `test` ejecuta cero pruebas de integración.

Compruebe pruebas, Failsafe, Checkstyle, SpotBugs y `BUILD SUCCESS`.

## Además de lo automático

- Reconciliación SQL.
- Tablero en navegador real.
- Escaneo de etiquetas.
- Integración HandheldPi contra WMS.
- GamePi20 y WiFi si forman parte del alcance.
- Inspección de funciones excluidas.

## Estados

- Aprobada: cumple y tiene evidencia.
- Fallida: no cumple; conservar y abrir defecto.
- Bloqueada: no pudo ejecutarse; nunca contar como aprobada.
- No aplica: necesita justificación.

## Evidencia mínima

ID, versión, configuración, fecha UTC, persona, datos, pasos, esperado, observado,
estado, rutas de evidencia y defecto.

## Nunca haga esto

- No acepte por compilación.
- No borre una falla después de corregirla.
- No marque como aprobada una prueba bloqueada.
- No pruebe destrucción contra una base conservada.
- No conserve claves, sesiones o `.env`.
- No acepte WiFi físico basándose solo en una simulación local.
