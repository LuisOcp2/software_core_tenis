START TRANSACTION;

-- Normalizar motores y tipos para FKs en MariaDB
ALTER TABLE inventario_bodega ENGINE=InnoDB;
ALTER TABLE producto_variantes ENGINE=InnoDB;
ALTER TABLE bodegas ENGINE=InnoDB;

-- Asegurar tipos y nullability compatibles
ALTER TABLE producto_variantes MODIFY id_variante INT(11) NOT NULL;
ALTER TABLE bodegas MODIFY id_bodega INT(11) NOT NULL;
ALTER TABLE inventario_bodega MODIFY id_variante INT(11) NOT NULL, MODIFY id_bodega INT(11) NOT NULL;

-- Crear PKs si faltan (MariaDB-compatible via information_schema)
DELIMITER $$
CREATE PROCEDURE ensure_primary_keys()
BEGIN
  DECLARE need_pk_pv INT DEFAULT 0;
  DECLARE need_pk_bo INT DEFAULT 0;
  SELECT COUNT(*) INTO need_pk_pv FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='producto_variantes' AND CONSTRAINT_TYPE='PRIMARY KEY';
  SELECT COUNT(*) INTO need_pk_bo FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='bodegas' AND CONSTRAINT_TYPE='PRIMARY KEY';
  IF need_pk_pv = 0 THEN
    SET @s := 'ALTER TABLE producto_variantes ADD PRIMARY KEY (id_variante)';
    PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
  IF need_pk_bo = 0 THEN
    SET @s := 'ALTER TABLE bodegas ADD PRIMARY KEY (id_bodega)';
    PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;
CALL ensure_primary_keys();
DROP PROCEDURE ensure_primary_keys;

ALTER TABLE inventario_bodega
  ADD CONSTRAINT fk_ib_variante FOREIGN KEY (id_variante) REFERENCES producto_variantes(id_variante) ON UPDATE CASCADE ON DELETE RESTRICT,
  ADD CONSTRAINT fk_ib_bodega FOREIGN KEY (id_bodega) REFERENCES bodegas(id_bodega) ON UPDATE CASCADE ON DELETE RESTRICT,
  ADD UNIQUE KEY uq_ib_bodega_variante (id_bodega, id_variante);

ALTER TABLE producto_variantes
  DROP COLUMN IF EXISTS id_bodega,
  DROP COLUMN IF EXISTS stock_por_pares,
  DROP COLUMN IF EXISTS stock_por_cajas;

CREATE OR REPLACE VIEW vista_inventario_pares AS
SELECT
  pv.id_producto,
  p.nombre,
  pv.ean AS codigo_barras,
  COALESCE(SUM(ib.Stock_par),0) AS stock,
  p.precio_venta AS precio,
  p.ubicacion,
  p.activo
FROM producto_variantes pv
JOIN productos p ON pv.id_producto=p.id_producto
LEFT JOIN inventario_bodega ib ON ib.id_variante=pv.id_variante AND ib.activo=1
GROUP BY pv.id_producto, pv.ean, p.nombre, p.precio_venta, p.ubicacion, p.activo;

CREATE OR REPLACE VIEW vista_inventario_cajas AS
SELECT
  pv.id_producto,
  p.nombre,
  pv.ean AS codigo_barras,
  COALESCE(SUM(ib.Stock_caja),0) AS stock,
  p.precio_compra AS costo,
  p.ubicacion,
  p.activo
FROM producto_variantes pv
JOIN productos p ON pv.id_producto=p.id_producto
LEFT JOIN inventario_bodega ib ON ib.id_variante=pv.id_variante AND ib.activo=1
GROUP BY pv.id_producto, pv.ean, p.nombre, p.precio_compra, p.ubicacion, p.activo;

DROP TRIGGER IF EXISTS trg_pv_insert_ib;
DELIMITER $$
CREATE TRIGGER trg_pv_insert_ib AFTER INSERT ON producto_variantes
FOR EACH ROW
BEGIN
  INSERT INTO inventario_bodega (id_bodega, id_variante, Stock_par, Stock_caja, stock_reservado, fecha_ultimo_movimiento, ubicacion_especifica, activo)
  SELECT b.id_bodega, NEW.id_variante, 0, 0, 0, NOW(), NULL, 1
  FROM bodegas b
  WHERE b.activo = 1
  LIMIT 1;
END$$
DELIMITER ;

COMMIT;

-- =====================================================================
-- Ajustes adicionales de integridad y compatibilidad entre bases
-- =====================================================================

DROP PROCEDURE IF EXISTS fix_sessions_fk;
DELIMITER $$
CREATE PROCEDURE fix_sessions_fk()
BEGIN
  DECLARE pk_hist INT DEFAULT 0;
  DECLARE pk_ses INT DEFAULT 0;

  SELECT COUNT(*) INTO pk_hist FROM information_schema.TABLE_CONSTRAINTS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='historial_sesiones' AND CONSTRAINT_TYPE='PRIMARY KEY';
  IF pk_hist = 0 THEN
    SET @s := 'ALTER TABLE historial_sesiones ADD PRIMARY KEY (id_historial)';
    PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
  SET @s := 'ALTER TABLE historial_sesiones MODIFY id_historial INT(11) NOT NULL AUTO_INCREMENT';
  PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

  SELECT COUNT(*) INTO pk_ses FROM information_schema.TABLE_CONSTRAINTS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='sesiones_activas' AND CONSTRAINT_TYPE='PRIMARY KEY';
  IF pk_ses = 0 THEN
    SET @s := 'ALTER TABLE sesiones_activas ADD PRIMARY KEY (id_sesion)';
    PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
  SET @s := 'ALTER TABLE sesiones_activas MODIFY id_sesion INT(11) NOT NULL AUTO_INCREMENT';
  PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

  -- Índices y FKs sesiones
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sesiones_activas' AND INDEX_NAME='idx_sesiones_usuario'
  ) THEN
    SET @s := 'ALTER TABLE sesiones_activas ADD INDEX idx_sesiones_usuario (id_usuario)';
    PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='fk_sesiones_usuario'
  ) THEN
    SET @s := 'ALTER TABLE sesiones_activas ADD CONSTRAINT fk_sesiones_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario) ON UPDATE CASCADE ON DELETE RESTRICT';
    PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='historial_sesiones' AND INDEX_NAME='idx_hist_sesion'
  ) THEN
    SET @s := 'ALTER TABLE historial_sesiones ADD INDEX idx_hist_sesion (id_sesion)';
    PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='historial_sesiones' AND INDEX_NAME='idx_hist_usuario'
  ) THEN
    SET @s := 'ALTER TABLE historial_sesiones ADD INDEX idx_hist_usuario (id_usuario)';
    PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;

  -- Limpieza de huerfanos antes de crear FKs
  SET @s := 'ALTER TABLE historial_sesiones MODIFY id_sesion INT(11) NULL';
  PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  SET @s := 'UPDATE historial_sesiones h LEFT JOIN sesiones_activas s ON s.id_sesion=h.id_sesion SET h.id_sesion=NULL WHERE s.id_sesion IS NULL';
  PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  SET @s := 'ALTER TABLE historial_sesiones MODIFY id_usuario INT(11) NULL';
  PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  SET @s := 'UPDATE historial_sesiones h LEFT JOIN usuarios u ON u.id_usuario=h.id_usuario SET h.id_usuario=NULL WHERE u.id_usuario IS NULL';
  PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='fk_hist_sesion'
  ) THEN
    SET @s := 'ALTER TABLE historial_sesiones ADD CONSTRAINT fk_hist_sesion FOREIGN KEY (id_sesion) REFERENCES sesiones_activas (id_sesion) ON UPDATE CASCADE ON DELETE SET NULL';
    PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='fk_hist_usuario'
  ) THEN
    SET @s := 'ALTER TABLE historial_sesiones ADD CONSTRAINT fk_hist_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario) ON UPDATE CASCADE ON DELETE SET NULL';
    PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;
CALL fix_sessions_fk();
DROP PROCEDURE fix_sessions_fk;

-- FKs y índices para productos/variantes (idempotentes mediante IF NOT EXISTS)
ALTER TABLE producto_variantes
  ADD INDEX IF NOT EXISTS idx_pv_producto (id_producto),
  ADD INDEX IF NOT EXISTS idx_pv_talla (id_talla),
  ADD INDEX IF NOT EXISTS idx_pv_color (id_color);

ALTER TABLE inventario_bodega
  ADD INDEX IF NOT EXISTS idx_ib_bodega (id_bodega),
  ADD INDEX IF NOT EXISTS idx_ib_variante (id_variante);

-- Claves foráneas principales (si no existen)
DELIMITER $$
CREATE PROCEDURE apply_product_fks()
BEGIN
  DECLARE add_fk_pv_prod INT DEFAULT 0;
  DECLARE add_fk_pv_talla INT DEFAULT 0;
  DECLARE add_fk_pv_color INT DEFAULT 0;
  DECLARE add_fk_prod_cat INT DEFAULT 0;
  DECLARE add_fk_prod_brand INT DEFAULT 0;
  DECLARE add_fk_prod_supplier INT DEFAULT 0;

  SELECT COUNT(*) INTO add_fk_pv_prod FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='fk_pv_producto';
  SELECT COUNT(*) INTO add_fk_pv_talla FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='fk_pv_talla';
  SELECT COUNT(*) INTO add_fk_pv_color FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='fk_pv_color';
  SELECT COUNT(*) INTO add_fk_prod_cat FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='fk_prod_categoria';
  SELECT COUNT(*) INTO add_fk_prod_brand FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='fk_prod_marca';
  SELECT COUNT(*) INTO add_fk_prod_supplier FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='fk_prod_proveedor';

  IF add_fk_pv_prod = 0 THEN
    SET @sql := 'ALTER TABLE producto_variantes ADD CONSTRAINT fk_pv_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto) ON UPDATE CASCADE ON DELETE RESTRICT';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF add_fk_pv_talla = 0 THEN
    SET @sql := 'ALTER TABLE producto_variantes ADD CONSTRAINT fk_pv_talla FOREIGN KEY (id_talla) REFERENCES tallas(id_talla) ON UPDATE CASCADE ON DELETE RESTRICT';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF add_fk_pv_color = 0 THEN
    SET @sql := 'ALTER TABLE producto_variantes ADD CONSTRAINT fk_pv_color FOREIGN KEY (id_color) REFERENCES colores(id_color) ON UPDATE CASCADE ON DELETE RESTRICT';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;

  IF add_fk_prod_cat = 0 THEN
    SET @sql := 'ALTER TABLE productos ADD CONSTRAINT fk_prod_categoria FOREIGN KEY (id_categoria) REFERENCES categorias(id_categoria) ON UPDATE CASCADE ON DELETE SET NULL';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF add_fk_prod_brand = 0 THEN
    SET @sql := 'ALTER TABLE productos ADD CONSTRAINT fk_prod_marca FOREIGN KEY (id_marca) REFERENCES marcas(id_marca) ON UPDATE CASCADE ON DELETE SET NULL';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
  IF add_fk_prod_supplier = 0 THEN
    SET @sql := 'ALTER TABLE productos ADD CONSTRAINT fk_prod_proveedor FOREIGN KEY (id_proveedor) REFERENCES proveedores(id_proveedor) ON UPDATE CASCADE ON DELETE SET NULL';
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
END$$
DELIMITER ;
CALL apply_product_fks();
DROP PROCEDURE apply_product_fks;

-- Defaults y consistencia en inventario_bodega
ALTER TABLE inventario_bodega
  MODIFY stock_reservado INT(11) NOT NULL DEFAULT 0,
  MODIFY activo TINYINT(1) NOT NULL DEFAULT 1;
