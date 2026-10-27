# Migración del instalador de Ely-Tesia: de MSI/MSIX (WiX · jpackage) a EXE (Inno Setup)

Documento de referencia del empaquetado de Windows de Ely-Tesia tras adoptar la
plantilla compartida **Biglex Modern 2.0**. Describe qué convive, qué rutas deja
cada instalador y cómo desinstalar el paquete anterior.

> Este documento describe el comportamiento verificado en esta máquina el
> 21 de octubre de 2026. No sustituye a la documentación compartida de la
> plantilla (`Scripts\tools\inno\README.md`).

## 1. Qué hay hoy en el repositorio

| Elemento | Ruta | Estado |
| --- | --- | --- |
| Instalador nuevo (Inno Setup) | `packaging\windows\setup.iss` + `BiglexModernUI.iss` + `identity.iss` + `version.iss` | **Activo** |
| Script de build del instalador | `scripts\packaging\Build-Installer.ps1` | **Activo** |
| Plantilla MSIX | `packaging\windows\AppxManifest.xml.template` | Se conserva |
| Script de build MSIX | `scripts\build-msix.ps1` | Se conserva |
| Manifiestos winget | `packaging\winget\manifests\b\biglexj\ElyTesia\` | 1.0.1, 1.0.2 y 1.0.9 (en este repositorio); en upstream `microsoft/winget-pkgs` constan 1.0.1 y 1.0.8 (tipo `wix`) |
| Paquete de firma Android | `keystore.properties`, `scripts\setup-android-signing.ps1` | Sin cambios |

Nada de esto afecta a Android, macOS ni Linux: el flujo `scripts\build-release.ps1`
solo cambia en su tramo de Windows.

## 2. Identidades: tres sistemas distintos, tres espacios distintos

| | Antes (WiX/MSI y MSIX) | Ahora (Inno Setup) |
| --- | --- | --- |
| Motor | Windows Installer (`msiexec`) / Deployment (MSIX) | Inno Setup 6.7.3 |
| Identidad | MSI `UpgradeCode {28da0bdd-0ec9-3096-9fe8-2c59b53ec0ab}` · MSIX `Identity Name="biglexj.ElyTesia"` | `AppId Biglex.ElyTesia.Desktop` |
| Registro de la identidad | `HKCU\Software\Microsoft\Windows\CurrentVersion\Uninstall\{ProductCode}` · paquete MSIX en Windows | `HKCU\Software\Microsoft\Windows\CurrentVersion\Uninstall\{Biglex.ElyTesia.Desktop}_is1` |
| Carpeta de instalación | `%LOCALAPPDATA%\ElyTesia` | `%LOCALAPPDATA%\Programs\Ely-Tesia` |
| Privilegios | Por usuario | Por usuario (`PrivilegesRequired=lowest`), sin UAC |
| Desinstalador | `msiexec /x {ProductCode}` | `{app}\unins000.exe` |

**El `AppId` de Inno no es el `UpgradeCode` del MSI ni el `Identity Name` del
MSIX, y no se ha reutilizado ningún GUID de ninguno de los dos.** Windows
Installer y Windows no reconocen una actualización por compartir un
identificador con Inno Setup: Inno Setup ni siquiera consulta el registro de
Windows Installer. Por eso **los dos instaladores pueden coexistir en el mismo
equipo**, pero también por eso **el EXE no actualiza el MSI**: hay que
desinstalar el MSI antes (ver §4).

A partir de la primera versión instalada con el EXE, las actualizaciones sí son
continuas: `Biglex.ElyTesia.Desktop` es estable, así que una versión nueva
actualiza la anterior en la misma carpeta y no crea una instalación paralela.

## 3. Qué rutas deja cada uno

| | MSI (jpackage + WiX) | MSIX | Inno Setup (EXE) |
| --- | --- | --- | --- |
| Programa | `%LOCALAPPDATA%\ElyTesia\` | Carpeta del paquete en `%LOCALAPPDATA%\Packages\...` | `%LOCALAPPDATA%\Programs\Ely-Tesia\` |
| Acceso directo | Menú Inicio y escritorio | Menú Inicio (aprovisionado por Windows) | `%APPDATA%\Microsoft\Windows\Start Menu\Programs\Ely-Tesia` y escritorio |
| Entrada en «Agregar o quitar programas» | Sí (Windows Installer) | Sí (Ajustes de Windows) | Sí (Inno Setup) |
| **Datos del usuario** | `%APPDATA%\Ely-Tesia\state.txt` (legado: `%LOCALAPPDATA%\ElyTesia\state.txt`) | — | **No toca los datos** |

El desinstalador de Inno Setup borra lo que instaló (la carpeta de programa y el
`unins000.exe`) y **no borra `%APPDATA%\Ely-Tesia`**. Tampoco borra
`%LOCALAPPDATA%\ElyTesia`, que pertenece a la instalación MSI.

> Comportamiento verificado: si Ely-Tesia está **ejecutándose** cuando se lanza
> una instalación silenciosa, el gestor de reinicio de Windows no consigue
> cerrarla y la instalación se aborta con «el programa de instalación no pudo
> cerrar de forma automática todas las aplicaciones» (código de salida 5). Es el
> comportamiento esperado de `CloseApplications=yes` en modo silencioso: hay que
> cerrar la aplicación antes de instalar. La instalación interactiva muestra el
> aviso y deja elegir.

## 4. Procedimiento de desinstalación del paquete anterior

### 4.1 Comprobar qué hay instalado

```powershell
Get-ItemProperty 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*',
                 'HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*' `
  -ErrorAction SilentlyContinue |
  Where-Object { $_.DisplayName -like '*ElyTesia*' -or $_.DisplayName -like '*Ely-Tesia*' } |
  Select-Object DisplayName, DisplayVersion, InstallLocation, UninstallString
```

### 4.2 Desinstalar el MSI (instalación por usuario, no necesita elevación)

```powershell
# Por código de producto (el de la versión instalada):
msiexec.exe /x {9A15221B-F361-3992-8BD0-BD50226C55E1}

# O, si se conserva el archivo .msi:
msiexec.exe /x ".\ElyTesia-1.0.9.msi" /qb
```

> El código de producto cambia en cada versión. Para descubrir el de la versión
> instalada, leer `UninstallString` en el registro (comando del apartado 4.1).

También sirve la vía Gráficos: «Configuración» → «Aplicaciones» →
«Aplicaciones instaladas» → **ElyTesia** → «Desinstalar».

### 4.3 Desinstalar el MSIX (si se instaló desde el paquete)

```powershell
Get-AppxPackage -Name 'biglexj.ElyTesia' | Remove-AppxPackage
```

### 4.4 Comprobar que quedó limpio

```powershell
Test-Path "$env:LOCALAPPDATA\ElyTesia"        # debe ser False tras desinstalar el MSI
Test-Path "$env:LOCALAPPDATA\Programs\Ely-Tesia" # False tras desinstalar el EXE de Inno
```

Si `%LOCALAPPDATA%\ElyTesia` persiste, puede borrarse a mano: ya no la usa
ningún instalador. **No borrar `%APPDATA%\Ely-Tesia`**: son los datos del
usuario (biblioteca de canciones, preferencias).

### 4.5 Instalar el EXE de Inno Setup

```powershell
.\ElyTesia-Setup-1.0.9.exe
```

Instalación por usuario, sin elevación, en `%LOCALAPPDATA%\Programs\Ely-Tesia`.
El desinstalador queda dentro de la propia carpeta de instalación y admite
modo silencioso:

```powershell
& "$env:LOCALAPPDATA\Programs\Ely-Tesia\unins000.exe" /VERYSILENT
```

## 5. Convivencia sin desinstalar (alternativa)

Si se instalan los dos, quedan dos copias de la aplicación, dos accesos
directos y dos entradas en «Agregar o quitar programas». La copia antigua sigue
en el disco y puede seguir ejecutándose; sus datos (`%APPDATA%\Ely-Tesia`) los
comparte la aplicación, porque son los mismos.

Es la opción más conservadora durante una transición: primero se publica el EXE
y solo se retira el MSI cuando el EXE esté disponible para todo el mundo.

## 6. Manifiestos de winget

**Sí existen manifiestos de winget** en
`packaging\winget\manifests\b\biglexj\ElyTesia\`:

- `1.0.1/` y `1.0.2/` → `InstallerType: wix`, con `ProductCode` propio y la URL
  del `.msi` publicado en GitHub Releases.
- `1.0.9/` → `InstallerType: exe`, con el `InstallerSha256` real del instalador
  de Inno Setup. **Release publicada en GitHub; PR a `microsoft/winget-pkgs`
  pendiente**.

El manifiesto de tipo `exe` lo genera el script de build con
`scripts\packaging\Build-Installer.ps1 -EmitWingetManifest`, que escribe

```
packaging\winget\manifests\b\biglexj\ElyTesia\<versión>\
  biglexj.ElyTesia.yaml
  biglexj.ElyTesia.installer.yaml   (InstallerType: exe, InstallerSha256 real)
```

Desde la 1.0.9 ese paso forma parte del propio build de release
(`.\build-release.ps1 local -BuildInno`): el `InstallerSha256` se calcula siempre
sobre el `.exe` recién compilado, así que no puede quedar desfasado.

El manifiesto generado **no se ha enviado a `microsoft/winget-pkgs`**: la URL
`InstallerUrl` solo es válida cuando exista el release correspondiente de GitHub,
es decir, después de publicar el `.exe`. Antes de abrir el PR hay que:

1. Publicar `ElyTesia-Setup-<versión>.exe` en GitHub Releases.
2. Comprobar que el `InstallerSha256` del manifiesto coincide con el archivo
   publicado (lo calcula el propio script de build).
3. `winget validate --manifest <directorio>`.
4. Copiar el árbol al fork de `microsoft/winget-pkgs` y abrir el PR para el
   identificador `biglexj.ElyTesia`.

El `PackageIdentifier` `biglexj.ElyTesia` se conserva: es el identificador
reservado del producto y no debe cambiar aunque cambie el formato del
instalador.

## 7. Lo que el EXE no reproduce del MSI

- **Firma digital**: el EXE de Inno Setup no se firma por sí mismo. La firma
  Authenticode es **opcional** y la aplica el flujo de release con
  `signtool.exe` sobre el artefacto final, indicándole un certificado:

  ```powershell
  .\build-release.ps1 local -BuildInno -InstallerCertificate "C:\certificados\ElyTesia.pfx"
  ```

  La contraseña puede pasarse con `-InstallerCertificatePassword` o, mejor, por
  la variable de entorno `ELYTESIA_INSTALLER_CERT_PASSWORD` para no dejarla
  expuesta en la línea de órdenes. **Sin certificado, el instalador se genera sin
  firmar** y Windows SmartScreen lo mostrará como de editor desconocido. El
  `unins000.exe` que queda dentro de la instalación va sin firmar en cualquier
  caso.
- **Actualización automática en segundo plano**: el MSI gestionaba la
  actualización con `RemoveExistingProducts`. Con Inno Setup hay que ejecutar
  el instalador nuevo; la app sigue sin sobrescribir una instalación en uso
  porque `CloseApplications=yes` cierra Ely-Tesia si sigue abierto.
- **Personalización de «Agregar o quitar programas»**: Inno Setup registra la
  instalación, pero la entrada la crea Inno Setup y no WiX.
