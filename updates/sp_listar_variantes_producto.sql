DROP PROCEDURE IF EXISTS sp_listar_variantes_producto;
$$
CREATE PROCEDURE sp_listar_variantes_producto(
    IN p_id_producto INT,
    IN p_id_bodega INT
)
BEGIN
    SELECT 
        pv.id_variante,
        pv.id_producto,
        pv.id_talla,
        pv.id_color,
        pv.sku,
        pv.ean,
        pv.precio_compra,
        pv.precio_venta,
        pv.stock_minimo_variante,
        pv.disponible,
        t.numero,
        t.sistema,
        t.genero,
        col.nombre AS color,
        col.codigo_hex,
        ib.id_inventario_bodega,
        COALESCE(ib.id_bodega, p_id_bodega) as id_bodega,
        b.nombre as bodega_nombre,
        COALESCE(ib.Stock_par, 0) AS stock_par,
        COALESCE(ib.Stock_caja, 0) AS stock_caja,
        ib.ubicacion_especifica AS ubicacion_especifica,
        pv.imagen,
        pv.id_proveedor,
        pr.nombre as proveedor_nombre
    FROM producto_variantes pv
    LEFT JOIN tallas t ON pv.id_talla = t.id_talla
    LEFT JOIN colores col ON pv.id_color = col.id_color
    LEFT JOIN inventario_bodega ib ON pv.id_variante = ib.id_variante 
        AND (p_id_bodega IS NULL OR ib.id_bodega = p_id_bodega) AND ib.activo = 1
    LEFT JOIN bodegas b ON ib.id_bodega = b.id_bodega
    LEFT JOIN proveedores pr ON pv.id_proveedor = pr.id_proveedor
    WHERE pv.id_producto = p_id_producto
    ORDER BY CAST(t.numero AS UNSIGNED), col.nombre;
END
$$
