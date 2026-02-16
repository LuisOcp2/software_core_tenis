DROP TRIGGER IF EXISTS trg_pv_insert_ib;
DELIMITER $$
CREATE TRIGGER trg_pv_insert_ib AFTER INSERT ON producto_variantes
FOR EACH ROW
BEGIN
  INSERT INTO inventario_bodega (id_bodega, id_variante, Stock_par, Stock_caja, stock_reservado, fecha_ultimo_movimiento, ubicacion_especifica, activo)
  SELECT b.id_bodega, NEW.id_variante, 0, 0, 0, NOW(), NULL, 1
  FROM bodegas b
  WHERE b.activa = 1;
END$$
DELIMITER ;
