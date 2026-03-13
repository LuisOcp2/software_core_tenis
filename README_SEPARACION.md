# Separación de Proyectos

## Estructura Actual

El proyecto ha sido separado en dos carpetas principales:

### 📁 java_project/
Aplicación desktop desarrollada en Java:
- **src/raven/**: Código fuente completo de la aplicación Java
- **build.xml**: Script de构建 Ant
- **manifest.mf**: Manifiesto de la aplicación
- **lib/**: Dependencias Java
- **nbproject/**: Configuración NetBeans
- **out/**: Archivos compilados
- **test/**: Pruebas unitarias

### 📁 android_project/
Aplicación móvil y API:
- **inventory_app/**: Aplicación Flutter completa
- **inventory_api/**: API Node.js para la app móvil

## Componentes Compartidos

Estos elementos permanecen en la raíz ya que son compartidos:

- **SQL/**: Scripts de base de datos
- **vista_nueva/**: Interfaces web HTML
- **updates/**: Actualizaciones SQL
- **.git/**: Control de versiones
- **crear_instalador.bat**: Script de instalación
- **setup.iss**: Configuración Inno Setup
- **readme.md**: Documentación general

## Próximos Pasos

1. **Java Project**: Configurar rutas de dependencias si es necesario
2. **Android Project**: Verificar configuración de Flutter y API
3. **Base de datos**: Asegurar que ambos proyectos apunten a la misma BD

## Beneficios de la Separación

- ✅ Mantenimiento independiente
- ✅ Despliegue separado
- ✅ Dependencias específicas por proyecto
- ✅ Mayor claridad en la estructura
