# Guía de capacitación y publicación del manual

**Público:** capacitadores, responsables de cada área, propietario del manual y
responsable del proyecto\
**Nivel:** organización y enseñanza práctica\
**Estado:** borrador inicial; pendiente de ejecutar un piloto con cada perfil

## Objetivo

Esta guía explica cómo enseñar el uso del sistema y cómo convertir los borradores
en una versión publicada y controlada.

La capacitación no consiste en leer el manual en voz alta. Cada persona debe
practicar las acciones de su puesto y demostrar que sabe cuándo continuar, cuándo
detenerse y a quién avisar.

## Funciones necesarias

| Función | Responsabilidad |
|---|---|
| Propietario del manual | Mantener la versión oficial y retirar copias antiguas |
| Responsable de contenido | Confirmar que cada capítulo coincide con el sistema |
| Capacitador | Preparar el ambiente, demostrar y observar la práctica |
| Participante | Practicar con su propia función y comunicar dudas |
| Supervisor del participante | Confirmar que puede trabajar bajo las reglas del área |
| Responsable de pruebas | Conservar resultados y defectos encontrados |
| Responsable del proyecto | Autorizar el alcance de la publicación |

Una persona puede cumplir más de una función, pero el registro debe indicar cuál
desempeñó.

## Principios de capacitación

1. Utilice español sencillo y frases cortas.
2. Enseñe una acción a la vez.
3. Muestre primero; permita que la persona lo repita después.
4. Use datos y etiquetas de demostración.
5. No pida que una persona comunique su clave al grupo.
6. No corrija tomando el dispositivo de sus manos sin explicar el paso.
7. Permita volver a practicar sin presentar el error como castigo.
8. Evalúe la acción segura, no la velocidad ni la memoria del vocabulario inglés.
9. Incluya problemas reales: etiqueta incorrecta, cantidad diferente y pérdida de
   conexión.
10. Entregue la guía rápida solo después de enseñar el procedimiento completo.

## Adaptación para personas con poca experiencia tecnológica

Antes de comenzar, pregunte en privado si la persona necesita:

- letra impresa más grande;
- más tiempo para practicar;
- explicación de los botones y símbolos;
- repetición de la demostración;
- instrucciones leídas en voz alta;
- apoyo por idioma o dificultad visual, auditiva o motriz.

No etiquete a una persona como “poco tecnológica” frente al grupo. El objetivo es
adaptar la enseñanza, no clasificar al participante.

Para el preparador:

- evite explicar API, base de datos, red, token o UUID;
- diga “sistema central”, “información pendiente” y “sesión de trabajo”;
- use la terminal física y etiquetas del mismo tamaño que en el almacén;
- repita la relación entre pantalla, ubicación, artículo y cantidad;
- practique cómo pedir ayuda sin comunicar la clave.

## Antes de impartir una sesión

1. Identifique el perfil y la versión del sistema.
2. Seleccione los capítulos y ejercicios correspondientes.
3. Confirme que el manual pertenece a esa misma versión.
4. Prepare un ambiente de demostración separado de datos conservados.
5. Compruebe salud, cuentas, terminales, batería, red y etiquetas.
6. Prepare un escenario normal y los errores que se simularán.
7. Confirme quién atenderá una falla técnica durante la sesión.
8. Imprima o abra las guías rápidas sin mostrar documentos técnicos al preparador.
9. Prepare el [registro de capacitación](plantillas/registro-de-capacitacion.md).
10. Informe que la actividad evalúa las instrucciones y la práctica, no la dignidad
    ni capacidad personal.

No inicie si el ambiente contiene datos reales que puedan cambiarse por error.

## Estructura recomendada de una sesión

### 1. Explicar el propósito

Indique qué trabajo podrá realizar la persona y cuáles acciones no pertenecen a su
función.

### 2. Mostrar el flujo

El capacitador realiza una vez el procedimiento completo, despacio y diciendo por
qué se detiene en cada comprobación.

### 3. Practicar con acompañamiento

El participante repite el flujo. El capacitador puede responder preguntas, pero no
realiza la acción por la persona.

### 4. Practicar sin ayuda

El participante recibe un escenario y usa el manual. El capacitador observa en
silencio, excepto si existe riesgo de cambiar datos incorrectos o de repetir un
movimiento físico.

### 5. Practicar un problema

Incluya al menos un caso en que la persona deba detenerse y avisar. Para perfiles
operativos, no basta con practicar únicamente el camino correcto.

### 6. Cerrar y registrar

Revise dudas, indique dónde está la guía rápida y registre resultado, observaciones
y nueva capacitación necesaria.

## Método de demostración práctica

Utilice la secuencia “mostrar, practicar, explicar”:

1. El capacitador muestra la acción.
2. El participante realiza la acción.
3. El participante explica con sus propias palabras qué comprobó.

No exija repetir literalmente el manual. Una explicación sencilla es suficiente si
demuestra la decisión correcta.

## Evaluación del preparador

El preparador debe demostrar todos estos puntos críticos:

- iniciar sesión con su propio gafete sin revelar su clave;
- tomar una tarea únicamente con conexión;
- escanear ubicación y artículo en el orden correcto;
- rechazar físicamente una ubicación o artículo equivocados;
- contar y confirmar solo la cantidad exacta;
- volver a contar y avisar ante una diferencia;
- continuar una tarea ya asignada cuando aparece `OFF`;
- esperar cuando aparece `Q:`;
- no repetir el trabajo ante `SYNC FAILED`;
- cerrar sesión solamente cuando no hay información pendiente.

Los puntos críticos se evalúan como **Demostrado** o **Requiere nueva práctica**.
No se compensan con una calificación promedio. Si una acción crítica no se
demuestra, la persona necesita práctica adicional antes de operar sin supervisión.

## Evaluación de otros perfiles

### Supervisor

Debe interpretar estados, comprobar una tarea atascada, atender diferencias,
conservar datos de una incidencia y decidir correctamente entre continuar,
bloquear o escalar.

### Administrador

Debe revisar datos antes de crear, evitar una repetición después de respuesta
dudosa, ejecutar ajustes con autorización y comprobar el resultado.

### Operación y soporte

Deben iniciar y detener de forma controlada, reconocer una falla, preservar
evidencia, utilizar identificadores seguros y ejecutar solo recuperaciones
autorizadas.

### Pruebas

Debe distinguir aprobado, fallido y bloqueado; identificar versiones; conservar
evidencia y no sustituir hardware o red real con una prueba automática.

### Desarrollo e integración

Deben proteger reglas de negocio, contrato, idempotencia, transacciones y secretos.
Una integración HHT debe probar trabajo sin conexión. MFC real requiere una nueva
decisión y autorización.

### Gobierno, seguridad y auditoría

Deben distinguir lo implementado de lo pendiente, asignar responsables, revisar
riesgos y limitar cualquier aceptación al ambiente realmente probado.

El [plan por perfiles](anexos/plan-de-capacitacion-por-perfil.md) propone duración,
materiales y ejercicios para cada grupo.

## Qué hacer cuando el participante se equivoca

1. Detenga el ejercicio si puede producir un cambio incorrecto.
2. Pregunte qué esperaba ver.
3. Indique el paso del manual relacionado.
4. Permita que la persona vuelva a realizarlo desde un estado seguro.
5. Anote si la causa fue instrucción confusa, pantalla, ambiente o falta de
   práctica.
6. Corrija el manual si otra persona razonable podría cometer el mismo error.

No registre automáticamente todo error como “fallo del usuario”. La capacitación
también prueba la calidad del manual y del sistema.

## Validar una instrucción con usuarios

Antes de publicar un capítulo operativo:

1. seleccione una persona del público que no haya redactado el capítulo;
2. entregue únicamente el material que tendría durante el trabajo;
3. pida que siga el procedimiento sin explicación adicional;
4. observe dudas, retrocesos, errores y preguntas;
5. detenga solo por seguridad o para proteger datos;
6. registre el resultado en la
   [plantilla de validación](plantillas/validacion-de-instrucciones-con-usuario.md);
7. corrija y repita con una versión nueva.

Una revisión de escritorio no reemplaza esta validación.

## Estados de un documento

| Estado | Significado |
|---|---|
| Borrador | Contenido en preparación; no autorizado para trabajo sin acompañamiento |
| En revisión | Una persona responsable revisa exactitud y claridad |
| Validación práctica | El público objetivo sigue las instrucciones en el ambiente indicado |
| Aprobado con límite | Puede usarse únicamente en el alcance y hasta la fecha indicados |
| Publicado | Versión autorizada para el uso definido |
| Retirado | Ya no debe utilizarse; existe una versión posterior o el proceso cambió |

No use “final” como estado permanente. Todo manual puede requerir una versión
posterior.

## Reglas de versión

La portada o encabezado de una publicación debe indicar:

- identificador y versión del manual;
- versiones o commits del WMS y HandheldPi;
- ambiente y hardware cubiertos;
- fecha de publicación y próxima revisión;
- propietario y aprobador;
- estado y limitaciones;
- ubicación de la copia oficial.

Propuesta de numeración:

- `0.x`: borrador o piloto;
- `1.0`: primera publicación autorizada para un alcance concreto;
- aumento menor, por ejemplo `1.1`: aclaración o cambio compatible;
- aumento mayor, por ejemplo `2.0`: flujo, responsabilidades o pantallas que
  requieren nueva capacitación.

La numeración debe aprobarse antes de la primera publicación. No cambie un archivo
publicado sin cambiar su versión y registrar la modificación.

## Lista antes de publicar

- [ ] El alcance y las versiones están identificados.
- [ ] Cada afirmación de estado coincide con README y evidencia.
- [ ] Los capítulos tienen público, estado y responsable.
- [ ] Los textos visibles coinciden con la pantalla real.
- [ ] Las imágenes corresponden a la versión y tienen texto alternativo.
- [ ] No existen claves, sesiones, datos personales ni rutas privadas.
- [ ] Los enlaces y referencias funcionan.
- [ ] Los pasos críticos se probaron con el público objetivo.
- [ ] Las limitaciones aparecen junto a las instrucciones afectadas.
- [ ] El responsable técnico confirmó exactitud.
- [ ] El responsable de negocio confirmó utilidad.
- [ ] La publicación y sus anexos tienen la misma versión.
- [ ] Se definieron distribución, retiro y próxima revisión.
- [ ] La decisión quedó registrada.

Utilice la [plantilla de control de publicación](plantillas/control-de-version-y-publicacion.md)
y la [matriz de validación](anexos/matriz-de-validacion-del-manual.md).

## Distribución

Mantenga una sola ubicación como copia oficial. Cada copia impresa debe mostrar
versión y fecha. Las copias para capacitación deben marcarse como demostración si
no están autorizadas para operación.

Al publicar una versión nueva:

1. coloque la nueva versión en la ubicación oficial;
2. informe qué cambió y quién debe capacitarse de nuevo;
3. retire o marque las copias impresas anteriores;
4. conserve una copia histórica como evidencia, no como instrucción vigente;
5. actualice las guías rápidas junto con el manual completo;
6. confirme que supervisores conocen la fecha de entrada en vigor.

## Cuándo revisar o volver a capacitar

Revise el contenido cuando cambie:

- una pantalla, botón, mensaje o etiqueta;
- una regla de preparación o recuperación;
- una ruta administrativa o código de error;
- un perfil, permiso o responsabilidad;
- hardware, red, instalación o ambiente;
- el contrato entre WMS y HandheldPi;
- una medida de seguridad;
- un hallazgo de incidente, prueba o validación con usuario.

La nueva capacitación es obligatoria cuando el cambio afecta una decisión o acción
que la persona debe realizar. Un aviso por correo no sustituye la práctica cuando
cambia un paso crítico.

## Estado actual de publicación

Todos los documentos de esta carpeta siguen siendo borradores o documentos de
preparación. La conexión local está evidenciada, pero faltan la validación física
en GamePi20/Wi-Fi, las imágenes operativas de la terminal y los ejercicios con los
públicos correspondientes. Por eso todavía no existe una edición `1.0` autorizada
para uso de almacén.
