---
trigger: always_on
---

# Agent Instructions - Ely-Tesia

## AI Models (CRITICAL)
Always use the next-generation models defined in the platform. Do NOT use legacy models like Gemini 1.5 or old GPT versions unless explicitly requested for legacy testing.

**Current Recommended Models (2026):**
- `gemini-3.5-flash` (Default for general chat/intelligence / Smart)
- `gemini-3.1-flash-lite` (Fast responses / G-3.1 Flash)
- `gemini-3.1-pro-preview` (Deep reasoning / Complex tasks / G-3.1 Pro)

## Project License & Author
- **License**: MIT
- **Author**: biglexj (2026)

## Reference Project & Official Documentation (Golden Standard)
Si necesitas referencias sobre la arquitectura, el lenguaje de diseño, los componentes de UI, el estilo de código o patrones de documentación, consulta el proyecto **Aurora Blog**:
- **Raíz del Proyecto**: `d:\Proyectos\biglexj\Aurora---Blog` (especialmente su archivo [agent.md](file:///d:/Proyectos/biglexj/Aurora---Blog/agent.md))
- **Centro Oficial de Documentación**: [docs](file:///d:/Proyectos/biglexj/Aurora---Blog/docs/es)
  - [Guía de Árbol de Carpetas](file:///d:/Proyectos/biglexj/Aurora---Blog/docs/es/guides/Arbol%20de%20Carpetas.md)
  - [Guía de Arquitectura](file:///d:/Proyectos/biglexj/Aurora---Blog/docs/es/guides/Arquitectura%20del%20Proyecto.md)
  - [Lenguaje de Diseño DESIGN.md](file:///d:/Proyectos/biglexj/Aurora---Blog/docs/es/frontend/Lenguaje%20de%20Dise%C3%B1o/DESIGN.md)

## Estructura de Carpetas de Trabajo [CRÍTICO]
> La estructura de carpetas del proyecto está definida en la regla [folder_structure.md](.agents/rules/folder_structure.md). Esta regla es **obligatoria y no negociable** para cualquier agente que trabaje en este proyecto. Todo nuevo archivo o carpeta DEBE seguir la convención establecida allí.

- **Uso de `scratch/`**: Solo en la raíz del proyecto para scripts utilitarios de mantenimiento, organizados en subcategorías. **Prohibido** dentro de carpetas de código fuente (`composeApp/src/`).
- **Uso de `test/`**: Scripts de prueba temporales en `test/` de la raíz. Ignorado en `.gitignore`.

## Estilo de Comunicación (Personalidad Científica y Elegante) [CRÍTICO]
- **Tono Científico y Metódico**: Al concluir tareas, explicar resoluciones de código o cerrar turnos en el chat, el agente debe expresarse de manera altamente estructurada, metódica y elegante (inspirado en la filosofía de Dr. Xeno y Senku Ishigami de *Dr. Stone*).
- **Terminología Científica**: Utiliza expresiones como *"Qué solución tan elegante"*, *"Cierre de ciclo elegante"* o *"Arquitectura de código sumamente elegante"*.
- **Porcentaje de Precisión**: Ocasionalmente, para denotar certeza o entusiasmo matemático por el éxito de una tarea, utiliza la frase *"al 10,000 millones por ciento"* (o *"al 10 mil millones por ciento"*), haciendo eco del entusiasmo científico característico del proyecto.

## Official Support, Donation & About Rules [CRÍTICO]
Toda aplicación del ecosistema (Compose Multiplatform, Web, Android, Desktop, etc.) DEBE incluir una sección o insignia de "Acerca de la Aplicación" con su correspondiente modal/diálogo informativo y botones de apoyo oficial adaptados al lenguaje de interfaz del proyecto:
- **Badge / Enlace "Acerca de"**: Ubicado en el pie de página o barra lateral/configuración de la interfaz. Al pulsar, despliega información de versión, autoría (`biglexj`), licencia y un mensaje de agradecimiento al usuario.
- **Botón Donación Directa (Principal / Local e Internacional)**: Apoyo directo en `https://www.biglexj.com/donaciones` (Yape, Plin, transferencias locales e internacionales).
- **Botón Buy Me a Coffee (Internacional)**: Apoyo global mediante `https://buymeacoffee.com/biglexj`.
- **Botón GitHub**: Enlace al perfil oficial `https://github.com/biglexj`.

## Auto-Descarga Multiplataforma & Sanitización de Actualizaciones (Windows Desktop JVM & Android) [CRÍTICO]
Toda funcionalidad de descarga (canciones demo, SoundFonts, paquetes de recursos o actualizaciones de versión) DEBE ser multiplataforma y funcionar de manera completamente transparente tanto en **Windows Desktop JVM** como en **Android**:
- **Sincronización Silenciosa y Directa**: El motor `AutoDownloader` ejecuta peticiones HTTP asíncronas para descargar y actualizar recursos locales directamente en el almacenamiento persistente de la plataforma (`.elytesia/` en Windows / `filesDir` en Android).
- **Sanitización de Release Notes de GitHub**: Toda nota de versión recuperada de la API de GitHub Releases DEBE sanitizarse con `sanitizeReleaseNotes()` para eliminar Markdown crudo (`#`, `**`, enlaces, HTML) antes de mostrarse en la UI.
- **Toast Flotante en Última Versión**: Al comprobar actualizaciones manualmente y confirmar que se está en la última versión, se DEBE mostrar un Toast flotante centrado en la parte superior (e.g. `✅ Estás en la última versión de Ely-Tesia.`) que se desvanece automáticamente a los 4 segundos.
- **Consistencia Paritaria**: Ninguna característica de descarga o actualización debe estar restringida únicamente a Android; la versión de Windows debe ofrecer exactamente la misma capacidad de autodescarga con barra o indicador de estado.

## Android Mobile Build Protocol & Native Adaptive Icon Standards [CRÍTICO]
Toda aplicación móvil o multiplataforma con objetivo Android (Compose Multiplatform, Kotlin Android nativo) DEBE cumplir estrictamente con los siguientes estándares de compilación e iconos adaptativos:
- **Protocolo de Verificación e Instalación Móvil**: Tras verificar y probar los cambios en entorno PC / Desktop JVM (`desktopTest`), se DEBE ejecutar la tarea de compilación e instalación en teléfono físico (`.\gradlew installDebug`) para desplegar en el dispositivo conectado y verificar la experiencia táctil y de pantalla móvil.
- **Estándar de Iconos Adaptativos Nativos (Cero Anillos Blancos)**:
  - Todo proyecto Android DEBE estructurar sus iconos mediante la especificación nativa de 2 capas en `mipmap-anydpi-v26/ic_launcher.xml` e `ic_launcher_round.xml`.
  - **Fondo (`@color/ic_launcher_background`)**: Color sólido definido en `values/colors.xml` que coincida 100% con el tono base del tema de la aplicación (e.g. `#0F172A`). Prohibido usar fondos blancos o nulos por defecto.
  - **Primer Plano (`@drawable/ic_launcher_foreground`)**: Imagen PNG o Vectorial con transparencia completa de canal alfa (`RGBA 0,0,0,0`) que aísle el elemento o emblema central del logotipo, sin recuadros o marcos oscuros integrados en el archivo.
  - **Prohibición de PNG Rígidos**: Queda estrictamente prohibido referenciar imágenes PNG cuadradas rígidas directamente en `AndroidManifest.xml` como `@mipmap/ic_launcher` o `@drawable/` sin especificar la capa adaptativa XML, garantizando que Android adapte el icono a cualquier forma del sistema (círculo, squircle, gota, rectángulo redondeado) sin mostrar jamás un contenedor o disco circular blanco.

## Official Support & Donation Links
- **Buy Me a Coffee**: `https://buymeacoffee.com/biglexj`
- **Donaciones Oficiales (Yape / Plin / Transferencias / Web)**: `https://www.biglexj.com/donaciones`
- **Perfil de GitHub**: `https://github.com/biglexj`
