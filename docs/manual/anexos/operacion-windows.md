# Operación en Windows

**Público:** operador técnico autorizado\
**Sistema:** Windows de 64 bits con PowerShell\
**Fuente técnica:** [`docs/runbook-windows.md`](../../runbook-windows.md)

Este anexo supone que el propietario ya instaló Java 21 y, cuando se utiliza,
Docker Desktop. No autoriza instalar o actualizar herramientas del equipo.

La fuente técnica todavía muestra una comprobación HTTP remota para `preprod`.
Este anexo aplica la regla de seguridad posterior y más estricta de ADR 0005:
todo acceso no local a `preprod` requiere HTTPS.

## 1. Comprobar herramientas

```powershell
java -version
.\mvnw.cmd -v
docker --version
docker compose version
```

Si se utilizará Docker, compruebe que el motor está activo:

```powershell
docker info --format "{{.ServerVersion}}"
```

Una instalación de Docker no es suficiente si Docker Desktop está detenido.

## 2. Construir el paquete

Desde la raíz del repositorio:

```powershell
.\mvnw.cmd -B package -DskipTests
```

Compruebe `BUILD SUCCESS` y la existencia de:

```text
target\warehouse-management-0.1.0-SNAPSHOT.jar
```

## 3. Preparar `preprod`

Solicite las credenciales al administrador de base de datos. No las guarde en el
repositorio.

```powershell
$env:SPRING_PROFILES_ACTIVE = "preprod"
$env:WMS_DB_URL = "jdbc:postgresql://<db-host>:5432/<db-name>"
$env:WMS_DB_USERNAME = "<preprod-username>"
$env:WMS_DB_PASSWORD = "<preprod-password>"
```

Estas variables solo permanecen en el proceso actual de PowerShell y en los
procesos iniciados desde él. Para un servicio permanente, utilice el mecanismo de
secretos y configuración aprobado por el responsable del equipo.

## 4. Iniciar

Con Maven:

```powershell
.\mvnw.cmd spring-boot:run
```

O con el paquete construido:

```powershell
java -jar target\warehouse-management-0.1.0-SNAPSHOT.jar
```

Mantenga la consola abierta si no existe un gestor de procesos externo.

## 5. Comprobar salud local

En otra ventana de PowerShell:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Espere una respuesta con `status` igual a `UP` antes de cambiar el firewall.

## 6. Crear una regla limitada de firewall para desarrollo

Esta acción requiere autorización administrativa. Sustituya la red de ejemplo por
el segmento real del almacén; nunca use `Any` sin una aprobación específica.

El siguiente comando es únicamente para un ensayo `dev` mediante HTTP en una red
confiable. No exponga de esta forma un ambiente `preprod`. El acceso no local a
`preprod` requiere una terminación HTTPS aprobada, que este proyecto no instala.

```powershell
New-NetFirewallRule -DisplayName "WMS API (LAN)" `
    -Direction Inbound -Protocol TCP -LocalPort 8080 `
    -RemoteAddress 192.168.1.0/24 `
    -Action Allow
```

Solo se abre el puerto de la aplicación. No abra el puerto `5432` de PostgreSQL a
la red de terminales.

## 7. Comprobar desde la red

Para el ensayo `dev`, desde otro equipo del mismo segmento:

```powershell
Invoke-RestMethod http://<server-lan-ip>:8080/actuator/health
```

Si la comprobación local funciona y esta falla, revise red y firewall. No reinicie
la aplicación como primera medida.

Para `preprod`, realice la comprobación mediante la dirección HTTPS entregada por
infraestructura. No pruebe el puerto HTTP directo desde otro equipo.

## 8. Detener

En la consola donde se ejecuta la aplicación, pulse:

```text
Ctrl+C
```

Espere a que termine el proceso y compruebe que salud ya no responde.

## 9. Retirar la regla de firewall

```powershell
Remove-NetFirewallRule -DisplayName "WMS API (LAN)"
```

Compruebe desde otro equipo que el puerto dejó de ser accesible.

## 10. Desarrollo local

Para el ambiente de demostración:

```powershell
Copy-Item .env.example .env
docker compose up -d
docker compose ps
.\mvnw.cmd spring-boot:run
```

Revise `.env` antes de iniciar y nunca lo confirme en Git.

Para detener sin eliminar el volumen:

```powershell
# Primero detenga la aplicación con Ctrl+C.
docker compose down
```

No añada `-v` salvo que quiera eliminar deliberadamente toda la base local de
desarrollo.
