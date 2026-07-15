# Criterios de redacción y accesibilidad

**Estado:** decisión editorial inicial\
**Aplicación:** todos los documentos de `docs/manual/`

## Decisiones aprobadas

1. Los manuales se escribirán en español.
2. Se utilizará español claro y neutral. Cuando sea útil para una operación en
   México, se podrá añadir el término local entre paréntesis.
3. Los manuales dirigidos a preparadores de pedidos y personas con poca
   experiencia tecnológica no exigirán conocimientos de programación, redes,
   API, bases de datos ni comandos de consola.
4. Los textos visibles en el prototipo que todavía estén en inglés se mostrarán
   tal como aparecen y se explicarán inmediatamente en español.
5. Los detalles técnicos se incluirán únicamente en los manuales de instalación,
   soporte, pruebas, desarrollo e integración.

## Niveles de contenido

| Nivel | Público principal | Forma de explicación |
|---|---|---|
| Operativo | Preparadores y personal con poca experiencia tecnológica | Pasos breves, una acción por paso, imágenes y resultado esperado |
| Supervisión | Supervisores y responsables del almacén | Procedimientos, decisiones permitidas, recuperación y escalamiento |
| Técnico | Instalación, soporte, pruebas y desarrollo | Parámetros, comandos, contratos e información de diagnóstico |

Un capítulo operativo no debe enviar al lector a un capítulo técnico para poder
terminar una tarea normal.

## Vocabulario preferido

| Evitar en una guía operativa | Usar |
|---|---|
| HHT | terminal de mano |
| backend o servidor | sistema central |
| endpoint | función administrativa o dirección del sistema |
| payload QR | contenido del código QR |
| token | sesión de trabajo |
| store-and-forward | guardar y enviar al recuperar la conexión |
| dead-letter | registro de una sincronización rechazada |
| claim task | tomar la siguiente tarea |
| stock | existencias |
| picker | preparador de pedidos |

La primera aparición de una sigla técnica debe escribirse con su significado. Por
ejemplo: “terminal de mano (HHT)”. Después se utilizará el término sencillo.

## Forma de redactar los pasos

- Comenzar cada paso con una acción: “Pulse”, “Escanee”, “Cuente” o “Avise”.
- Explicar una sola acción principal por paso.
- Indicar qué debe ver la persona después de una acción importante.
- Usar el nombre y el color que aparecen realmente en la pantalla.
- Evitar párrafos largos dentro de una secuencia.
- No usar solamente el color para explicar un estado; incluir también el texto o
  símbolo visible.
- No suponer que la persona entiende una abreviatura, un icono o una palabra en
  inglés.
- Colocar la solución junto al problema, no en un capítulo distante.

Ejemplo:

> Pulse **A** para pedir la siguiente tarea.\
> **Resultado esperado:** aparece una ubicación grande en la pantalla.\
> **Si aparece `NO TASKS`:** no hay tareas disponibles. Avise al supervisor si
> esperaba recibir trabajo.

## Advertencias

Se usarán tres tipos de aviso:

- **Resultado esperado:** confirma que el paso salió bien.
- **Deténgase y avise:** la persona no debe continuar sin un supervisor o soporte.
- **No haga esto:** la acción puede duplicar trabajo, ocultar un incidente o dañar
  información.

Las advertencias deben describir la acción concreta que se debe evitar. No se
utilizarán avisos genéricos como “tenga cuidado”.

## Imágenes

Las imágenes destinadas al personal operativo deberán:

- mostrar la pantalla completa y el botón que debe pulsarse;
- tener texto alternativo en español;
- ocultar claves, sesiones de acceso y datos personales;
- utilizar datos de demostración claramente identificados;
- corresponder a la versión publicada del sistema;
- ser legibles impresas y en una pantalla de teléfono.

Para cada flujo crítico se deben capturar, como mínimo, el inicio de sesión, la
ubicación, el artículo, la cantidad, la confirmación, la falta de conexión, la
discrepancia y el fallo de sincronización.

## Validación con lectores

Antes de publicar un capítulo operativo:

1. Una persona que no haya redactado el capítulo seguirá los pasos.
2. No recibirá explicaciones adicionales durante la prueba.
3. Se anotarán los pasos donde dude, se detenga o cometa un error.
4. Se corregirá el manual y se repetirá el ejercicio.
5. Se registrarán la versión probada, la fecha y el resultado.

El objetivo no es comprobar la memoria de la persona. El objetivo es demostrar
que el manual permite realizar el trabajo de forma segura.

## Uso en capacitación

- La capacitación debe enseñar con demostración y práctica, no solo lectura.
- Las guías rápidas son recordatorios posteriores y no sustituyen el manual.
- Los puntos críticos se demuestran individualmente; no se compensan con una
  calificación promedio.
- Un error observado puede indicar un problema del texto, la pantalla, el ambiente
  o la práctica; no debe atribuirse automáticamente a la persona.
- Las necesidades de letra, tiempo, idioma o apoyo se consultan de forma privada y
  respetuosa.

Consulte la [Guía de capacitación y publicación](10-capacitacion-y-publicacion.md).
