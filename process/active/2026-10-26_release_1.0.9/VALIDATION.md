# Release 1.0.9 — Validación

- Estado: `IN_PROGRESS`

## Comprobaciones

### Metadatos y versiones

- [x] V01 — Agente — `gradle.properties` (`elytesia.versionName=1.0.9`,
      `elytesia.versionCode=9`), `package.json` (`version`), `packaging/windows/version.iss`
      (`MyAppVersion`) y `UpdateChecker` (desktop y Android) declaran 1.0.9.
      Esperado: un único valor 1.0.9 en todas las fuentes. Resultado: correcto.
- [x] V02 — Agente — Acerca de (`AboutDialog.kt:98`), barra lateral
      (`SidebarNavigation.kt:140`) y diálogo de actualización
      (`UpdateModalDialog.kt:102`) derivan la versión de
      `UpdateChecker.currentVersion`. Esperado: sin literales de versión
      divergentes en la UI. Resultado: correcto; no hay literales fuera de
      `UpdateChecker`.
- [x] V03 — Agente — `aapt dump badging` sobre el APK de publicación.
      Esperado: `com.biglexj.elytesia`, `versionCode=9`, `versionName=1.0.9`,
      `minSdk=24`, `targetSdk=34`. Resultado: correcto.
- [x] V04 — Agente — `FileVersion`/`ProductVersion` del ejecutable de escritorio.
      Esperado: `1.0.9`. Resultado: correcto (`1.0.9` / `1.0.9`).

### Compilación y pruebas

- [x] V05 — Agente — `.\gradlew.bat :composeApp:allTests` (tarea que ejecuta el
      propio pipeline de release, incluye las pruebas de escritorio y de
      Android). Esperado: `BUILD SUCCESSFUL`. Resultado: correcto, sin fallos;
      solo avisos de deprecación de Compose.
- [x] V06 — Agente — Compilación de `packaging/windows/setup.iss` con ISCC.
      Esperado: `Successful compile`. Resultado: correcto.
- [x] V07 — Agente — `scripts/packaging/Build-Installer.ps1 -SkipBuild -EmitWingetManifest`.
      Esperado: instalador generado y manifiesto emitido. Resultado: correcto
      (código 0, `Successful compile`).
- [x] V17 — Agente — El build emite **y valida** el manifiesto WinGet por
      interno (`winget validate` dentro de `Build-Installer.ps1`).
      Esperado: validación correcta sin advertencias. Resultado: correcto
      (`Manifiesto de winget validado correctamente.`).
- [x] V18 — Agente — Reproducibilidad del instalador: dos compilaciones
      consecutivas producen el mismo SHA-256.
      Esperado: hash estable. Resultado: correcto
      (`1a486266e3c5315b829708c16cc4fc3acc9c36145e5336f203163fab9e3462e1` en
      ambas).

### Distribución, seguridad y checksums

- [x] V08 — Agente — `apksigner verify --print-certs` sobre el APK.
      Esperado: firma válida con la identidad permanente. Resultado: correcto
      (esquema v2, `CN=Ely-Tesia, OU=Release, O=biglexj`, RSA 4096).
- [x] V09 — Agente — `sha256sum -c SHA256SUMS.txt` sobre los artefactos de
      publicación. Esperado: `OK` en ambos archivos. Resultado: correcto.
- [x] V10 — Agente — `InstallerSha256` del manifiesto WinGet frente al hash
      real del instalador. Esperado: cadena idéntica. Resultado: correcto.
- [x] V11 — Agente — `winget validate --manifest <1.0.9>`. Esperado: validación
      correcta sin advertencias. Resultado: correcto.
- [x] V12 — Agente — Credenciales de firma fuera del repositorio:
      `keystore.properties` ignorado por Git y no trackeado;
      `signing/elytesia-release.jks` excluido por `*.jks` y no trackeado;
      `keystore.properties.example` con valores de marcador.
      Esperado: ningún secreto versionado. Resultado: correcto.
- [ ] V13 — Agente — `signtool verify /pa` sobre el instalador.
      Esperado: firma Authenticode válida. Resultado: **SIN FIRMAR**. El
      instalador se genera sin firma cuando no se aporta certificado; la firma
      quedó como paso opcional (`-InstallerCertificate`). Pendiente de
      decisión de Biglex.

### Documentación

- [x] V14 — Agente — Barrido de referencias de versión obsoletas en `*.md`.
      Esperado: sin literales de versión vigentes fuera del historial.
      Resultado: correcto; solo quedan las entradas históricas de
      `RELEASE_NOTES.md` y los procesos cerrados de `process/completed/`.
- [x] V15 — Agente — Rutas y enlaces citados por la documentación.
      Esperado: todos los scripts y rutas referenciados existen.
      Resultado: correcto.
- [x] V19 — Agente — Decisión de versión del pipeline con la versión actual sin
      publicar (verificación aislada, sin ejecutar el pipeline).
      Esperado: conservar 1.0.9 en lugar de saltar a 1.0.10.
      Resultado: correcto (`SE CONSERVA 1.0.9`).

### Defectos corregidos durante la preparación

- El manifiesto WinGet generado por `Build-Installer.ps1` omitía los campos
  obligatorios `ManifestType` y `ManifestVersion`: winget rechazaba el
  manifiesto con «Preview manifest does not support multi file manifest
  format». Corregido y validado.
- El mismo generador omitía `InstallerSwitches`, por lo que winget avisaba de
  que el instalador podía no ejecutarse sin supervisión. Añadidos los
  interruptores de Inno Setup (`/VERYSILENT`, `/SILENT`).
- `Get-FileSha256` (fallback .NET) fallaba por ambigüedad de sobrecarga de
  `ComputeHash` y por bloqueos transitorios del antivirus sobre binarios recién
  escritos. Corregido con cast explícito, `FileShare.ReadWrite` y reintentos.
- `SHA256SUMS.txt` se generaba con CRLF + BOM, formato que `sha256sum -c`
  rechaza en Linux y macOS. Ahora se escribe en UTF-8 sin BOM con saltos LF.

### Pendiente de comprobación manual

- [ ] V16 — Tester — Instalar `ElyTesia-Setup-1.0.9.exe` en Windows limpio y
      comprobar la instalación por usuario, la ausencia de elevación y la
      creación de los accesos directos.
- [ ] V17 — Tester — Instalar `ElyTesia-Android-1.0.9.apk` en un dispositivo
      real y comprobar la actualización directa desde la versión anterior.
- [ ] V18 — Tester — Ejecutar la app y comprobar que Acerca de muestra
      `1.0.9` y que el actualizador no ofrece una versión superior inexistente.
- [ ] V19 — Tester — `winget install --id biglexj.ElyTesia` tras publicar el
      release: la instalación silenciosa usa `/VERYSILENT /SUPPRESSMSGBOXES
      /NORESTART`.

### Publicación

- [x] V20 — Agente — Release publicada en GitHub: tag `v1.0.9`, commit
      `abc0a11`, release "Ely-Tesia 1.0.9" con tres assets
      (`ElyTesia-Setup-1.0.9.exe`, `ElyTesia-Android-1.0.9.apk`,
      `SHA256SUMS.txt`). Esperado: release pública y sin borrador.
      Resultado: correcto
      (https://github.com/biglexj/Ely-Tesia/releases/tag/v1.0.9).
- [x] V21 — Agente — Descarga de los artefactos publicados y contraste de sus
      hashes contra el `SHA256SUMS.txt` publicado. Esperado: coincidencia
      exacta. Resultado: correcto.
      - `ElyTesia-Setup-1.0.9.exe` →
        `1a486266e3c5315b829708c16cc4fc3acc9c36145e5336f203163fab9e3462e1`
      - `ElyTesia-Android-1.0.9.apk` →
        `b7784f84d4c525c0a9a46d63a89496787325f980165a50f5aca7c2fd4c48c306`
- [x] V22 — Agente — `InstallerUrl` del manifiesto WinGet contra el artefacto
      publicado. Esperado: descarga accesible. Resultado: correcto
      (HTTP 200).
- [x] V23 — Agente — `InstallerSha256` del manifiesto WinGet frente al hash del
      instalador publicado. Esperado: cadena idéntica. Resultado: correcto.

## Registro de fallos

- Fallo técnico → crear o reabrir una tarea.
- Plan incorrecto → regresar a `PLAN.md`.
- Entorno bloqueado → registrar el bloqueo sin marcar la validación.

Al aprobar una comprobación, cambia `[ ]` por `[x]`. Si falla, mantenla pendiente y añade una sola línea con el motivo y la tarea relacionada.
