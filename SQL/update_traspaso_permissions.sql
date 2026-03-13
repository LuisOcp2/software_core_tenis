-- Script para insertar permisos del módulo de Traspasos
-- Se utiliza INSERT IGNORE o comprobación manual para evitar duplicados si ya existen

INSERT INTO permisos (modulo, nombre_mostrar, descripcion, categoria, requiere_admin, activo)
SELECT * FROM (SELECT 'crear_traspaso', 'Crear Traspaso', 'Permite crear nuevas solicitudes de traspaso', 'Traspasos', 0, 1) AS tmp
WHERE NOT EXISTS (
    SELECT modulo FROM permisos WHERE modulo = 'crear_traspaso'
) LIMIT 1;

INSERT INTO permisos (modulo, nombre_mostrar, descripcion, categoria, requiere_admin, activo)
SELECT * FROM (SELECT 'autorizar_traspaso', 'Autorizar Traspaso', 'Permite autorizar traspasos de salida', 'Traspasos', 0, 1) AS tmp
WHERE NOT EXISTS (
    SELECT modulo FROM permisos WHERE modulo = 'autorizar_traspaso'
) LIMIT 1;

INSERT INTO permisos (modulo, nombre_mostrar, descripcion, categoria, requiere_admin, activo)
SELECT * FROM (SELECT 'enviar_traspaso', 'Enviar Traspaso', 'Permite marcar traspasos como enviados (despachados)', 'Traspasos', 0, 1) AS tmp
WHERE NOT EXISTS (
    SELECT modulo FROM permisos WHERE modulo = 'enviar_traspaso'
) LIMIT 1;

INSERT INTO permisos (modulo, nombre_mostrar, descripcion, categoria, requiere_admin, activo)
SELECT * FROM (SELECT 'recibir_traspaso', 'Recibir Traspaso', 'Permite recibir traspasos en la bodega de destino', 'Traspasos', 0, 1) AS tmp
WHERE NOT EXISTS (
    SELECT modulo FROM permisos WHERE modulo = 'recibir_traspaso'
) LIMIT 1;

INSERT INTO permisos (modulo, nombre_mostrar, descripcion, categoria, requiere_admin, activo)
SELECT * FROM (SELECT 'cancelar_traspaso', 'Cancelar Traspaso', 'Permite cancelar solicitudes de traspaso', 'Traspasos', 0, 1) AS tmp
WHERE NOT EXISTS (
    SELECT modulo FROM permisos WHERE modulo = 'cancelar_traspaso'
) LIMIT 1;

INSERT INTO permisos (modulo, nombre_mostrar, descripcion, categoria, requiere_admin, activo)
SELECT * FROM (SELECT 'editar_traspaso', 'Editar Traspaso', 'Permite editar solicitudes pendientes', 'Traspasos', 0, 1) AS tmp
WHERE NOT EXISTS (
    SELECT modulo FROM permisos WHERE modulo = 'editar_traspaso'
) LIMIT 1;

INSERT INTO permisos (modulo, nombre_mostrar, descripcion, categoria, requiere_admin, activo)
SELECT * FROM (SELECT 'admin_traspasos', 'Administrar Traspasos', 'Acceso total a traspasos sin restricciones de bodega', 'Traspasos', 1, 1) AS tmp
WHERE NOT EXISTS (
    SELECT modulo FROM permisos WHERE modulo = 'admin_traspasos'
) LIMIT 1;

-- Asignar permisos al rol Administrador (ID 1 asumo, o por nombre)
-- Esto es opcional, depende de si queremos auto-asignar
-- INSERT INTO privilegios_rol (id_rol, id_permiso, ver, crear, editar, eliminar)
-- SELECT r.id_rol, p.id_permiso, 1, 1, 1, 1
-- FROM roles r, permisos p
-- WHERE r.nombre = 'Administrador' AND p.categoria = 'Traspasos'
-- AND NOT EXISTS (SELECT 1 FROM privilegios_rol pr WHERE pr.id_rol = r.id_rol AND pr.id_permiso = p.id_permiso);
