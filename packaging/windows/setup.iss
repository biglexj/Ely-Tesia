; ============================================================================
; setup.iss - Instalador de Windows de Ely-Tesia (Inno Setup 6.7+)
; ----------------------------------------------------------------------------
; Basado en la plantilla compartida Biglex Modern 2.0. Copia autocontenida:
;   - BiglexModernUI.iss : presentacion y validacion de la entrada
;   - identity.iss        : nombre, AppId, ejecutable, carpeta, icono y tema
;   - version.iss         : version real, la regenera el script de build
;
; Sustituye al instalador WiX/MSI de jpackage: un EXE autonomo, instalacion
; por usuario sin UAC y una sola pantalla de entrada (logo, nombre, bajada,
; ubicacion editable con selector de carpeta y un unico boton principal).
; Despues: progreso y pantalla de exito, con los controles nativos.
;
; La distribucion de la app (imagen de aplicacion de jpackage: lanzador +
; app\ + runtime\) la prepara scripts\packaging\Build-Installer.ps1 en
; build\inno-staging\app antes de compilar este script.
;
; Compilar:
;   & "D:\Proyectos\biglexj\Scripts\tools\InnoSetup\ISCC.exe" setup.iss /O<release>
; ============================================================================

#include "version.iss"
#include "identity.iss"

[Setup]
; --- IDENTIDAD DE ACTUALIZACION: unica y estable por app -------------------
; MyAppId vive en identity.iss y no cambia entre versiones: una version nueva
; actualiza la anterior en la misma carpeta en lugar de instalar en paralelo.
; No es el UpgradeCode del MSI de WiX ni el Identity Name del MSIX
; (ver MIGRATION.md).
AppId={#MyAppId}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher=Biglex
AppPublisherURL=https://biglexj.com
AppSupportURL=https://github.com/biglexj/Ely-Tesia
DefaultDirName={localappdata}\Programs\{#MyAppDirectory}
DefaultGroupName={#MyAppName}
; --- Pantallas: solo la nuestra + progreso + exito --------------------------
DisableWelcomePage=yes
DisableDirPage=yes
DisableProgramGroupPage=yes
DisableReadyPage=yes
OutputDir=Output
; El nombre del artefacto no colisiona con el EXE de jpackage del release
; actual (ElyTesia-Windows-X.Y.Z.exe): conviven los dos en release/.
OutputBaseFilename=ElyTesia-Setup-{#MyAppVersion}
SetupIconFile={#MyAppIcon}
UninstallDisplayIcon={app}\{#MyAppExe}
UninstallDisplayName={#MyAppName}
Compression=lzma2/max
SolidCompression=yes
ShowLanguageDialog=no
; --- Identidad visual moderna + tema claro/oscuro automatico (IS 6.6+) ---
WizardStyle=modern {#MyAppTheme}
#if MyAppTheme == "dark"
WizardBackColor=#15191C
WizardImageBackColor=#15191C
#else
WizardBackColor=#F7F9FA
WizardImageBackColor=clWhite
#endif
WizardBackColorDynamicDark=#15191C
WizardImageBackColorDynamicDark=#15191C
; --- Instalacion por usuario (sin privilegios de administrador) -------------
; PrivilegesRequiredOverridesAllowed se deja vacio a proposito: abrir el
; dialogo "Seleccion del Modo de Instalacion" anade una pantalla extra.
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
; --- Comportamiento --------------------------------------------------------
CloseApplications=yes
RestartApplications=no
SetupLogging=yes
VersionInfoDescription={#MyAppName} instalador
VersionInfoProductName={#MyAppName}
VersionInfoCompany=Biglex

[Languages]
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"

[Messages]
spanish.FinishedHeadingLabel=Instalación completada
spanish.FinishedLabel={#MyAppName} se instaló correctamente.
spanish.FinishedLabelNoIcons={#MyAppName} se instaló correctamente.
spanish.ClickFinish=Pulsa Finalizar para cerrar el instalador.

[CustomMessages]
spanish.RunApp=Ejecutar {#MyAppName}

[Files]
; Extraído explícitamente por la UI; al principio para evitar descomprimir payload solid.
Source: "{#MyAppIcon}"; Flags: dontcopy
; La distribucion real de la app (imagen de aplicacion de jpackage).
Source: "..\..\build\inno-staging\app\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExe}"; WorkingDir: "{app}"
Name: "{userdesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExe}"; WorkingDir: "{app}"

[Run]
; Lanza la app via el shell del escritorio: heredar el contexto del instalador
; hace que el proceso falle con 0xc00004bc. explorer.exe vive en {win}, no en {sys}.
Filename: "{win}\explorer.exe"; Parameters: """{app}\{#MyAppExe}"""; Description: "{cm:RunApp}"; WorkingDir: "{app}"; Flags: nowait postinstall skipifsilent

; Limpieza de lo que la aplicacion deja dentro de su propia carpeta de
; instalacion: el esqueleto de carpetas vacias de la imagen de aplicacion y el
; demo_escala.mid que la app regenera en su directorio de trabajo. Se limita a
; {app}: nunca toca los datos del usuario (%APPDATA%\Ely-Tesia) ni la
; instalacion MSI anterior (%LOCALAPPDATA%\ElyTesia).
[UninstallDelete]
Type: files; Name: "{app}\demo_escala.mid"
Type: filesandordirs; Name: "{app}\app"
Type: filesandordirs; Name: "{app}\runtime"

; Presentacion de la pantalla de entrada (Biglex Modern 2.0).
#include "BiglexModernUI.iss"

[Code]
procedure InitializeWizard();
begin
  ModernInitialize;
end;

procedure CurPageChanged(CurPageID: Integer);
begin
  ModernPageChanged(CurPageID);
end;

function NextButtonClick(CurPageID: Integer): Boolean;
begin
  Result := ModernNextButtonClick(CurPageID);
end;

[Code]
procedure CancelButtonClick(CurPageID: Integer; var Cancel, Confirm: Boolean);
begin
  ModernCancelButtonClick(CurPageID, Cancel, Confirm);
end;
