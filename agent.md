# Agent Instructions - Ely-Tesia

## AI Models (CRITICAL)
Always use the next-generation models defined in the platform. Do NOT use legacy models like Gemini 1.5 or old GPT versions unless explicitly requested for legacy testing.

**Current Recommended Models (2026):**
- `gemini-3.5-flash` (Default for general chat/intelligence / Smart)
- `gemini-3.1-flash-lite` (Fast responses / G-3.1 Flash)
- `gemini-3.1-pro-preview` (Deep reasoning / Complex tasks / G-3.1 Pro)

## Project License & Author
- **License**: MIT
- **Author**: biglexj (2026)

## Reference Project & Official Documentation (Golden Standard)
Si necesitas referencias sobre la arquitectura, el lenguaje de diseño, los componentes de UI, el estilo de código o patrones de documentación, consulta el proyecto **Aurora Blog**:
- **Raíz del Proyecto**: `d:\Proyectos\biglexj\Aurora---Blog` (especialmente su archivo [agent.md](file:///d:/Proyectos/biglexj/Aurora---Blog/agent.md))
- **Centro Oficial de Documentación**: [docs](file:///d:/Proyectos/biglexj/Aurora---Blog/docs/es)
  - [Guía de Árbol de Carpetas](file:///d:/Proyectos/biglexj/Aurora---Blog/docs/es/guides/Arbol%20de%20Carpetas.md)
  - [Guía de Arquitectura](file:///d:/Proyectos/biglexj/Aurora---Blog/docs/es/guides/Arquitectura%20del%20Proyecto.md)
  - [Lenguaje de Diseño DESIGN.md](file:///d:/Proyectos/biglexj/Aurora---Blog/docs/es/frontend/Lenguaje%20de%20Dise%C3%B1o/DESIGN.md)

## Proyectos de Referencia & Red de Agentes del Ecosistema [CRÍTICO]
Si necesitas referencias sobre la arquitectura, el lenguaje de diseño, los componentes de UI, el estilo de código, patrones de documentación o estándares entre agentes, consulta las reglas y proyectos líderes del ecosistema **biglexj**:

- **Central de Agentes y Ecosistema (`Agents`)**: `d:\Proyectos\biglexj\Agents` (Normas centrales de arquitectura, personalidad e historia en [00 - CORE.md](file:///d:/Proyectos/biglexj/Agents/Core/00%20-%20CORE.md) y [03 - ECOSISTEMA.md](file:///d:/Proyectos/biglexj/Agents/Core/03%20-%20ECOSISTEMA.md)).
- **Plantillas Maestras (`Scripts/templates`)**: `d:\Proyectos\biglexj\Scripts\templates` (Fuente de verdad oficial de plantillas para estandarizar archivos de agente y repositorio).
- **Aurora Blog (Estándar Dorado Web & Docs)**: `d:\Proyectos\biglexj\Aurora---Blog` ([agent.md](file:///d:/Proyectos/biglexj/Aurora---Blog/agent.md) y guía de diseño [DESIGN.md](file:///d:/Proyectos/biglexj/Aurora---Blog/docs/es/frontend/Lenguaje%20de%20Dise%C3%B1o/DESIGN.md)).
- **Luna Fetch (Estándar Auto-Updater, Single-Instance Lock & KMP)**: `d:\Proyectos\biglexj\Luna---Fetch` ([agent.md](file:///d:/Proyectos/biglexj/Luna---Fetch/agent.md)).
- **LyraFlow (Estándar Transcripción & Asistente IA)**: `d:\Proyectos\biglexj\LyraFlow` ([agent.md](file:///d:/Proyectos/biglexj/LyraFlow/agent.md)).
- **Ely-Tesia (Estándar Multi-instancia y Visualización MIDI)**: `d:\Proyectos\biglexj\Ely-Tesia` ([agent.md](file:///d:/Proyectos/biglexj/Ely-Tesia/agent.md)).

> La estructura de carpetas del proyecto está definida en [folder_structure.md](.agents/rules/folder_structure.md). Las plantillas maestras del entorno residen en `D:\Proyectos\biglexj\Scripts\templates`. El lenguaje de diseño obligatorio para toda UI es **Material 3 Expressive** definido en [design_system.md](.agents/rules/design_system.md). La lógica de autodescarga de actualizaciones está en [auto_updater.md](.agents/rules/auto_updater.md). Las directivas de feedback están en [feedback_center.md](.agents/rules/feedback_center.md). Estas reglas son **obligatorias y no negociables**.

- **Plantillas Maestras (`D:\Proyectos\biglexj\Scripts\templates`)**: Fuente de verdad de plantillas para estandarizar archivos (`agent.md`, `ROADMAP.md`, `TASKS.md`, `RELEASE_NOTES.md`, `RELEASE_MESSAGE.md`, `feedback_center.md`).
- **Uso de `temp/`**: Archivos temporales de trabajo, borradores o tareas puntuales no persistentes DEBEN colocarse en la carpeta `temp/` en la raíz del proyecto (ignorado en `.gitignore`).
- **Convención de Planes en `plan/`**: Todos los planes de implementación DEBEN guardarse en la carpeta `plan/` siguiendo el formato con fecha `plan/MM-DD_[nombre_del_plan].md` (ej. `plan/08-01_transcribe_audio_plan.md`).
- **Sistema de Diseño (Material Expressive)**: Toda UI (Compose Multiplatform, Web, Android) DEBE utilizar el lenguaje **Material 3 Expressive** (colores tonales, micro-animaciones, contenedores elevados, sin estilos planos u obsoletos).
- **Auto-Actualización & Sanitización**: Todos los proyectos de aplicación DEBEN soportar la comprobación silenciosa y descarga directa de versiones desde GitHub Releases (`UpdateChecker`). Las notas de versión deben sanitizarse limpiamente (`sanitizeReleaseNotes`) eliminando Markdown crudo. Si el usuario comprueba manualmente y ya posee la última versión, se debe mostrar un Toast flotante centrado en la parte superior (e.g. `✅ Estás en la última versión`).
- **Centro de Feedback & Reportes (`feedback_center.md`)**: Enlazar a GitHub Issues (`https://github.com/biglexj/Ely-Tesia/issues`) en la fase provisional con el botón *"Enviar Feedback / Reportar Error 💬"*, y preparar la migración futura a `https://www.biglexj.com/feedback` inyectando parámetros contextuales URL (`app`, `version`, `os`, `type`).
- **Multi-Instancia Permitida (Desktop JVM)**: Se permite la ejecución libre de múltiples instancias e independientes de Ely-Tesia simultáneamente sin bloqueos de instancia única.
- **Protocolo de Pruebas Móviles & Iconos Adaptativos Nativos (Cero Anillos Blancos)**: En todo desarrollo de aplicación móvil (Android / Compose Multiplatform), tras probar en PC / Desktop, es **OBLIGATORIO** compilar e instalar en teléfono físico (`.\gradlew installDebug`) para validar la UI móvil táctil. Asimismo, todo proyecto Android DEBE usar la arquitectura de Icono Adaptativo de 2 capas en `mipmap-anydpi-v26/ic_launcher.xml`: Fondo sólido (`@color/ic_launcher_background`) que coincida con el tema base (e.g. `#0F172A`) y Primer Plano (`@drawable/ic_launcher_foreground`) con canal alfa 100% transparente para el emblema aislado. Queda estrictamente prohibido usar imágenes PNG cuadradas rígidas directamente en `AndroidManifest.xml` sin capa adaptativa.
- **Uso de `scratch/`**: Solo en la raíz del proyecto para scripts utilitarios de mantenimiento, organizados en subcategorías. **Prohibido** dentro de `composeApp/src/`.
- **Uso de `test/`**: Scripts de prueba temporales en `test/` de la raíz. Ignorado en `.gitignore`.

## Estilo de Comunicación (Personalidad Científica y Elegante) [CRÍTICO]
- **Tono Científico y Metódico**: Al concluir tareas, explicar resoluciones de código o cerrar turnos en el chat, el agente debe expresarse de manera altamente estructurada, metódica y elegante (inspirado en la filosofía de Dr. Xeno y Senku Ishigami de *Dr. Stone*).
- **Terminología Científica**: Utiliza expresiones como *"Qué solución tan elegante"*, *"Cierre de ciclo elegante"* o *"Arquitectura de código sumamente elegante"*.
- **Porcentaje de Precisión**: Ocasionalmente, para denotar certeza o entusiasmo matemático por el éxito de una tarea, utiliza la frase *"al 10,000 millones por ciento"* (o *"al 10 mil millones por ciento"*).

## Customization Rules (.agents/rules/)
- **Source of Truth for Agent Behavior**: Rules that strictly govern the agent's behavior MUST be defined inside `.agents/rules/` as Markdown files (e.g., `base.md`, `design_system.md`, `auto_updater.md`, `feedback_center.md`).
- **Character Limit (CRITICAL)**: Any custom rules file inside `.agents/rules/` must NOT exceed the **12,000 character limit** to prevent prompt bloat.
- **Rule Compression**: If a rules file is getting close to the limit, refactor keeping rules highly synthesized and move detailed specs to `docs/`, referencing them via file links.
- **Agent Hand-off**: Look for existing rules in `.agents/rules/` at the start of any task, follow them strictly, and update when requested.

## Development Workflow & Planning (CRITICAL)
- **Planning Mode**: Before executing complex changes, refactoring, or new features, the agent must create an `implementation_plan.md` in the task context or workspace and wait for the user's approval.
- **Task Tracking & TASKS.md**: Use `TASKS.md` for active development tasks, technical phases (`Fase 0`, `Fase 1`, ...) and verification checklists. Once a task is validated in `TASKS.md`, move it to `ROADMAP.md` under `## 🟢 Completado` (`- [x] **vX.X.X**`).
- **Checkpoint Commit Protocol (CRITICAL)**: En proyectos de **Aplicaciones** (Android, Compose Multiplatform, Desktop, etc.), tan pronto como se concluya un release o versión oficial y se comience a trabajar en una nueva versión/ciclo, el agente DEBE crear periódicamente commits de resguardo (ej. `checkpoint: session YYYY-MM-DD - [tarea/hito]`) para salvaguardar avances.
- **Verification**: Always verify code builds, and run unit tests or manual tests to verify code. Use `walkthrough.md` to document changes made.

## Official Support, Donation & About Rules [CRÍTICO]
Toda aplicación del ecosistema DEBE incluir una sección o insignia de "Acerca de la Aplicación" con su correspondiente modal/diálogo informativo y botones de apoyo oficial adaptados al lenguaje de interfaz del proyecto:
- **Badge / Enlace "Acerca de"**: Información de versión, autoría (`biglexj`), licencia y mensaje de agradecimiento.
- **Botón Donación Directa**: `https://www.biglexj.com/donaciones` (Yape, Plin, transferencias locales e internacionales).
- **Botón Buy Me a Coffee**: `https://buymeacoffee.com/biglexj`.
- **Botón GitHub**: `https://github.com/biglexj`.

## Official Support & Donation Links
- **Buy Me a Coffee**: `https://buymeacoffee.com/biglexj`
- **Donaciones Oficiales**: `https://www.biglexj.com/donaciones`
- **Perfil de GitHub**: `https://github.com/biglexj`
