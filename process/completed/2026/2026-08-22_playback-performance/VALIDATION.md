# VALIDATION

## Comprobaciones automatizadas

- `.\gradlew.bat :composeApp:allTests --console=plain`: correcto (62 tareas ejecutadas/actualizadas, 0 fallos).
- 5 pruebas en `PlaybackLogicTest`, 3 en `StandardMidiCodecTest` y 3 en `ThemeJsonCodecTest` en todas las plataformas objetivo (Desktop, Android Debug y Android Release).
- `PianoRollCanvas`: optimizado con `currentTimeMsProvider: () -> Float`, interpolación sub-pixel continua en punto flotante y `forEachVisibleNote`.
- Restaurado y optimizado el gradiente vertical de estela y desenfoque de movimiento (`noteTrail` / motion blur): cabeza luminosa en el punto de impacto, difuminado progresivo hacia la cola y bordes armónicos que eliminan el judder/estroboscopio óptico retiniano.
- Erradicación del Garbage Collector (GC) jitter en partículas con bucle indexado directo (`particles.lastIndex downTo 0`).
- `ElyTimelineSlider`: nuevo componente en Canvas para sustituir el slider de Material 3. Elimina la cuantización de 50ms, ejecutándose en tiempo real a 60/144Hz dentro de `DrawScope` con cero recomposiciones, soporte táctil/drag y visualización del bucle A-B.
- `scripts/ReleaseTools.ps1` creado y `scripts/build-release.ps1` sincronizado con las directrices oficiales de `Docs` (distribución unificada exclusiva `.exe` en Windows y `.apk`/`.aab` universal en Android).

## Comprobación de arranque

- `.\gradlew.bat :composeApp:run --console=plain`: la aplicación Desktop inicia limpiamente y sin bloqueos.
- Multi-instancia verificada y preservada (sin single-instance lock).

## Cambios ajenos preservados

- `scripts/build-msix.ps1` conserva su modificación local de branding y no fue alterado negativamente.
