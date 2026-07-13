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

## Versión 1.0.0

Estado: **Completado**

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

## Versión 1.0.1

Estado: **Completado**

- Sincronización temporal exacta entre barras y teclado.
- Colores por pista conservados en barras, partículas y teclas.
- Sostenidos y bemoles identificados en rosa.
- Evaluación de notas incorrectas en ámbar con tolerancia temporal.
- Retención del resultado y color durante toda la pulsación física.
- Resolución de tonos simultáneos compartidos por varias pistas.
- Corrección del sintetizador virtual y salida de audio de Windows.
- Persistencia y migración segura de la biblioteca entre actualizaciones.
- Empaquetado MSIX para Windows.

## Versión 1.0.2

Estado: **Completado**

- Incorporación de un banco de instrumentos inicial (Piano Acústico, Piano Eléctrico, Órgano, Sintetizador Pad, Clavecín, Flauta y Bajo Sintetizado).
- Correcciones de diseño visual en selectores y componentes de control.
- Envío automático de comando MIDI "Local Control: OFF" para silenciar las bocinas internas de los teclados físicos conectados.
- Soporte para audio estéreo real de 16 bits en Little Endian para compatibilidad total con auriculares Bluetooth/USB.

## Próxima actualización - Estabilización

Estado: **Planificado**

- Pruebas adicionales en distintos dispositivos Android reales.
- Ajustes de latencia y tamaño de buffer por fabricante.
- Mejoras de mensajes de conexión y errores MIDI.
- Correcciones encontradas tras la publicación 1.0.0.
- APK y AAB release firmados con una clave permanente.

## Próximas funciones

### Práctica por secciones

Estado: **Planificado**

- Selección de fragmentos A-B desde la barra de progreso.
- Repetición continua de una sección.
- Cuenta previa antes de cada repetición.
- Guardado de fragmentos difíciles por canción.

### Manos, pistas y canales

Estado: **Planificado**

- Colores independientes para mano izquierda y derecha.
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

### Herramientas de aprendizaje

Estado: **En evaluación**

- Precisión de notas y ritmo.
- Notas correctas, incorrectas, adelantadas y atrasadas.
- Historial de sesiones de práctica.
- BPM máximo alcanzado por canción o fragmento.
- Aumento automático de velocidad tras completar una repetición.
- Objetivos diarios y tiempo total practicado.

## Compatibilidad MIDI futura

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

## Audio

Estado: **En evaluación**

- Instrumentos seleccionables.
- Soporte de SoundFont.
- Piano muestreado de mayor calidad.
- Control de volumen maestro y por pista.
- Reverb opcional y limitador configurable.
- Medición y ajuste automático de latencia.
- Motor Oboe para optimizar Android en dispositivos que lo necesiten.

## Biblioteca y edición

Estado: **Idea**

- Carpetas, etiquetas y favoritos.
- Búsqueda por nombre, BPM, duración o tonalidad.
- Edición sencilla de notas grabadas.
- Recorte de silencio al inicio y al final.
- Cuantización opcional.
- Duplicar, dividir o unir grabaciones.
- Importar y exportar colecciones.
- Copia de seguridad y restauración.

## Biblioteca comunitaria de archivos MIDI

Estado: **Idea**

Crear una biblioteca opcional para compartir archivos MIDI entre usuarios de
Ely-Tesia con fines personales, educativos y no comerciales.

### Propuesta técnica

Supabase es la primera opción a evaluar porque permite mantener en un mismo
servicio:

- Supabase Auth para cuentas y perfiles.
- PostgreSQL para metadatos, licencias, favoritos, reportes y moderación.
- Supabase Storage para almacenar los archivos `.mid`.
- Row Level Security para controlar lectura, subida, edición y eliminación.
- Edge Functions para validar archivos y ejecutar tareas de moderación.

La aplicación nunca debe contener una `service key`. El cliente utilizará una
clave pública y todas las tablas y objetos estarán protegidos mediante políticas
RLS. Cada usuario solamente podrá modificar o eliminar sus propias subidas.

### Información de cada publicación

- Título de la pieza.
- Compositor y arreglista, cuando corresponda.
- Usuario que realiza la subida.
- Descripción y etiquetas.
- BPM, duración, número de pistas y canales.
- Nivel de dificultad aproximado.
- Fecha y versión del archivo.
- Licencia seleccionada.
- Fuente original o enlace de atribución.
- Declaración de autoría o permiso.
- Estado de moderación.
- Hash SHA-256 para detectar archivos duplicados.

### Licencias permitidas

El hecho de compartir un archivo gratuitamente no elimina los derechos sobre
la composición, el arreglo o la interpretación. Por ello, toda subida deberá
elegir una licencia y confirmar que el usuario posee los derechos necesarios.

Opciones iniciales a evaluar:

- Obra original del usuario, permitida para uso personal y no comercial.
- CC BY-NC 4.0.
- CC BY-NC-SA 4.0.
- CC0 o dominio público verificable.
- Archivo compartido con permiso expreso del autor o titular.

No se ofrecerá una opción genérica como "encontrado en Internet". Ely-Tesia
no concederá licencias sobre obras que pertenezcan a terceros.

### Reglas de publicación

- Uso de la biblioteca exclusivamente personal, educativo y no comercial.
- Aceptación de unas condiciones de subida antes de publicar.
- Confirmación de que el usuario es autor o posee permiso suficiente.
- Atribución visible y conservada al descargar o volver a compartir.
- Prohibición de vender archivos descargados desde la plataforma.
- Prohibición de subir obras comerciales sin autorización.
- Sistema de reporte por derechos de autor, contenido incorrecto o spam.
- Procedimiento de retirada y posibilidad de apelación.
- Historial de cambios de licencia y metadatos.

### Experiencia dentro de Ely-Tesia

- Explorar por nombre, compositor, dificultad, BPM y etiquetas.
- Previsualizar metadatos antes de descargar.
- Añadir una pieza directamente a la biblioteca local.
- Favoritos y colecciones personales.
- Descarga local para practicar sin conexión.
- Indicador claro de licencia y atribución.
- Enlace para reportar un archivo.
- Perfil con las obras originales y arreglos compartidos por cada usuario.

### Moderación y seguridad

- Validar que el archivo sea realmente un Standard MIDI File.
- Limitar tamaño, frecuencia y cantidad de subidas.
- Rechazar ejecutables, archivos renombrados y contenido malformado.
- Analizar metadatos y evitar nombres o enlaces peligrosos.
- Detectar duplicados mediante hash sin asumir automáticamente que sean
  infracciones.
- Mantener publicaciones nuevas en revisión cuando sea necesario.
- Registrar acciones administrativas y retiradas.

### Decisiones pendientes

- Confirmar si la biblioteca será pública o requerirá iniciar sesión para
  descargar.
- Definir quién puede moderar y resolver reclamaciones.
- Revisar los términos legales antes de abrir subidas públicas.
- Estimar almacenamiento, ancho de banda y límites del plan de Supabase.
- Definir una alternativa de exportación o migración para no depender de un
  único proveedor.
- Decidir si los comentarios y calificaciones son necesarios en la primera
  versión.

## Experiencia móvil

Estado: **En evaluación**

- Modo horizontal dedicado para piano roll.
- Desplazamiento del teclado por octavas.
- Rango visible de 24, 36, 49, 61 u 88 teclas.
- Controles compactos durante la reproducción.
- Compatibilidad con tabletas y pantallas plegables.
- Mantener la pantalla activa durante una sesión.

## Nuevas plataformas

Estado: **Planificado**

La interfaz, los modelos, el parser MIDI, la biblioteca y gran parte de la
lógica son compartidos mediante Kotlin y Compose Multiplatform. Cada nuevo
sistema todavía requiere adaptadores propios para MIDI, audio, archivos,
persistencia, firma y empaquetado.

### Linux

Prioridad: **Alta**

- Crear una entrada de aplicación y distribución JVM para Linux.
- Verificar MIDI mediante ALSA, dispositivos USB y Bluetooth MIDI.
- Probar el sintetizador y las salidas de audio en PipeWire y PulseAudio.
- Adaptar los selectores de archivos y las rutas de datos a XDG.
- Generar paquetes `.deb` para Debian, Ubuntu y derivados.
- Generar paquetes `.rpm` para Fedora, openSUSE y derivados.
- Generar un AppImage `x86_64` autocontenido como distribución principal y
  agnóstica de la distribución.
- Evaluar AppImage para ARM64 cuando exista infraestructura de pruebas.
- Incluir archivo `.desktop`, icono, categorías y tipos MIME para `.mid` y
  `.midi`.
- Probar en Arch Linux y una distribución basada en Arch.

#### Arch Linux y Pacman

- Mantener inicialmente una receta `PKGBUILD` dentro del repositorio.
- Permitir que los usuarios construyan e instalen mediante `makepkg -si`.
- Evaluar publicación en AUR después de estabilizar el paquete.
- No depender de entrar inmediatamente en los repositorios oficiales de Arch.
- Aceptar colaboradores que quieran mantener el `PKGBUILD` y validar nuevas
  versiones.

#### Estrategia de compilación Linux

- Construir los paquetes en una máquina o runner Linux; no intentar generar
  paquetes nativos Linux desde Windows.
- Añadir un script `build-release-linux.sh` equivalente al script PowerShell.
- Incorporar un workflow de GitHub Actions para `.deb`, `.rpm` y AppImage.
- Publicar hashes SHA-256 junto a cada artefacto.
- Probar el AppImage al menos en Debian/Ubuntu, Fedora y Arch/Manjaro.

### macOS

Prioridad: **Media**

- Crear paquetes `.dmg` y `.pkg` desde un equipo o runner macOS.
- Probar dispositivos MIDI mediante CoreMIDI y los adaptadores JVM disponibles.
- Implementar un adaptador nativo si la ruta JVM no ofrece latencia o
  compatibilidad suficientes.
- Adaptar audio, selector de archivos y directorios de datos de usuario.
- Integrar el icono en formato `.icns`.
- Firmar con Developer ID y completar notarización para evitar advertencias de
  Gatekeeper.
- Probar Intel y Apple Silicon.
- Evaluar un binario universal cuando el proceso de build sea estable.

### iOS y iPadOS

Prioridad: **Media/Baja**

- Añadir targets iOS a Kotlin Multiplatform y un proyecto contenedor de Xcode.
- Reutilizar la UI Compose y adaptar la navegación a iPhone y iPad.
- Implementar MIDI mediante CoreMIDI, incluyendo dispositivos USB y Bluetooth.
- Implementar audio de baja latencia mediante AVAudioEngine u otra API nativa.
- Usar el selector de documentos de iOS para importar y exportar MIDI.
- Adaptar persistencia, rotación, multitarea y sesiones de audio.
- Probar teclados conectados mediante adaptadores USB-C/Lightning.
- Configurar firma, perfiles, TestFlight y distribución en App Store.
- Requerir macOS y Xcode para compilar, probar y publicar.

### Matriz prevista de artefactos

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

### Colaboración comunitaria

- Etiquetar issues por plataforma y tipo de paquete.
- Documentar cómo reproducir cada build desde cero.
- Aceptar mantenedores voluntarios para AUR, RPM, Homebrew y pruebas de
  hardware.
- Evitar declarar una plataforma estable sin una persona o entorno capaz de
  probar cada release.
- Mantener los adaptadores de plataforma separados de la lógica musical
  compartida.

## Distribución

Estado: **Planificado**

- Instaladores EXE/MSI y paquete MSIX para Windows. **Completado**
- Publicación futura mediante Microsoft Store y WinGet.
- APK firmado para distribución directa.
- Android App Bundle para Google Play.
- Firma de código para Windows.
- Canal estable y canal de pruebas.
- Automatización de builds y releases mediante GitHub Actions.
- Publicación automática de hashes SHA-256.
- Builds nativos independientes para Windows, Linux y macOS.
- AppImage, DEB, RPM y receta PKGBUILD/AUR.
- Paquetes DMG/PKG firmados y notarizados para macOS.
- TestFlight y App Store para iOS/iPadOS.

## Ideas abiertas

Estado: **Idea**

- Temas de color personalizados.
- Modo concierto sin paneles visibles.
- Captura de video del piano roll.
- Compartir una interpretación como imagen o video.
- Integración opcional con partituras MusicXML.
- Detección de acordes y tonalidad.
- Generación de ejercicios a partir de una canción.
- Atajos de teclado configurables.
- Control mediante pedales o botones MIDI asignables.

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

Copiar esta sección al final de `Ideas abiertas` o utilizarla en una issue de
GitHub:

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
