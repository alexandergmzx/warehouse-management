# Glosario sencillo

## Palabras de uso diario

| Palabra | Significado |
|---|---|
| Artículo | Producto que se guarda o se recoge en el almacén |
| Código de artículo | Identificador del producto; también puede aparecer como `SKU` |
| Existencias | Cantidad registrada de un artículo en una ubicación |
| Gafete | Identificación personal con un código QR para iniciar sesión |
| Orden | Solicitud que contiene uno o más artículos por preparar |
| Preparación | Acción de recoger los artículos de una orden |
| Preparador de pedidos | Persona que recoge y confirma artículos usando la terminal |
| Tarea | Una instrucción concreta: ubicación, artículo y cantidad |
| Ubicación | Lugar identificado dentro del almacén donde se guarda un artículo |

## Palabras de la terminal

| Palabra o texto | Significado |
|---|---|
| Terminal de mano | Dispositivo portátil que guía la preparación; en documentos técnicos puede llamarse HHT |
| `ON` | La terminal tiene conexión con el sistema central |
| `OFF` | La terminal no tiene conexión con el sistema central |
| `Q:` | Cantidad de pasos guardados que todavía deben enviarse |
| `READY` | La terminal está lista para pedir una tarea |
| `GO TO` | Vaya a la ubicación mostrada |
| `PICK` | Recoja el artículo mostrado |
| `COUNT` | Cuente las unidades |
| `PICK OK` | La preparación fue aceptada por la terminal |
| `NO TASKS` | No hay tareas disponibles en ese momento |
| `COUNT MISMATCH` | La cantidad contada no coincide con la solicitada |
| `SYNC FAILED` | El sistema rechazó información que la terminal había guardado |
| Sincronizar | Enviar al sistema central la información guardada por la terminal |
| Sesión | Periodo en el que una persona está identificada en la terminal |

## Palabras de supervisión

| Palabra | Significado |
|---|---|
| Bloquear una tarea | Detener administrativamente una tarea con problema y dejar registrada la razón |
| Reanudar una tarea | Devolver una tarea bloqueada a la lista de trabajo disponible |
| Ajuste de existencias | Corrección autorizada y registrada de una cantidad |
| Incidencia | Problema que afecta el trabajo y que debe investigarse o registrarse |
| Tarea atascada | Tarea que lleva demasiado tiempo activa sin avanzar |
| Historial | Registro de los cambios realizados en tareas y existencias |
| Tablero | Pantalla de consulta que muestra el estado reciente de las tareas |
| Datos maestros | Información básica y controlada de artículos y ubicaciones |
| Secuencia de recorrido | Número único reservado para ordenar ubicaciones en una mejora futura |
| Cantidad resultante | Existencias que quedan después de aplicar un aumento o una reducción |
| `AVAILABLE` | Tarea disponible para ser tomada |
| `ASSIGNED` | Tarea asignada a un preparador y una terminal |
| `LOCATION_CONFIRMED` | Tarea cuya ubicación ya fue confirmada |
| `ARTICLE_CONFIRMED` | Tarea cuyo artículo ya fue confirmado |
| `BLOCKED` | Tarea detenida administrativamente |
| `COMPLETED` | Tarea terminada y registrada |
| `STUCK` | Aviso de que una tarea activa lleva demasiado tiempo sin avanzar |

## Términos reservados para guías técnicas

El personal operativo no necesita conocer estos términos para preparar pedidos:

| Término | Explicación general |
|---|---|
| API | Forma técnica en que dos sistemas intercambian información |
| REST | Forma utilizada por la herramienta administrativa para comunicarse con el sistema |
| JSON | Formato de texto estructurado usado por la interfaz técnica |
| Perfil | Conjunto de configuración seleccionado al iniciar el sistema, como `dev` o `preprod` |
| Despliegue | Instalación o cambio de una versión del sistema en un ambiente |
| Migración | Cambio controlado y numerado de la estructura de la base de datos |
| Salud | Respuesta que confirma que la aplicación y su base de datos están disponibles |
| Firewall | Control de red que permite o bloquea conexiones según reglas autorizadas |
| Copia de seguridad | Copia protegida de los datos que puede utilizarse para una recuperación |
| Restauración | Recuperación de datos desde una copia o instantánea |
| Rollback | Regreso controlado a una versión anterior de la aplicación |
| Incidencia | Evento que afecta o pone en duda el trabajo y debe investigarse |
| Triaje | Primera revisión para conocer gravedad, alcance y responsable |
| Causa raíz | Motivo demostrado que originó una incidencia |
| Evidencia | Datos conservados que permiten demostrar qué ocurrió |
| Identificador de correlación | Código que une una solicitud con sus logs y movimientos relacionados |
| `confirmationId` | Código único de una confirmación; se conserva igual durante sus reintentos |
| Reconciliación | Comparación entre existencias actuales y su historial de movimientos |
| Prueba de integración | Prueba que comprueba varias partes reales trabajando juntas |
| Regresión | Repetición de pruebas para confirmar que un cambio no dañó funciones existentes |
| Testcontainers | Herramienta que crea bases PostgreSQL desechables para las pruebas automáticas |
| Evidencia de prueba | Salida, captura, log o consulta que demuestra el resultado de una ejecución |
| Caso bloqueado | Prueba que no pudo ejecutarse por faltar un recurso; no equivale a aprobada |
| Aceptación limitada | Aprobación restringida a un alcance concreto, con riesgos pendientes explícitos |
| Monolito modular | Una aplicación desplegada como una unidad, pero organizada en módulos con responsabilidades separadas |
| Controlador | Componente que recibe una solicitud y la entrega al servicio correspondiente |
| Servicio de aplicación | Componente que coordina una regla de negocio y su transacción |
| Repositorio | Componente que lee o guarda datos sin decidir el flujo de negocio |
| Idempotencia | Propiedad que permite repetir una solicitud segura sin aplicar dos veces el cambio |
| Commit | Confirmación definitiva de una transacción o identificación de un cambio en Git, según el contexto |
| Outbox | Patrón para guardar un evento junto con la transacción y enviarlo después de forma confiable |
| Store-and-forward | Guardar acciones durante una interrupción y enviarlas en orden cuando vuelve la conexión |
| Dead letter | Acción rechazada que se conserva para investigar y no se reintenta automáticamente |
| Telegrama | Mensaje con formato acordado entre sistemas industriales; el proyecto todavía no implementa telegramas MFC |
| Gobierno | Reglas que definen quién decide, quién ejecuta y cómo se demuestra una decisión |
| Control | Medida destinada a reducir o detectar un riesgo |
| Riesgo residual | Riesgo que permanece después de aplicar controles |
| Control compensatorio | Medida temporal o alternativa cuando el control principal todavía no existe |
| RPO | Cantidad máxima de datos que una recuperación podría perder |
| RTO | Tiempo objetivo para recuperar el servicio |
| SBOM | Inventario de componentes y dependencias incluidos en el software |
| Mínimo privilegio | Conceder únicamente los permisos necesarios para realizar una función |
| Base de datos | Lugar donde el sistema conserva su información estructurada |
| Correlation ID | Identificador que ayuda a soporte a seguir una solicitud en los registros |
| WMS | Siglas en inglés de sistema de gestión de almacén |
| MFC | Posible sistema futuro para controlar el flujo de materiales; no está implementado en este proyecto |

Cuando uno de estos términos sea necesario, el supervisor o soporte deberá explicar
la acción concreta que necesita del preparador.
