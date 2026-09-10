# PLAN

## Objetivo

Mejorar la fluidez del reproductor y del piano roll: corregir el deslizador inferior para eliminar los saltitos (reemplazándolo por un scrubber continuo de alto rendimiento en Canvas), restaurar el efecto de desenfoque de movimiento (blur / motion trail) en las notas que caen para erradicar el judder visual, y preparar la versión v1.0.9.

## Alcance

- Eliminar la cuantización a 50ms del slider de progreso.
- Implementar un scrubber de línea de tiempo continuo en Canvas (`ElyTimelineSlider`) con renderizado sub-píxel a 60/144Hz, cero recomposiciones en reproducción, interacción táctil/ratón y visualización de bucle A-B.
- Restaurar y refinar el gradiente vertical de estela / desenfoque de movimiento (`noteTrail`) en `PianoRollCanvas` con cabeza luminosa y bordes suaves sin efecto estroboscópico.
- Ajustar la sincronización de fotogramas y verificar fluidez en canciones de alta velocidad (como *La Bamba* a 150 BPM).
- Preparar los artefactos de versión v1.0.9 tras validar los retoques.

## Fuera de alcance

- Modificación del motor de síntesis de audio `SimpleSoftwareSynth`.
- Introducción de bloqueos de instancia única (prohibido en Ely-Tesia).

## Criterio de éxito

- El deslizador inferior avanza de forma continua y sedosa a 60/144Hz sin saltos discretos.
- Las notas del piano roll caen con un gradiente de desenfoque de movimiento fluido y sin vibración o saltitos visuales.
- Las pruebas automatizadas continúan pasando limpiamente al 100%.
