# 🎯 Ely-Tesia — Roadmap

Plan de trabajo, objetivos de producto y hoja de ruta estratégica del proyecto.

> **Regla del roadmap:** El Roadmap reúne la visión general del producto y las ideas por estado. Las tareas activas de compilación, pruebas y fases técnicas se gestionan en los procesos activos de `process/active/`. Una vez validada y completada una tarea, pasa a ROADMAP.md dentro de la sección **Completado** (`- [x] **vX.X.X**`).

---

## 🔴 Pendientes activos

- [ ] **Gestión Fina de Pistas y Canales MIDI**: Selector visual para silenciar, reproducir en solitario u ocultar pistas específicas, con asignación manual de manos y control de volumen individual por pista.
- [ ] **Validación y Pruebas Físicas en Android**: Comprobación en múltiples dispositivos Android reales por ABI, calibración de latencia táctil y ajuste fino de buffers de audio por fabricante.
- [ ] **Empaquetado y Distribución en Linux**: AppImage autocontenido x86_64 como distribución principal, paquetes `.deb` / `.rpm` y soporte de backend de audio PipeWire/PulseAudio y ALSA MIDI.

---

## 🟡 Intermedio (Prioridad Media/Baja)

- [ ] **Soporte de Plataforma macOS**: Paquetes `.dmg` y `.pkg` firmados y notarizados, con integración de CoreMIDI nativo y compatibilidad universal para Apple Silicon e Intel.
- [ ] **Soporte de Plataforma iOS / iPadOS**: Target Kotlin Multiplatform con backend de audio de baja latencia (AVAudioEngine) y selector de documentos adaptado a iPad y iPhone.
- [ ] **Integración con Ecosistema Aurora Synapse**: Protocolo de interconexión local para sincronizar prácticas, logros y banco de archivos MIDI compartidos en red local (`aurora-synapse://elytesia/`).
- [ ] **Grabación y Exportación de Interpretaciones**: Grabador MIDI integrado para registrar sesiones de práctica del usuario, evaluar precisión histórica y exportar a formato `.mid`.

---

## ⚪ Descartado / En Pausa

- ⏸️ **Single-Instance Lock (Instancia Única)**: Descartado por regla de arquitectura (`desktop_app_standards.md`). Ely-Tesia permite deliberadamente abrir múltiples instancias independientes para comparar o practicar diferentes archivos MIDI a la vez.
- ⏸️ **Distribución MSI / MSIX en Windows**: Descartado de los releases oficiales en favor del instalador ejecutable unificado `.exe` (`Docs/global/releases/web-release-message-standard.md`) con compatibilidad WinGet e instalación silenciosa/pasiva.
- ⏸️ **Modo Online Obligatorio / Cuentas en la Nube**: Descartado en favor de una experiencia 100% offline, local, privada y sin telemetría intrusiva.

---

## 🟢 Completado

- [x] **v1.0.9** (2026-10-26)
  - Deslizador de línea de tiempo continuo en Canvas (`ElyTimelineSlider`): renderizado sub-píxel a 60/144 Hz con cero recomposiciones, respuesta táctil/arrastre inmediata y visualización del bucle A-B.
  - Estela y desenfoque de movimiento vertical (Motion Trail & Head Glow) en `PianoRollCanvas`: gradiente de velocidad difuminado y cabeza de impacto luminosa que eliminan el judder óptico estroboscópico.
  - Aislamiento de la fase de dibujo (`DrawScope`) en cascada y partículas sin mutaciones pesadas ni pausas del Garbage Collector.
  - Optimización del motor de reproducción con indexación temporal binaria de notas (O(log N)) y eliminación del doble retraso.
  - Pipeline de distribución oficial estandarizado conforme a Core-Docs (instalador `.exe` unificado en Windows y paquete `.apk` firmado en Android).
  - Clasificación automática de dificultad para canciones importadas (`DifficultyAnalyzer`): motor heurístico multimétrico que evalúa BPM, densidad de notas por segundo, polifonía máxima, rango tonal y duración para categorizar la canción como `FACIL`, `INTERMEDIO` o `AVANZADO`.

- [x] **v1.0.8**
  - Corrección de salida de audio MIDI en teclados físicos: eliminación de comandos invasivos `Local Control` (CC 122) al conectar instrumentos por USB.
  - Icono oficial de aplicación (`elytesia-icon.png`) integrado en la barra de título superior y en la barra de tareas de Windows.
  - Resolución de bloqueo crítico de arranque (bucle al 95%) con persistencia de ventana puramente nativa en Compose.
  - Micro-interacciones de puntero fluido (`PointerIcon.Hand`) en los menús desplegables de configuración de teclado e instrumentos.
  - Proporciones adaptativas refinadas para modales informativos y de actualización en PC y dispositivos móviles.

- [x] **v1.0.7**
  - **Modo Práctica por Mano**: Ejercitación aislada e independiente de mano izquierda (notas graves), mano derecha (notas agudas) o ambas manos simultáneamente (🎹 / 🫲 / 🫱).
  - **Persistencia Automática de Canciones Importadas**: Almacenamiento local persistente (`state.txt`) de canciones añadidas por el usuario entre inicios de aplicación.
  - **Visualización Diferenciada por Mano**: Motor de temas dinámicos con diferenciación tonal para la mano izquierda (color secundario) y derecha (color primario).
  - **Práctica por Secciones (Bucle A-B)**: Marcado con notación intuitiva (`📍 A: Inicio` / `📌 B: Fin`) para la repetición continua de fragmentos de la canción.
  - **Optimizaciones de UI y Navegación Táctil**: Botón de importación MIDI de ancho completo (`fillMaxWidth`), barra lateral scrolleable con pie de página fijado abajo en modo vertical, y chips de dificultad scrolleables sin saltos de línea.
  - **Estandarización de Templates del Ecosistema**: Sincronización completa de reglas locales (`.agents/rules/`) alineadas a `Docs`.

- [x] **v1.0.6**
  - Estandarización modular de la arquitectura del proyecto alineada estrictamente a `folder_structure.md`.
  - Optimización móvil táctil para orientación horizontal (landscape) con barra compacta en 1 sola fila y reducción de teclado a 95.dp.
  - Persistencia total de canción cargada y estados de reproducción entre rotaciones de pantalla (`rememberSaveable`).
  - Rediseño de controles de BPM y Transposición con botones uniformes y diálogos modales emergentes ergonómicos (`BpmAdjusterDialog` y `TransposeAdjusterDialog`).
  - Iconos adaptativos nativos Android de 2 capas (`mipmap-anydpi-v26/ic_launcher.xml` e `ic_launcher_round.xml`) con fondo sólido `#0F172A` y sin discos blancos.
  - Restauración y visibilidad permanente del botón para importar archivos MIDI.

- [x] **v1.0.0 – v1.0.5**
  - Motor inicial de síntesis MIDI multiplataforma (Kotlin Multiplatform + Compose Desktop JVM / Android).
  - Piano Roll visual con cascada de notas en tiempo real y teclado interactivo multitáctil con zoom por octavas.
  - Detección y soporte de dispositivos MIDI USB nativos.
  - Catálogo de canciones demo organizadas por dificultad (Fácil, Intermedio, Avanzado).
  - Sistema de temas visuales (Material 3 Expressive, Neón, Clásico, Bosque, Medianoche) y efectos de estela.
  - Sistema de auto-actualización silenciosa desde GitHub Releases y centro de feedback integrado.
