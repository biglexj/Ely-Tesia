# 🚀 Instrucciones de Ejecución para el Agente - Ely-Tesia

> [!IMPORTANT]
> **ORDEN DE EJECUCIÓN INMEDIATA**: Si el usuario ha abierto este archivo o ha indicado *"Lee este archivo"*, debes seguir estrictamente los pasos descritos a continuación sin desviarte del plan.

## 📚 1. Reglas y Documentación a Consultar

Antes de iniciar cualquier modificación en el código:
1. Lee las reglas del proyecto en [.agents/rules/base.md](file:///.agents/rules/base.md), [.agents/rules/core_profile.md](file:///.agents/rules/core_profile.md) y [.agents/rules/folder_structure.md](file:///.agents/rules/folder_structure.md).
2. Revisa las instrucciones principales en [agent.md](file:///agent.md).
3. **Documentación Core & Proyectos de Referencia (CRÍTICO)**: Si tienes cualquier duda sobre patrones de diseño, arquitectura, manejo de carpetas o estándares compartidos:
   - **Documentación Core**: `D:\Proyectos\biglexj\Core-Docs` (consulta `README.md`, `ARCHITECTURE.md` y `REFERENCES.md`).
   - **Aurora (Aurora Blog)**: `D:\Proyectos\biglexj\Aurora---Blog` (referencia fullstack y documental).
   - **Luna Fetch**: `D:\Proyectos\biglexj\Luna---Fetch` (referencia KMP, Auto-updater y Desktop).

---

## 🛠️ 2. Flujo de Trabajo Estandarizado (Process Workflow)

- `[x]` **Paso 1**: Apertura de procesos en `process/active/YYYY-MM-DD_objetivo/` con sus 4 moldes (`PLAN.md`, `TASKS.md`, `VALIDATION.md`, `APPROVAL.md`).
- `[x]` **Paso 2**: Auditar la estructura de `composeApp/src/commonMain/kotlin/` y organizar composables por dominios en `features/` (`difficulty`, `handmode`, `library`, `player`, `shell`, `theme`).
- `[x]` **Paso 3**: Asegurar que los componentes compartidos entre 2 o más pantallas residan en `shared/components/` (`ElyBadge`, `ElyButton`, `ElyCard`, `ElyToast`).
- `[x]` **Paso 4**: Validar el crecimiento de archivos (límite preferido < 900 líneas, máximo 1200).
- `[x]` **Paso 5**: Mover scripts utilitarios fuera de norma a `scratch/` en la raíz.
- `[x]` **Paso 6**: Mantener actualizados `ROADMAP.md` y `RELEASE_NOTES.md` tras completar cada hito.
