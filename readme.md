# GlobalTennis

GlobalTennis es una aplicación de gestión de ventas y control de inventario para una tienda de artículos de tenis. Este proyecto está desarrollado en **Java** y utiliza herramientas como **JasperReports** para la generación de reportes y **MySQL** para la gestión de datos.

![Logo GlobalTennis](src/raven/icon/png/logo.png)

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

## Estructura del Proyecto

```
GlobalTennis/
├── build/                  # Archivos compilados
├── Fonts/                  # Fuentes utilizadas en la aplicación
├── lib/                    # Bibliotecas y dependencias
├── nbproject/              # Configuración de NetBeans
├── SQL/                    # Scripts SQL para la base de datos
└── src/                    # Código fuente
    └── raven/
        ├── application/    # Formularios y pantallas principales
        ├── clases/         # Lógica de negocio
        ├── componentes/    # Componentes personalizados
        ├── controlador/    # Controladores y modelos
        ├── icon/           # Iconos y recursos gráficos
        ├── menu/           # Componentes del menú
        ├── reportes/       # Plantillas de reportes
        ├── theme/          # Configuración de temas
        └── utils/          # Utilidades generales
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
