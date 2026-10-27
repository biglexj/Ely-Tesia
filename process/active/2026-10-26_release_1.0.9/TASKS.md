# Release 1.0.9 — Tareas

- Estado: `IN_PROGRESS`

## Ejecución

- [x] T01 — Confirmar el estado de publicación de 1.0.9 (tag remoto, GitHub
      Release, `InstallerUrl` del manifiesto) para decidir si se conserva la
      versión o se incrementa el patch.
- [x] T02 — Auditoría previa de versiones, metadatos, documentación y
      distribución; verificación de la firma del APK y de los hashes reales.
- [x] T03 — Exponer `-BuildInno` en el wrapper raíz y en `package.json` para
      que el pipeline documentado produzca el instalador oficial.
- [x] T04 — Regenerar el manifiesto WinGet dentro del propio build de release
      (`-EmitWingetManifest`) para que el `InstallerSha256` no vuelva a
      desfasarse.
- [x] T05 — Añadir firma Authenticode opcional del instalador
      (`-InstallerCertificate`), con contraseña por variable de entorno, y
      corregir la documentación que afirmaba que la firma se aplicaba sola.
- [x] T06 — Conservar la versión 1.0.9 cuando aún no se ha publicado, en lugar
      de incrementarla automáticamente.
- [x] T07 — Compilar el instalador definitivo con Inno Setup y regenerar el
      manifiesto WinGet con el hash real.
- [x] T08 — Generar `SHA256SUMS.txt` únicamente sobre los artefactos previstos
      para publicación.
- [x] T09 — Alinear README (título, badges, funciones, descargas, requisitos,
      comandos de build), Release Notes, ROADMAP, `packaging/winget/README.md` y
      `packaging/windows/MIGRATION.md` con el estado real de la entrega.
- [x] T10 — Validar el manifiesto WinGet, la firma del APK, los metadatos del
      APK y del EXE, y las pruebas automatizadas.
- [x] T11 — Corregir el generador del manifiesto WinGet: añadir `ManifestType`
      y `ManifestVersion` (obligatorios; sin ellos winget rechazaba el
      manifiesto) e `InstallerSwitches` (instalación silenciosa de Inno Setup).
- [x] T12 — Corregir `Get-FileSha256`: ambigüedad de sobrecarga en el
      fallback .NET y falta de tolerancia a los bloqueos transitorios del
      antivirus sobre binarios recién escritos.
- [x] T13 — Generar `SHA256SUMS.txt` en UTF-8 sin BOM y con saltos LF, para
      que `sha256sum -c` lo verifique también en Linux y macOS.
- [x] T14 — Añadir al pipeline la validación interna del manifiesto WinGet
      (`winget validate`) y el aviso por consola cuando falta el manifiesto de
      locale de la versión en curso.

Las pruebas no se documentan aquí. Deben registrarse en `VALIDATION.md`.
