# Referencia técnica de diagnóstico

**Público:** soporte técnico y administrador de base de datos\
**Fuentes oficiales:**
[guía de logs](../../log-analysis-guide.md) y
[consultas SQL](../../sql-diagnostics.md)

## Antes de consultar

Registre:

- fecha y hora UTC;
- ambiente y perfil;
- versión desplegada según el registro de despliegue;
- síntoma y alcance;
- orden, tarea, usuario, terminal, artículo y ubicación disponibles;
- `correlationId` cuando exista.

No utilice una cuenta con permisos de modificación para SQL si el administrador de
base de datos puede proporcionar una cuenta de diagnóstico de solo lectura.

## Ubicación de los logs

La aplicación escribe JSON estructurado en la salida estándar. El archivo o sistema
donde se conserva depende del proceso que aloja la aplicación:

- consola redirigida por el operador;
- registro del servicio de Windows;
- `journald` u otro gestor en Linux;
- plataforma externa de logs.

El proyecto no crea un archivo `app.log` ni administra retención por sí solo. En los
ejemplos siguientes, `app.log` representa la copia conservada por infraestructura.

## Filtrar logs con PowerShell

Todas las reglas de negocio, de más reciente a más antigua:

```powershell
Get-Content app.log | ForEach-Object { $_ | ConvertFrom-Json } |
    Where-Object { $_.message -eq 'business rule violation' } |
    Select-Object '@timestamp', problemCode, detail, correlationId |
    Sort-Object '@timestamp' -Descending
```

Una solicitud concreta:

```powershell
Get-Content app.log | ForEach-Object { $_ | ConvertFrom-Json } |
    Where-Object { $_.correlationId -eq '<uuid>' }
```

Preparaciones confirmadas de una orden:

```powershell
Get-Content app.log | ForEach-Object { $_ | ConvertFrom-Json } |
    Where-Object { $_.message -eq 'pick confirmed' -and $_.orderNumber -eq '<orden>' } |
    Select-Object taskNumber, articleSku, locationCode, quantity, durationMs
```

## Filtrar logs con `jq`

```bash
jq 'select(.message == "business rule violation")' app.log
jq 'select(.correlationId == "<uuid>")' app.log
jq 'select(.message == "stock adjusted") | {articleSku, quantityDelta, resultingQuantity, adminUserId}' app.log
```

No publique líneas completas sin revisarlas. Aunque los campos están controlados,
pueden contener identificadores operativos.

## Abrir una sesión SQL de desarrollo

Con PostgreSQL iniciado mediante Compose:

```bash
docker compose exec postgres psql -U wms -d wms
```

En `preprod`, utilice el método y la cuenta de solo lectura proporcionados por el
administrador de base de datos. No abra PostgreSQL a la red de terminales.

## Elegir la consulta

Ejecute el SQL directamente desde
[`docs/sql-diagnostics.md`](../../sql-diagnostics.md) para evitar que una copia
traducida quede desactualizada.

| Sección | Utilícela para |
|---|---|
| 1. `Find stuck picking tasks` | Tareas activas sin avance durante el umbral |
| 2. `Find stock discrepancies against movements` | Comparar existencias actuales con el historial de movimientos |
| 3. `Trace one order end to end` | Reconstruir orden, líneas, tareas, transiciones y movimientos |
| 4. `Integrity overview for shift handover` | Resumen antes o después de un turno o prueba |

Cambie únicamente el parámetro de orden o el umbral indicado por la propia guía.
Conserve la consulta, hora, resultado y ambiente.

## Interpretar resultados

### Tareas atascadas

Una fila indica una tarea activa que superó el umbral. No demuestra por sí sola una
falla. Compare con el preparador, la terminal y la conectividad antes de bloquear.

### Diferencias de existencias

El resultado normal es cero filas. Cualquier fila es una incidencia de integridad:

1. preserve logs y resultados;
2. detenga ajustes relacionados;
3. trace las órdenes y movimientos afectados;
4. no corrija con SQL;
5. escale antes de recuperar.

### Recorrido de orden

Ordene los eventos por hora y secuencia. Compare:

- creación de orden;
- cambios de tarea;
- estado actual;
- movimiento `PICK`;
- `correlationId` de cada evento.

Una tarea `COMPLETED` debe tener su movimiento de existencias correspondiente.

### Resumen de turno

Registre los conteos de tareas activas, atascadas, bloqueadas, órdenes completadas y
diferencias de existencias. Investigue cualquier diferencia distinta de cero.

## Diagnóstico de red

1. Compruebe `http://localhost:8080/actuator/health` en el servidor.
2. Para `dev`, compruebe desde la red autorizada usando HTTP.
3. Para `preprod`, compruebe únicamente mediante la dirección HTTPS aprobada.
4. Si local funciona y remoto falla, revise ruta, firewall, DNS y terminación HTTPS.
5. Si local falla, revise arranque, configuración y PostgreSQL.

No pruebe la conectividad de terminales abriendo PostgreSQL.

## Diagnóstico de arranque `preprod`

Una configuración ausente o insegura falla antes de abrir la conexión. Busque:

```text
APPLICATION FAILED TO START
Description:
Action:
```

La explicación nombra la variable, nunca debe mostrar su valor. Si aparece una
cadena de conexión o una clave en evidencia, detenga su distribución y aplique el
procedimiento de secretos del ambiente.

## Verificación posterior

Después de cualquier recuperación:

1. repita la comprobación que falló;
2. ejecute salud local y remota según el perfil;
3. vuelva a consultar la tarea u orden;
4. vuelva a ejecutar la reconciliación si hubo existencias;
5. confirme que no apareció un segundo movimiento;
6. adjunte el antes y después a la incidencia.

## Evidencia segura

Incluya:

- líneas necesarias alrededor del evento;
- salida SQL necesaria;
- `correlationId`;
- identificadores de negocio;
- versión obtenida del registro de despliegue.

Excluya:

- claves;
- encabezados `Authorization`;
- sesiones de acceso;
- hashes de claves;
- archivos `.env`;
- volcados completos sin revisión.
