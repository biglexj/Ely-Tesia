# Plan de Implementación — Características del Roadmap (v1.0.7)

Implementación de características del roadmap prioritarias en Ely-Tesia:

1. **Motor de Clasificación Automática de Dificultad (`DifficultyAnalyzer.kt`)**: Análisis mulimétrico de canciones MIDI importadas.
2. **Práctica por Secciones (Bucle A-B)**: Marcado de puntos de inicio (A) y fin (B) en la barra de progreso para repetición continua de fragmentos.
3. **Control y Silenciado de Pistas/Canales MIDI**: Posibilidad de mutear o reproducir en solo pistas específicas de la canción cargada.

---

## Proposed Changes

### Core & Analyzer

#### [NEW] [DifficultyAnalyzer.kt](file:///d:/Proyectos/biglexj/Ely-Tesia/composeApp/src/commonMain/kotlin/com/biglexj/elytesia/core/analyzer/DifficultyAnalyzer.kt)
- Crear el analizador heurístico de dificultad con pesos para:
  - BPM (Pulsos por minuto)
  - Densidad de notas por segundo (NPS)
  - Polifonía máxima simultánea (Acordes)
  - Rango tonal de teclado (Span entre nota más baja y alta)
  - Varianza de duraciones
- Retorna `Difficulty.FACIL`, `Difficulty.INTERMEDIO` o `Difficulty.AVANZADO`.

#### [MODIFY] [StandardMidiCodec.kt](file:///d:/Proyectos/biglexj/Ely-Tesia/composeApp/src/commonMain/kotlin/com/biglexj/elytesia/core/midi/StandardMidiCodec.kt)
- Integrar la llamada al nuevo `DifficultyAnalyzer` al decodificar cualquier archivo MIDI importado.

---

### Reproductor & Control de Reproducción

#### [MODIFY] [PlaybackControlBar.kt](file:///d:/Proyectos/biglexj/Ely-Tesia/composeApp/src/commonMain/kotlin/com/biglexj/elytesia/features/player/PlaybackControlBar.kt)
- Añadir controles ergonómicos para **Práctica por Secciones (Bucle A-B)**:
  - Botones "Marcar A" y "Marcar B".
  - Indicador visual de rango A-B activo (ej. `Bucle A-B: 0:12 ➔ 0:45`).
  - Botón para limpiar rango A-B.

#### [MODIFY] [ElyTesiaAppContent.kt](file:///d:/Proyectos/biglexj/Ely-Tesia/composeApp/src/commonMain/kotlin/com/biglexj/elytesia/features/shell/ElyTesiaAppContent.kt)
- Actualizar el reloj de reproducción (`LaunchedEffect(isPlaying)`) para que al alcanzar `loopEndMs` (Punto B) retroceda automáticamente a `loopStartMs` (Punto A) cuando el bucle A-B esté activo.
- Filtrar la emisión de notas e indicadores del teclado para respetar las pistas silenciadas.

---

### Mapeo e Interfaz de Biblioteca

#### [MODIFY] [LibraryPanel.kt](file:///d:/Proyectos/biglexj/Ely-Tesia/composeApp/src/commonMain/kotlin/com/biglexj/elytesia/features/library/LibraryPanel.kt)
- Mostrar badges de dificultad en tarjetas de canciones importadas basándose en `DifficultyAnalyzer`.

---

## Verification Plan

### Automated Tests
- Ejecutar `./gradlew desktopTest` para asegurar que las pruebas y compilaciones pasen limpiamente en Windows JVM.

### Manual Verification
- Cargar una canción MIDI y marcar los puntos A y B durante la reproducción para validar que el reproductor regrese suavemente al inicio de la sección marcada.
- Importar un archivo MIDI complejo y un MIDI simple para verificar la clasificación de dificultad resultante.
