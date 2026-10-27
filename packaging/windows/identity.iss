; Configuración por aplicación de Ely-Tesia (Biglex Modern 2.0).
; Mantener MyAppId estable entre versiones: una versión nueva debe actualizar
; la anterior en el mismo sitio y no crear una instalación paralela.
;
; MyAppId NO es el UpgradeCode del MSI de WiX ({28da0bdd-0ec9-3096-9fe8-2c59b53ec0ab})
; ni el Identity Name del MSIX (biglexj.ElyTesia): ni Windows Installer ni
; Windows recognises el EXE de Inno Setup como actualización de esos paquetes.
; La coexistencia MSI/EXE está documentada en MIGRATION.md.
#ifndef MyAppName
  #define MyAppName "Ely-Tesia"
#endif
#ifndef MyAppId
  #define MyAppId "Biglex.ElyTesia.Desktop"
#endif
#ifndef MyAppDirectory
  #define MyAppDirectory "Ely-Tesia"
#endif
#ifndef MyAppExe
  #define MyAppExe "ElyTesia.exe"
#endif
#ifndef MyAppIcon
  #define MyAppIcon "ElyTesia.ico"
#endif
#ifndef MyAppTheme
  #define MyAppTheme "dynamic"
#endif

#ifndef MyAppSubtitle
  #define MyAppSubtitle "Explora y practica tu música MIDI."
#endif
