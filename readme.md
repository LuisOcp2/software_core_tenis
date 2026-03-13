# Software Core Tenis 🎾

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8+-orange.svg)](https://www.oracle.com/java/)
[![Flutter](https://img.shields.io/badge/Flutter-3.0+-blue.svg)](https://flutter.dev/)
[![Node.js](https://img.shields.io/badge/Node.js-14+-green.svg)](https://nodejs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-blue.svg)](https://www.mysql.com/)

Sistema integral de gestión de inventario y ventas para tiendas de artículos de tenis, desarrollado con arquitectura multiplataforma.

## 🚀 Descripción del Proyecto

Software Core Tenis es una solución completa que combina una aplicación desktop en Java, una aplicación móvil en Flutter y una API REST en Node.js para ofrecer una gestión eficiente de inventario, ventas y operaciones comerciales.

## 📱 Arquitectura del Sistema

El proyecto está organizado en tres componentes principales:

### 🖥️ **java_project/** - Aplicación Desktop
- **Tecnología**: Java 8+ con Swing y FlatLaf
- **Funcionalidades**: Gestión completa de inventario, ventas, reportes
- **Base de Datos**: MySQL 8.0+
- **Reportes**: JasperReports

### 📱 **android_project/inventory_app/** - Aplicación Móvil
- **Tecnología**: Flutter 3.0+
- **Plataformas**: Android (iOS en desarrollo)
- **Funcionalidades**: Consulta de inventario, gestión móvil, sincronización

### 🔧 **android_project/inventory_api/** - API REST
- **Tecnología**: Node.js + Express
- **Funcionalidades**: Servicio para la aplicación móvil
- **Autenticación**: JWT

## Características Principales

### Gestión Comercial

- **Ventas**: Registro y seguimiento de ventas realizadas con generación de facturas.
- **Clientes**: Administración de información de clientes y su historial de compras.
- **Proveedores**: Gestión de proveedores y compras realizadas.
- **Reportes de Ventas**: Análisis detallado de ventas por período, producto o cliente.

### Gestión de Productos

- **Inventario**: Control de stock, entradas y salidas de productos.
- **Categorías**: Organización de productos por categorías.
- **Marcas**: Gestión de marcas de productos.
- **Rotulación**: Generación e impresión de etiquetas para productos.
- **Movimientos**: Registro de todos los movimientos de inventario.

### Administración

- **Usuarios**: Control de acceso y permisos para diferentes roles.
- **Cajas**: Gestión de cajas y movimientos de efectivo.
- **Promociones**: Configuración de descuentos y ofertas especiales.

## Tecnologías Utilizadas

- **Lenguaje**: Java
- **Interfaz Gráfica**: Java Swing con FlatLaf para un diseño moderno
- **Base de Datos**: MySQL
- **Reportes**: JasperReports
- **Componentes Adicionales**:
  - MiGLayout para diseño de interfaces
  - Barcode4j para generación de códigos de barras
  - POI para exportación a Excel
  - iText para manejo de PDF

## Requisitos del Sistema

- **Java**: JDK 8 o superior
- **Base de Datos**: MySQL 8.0 o superior
- **Memoria RAM**: 4GB mínimo recomendado
- **Espacio en Disco**: 500MB para la aplicación
- **Sistema Operativo**: Windows, Linux o macOS con soporte para Java

## Instalación

### Preparación del Entorno

1. **Instalar Java**:

   - Descarga e instala [Java JDK 8 o superior](https://www.oracle.com/java/technologies/javase-downloads.html)
   - Configura las variables de entorno JAVA_HOME y PATH

2. **Instalar MySQL**:
   - Descarga e instala [MySQL 8.0 o superior](https://dev.mysql.com/downloads/mysql/)
   - Crea un usuario con permisos adecuados para la aplicación

### Configuración del Proyecto

1. **Clonar el Repositorio**:

   ```bash
   git clone https://github.com/tu-usuario/GlobalTennis.git
   cd GlobalTennis
   ```

2. **Configurar la Base de Datos**:

   - Importa los scripts SQL desde la carpeta `SQL/` para crear las tablas necesarias:
     ```bash
     mysql -u tu_usuario -p tu_base_de_datos < SQL/bodega_zapatos.sql
     ```
   - Configura los parámetros de conexión en la clase `conexion.java` ubicada en `src/raven/controlador/principal/`

3. **Compilar el Proyecto**:

   - **Con NetBeans**:

     - Abre NetBeans IDE
     - Selecciona "Abrir Proyecto" y navega hasta la carpeta de GlobalTennis
     - Compila el proyecto con la opción "Construir Proyecto"

   - **Con Línea de Comandos**:
     ```bash
     ant compile
     ant jar
     ```

## 🏗️ Estructura del Proyecto

```text
software_core_tenis/
├── 📁 java_project/                 # Aplicación Desktop Java
│   ├── src/raven/                   # Código fuente principal
│   ├── lib/                        # Dependencias Java
│   ├── nbproject/                  # Configuración NetBeans
│   ├── build.xml                   # Script de构建 Ant
│   └── README.md                   # Documentación específica
├── 📁 android_project/              # Proyectos Móviles
│   ├── inventory_app/              # Aplicación Flutter
│   │   ├── lib/                    # Código Dart
│   │   ├── android/                 # Configuración Android
│   │   └── pubspec.yaml            # Dependencias Flutter
│   ├── inventory_api/              # API Node.js
│   │   ├── server.js               # Servidor principal
│   │   ├── package.json            # Dependencias Node.js
│   │   └── node_modules/           # Módulos instalados
│   └── README.md                   # Documentación móvil
├── 📁 SQL/                          # Scripts de base de datos
├── 📁 vista_nueva/                  # Interfaces web adicionales
├── 📁 updates/                      # Actualizaciones SQL
├── .gitignore                      # Archivos ignorados por Git
├── README.md                       # Este archivo
└── README_SEPARACION.md            # Documentación de separación
```

## Uso de la Aplicación

### Inicio de Sesión

Al iniciar la aplicación, se presenta la pantalla de inicio de sesión donde deberás ingresar tus credenciales:

1. Ingresa tu nombre de usuario
2. Ingresa tu contraseña
3. Haz clic en "Iniciar Sesión"

### Módulos Principales

- **Dashboard**: Muestra información general y estadísticas de ventas
- **Ventas**: Permite registrar nuevas ventas y consultar el historial
- **Productos**: Gestión completa del catálogo de productos
- **Clientes**: Administración de la información de clientes
- **Proveedores**: Gestión de proveedores y compras
- **Reportes**: Generación de informes y análisis
- **Administración**: Configuración del sistema y gestión de usuarios

## Base de Datos

El sistema utiliza una base de datos MySQL con las siguientes tablas principales:

- **productos**: Catálogo de productos
- **categorias**: Categorías de productos
- **marcas**: Marcas de productos
- **inventario_movimientos**: Registro de movimientos de inventario
- **clientes**: Información de clientes
- **proveedores**: Información de proveedores
- **ventas**: Registro de ventas
- **venta_detalles**: Detalles de cada venta
- **usuarios**: Usuarios del sistema
- **cajas**: Registro de cajas
- **caja_movimientos**: Movimientos de efectivo en cajas

## Solución de Problemas

### Problemas de Conexión a la Base de Datos

- Verifica que el servidor MySQL esté en ejecución
- Comprueba que los datos de conexión en `conexion.java` sean correctos
- Asegúrate de que el usuario tenga permisos suficientes

### Errores de Compilación

- Verifica que todas las dependencias en la carpeta `lib/` estén correctamente referenciadas
- Asegúrate de usar la versión correcta de Java (JDK 8 o superior)

### Problemas con los Reportes

- Comprueba que las plantillas de reportes en `src/raven/reportes/` estén accesibles
- Verifica que JasperReports esté correctamente configurado

## Contribuciones

Si deseas contribuir al proyecto, por favor:

1. Haz un fork del repositorio
2. Crea una rama para tu funcionalidad (`git checkout -b nueva-funcionalidad`)
3. Realiza tus cambios y haz commit (`git commit -am 'Añadir nueva funcionalidad'`)
4. Sube los cambios a tu fork (`git push origin nueva-funcionalidad`)
5. Crea un Pull Request

## Licencia

Este proyecto está licenciado bajo [Licencia MIT](LICENSE) - ver el archivo LICENSE para más detalles.

## Contacto

Para soporte o consultas, contacta a:

- Email: lmog240@gmail.com
- Web: [pagina web](https://lmog.netlify.app/)

---

© 2025 GlobalTennis. Todos los derechos reservados.
