# 🚀 Regla y Sistema de Auto-Actualización desde GitHub Releases

> [!IMPORTANT]
> Todos los proyectos de aplicación (Android, Kotlin Multiplatform, Desktop) DEBEN incluir compatibilidad para la **comprobación automática, descarga interactiva e instalación directa de actualizaciones desde GitHub Releases**.

## Requerimientos Obligatorios

1. **Verificación Silenciosa al Iniciar**:
   - Al iniciar la aplicación, consultar la API de GitHub (`https://api.github.com/repos/biglexj/Ely-Tesia/releases/latest`) en segundo plano sin interrumpir al usuario.
   - **No mostrar ningún mensaje** si ya se está en la última versión (el check de inicio es transparente).
2. **Verificación Manual**:
   - Si el usuario pulsa "Buscar actualizaciones" (en Ajustes o en el diálogo Acerca de), mostrar la verificación inmediatamente:
     - Hay nueva versión → Abrir el `UpdateModalDialog` al 80% de ancho de forma síncrona.
     - Ya en la última versión → Mostrar un **Toast flotante centrado en la parte superior** (✅ "¡Estás en la última versión!") que se desvanece automáticamente a los **4 segundos**. No usar diálogos bloqueantes para este caso.
3. **Transición Flotante y Cierre de Diálogos Secundarios [CRÍTICO]**:
   - Al accionar "Buscar actualizaciones" desde un diálogo flotante (ej. "Acerca de"), la ventana flotante DEBE cerrarse en la misma acción y activar `showUpdateModal = true` de forma síncrona en el hilo principal.
   - **Prohibido**: No dejar gaps ni estados intermedios vacíos que causen parpadeos o destellos blancos entre modales.
4. **Modal Central Interactivo (Ancho al 80%) [CRÍTICO]**:
   - Mostrar actualizaciones dentro de un **Modal Central Interactivo (`UpdateModalDialog`)**.
   - El diálogo DEBE usar `DialogProperties(usePlatformDefaultWidth = false)` con `fillMaxWidth(0.80f)` (máximo 480.dp).
5. **Sanitización Canónica del `body` en Markdown [CRÍTICO]**:
   - Las notas de versión DEBEN sanitizarse mediante `sanitizeReleaseNotes(body: String): String` para eliminar Markdown crudo antes de mostrarse en la UI.
6. **Consistencia Paritaria Windows Desktop JVM [CRÍTICO]**:
   - Ninguna funcionalidad de descarga o actualización debe estar restringida únicamente a Android.
7. **Verificación Previa Obligatoria de la Release en GitHub [CRÍTICO]**:
   - Antes de cambiar el número de versión, el agente DEBE verificar la última release publicada en GitHub Releases.
   - La nueva versión DEBE ser estrictamente superior a la última tag publicada para evitar colisiones de versionado.
8. **Script Oficial de Release `build-release.ps1` [CRÍTICO]**:
   - Si el proyecto tiene `build-release.ps1`, el agente DEBE usarlo siempre:
     ```powershell
     .\build-release.ps1 -Version "X.Y.Z"
     ```
   - **Título de la Release (CRÍTICO)**: El título debe ser únicamente `Ely-Tesia vX.Y.Z`. Prohibido añadir subtítulos al título.
