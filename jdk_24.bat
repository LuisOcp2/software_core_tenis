@echo off
setlocal enabledelayedexpansion

REM --- 1) Intento hallar Java 24 por registro ---
set "JAVA24="
for /f "tokens=2,*" %%A in ('reg query "HKLM\SOFTWARE\JavaSoft\JDK\24" /v JavaHome 2^>nul ^| find "JavaHome"') do set "JAVA24=%%B\bin\java.exe"

REM --- 2) Intento por rutas comunes (Oracle / Adoptium / Azul) ---
if not defined JAVA24 (
  for /d %%D in ("C:\Program Files\Java\jdk-24*"
                 "C:\Program Files\Eclipse Adoptium\jdk-24*"
                 "C:\Program Files\Microsoft\jdk-24*"
                 "C:\Program Files\Zulu\zulu-24*"
                 "C:\Program Files (x86)\Java\jdk-24*") do (
    if exist "%%~fD\bin\java.exe" set "JAVA24=%%~fD\bin\java.exe"
  )
)

REM --- 3) Si sigue sin encontrarse, pregunta al usuario ---
if not defined JAVA24 (
  echo No encontre Java 24 automaticamente.
  set /p "JAVA24=Arrastra aqui tu java.exe de JDK 24 y presiona ENTER: "
)

if not exist "%JAVA24%" (
  echo [ERROR] No existe: %JAVA24%
  echo Ajusta la ruta al java.exe de tu JDK 24.
  pause
  exit /b 1
)

echo Usando: %JAVA24%
"%JAVA24%" -version
echo --------------- RUN ---------------
pushd "%~dp0"
"%JAVA24%" -XX:+ShowCodeDetailsInExceptionMessages -jar "store\GlobalTenis.jar" 2>run_error.log
set "ERR=%ERRORLEVEL%"
popd
echo ----------------------------------
if not "%ERR%"=="0" (
  echo La app salio con codigo %ERR%. Revise run_error.log para el stacktrace.
)
pause
exit /b %ERR%
