# Plan de Adopción de Templates y Revisión de Estado (Ely-Tesia)

Revisión completa de los cambios pendientes en el repositorio, adopción de las nuevas reglas de las plantillas maestras (`D:\Proyectos\biglexj\Scripts\templates`) y análisis de características faltantes registradas en el `ROADMAP.md`.

## User Review Required

> [!IMPORTANT]
> Se actualizarán los archivos de reglas locales ([.agents/rules/base.md](file:///d:/Proyectos/biglexj/Ely-Tesia/.agents/rules/base.md) y [agent.md](file:///d:/Proyectos/biglexj/Ely-Tesia/agent.md)) para adoptar al 100% las nuevas directivas del ecosistema `biglexj`.

## Proposed Changes

### Reglas del Agente y Configuración

#### [MODIFY] [base.md](file:///d:/Proyectos/biglexj/Ely-Tesia/.agents/rules/base.md)
- Incorporar reglas de `desktop_app_standards.md` (Single-Instance Lock en Windows JVM, In-App Auto-Update silencioso y directo).
- Incorporar reglas de `auto_updater.md` (diálogo modal al 80% de ancho, Toast flotante de 4 segundos centrado en la parte superior al estar en la versión más reciente, sanitización con `sanitizeReleaseNotes`, transición síncrona sin destellos).
- Incorporar el protocolo de **Checkpoint Commits** (`checkpoint: session YYYY-MM-DD - [tarea/hito]`) durante el desarrollo activo.
- Añadir la plantilla `desktop_app_standards.md` a la lista de plantillas maestras en `D:\Proyectos\biglexj\Scripts\templates`.

#### [MODIFY] [agent.md](file:///d:/Proyectos/biglexj/Ely-Tesia/agent.md)
- Sincronizar con la plantilla maestra `D:\Proyectos\biglexj\Scripts\templates\agent.md`.

---

### Verificación y Resumen de Estado Actual

#### 1. Cambios Pendientes por Commit (Uncommitted Changes)
- **Auto-Actualización In-App**:
  - `UpdateModalDialog.kt` (Nuevo componente modal de actualización al 80% de ancho).
  - `UpdateChecker.kt` (Versión Android y Desktop JVM con sanitización e integración API).
  - `AboutDialog.kt`, `SidebarNavigation.kt`, `ElyTesiaAppContent.kt` (Integración de eventos, cierre síncrono de diálogos secundarios, Toast flotante global).
- **Organización de Arquitectura**:
  - Traslado de planes a la carpeta oficial `plan/` (`plan/implementation_plan.md`, `plan/implementation_plan_themes.md`).
  - Eliminación de borradores sueltos en la raíz (`task.md`, `walkthrough.md`, `task_themes.md`, etc.).

#### 2. Revisión del Roadmap (`ROADMAP.md`)
- **Versión activa en desarrollo**: 1.0.7 / 1.1.0
- **Características pendientes de implementar**:
  1. *Clasificación Automática de Dificultad para Canciones Importadas*: Motor `DifficultyAnalyzer.kt` (BPM, densidad de notas, polifonía simultánea, rango tonal, varianza rítmica).
  2. *Práctica por Secciones*: Selección A-B en la barra de progreso, repetición continua y cuenta previa.
  3. *Manos, Pistas y Canales*: Selector y silenciado de pistas MIDI, modo espera para una sola mano, volumen por canal.
  4. *Barra de Progreso Avanzada*: Pulsar/arrastrar para buscar tiempo, marcadores de sección, cambios de tempo.

## Verification Plan

### Automated Tests
- Ejecutar `./gradlew desktopTest` para confirmar la integridad de la compilación en Windows JVM.

### Manual Verification
- Comprobar que los archivos de reglas `.agents/rules/base.md` y `agent.md` respetan la estructura sin superar los 12,000 caracteres.
- Preparar la estructura para recibir los nuevos detalles del sistema de actualizaciones que enviará el usuario.
