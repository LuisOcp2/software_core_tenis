package raven.utils;

import java.sql.Connection;
import java.sql.Statement;
import raven.controlador.principal.conexion;

public class DBMigrationVariantes {
    
    public static void migrate() {
        System.out.println("Iniciando migración de procedimiento sp_listar_variantes_producto...");
        
        String dropSql = "DROP PROCEDURE IF EXISTS `sp_listar_variantes_producto`";
        
        String createSql = "CREATE PROCEDURE `sp_listar_variantes_producto`(\n" +
                           "    IN p_id_producto INT,\n" +
                           "    IN p_id_bodega INT\n" +
                           ")\n" +
                           "BEGIN\n" +
                           "    SELECT \n" +
                           "        pv.id_variante, \n" +
                           "        ib.id_bodega, \n" +
                           "        ib.id_inventario_bodega, \n" +
                           "        COALESCE(ib.Stock_par, 0) AS stock_par, \n" +
                           "        COALESCE(ib.Stock_caja, 0) AS stock_caja, \n" +
                           "        pv.imagen, \n" +
                           "        pv.id_proveedor, \n" +
                           "        prov.nombre AS proveedor_nombre, \n" +
                           "        t.numero, \n" +
                           "        t.sistema, \n" +
                           "        t.genero, \n" +
                           "        c.nombre AS color, \n" +
                           "        b.nombre AS bodega_nombre \n" +
                           "    FROM producto_variantes pv \n" +
                           "    LEFT JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante AND ib.activo = 1 \n" +
                           "        AND (p_id_bodega IS NULL OR p_id_bodega <= 0 OR ib.id_bodega = p_id_bodega)\n" +
                           "    LEFT JOIN tallas t ON pv.id_talla = t.id_talla \n" +
                           "    LEFT JOIN colores c ON pv.id_color = c.id_color \n" +
                           "    LEFT JOIN bodegas b ON ib.id_bodega = b.id_bodega \n" +
                           "    LEFT JOIN proveedores prov ON pv.id_proveedor = prov.id_proveedor \n" +
                           "    WHERE pv.id_producto = p_id_producto \n" +
                           "      AND pv.disponible = 1 \n" +
                           "    ORDER BY \n" +
                           "        b.nombre ASC, \n" +
                           "        prov.nombre ASC, \n" +
                           "        CAST(REGEXP_REPLACE(t.numero, '[^0-9.]', '') AS DECIMAL(10,2)) ASC,\n" +
                           "        c.nombre ASC;\n" +
                           "END";

        try (Connection con = conexion.getInstance().createConnection();
             Statement stmt = con.createStatement()) {
            
            // Ejecutar DROP
            stmt.execute(dropSql);
            System.out.println("Procedimiento anterior eliminado (si existía).");
            
            // Ejecutar CREATE
            stmt.execute(createSql);
            System.out.println("Procedimiento sp_listar_variantes_producto creado exitosamente.");
            
        } catch (Exception e) {
            System.err.println("Error durante la migración: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        migrate();
    }
}
