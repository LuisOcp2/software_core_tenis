-- =================================================================
-- SCRIPT DE ACTUALIZACIÓN DE PERMISOS (ANTIGRAVITY FINAL)
-- Ejecutar en la base de datos 'soft_xtreme' existente.
-- =================================================================

-- 1. Insertar permiso para ver todos los usuarios (Reemplaza check de rol 'gerente')
INSERT INTO `permisos` (modulo, nombre_mostrar, descripcion, categoria, requiere_admin, activo)
VALUES ('ver_todos_usuarios', 'Ver Todos los Usuarios', 'Permite ver usuarios de todas las bodegas', 'Admin', 1, 1)
ON DUPLICATE KEY UPDATE nombre_mostrar=nombre_mostrar;

-- 2. Insertar permiso para ver toda la deuda (Reemplaza check de rol 'admin' en Cuentas por Cobrar)
INSERT INTO `permisos` (modulo, nombre_mostrar, descripcion, categoria, requiere_admin, activo)
VALUES ('ver_toda_deuda', 'Ver Toda Deuda', 'Permite ver deuda de clientes de todas las bodegas', 'Comercial', 1, 1)
ON DUPLICATE KEY UPDATE nombre_mostrar=nombre_mostrar;

-- 3. Asignar estos nuevos permisos al Rol ADMIN (id_rol = 1)
-- Obtenemos los IDs insertados dinámicamente o usamos los nombres
INSERT INTO `privilegios_rol` (id_rol, id_permiso, puede_ver, puede_crear, puede_editar, puede_eliminar)
SELECT 1, id_permiso, 1, 1, 1, 1 
FROM `permisos` 
WHERE modulo IN ('ver_todos_usuarios', 'ver_toda_deuda')
AND id_permiso NOT IN (
    SELECT id_permiso FROM `privilegios_rol` WHERE id_rol = 1
);
