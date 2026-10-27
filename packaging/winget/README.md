# Publicación en WinGet

## Identificador

El identificador reservado del producto es **`biglexj.ElyTesia`**. No se debe
reutilizar el identificador de otro paquete del ecosistema (por ejemplo el de
WinTTS). El mismo certificado de publicación puede firmar más de una
aplicación si su sujeto coincide exactamente con `Publisher`; la identidad de
Microsoft Store debe copiarse desde Partner Center.

## Manifiestos existentes

| Versión | Ruta | Tipo de instalador | Estado |
| --- | --- | --- | --- |
| 1.0.1 | `manifests/b/biglexj/ElyTesia/1.0.1/` | `wix` (MSI) | Histórico, publicado en upstream |
| 1.0.2 | `manifests/b/biglexj/ElyTesia/1.0.2/` | `wix` (MSI) | Histórico, nunca llegó a publicarse en upstream |
| 1.0.9 | `manifests/b/biglexj/ElyTesia/1.0.9/` | `exe` (Inno Setup) | Release publicada; PR a upstream pendiente |

En `microsoft/winget-pkgs` la versión publicada más reciente es la **1.0.8**
(tipo `wix`, MSI). Su manifiesto no está en este repositorio porque se
generó fuera del flujo de build.

Las versiones 1.0.1, 1.0.2 y 1.0.8 apuntan al `.msi` de jpackage + WiX
publicado en GitHub Releases. La 1.0.9 cambia al instalador ejecutable `.exe`.

## Cambio de tipo a `exe` (plantilla Biglex Modern 2.0)

Desde la versión instalada con **Inno Setup** el instalador de Windows es un
`.exe` autónomo (`ElyTesia-Setup-<versión>.exe`), no un `.msi`. El manifiesto
de winget debe cambiar de `InstallerType: wix` a `InstallerType: exe`.

El manifiesto nuevo **se genera, no se escribe a mano**:

```powershell
.\scripts\packaging\Build-Installer.ps1 -EmitWingetManifest
```

El script calcula el `InstallerSha256` real del `.exe` compilado y escribe:

```
packaging/winget/manifests/b/biglexj/ElyTesia/<versión>/
  biglexj.ElyTesia.yaml
  biglexj.ElyTesia.installer.yaml   (InstallerType: exe)
```

El manifiesto de **locale** (`biglexj.ElyTesia.locale.es-PE.yaml`) **no se
genera**: lleva descripción y notas de release en prosa. El script avisa por
consola cuando falta para la versión en curso. Para una versión nueva, copia el
locale de la versión anterior y actualiza `PackageVersion`, `ReleaseNotes`,
`ReleaseNotesUrl` y `ReleaseDate`.

Sin los tres archivos (versión, instalador y locale) el manifiesto está
incompleto y WinGet lo rechaza: los campos `ManifestType` y `ManifestVersion`
son obligatorios en cada archivo.

**No se publica nada automáticamente**: el manifiesto queda en el repositorio
listo para el PR, pero la `InstallerUrl` solo es válida cuando exista el
release correspondiente en GitHub Releases.

Desde la 1.0.9 el manifiesto se regenera **dentro del propio build de release**
(`.\build-release.ps1 local -BuildInno`), que invoca `Build-Installer.ps1` con
`-EmitWingetManifest`. El `InstallerSha256` se calcula siempre sobre el `.exe`
recién compilado, de modo que el hash no puede quedar desfasado respecto al
artefacto publicado.

### Procedimiento de publicación

1. Publicar `ElyTesia-Setup-<versión>.exe` en GitHub Releases.
2. Confirmar que el `InstallerSha256` del manifiesto coincide con el archivo
   publicado (lo calcula el propio script de build).
3. Comprobar con `winget validate --manifest <directorio>`.
4. Copiar el árbol desde `manifests/` al fork de `microsoft/winget-pkgs`.
5. Abrir el PR para el identificador `biglexj.ElyTesia`.

La coexistencia con el MSI anterior (desinstalar el MSI antes de instalar el
EXE, o mantener ambos) está documentada en
[`../windows/MIGRATION.md`](../windows/MIGRATION.md).
