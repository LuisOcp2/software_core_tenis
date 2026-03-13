# Android/Flutter Mobile Application

## Descripción
Aplicación móvil desarrollada en Flutter para gestión de inventario en dispositivos Android.

## Estructura del Proyecto

### inventory_app/
Aplicación Flutter principal:
- `lib/` - Código fuente Dart
- `android/` - Configuración específica de Android
- `ios/` - Configuración específica de iOS
- `pubspec.yaml` - Dependencias Flutter
- `build/` - Archivos compilados

### inventory_api/
API Node.js para la aplicación móvil:
- `server.js` - Servidor principal
- `package.json` - Dependencias Node.js
- `.env` - Variables de entorno
- `node_modules/` - Módulos instalados

## Requisitos

### Para la App Flutter:
- Flutter SDK
- Dart SDK
- Android Studio o VS Code con plugins Flutter
- Android SDK

### Para la API:
- Node.js
- npm

## Instalación y Ejecución

### App Flutter:
```bash
cd inventory_app
flutter pub get
flutter run
```

### API Node.js:
```bash
cd inventory_api
npm install
npm start
```

## Funcionalidades Principales
- Consulta de inventario
- Gestión de productos
- Sincronización de datos
- Reportes móviles
