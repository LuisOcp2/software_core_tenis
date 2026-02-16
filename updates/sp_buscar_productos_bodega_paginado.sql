DELIMITER //

DROP PROCEDURE IF EXISTS `sp_buscar_productos_bodega_paginado` //

CREATE PROCEDURE `sp_buscar_productos_bodega_paginado` (
    IN `p_id_bodega` INT, 
    IN `p_texto` VARCHAR(100), 
    IN `p_tipo` VARCHAR(10),
    IN `p_id_categoria` INT,
    IN `p_id_marca` INT,
    IN `p_id_color` INT,
    IN `p_id_talla` INT,
    IN `p_limit` INT,
    IN `p_offset` INT,
    OUT `p_total_rows` INT
)
BEGIN
    DECLARE v_pat VARCHAR(110);
    SET v_pat = CONCAT('%', COALESCE(p_texto,''), '%');

    -- 1. Calcular total de filas (para paginación)
    SELECT COUNT(DISTINCT p.id_producto)
      INTO p_total_rows
    FROM inventario_bodega ib
    INNER JOIN producto_variantes pv ON pv.id_variante = ib.id_variante
    INNER JOIN productos p           ON p.id_producto = pv.id_producto
    LEFT JOIN marcas m               ON p.id_marca = m.id_marca
    WHERE ib.id_bodega = p_id_bodega
      AND ib.activo = 1
      AND p.activo = 1
      AND pv.disponible = 1
      AND (p_id_categoria IS NULL OR p.id_categoria = p_id_categoria)
      AND (p_id_marca IS NULL OR p.id_marca = p_id_marca)
      AND (p_id_color IS NULL OR pv.id_color = p_id_color)
      AND (p_id_talla IS NULL OR pv.id_talla = p_id_talla)
      AND (
        (p_tipo IS NULL OR p_tipo = '') OR
        (LOWER(p_tipo) IN ('par', 'pares')  AND COALESCE(ib.Stock_par,0)  > 0) OR
        (LOWER(p_tipo) IN ('caja', 'cajas') AND COALESCE(ib.Stock_caja,0) > 0)
      )
      AND (COALESCE(ib.Stock_par,0) > 0 OR COALESCE(ib.Stock_caja,0) > 0)
      AND (
          p_texto IS NULL OR p_texto = '' OR
          p.nombre         LIKE v_pat OR
          p.codigo_modelo  LIKE v_pat OR
          m.nombre         LIKE v_pat OR
          COALESCE(pv.ean,'') LIKE v_pat OR
          COALESCE(pv.sku,'') LIKE v_pat
      );

    -- 2. Devolver resultados paginados
    IF p_total_rows > 0 THEN
        SELECT
            p.id_producto,
            p.codigo_modelo,
            p.nombre,
            p.genero,
            p.descripcion,
            p.precio_compra,
            p.precio_venta,
            p.stock_minimo,
            p.ubicacion,
            p.pares_por_caja,
            p.activo,
            p.id_categoria,
            p.id_marca,
            p.id_proveedor,
            c.nombre AS categoria_nombre,
            m.nombre AS marca_nombre,
            pr.nombre AS proveedor_nombre,
            
            -- Subconsultas para evitar explosión de JOINs en GROUP_CONCAT
            (SELECT GROUP_CONCAT(DISTINCT col.nombre ORDER BY col.nombre SEPARATOR ', ') 
             FROM producto_variantes pv2 
             JOIN colores col ON pv2.id_color = col.id_color 
             WHERE pv2.id_producto = p.id_producto AND pv2.disponible = 1) AS colores,
             
            (SELECT GROUP_CONCAT(DISTINCT t.numero ORDER BY CAST(t.numero AS UNSIGNED) SEPARATOR ', ') 
             FROM producto_variantes pv3 
             JOIN tallas t ON pv3.id_talla = t.id_talla 
             WHERE pv3.id_producto = p.id_producto AND pv3.disponible = 1) AS tallas,

            p_id_bodega AS id_bodega,
            SUM(COALESCE(ib.Stock_par, 0))   AS total_pares,
            SUM(COALESCE(ib.Stock_caja, 0))  AS total_cajas,
            
            -- Cálculo de relevancia
            CASE
                WHEN p_texto IS NOT NULL AND p_texto <> '' AND (
                    p.nombre = p_texto OR
                    p.codigo_modelo = p_texto OR
                    COALESCE(pv.ean,'') = p_texto OR
                    COALESCE(pv.sku,'') = p_texto
                ) THEN 0
                WHEN p_texto IS NOT NULL AND p_texto <> '' AND (
                    p.nombre LIKE v_pat OR
                    p.codigo_modelo LIKE v_pat OR
                    COALESCE(pv.ean,'') LIKE v_pat OR
                    COALESCE(pv.sku,'') LIKE v_pat
                ) THEN 1
                ELSE 2
            END AS relevancia

        FROM inventario_bodega ib
        INNER JOIN producto_variantes pv ON pv.id_variante = ib.id_variante
        INNER JOIN productos p           ON p.id_producto = pv.id_producto
        LEFT JOIN categorias c           ON p.id_categoria = c.id_categoria
        LEFT JOIN marcas m               ON p.id_marca = m.id_marca
        LEFT JOIN proveedores pr         ON p.id_proveedor = pr.id_proveedor
        
        WHERE ib.id_bodega = p_id_bodega
          AND ib.activo = 1
          AND p.activo = 1
          AND pv.disponible = 1
          AND (p_id_categoria IS NULL OR p.id_categoria = p_id_categoria)
          AND (p_id_marca IS NULL OR p.id_marca = p_id_marca)
          AND (p_id_color IS NULL OR pv.id_color = p_id_color)
          AND (p_id_talla IS NULL OR pv.id_talla = p_id_talla)
          AND (
            (p_tipo IS NULL OR p_tipo = '') OR
            (LOWER(p_tipo) IN ('par', 'pares')  AND COALESCE(ib.Stock_par,0)  > 0) OR
            (LOWER(p_tipo) IN ('caja', 'cajas') AND COALESCE(ib.Stock_caja,0) > 0)
          )
          AND (COALESCE(ib.Stock_par,0) > 0 OR COALESCE(ib.Stock_caja,0) > 0)
          AND (
              p_texto IS NULL OR p_texto = '' OR
              p.nombre         LIKE v_pat OR
              p.codigo_modelo  LIKE v_pat OR
              m.nombre         LIKE v_pat OR
              COALESCE(pv.ean,'') LIKE v_pat OR
              COALESCE(pv.sku,'') LIKE v_pat
          )
        GROUP BY p.id_producto
        ORDER BY relevancia, p.nombre ASC
        LIMIT p_limit OFFSET p_offset;
    END IF;
END //

DELIMITER ;