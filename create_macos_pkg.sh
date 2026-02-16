#!/bin/bash

# =============================================================================
# SCRIPT PARA GENERAR INSTALADOR .PKG EN MACOS
# =============================================================================
# INSTRUCCIONES:
# 1. Copia este archivo y tu 'GlobalTenis.jar' a una carpeta en tu Mac.
# 2. Abre la terminal en esa carpeta.
# 3. Ejecuta: chmod +x create_macos_pkg.sh
# 4. Ejecuta: ./create_macos_pkg.sh
# =============================================================================

# NOMBRE DE TU APP
APP_NAME="GlobalTenis"
APP_VERSION="1.0.18"
MAIN_JAR="GlobalTenis.jar"
MAIN_CLASS="raven.application.Application"


# Comprobar si existe el JAR
if [ ! -f "$MAIN_JAR" ]; then
    echo "ERROR: No encuentro el archivo $MAIN_JAR en esta carpeta."
    echo "Asegurate de haber copiado el archivo generated en Windows (store/$MAIN_JAR) aqui."
    exit 1
fi

# Comprobar si jpackage esta disponible
if ! command -v jpackage &> /dev/null; then
    echo "ERROR CRITICO: El comando 'jpackage' no se encuentra."
    echo "Por favor, asegurate de instalar el JDK (Development Kit), no solo el JRE."
    echo "Intenta ejecutar: java -version"
    exit 1
fi

echo "--- Iniciando creacion del paquete para $APP_NAME ---"
echo "Usando Java version:"
java -version
echo "----------------------------------------------------"

# Limpieza previa
rm -rf "$APP_NAME.app"
rm -f "$APP_NAME-$APP_VERSION.pkg"

# Configuración del icono
ICON_PARAM=""
if [ -f "AppIcon.icns" ]; then
    ICON_PARAM="--icon AppIcon.icns"
else
    echo "AVISO: No se encontro AppIcon.icns. Se usara el icono por defecto."
fi

# 1. Crear la APP imagen primero
echo "[1/2] Generando .app..."
# EJECUTAMOS SIN SILENCIAR ERRORES PARA PODER VER QUE PASA
jpackage \
  --name "$APP_NAME" \
  --input . \
  --main-jar "$MAIN_JAR" \
  --main-class "$MAIN_CLASS" \
  --type app-image \
  --java-options "-Xmx1024m" \
  --vendor "RAVEN" \
  --app-version "$APP_VERSION" \
  $ICON_PARAM 

# Verifica si se creo la app
if [ -d "$APP_NAME.app" ]; then
    echo "Exito: $APP_NAME.app creado."
else
    echo "----------------------------------------------------------------"
    echo "ERROR: Fallo la creacion de la .app." 
    echo "Mira los mensajes de error arriba (lines rojas o textos de error)."
    echo "----------------------------------------------------------------"
    exit 1
fi

# 2. Crear el instalador PKG
echo "[2/2] Generando instalador .pkg..."
jpackage \
  --name "$APP_NAME" \
  --app-image "$APP_NAME.app" \
  --type pkg \
  --vendor "RAVEN" \
  --app-version "$APP_VERSION" \
  --mac-package-name "$APP_NAME"

# Nota: Si quieres que se instale en /Applications, jpackage lo hace por defecto con pkg.

echo "----------------------------------------------------------------"
if [ -f "$APP_NAME-$APP_VERSION.pkg" ]; then
    echo "¡LISTO! Instalador creado: $APP_NAME-$APP_VERSION.pkg"
else
    # A veces jpackage nombra el archivo diferente, buscamos cualquier pkg reciente
    PKG_FILE=$(ls -t *.pkg | head -1)
    if [ -n "$PKG_FILE" ]; then
        echo "¡LISTO! Instalador creado: $PKG_FILE"
    else
        echo "Algo fallo generando el PKG."
    fi
fi
echo "----------------------------------------------------------------"
