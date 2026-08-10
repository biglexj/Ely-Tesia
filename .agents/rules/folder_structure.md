---
trigger: always_on
---

# 📁 Regla de Estructura de Carpetas — Ely-Tesia

> [!CAUTION]
> Esta regla es **CRÍTICA y no negociable**. Todo nuevo archivo, carpeta o módulo creado por el agente DEBE seguir esta convención. Violar esta estructura es inaceptable y debe ser corregido inmediatamente.

## Estructura Raíz del Proyecto (Kotlin Multiplatform / Compose)

```
Ely-Tesia/                          # Raíz del repositorio
├── .agents/rules/                  # Reglas del agente (base.md, core_profile.md, folder_structure.md, ...)
├── composeApp/                     # Módulo principal KMP / Compose
│   └── src/
│       ├── commonMain/kotlin/
│       │   ├── features/           # Lógica de negocio organizada por dominios (PascalCase o camelCase)
│       │   │   ├── difficulty/     # Clasificación y análisis de dificultad
│       │   │   ├── handmode/       # Práctica por mano independiente
│       │   │   ├── library/        # Gestión y persistencia de canciones MIDI
│       │   │   ├── player/         # Motor y lógica determinista de reproducción
│       │   │   ├── shell/          # Contenedores principales y navegación adaptativa
│       │   │   └── theme/          # Sistema de diseño, tokens dinámicos y paletas
│       │   └── shared/             # Componentes transversales usados en 2+ features
│       │       └── components/     # Átomos UI compartidos (ElyBadge, ElyButton, ElyCard, ElyToast...)
│       ├── desktopMain/kotlin/     # Implementaciones de plataforma Windows Desktop JVM
│       └── androidMain/kotlin/     # Implementaciones de plataforma Android
├── docs/                           # Documentación técnica y guías del proyecto
│   └── es/guides/                  # Guías de referencia en español
├── process/                        # Planificación, tareas, validación y aprobación
│   ├── active/                     # Procesos actualmente en ejecución
│   ├── completed/                  # Procesos validados y aprobados, por año
│   ├── archive/                    # Procesos cancelados o cerrados incompletos
│   └── templates/                  # Moldes locales para crear procesos (PLAN, TASKS, VALIDATION, APPROVAL)
├── scratch/                        # Scripts utilitarios de mantenimiento (solo raíz)
├── test/                           # Scripts de prueba temporales (ignorado en .gitignore)
├── agent.md                        # Instrucciones principales del agente (raíz)
├── ROADMAP.md                      # Pendientes, prioridades e historial del producto
├── RELEASE_NOTES.md                # Historial de cambios por versión
├── RELEASE_MESSAGE.md              # Mensaje de anuncio del último lanzamiento
├── NOTICE.md                       # Aviso de atribución de licencia MIT
└── README.md                       # Documentación pública del proyecto
```

## Reglas de Nomenclatura [CRÍTICO]

| Elemento | Convención | Ejemplo |
|---|---|---|
| Carpetas de feature | `PascalCase` o `camelCase` según stack | `player/`, `LibraryFeature/` |
| Archivos de componente | `PascalCase` + sufijo de tipo | `MusicScreen.kt`, `SongCard.kt` |
| Archivos de ruta/página | `kebab-case` o `PascalCase` según stack | `PlayerScreen.kt`, `index.kt` |
| Modelos / Data classes | `PascalCase` | `Song`, `MidiTrack` |
| Constantes | `SCREAMING_SNAKE_CASE` | `MAX_RETRY_COUNT` |
| Variables / funciones | `camelCase` | `loadSongs()`, `isPlaying` |

## Reglas Estructurales Obligatorias

### ✅ PERMITIDO
- Crear sub-componentes dentro de la carpeta de su feature.
- Crear componentes en `shared/` solo si son usados por **2 o más** features distintas.
- Usar `test/` en la raíz para scripts temporales de prueba.
- Usar `scratch/` en la raíz para scripts de mantenimiento, organizados en subcategorías.

### ❌ PROHIBIDO — VIOLACIONES COMUNES A EVITAR
- **Nunca** crear carpetas `scratch/` dentro de `composeApp/src/` o carpetas de código fuente.
- **Nunca** colocar archivos de lógica de negocio directamente en la raíz de `src/` sin una carpeta de feature.
- **Nunca** duplicar componentes: si ya existe en `shared/`, importarlo; no copiarlo.
- **Nunca** añadir archivos de modelo/tipo directamente dentro de carpetas de UI sin separación.
- **Nunca** crear carpetas con nombres genéricos (`utils/`, `helpers/`, `misc/`) en la raíz del proyecto sin una categoría clara.
- **Nunca** dejar archivos de código sueltos en la raíz de `src/` sin pertenecer a una carpeta semántica.

## Regla de Crecimiento de Archivos

Como buena práctica, se debe **evitar normalmente que un archivo supere las 800 - 900 líneas**. El límite máximo permitido es de **1000 a 1200 líneas** (pudiendo llegar excepcionalmente hasta **1220 líneas**). Los archivos que superen las **1200 - 1220 líneas** son **deuda técnica activa** y el agente DEBE proponer su división en sub-componentes y registrarlo en el ROADMAP como tarea de refactorización pendiente.

- **Límite Preferido**: Evitar exceder de 800 a 900 líneas por archivo.
- **Límite Máximo Absoluto**: 1000 a 1200 líneas (máximo 1220 líneas excepcionales).
- **Componentes y Screens**: Si un archivo supera las 1200 líneas, extraer sub-componentes en su carpeta de feature.
