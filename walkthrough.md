# Recorrido de cambios — Ely-Tesia 1.0.5

## Resultado

La versión Android se corrigió para producir APK y AAB release firmados. La
versión se lee desde `gradle.properties` (`1.0.5`, código `6`) y el flujo de
release valida firma, identificador y metadatos antes de permitir una entrega.

`app/App.kt` es ahora un punto de composición de 35 líneas. El código se ordenó
físicamente en `core`, `feature` y adaptadores `platform`; los generadores de
demos y los cálculos deterministas de reproducción quedaron fuera del shell de
UI. La siguiente iteración puede continuar dividiendo los paneles visuales del
coordinador sin alterar la API pública de `App`.

## Funciones incorporadas

- Barra de progreso arrastrable con tiempo actual y duración.
- Reinicio seguro del motor MIDI después de desplazarse por la canción.
- Botón de Instrumentos disponible en la interfaz compacta.
- Diseño compacto también en teléfonos con poca altura horizontal.
- Pantalla Android activa solamente durante reproducción o grabación.
- Mensajes visibles para errores de MIDI, audio e instrumentos.

## Firma Android

Se creó una clave permanente local en `signing/elytesia-release.jks` y su
configuración en `keystore.properties`. Los dos archivos están ignorados por Git
y deben conservarse juntos en una copia de seguridad privada.

La firma verificada del APK usa APK Signature Scheme v2 y el certificado tiene
huella SHA-256 `4a249d294e9f580a6953c141158fa3fc4a876bc805d81ef74602459bd685d366`.

## Verificación realizada

- Compilación Kotlin Android debug: correcta.
- Compilación Kotlin Desktop: correcta.
- Android `assembleRelease`: correcto.
- Android `bundleRelease`: correcto.
- Empaquetado Windows EXE, MSI y MSIX: correcto.
- APK: `com.biglexj.elytesia`, `versionName 1.0.5`, `versionCode 6`.
- Firma APK: válida, un firmante RSA de 4096 bits.
- Artefactos y hashes SHA-256 generados en `release/`.

No había un dispositivo visible mediante ADB al cerrar esta tarea, por lo que la
instalación física queda como comprobación manual. No se desinstaló ninguna
versión anterior ni se alteraron datos de un teléfono.
