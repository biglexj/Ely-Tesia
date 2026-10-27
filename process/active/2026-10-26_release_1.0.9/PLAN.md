# Release 1.0.9 — Plan

- Estado: `IN_PROGRESS`
- Fecha: `2026-10-26`
- Proyecto: `Ely-Tesia`

## Objetivo

Dejar la versión 1.0.9 consolidada y coherente en todas sus referencias
(metadatos, documentación, distribución y checksums) para que Biglex pueda
revisarla y decidir la publicación.

## Alcance

- Incluye: consolidación de la versión 1.0.9 en metadatos, manifiestos y
  textos; corrección de las inconsistencias de empaquetado que afectan a esta
  entrega; sincronización de `SHA256SUMS.txt` y del manifiesto WinGet con los
  artefactos definitivos; regeneration del manifiesto WinGet dentro del propio
  build.
- No incluye: implementación de funcionalidades nuevas, publicación de
  releases, tags o commits, y avance del roadmap (gestión fina de pistas,
  grabación de interpretaciones, Linux, macOS, iOS).

## Enfoque

1. Confirmar que 1.0.9 nunca se publicó (sin tag, sin GitHub Release) y por
   tanto conservar esa versión sin incrementar el patch.
2. Compilar el instalador oficial con Inno Setup y regenerar el manifiesto
   WinGet con el `InstallerSha256` real del artefacto.
3. Generar los checksums únicamente sobre los dos archivos previstos para
   publicación.
4. Alinear README, Release Notes, Release Message, ROADMAP y la documentación de
   empaquetado con el estado real de la entrega.

## Criterios de finalización

- [ ] La versión 1.0.9 es coherente en `gradle.properties`, `package.json`,
      `version.iss`, `UpdateChecker` (desktop y Android), About, barra lateral,
      APK, EXE y manifiesto WinGet.
- [ ] `release/SHA256SUMS.txt` y `InstallerSha256` coinciden con el hash real
      de los artefactos publicados.
- [ ] `winget validate --manifest <1.0.9>` es correcto.
- [ ] Las pruebas automatizadas del proyecto pasan.
- [ ] La documentación no cita artefactos ni formatos que la entrega no
      produce.

## Autorización

- [x] Plan aprobado para ejecución.
