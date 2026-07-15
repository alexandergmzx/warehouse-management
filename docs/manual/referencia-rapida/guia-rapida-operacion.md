# Guía rápida de operación

## Antes de iniciar

- Confirme versión, ambiente y perfil.
- Confirme PostgreSQL y credenciales.
- Confirme copia de seguridad antes de migraciones nuevas.
- Confirme versión anterior estable.
- Para acceso remoto a `preprod`, confirme la terminación HTTPS aprobada.
- Avise la ventana de servicio.

## Inicio

1. Compruebe PostgreSQL.
2. Cargue la configuración del ambiente.
3. Inicie la aplicación.
4. Espere el final de las migraciones.
5. Compruebe salud en `localhost`.
6. Compruebe salud desde la red del almacén; en `preprod`, solo mediante HTTPS.
7. Abra el tablero y revise `Last refreshed`.
8. Confirme que los registros se están conservando.

## Diagnóstico rápido

| Situación | Primera revisión |
|---|---|
| Salud local falla | Aplicación, configuración y PostgreSQL |
| Salud local funciona, red falla | HTTPS, firewall, dirección y ruta de red |
| Tablero no actualiza | Sesión, navegador y registros de aplicación |
| Varias terminales sin conexión | Salud desde red y equipo de comunicaciones |
| `APPLICATION FAILED TO START` | Nombre de variable indicado; no copie secretos |

## Detención

1. Avise a supervisión.
2. Espere que las terminales eliminen `Q:`.
3. Detenga ordenadamente la aplicación.
4. Compruebe que salud dejó de responder.
5. Registre hora y resultado.

## Nunca haga esto

- No use `dev` como ambiente real.
- No exponga HTTP directo de `preprod` fuera del servidor.
- No abra PostgreSQL a la red de terminales.
- No edite una migración aplicada.
- No use `flyway repair` para ocultar cambios.
- No use `docker compose down -v` fuera de desarrollo local.
- No reinicie la aplicación para corregir una falla que solo ocurre desde la red.
- No despliegue una migración nueva sin copia de seguridad.

## Evidencia mínima

Versión, ambiente, operador, configuración, copia de seguridad, salud local, salud
desde red, tablero, prueba de terminal, registros y resultado final.
