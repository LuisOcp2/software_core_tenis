-- =================================================================
-- SCRIPT DE ACTUALIZACIÓN DE PERMISOS - CONSULTA DETALLADA Y AUDITORIA
-- Ejecutar en la base de datos 'soft_xtreme'
-- =================================================================

-- 1. Insertar permiso para Consulta Detallada (separado de Inventario)
INSERT INTO `permisos` (modulo, nombre_mostrar, descripcion, categoria, requiere_admin, activo)
VALUES ('consulta_detallada', 'Consulta Detallada de Stock', 'Permite consultar el inventario detallado por bodegas', 'Productos', 0, 1)
ON DUPLICATE KEY UPDATE nombre_mostrar=nombre_mostrar;

-- 2. Insertar permiso para Ver Productos (Catálogo)
INSERT INTO `permisos` (modulo, nombre_mostrar, descripcion, categoria, requiere_admin, activo)
VALUES ('ver_productos', 'Ver Catálogo de Productos', 'Permite visualizar la lista de productos (solo lectura)', 'Productos', 0, 1)
ON DUPLICATE KEY UPDATE nombre_mostrar=nombre_mostrar;

-- 3. Asignar nuevos permisos al Rol ADMIN (id_rol = 1)
INSERT INTO `privilegios_rol` (id_rol, id_permiso, puede_ver, puede_crear, puede_editar, puede_eliminar)
SELECT 1, id_permiso, 1, 1, 1, 1 
FROM `permisos` 
WHERE modulo IN ('consulta_detallada', 'ver_productos')
AND id_permiso NOT IN (
    SELECT id_permiso FROM `privilegios_rol` WHERE id_rol = 1
);
