# 🚀 Ely-Tesia v1.0.9 — Deslizador Continuo en Canvas, Desenfoque de Movimiento y Renderizado a 144 FPS

Llega **Ely-Tesia v1.0.9**, una actualización enfocada en la fluidez perceptual absoluta y la optimización del motor de reproducción. Esta versión estrena el nuevo **deslizador de línea de tiempo continuo en Canvas** con avance sub-píxel a 60/144 Hz sin saltos escalonados, el **efecto de estela y desenfoque de movimiento vertical (Motion Trail)** en la cascada de notas del piano roll, la eliminación de pausas del recolector de basura (GC) y la estandarización oficial del pipeline de distribución unificado.

---

### ✨ Novedades destacadas

- 🎛️ **Nuevo Deslizador Continuo en Canvas (ElyTimelineSlider)**: El cursor de la línea de tiempo ahora se dibuja directamente en Canvas con fluidez sub-píxel a 60/144 Hz, eliminando por completo los saltitos discretos, con soporte de arrastre inmediato y visualización de la región de bucle A-B.
- 🎹 **Estela y Desenfoque de Movimiento (Motion Trail & Head Glow)**: Las notas en el Piano Roll cuentan con un gradiente vertical de velocidad suave y cabeza de impacto luminosa, erradicando el parpadeo estroboscópico retiniano en piezas rápidas como *La Bamba*.
- ⚡ **Rendimiento Máximo y Cero Congelamientos**: Desacople total de la fase de dibujo (`DrawScope`) respecto a la composición global de Compose, optimización de indexación temporal de notas (O(log N)) y física de partículas directa sin pausas del Garbage Collector.
- 📦 **Distribución Oficial Estandarizada**: Distribución centrada exclusivamente en el instalador ejecutable unificado `.exe` para Windows y el paquete `.apk` firmado para Android conforme a las directrices del ecosistema.

---

### 💖 Apoyo y comunidad

Si disfrutas aprendiendo y practicando con **Ely-Tesia**, considera apoyar el desarrollo continuo:
- ☕ **Buy Me a Coffee**: https://buymeacoffee.com/biglexj
- 💳 **Donaciones directas**: https://www.biglexj.com/donaciones
- 🐙 **GitHub**: https://github.com/biglexj/Ely-Tesia
