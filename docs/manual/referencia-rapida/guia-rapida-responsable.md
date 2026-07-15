# Guía rápida del responsable

## Estado actual

- Desarrollo/demostración: evidenciado para versiones citadas.
- Integración HandheldPi local: evidenciada.
- WiFi físico: pendiente.
- `preprod` remoto: bloqueado sin HTTPS.
- Producción: no autorizada.

## Antes de autorizar

- Alcance y versión exactos.
- Pruebas automáticas y manuales.
- Reconciliación de existencias.
- Accesos y secretos.
- Copia y restauración.
- Red y HTTPS.
- Riesgos altos y responsables.
- Manuales actualizados.
- Evidencia conservada.

## Controles existentes

Argon2id, sesiones opacas, funciones `ADMIN`/`PICKER`, CSRF en tablero,
configuración `preprod` segura, PostgreSQL local aislado, históricos inmutables,
correlación y pruebas de integración.

## Bloqueos principales

- HTTPS no incluido.
- Sin administración de usuarios/terminales.
- Sin revocación global o límite de intentos.
- Sin privilegios mínimos DB documentados.
- Sin restauración demostrada.
- Sin retención/alertas operativas.
- Sin análisis de vulnerabilidades/SBOM.
- Sin política de datos personales.
- Sin aceptación física WiFi.

## Regla

No convierta una aprobación técnica de prueba de concepto en autorización
productiva. Toda autorización debe indicar alcance, límites, responsable, vigencia y
evidencia.
