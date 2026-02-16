@echo off
:: 1. Rutas de herramientas
set "J_PATH=C:\Program Files\Java\jdk-25\bin\jpackage.exe"
set "WIX_PATH=C:\Program Files (x86)\WiX Toolset v3.14\bin"
set "PATH=%PATH%;%WIX_PATH%"

:: 2. Configuración de tu App
set "NOMBRE_APP=GlobalTennis"
set "JAR_PRINCIPAL=GlobalTennis_1.0.18.jar"
set "CLASE_PRINCIPAL=raven.application.Application"
set "ICONO=src\raven\icon\xtreme.ico"
:: UUID UNICO para actualizaciones - NO CAMBIAR NUNCA
set "UPGRADE_UUID=54067302-6691-4976-9635-D66914976963"

echo ===========================================
echo Generando Instalador para %NOMBRE_APP%
echo usando JDK 25 y WiX 3.14
echo ===========================================
echo.
echo *** IMPORTANTE: INSTRUCCIONES PARA ACTUALIZACIONES ***
echo.
echo 1. Para ACTUALIZAR una version existente, use un numero
echo    de version MAYOR al instalado actualmente.
echo.
echo 2. Si usa la MISMA version, el instalador puede cerrarse
echo    inmediatamente sin mostrar opciones.
echo.
echo 3. Para REPARAR o DESINSTALAR, use "Agregar o quitar
echo    programas" en Windows.
echo.
echo Ejemplo: Si tiene 1.0.26 instalado, use 1.0.27 o superior
echo ===========================================
echo.

:: Solicitar version al usuario
set /p "APP_VERSION=Ingrese version del instalador (ej. 1.0.27): "

echo.
echo ===========================================
echo CONFIGURACION DEL INSTALADOR
echo ===========================================
echo Aplicacion: %NOMBRE_APP%
echo Version: %APP_VERSION%
echo JAR Principal: %JAR_PRINCIPAL%
echo UUID Upgrade: %UPGRADE_UUID%
echo Scope: Per-User (solo usuario actual)
echo ===========================================
echo.
echo Presione Ctrl+C para cancelar o cualquier tecla para continuar...
pause >nul

if exist output rmdir /s /q output

"%J_PATH%" ^
  --dest output ^
  --input dist ^
  --name "%NOMBRE_APP%" ^
  --main-jar "%JAR_PRINCIPAL%" ^
  --main-class %CLASE_PRINCIPAL% ^
  --icon "%ICONO%" ^
  --type exe ^
  --win-shortcut ^
  --win-menu ^
  --win-menu-group "%NOMBRE_APP%" ^
  --app-version %APP_VERSION% ^
  --win-upgrade-uuid "%UPGRADE_UUID%" ^
  --win-per-user-install ^
  --vendor "Raven" ^
  --win-dir-chooser

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ===========================================
    echo [OK] ¡Instalador creado exitosamente!
    echo ===========================================
    echo Ubicacion: output\%NOMBRE_APP%-%APP_VERSION%.exe
    echo.
    echo RECORDATORIO: Para actualizar, la proxima version
    echo debe ser MAYOR a %APP_VERSION%
    echo ===========================================
) else (
    echo.
    echo ===========================================
    echo [ERROR] Fallo al crear el instalador
    echo ===========================================
    echo Revisa los mensajes de error anteriores.
    echo ===========================================
)
pause

