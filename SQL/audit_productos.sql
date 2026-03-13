CREATE TABLE IF NOT EXISTS `historial_cambios_productos` (
  `id_historial` INT(11) NOT NULL AUTO_INCREMENT,
  `id_producto` INT(11) NOT NULL,
  `id_usuario` INT(11) NOT NULL,
  `campo_modificado` VARCHAR(50) NOT NULL,
  `valor_anterior` TEXT,
  `valor_nuevo` TEXT,
  `fecha_cambio` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_historial`),
  KEY `idx_producto_historial` (`id_producto`),
  KEY `idx_fecha_historial` (`fecha_cambio`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
