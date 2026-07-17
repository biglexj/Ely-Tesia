# Roadmap de Ely-Tesia

Este documento organiza la evolución prevista de Ely-Tesia. No representa una
promesa de fechas ni obliga a implementar inmediatamente cada idea. Su objetivo
es conservar propuestas, discutirlas y priorizarlas antes de convertirlas en
cambios de código.

## Estados

- **En desarrollo:** existe trabajo activo y puede cambiar.
- **Planificado:** aprobado para una versión futura, pero todavía sin comenzar.
- **En evaluación:** la idea parece útil, aunque falta definir diseño o alcance.
- **Idea:** propuesta registrada sin compromiso de implementación.
- **Completado:** disponible en una versión publicada o candidata.

---

## Por implementar

### Próxima actualización — Validación y pulido Android

Estado: **Planificado**

- Pruebas adicionales en distintos dispositivos Android reales.
- Ajustes de latencia y tamaño de buffer por fabricante.

### Práctica por secciones

Estado: **Planificado**

- Selección de fragmentos A-B desde la barra de progreso.
- Repetición continua de una sección.
- Cuenta previa antes de cada repetición.
- Guardado de fragmentos difíciles por canción.

### Manos, pistas y canales

Estado: **Planificado**

- Selector de pistas y canales MIDI.
- Silenciar, reproducir en solitario u ocultar una pista.
- Modo de espera aplicado solamente a una mano.
- Volumen individual por pista.
- Asignación manual de pistas cuando el archivo no separa las manos.

### Barra de progreso avanzada

Estado: **Planificado**

- Pulsar o arrastrar para desplazarse por la canción.
- Tiempo actual y duración total.
- Marcadores de sección.
- Indicadores de cambios de tempo y compás.

### Nuevas plataformas

Estado: **Planificado**

La interfaz, los modelos, el parser MIDI, la biblioteca y gran parte de la
lógica son compartidos mediante Kotlin y Compose Multiplatform. Cada nuevo
sistema todavía requiere adaptadores propios para MIDI, audio, archivos,
persistencia, firma y empaquetado.

#### Linux

Prioridad: **Alta**

- Crear una entrada de aplicación y distribución JVM para Linux.
- Verificar MIDI mediante ALSA, dispositivos USB y Bluetooth MIDI.
- Probar el sintetizador y las salidas de audio en PipeWire y PulseAudio.
- Adaptar los selectores de archivos y las rutas de datos a XDG.
- Generar paquetes `.deb` para Debian, Ubuntu y derivados.
- Generar paquetes `.rpm` para Fedora, openSUSE y derivados.
- Generar un AppImage `x86_64` autocontenido como distribución principal.
- Evaluar AppImage para ARM64 cuando exista infraestructura de pruebas.
- Incluir archivo `.desktop`, icono, categorías y tipos MIME para `.mid` y `.midi`.
- Probar en Arch Linux y una distribución basada en Arch.

##### Arch Linux y Pacman

- Mantener inicialmente una receta `PKGBUILD` dentro del repositorio.
- Permitir que los usuarios construyan e instalen mediante `makepkg -si`.
- Evaluar publicación en AUR después de estabilizar el paquete.
- No depender de entrar inmediatamente en los repositorios oficiales de Arch.
- Aceptar colaboradores que quieran mantener el `PKGBUILD` y validar nuevas versiones.

##### Estrategia de compilación Linux

- Construir los paquetes en una máquina o runner Linux.
- Añadir un script `build-release-linux.sh` equivalente al script PowerShell.
- Incorporar un workflow de GitHub Actions para `.deb`, `.rpm` y AppImage.
- Publicar hashes SHA-256 junto a cada artefacto.
- Probar el AppImage al menos en Debian/Ubuntu, Fedora y Arch/Manjaro.

#### macOS

Prioridad: **Media**

- Crear paquetes `.dmg` y `.pkg` desde un equipo o runner macOS.
- Probar dispositivos MIDI mediante CoreMIDI y los adaptadores JVM disponibles.
- Adaptar audio, selector de archivos y directorios de datos de usuario.
- Integrar el icono en formato `.icns`.
- Firmar con Developer ID y completar notarización para evitar advertencias de Gatekeeper.
- Probar Intel y Apple Silicon.
- Evaluar un binario universal cuando el proceso de build sea estable.

#### iOS y iPadOS

Prioridad: **Media/Baja**

- Añadir targets iOS a Kotlin Multiplatform y un proyecto contenedor de Xcode.
- Reutilizar la UI Compose y adaptar la navegación a iPhone y iPad.
- Implementar MIDI mediante CoreMIDI, incluyendo dispositivos USB y Bluetooth.
- Implementar audio de baja latencia mediante AVAudioEngine u otra API nativa.
- Usar el selector de documentos de iOS para importar y exportar MIDI.
- Adaptar persistencia, rotación, multitarea y sesiones de audio.
- Probar teclados conectados mediante adaptadores USB-C/Lightning.
- Configurar firma, perfiles, TestFlight y distribución en App Store.

#### Matriz prevista de artefactos

| Plataforma | Artefactos principales | Entorno de build |
|---|---|---|
| Windows | EXE, MSI y MSIX | Windows + JDK 17 + WiX + Windows SDK |
| Android | APK y AAB | JDK 17 + Android SDK |
| Linux universal | AppImage | Linux x86_64 |
| Debian/Ubuntu | DEB | Linux |
| Fedora/openSUSE | RPM | Linux |
| Arch Linux | PKGBUILD/AUR | Arch Linux |
| macOS | DMG y PKG | macOS + JDK 17 |
| iOS/iPadOS | IPA/TestFlight | macOS + Xcode |

### Distribución

Estado: **Planificado**

- Publicación futura mediante Microsoft Store y WinGet.
- APK firmado para distribución directa.
- Android App Bundle para Google Play.
- Firma de código para Windows.
- Canal estable y canal de pruebas.
- Automatización de builds y releases mediante GitHub Actions.
- Publicación automática de hashes SHA-256.
- AppImage, DEB, RPM y receta PKGBUILD/AUR para Linux.
- Paquetes DMG/PKG firmados y notarizados para macOS.
- TestFlight y App Store para iOS/iPadOS.

---

## Ideas y conceptos

> Propuestas registradas sin compromiso de implementación. Pueden avanzar o descartarse según prioridad futura.

### Sistema de colores y temas visuales

Estado: **Completado en 1.0.5**

Separar la lógica de colores del código de UI para que el usuario pueda
personalizar la paleta de la aplicación sin tocar código.

- Colores independientes configurables para mano izquierda y mano derecha en el teclado virtual y en el piano roll.
- Paleta de colores por pista MIDI configurable desde la interfaz.
- Temas predefinidos seleccionables (Aurora, Clásico, Alto Contraste, etc.).
- Persistencia de la configuración de colores en la biblioteca de preferencias.
- Exportación e importación de temas personalizados.
- Modo alto contraste para accesibilidad.

### Rediseño visual para Android — Material Expressive

Estado: **Completado parcialmente en 1.0.5**

La interfaz de Android comparte actualmente el mismo diseño que Desktop. Para
Android existe una oportunidad de aprovechar las APIs nativas de Material 3 /
Material Expressive para una experiencia mucho más rica y nativa del ecosistema.

- Migrar los componentes de Android a Material 3 con Motion y Shape Tokens.
- Adoptar Material Expressive: ondas de tinta, transiciones de contenedor y
  morfología de formas animadas al tocar teclas o abrir secciones.
- Efectos visuales por toque: ripple extendido y retroalimentación háptica en
  el teclado virtual.
- Animaciones de entrada y salida en sidebars y modales con MotionLayout o la
  API de animación de Compose.
- Superficies tintadas dinámicas (Dynamic Color) tomando el color dominante del
  tema activo.
- Barra de navegación y estado adaptadas al tema oscuro de Ely-Tesia.

### Herramientas de aprendizaje

Estado: **En evaluación**

- Precisión de notas y ritmo.
- Notas correctas, incorrectas, adelantadas y atrasadas.
- Historial de sesiones de práctica.
- BPM máximo alcanzado por canción o fragmento.
- Aumento automático de velocidad tras completar una repetición.
- Objetivos diarios y tiempo total practicado.

### Compatibilidad MIDI avanzada

Estado: **En evaluación**

- Cambios de tempo durante una canción.
- Indicaciones de compás.
- Program Change y bancos General MIDI.
- Volumen y panorámica por canal.
- Pedales sostenuto y una corda.
- Pitch bend y rueda de modulación.
- Aftertouch y presión polifónica.
- Marcadores, letras y nombres de pista.
- Archivos MIDI tipo 0, 1 y secuencias múltiples.
- Evaluación futura de MIDI 2.0 y MPE.

### Audio avanzado

Estado: **En evaluación**

- Soporte de SoundFont.
- Piano muestreado de mayor calidad.
- Control de volumen maestro y por pista.
- Reverb opcional y limitador configurable.
- Medición y ajuste automático de latencia.
- Motor Oboe para optimizar Android en dispositivos que lo necesiten.
- Soporte de plugins VST3 (Windows/Linux/macOS) mediante puente JNI nativo.

### Experiencia móvil

Estado: **En evaluación**

- Modo horizontal dedicado para piano roll.
- Desplazamiento del teclado por octavas.
- Rango visible de 24, 36, 49, 61 u 88 teclas.
- Controles compactos durante la reproducción.
- Compatibilidad con tabletas y pantallas plegables.
- Mantener la pantalla activa durante una sesión.

### Biblioteca y edición

Estado: **Idea**

- Carpetas, etiquetas y favoritos.
- Búsqueda por nombre, BPM, duración o tonalidad.
- Edición sencilla de notas grabadas.
- Recorte de silencio al inicio y al final.
- Cuantización opcional.
- Duplicar, dividir o unir grabaciones.
- Importar y exportar colecciones.
- Copia de seguridad y restauración.

### Biblioteca comunitaria de archivos MIDI

Estado: **Idea**

Crear una biblioteca opcional para compartir archivos MIDI entre usuarios de
Ely-Tesia con fines personales, educativos y no comerciales.

#### Propuesta técnica

Supabase es la primera opción a evaluar (Auth, PostgreSQL, Storage, RLS y Edge Functions).
La aplicación nunca debe contener una `service key`. Todas las tablas estarán
protegidas mediante políticas RLS; cada usuario solo podrá modificar o eliminar
sus propias subidas.

#### Información de cada publicación

- Título, compositor, arreglista, usuario que sube, descripción y etiquetas.
- BPM, duración, número de pistas y canales.
- Nivel de dificultad aproximado.
- Licencia seleccionada, fuente original y declaración de autoría.
- Estado de moderación y hash SHA-256 para detectar duplicados.

#### Licencias permitidas

- Obra original del usuario, permitida para uso personal y no comercial.
- CC BY-NC 4.0 / CC BY-NC-SA 4.0.
- CC0 o dominio público verificable.
- Archivo compartido con permiso expreso del autor o titular.

No se ofrecerá una opción genérica como "encontrado en Internet".

#### Reglas de publicación

- Uso exclusivamente personal, educativo y no comercial.
- Confirmación de autoría o permiso suficiente antes de subir.
- Prohibición de vender archivos descargados desde la plataforma.
- Sistema de reporte y procedimiento de retirada con posibilidad de apelación.

#### Decisiones pendientes

- ¿Pública o requiere sesión para descargar?
- ¿Quién modera y resuelve reclamaciones?
- Revisar términos legales antes de abrir subidas públicas.
- Estimar almacenamiento, ancho de banda y límites del plan de Supabase.

### Ideas abiertas

Estado: **Idea**

- Modo concierto sin paneles visibles.
- Captura de video del piano roll.
- Compartir una interpretación como imagen o video.
- Integración opcional con partituras MusicXML.
- Detección de acordes y tonalidad.
- Generación de ejercicios a partir de una canción.
- Atajos de teclado configurables.
- Control mediante pedales o botones MIDI asignables.

---

## Completado

### Versión 1.0.0

- Visualizador MIDI y piano roll.
- Teclado virtual interactivo.
- Importación, entrada MIDI y exportación de grabaciones en Windows.
- Importación y exportación MIDI mediante el selector nativo de Android.
- Entrada MIDI Android mediante USB/OTG y Bluetooth.
- Sintetizador Android de baja latencia.
- Pedal sustain CC64 en entrada, grabación, reproducción y exportación.
- Sintetizador interno y selección de salida de audio en Windows.
- Biblioteca y preferencias persistentes.
- Grabación de notas, velocidad y duración.
- Cuenta previa configurable.
- Modo de espera, metrónomo, control de BPM, reinicio y bucle.
- Interfaz de escritorio y composición responsive para teléfonos.
- Instalador MSI y APK de prueba.

### Versión 1.0.1

- Sincronización temporal exacta entre barras y teclado.
- Colores por pista conservados en barras, partículas y teclas.
- Sostenidos y bemoles identificados en rosa.
- Evaluación de notas incorrectas en ámbar con tolerancia temporal.
- Retención del resultado y color durante toda la pulsación física.
- Resolución de tonos simultáneos compartidos por varias pistas.
- Corrección del sintetizador virtual y salida de audio de Windows.
- Persistencia y migración segura de la biblioteca entre actualizaciones.
- Empaquetado MSIX para Windows.

### Versión 1.0.2

- Banco de instrumentos inicial (Piano Acústico, Piano Eléctrico, Órgano, Sintetizador Pad, Clavecín, Flauta y Bajo Sintetizado).
- Correcciones de diseño visual en selectores y componentes de control.
- Envío automático de "Local Control: OFF" para silenciar bocinas internas de teclados físicos conectados.
- Soporte para audio estéreo real de 16 bits en Little Endian para compatibilidad con auriculares Bluetooth/USB.

### Versión 1.0.5

- APK y AAB Android firmados con una identidad permanente.
- Versionado unificado y validación automática de firma e identidad.
- Punto de composición `App.kt` pequeño y estructura por `app`, `core`, `feature` y `platform`.
- Lógica determinista del reproductor separada de Compose y cubierta por pruebas.
- Barra de progreso interactiva con tiempo actual y duración.
- Acceso a Instrumentos en portrait y layout compacto para teléfonos horizontales.
- Pantalla activa durante reproducción y grabación en Android.
- Mensajes visibles ante errores de dispositivos MIDI y audio.
- Temas comunitarios JSON importables y exportables con validación y persistencia.
- Temas integrados Aurora, Clásico y Alto Contraste.
- Tokens para manos, blancas, negras/bemoles, pulsación, errores, estelas y partículas.
- Material 3, movimiento expresivo y Dynamic Color opcional en Android 12+.
- Zoom multitáctil del teclado virtual en pasos de octava, centrado y limitado por el rango mapeado.
- Sincronización del rango visible entre teclado y piano roll durante el gesto de pellizco.

---

## Criterios para priorizar

Antes de aprobar una función se valorará:

1. Utilidad real para practicar, visualizar, grabar o aprender.
2. Cantidad de usuarios y plataformas beneficiadas.
3. Complejidad de uso y espacio que ocupa en la interfaz.
4. Compatibilidad con archivos y dispositivos existentes.
5. Riesgo de latencia, errores musicales o pérdida de grabaciones.
6. Coste de mantenimiento y pruebas.
7. Posibilidad de implementarla de forma opcional y no intrusiva.

## Plantilla para nuevas propuestas

```markdown
### Nombre de la propuesta

- Estado: Idea
- Problema que resuelve:
- Usuario o situación beneficiada:
- Funcionamiento esperado:
- Plataformas: Windows / Android / ambas
- Cambios de interfaz necesarios:
- Datos MIDI implicados:
- Riesgos o dudas:
- Criterio para considerarla terminada:
```

## Fuera de alcance por ahora

- Sustituir una estación de trabajo de audio profesional completa.
- Edición multipista avanzada comparable con un DAW.
- Sincronización en la nube obligatoria.
- Funciones que requieran conexión permanente para practicar localmente.
