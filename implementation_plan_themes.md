# Plan de implementación — Temas comunitarios y Material 3

## Objetivo

Implementar en la versión activa Ely-Tesia 1.0.5 un sistema de temas versionado, importable y
exportable, capaz de personalizar la interfaz, el piano, las manos, las notas,
los estados de pulsación y los efectos visuales. Los mismos datos alimentarán
el diseño compartido de Desktop y Android; Android añadirá Dynamic Color y una
capa Material 3 Expressive cuando la plataforma lo permita.

## Formato de intercambio

Se usará JSON UTF-8 con extensión recomendada `.elytheme.json`. JSON ofrece
portabilidad, validación mediante JSON Schema, revisión sencilla en Git y
compatibilidad con herramientas de la comunidad.

Cada archivo tendrá:

- `schemaVersion`: versión entera del contrato, comenzando en `1`.
- `id`: identificador estable en minúsculas, por ejemplo
  `com.usuario.aurora-midnight`.
- `name`, `author`, `description`, `version` y `license`.
- `mode`: `dark`, `light` o `auto`.
- `material`: roles semánticos Material 3 (`primary`, `onPrimary`, `surface`,
  `onSurface`, `error`, `outline`, contenedores y equivalentes).
- `music`: colores propios del dominio musical.
- `effects`: parámetros visuales limitados y seguros.

Los colores se escribirán como `#RRGGBB` o `#RRGGBBAA`. No se aceptarán código,
scripts, fuentes remotas, imágenes remotas ni rutas locales dentro de un tema.

Ejemplo resumido:

```json
{
  "$schema": "https://elytesia.app/schemas/theme-v1.schema.json",
  "schemaVersion": 1,
  "id": "com.biglexj.aurora",
  "name": "Aurora",
  "author": "biglexj",
  "version": "1.0.0",
  "license": "CC0-1.0",
  "mode": "dark",
  "material": {
    "primary": "#5B4CFF",
    "onPrimary": "#FFFFFF",
    "surface": "#1E293B",
    "onSurface": "#F8FAFC",
    "error": "#FFD166"
  },
  "music": {
    "leftHand": "#00C7B1",
    "rightHand": "#5B4CFF",
    "blackKey": "#0F172A",
    "blackKeyPressed": "#FB7793",
    "whiteKey": "#E2E8F0",
    "correctNote": "#00C7B1",
    "wrongNote": "#FFD166",
    "waitingNote": "#FB7793",
    "particleLeft": "#00C7B1CC",
    "particleRight": "#5B4CFFCC"
  },
  "effects": {
    "pressedGlow": 0.75,
    "noteTrail": 0.55,
    "particleIntensity": 0.8,
    "expressiveMotion": true
  }
}
```

## Arquitectura

### Núcleo compartido

Crear bajo `composeApp/src/commonMain/.../core/theme/`:

- `ElyTheme.kt`: modelos inmutables de metadatos, roles Material, colores
  musicales y efectos.
- `ThemeJsonCodec.kt`: serialización/deserialización estricta.
- `ThemeValidator.kt`: identificadores, formato de color, rangos, contraste,
  tamaño máximo y campos obligatorios.
- `ThemeRepository.kt`: temas integrados, temas importados y tema seleccionado.
- `ThemeDefaults.kt`: Aurora, Clásico y Alto Contraste.
- `ThemeLocals.kt`: `CompositionLocal` para acceder a tokens musicales y
  efectos junto con `MaterialTheme.colorScheme`.
- `HandColorResolver.kt`: resolución de mano por pista y respaldo por tono.

Se añadirá `kotlinx-serialization-json` al source set común para evitar un parser
manual frágil. El codec ignorará campos futuros desconocidos, pero rechazará
versiones de esquema superiores que no pueda interpretar.

### Persistencia e intercambio

- Guardar el `id` del tema activo en `SavedAppState` con migración compatible
  desde `ELYTESIA_STATE_1`.
- Guardar temas importados en almacenamiento privado de la aplicación.
- Añadir contratos multiplataforma para importar y exportar
  `.elytheme.json` mediante los selectores nativos ya usados para MIDI.
- Permitir vista previa antes de instalar, restaurar Aurora y eliminar temas
  comunitarios sin afectar los temas integrados.
- Exportar exactamente el JSON validado para compartirlo por cualquier canal.

### Aplicación de colores

Eliminar colores musicales hardcodeados de:

- `Theme.kt` y `ElyTesiaAppContent.kt`.
- `PianoKeyboard.kt`: blancas, negras/bemoles, pulsación correcta, incorrecta,
  sostenida y por mano.
- `PianoRollCanvas.kt`: barras, bordes, brillo, estela y partículas.
- `PlaybackProgressBar.kt`: gradiente, pista y thumb.

La UI general usará roles de `MaterialTheme.colorScheme`; los elementos
musicales usarán `ElyTheme.music` y `ElyTheme.effects`.

Como MIDI no normaliza la mano, el resolvedor seguirá este orden:

1. Asignación explícita de pista/mano si existe.
2. Metadatos o nombre de pista reconocible.
3. Respaldo configurable: tonos menores a MIDI 60 = izquierda; MIDI 60 o más =
   derecha.

## Gestor de temas

Crear una pantalla/panel accesible desde Configuración con:

- Galería de temas integrados e importados.
- Vista previa de colores Material, teclado y notas de ambas manos.
- Aplicar, importar, exportar, duplicar y eliminar.
- Mensajes precisos de validación indicando el campo incorrecto.
- Indicador de contraste: advertir debajo de 4.5:1 para texto normal y rechazar
  combinaciones críticas ilegibles en el tema Alto Contraste.
- Opción Android `Usar colores dinámicos del sistema` independiente del tema
  musical: el wallpaper puede controlar la UI Material sin reemplazar los
  colores de manos y notas.

No se incluye todavía un editor completo con selectores de color; en 1.0.5 se
podrá duplicar/exportar un tema y editar su JSON. El editor visual puede llegar
en 1.0.7 después de estabilizar el esquema comunitario.

## Android Material 3 y Material Expressive

- Mantener `MaterialTheme` como fuente de verdad de colores, tipografía y formas.
- Añadir un proveedor `androidMain` para `dynamicDarkColorScheme` y
  `dynamicLightColorScheme` en Android 12 o superior, con fallback al JSON.
- Usar superficies y colores `on-*` emparejados para conservar contraste.
- Añadir formas expresivas, touch targets mínimos de 48 dp, ripple del sistema,
  transiciones de contenedor y movimiento de énfasis en paneles y selector de
  temas.
- Adoptar inicialmente APIs Material 3 estables. Las APIs Expressive que sigan
  siendo experimentales quedarán aisladas en `androidMain` y detrás de una
  opción, para no comprometer Desktop ni el formato comunitario.
- Revisar la compatibilidad entre Compose Multiplatform y Material 3 Android
  antes de actualizar dependencias; no se mezclarán versiones binarias
  incompatibles solo para obtener un componente Expressive.

La documentación oficial confirma que Material 3 Compose cubre color dinámico,
formas, tipografía y Material 3 Expressive; Dynamic Color requiere Android 12 o
superior.

## Documentación comunitaria

Crear:

```text
docs/
└── themes/
    ├── README.md                  # introducción y flujo de compartir
    ├── CREAR_UN_TEMA.md           # tutorial paso a paso
    ├── REGLAS_DE_TEMAS.md         # seguridad, accesibilidad y licencias
    ├── THEME_SCHEMA_V1.md         # referencia de todos los campos
    ├── theme-v1.schema.json       # JSON Schema validable
    └── examples/
        ├── aurora.elytheme.json
        ├── classic.elytheme.json
        └── high-contrast.elytheme.json
```

Reglas comunitarias mínimas:

- El autor debe escoger una licencia explícita; recomendadas CC0-1.0,
  CC-BY-4.0 o MIT para datos de tema.
- No se permiten nombres o descripciones ofensivas ni suplantación de autoría.
- No se admiten recursos ejecutables o remotos.
- Máximo 64 KiB por archivo y límites numéricos definidos por el esquema.
- Todo tema debe tener combinaciones `on-*` legibles y una vista previa antes de
  instalarse.
- Ely-Tesia podrá mostrar advertencias o rechazar archivos corruptos, esquemas
  incompatibles y valores fuera de rango.

## Pruebas y aceptación

- Round-trip JSON sin pérdida de datos.
- Rechazo de colores inválidos, tamaños excesivos, `schemaVersion` desconocida y
  efectos fuera de rango.
- Migración del estado actual conservando biblioteca y preferencias.
- Cambio de tema en caliente sin reiniciar reproducción ni perder MIDI activo.
- Colores de ambas manos, teclas negras/bemoles, aciertos, errores, estelas y
  partículas visibles en teclado y piano roll.
- Importación/exportación en Android y Desktop.
- Dynamic Color probado con fallback en Android menor a 12.
- Contraste y touch targets revisados en los tres temas integrados.
- `allTests`, compilaciones Android/Windows y APK firmado continúan correctos.

## Entrega

- Crear un `task_themes.md` al aprobar este plan.
- Actualizar `ROADMAP.md`, `README.md` y las notas activas de 1.0.5 sin crear otra versión.
- Crear `walkthrough_themes.md` con capturas o descripción de las pruebas.
- No publicar ni etiquetar 1.0.5 hasta validar importación de un tema comunitario
  en Android y Desktop.
