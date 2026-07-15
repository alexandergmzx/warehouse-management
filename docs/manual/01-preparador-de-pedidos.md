# Manual del preparador de pedidos

**Público:** personal que prepara pedidos con la terminal de mano\
**Nivel:** operativo, sin conocimientos técnicos\
**Estado:** borrador inicial; pendiente de validación completa en el dispositivo
físico y la red del almacén

## Su responsabilidad

Su trabajo consiste en recoger el artículo correcto, en la ubicación correcta y
por la cantidad indicada en la terminal.

Si la ubicación, el artículo o la cantidad no coinciden, no intente forzar la
confirmación. Deténgase y avise al supervisor.

## Lo que necesita antes de empezar

- Su gafete personal con código QR.
- Su clave numérica personal.
- Una terminal encendida y asignada al almacén.
- Etiquetas de ubicación y artículo legibles.
- Acceso a un supervisor cuando exista una diferencia.

**No haga esto:** no use el gafete ni la clave de otra persona. No preste los
suyos.

## Botones que utilizará

| Botón | Uso habitual |
|---|---|
| Flechas `▲` `▼` `◀` `▶` | Cambiar un número o moverse entre posiciones |
| `A` | Aceptar, confirmar o continuar |
| `B` | Regresar o volver a contar |
| `SELECT` | Ver el estado de la terminal |
| Mantener `START` | Cerrar la sesión al terminar el turno |

La cámara lee los códigos QR. Acerque la terminal a la etiqueta hasta que la
lectura sea aceptada. No es necesario pulsar un botón para escanear.

## 1. Revisar la terminal

1. Encienda la terminal.
2. Espere mientras aparece `HHT starting…`.
3. Compruebe la esquina superior derecha:
   - `ON` significa que hay conexión;
   - `OFF` significa que no hay conexión.
4. Si aparece `OFF` antes de iniciar una tarea, muévase a una zona con cobertura.
5. Si continúa en `OFF`, avise al supervisor.

**Resultado esperado:** aparece `SCAN BADGE`, que significa “escanear gafete”.

## 2. Iniciar sesión

1. Coloque el código QR de su gafete frente a la cámara.
2. Espere a que aparezca `PIN` seguido de su nombre de usuario.
3. Use `◀` y `▶` para elegir la posición del número.
4. Use `▲` y `▼` para cambiar cada número.
5. Pulse `A` cuando la clave esté completa.

**Resultado esperado:** aparece `READY`, que significa “listo”.

Si la clave no es aceptada:

1. Revise que haya usado su propio gafete.
2. Introduzca la clave una vez más con cuidado.
3. Si vuelve a fallar, deténgase y avise al supervisor. No siga intentando
   repetidamente.

Pulse `B` si necesita volver a la pantalla del gafete.

## 3. Tomar la siguiente tarea

1. Compruebe que la terminal muestra `ON`.
2. En la pantalla `READY`, pulse `A`.
3. Espere a que aparezca una ubicación grande debajo de `GO TO`.

`GO TO` significa “vaya a”. La pantalla también muestra el artículo y la cantidad
que se necesitarán después.

Si aparece `NO TASKS`, no hay tareas disponibles:

- pulse `A` para volver a consultar;
- pulse `B` para regresar a `READY`;
- avise al supervisor si esperaba recibir una tarea.

**No haga esto:** no apague la terminal después de recibir una tarea. La tarea ya
quedó asignada a usted y a ese dispositivo.

## 4. Ir a la ubicación y escanearla

1. Lea la ubicación grande que aparece en la pantalla.
2. Vaya físicamente a esa ubicación.
3. Compare el código visible de la estantería con el de la pantalla.
4. Escanee la etiqueta QR de la ubicación.

**Resultado esperado:** aparece una marca `✓` junto a `LOC` y después la pantalla
`PICK`.

Si aparece un mensaje rojo de ubicación incorrecta:

1. No recoja ningún artículo.
2. Vuelva a leer la ubicación indicada en la pantalla.
3. Compruebe el rótulo físico de la estantería.
4. Vaya a la ubicación correcta y escanéela.
5. Si la etiqueta correcta no puede leerse o no existe, avise al supervisor.

**No haga esto:** no escanee otra ubicación solamente para intentar avanzar.

## 5. Recoger y escanear el artículo

1. Lea el código y la descripción del artículo en la pantalla `PICK`.
2. Busque el artículo en la ubicación confirmada.
3. Compare el artículo físico con el código mostrado.
4. Escanee la etiqueta QR del artículo.

**Resultado esperado:** aparece la pantalla `COUNT`, que significa “contar”.

Si aparece un mensaje rojo de artículo incorrecto:

1. Devuelva el artículo al lugar donde estaba si lo había tomado.
2. Lea nuevamente el código y la descripción de la pantalla.
3. Busque y escanee el artículo correcto.
4. Si el artículo correcto no está en la ubicación, avise al supervisor.

**No haga esto:** no retire un artículo diferente aunque se parezca o tenga una
descripción similar.

## 6. Contar y confirmar

1. Cuente físicamente las unidades disponibles.
2. Compare su conteo con la cantidad solicitada que aparece en la pantalla.
3. Si coinciden, pulse `A` para confirmar.
4. Si no coinciden, use `▲` o `▼` para mostrar la cantidad que encontró y pulse
   `A`.

Cuando la cantidad no coincide, aparece `COUNT MISMATCH`, que significa “la
cantidad no coincide”, junto con `call supervisor`.

En ese caso:

1. Pulse `B` para volver a contar.
2. Cuente las unidades por segunda vez.
3. Si ahora coincide la cantidad, pulse `A` para confirmar.
4. Si continúa sin coincidir, deténgase y avise al supervisor.

**No haga esto:** no cambie el número para que coincida sin haber contado las
unidades. El sistema no permite confirmar una cantidad diferente.

## 7. Comprobar la confirmación

Después de una confirmación correcta aparece `PICK OK ✓`.

Debajo puede aparecer uno de estos mensajes:

| Mensaje | Significado | Qué debe hacer |
|---|---|---|
| `synced ✓` | La preparación ya quedó registrada | Pulse `A` para continuar |
| `stored offline — will sync` | La terminal guardó la preparación y la enviará cuando vuelva la conexión | Mantenga la terminal encendida y busque cobertura |

Si la preparación está guardada por falta de conexión, no vuelva a recoger el
mismo artículo. La terminal ya conserva el trabajo realizado.

Cuando vuelva la conexión, espere a que desaparezca el contador `Q:` de la parte
superior. Después podrá tomar otra tarea.

## 8. Trabajar cuando se pierde la conexión

La terminal puede terminar una tarea ya asignada aunque aparezca `OFF`.

1. Continúe escaneando la ubicación y el artículo indicados.
2. Cuente y confirme normalmente.
3. Mantenga encendida la terminal.
4. Regrese a una zona con cobertura.
5. Espere a que `OFF` cambie a `ON` y desaparezca `Q:`.

Mientras `Q:` muestre un número:

- hay información pendiente de enviar;
- no podrá tomar otra tarea;
- no debe cerrar la sesión;
- no debe apagar ni reiniciar la terminal salvo que soporte se lo indique.

## 9. Si aparece `SYNC FAILED`

`SYNC FAILED` significa que el sistema central rechazó una tarea que la terminal
había guardado. La pantalla muestra el número de tarea y un código del problema.

1. No vuelva a preparar físicamente el mismo artículo.
2. Anote o fotografíe el número de tarea y el código de la pantalla.
3. Avise al supervisor.
4. Pulse `A` únicamente después de que el supervisor haya tomado los datos y le
   indique continuar.

**Deténgase y avise:** el mensaje también indica `pick not booked`, es decir, la
preparación no quedó registrada en las existencias. El supervisor debe comprobar
qué ocurrió antes de decidir la recuperación.

## 10. Si la sesión vence

La terminal puede pedir nuevamente el gafete y la clave durante el turno.

1. No repita físicamente la preparación.
2. Escanee su gafete.
3. Introduzca su clave.
4. Mantenga la terminal encendida mientras envía cualquier información pendiente.

La terminal conserva las operaciones pendientes aunque vuelva a pedir el inicio de
sesión.

## 11. Consultar el estado

Pulse `SELECT` para abrir `STATUS`.

Los datos más útiles son:

| Texto | Significado |
|---|---|
| `Operator` | Persona que inició sesión |
| `Link ONLINE` | Hay conexión |
| `Link OFFLINE` | No hay conexión |
| `Queue 0 pending` | No hay información pendiente |
| `Queue` con un número mayor que cero | Hay información pendiente de envío |
| `Device` | Identificador de la terminal |

Pulse `SELECT` otra vez para cerrar esta pantalla.

## 12. Terminar el turno

1. Termine o entregue al supervisor cualquier tarea activa.
2. Compruebe que la terminal muestre `ON`.
3. Compruebe que no aparezca `Q:` o que `STATUS` muestre `Queue 0 pending`.
4. Mantenga pulsado `START` para cerrar la sesión.
5. Espere a que aparezca `SCAN BADGE`.
6. Coloque la terminal en el lugar definido para carga y entrega.
7. Informe cualquier golpe, daño, etiqueta ilegible o problema de batería.

Si la terminal no permite cerrar la sesión, revise si existe información pendiente.
Busque cobertura y espere. Si no se envía, avise al supervisor.

## Datos que debe dar al supervisor

Cuando informe un problema, indique:

- su nombre de usuario;
- el identificador `Device` de la terminal;
- el número de tarea, si aparece;
- la ubicación y el artículo mostrados;
- el mensaje exacto de la pantalla;
- la hora aproximada del problema;
- si la terminal mostraba `ON`, `OFF` o `Q:`.

No comunique su clave personal.

## Resumen rápido

1. Escanee su gafete e introduzca la clave.
2. Pulse `A` en `READY`.
3. Vaya a la ubicación mostrada y escanéela.
4. Recoja y escanee el artículo mostrado.
5. Cuente las unidades.
6. Confirme únicamente la cantidad exacta.
7. Espere `PICK OK ✓` y compruebe que quede sincronizado.
8. Ante una diferencia o `SYNC FAILED`, deténgase y avise al supervisor.

Para tener estos pasos en una sola página, utilice la
[Guía rápida del preparador](referencia-rapida/guia-rapida-preparador.md).
