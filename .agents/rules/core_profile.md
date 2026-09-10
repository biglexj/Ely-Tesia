---
trigger: always_on
---

# Perfil de Documentación Core — Ely-Tesia

- Última revisión: 2026-08-09
- Tipo principal: `multiplatform`, `desktop`, `mobile`
- Plataformas: `windows`, `android`
- Stack: `kotlin-multiplatform`
- Funciones activas: `design-system`, `auto-updater`, `feedback-center`, `installer`

## Regla de selección

Antes de aplicar la Documentación Core, completar este perfil con el alcance real del proyecto y consultar únicamente:

1. Las reglas globales pertinentes.
2. El tipo principal en `Docs/types`.
3. Cada plataforma distribuida en `Docs/platforms`.
4. El stack utilizado en `Docs/stacks`.
5. Las funciones realmente adoptadas en `Docs/features`.

No aplicar una capacidad por semejanza. Instancia única, bandeja, autoactualización, instalador, IA y otras funciones deben figurar expresamente como activas.

## Documentos Core seleccionados

- `Docs/global/` (Agents, architecture, design, documentation, quality, releases, security)
- `Docs/types/multiplatform/`
- `Docs/types/desktop/`
- `Docs/types/mobile/`
- `Docs/platforms/windows/`
- `Docs/platforms/android/`
- `Docs/stacks/kotlin-multiplatform/`
- `Docs/features/design-system/`
- `Docs/features/auto-updater/`
- `Docs/features/feedback-center/`
- `Docs/features/installer/`

## Excepciones locales

- **Multi-Instancia Permitida (Desktop JVM)**: No aplica bloqueo de instancia única (`single-instance lock`). Se permite la ejecución simultánea de múltiples instancias independientes para comparar o reproducir distintos archivos MIDI.
