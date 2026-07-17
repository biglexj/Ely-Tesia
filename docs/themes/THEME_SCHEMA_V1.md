# Referencia del esquema v1

| Sección | Campo | Descripción |
|---|---|---|
| raíz | `schemaVersion` | Debe ser `1`. |
| raíz | `id` | Identificador comunitario estable. |
| raíz | `name`, `author`, `version`, `license` | Metadatos visibles. |
| raíz | `mode` | `dark`, `light` o `auto`. |
| `material` | `primary`, `secondary`, `tertiary` | Acentos Material 3. |
| `material` | `on*` | Texto/iconos contrastantes para cada fondo. |
| `material` | `background`, `surface`, `outline`, `error` | Superficies y estados. |
| `music` | `leftHand`, `rightHand`, `neutralTrack` | Colores de interpretación. |
| `music` | `whiteKey*`, `blackKey*` | Teclas normales y pulsadas. |
| `music` | `correctNote`, `wrongNote`, `waitingNote` | Evaluación y espera. |
| `music` | `particleLeft`, `particleRight` | Efectos por mano. |
| `effects` | `pressedGlow`, `noteTrail`, `particleIntensity` | Números entre 0 y 1. |
| `effects` | `expressiveMotion` | Activa movimiento expresivo compatible. |
