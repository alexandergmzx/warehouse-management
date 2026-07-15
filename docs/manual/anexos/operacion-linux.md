# Operación en Linux Mint

**Público:** operador técnico autorizado\
**Sistema:** Linux Mint 22 con Bash\
**Fuente técnica:** [`docs/runbook-linux.md`](../../runbook-linux.md)

Este anexo supone que el propietario ya instaló Java 21 y, cuando se utiliza,
Docker Engine. No autoriza instalar o actualizar herramientas del equipo.

La fuente técnica todavía muestra una comprobación HTTP remota para `preprod`.
Este anexo aplica la regla de seguridad posterior y más estricta de ADR 0005:
todo acceso no local a `preprod` requiere HTTPS.

## 1. Comprobar herramientas

```bash
java -version
./mvnw -v
docker --version
docker compose version
```

Si se utilizará Docker, compruebe el motor:

```bash
docker info --format '{{.ServerVersion}}'
```

Si falla, el propietario debe decidir si corresponde iniciar el servicio. No active
el inicio automático de Docker como parte implícita de este procedimiento.

## 2. Construir el paquete

Desde la raíz del repositorio:

```bash
./mvnw -B package -DskipTests
```

Compruebe `BUILD SUCCESS` y la existencia de:

```text
target/warehouse-management-0.1.0-SNAPSHOT.jar
```

## 3. Preparar `preprod`

```bash
export SPRING_PROFILES_ACTIVE=preprod
export WMS_DB_URL="jdbc:postgresql://<db-host>:5432/<db-name>"
export WMS_DB_USERNAME="<preprod-username>"
export WMS_DB_PASSWORD="<preprod-password>"
```

Estas variables permanecen durante la sesión de la terminal. Los procesos iniciados
desde ella las heredan.

**Precaución para pruebas:** una terminal que conserva
`SPRING_PROFILES_ACTIVE=preprod` puede hacer fallar la suite de integración. Ejecute
las pruebas desde otra terminal o retire esa variable para el comando:

```bash
env -u SPRING_PROFILES_ACTIVE ./mvnw -B verify
```

Para un servicio permanente, utilice el mecanismo de secretos y configuración
aprobado por el responsable del equipo.

## 4. Iniciar

Con Maven:

```bash
./mvnw spring-boot:run
```

O con el paquete:

```bash
java -jar target/warehouse-management-0.1.0-SNAPSHOT.jar
```

Mantenga la terminal abierta si no existe un gestor de procesos externo.

## 5. Comprobar salud local

En otra terminal:

```bash
curl http://localhost:8080/actuator/health
```

Espere `{"status":"UP"}` antes de cambiar el firewall.

## 6. Crear una regla limitada de firewall para desarrollo

Sustituya la red de ejemplo por el segmento real del almacén:

Estos comandos son únicamente para un ensayo `dev` mediante HTTP en una red
confiable. No exponga de esta forma un ambiente `preprod`. El acceso no local a
`preprod` requiere una terminación HTTPS aprobada, que este proyecto no instala.

```bash
sudo ufw allow from 192.168.1.0/24 to any port 8080 proto tcp comment "WMS API (LAN)"
sudo ufw status numbered
```

Si `ufw` está inactivo, activarlo es una decisión independiente para todo el equipo.
No ejecute `sudo ufw enable` sin coordinar los demás servicios.

No abra PostgreSQL, puerto `5432`, a la red de terminales.

## 7. Comprobar desde la red

Para el ensayo `dev`, desde otro equipo del mismo segmento:

```bash
curl http://<server-lan-ip>:8080/actuator/health
```

Si la comprobación local funciona y esta falla, revise red y firewall. No reinicie
la aplicación como primera medida.

Para `preprod`, realice la comprobación mediante la dirección HTTPS entregada por
infraestructura. No pruebe el puerto HTTP directo desde otro equipo.

## 8. Detener

En la terminal donde se ejecuta la aplicación, pulse:

```text
Ctrl+C
```

Espere la salida del proceso y compruebe que salud ya no responde.

## 9. Retirar la regla de firewall

Primero encuentre el número:

```bash
sudo ufw status numbered
```

Después elimine la regla correcta:

```bash
sudo ufw delete <numero-de-regla>
```

Compruebe desde otro equipo que el puerto dejó de ser accesible.

## 10. Desarrollo local

```bash
cp .env.example .env
docker compose up -d
docker compose ps
./mvnw spring-boot:run
```

Revise `.env` antes de iniciar y nunca lo confirme en Git.

Para detener sin eliminar el volumen:

```bash
# Primero detenga la aplicación con Ctrl+C.
docker compose down
```

No añada `-v` salvo que quiera eliminar deliberadamente toda la base local de
desarrollo.
