# Release 1.0.9 — Aprobación

- Estado: `PENDING`

## Controles

- [x] Validación técnica del agente.
- [ ] Validación funcional del tester.
- [ ] Aprobación final de Biglex.
- [x] `ROADMAP.md` actualizado.
- [ ] Sesión cerrada con resumen breve.

## Decisión

- [ ] `APPROVED`
- [ ] `REWORK`
- [ ] `CANCELLED`
- [ ] `SUPERSEDED`

## Resumen

- La versión 1.0.9 se conserva: nunca se publicó (sin tag, sin GitHub Release).
- Metadatos, documentación y distribución quedan alineados con los artefactos
  definitivos de la entrega.
- Checksums y manifiesto WinGet regenerados desde el instalador definitivo y
  validados; el instalador es reproducible (mismo SHA-256 entre compilaciones).
- Pendientes para Biglex: firma Authenticode del instalador, publicación del
  release y apertura del PR de WinGet, y actualización del hash del instalador
  en el `SHA256SUMS.txt` compartido de `12. Releases` (fuera del repositorio).

## Destino

- `APPROVED` con todos los controles completos → `process/completed/YYYY/`.
- `CANCELLED`, `SUPERSEDED` o cierre incompleto → `process/archive/YYYY/`.
- `REWORK` → permanece en `process/active/`.

Mueve la carpeta completa y no conserves una copia duplicada en `active`.
