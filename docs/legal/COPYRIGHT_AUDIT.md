# Registro de auditoría de copyright y licencias

## Alcance y resultado inicial

Esta auditoría cubre el árbol versionado de `warehouse-management` y su
historial Git hasta el 2026-07-15. El historial muestra contribuciones con los
dos correos de Alexander Gomez y no muestra otro autor. Es evidencia de
proveniencia, no una garantía legal de titularidad.

La clasificación operativa es la siguiente:

| Grupo | Tratamiento |
| --- | --- |
| Material original del proyecto, incluidos código, documentación, configuración, pruebas y evidencia generada | Copyright de Alexander Gomez; `LicenseRef-Warehouse-Management-Proprietary` (todos los derechos reservados) |
| Maven Wrapper | Apache-2.0 |
| Liberation Sans y sus avisos | OFL-1.1 |
| Dependencias Maven descargadas en una compilación | No se redistribuyen con este repositorio fuente; cada una conserva la licencia de su editor |

La clasificación de material original depende de la declaración del titular:
proyecto personal realizado en equipo propio y tiempo personal, sin código
copiado conocido fuera de los elementos identificados arriba. Si se descubre
una copia de un tutorial, ejemplo o repositorio externo, no se debe marcar como
material original: hay que registrar su fuente, licencia y avisos antes de
compartir una nueva versión.

## Cómo repetir la comprobación

Ejecutar antes de compartir una revisión relevante:

```bash
git shortlog -sne --all
git log --all --format='%H %an <%ae> %ad %s' --date=short
git ls-files
./mvnw org.codehaus.mojo:license-maven-plugin:add-third-party
./mvnw org.cyclonedx:cyclonedx-maven-plugin:makeAggregateBom
```

Los dos últimos comandos producen, bajo `target/`, un informe de dependencias
de terceros y un SBOM. Revísalos para detectar licencias ausentes, copyleft o
avisos adicionales antes de distribuir binarios. No se deben añadir sus
artefactos generados al repositorio sin una revisión deliberada.

Para revisar material no textual, usar un escáner de licencias y conservar el
resultado fuera del repositorio público junto con el hash del commit revisado.
El archivo `REUSE.toml` proporciona la clasificación legible por herramientas
para cada archivo del árbol actual.

## Evidencia privada recomendada

Conservar fuera de este repositorio público: copias de los resultados de los
comandos anteriores, hashes de los archivos de terceros, capturas o enlaces de
origen, y una nota de revisión humana de los archivos sustantivos. Si se usó
asistencia de IA, la nota privada debe limitarse a qué partes se revisaron y a
la confirmación de que Alexander Gomez verificó y asumió el contenido final;
no se publica como atribución ni sustituye la revisión humana.

## Entrega a una empresa

1. Elegir un commit limpio y anotar su hash.
2. Ejecutar la comprobación anterior y revisar el informe y SBOM.
3. Crear un archivo desde `COMPANY_EVALUATION_LICENSE.template.md`, completar
   destinatario, commit, fecha y plazo, y firmarlo o confirmarlo por escrito.
4. Entregar un archivo fuente del commit, junto con `LICENSE`,
   `THIRD_PARTY_NOTICES.md` y `LICENSES/`.

El repositorio público no convierte el material original en open source. La
plantilla de evaluación es la única vía prevista para dar a una empresa un
permiso de uso acotado.
