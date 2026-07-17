# Walkthrough — Sistema de temas de Ely-Tesia 1.0.5

## Resultado

La versión activa 1.0.5 permite seleccionar Aurora, Clásico o Alto Contraste,
instalar temas comunitarios desde `.elytheme.json`, exportarlos para compartir y
eliminar temas importados. La selección y los temas instalados persisten entre
sesiones.

Los archivos controlan los roles Material de la interfaz y los colores
musicales de mano izquierda, mano derecha, teclas blancas, teclas
negras/bemoles, pulsación correcta o incorrecta, espera, estelas y partículas.
Los efectos regulan brillo de pulsación, longitud de estela, intensidad de
partículas y movimiento expresivo.

## Plataformas

- Android usa el selector de documentos para importar y crear archivos de tema.
- Windows usa el selector nativo de archivos para abrir y guardar temas.
- Android 12+ puede aplicar Dynamic Color del sistema a los componentes
  Material; los colores musicales permanecen definidos por el tema.
- Sistemas anteriores y Desktop usan siempre la paleta Material del JSON.

## Seguridad y compatibilidad

- Tamaño máximo: 64 KiB.
- Esquema compatible: `schemaVersion: 1`.
- Colores permitidos: `#RRGGBB` y `#RRGGBBAA`.
- Se rechazan identificadores, versiones, colores, rangos y contrastes críticos
  inválidos.
- No se ejecuta código ni se descargan recursos indicados por un tema.
- El estado anterior se lee sin exigir campos de temas nuevos.

## Verificación

- Round-trip JSON de los tres temas integrados.
- Rechazo de esquema y color inválidos.
- Persistencia de tema seleccionado, Dynamic Color y temas importados.
- Pruebas comunes, Android debug/release y Desktop mediante
  `:composeApp:allTests`.
- Validación sintáctica de JSON Schema y ejemplos.
- Build final Android firmado y paquetes Windows mediante `build-release.ps1`.

La documentación pública se encuentra en [`docs/themes/`](docs/themes/README.md).
