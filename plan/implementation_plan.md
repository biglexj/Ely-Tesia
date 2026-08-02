# Implementation Plan - Refinamiento del Sistema de Auto-Actualización y Diálogos en Ely-Tesia

Refinar el sistema de verificación de actualizaciones y la interfaz de usuario de diálogos ("Acerca de la Aplicación" y "Modal Central de Actualizaciones") de acuerdo con los estándares canónicos del ecosistema `biglexj` y las reglas en `auto_updater.md` / `github_auto_updater_guide.md`.

## Problem Statement & Root Cause

1. **Error de Permiso de Red en Android**: La imagen `image.png` evidenció el mensaje `⚠️ Sin conexión o error: Permission denied (missing INTERNET permission?)`. `AndroidManifest.xml` en Android no declaraba el permiso `android.permission.INTERNET`.
2. **Ancho del Diálogo Reducido**: El diálogo `AboutDialog` estaba limitado a un ancho fijo estricto de `380.dp`, lo que resultaba estrecho y apretado. Debe ampliarse al **80% del ancho** de la pantalla (`fillMaxWidth(0.80f)` / `widthIn(max = 520.dp)` con `DialogProperties(usePlatformDefaultWidth = false)`).
3. **Flujo de Verificación Manual de Actualizaciones**:
   - Al pulsar "Buscar actualizaciones" estando en la versión más reciente, debe mostrar un **Toast flotante centrado en la parte superior** (`✅ Estás en la última versión de Ely-Tesia.`) que se desvanezca a los 4 segundos, sin mostrar cajas rojas de error o mensajes bloqueantes.
   - Al haber una actualización disponible, se debe cerrar el diálogo secundario `AboutDialog` de forma limpia y desplegar el **`UpdateModalDialog`** interactivo al 80% de ancho con las notas de versión sanitizadas (`sanitizeReleaseNotes`) y el botón directo de descarga/actualización.
4. **Modo Test en Desktop JVM**: `desktopMain/UpdateChecker.kt` tenía `TEST_UPDATE_MODE = true` forzado, impidiendo la comprobación real con GitHub Releases. Debe desactivarse para producción.

---

## User Review Required

> [!IMPORTANT]
> - El plan se ha guardado en la carpeta del proyecto: [`plan/implementation_plan.md`](file:///d:/Proyectos/biglexj/Ely-Tesia/plan/implementation_plan.md).
> - La verificación manual mostrará un **Toast flotante superior** si estás en la última versión (4 segundos de duración) y el **Modal Central al 80%** si hay nueva versión en GitHub Releases.

---

## Proposed Changes

### Android Configuration & Permissions

#### [MODIFY] [AndroidManifest.xml](file:///d:/Proyectos/biglexj/Ely-Tesia/composeApp/src/androidMain/AndroidManifest.xml)
- Agregar permiso de red: `<uses-permission android:name="android.permission.INTERNET" />`.

---

### Update Logic & Data Layer

#### [MODIFY] [UpdateChecker.kt (desktopMain)](file:///d:/Proyectos/biglexj/Ely-Tesia/composeApp/src/desktopMain/kotlin/com/biglexj/elytesia/update/UpdateChecker.kt)
- Desactivar `TEST_UPDATE_MODE = false` para consultar directamente GitHub Releases.

---

### UI Components & Dialogs

#### [NEW] [UpdateModalDialog.kt](file:///d:/Proyectos/biglexj/Ely-Tesia/composeApp/src/commonMain/kotlin/com/biglexj/elytesia/update/UpdateModalDialog.kt)
- Crear el componente `UpdateModalDialog` con `DialogProperties(usePlatformDefaultWidth = false)` y `fillMaxWidth(0.80f).widthIn(max = 520.dp)`.
- Mostrar notas de versión sanitizadas (`sanitizeReleaseNotes`), badge de versión y botones de acción ("📥 Descargar e Instalar" y "Cerrar").

#### [MODIFY] [AboutDialog.kt](file:///d:/Proyectos/biglexj/Ely-Tesia/composeApp/src/commonMain/kotlin/com/biglexj/elytesia/features/shell/AboutDialog.kt)
- Modificar las propiedades del diálogo para usar `DialogProperties(usePlatformDefaultWidth = false)` y `fillMaxWidth(0.80f).widthIn(max = 500.dp)`.
- Al pulsar "Buscar actualizaciones":
  - Si ya se está en la última versión: emitir callback de Toast flotante (`✅ Estás en la última versión de Ely-Tesia.`).
  - Si hay actualización disponible: invocar callback para cerrar `AboutDialog` y abrir `UpdateModalDialog`.

#### [MODIFY] [SidebarNavigation.kt](file:///d:/Proyectos/biglexj/Ely-Tesia/composeApp/src/commonMain/kotlin/com/biglexj/elytesia/features/shell/SidebarNavigation.kt) & [ElyTesiaAppContent.kt](file:///d:/Proyectos/biglexj/Ely-Tesia/composeApp/src/commonMain/kotlin/com/biglexj/elytesia/features/shell/ElyTesiaAppContent.kt)
- Manejar estados globales de `showUpdateModal`, `updateResult` y `toastMessage`.
- Integrar la comprobación silenciosa al iniciar la aplicación si no se ha comprobado en la sesión.
- Conectar la transición limpia entre `AboutDialog`, `UpdateModalDialog` y `ElyToast`.

---

## Verification Plan

### Automated Build & Tests
- Ejecutar `./gradlew desktopTest` para asegurar la compilación del código común y de escritorio.
- Compilar la versión de escritorio JVM con `./gradlew desktopRun` o `./gradlew packageReleaseUberJarForCurrentOS`.

### Manual Verification
- Comprobar visualmente que el diálogo "Acerca de" se despliega ocupando el 80% del ancho con estética amplia y limpia.
- Probar el botón "Buscar actualizaciones":
  - Si no hay actualización disponible: aparece el `ElyToast` en la parte superior durante 4 segundos.
  - Si hay actualización: `AboutDialog` se cierra y se abre el `UpdateModalDialog` al 80% de ancho con las release notes formateadas.
