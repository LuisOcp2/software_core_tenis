package raven.utils;

import java.sql.Connection;
import java.sql.Statement;
import raven.controlador.principal.conexion;

public class DBMigration {

    public static void main(String[] args) {
        updateStoredProcedure();
    }

    public static void updateStoredProcedure() {
        System.out.println("Iniciando actualización de procedimiento almacenado sp_listar_variantes_producto...");
        
        String dropSql = "DROP PROCEDURE IF EXISTS `sp_listar_variantes_producto`";
        
        String createSql = "CREATE PROCEDURE `sp_listar_variantes_producto`(\n" +
"    IN p_id_producto INT,\n" +
"    IN p_id_bodega INT\n" +
")\n" +
"BEGIN\n" +
"    SELECT \n" +
"        pv.id_variante,\n" +
"        pv.id_producto,\n" +
"        t.numero,\n" +
"        t.sistema,\n" +
"        t.genero,\n" +
"        c.nombre AS color,\n" +
"        pv.sku,\n" +
"        pv.ean,\n" +
"        pv.precio_compra,\n" +
"        pv.precio_venta,\n" +
"        pv.stock_minimo_variante,\n" +
"        pv.disponible,\n" +
"        pv.imagen,\n" +
"        pv.id_proveedor,\n" +
"        pr.nombre AS proveedor_nombre,\n" +
"        COALESCE(ib.id_bodega, p_id_bodega) AS id_bodega,\n" +
"        b.nombre AS bodega_nombre,\n" +
"        COALESCE(ib.Stock_par, 0) AS stock_par,\n" +
"        COALESCE(ib.Stock_caja, 0) AS stock_caja,\n" +
"        COALESCE(ib.id_inventario_bodega, 0) AS id_inventario_bodega\n" +
"    FROM producto_variantes pv\n" +
"    LEFT JOIN tallas t ON pv.id_talla = t.id_talla\n" +
"    LEFT JOIN colores c ON pv.id_color = c.id_color\n" +
"    LEFT JOIN proveedores pr ON pv.id_proveedor = pr.id_proveedor\n" +
"    LEFT JOIN inventario_bodega ib ON pv.id_variante = ib.id_variante \n" +
"         AND (p_id_bodega IS NULL OR ib.id_bodega = p_id_bodega)\n" +
"         AND ib.activo = 1\n" +
"    LEFT JOIN bodegas b ON ib.id_bodega = b.id_bodega\n" +
"    WHERE pv.id_producto = p_id_producto\n" +
"      AND pv.disponible = 1\n" +
"    ORDER BY CAST(t.numero AS UNSIGNED), c.nombre;\n" +
"END";

        try (Connection conn = conexion.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(dropSql);
            System.out.println("Procedimiento eliminado (si existía).");
            
            stmt.execute(createSql);
            System.out.println("Procedimiento sp_listar_variantes_producto creado exitosamente.");
            
        } catch (Exception e) {
            System.err.println("Error actualizando procedimiento: " + e.getMessage());
            e.printStackTrace();
        }
    }
}