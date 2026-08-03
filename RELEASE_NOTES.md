# 🚀 Ely-Tesia - Historial de Versiones
📌 **Versión actual: `1.0.7` · Versión mínima requerida: `1.0.0`**

> [!IMPORTANT]
> **Regla del .9 para Versionado:**
> - Nunca se debe pasar de una versión de parche `.9` (ej. de `1.0.9` no se pasa a `1.1.0`). Al alcanzar el límite del parche `.9`, se incrementa el número menor/secundario (ej. pasando a `1.1.0`).
> - De igual manera, al alcanzar el límite de la versión menor `1.9.9` (o ante hitos de arquitectura significativos posteriores a `1.9.x`), se debe saltar obligatoriamente al siguiente número mayor completo, pasando a **`2.0.0`**. No se permiten números como `1.9.10` o `1.10.x`.
> - **Nombres de Dulces para Versiones Mayores:** Cada versión mayor (ej. `1.0.0`, `2.0.0`) debe nombrarse con un nombre de dulce o postre al estilo de las versiones clásicas de Android en orden alfabético (ej. `v1.0.0 (Apple Pie Update)`, `v2.0.0 (Banana Bread Update)`). Este nombre debe quedar reflejado de manera coordinada en el título de `README.md`, en `RELEASE_NOTES.md` al documentar la release, y en los archivos de configuración del proyecto.

### 🚀 v1.0.7 — "Persistencia de Canciones, Modo Práctica por Mano & Optimización UI" (patch) (02/08/2026)

Esta actualización introduce el nuevo **Modo Práctica por Mano**, permitiendo al usuario aislar y ejercitar de forma independiente la mano izquierda (notas graves), la mano derecha (notas agudas) o ambas manos simultáneamente. El teclado físico y el sistema de evaluación adaptan dinámicamente las notas iluminadas y los aciertos en tiempo real según la mano seleccionada.

Se implementó la **persistencia automática de canciones importadas** entre sesiones, asegurando que las canciones añadidas por el usuario se conserven de forma permanente al reiniciar la aplicación. Asimismo, se optimizó el motor de temas visuales dinámicos diferenciando los tonos de ambas manos para una mejor orientación cromática en el teclado.

En la interfaz de usuario, los marcadores de bucle de sección A-B se rediseñaron con notación clara (`📍 A: Inicio` / `📌 B: Fin`), el botón de importación MIDI se expandió a todo el ancho del panel para mayor facilidad táctil, y los selectores de dificultad y la barra lateral de navegación se dotaron de desplazamiento fluido adaptativo sin cortes de texto tanto en pantalla vertical como horizontal.

---


### 🚀 v1.0.6 — **"Experiencia Móvil Adaptativa, Transposición Musical & Iconos Nativos" (patch)** (25/07/2026)

Esta actualización optimiza la experiencia móvil táctil en teléfonos tanto en orientación vertical como horizontal (landscape), introduciendo controles numéricos ergonómicos, transposición de tono, estandarización nativa de iconos adaptativos y la refactorización modular de la arquitectura del proyecto.

Se implementó la persistencia del estado de reproducción y canción cargada (`rememberSaveable`), garantizando que la canción activa no se reinicie al girar el dispositivo. La barra de controles de reproducción se optimizó en modo horizontal ocupando una sola fila compacta y reduciendo la altura del teclado virtual a 95.dp para maximizar la visibilidad del piano roll en más del 60% de la pantalla. Los controles de Tempo (BPM) y Transposición (semitonos) se rediseñaron mediante botones uniformes que despliegan diálogos modales ergonómicos en el centro de la pantalla con botones amplios de ajuste fácil (`+`/`-`), selectores rápidos de porcentaje (`50%`, `75%`, `100%`, `125%`, `150%`) y opción de restablecimiento de tono.

En la plataforma Android se implementó el estándar nativo de iconos adaptativos de 2 capas (`mipmap-anydpi-v26/ic_launcher.xml` e `ic_launcher_round.xml`) utilizando un fondo sólido `#0F172A` y un primer plano PNG con transparencia de canal alfa completa, eliminando bordes o discos blancos al adaptarse a cualquier launcher del sistema. Asimismo, se integró el motor de color dinámico Material 3 Expressive, se estandarizó la arquitectura modular a `features/` y `shared/components/` alineada a `folder_structure.md`, y se aseguró la visibilidad permanente del botón para importar archivos MIDI.

---

Se completó una refactorización de `ElyTesiaAppContent.kt` reduciendo su tamaño de 2,194 líneas a un coordinador ultra conciso de < 230 líneas. Toda la lógica visual y de interacción se extrajo en sub-composables especializados (`SidebarNavigation`, `TopHeaderControlBar`, `PracticeStatsCard`, `MidiMappingDialog`, `ExportDialogs`, `LibraryPanel`, `InstrumentConfigPanel`, `PlaybackControlBar`), garantizando un límite estricto de < 300 líneas por archivo y verificando que el 100% de la suite de pruebas unitarias y de compilación se ejecuten sin errores.

---

### 🚀 v1.0.5 — **"Release Android estable y arquitectura ordenada" (patch)** (16/07/2026)

Android genera ahora APK y AAB firmados con una identidad permanente, versión interna 6 y validaciones que bloquean artefactos sin firma o con metadatos incorrectos. Debido a que las compilaciones Android anteriores usaban una firma debug o no tenían firma, puede ser necesario desinstalarlas una sola vez antes de instalar 1.0.5; las actualizaciones posteriores conservarán la nueva identidad.

La composición compartida se reorganizó en `app`, `core`, `feature` y adaptadores `platform`; `App.kt` quedó reducido a un punto de entrada de 35 líneas y la lógica determinista del reproductor dispone de pruebas. Se añadió progreso interactivo con tiempo, acceso a Instrumentos en móvil, mejor adaptación horizontal, pantalla activa durante la práctica y mensajes visibles para errores de dispositivos.

La misma 1.0.5 incorpora un sistema de temas comunitarios JSON con validación y persistencia, galería de Aurora, Clásico y Alto Contraste, importación/exportación nativa en Android y Windows, y colores separados para manos, teclas blancas, negras/bemoles, estados de pulsación, errores, estelas y partículas. La interfaz usa Material 3 con formas y transiciones expresivas; Android 12 o superior puede aplicar Dynamic Color a la interfaz sin sustituir los colores musicales del tema.

La revisión final corrige la lectura de archivos MIDI tipo 1 con varias pistas, restaura las 12 canciones incluidas con duraciones y reproducción funcionales y migra entradas duplicadas de versiones de prueba. En Android, la barra de progreso aparece únicamente al reproducir, los controles están centrados y el teclado virtual admite zoom multitáctil: separar los dedos agranda las teclas y pellizcar muestra una extensión mayor, manteniendo teclado y piano roll alineados.

---

### 🚀 v1.0.3 — **"Modo Libre y Gestión de Biblioteca" (patch)** (15/07/2026)

Esta actualización añade un nuevo "Modo Libre" que oculta por completo los paneles de control y menús para ofrecer una visualización limpia del piano roll y teclado virtual, accesible a través de un discreto botón de reactivación. Además, se añade la opción de eliminar canciones de la biblioteca y del almacenamiento local persistente mediante un botón de papelera en la barra lateral.

Se optimizó la interfaz móvil para dispositivos en orientación horizontal (landscape) ajustando el tamaño de textos, el espaciado de los botones y la altura del teclado virtual, asegurando que el contenido fluya sin superposiciones. También se mejoró el soporte de caracteres unicode (acentos y letra eñe) en la biblioteca de canciones de la versión de escritorio JVM y se corrigió el comportamiento del "Local Control" al alternar entre el sonido virtual y el físico.

**Archivos y Paquetes:**
- `ElyTesia-Windows-1.0.3.msi`
- `ElyTesia-Windows-1.0.3.exe`
- `ElyTesia-Windows-1.0.3.msix`
- `ElyTesia-Android-1.0.3.apk`
- `SHA256SUMS.txt`

---

### 🚀 v1.0.2 — **"Banco de Instrumentos y Silenciado MIDI Local" (patch)** (12/07/2026)

Esta actualización incorpora mejoras visuales de control y añade soporte para diversidad instrumental en el sintetizador. Se integró un banco inicial de 7 instrumentos (Piano Acústico, Piano Eléctrico, Órgano, Sintetizador Pad, Clavecín, Flauta y Bajo Sintetizado) seleccionables directamente desde la interfaz. 

Para mejorar la interacción con teclados físicos, se implementó el envío automático del mensaje MIDI "Local Control Off" (CC 122) al conectar un dispositivo, lo que desactiva los altavoces internos del teclado compatible para escuchar únicamente la salida virtual generada por el software. También se optimizó la compatibilidad de audio estéreo en Little Endian y se corrigieron detalles visuales en los selectores y menús deslizantes.

**Archivos y Paquetes:**
- `ElyTesia-Windows-1.0.2.msi`
- `ElyTesia-Windows-1.0.2.exe`
- `ElyTesia-Windows-1.0.2.msix`
- `ElyTesia-Android-1.0.2-debug.apk`
- `SHA256SUMS.txt`

---

### 🚀 v1.0.1 — **"Correspondencia Visual de Notas y Estabilidad de Sintetizador" (patch)** (12/07/2026)

Primera actualización de parche de Ely-Tesia centrada en la correspondencia visual entre el piano roll, las teclas y la interpretación del usuario. Se mejoró la sincronización temporal eliminando el adelanto visual de 120 ms entre la llegada de una nota y la iluminación del teclado. Además, las teclas blancas conservan ahora el color asignado de su pista activa (verde, violeta o crema), mientras que las teclas negras se iluminan en rosa para denotar sostenidos y bemoles. Las pulsaciones correctas retienen el color original de la nota y las incorrectas se muestran en un destello ámbar dorado con una tolerancia temporal de 180 ms para evitar penalizaciones injustas. El color permanece fijo durante toda la pulsación de la tecla y se resolvió el conflicto cuando dos pistas sostienen el mismo tono de forma simultánea.

En la plataforma Windows, se corrigió la salida de audio virtual al alternar dispositivos de salida y se unificó la instancia del gestor MIDI y el sintetizador para evitar problemas durante la recomposición de la interfaz. Los datos de la biblioteca local, grabaciones y preferencias ahora se guardan de forma externa en `%APPDATA%\Ely-Tesia` para asegurar que una actualización del software no elimine el estado previo del usuario, realizando una migración automática si está disponible. Finalmente, se añade soporte para el empaquetado y distribución en formato MSIX.

**Archivos y Paquetes:**
- `ElyTesia-Windows-1.0.1.msi`
- `ElyTesia-Windows-1.0.1.exe`
- `ElyTesia-Windows-1.0.1.msix`
- `ElyTesia-Android-1.0.1-debug.apk`
- `SHA256SUMS.txt`

> [!NOTE]
> **Actualización e instalación:** Si existe una compilación experimental anterior de Windows con la misma versión interna, se recomienda desinstalarla antes de instalar este MSI. Las versiones futuras conservarán la identidad de actualización configurada.
>
> **Licencia:** Publicado bajo la licencia MIT.

---

### 🚀 v1.0.0 — **"Lanzamiento Inicial de Ely-Tesia" (Apple Pie Update) (major)** (12/07/2026)

Lanzamiento inicial de Ely-Tesia como una herramienta de visualización y práctica MIDI multiplataforma para Windows y Android. El sistema incorpora un piano roll animado tridimensional con un teclado interactivo de 88 teclas personalizable. Permite la entrada de dispositivos MIDI físicos a través de USB/OTG y Bluetooth (en Android) y la API multimedia nativa de Windows, integrando un sintetizador de software interno de baja latencia con metrónomo y controles de reproducción (BPM, bucle, pausa y reinicio).

El flujo de práctica soporta la grabación en tiempo real conservando velocidad, duración y eventos del pedal de sustain (CC64), respaldado por una cuenta previa configurable. La interfaz es adaptativa para pantallas de escritorio y teléfonos móviles. Las canciones y tomas grabadas pueden renombrarse, reproducirse y exportarse directamente en formato MIDI. El instalador oficial se distribuye mediante paquetes MSI para Windows y archivos APK compilados de depuración para Android.
