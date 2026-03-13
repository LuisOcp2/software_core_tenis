DELIMITER $$

DROP PROCEDURE IF EXISTS `sp_registrar_abono_con_evidencia`$$
CREATE DEFINER=`pma_admin`@`localhost` PROCEDURE `sp_registrar_abono_con_evidencia` (IN `p_id_compra` INT, IN `p_monto` DECIMAL(18,2), IN `p_medio_pago` VARCHAR(20), IN `p_evidencia_url` VARCHAR(255), IN `p_evidencia_bytes` LONGBLOB, IN `p_evidencia_mime` VARCHAR(64), IN `p_evidencia_nombre` VARCHAR(200), OUT `p_id_abono` INT, OUT `p_saldo_nuevo` DECIMAL(18,2), OUT `p_estado_pago` VARCHAR(20))   BEGIN
  DECLARE v_total DECIMAL(18,2);
  DECLARE v_abonado DECIMAL(18,2);
  DECLARE v_saldo DECIMAL(18,2);

  IF p_monto IS NULL OR p_monto <= 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Monto de abono inválido';
  END IF;

  -- Calcular abonado real desde tabla abonos para mayor seguridad
  SELECT IFNULL(SUM(monto), 0) INTO v_abonado
    FROM abonos WHERE id_compra = p_id_compra AND estado != 'cancelado';

  SELECT total INTO v_total
    FROM compras WHERE id_compra = p_id_compra FOR UPDATE;

  IF v_total IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Compra no encontrada';
  END IF;

  SET v_saldo = IFNULL(v_total,0) - IFNULL(v_abonado,0);
  
  IF p_monto > (v_saldo + 0.01) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Abono mayor al saldo pendiente real';
  END IF;

  INSERT INTO abonos (id_compra, monto, medio_pago, evidencia_url, evidencia_bytes, evidencia_mime, evidencia_nombre, estado)
  VALUES (p_id_compra, p_monto, IFNULL(p_medio_pago,'efectivo'), p_evidencia_url, p_evidencia_bytes, p_evidencia_mime, p_evidencia_nombre, 'pendiente');

  SET p_id_abono = LAST_INSERT_ID();

  -- Actualizar compras explícitamente para asegurar consistencia
  UPDATE compras
     SET total_abonado = v_abonado + p_monto,
         saldo_pendiente = GREATEST(v_total - (v_abonado + p_monto), 0),
         estado_pago = CASE WHEN (v_total - (v_abonado + p_monto)) <= 0.01 THEN 'completado' ELSE 'pendiente' END
   WHERE id_compra = p_id_compra;

  SELECT saldo_pendiente, estado_pago INTO p_saldo_nuevo, p_estado_pago
    FROM compras WHERE id_compra = p_id_compra;

  UPDATE abonos
     SET numero_comprobante = CONCAT('AB-', DATE_FORMAT(NOW(), '%Y%m%d'), '-', LPAD(p_id_abono, 6, '0')),
         estado = CASE WHEN p_estado_pago = 'completado' THEN 'completado' ELSE 'pendiente' END
   WHERE id_abono = p_id_abono;
END$$

DROP PROCEDURE IF EXISTS `sp_registrar_abono_con_evidencia_rs`$$
CREATE DEFINER=`pma_admin`@`localhost` PROCEDURE `sp_registrar_abono_con_evidencia_rs` (IN `p_id_compra` INT, IN `p_monto` DECIMAL(18,2), IN `p_medio_pago` VARCHAR(20), IN `p_evidencia_url` VARCHAR(255), IN `p_evidencia_bytes` LONGBLOB, IN `p_evidencia_mime` VARCHAR(64), IN `p_evidencia_nombre` VARCHAR(200))   BEGIN
  DECLARE v_total DECIMAL(18,2);
  DECLARE v_abonado DECIMAL(18,2);
  DECLARE v_saldo DECIMAL(18,2);
  DECLARE v_id_abono INT;
  DECLARE v_estado_pago VARCHAR(20);

  IF p_monto IS NULL OR p_monto <= 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Monto de abono inválido';
  END IF;

  -- Calcular abonado real desde tabla abonos
  SELECT IFNULL(SUM(monto), 0) INTO v_abonado
    FROM abonos WHERE id_compra = p_id_compra AND estado != 'cancelado';

  SELECT total INTO v_total
    FROM compras WHERE id_compra = p_id_compra FOR UPDATE;

  IF v_total IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Compra no encontrada';
  END IF;

  SET v_saldo = IFNULL(v_total,0) - IFNULL(v_abonado,0);
  
  IF p_monto > (v_saldo + 0.01) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Abono mayor al saldo pendiente real';
  END IF;

  INSERT INTO abonos (id_compra, monto, medio_pago, evidencia_url, evidencia_bytes, evidencia_mime, evidencia_nombre, estado)
  VALUES (p_id_compra, p_monto, IFNULL(p_medio_pago,'efectivo'), p_evidencia_url, p_evidencia_bytes, p_evidencia_mime, p_evidencia_nombre, 'pendiente');

  SET v_id_abono = LAST_INSERT_ID();

  -- Actualizar compras explícitamente
  UPDATE compras
     SET total_abonado = v_abonado + p_monto,
         saldo_pendiente = GREATEST(v_total - (v_abonado + p_monto), 0),
         estado_pago = CASE WHEN (v_total - (v_abonado + p_monto)) <= 0.01 THEN 'completado' ELSE 'pendiente' END
   WHERE id_compra = p_id_compra;

  SELECT estado_pago INTO v_estado_pago FROM compras WHERE id_compra = p_id_compra;

  UPDATE abonos
     SET numero_comprobante = CONCAT('AB-', DATE_FORMAT(NOW(), '%Y%m%d'), '-', LPAD(v_id_abono, 6, '0')),
         estado = CASE WHEN v_estado_pago = 'completado' THEN 'completado' ELSE 'pendiente' END
   WHERE id_abono = v_id_abono;

  SELECT v_id_abono AS id_abono,
         (SELECT saldo_pendiente FROM compras WHERE id_compra = p_id_compra) AS saldo_nuevo,
         (SELECT estado_pago FROM compras WHERE id_compra = p_id_compra) AS estado_pago;
END$$

DELIMITER ;
