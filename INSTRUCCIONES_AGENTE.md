# 🚀 Instrucciones de Ejecución para el Agente - Ely-Tesia

> [!IMPORTANT]
> **ORDEN DE EJECUCIÓN INMEDIATA**: Si el usuario ha abierto este archivo o ha indicado *"Lee este archivo"*, debes seguir estrictamente los pasos descritos a continuación sin desviarte del plan.

## 📚 1. Reglas y Documentación a Consultar

Antes de iniciar cualquier modificación en el código:
1. Lee las reglas del proyecto en [.agents/rules/base.md](file:///.agents/rules/base.md) y [.agents/rules/folder_structure.md](file:///.agents/rules/folder_structure.md).
2. Revisa las instrucciones principales en [agent.md](file:///agent.md).
3. **Centro Oficial de Documentación (CRÍTICO)**: Si tienes cualquier duda sobre patrones de diseño, arquitectura, manejo de carpetas o reglas, DEBES consultar obligatoriamente la documentación oficial en:
   - [Documentación Oficial Aurora Blog](file:///d:/Proyectos/biglexj/Aurora---Blog/docs/es)
   - [Guía de Árbol de Carpetas](file:///d:/Proyectos/biglexj/Aurora---Blog/docs/es/guides/Arbol%20de%20Carpetas.md)
   - [Guía de Arquitectura](file:///d:/Proyectos/biglexj/Aurora---Blog/docs/es/guides/Arquitectura%20del%20Proyecto.md)
   - [Lenguaje de Diseño DESIGN.md](file:///d:/Proyectos/biglexj/Aurora---Blog/docs/es/frontend/Lenguaje%20de%20Dise%C3%B1o/DESIGN.md)

---

## 🛠️ 2. Checklist de Refactorización y Estandarización

- `[x]` **Paso 1**: Auditar la estructura de `composeApp/src/commonMain/kotlin/` y organizar composables por dominios en `features/` (`instrument`, `keyboard`, `library`, `player`, `shell`, `theme`).
- `[x]` **Paso 2**: Asegurar que los componentes compartidos entre 2 o más pantallas residan en `shared/components/` (`ElyBadge`, `ElyButton`, `ElyCard`, `ElyToast`).
- `[x]` **Paso 3**: Verificar el límite de 400 líneas por archivo composable. Dividir en sub-composables si se supera.
- `[x]` **Paso 4**: Mover scripts utilitarios fuera de norma a `scratch/` en la raíz.
- `[x]` **Paso 5**: Actualizar `ROADMAP.md` y `RELEASE_NOTES.md` tras completar los cambios.
