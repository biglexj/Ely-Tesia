# Ely-Tesia 1.0.1

Primera actualización de parche centrada en la correspondencia visual entre el
piano roll, las teclas y la interpretación del usuario.

## Correcciones

- Sincronización exacta entre la llegada de una barra y la iluminación de su
  tecla; se eliminó el adelanto visual de 120 ms.
- Las teclas blancas conservan ahora el color de la pista activa: verde,
  violeta o crema.
- Las teclas negras se iluminan en rosa cuando reciben sostenidos o bemoles.
- Las pulsaciones correctas conservan el color de su pista o tipo de tecla:
  violeta, verde, crema o rosa.
- Las notas incorrectas muestran un destello ámbar dorado en lugar de rojo.
- Se aplica una tolerancia temporal de 180 ms para no marcar como error una
  pulsación ligeramente adelantada o atrasada.
- Las notas incorrectas permanecen ámbar mientras la tecla continúa pulsada.
- El resultado visual queda fijado desde el ataque hasta soltar la tecla; el
  color ya no vuelve a verde cuando termina una barra sostenida.
- Si dos pistas sostienen el mismo tono, la tecla representa la nota cuyo
  ataque es más reciente en vez de depender del orden interno del archivo.
- Las partículas de impacto respetan ahora el verde, violeta, crema o rosa de
  la nota correspondiente.
- Corrección de la salida virtual de Windows al cambiar o restaurar el
  dispositivo de audio.
- El gestor MIDI y el sintetizador de Windows conservan una única instancia
  durante las recomposiciones de la interfaz.
- Los datos locales se guardan fuera de la carpeta de instalación para evitar
  que una actualización elimine la biblioteca.
- Migración automática del estado anterior cuando todavía está disponible.
- Nuevo paquete MSIX para Microsoft Store o distribución firmada.

## Paquetes

- `ElyTesia-Windows-1.0.1.exe`
- `ElyTesia-Windows-1.0.1.msi`
- `ElyTesia-Windows-1.0.1.msix`
- `ElyTesia-Android-1.0.1-debug.apk`
- `SHA256SUMS.txt`

## Compatibilidad

La actualización conserva la biblioteca local, las canciones importadas, las
grabaciones, el rango mapeado y las preferencias de la versión 1.0.0 cuando el
archivo anterior sigue presente. Los nuevos datos se almacenan en
`%APPDATA%\Ely-Tesia`, separado del directorio de instalación.
