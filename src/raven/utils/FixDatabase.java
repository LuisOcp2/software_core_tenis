package raven.utils;

import raven.controlador.principal.conexion;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class FixDatabase {

    public static void main(String[] args) {
        System.out.println("Iniciando reparación de base de datos (Intento 2 - Corrección de Sintaxis)...");
        try {
            crearProcedimientoAlmacenado();
            System.out.println("¡ÉXITO! Base de datos actualizada correctamente.");
            System.out.println("Ahora puede ejecutar la aplicación.");
        } catch (Exception e) {
            System.err.println("ERROR actualizando base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void crearProcedimientoAlmacenado() throws SQLException {
        Connection con = conexion.getInstance().createConnection();
        Statement stmt = con.createStatement();
        
        try {
            // 1. Eliminar procedimiento anterior si existe
            System.out.println("1. Eliminando versión anterior del procedimiento...");
            stmt.execute("DROP PROCEDURE IF EXISTS sp_buscar_productos_bodega_paginado");
            
            // 2. Crear el procedimiento
            System.out.println("2. Creando procedimiento sp_buscar_productos_bodega_paginado con sintaxis corregida...");
            
            // NOTA: Para las comillas simples dentro de un string SQL delimitado por comillas simples,
            // debemos usar dos comillas simples ('') para escapar.
            // Ejemplo: SEPARATOR '', '' se convierte en SEPARATOR ', ' dentro del string.
            
            String sql = "CREATE PROCEDURE sp_buscar_productos_bodega_paginado( " +
                    "IN p_id_bodega INT, " +
                    "IN p_texto VARCHAR(100), " +
                    "IN p_tipo VARCHAR(10), " +
                    "IN p_categoria INT, " +
                    "IN p_marca INT, " +
                    "IN p_color INT, " +
                    "IN p_talla INT, " +
                    "IN p_limit INT, " +
                    "IN p_offset INT, " +
                    "OUT p_total_rows INT " +
                    ") " +
                    "BEGIN " +
                    "    SET @sql_base = 'FROM productos p " +
                    "        INNER JOIN categorias c ON p.id_categoria = c.id_categoria " +
                    "        INNER JOIN marcas m ON p.id_marca = m.id_marca " +
                    "        LEFT JOIN producto_variantes pv ON pv.id_producto = p.id_producto AND pv.disponible = 1 " +
                    "        LEFT JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante AND ib.activo = 1'; " +
                    "    SET @sql_where = ' WHERE p.activo = 1'; " +
                    "    IF p_id_bodega IS NOT NULL AND p_id_bodega > 0 THEN " +
                    "        SET @sql_where = CONCAT(@sql_where, ' AND ib.id_bodega = ', p_id_bodega); " +
                    "    END IF; " +
                    "    IF p_texto IS NOT NULL AND p_texto != '' THEN " +
                    "        SET @sql_where = CONCAT(@sql_where, ' AND (p.nombre LIKE \"%', p_texto, '%\" OR p.codigo_modelo LIKE \"%', p_texto, '%\" OR c.nombre LIKE \"%', p_texto, '%\" OR m.nombre LIKE \"%', p_texto, '%\" OR pv.sku LIKE \"%', p_texto, '%\" OR pv.ean LIKE \"%', p_texto, '%\" OR pv.codigo_barras LIKE \"%', p_texto, '%\" )'); " +
                    "    END IF; " +
                    "    IF p_categoria IS NOT NULL AND p_categoria > 0 THEN " +
                    "        SET @sql_where = CONCAT(@sql_where, ' AND p.id_categoria = ', p_categoria); " +
                    "    END IF; " +
                    "    IF p_marca IS NOT NULL AND p_marca > 0 THEN " +
                    "        SET @sql_where = CONCAT(@sql_where, ' AND p.id_marca = ', p_marca); " +
                    "    END IF; " +
                    "    IF p_color IS NOT NULL AND p_color > 0 THEN " +
                    "        SET @sql_where = CONCAT(@sql_where, ' AND pv.id_color = ', p_color); " +
                    "    END IF; " +
                    "    IF p_talla IS NOT NULL AND p_talla > 0 THEN " +
                    "        SET @sql_where = CONCAT(@sql_where, ' AND pv.id_talla = ', p_talla); " +
                    "    END IF; " +
                    "    IF p_tipo IS NOT NULL AND p_tipo = 'BAJO' THEN " +
                    "        SET @sql_where = CONCAT(@sql_where, ' AND ib.Stock_par <= p.stock_minimo'); " +
                    "    ELSEIF p_tipo = 'AGOTADO' THEN " +
                    "        SET @sql_where = CONCAT(@sql_where, ' AND (ib.Stock_par <= 0 OR ib.Stock_par IS NULL)'); " +
                    "    END IF; " +
                    "    SET @sql_count = CONCAT('SELECT COUNT(DISTINCT p.id_producto) INTO @total ', @sql_base, @sql_where); " +
                    "    PREPARE stmt_count FROM @sql_count; " +
                    "    EXECUTE stmt_count; " +
                    "    DEALLOCATE PREPARE stmt_count; " +
                    "    SET p_total_rows = @total; " +
                    "    SET @sql_query = CONCAT('SELECT " +
                    "        p.id_producto, p.codigo_modelo, p.nombre, p.descripcion, " +
                    "        p.precio_compra, p.precio_venta, p.stock_minimo, p.genero, " +
                    "        p.ubicacion, p.pares_por_caja, p.activo, " +
                    "        c.nombre AS categoria_nombre, m.nombre AS marca_nombre, " +
                    "        c.id_categoria, m.id_marca, " +
                    "        (SELECT nombre FROM proveedores WHERE id_proveedor = (SELECT id_proveedor FROM producto_variantes WHERE id_producto = p.id_producto LIMIT 1) LIMIT 1) as proveedor_nombre, " +
                    "        (SELECT id_proveedor FROM producto_variantes WHERE id_producto = p.id_producto LIMIT 1) as id_proveedor, " +
                    "        (SELECT GROUP_CONCAT(DISTINCT col.nombre ORDER BY col.nombre SEPARATOR '', '') FROM producto_variantes pv2 JOIN colores col ON pv2.id_color = col.id_color WHERE pv2.id_producto = p.id_producto AND pv2.disponible = 1) AS colores, " +
                    "        (SELECT GROUP_CONCAT(DISTINCT t.numero ORDER BY CAST(t.numero AS UNSIGNED) SEPARATOR '', '') FROM producto_variantes pv3 JOIN tallas t ON pv3.id_talla = t.id_talla WHERE pv3.id_producto = p.id_producto AND pv3.disponible = 1) AS tallas, " +
                    "        COALESCE(SUM(ib.Stock_par), 0) AS total_pares, " +
                    "        COALESCE(SUM(ib.Stock_caja), 0) AS total_cajas " +
                    "        ', @sql_base, @sql_where, " +
                    "        ' GROUP BY p.id_producto " +
                    "          ORDER BY p.nombre ASC " +
                    "          LIMIT ', p_limit, ' OFFSET ', p_offset); " +
                    "    PREPARE stmt_query FROM @sql_query; " +
                    "    EXECUTE stmt_query; " +
                    "    DEALLOCATE PREPARE stmt_query; " +
                    "END";
            
            stmt.execute(sql);
            
        } finally {
            try { stmt.close(); } catch (Exception ignore) {}
            try { con.close(); } catch (Exception ignore) {}
        }
    }
}
