# 📋 Ely-Tesia — Registro de Tareas y Verificación (TASKS)

Documento dinámico de seguimiento técnico, listas de tareas activas, fases de desarrollo y checklist de verificación.

---

## 🔴 Pendientes Activos de Desarrollo

- [ ] Evaluación de umbrales automáticos en `DifficultyAnalyzer.kt`

---

## ⏳ Checklist de Verificación y Validaciones

- [x] **[Modo Práctica por Mano]**:
  - *Cómo probar:* Pulsar el botón de modo de manos en la barra de control para alternar entre Ambas Manos, Mano Izquierda y Mano Derecha.
  - *Resultado:* El teclado e indicadores de acierto filtran las notas según la mano seleccionada.

- [x] **[Persistencia de Canciones Importadas]**:
  - *Cómo probar:* Importar un archivo MIDI, cerrar la app y volver a abrirla.
  - *Resultado:* Las canciones importadas se mantienen en la biblioteca automáticamente.

- [x] **[Navegación y UI Táctil Adaptativa]**:
  - *Cómo probar:* Girar la pantalla o ajustar dimensiones de ventana.
  - *Resultado:* Barra lateral scrolleable con pie fijado abajo en modo vertical, chips en una línea y botón de importación a ancho completo.

---

## 🧭 Fases Técnicas de Desarrollo

### Fase v1.0.7 — Completada
- [x] Modo Práctica por Mano (`HandMode.kt`, `PlaybackLogic.kt`)
- [x] Auto-guardado y restauración de `customSongs` vía `snapshotFlow` y `state.txt`
- [x] Ajustes ergonómicos de UI en `SidebarNavigation.kt` y `LibraryPanel.kt`
- [x] Estandarización de templates del ecosistema biglexj
