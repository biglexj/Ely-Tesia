# 💬 Estándar de Centro de Feedback y Reportes

> **Ámbito**: Convenciones para canalizar opiniones, reporte de bugs, solicitudes de mejora y comunicación directa con el usuario en Ely-Tesia.

---

## 1. 🌐 Canal de Feedback (Fase Actual vs Futura)

### 📌 Fase Provisional (Actual) — GitHub Issues Directo
Mientras el Centro de Feedback centralizado esté completando su integración:
- Enlazar directamente a **GitHub Issues** (`https://github.com/biglexj/Ely-Tesia/issues`).
- El botón en la UI debe etiquetarse como *"Enviar Feedback / Reportar Error 💬"*.

### 🚀 Fase Planificada (Futura) — Centro de Feedback (`https://www.biglexj.com/feedback`)
- **Paso de Metadatos vía URL**:
  Al accionar *"Enviar Feedback"*, la app abrirá `https://www.biglexj.com/feedback` inyectando:
  - `app`: `ElyTesia`
  - `version`: versión actual instalada
  - `os`: sistema operativo y arquitectura
  - `type`: tipo de reporte (`bug` | `mejora` | `otro`)

---

## 2. 📝 Inclusión en la UI
En todas las ventanas *"Acerca de la Aplicación"* y paneles de Ajustes principales, se debe incluir una opción visible para que el usuario pueda enviar su retroalimentación en cualquier momento.
