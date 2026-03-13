# Contributing to Software Core Tenis 🎾

¡Gracias por tu interés en contribuir a Software Core Tenis! Este documento te guiará sobre cómo puedes contribuir al proyecto.

## 🤝 Cómo Contribuir

### Reportando Issues 🐛

Si encuentras un bug o tienes una sugerencia:

1. **Busca issues existentes** antes de crear uno nuevo
2. **Usa plantillas apropiadas** para bugs o feature requests
3. **Proporciona información detallada**:
   - Pasos para reproducir el problema
   - Comportamiento esperado vs actual
   - Capturas de pantalla si es aplicable
   - Entorno (SO, versión de Java/Flutter/Node.js)

### Desarrollo de Código 💻

#### 1. Prepara tu Entorno

```bash
# Clona el repositorio
git clone https://github.com/LuisOcp2/software_core_tenis.git
cd software_core_tenis

# Configura tu rama
git checkout -b feature/tu-nueva-funcionalidad
```

#### 2. Instala Dependencias

**Para Java Desktop:**
```bash
cd java_project
# Asegúrate de tener JDK 8+ y MySQL configurados
# Las dependencias están en la carpeta lib/
```

**Para App Móvil Flutter:**
```bash
cd android_project/inventory_app
flutter pub get
```

**Para API Node.js:**
```bash
cd android_project/inventory_api
npm install
```

#### 3. Estándares de Código

**Java:**
- Sigue las convenciones de código de Oracle
- Usa nombres descriptivos en español o inglés
- Comenta clases y métodos complejos
- Formatea con el estilo de NetBeans o IntelliJ

**Dart/Flutter:**
- Sigue las [Flutter Style Guidelines](https://flutter.dev/docs/development/tools/formatting)
- Usa `dart format` para formatear el código
- Nombra widgets y variables en inglés

**Node.js:**
- Usa ES6+ features
- Sigue [StandardJS](https://standardjs.com/) guidelines
- Usa `npm run lint` para verificar el estilo

#### 4. Proceso de Pull Request

1. **Haz fork del repositorio**
2. **Crea una rama descriptiva:**
   - `feature/nueva-funcionalidad`
   - `fix/correction-bug-importante`
   - `docs/actualizar-documentacion`

3. **Realiza tus cambios:**
   - Haz commits atómicos y descriptivos
   - Usa mensajes de commit convencionales:
     ```
     feat: añadir módulo de reportes
     fix: corregir error de conexión a base de datos
     docs: actualizar README de instalación
     ```

4. **Prueba tus cambios:**
   - Java: Ejecuta pruebas unitarias si existen
   - Flutter: `flutter test`
   - Node.js: `npm test`

5. **Crea el Pull Request:**
   - Usa la plantilla de PR
   - Describe claramente los cambios
   - Incluye capturas de pantalla si aplica
   - Menciona los issues relacionados

## 📋 Tipos de Contribuciones

### 🐛 Bug Reports
Reporta errores con información detallada del entorno y pasos para reproducir.

### ✨ Features
Sugiere nuevas funcionalidades explicando el caso de uso y beneficios.

### 📚 Documentación
Mejora la documentación, tutoriales o ejemplos de código.

### 🎨 UI/UX
Mejoras en la interfaz de usuario y experiencia de usuario.

### ⚡ Performance
Optimizaciones de rendimiento en cualquier componente.

### 🔧 Testing
Añade pruebas unitarias, de integración o end-to-end.

## 🏷️ Etiquetas de Issues

- `bug`: Errores reportados
- `enhancement`: Mejoras propuestas
- `documentation`: Issues de documentación
- `good first issue`: Bueno para principiantes
- `help wanted`: Se necesita ayuda
- `priority: high`: Alta prioridad
- `priority: medium`: Prioridad media
- `priority: low`: Baja prioridad

## 📝 Guía de Commits

Usa el formato [Conventional Commits](https://www.conventionalcommits.org/):

```
<tipo>[alcance opcional]: <descripción>

[opcional cuerpo]

[opcional pie de página]
```

**Tipos comunes:**
- `feat`: Nueva funcionalidad
- `fix`: Corrección de bug
- `docs`: Cambios en documentación
- `style`: Cambios de formato (no afectan lógica)
- `refactor`: Cambios de código que no añaden funcionalidad
- `test`: Añadir o modificar pruebas
- `chore`: Cambios de mantenimiento

## 🚀 Proceso de Release

1. **Development** en rama `develop`
2. **Testing** y revisión de código
3. **Merge** a `main` para releases
4. **Versionamiento** semántico (MAJOR.MINOR.PATCH)

## 📞 Contacto

Para dudas sobre contribuciones:

- **Issues**: Usa GitHub Issues para preguntas técnicas
- **Discussions**: Para debates generales
- **Email**: lmog240@gmail.com

## 📜 Código de Conducta

Por favor, sé respetuoso y constructivo en todas tus interacciones. Sigamos estos principios:

- **Ser inclusivos** y respetuosos
- **Ser constructivos** en feedback
- **Ser colaborativos** y serviciales
- **Mantener profesionalismo** siempre

---

¡Gracias por contribuir a hacer Software Core Tenis mejor! 🎾✨
