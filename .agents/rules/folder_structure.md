---
trigger: always_on
---

# 📁 Regla de Estructura de Carpetas — Ely-Tesia

> [!CAUTION]
> Esta regla es **CRÍTICA y no negociable**. Todo nuevo archivo, carpeta o módulo creado por el agente DEBE seguir esta convención. Violar esta estructura es inaceptable.

## Estructura Raíz del Proyecto (Kotlin Multiplatform / Compose)

```
Ely-Tesia/                          # Raíz del repositorio
├── .agents/rules/                  # Reglas del agente (base.md, folder_structure.md)
├── composeApp/                     # Módulo principal KMP / Compose
│   └── src/commonMain/kotlin/
│       ├── features/               # Lógica de negocio organizada por dominios (PascalCase o camelCase)
│       ├── shared/                 # Componentes transversales reutilizados por 2+ features
│       │   └── components/         # Átomos UI compartidos
│       └── theme/                  # Tokens de diseño, colores, tipografía
├── docs/                           # Documentación técnica
├── scratch/                        # Scripts utilitarios de mantenimiento (solo en raíz)
├── test/                           # Scripts de prueba temporales (ignorado en .gitignore)
├── agent.md                        # Instrucciones principales del agente (raíz)
├── INSTRUCCIONES_AGENTE.md          # Archivo ejecutable directo para el agente (raíz)
├── ROADMAP.md                      # Plan de trabajo y prioridades
├── RELEASE_NOTES.md                # Historial de cambios por versión
└── README.md                       # Documentación pública del proyecto
```

## Reglas Estructurales Obligatorias
- **Uso de `scratch/`**: Solo en la raíz del proyecto para scripts utilitarios. **Prohibido** dentro de `composeApp/src/`.
- **Límite de líneas**: Archivos de más de **400 líneas** deben dividirse en sub-componentes.
- **Centro Oficial de Documentación**: Para cualquier duda sobre arquitectura, consultar [d:\Proyectos\biglexj\Aurora---Blog\docs\es](file:///d:/Proyectos/biglexj/Aurora---Blog/docs/es).
