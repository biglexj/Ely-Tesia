# Tareas — Ely-Tesia 1.0.5

- [x] Analizar el fallo de instalación de Android.
- [x] Aprobar `implementation_plan.md`.
- [x] Centralizar la versión 1.0.5 y establecer un `versionCode` monotónico.
- [x] Configurar firma release mediante secretos externos al repositorio.
- [x] Generar y validar APK y AAB firmados.
- [x] Endurecer `build-release.ps1` para impedir releases inválidas.
- [x] Extraer demos y lógica determinista de reproducción fuera del shell de UI.
- [x] Ordenar código compartido por `app`, `core` y `feature`.
- [x] Ordenar adaptadores Android/Desktop bajo `platform`.
- [x] Reducir `app/App.kt` a menos de 150 líneas (35 líneas).
- [x] Añadir progreso interactivo con tiempo actual y duración.
- [x] Mejorar controles Android portrait/horizontal.
- [x] Mantener la pantalla activa durante reproducción o grabación en Android.
- [x] Mejorar mensajes de errores Android.
- [x] Actualizar `ROADMAP.md`, `RELEASE_NOTES.md` y `README.md`.
- [x] Crear `walkthrough.md`.
- [x] Ejecutar pruebas comunes y builds Android/Windows.
- [x] Verificar firma, identidad y versión de artefactos Android.

## Seguimiento posterior

- [ ] Continuar extrayendo los paneles visuales del coordinador
  `ElyTesiaAppContent.kt` en una versión posterior, sin bloquear 1.0.5.

> No crear tag ni publicar una release hasta completar todas las validaciones.
