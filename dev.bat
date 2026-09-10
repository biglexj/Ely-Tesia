@echo off
REM ============================================================================
REM  Ely-Tesia - Modo Desarrollo Rapido (Desktop)
REM ============================================================================
setlocal
set "ROOT=%~dp0"
cd /d "%ROOT%"
echo [Ely-Tesia] Iniciando modo desarrollo desktop (:composeApp:run)...
call "%ROOT%gradlew.bat" :composeApp:run
if errorlevel 1 (
    echo [Ely-Tesia] Error durante la ejecucion. Codigo: %errorlevel%
    pause
)
