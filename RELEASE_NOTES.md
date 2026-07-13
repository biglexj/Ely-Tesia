# Ely-Tesia 1.0.1

Actualización de estabilidad centrada en la correspondencia entre el piano
roll, el teclado MIDI y los colores de interpretación.

## Novedades y correcciones

- Visualización de notas MIDI mediante un piano roll animado.
- Teclado virtual de 88 teclas con rango configurable.
- Entrada MIDI física en Windows y Android.
- Teclados Android USB/OTG y Bluetooth MIDI.
- Importación y exportación mediante el selector nativo de Android.
- Sintetizador interno, metrónomo y modo de espera en ambas plataformas.
- Controles de reproducción, BPM, reinicio y bucle.
- Barra de progreso y biblioteca persistente.
- Importación de canciones MIDI en Windows.
- Grabación con velocidad y duración de las notas.
- Cuenta previa de cero, uno o dos compases.
- Reproducción, renombrado y exportación MIDI de las tomas.
- Pedal sustain CC64 conservado al grabar, reproducir y exportar.
- Diseños independientes para escritorio y teléfonos.
- Instalador MSI y APK de prueba.
- Nuevo icono oficial de Ely-Tesia.
- Correspondencia de colores por pista entre barras, partículas y teclas.
- Sostenidos y bemoles iluminados en rosa.
- Errores indicados en ámbar, sin utilizar rojo.
- El color de una pulsación permanece fijado hasta soltar la tecla.
- Resolución estable cuando varias pistas mantienen el mismo tono.
- Corrección del sintetizador virtual y de la selección de audio en Windows.
- Persistencia de biblioteca y preferencias fuera de la instalación, con
  migración del estado anterior.
- Paquete MSIX para Windows.

## Correcciones incluidas

- Eliminación de notas duplicadas y reinicios accidentales del audio.
- Corrección del recorte visual de notas largas al entrar en pantalla.
- Reducción de chirridos, aliasing y distorsión del sintetizador.
- Espaciado y bordes uniformes en el teclado y los controles.
- Distribución responsive para pantallas móviles.
- Persistencia del rango MIDI, canciones y preferencias.

## Limitaciones conocidas

- El APK 1.0.0 está firmado como compilación `debug`; Google Play requerirá un
  AAB release y una clave de firma permanente.
- La latencia y compatibilidad MIDI pueden variar según el fabricante, la
  conexión USB/Bluetooth y las capacidades de audio del dispositivo Android.
- Los controladores MIDI distintos de sustain CC64 todavía no tienen acciones
  específicas dentro del sintetizador.
- La separación manual por pistas/manos y el bucle A-B están planificados para
  versiones posteriores.

## Archivos de la versión 1.0.1

- `ElyTesia-Windows-1.0.1.msi`
- `ElyTesia-Windows-1.0.1.exe`
- `ElyTesia-Windows-1.0.1.msix`
- `ElyTesia-Android-1.0.1-debug.apk`
- `SHA256SUMS.txt`

## Actualización e instalación

Si existe una compilación experimental anterior de Windows con la misma
versión interna, se recomienda desinstalarla antes de instalar este MSI.
Las versiones futuras conservarán la identidad de actualización configurada.

## Licencia

Ely-Tesia 1.0.1 se publica bajo la licencia MIT.
