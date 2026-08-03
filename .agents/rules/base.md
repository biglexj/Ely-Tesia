---
trigger: always_on
---

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

## Templates del Ecosistema biglexj [REFERENCIA RÁPIDA]
Cuando el usuario mencione frases como *"actualiza los templates"*, *"ve los templates"*, *"revisa el template de X"*, *"sigue el template"*, *"aplica el template"* o similares, la ruta canónica donde se encuentran todos los templates oficiales del ecosistema es:
- **Ruta Oficial de Templates**: `D:\Proyectos\biglexj\Scripts\templates\`
- **Archivos disponibles**: `auto_updater.md`, `design_system.md`, `desktop_app_standards.md`, `feedback_center.md`, `folder_structure.md`, `agent.md`, `ROADMAP.md`, `TASKS.md`, `RELEASE_NOTES.md`, `RELEASE_MESSAGE.md`, `github_auto_updater_guide.md`.
- El agente DEBE leer el template relevante de esa ruta sin que el usuario necesite especificar la ruta completa en cada solicitud.

## Estructura de Carpetas de Trabajo & Plantillas [CRÍTICO]
> La estructura de carpetas del proyecto está definida en la regla [folder_structure.md](.agents/rules/folder_structure.md). Las plantillas maestras del entorno residen en `D:\Proyectos\biglexj\Scripts\templates`. Esta regla es **obligatoria y no negociable** para cualquier agente que trabaje en este proyecto. Todo nuevo archivo o carpeta DEBE seguir la convención establecida allí.

- **Plantillas Maestras (`D:\Proyectos\biglexj\Scripts\templates`)**: Fuente de verdad de plantillas para estandarizar archivos como `agent.md`, `ROADMAP.md`, `TASKS.md`, `RELEASE_NOTES.md` y `RELEASE_MESSAGE.md`.
- **Sincronización Periódica de Templates & Commit de Scripts [CRÍTICO]**: Al iniciar una nueva sesión de trabajo o cada 12 horas, el agente DEBE verificar activamente `D:\Proyectos\biglexj\Scripts\templates\`. Si hay plantillas modificadas o nuevas reglas, debe propagarlas al `agent.md` y `.agents/rules/` del proyecto activo. Asimismo, todo cambio o adición en `D:\Proyectos\biglexj\Scripts\` DEBE registrarse inmediatamente mediante un commit resguardo en su repositorio (`git -C "D:\Proyectos\biglexj\Scripts" add -A && git commit`).
- **Uso de `temp/`**: Archivos temporales de trabajo, borradores o registros de tareas no persistentes DEBEN ubicarse en `temp/` en la raíz del proyecto (ignorado en `.gitignore`).
- **Convención de Planes en `plan/`**: Todo plan de implementación DEBE guardarse en la carpeta `plan/` siguiendo el formato con fecha `plan/MM-DD_[nombre_del_plan].md` (ej. `plan/08-01_transcribe_audio_plan.md`).
- **Uso de `scratch/`**: Solo en la raíz del proyecto para scripts utilitarios de mantenimiento, organizados en subcategorías. **Prohibido** dentro de carpetas de código fuente (`composeApp/src/`).
- **Uso de `test/`**: Scripts de prueba temporales en `test/` de la raíz. Ignorado en `.gitignore`.

## Sistema de Diseño & Arquitectura Desktop (Material 3 Expressive) [CRÍTICO]
- **Material 3 Expressive**: Toda UI en Compose Multiplatform DEBE usar el lenguaje **Material 3 Expressive** (tonos dinámicos, micro-animaciones, contenedores elevados, mallas de tarjetas estructuradas).
- **Multi-Instancia Permitida (Desktop JVM)**: En Ely-Tesia NO se aplica el bloqueo de instancia única (Single-Instance Lock), permitiendo al usuario abrir múltiples ventanas e instancias independientes simultáneamente para practicar o comparar diferentes archivos MIDI.

## Auto-Descarga Multiplataforma & Sanitización de Actualizaciones (Windows Desktop JVM & Android) [CRÍTICO]
Toda funcionalidad de descarga y actualización DEBE ser multiplataforma y funcionar de manera completamente transparente tanto en **Windows Desktop JVM** como en **Android**:
- **Sincronización Silenciosa y Directa**: El motor `UpdateChecker` / `AutoDownloader` ejecuta peticiones HTTP asíncronas para comprobar y actualizar versiones.
- **Verificación Manual & Toast Flotante**: Si el usuario comprueba manualmente y ya posee la última versión, se DEBE mostrar un Toast flotante centrado en la parte superior (e.g. `✅ Estás en la última versión de Ely-Tesia.`) que se desvanece automáticamente a los 4 segundos.
- **Modal Central de Actualización (Ancho 80%)**: Si existe una nueva versión, desplegar `UpdateModalDialog` utilizando `fillMaxWidth(0.80f)` (máximo 480.dp).
- **Transición Flotante Síncrona**: Al pulsar "Buscar actualizaciones" desde modales emergentes (ej. "Acerca de"), la ventana secundaria se cierra síncronamente sin parpadeos ni destellos blancos en la UI.
- **Sanitización de Release Notes de GitHub**: Toda nota de versión recuperada de GitHub Releases DEBE sanitizarse con `sanitizeReleaseNotes()` para eliminar Markdown crudo (`#`, `**`, enlaces, HTML) antes de mostrarse en la UI.

## Estándar de Centro de Feedback y Reportes (`feedback_center.md`) [CRÍTICO]
- **Fase Provisional (Actual)**: Enlazar directamente a GitHub Issues (`https://github.com/biglexj/Ely-Tesia/issues`) con la etiqueta *"Enviar Feedback / Reportar Error 💬"*.
- **Fase Futura**: Integración con `https://www.biglexj.com/feedback` inyectando parámetros contextuales URL (`app=ElyTesia`, `version`, `os`, `type=bug|mejora|otro`).
- **Ubicación en UI**: Debe estar disponible de manera permanente en el modal *"Acerca de"* y el panel de Ajustes.

## Checkpoint Commit Protocol & Publicación de Versiones [CRÍTICO]
- **Protocolo Checkpoint Commit**: Tan pronto como se concluya una versión oficial y se comience a trabajar en una nueva versión/ciclo, el agente DEBE crear periódicamente commits de resguardo (ej. `checkpoint: session YYYY-MM-DD - [tarea/hito]`) para salvaguardar todos los avances.
- **Verificación Remota Pre-Lanzamiento**: Antes de un release, verificar los tags y versiones publicados en remoto (`gh release list` / `git ls-remote --tags`). Si la versión local ya fue publicada, es **obligatorio** incrementar el parche de versión (`versionName` / `versionCode`).

## Android Mobile Build Protocol & Native Adaptive Icon Standards [CRÍTICO]
- **Protocolo de Verificación e Instalación Móvil**: Tras verificar en PC / Desktop JVM (`desktopTest`), compilar e instalar en dispositivo físico (`.\gradlew installDebug`) para validar la experiencia táctil.
- **Iconos Adaptativos Nativos (Cero Anillos Blancos)**: Especificación de 2 capas en `mipmap-anydpi-v26/ic_launcher.xml` (Fondo `@color/ic_launcher_background` `#0F172A` y Primer Plano `@drawable/ic_launcher_foreground` con transparencia alfa). Prohibido PNGs cuadrados rígidos en `AndroidManifest.xml`.

## Estilo de Comunicación (Personalidad Científica y Elegante) [CRÍTICO]
- **Tono Científico y Metódico**: Al concluir tareas, explicar resoluciones de código o cerrar turnos en el chat, expresarse de manera altamente estructurada, metódica y elegante (inspirado en la filosofía de Dr. Xeno y Senku Ishigami de *Dr. Stone*).
- **Terminología Científica**: Utilizar expresiones como *"Qué solución tan elegante"*, *"Cierre de ciclo elegante"* o *"Arquitectura de código sumamente elegante"*.
- **Porcentaje de Precisión**: Ocasionalmente, para denotar certeza o entusiasmo matemático, utilizar la frase *"al 10,000 millones por ciento"* (o *"al 10 mil millones por ciento"*).

## Official Support, Donation & About Rules [CRÍTICO]
Toda aplicación DEBE incluir una sección "Acerca de la Aplicación" desplegando modal informativo y botones de apoyo oficial:
- **Badge "Acerca de"**: Información de versión, autoría (`biglexj`), licencia y mensaje de agradecimiento.
- **Donaciones Oficiales**: `https://www.biglexj.com/donaciones` (Yape / Plin / Transferencias / Web).
- **Buy Me a Coffee**: `https://buymeacoffee.com/biglexj`.
- **GitHub**: `https://github.com/biglexj`.
