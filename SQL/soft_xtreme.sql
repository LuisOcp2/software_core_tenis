-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Servidor: localhost
-- Tiempo de generación: 10-02-2026 a las 18:13:16
-- Versión del servidor: 10.11.15-MariaDB-ubu2404
-- Versión de PHP: 8.3.27

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de datos: `soft_xtreme`
--

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `abonos`
--

CREATE TABLE `abonos` (
  `id_abono` int(11) NOT NULL,
  `id_compra` int(11) NOT NULL,
  `monto` decimal(18,2) NOT NULL,
  `fecha_abono` datetime NOT NULL DEFAULT current_timestamp(),
  `medio_pago` enum('efectivo','transferencia','tarjeta','cheque','otro') NOT NULL,
  `evidencia_url` varchar(255) DEFAULT NULL,
  `estado` enum('pendiente','completado','cancelado') NOT NULL DEFAULT 'pendiente',
  `numero_comprobante` varchar(30) DEFAULT NULL,
  `evidencia_bytes` longblob DEFAULT NULL,
  `evidencia_mime` varchar(64) DEFAULT NULL,
  `evidencia_nombre` varchar(200) DEFAULT NULL
) ;

--
-- Disparadores `abonos`
--
DELIMITER $$
CREATE TRIGGER `trg_abonos_ad` AFTER DELETE ON `abonos` FOR EACH ROW BEGIN
  DECLARE v_total DECIMAL(18,2);
  DECLARE v_abonado DECIMAL(18,2);
  DECLARE v_saldo DECIMAL(18,2);
  DECLARE v_estado_pago ENUM('pendiente','parcial','completado','cancelado');

  UPDATE compras
     SET total_abonado = GREATEST(IFNULL(total_abonado,0) - IFNULL(OLD.monto,0), 0)
   WHERE id_compra = OLD.id_compra;

  SELECT total, total_abonado INTO v_total, v_abonado
    FROM compras WHERE id_compra = OLD.id_compra;

  SET v_saldo = IFNULL(v_total,0) - IFNULL(v_abonado,0);
  SET v_estado_pago = CASE
      WHEN v_saldo <= 0 THEN 'completado'
      WHEN v_abonado > 0 THEN 'parcial'
      ELSE 'pendiente'
    END;

  UPDATE compras
     SET saldo_pendiente = GREATEST(v_saldo,0),
         estado_pago     = v_estado_pago
   WHERE id_compra = OLD.id_compra;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_abonos_ai` AFTER INSERT ON `abonos` FOR EACH ROW BEGIN
  DECLARE v_total DECIMAL(18,2);
  DECLARE v_abonado DECIMAL(18,2);
  DECLARE v_saldo DECIMAL(18,2);
  DECLARE v_estado_pago ENUM('pendiente','parcial','completado','cancelado');

  UPDATE compras
     SET total_abonado = IFNULL(total_abonado,0) + NEW.monto
   WHERE id_compra = NEW.id_compra;

  SELECT total, total_abonado INTO v_total, v_abonado
    FROM compras WHERE id_compra = NEW.id_compra;

  SET v_saldo = IFNULL(v_total,0) - IFNULL(v_abonado,0);
  SET v_estado_pago = CASE
      WHEN v_saldo <= 0 THEN 'completado'
      WHEN v_abonado > 0 THEN 'parcial'
      ELSE 'pendiente'
    END;

  UPDATE compras
     SET saldo_pendiente = GREATEST(v_saldo,0),
         estado_pago     = v_estado_pago
   WHERE id_compra = NEW.id_compra;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_abonos_au` AFTER UPDATE ON `abonos` FOR EACH ROW BEGIN
  DECLARE v_total DECIMAL(18,2);
  DECLARE v_abonado DECIMAL(18,2);
  DECLARE v_saldo DECIMAL(18,2);
  DECLARE v_estado_pago ENUM('pendiente','parcial','completado','cancelado');

  UPDATE compras
     SET total_abonado = IFNULL(total_abonado,0) - IFNULL(OLD.monto,0) + IFNULL(NEW.monto,0)
   WHERE id_compra = NEW.id_compra;

  SELECT total, total_abonado INTO v_total, v_abonado
    FROM compras WHERE id_compra = NEW.id_compra;

  SET v_saldo = IFNULL(v_total,0) - IFNULL(v_abonado,0);
  SET v_estado_pago = CASE
      WHEN v_saldo <= 0 THEN 'completado'
      WHEN v_abonado > 0 THEN 'parcial'
      ELSE 'pendiente'
    END;

  UPDATE compras
     SET saldo_pendiente = GREATEST(v_saldo,0),
         estado_pago     = v_estado_pago
   WHERE id_compra = NEW.id_compra;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_abonos_bi` BEFORE INSERT ON `abonos` FOR EACH ROW BEGIN
  DECLARE v_total DECIMAL(18,2);
  DECLARE v_abonado DECIMAL(18,2);
  DECLARE v_saldo DECIMAL(18,2);

  SELECT total, total_abonado INTO v_total, v_abonado
    FROM compras WHERE id_compra = NEW.id_compra FOR UPDATE;

  SET v_saldo = IFNULL(v_total,0) - IFNULL(v_abonado,0);
  IF NEW.monto > v_saldo THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Abono mayor al saldo pendiente';
  END IF;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_abonos_bu` BEFORE UPDATE ON `abonos` FOR EACH ROW BEGIN
  DECLARE v_total DECIMAL(18,2);
  DECLARE v_abonado DECIMAL(18,2);
  DECLARE v_saldo DECIMAL(18,2);

  SELECT total, total_abonado INTO v_total, v_abonado
    FROM compras WHERE id_compra = NEW.id_compra FOR UPDATE;

  SET v_saldo = IFNULL(v_total,0) - (IFNULL(v_abonado,0) - IFNULL(OLD.monto,0));
  IF NEW.monto > v_saldo THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Actualización de abono excede saldo pendiente';
  END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `ajustes_inventario`
--

CREATE TABLE `ajustes_inventario` (
  `id_ajuste` int(11) NOT NULL,
  `id_detalle_conteo` int(11) NOT NULL,
  `id_producto` int(11) NOT NULL,
  `id_variante` int(11) DEFAULT NULL,
  `tipo_ajuste` enum('entrada caja','salida caja','ajuste caja','entrada par','salida par','ajuste par') NOT NULL,
  `cantidad` int(11) NOT NULL,
  `razon` enum('error_conteo','perdida','deterioro','error_registro','otra') NOT NULL,
  `observaciones` text DEFAULT NULL,
  `aprobado` tinyint(1) DEFAULT 0,
  `id_usuario_creador` int(11) NOT NULL,
  `fecha_creacion` datetime DEFAULT current_timestamp(),
  `id_usuario_aprobador` int(11) DEFAULT NULL,
  `fecha_aprobacion` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `aplicaciones_nota_credito`
--

CREATE TABLE `aplicaciones_nota_credito` (
  `id_aplicacion` int(11) NOT NULL,
  `id_nota_credito` int(11) NOT NULL,
  `id_venta` int(11) NOT NULL,
  `monto_aplicado` decimal(10,2) NOT NULL,
  `fecha_aplicacion` datetime DEFAULT current_timestamp(),
  `id_usuario_aplica` int(11) NOT NULL,
  `observaciones` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `auditoria_trazabilidad`
--

CREATE TABLE `auditoria_trazabilidad` (
  `id_auditoria` int(11) NOT NULL,
  `id_variante` int(11) NOT NULL COMMENT 'La variante que se movió',
  `id_proveedor` int(11) DEFAULT NULL COMMENT 'Proveedor de la variante',
  `tipo_evento` enum('venta','traspaso_salida','traspaso_entrada','devolucion','compra','ajuste') NOT NULL,
  `cantidad` int(11) NOT NULL COMMENT 'Cantidad de unidades movidas',
  `id_bodega_origen` int(11) DEFAULT NULL COMMENT 'Bodega de salida',
  `id_bodega_destino` int(11) DEFAULT NULL COMMENT 'Bodega de entrada',
  `id_referencia` int(11) DEFAULT NULL COMMENT 'ID de venta, traspaso, devolución, etc.',
  `tipo_referencia` varchar(20) DEFAULT NULL COMMENT 'venta, traspaso, devolucion, compra',
  `fecha_evento` datetime DEFAULT current_timestamp(),
  `id_usuario` int(11) NOT NULL,
  `observaciones` text DEFAULT NULL,
  `activo` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Auditoría centralizada de todos los movimientos sin lotes';

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `bodegas`
--

CREATE TABLE `bodegas` (
  `id_bodega` int(11) NOT NULL,
  `codigo` varchar(20) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `direccion` text DEFAULT NULL,
  `telefono` varchar(20) DEFAULT NULL,
  `responsable` varchar(100) DEFAULT NULL,
  `tipo` enum('principal','sucursal','deposito','temporal') DEFAULT 'principal',
  `capacidad_maxima` int(11) DEFAULT NULL,
  `activa` tinyint(1) DEFAULT 1,
  `fecha_creacion` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `bodega_sectores`
--

CREATE TABLE `bodega_sectores` (
  `id_sector` int(11) NOT NULL,
  `id_bodega` int(11) NOT NULL,
  `codigo_sector` varchar(20) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `ubicacion` varchar(100) DEFAULT NULL,
  `capacidad` int(11) DEFAULT NULL,
  `tipo_sector` enum('estanteria','piso','vitrina','deposito') DEFAULT 'estanteria',
  `activo` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `busquedas`
--

CREATE TABLE `busquedas` (
  `id_busqueda` int(11) NOT NULL,
  `termino_busqueda` varchar(255) NOT NULL,
  `id_usuario` int(11) DEFAULT NULL,
  `fecha_busqueda` timestamp NULL DEFAULT current_timestamp(),
  `ip_usuario` varchar(45) DEFAULT NULL,
  `user_agent` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `cajas`
--

CREATE TABLE `cajas` (
  `id_caja` int(11) NOT NULL,
  `id_bodega` int(11) NOT NULL,
  `nombre` varchar(50) NOT NULL,
  `ubicacion` varchar(100) DEFAULT NULL,
  `activa` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `caja_menor`
--

CREATE TABLE `caja_menor` (
  `id_caja_menor` int(11) NOT NULL,
  `codigo` varchar(20) NOT NULL COMMENT 'Código único de la caja menor',
  `nombre` varchar(100) NOT NULL COMMENT 'Nombre descriptivo de la caja menor',
  `id_caja_principal` int(11) NOT NULL COMMENT 'FK a la caja registradora principal',
  `saldo_minimo` decimal(10,2) DEFAULT 50000.00 COMMENT 'Saldo mínimo operativo',
  `saldo_maximo` decimal(10,2) DEFAULT 500000.00 COMMENT 'Saldo máximo permitido',
  `responsable_actual` int(11) DEFAULT NULL COMMENT 'Usuario responsable actual',
  `ubicacion` varchar(100) DEFAULT NULL COMMENT 'Ubicación física de la caja menor',
  `activa` tinyint(1) DEFAULT 1 COMMENT '1=activa, 0=inactiva',
  `fecha_creacion` datetime DEFAULT current_timestamp(),
  `fecha_actualizacion` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Control específico de caja menor';

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `caja_movimientos`
--

CREATE TABLE `caja_movimientos` (
  `id_movimiento` int(11) NOT NULL,
  `id_caja` int(11) NOT NULL,
  `id_usuario` int(11) NOT NULL,
  `fecha_apertura` datetime NOT NULL,
  `fecha_cierre` datetime DEFAULT NULL,
  `monto_inicial` decimal(10,2) NOT NULL,
  `monto_final` decimal(10,2) DEFAULT NULL,
  `observaciones` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `caja_movimiento_detalle`
--

CREATE TABLE `caja_movimiento_detalle` (
  `id_detalle_movimiento` int(11) NOT NULL,
  `id_movimiento_caja` int(11) NOT NULL COMMENT 'FK a caja_movimientos',
  `fecha_movimiento` datetime NOT NULL DEFAULT current_timestamp(),
  `tipo_movimiento` enum('entrada_venta','entrada_otro','salida_gasto','salida_compra_externa','ajuste_entrada','ajuste_salida') NOT NULL COMMENT 'Tipo específico del movimiento',
  `concepto` varchar(200) NOT NULL COMMENT 'Descripción del movimiento',
  `monto` decimal(10,2) NOT NULL COMMENT 'Monto del movimiento (positivo siempre, tipo define si es entrada o salida)',
  `id_referencia` int(11) DEFAULT NULL COMMENT 'ID de la referencia (gasto_interno, compra_externa, etc.)',
  `tipo_referencia` enum('gasto_interno','compra_externa','venta','ajuste','otro') DEFAULT NULL COMMENT 'Tipo de referencia',
  `numero_comprobante` varchar(50) DEFAULT NULL COMMENT 'Número de comprobante único',
  `id_usuario` int(11) NOT NULL COMMENT 'Usuario que registra el movimiento',
  `observaciones` text DEFAULT NULL COMMENT 'Observaciones adicionales',
  `activo` tinyint(1) DEFAULT 1 COMMENT '1=activo, 0=anulado',
  `fecha_registro` datetime DEFAULT current_timestamp(),
  `fecha_actualizacion` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Movimientos detallados de caja para cuadre diario';

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `carrito`
--

CREATE TABLE `carrito` (
  `id_carrito` int(11) NOT NULL COMMENT 'ID único del item en el carrito',
  `usuario_id` int(11) DEFAULT NULL,
  `session_id` varchar(128) DEFAULT NULL,
  `id_producto` int(11) NOT NULL COMMENT 'ID del producto en el carrito',
  `id_variante` int(11) NOT NULL COMMENT 'ID de la variante específica del producto',
  `id_bodega` int(11) DEFAULT NULL,
  `cantidad` int(11) NOT NULL DEFAULT 1 COMMENT 'Cantidad del producto en el carrito',
  `precio_unitario` decimal(10,2) NOT NULL COMMENT 'Precio unitario del producto al momento de agregarlo',
  `fecha_agregado` timestamp NULL DEFAULT current_timestamp() COMMENT 'Fecha y hora cuando se agregó el item',
  `fecha_actualizado` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT 'Fecha y hora de la última actualización'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `catalogo_tipos_gasto`
--

CREATE TABLE `catalogo_tipos_gasto` (
  `id_tipo_gasto` int(11) NOT NULL,
  `codigo` varchar(20) NOT NULL COMMENT 'Código único del tipo de gasto',
  `nombre` varchar(100) NOT NULL COMMENT 'Nombre descriptivo del tipo de gasto',
  `descripcion` text DEFAULT NULL COMMENT 'Descripción detallada del tipo de gasto',
  `categoria` enum('operativo','administrativo','emergencia','inversion','otros') NOT NULL DEFAULT 'operativo',
  `requiere_autorizacion` tinyint(1) DEFAULT 0 COMMENT '1 si requiere autorización previa',
  `monto_maximo` decimal(10,2) DEFAULT NULL COMMENT 'Monto máximo permitido sin autorización',
  `activo` tinyint(1) DEFAULT 1 COMMENT '1 si el tipo está activo',
  `fecha_creacion` datetime DEFAULT current_timestamp(),
  `fecha_actualizacion` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Catálogo de tipos de gastos para caja menor';

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `categorias`
--

CREATE TABLE `categorias` (
  `id_categoria` int(11) NOT NULL,
  `nombre` varchar(50) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `activo` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `clientes`
--

CREATE TABLE `clientes` (
  `id_cliente` int(11) NOT NULL,
  `id_bodega` int(11) DEFAULT NULL,
  `nombre` varchar(100) NOT NULL,
  `dni` varchar(20) DEFAULT NULL,
  `direccion` text DEFAULT NULL,
  `telefono` varchar(20) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `fecha_registro` datetime DEFAULT current_timestamp(),
  `puntos_acumulados` int(11) DEFAULT 0,
  `activo` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `colores`
--

CREATE TABLE `colores` (
  `id_color` int(11) NOT NULL,
  `nombre` varchar(50) NOT NULL,
  `codigo_hex` varchar(7) DEFAULT NULL,
  `codigo_pantone` varchar(20) DEFAULT NULL,
  `descripcion` text DEFAULT NULL,
  `activo` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `compras`
--

CREATE TABLE `compras` (
  `id_compra` int(11) NOT NULL,
  `numero_compra` varchar(50) DEFAULT NULL COMMENT 'Número secuencial de compra',
  `id_proveedor` int(11) NOT NULL,
  `id_usuario` int(11) NOT NULL,
  `id_bodega` int(11) NOT NULL DEFAULT 1 COMMENT 'FK a bodegas (donde ingresa la mercancía)',
  `numero_factura` varchar(50) DEFAULT NULL COMMENT 'Número de factura del proveedor',
  `fecha_compra` date DEFAULT NULL,
  `fecha_recepcion` datetime DEFAULT NULL COMMENT 'Fecha de recepción efectiva',
  `subtotal` decimal(10,2) NOT NULL,
  `iva` decimal(10,2) NOT NULL,
  `total` decimal(10,2) NOT NULL,
  `estado` enum('pendiente','recibida','cancelada') DEFAULT 'recibida',
  `observaciones` text DEFAULT NULL,
  `fecha_creacion` datetime DEFAULT current_timestamp(),
  `fecha_actualizacion` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `total_abonado` decimal(18,2) NOT NULL DEFAULT 0.00,
  `saldo_pendiente` decimal(18,2) NOT NULL DEFAULT 0.00,
  `estado_pago` enum('pendiente','parcial','completado','cancelado') NOT NULL DEFAULT 'pendiente'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `compras_externas`
--

CREATE TABLE `compras_externas` (
  `id_compra_externa` int(11) NOT NULL,
  `numero_compra` varchar(50) NOT NULL COMMENT 'Número secuencial de compra',
  `tienda_proveedor` varchar(100) NOT NULL COMMENT 'Nombre de la tienda o proveedor externo',
  `numero_factura_recibo` varchar(50) DEFAULT NULL COMMENT 'Número de factura del proveedor',
  `id_bodega` int(11) NOT NULL COMMENT 'FK a bodegas (donde ingresa la compra)',
  `id_usuario` int(11) NOT NULL COMMENT 'FK a usuarios (quien registra)',
  `subtotal` decimal(10,2) NOT NULL,
  `iva` decimal(10,2) DEFAULT 0.00,
  `total` decimal(10,2) NOT NULL,
  `estado` enum('pendiente','recibida','facturada') DEFAULT 'recibida',
  `observaciones` text DEFAULT NULL,
  `fecha_compra` datetime DEFAULT current_timestamp(),
  `fecha_recepcion` datetime DEFAULT NULL,
  `fecha_creacion` datetime DEFAULT current_timestamp(),
  `fecha_actualizacion` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Compras a tiendas externas o proveedores externos';

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `compras_externas_detalles`
--

CREATE TABLE `compras_externas_detalles` (
  `id_detalle_compra_externa` int(11) NOT NULL,
  `id_compra_externa` int(11) NOT NULL COMMENT 'FK a compras_externas',
  `id_producto` int(11) NOT NULL COMMENT 'FK a productos',
  `id_variante` int(11) NOT NULL COMMENT 'FK a producto_variantes',
  `cantidad` int(11) NOT NULL COMMENT 'Cantidad en pares',
  `precio_unitario` decimal(10,2) NOT NULL COMMENT 'Precio por par pagado',
  `subtotal` decimal(10,2) NOT NULL,
  `fecha_creacion` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Detalle de productos en compras externas';

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `compra_detalles`
--

CREATE TABLE `compra_detalles` (
  `id_detalle_compra` int(11) NOT NULL,
  `id_compra` int(11) NOT NULL COMMENT 'FK a compras',
  `id_producto` int(11) NOT NULL COMMENT 'FK a productos',
  `id_variante` int(11) NOT NULL COMMENT 'FK a producto_variantes',
  `cantidad` int(11) NOT NULL COMMENT 'Cantidad de unidades',
  `tipo_unidad` enum('par','caja') NOT NULL DEFAULT 'par' COMMENT 'Tipo de unidad (par o caja)',
  `precio_unitario` decimal(10,2) NOT NULL COMMENT 'Precio de compra por unidad',
  `subtotal` decimal(10,2) NOT NULL COMMENT 'cantidad * precio_unitario',
  `observaciones` text DEFAULT NULL COMMENT 'Observaciones del detalle',
  `fecha_creacion` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Detalle de productos en compras a proveedores';

--
-- Disparadores `compra_detalles`
--
DELIMITER $$
CREATE TRIGGER `tr_auditoria_compra_insert` AFTER INSERT ON `compra_detalles` FOR EACH ROW BEGIN
    DECLARE var_id_proveedor INT;
    DECLARE var_id_usuario INT;
    DECLARE var_id_bodega INT;
    
    -- Obtener proveedor de la compra
    SELECT c.id_proveedor, c.id_usuario, c.id_bodega 
    INTO var_id_proveedor, var_id_usuario, var_id_bodega
    FROM compras c
    WHERE c.id_compra = NEW.id_compra;
    
    -- Registrar en auditoría
    INSERT INTO auditoria_trazabilidad (
        id_variante, 
        id_proveedor, 
        tipo_evento, 
        cantidad,
        id_bodega_destino,
        id_referencia, 
        tipo_referencia,
        id_usuario, 
        fecha_evento,
        observaciones
    ) VALUES (
        NEW.id_variante, 
        var_id_proveedor, 
        'compra', 
        NEW.cantidad,
        var_id_bodega,
        NEW.id_compra, 
        'compra',
        var_id_usuario, 
        NOW(),
        CONCAT('Ingreso por compra - ', NEW.tipo_unidad)
    );
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `configuraciones_tallas`
--

CREATE TABLE `configuraciones_tallas` (
  `id_configuracion` int(11) NOT NULL,
  `nombre_configuracion` varchar(100) NOT NULL,
  `genero` enum('HOMBRE','MUJER','NIÑO','UNISEX') NOT NULL,
  `descripcion` text DEFAULT NULL,
  `total_pares` int(11) NOT NULL DEFAULT 24,
  `activo` tinyint(1) DEFAULT 1,
  `fecha_creacion` timestamp NULL DEFAULT current_timestamp(),
  `fecha_modificacion` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `id_usuario_creador` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Configuraciones personalizadas para conversión de cajas a pares';

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `configuraciones_tallas_detalle`
--

CREATE TABLE `configuraciones_tallas_detalle` (
  `id_detalle` int(11) NOT NULL,
  `id_configuracion` int(11) NOT NULL,
  `id_talla` int(11) NOT NULL,
  `cantidad_pares` int(11) NOT NULL DEFAULT 0,
  `orden` int(11) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Detalle de tallas y cantidades para cada configuración';

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `configuracion_devoluciones`
--

CREATE TABLE `configuracion_devoluciones` (
  `id_configuracion` int(11) NOT NULL,
  `dias_limite_devolucion` int(11) DEFAULT 30,
  `requiere_autorizacion_monto` decimal(10,2) DEFAULT 500.00,
  `permite_devolucion_productos_usados` tinyint(1) DEFAULT 1,
  `permite_devolucion_productos_promocion` tinyint(1) DEFAULT 0,
  `permite_devolucion_parcial` tinyint(1) DEFAULT 1,
  `dias_vencimiento_nota_credito` int(11) DEFAULT 365,
  `porcentaje_restocking_fee` decimal(5,2) DEFAULT 0.00,
  `activa` tinyint(1) DEFAULT 1,
  `fecha_actualizacion` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `conteos_inventario`
--

CREATE TABLE `conteos_inventario` (
  `id_conteo` int(11) NOT NULL,
  `id_bodega` int(11) NOT NULL DEFAULT 1,
  `nombre` varchar(100) NOT NULL,
  `fecha_creacion` datetime DEFAULT current_timestamp(),
  `fecha_programada` datetime NOT NULL,
  `hora_programada` time NOT NULL,
  `tipo` enum('cajas','pares') NOT NULL,
  `tipo_conteo` enum('general','parcial','ciclico','verificacion') NOT NULL,
  `estado` enum('pendiente','en_proceso','completado','cerrado') DEFAULT 'pendiente',
  `id_usuario_responsable` int(11) NOT NULL,
  `prioridad` enum('alta','media','baja') DEFAULT 'media',
  `observaciones` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `conversion_caja_traspaso`
--

CREATE TABLE `conversion_caja_traspaso` (
  `id_conversion` int(11) NOT NULL,
  `id_traspaso` int(11) NOT NULL,
  `id_detalle_traspaso` int(11) NOT NULL,
  `id_bodega_destino` int(11) NOT NULL,
  `id_variante_caja` int(11) NOT NULL,
  `cajas_convertidas` int(11) NOT NULL,
  `pares_generados` int(11) NOT NULL,
  `fecha_conversion` datetime DEFAULT current_timestamp(),
  `id_usuario` int(11) DEFAULT NULL,
  `observaciones` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `cotizaciones`
--

CREATE TABLE `cotizaciones` (
  `id_cotizacion` int(11) NOT NULL,
  `numero_cotizacion` varchar(50) NOT NULL,
  `id_cliente` int(11) NOT NULL,
  `id_usuario` int(11) NOT NULL,
  `fecha_cotizacion` date NOT NULL,
  `fecha_vencimiento` date NOT NULL,
  `subtotal` decimal(10,2) NOT NULL DEFAULT 0.00,
  `descuento` decimal(10,2) DEFAULT 0.00,
  `iva` decimal(10,2) NOT NULL DEFAULT 0.00,
  `total` decimal(10,2) NOT NULL DEFAULT 0.00,
  `estado` enum('pendiente','enviada','aprobada','rechazada','vencida','convertida') DEFAULT 'pendiente',
  `observaciones` text DEFAULT NULL,
  `condiciones` text DEFAULT NULL,
  `id_venta_generada` int(11) DEFAULT NULL,
  `fecha_creacion` datetime DEFAULT current_timestamp(),
  `fecha_actualizacion` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `cotizacion_detalles`
--

CREATE TABLE `cotizacion_detalles` (
  `id_detalle_cotizacion` int(11) NOT NULL,
  `id_cotizacion` int(11) NOT NULL,
  `id_producto` int(11) NOT NULL,
  `id_variante` int(11) DEFAULT NULL,
  `cantidad` int(11) NOT NULL,
  `precio_unitario` decimal(10,2) NOT NULL,
  `descuento` decimal(10,2) DEFAULT 0.00,
  `subtotal` decimal(10,2) NOT NULL,
  `observaciones` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `detalles_conteo_inventario`
--

CREATE TABLE `detalles_conteo_inventario` (
  `id_detalle_conteo` int(11) NOT NULL,
  `id_conteo` int(11) NOT NULL,
  `id_producto` int(11) NOT NULL,
  `id_variante` int(11) DEFAULT NULL,
  `stock_sistema` int(11) NOT NULL,
  `stock_contado` int(11) DEFAULT NULL,
  `diferencia` int(11) DEFAULT NULL,
  `estado` enum('pendiente','contado') DEFAULT 'pendiente',
  `fecha_conteo` datetime DEFAULT NULL,
  `id_usuario_contador` int(11) DEFAULT NULL,
  `observaciones` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `devoluciones`
--

CREATE TABLE `devoluciones` (
  `id_devolucion` int(11) NOT NULL,
  `numero_devolucion` varchar(50) NOT NULL,
  `id_venta` int(11) NOT NULL,
  `id_cliente` int(11) NOT NULL,
  `id_usuario_procesa` int(11) NOT NULL,
  `fecha_devolucion` datetime DEFAULT current_timestamp(),
  `tipo_devolucion` enum('total','parcial') NOT NULL,
  `motivo` enum('defecto_fabrica','talla_incorrecta','producto_dañado','insatisfaccion','error_facturacion','cambio_modelo','otros') NOT NULL,
  `estado` enum('pendiente','procesando','aprobada','rechazada','finalizada','anulada') DEFAULT 'pendiente',
  `subtotal_devolucion` decimal(10,2) NOT NULL DEFAULT 0.00,
  `iva_devolucion` decimal(10,2) NOT NULL DEFAULT 0.00,
  `total_devolucion` decimal(10,2) NOT NULL DEFAULT 0.00,
  `observaciones` text DEFAULT NULL,
  `requiere_autorizacion` tinyint(1) DEFAULT 0,
  `id_usuario_autoriza` int(11) DEFAULT NULL,
  `fecha_autorizacion` datetime DEFAULT NULL,
  `observaciones_autorizacion` text DEFAULT NULL,
  `fecha_limite_devolucion` datetime NOT NULL,
  `activa` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `devolucion_detalles`
--

CREATE TABLE `devolucion_detalles` (
  `id_detalle_devolucion` int(11) NOT NULL,
  `id_devolucion` int(11) NOT NULL,
  `id_detalle_venta` int(11) NOT NULL,
  `id_producto` int(11) NOT NULL,
  `id_variante` int(11) DEFAULT NULL,
  `cantidad_devuelta` int(11) NOT NULL,
  `cantidad_original` int(11) NOT NULL,
  `precio_unitario_original` decimal(10,2) NOT NULL,
  `descuento_original` decimal(10,2) DEFAULT 0.00,
  `subtotal_devolucion` decimal(10,2) NOT NULL,
  `motivo_detalle` enum('defecto_fabrica','talla_incorrecta','producto_dañado','insatisfaccion','otros') DEFAULT NULL,
  `condicion_producto` enum('nuevo','usado_bueno','usado_regular','dañado','defectuoso') NOT NULL,
  `accion_producto` enum('reingreso_inventario','reparacion','descarte','devolucion_proveedor') DEFAULT 'reingreso_inventario',
  `id_proveedor_original` int(11) DEFAULT NULL COMMENT 'Proveedor de origen del producto devuelto',
  `observaciones_detalle` text DEFAULT NULL,
  `activo` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Disparadores `devolucion_detalles`
--
DELIMITER $$
CREATE TRIGGER `tr_auditoria_devolucion_insert` AFTER INSERT ON `devolucion_detalles` FOR EACH ROW BEGIN
    DECLARE var_id_proveedor INT;
    DECLARE var_id_usuario INT;
    
    -- Obtener proveedor de la variante
    SELECT pv.id_proveedor INTO var_id_proveedor
    FROM producto_variantes pv
    WHERE pv.id_variante = NEW.id_variante;
    
    -- NOTA: Ya NO actualizamos devolucion_detalles aquí
    -- porque eso causa el error. Si necesitas id_proveedor_original,
    -- debes establecerlo en el INSERT desde el código Java.
    
    -- Obtener usuario que procesa
    SELECT d.id_usuario_procesa INTO var_id_usuario
    FROM devoluciones d
    WHERE d.id_devolucion = NEW.id_devolucion;
    
    -- Registrar en auditoría
    INSERT INTO auditoria_trazabilidad (
        id_variante, id_proveedor, tipo_evento, cantidad,
        id_referencia, tipo_referencia,
        id_usuario, fecha_evento
    ) VALUES (
        NEW.id_variante, var_id_proveedor, 'devolucion', NEW.cantidad_devuelta,
        NEW.id_devolucion, 'devolucion',
        var_id_usuario, NOW()
    );
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `gastos_operativos`
--

CREATE TABLE `gastos_operativos` (
  `id_gasto` int(11) NOT NULL,
  `id_tipo_gasto` int(11) NOT NULL COMMENT 'FK a tipos_gastos',
  `concepto` varchar(255) NOT NULL COMMENT 'Descripción específica del gasto',
  `monto` decimal(10,2) NOT NULL COMMENT 'Monto del gasto',
  `proveedor_persona` varchar(100) DEFAULT NULL COMMENT 'Nombre del proveedor o persona',
  `numero_recibo` varchar(50) DEFAULT NULL COMMENT 'Número de recibo o factura',
  `observaciones` text DEFAULT NULL COMMENT 'Notas adicionales',
  `id_bodega` int(11) NOT NULL COMMENT 'FK a bodegas',
  `id_usuario` int(11) NOT NULL COMMENT 'FK a usuarios (quien registra)',
  `id_movimiento_caja` int(11) DEFAULT NULL COMMENT 'FK a caja_movimiento_detalle para cuadre',
  `estado` enum('registrado','contabilizado','anulado') DEFAULT 'registrado',
  `fecha_gasto` datetime DEFAULT current_timestamp(),
  `fecha_creacion` datetime DEFAULT current_timestamp(),
  `fecha_actualizacion` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Registro de gastos operativos con trazabilidad a caja';

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `historial_cambios_productos`
--

CREATE TABLE `historial_cambios_productos` (
  `id_historial` int(11) NOT NULL,
  `id_producto` int(11) NOT NULL,
  `id_usuario` int(11) NOT NULL,
  `campo_modificado` varchar(50) NOT NULL,
  `valor_anterior` text DEFAULT NULL,
  `valor_nuevo` text DEFAULT NULL,
  `fecha_cambio` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `historial_sesiones`
--

CREATE TABLE `historial_sesiones` (
  `id_historial` int(11) NOT NULL,
  `id_sesion` int(11) DEFAULT NULL,
  `id_usuario` int(11) DEFAULT NULL,
  `accion` enum('login','logout','timeout','forced_logout') NOT NULL,
  `fecha_accion` datetime NOT NULL DEFAULT current_timestamp(),
  `detalles` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `inventario_bodega`
--

CREATE TABLE `inventario_bodega` (
  `id_inventario_bodega` int(11) NOT NULL,
  `id_bodega` int(11) NOT NULL,
  `id_variante` int(11) NOT NULL,
  `Stock_par` int(11) NOT NULL,
  `Stock_caja` int(11) NOT NULL,
  `stock_reservado` int(11) DEFAULT 0,
  `fecha_ultimo_movimiento` datetime DEFAULT NULL,
  `fecha_actualizacion` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `ubicacion_especifica` varchar(100) DEFAULT NULL,
  `activo` int(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `inventario_historial`
--

CREATE TABLE `inventario_historial` (
  `id_historial` int(11) NOT NULL,
  `id_producto` int(11) NOT NULL,
  `id_variante` int(11) DEFAULT NULL,
  `accion` varchar(50) NOT NULL,
  `cantidad` int(11) DEFAULT 0,
  `tipo_stock` enum('par','caja') DEFAULT 'par',
  `id_bodega` int(11) DEFAULT NULL,
  `id_usuario` int(11) NOT NULL,
  `detalles` text DEFAULT NULL,
  `fecha` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Historial detallado de movimientos de inventario';

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `inventario_movimientos`
--

CREATE TABLE `inventario_movimientos` (
  `id_movimiento` int(11) NOT NULL,
  `id_producto` int(11) NOT NULL,
  `id_variante` int(11) DEFAULT NULL,
  `tipo_movimiento` enum('entrada caja','salida caja','ajuste caja','entrada par','salida par','ajuste par','salida_traspaso','entrada_traspaso','conversion_caja_pares','cambio') NOT NULL,
  `cantidad` int(11) NOT NULL,
  `cantidad_pares` int(11) DEFAULT NULL,
  `fecha_movimiento` date DEFAULT NULL,
  `id_referencia` int(11) DEFAULT NULL,
  `tipo_referencia` varchar(21) DEFAULT NULL,
  `id_usuario` int(11) NOT NULL,
  `observaciones` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `marcas`
--

CREATE TABLE `marcas` (
  `id_marca` int(11) NOT NULL,
  `nombre` varchar(50) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `activo` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `notas_credito`
--

CREATE TABLE `notas_credito` (
  `id_nota_credito` int(11) NOT NULL,
  `numero_nota_credito` varchar(50) NOT NULL,
  `id_devolucion` int(11) NOT NULL,
  `id_cliente` int(11) NOT NULL,
  `id_usuario_genera` int(11) NOT NULL,
  `fecha_emision` datetime DEFAULT current_timestamp(),
  `tipo_nota` enum('devolucion','descuento','ajuste','error_facturacion') DEFAULT 'devolucion',
  `subtotal` decimal(10,2) NOT NULL,
  `iva` decimal(10,2) NOT NULL,
  `total` decimal(10,2) NOT NULL,
  `estado` enum('emitida','aplicada','anulada','vencida','generada','consumida') DEFAULT 'emitida',
  `fecha_vencimiento` datetime NOT NULL,
  `saldo_disponible` decimal(10,2) NOT NULL,
  `saldo_usado` decimal(10,2) DEFAULT 0.00,
  `observaciones` text DEFAULT NULL,
  `id_venta_aplicada` int(11) DEFAULT NULL,
  `fecha_aplicacion` datetime DEFAULT NULL,
  `activa` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `notificaciones`
--

CREATE TABLE `notificaciones` (
  `id_notificacion` int(11) NOT NULL,
  `titulo` varchar(200) NOT NULL,
  `mensaje` text NOT NULL,
  `tipo` enum('info','warning','error','success','urgent') DEFAULT 'info',
  `categoria` enum('stock','ventas','compras','sistema','inventario','usuarios','Orden web') DEFAULT 'sistema',
  `id_usuario_destinatario` int(11) DEFAULT NULL,
  `para_todos` tinyint(1) DEFAULT 0,
  `leida` tinyint(1) DEFAULT 0,
  `fecha_creacion` datetime DEFAULT current_timestamp(),
  `fecha_lectura` datetime DEFAULT NULL,
  `id_referencia` int(11) DEFAULT NULL,
  `tipo_referencia` varchar(50) DEFAULT NULL,
  `activa` tinyint(1) DEFAULT 1,
  `id_bodega_destino` int(11) DEFAULT NULL,
  `id_bodega_origen` int(11) DEFAULT NULL,
  `evento` varchar(30) DEFAULT NULL,
  `datos_adicionales` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'Datos adicionales de la notificación, como URLs de instalador' CHECK (json_valid(`datos_adicionales`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `ordenes_reserva`
--

CREATE TABLE `ordenes_reserva` (
  `id_orden` int(11) NOT NULL,
  `id_usuario` int(11) NOT NULL,
  `id_bodega` int(11) NOT NULL,
  `direccion` text DEFAULT NULL,
  `ciudad` varchar(100) DEFAULT NULL,
  `departamento` varchar(100) DEFAULT NULL,
  `codigo_postal` varchar(20) DEFAULT NULL,
  `pais` varchar(100) DEFAULT NULL,
  `subtotal` decimal(10,2) DEFAULT 0.00,
  `impuestos` decimal(10,2) DEFAULT 0.00,
  `total` decimal(10,2) DEFAULT 0.00,
  `metodo_pago` varchar(50) DEFAULT NULL,
  `notas` text DEFAULT NULL,
  `fecha_creacion` datetime DEFAULT current_timestamp(),
  `fecha_vencimiento` datetime DEFAULT NULL,
  `estado` enum('pendiente','retirado','pagado','finalizado','cancelado') NOT NULL DEFAULT 'pendiente',
  `fecha_retirado` datetime DEFAULT NULL,
  `fecha_pagado` datetime DEFAULT NULL,
  `fecha_finalizado` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `ordenes_reserva_detalle`
--

CREATE TABLE `ordenes_reserva_detalle` (
  `id_detalle` int(11) NOT NULL,
  `id_orden` int(11) NOT NULL,
  `id_producto` int(11) NOT NULL,
  `id_variante` int(11) DEFAULT NULL,
  `id_bodega` int(11) DEFAULT NULL,
  `cantidad` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `permisos`
--

CREATE TABLE `permisos` (
  `id_permiso` int(11) NOT NULL,
  `modulo` varchar(100) NOT NULL,
  `nombre_mostrar` varchar(100) DEFAULT NULL,
  `descripcion` varchar(255) DEFAULT NULL,
  `categoria` varchar(50) DEFAULT NULL,
  `requiere_admin` tinyint(1) DEFAULT 0,
  `activo` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `prestamos_zapatos`
--

CREATE TABLE `prestamos_zapatos` (
  `id_prestamo` int(11) NOT NULL,
  `id_bodega` int(11) NOT NULL,
  `id_producto` int(11) NOT NULL,
  `id_variante` int(11) NOT NULL,
  `pie` enum('DERECHO','IZQUIERDO','AMBOS') NOT NULL,
  `nombre_prestatario` varchar(100) NOT NULL,
  `celular_prestatario` varchar(20) NOT NULL,
  `direccion_prestatario` text DEFAULT NULL,
  `estado` enum('PRESTADO','DEVUELTO','PERDIDO','DANADO') NOT NULL DEFAULT 'PRESTADO',
  `fecha_prestamo` datetime DEFAULT current_timestamp(),
  `fecha_devolucion` datetime DEFAULT NULL,
  `id_usuario` int(11) DEFAULT NULL,
  `observaciones` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `privilegios_rol`
--

CREATE TABLE `privilegios_rol` (
  `id_privilegio` int(11) NOT NULL,
  `puede_ver` tinyint(1) DEFAULT 0,
  `puede_crear` tinyint(1) DEFAULT 0,
  `puede_editar` tinyint(1) DEFAULT 0,
  `puede_eliminar` tinyint(1) DEFAULT 0,
  `id_rol` int(11) DEFAULT NULL,
  `id_permiso` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `privilegio_rol`
--

CREATE TABLE `privilegio_rol` (
  `id_privilegio` int(11) NOT NULL,
  `id_rol` int(11) NOT NULL,
  `id_permiso` int(11) NOT NULL,
  `puede_ver` tinyint(1) DEFAULT 0,
  `puede_crear` tinyint(1) DEFAULT 0,
  `puede_editar` tinyint(1) DEFAULT 0,
  `puede_eliminar` tinyint(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `privilegio_usuario`
--

CREATE TABLE `privilegio_usuario` (
  `id_privilegio_usuario` int(11) NOT NULL,
  `id_usuario` int(11) NOT NULL,
  `id_permiso` int(11) NOT NULL,
  `puede_ver` tinyint(1) DEFAULT 0,
  `puede_crear` tinyint(1) DEFAULT 0,
  `puede_editar` tinyint(1) DEFAULT 0,
  `puede_eliminar` tinyint(1) DEFAULT 0,
  `fecha_asignacion` datetime DEFAULT current_timestamp(),
  `id_usuario_asignador` int(11) DEFAULT NULL COMMENT 'Quién asignó este permiso'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Permisos personalizados asignados directamente a usuarios (sobrescriben permisos de rol)';

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `productos`
--

CREATE TABLE `productos` (
  `id_producto` int(11) NOT NULL,
  `codigo_modelo` varchar(50) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `id_categoria` int(11) NOT NULL,
  `id_marca` int(11) NOT NULL,
  `id_proveedor` int(11) DEFAULT NULL,
  `precio_compra` decimal(10,2) NOT NULL,
  `precio_venta` decimal(10,2) NOT NULL,
  `stock_minimo` int(11) DEFAULT 5,
  `talla` varchar(10) DEFAULT NULL,
  `color` varchar(100) DEFAULT NULL,
  `genero` enum('MUJER','HOMBRE','NIÃO','UNISEX') NOT NULL,
  `activo` tinyint(1) DEFAULT 1,
  `ubicacion` enum('bodega','tienda') DEFAULT 'bodega',
  `fecha_creacion` datetime DEFAULT current_timestamp(),
  `fecha_actualizacion` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `pares_por_caja` int(11) DEFAULT 24,
  `ubicacion_bodega` varchar(100) DEFAULT NULL,
  `ubicacion_tienda` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `productos_cache`
--

CREATE TABLE `productos_cache` (
  `id_producto` int(11) NOT NULL,
  `codigo_modelo` varchar(50) DEFAULT NULL,
  `nombre` varchar(255) DEFAULT NULL,
  `descripcion` text DEFAULT NULL,
  `precio_venta` decimal(10,2) DEFAULT NULL,
  `genero` varchar(20) DEFAULT NULL,
  `activo` tinyint(1) DEFAULT NULL,
  `fecha_creacion` datetime DEFAULT NULL,
  `id_marca` int(11) DEFAULT NULL,
  `id_categoria` int(11) DEFAULT NULL,
  `total_variantes` int(11) DEFAULT NULL,
  `variantes_disponibles` int(11) DEFAULT NULL,
  `total_colores` int(11) DEFAULT NULL,
  `total_tallas` int(11) DEFAULT NULL,
  `stock_total` int(11) DEFAULT NULL,
  `stock_disponible` int(11) DEFAULT NULL,
  `imagen_principal_id` int(11) DEFAULT NULL,
  `color_imagen_principal` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `productos_genericos`
--

CREATE TABLE `productos_genericos` (
  `id_generico` int(11) NOT NULL,
  `id_variante` int(11) NOT NULL COMMENT 'FK a producto_variantes',
  `tipo_origen` enum('compra_externa','importacion','otro') DEFAULT 'compra_externa',
  `tienda_origen` varchar(100) DEFAULT NULL COMMENT 'Tienda donde se compró',
  `id_usuario_crea` int(11) NOT NULL COMMENT 'Usuario que creó la variante',
  `fecha_creacion` datetime DEFAULT current_timestamp(),
  `fecha_primer_uso` datetime DEFAULT NULL COMMENT 'Primera vez que se vendió',
  `observaciones` text DEFAULT NULL,
  `descripcion_compra` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Rastreo de variantes genéricas creadas automáticamente';

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `producto_variantes`
--

CREATE TABLE `producto_variantes` (
  `id_variante` int(11) NOT NULL,
  `id_producto` int(11) NOT NULL,
  `id_talla` int(11) NOT NULL,
  `id_color` int(11) NOT NULL,
  `id_proveedor` int(11) DEFAULT NULL COMMENT 'FK al proveedor que suministra esta variante (talla/color)',
  `imagen` mediumblob DEFAULT NULL,
  `ean` varchar(20) DEFAULT NULL,
  `sku` varchar(50) NOT NULL,
  `precio_compra` decimal(10,2) DEFAULT NULL,
  `precio_venta` decimal(10,2) DEFAULT NULL,
  `stock_minimo_variante` int(11) DEFAULT NULL,
  `disponible` tinyint(1) DEFAULT 1,
  `fecha_creacion` datetime DEFAULT current_timestamp(),
  `fecha_actualizacion` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Disparadores `producto_variantes`
--
DELIMITER $$
CREATE TRIGGER `trg_pv_insert_ib` AFTER INSERT ON `producto_variantes` FOR EACH ROW BEGIN
  INSERT INTO inventario_bodega (id_bodega, id_variante, Stock_par, Stock_caja, stock_reservado, fecha_ultimo_movimiento, ubicacion_especifica, activo)
  SELECT b.id_bodega, NEW.id_variante, 0, 0, 0, NOW(), NULL, 1
  FROM bodegas b
  WHERE b.activa = 1;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `promociones`
--

CREATE TABLE `promociones` (
  `id_promocion` int(11) NOT NULL,
  `codigo` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_spanish_ci NOT NULL COMMENT 'Código único (cupón/etiqueta)',
  `nombre` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_spanish_ci NOT NULL,
  `descripcion` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_spanish_ci DEFAULT NULL,
  `tipo_descuento` enum('PORCENTAJE','MONTO_FIJO') CHARACTER SET utf8mb4 COLLATE utf8mb4_spanish_ci NOT NULL,
  `valor_descuento` decimal(10,2) NOT NULL,
  `fecha_inicio` datetime NOT NULL,
  `fecha_fin` datetime NOT NULL,
  `activa` tinyint(1) NOT NULL DEFAULT 1,
  `min_compra` decimal(12,2) NOT NULL DEFAULT 0.00,
  `limite_uso_total` int(11) DEFAULT NULL,
  `limite_uso_por_usuario` int(11) DEFAULT NULL,
  `creado_en` datetime NOT NULL DEFAULT current_timestamp(),
  `actualizado_en` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `promociones_detalle`
--

CREATE TABLE `promociones_detalle` (
  `id_detalle` int(11) NOT NULL,
  `id_promocion` int(11) NOT NULL,
  `tipo_aplicacion` enum('CATEGORIA','MARCA','PRODUCTO','ROL_USUARIO','USUARIO') NOT NULL,
  `id_categoria` int(11) DEFAULT NULL,
  `id_marca` int(11) DEFAULT NULL,
  `id_producto` int(11) DEFAULT NULL,
  `id_usuario` int(11) DEFAULT NULL,
  `rol_usuario` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_spanish_ci DEFAULT NULL,
  `activo` tinyint(1) NOT NULL DEFAULT 1,
  `creado_en` datetime NOT NULL DEFAULT current_timestamp(),
  `target_count` int(11) GENERATED ALWAYS AS ((`id_categoria` is not null) + (`id_marca` is not null) + (`id_producto` is not null) + (`rol_usuario` is not null) + (`id_usuario` is not null)) STORED
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `proveedores`
--

CREATE TABLE `proveedores` (
  `id_proveedor` int(11) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `ruc` varchar(20) DEFAULT NULL,
  `direccion` text DEFAULT NULL,
  `telefono` varchar(20) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `activo` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `rentabilidad_mensual`
--

CREATE TABLE `rentabilidad_mensual` (
  `id_rentabilidad` int(11) NOT NULL,
  `periodo` date NOT NULL,
  `ventas_totales` decimal(12,2) NOT NULL,
  `costo_productos` decimal(12,2) NOT NULL,
  `margen_bruto` decimal(12,2) NOT NULL,
  `gastos_operativos` decimal(12,2) NOT NULL,
  `utilidad_neta` decimal(12,2) NOT NULL,
  `margen_neto` decimal(5,2) NOT NULL,
  `rotacion_inventario` decimal(5,2) DEFAULT NULL,
  `fecha_calculo` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `reservas`
--

CREATE TABLE `reservas` (
  `id_reserva` int(11) NOT NULL,
  `numero_reserva` varchar(50) NOT NULL,
  `id_cliente` int(11) NOT NULL,
  `id_usuario` int(11) NOT NULL,
  `fecha_reserva` datetime DEFAULT current_timestamp(),
  `fecha_vencimiento` datetime NOT NULL,
  `estado` enum('activa','vencida','cancelada','completada') DEFAULT 'activa',
  `monto_anticipo` decimal(10,2) DEFAULT 0.00,
  `total_reserva` decimal(10,2) NOT NULL,
  `observaciones` text DEFAULT NULL,
  `id_venta_generada` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `roles`
--

CREATE TABLE `roles` (
  `id_rol` int(11) NOT NULL,
  `nombre` varchar(50) NOT NULL,
  `descripcion` varchar(255) DEFAULT NULL,
  `activo` tinyint(1) DEFAULT 1,
  `es_sistema` tinyint(1) DEFAULT 0,
  `fecha_creacion` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `sesiones_activas`
--

CREATE TABLE `sesiones_activas` (
  `id_sesion` int(11) NOT NULL,
  `id_usuario` int(11) NOT NULL,
  `token_sesion` varchar(255) NOT NULL,
  `ip_address` varchar(50) DEFAULT NULL,
  `hostname` varchar(100) DEFAULT NULL,
  `fecha_inicio` datetime NOT NULL DEFAULT current_timestamp(),
  `fecha_ultimo_ping` datetime NOT NULL DEFAULT current_timestamp(),
  `id_caja_abierta` int(11) DEFAULT NULL,
  `id_movimiento_caja` int(11) DEFAULT NULL,
  `estado` enum('activa','bloqueada','cerrada') DEFAULT 'activa',
  `navegador` varchar(100) DEFAULT NULL,
  `sistema_operativo` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `tallas`
--

CREATE TABLE `tallas` (
  `id_talla` int(11) NOT NULL,
  `numero` varchar(10) NOT NULL,
  `sistema` enum('EU','US','UK','CM') DEFAULT 'EU',
  `equivalencia_eu` decimal(4,1) DEFAULT NULL,
  `equivalencia_us` decimal(4,1) DEFAULT NULL,
  `equivalencia_uk` decimal(4,1) DEFAULT NULL,
  `equivalencia_cm` decimal(4,1) DEFAULT NULL,
  `genero` enum('HOMBRE','MUJER','NIÑO','UNISEX') NOT NULL,
  `activo` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `tallas_bak`
--

CREATE TABLE `tallas_bak` (
  `id_talla` int(11) NOT NULL,
  `numero` varchar(10) NOT NULL,
  `sistema` enum('EU','US','UK','CM') DEFAULT 'EU',
  `equivalencia_eu` decimal(4,1) DEFAULT NULL,
  `equivalencia_us` decimal(4,1) DEFAULT NULL,
  `equivalencia_uk` decimal(4,1) DEFAULT NULL,
  `equivalencia_cm` decimal(4,1) DEFAULT NULL,
  `genero` enum('MUJER','HOMBRE','NIÃO','UNISEX') NOT NULL,
  `activo` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `tipos_gastos`
--

CREATE TABLE `tipos_gastos` (
  `id_tipo_gasto` int(11) NOT NULL,
  `codigo` varchar(20) NOT NULL COMMENT 'Código único del tipo de gasto',
  `nombre` varchar(100) NOT NULL COMMENT 'Nombre del tipo de gasto',
  `descripcion` text DEFAULT NULL COMMENT 'Descripción detallada',
  `categoria` enum('operativo','administrativo','financiero','otro') DEFAULT 'operativo',
  `requiere_autorizacion` tinyint(1) DEFAULT 0 COMMENT '1 si requiere autorización previa',
  `monto_maximo_sin_autorizacion` decimal(10,2) DEFAULT 100000.00 COMMENT 'Monto máximo sin autorización',
  `cuenta_contable` varchar(20) DEFAULT NULL COMMENT 'Código de cuenta contable',
  `activo` tinyint(1) DEFAULT 1 COMMENT '1=activo, 0=inactivo',
  `fecha_creacion` datetime DEFAULT current_timestamp(),
  `fecha_actualizacion` datetime DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Catálogo de tipos de gastos parametrizables';

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `traspasos`
--

CREATE TABLE `traspasos` (
  `id_traspaso` int(11) NOT NULL,
  `numero_traspaso` varchar(50) NOT NULL,
  `id_bodega_origen` int(11) NOT NULL,
  `id_bodega_destino` int(11) NOT NULL,
  `id_usuario_solicita` int(11) NOT NULL,
  `id_usuario_autoriza` int(11) DEFAULT NULL,
  `id_usuario_recibe` int(11) DEFAULT NULL,
  `fecha_solicitud` datetime DEFAULT current_timestamp(),
  `fecha_autorizacion` datetime DEFAULT NULL,
  `fecha_envio` datetime DEFAULT NULL,
  `fecha_recepcion` datetime DEFAULT NULL,
  `estado` enum('pendiente','autorizado','en_transito','recibido','cancelado') DEFAULT 'pendiente',
  `motivo` text DEFAULT NULL,
  `observaciones` text DEFAULT NULL,
  `total_productos` int(11) DEFAULT 0,
  `monto_total` decimal(10,2) NOT NULL DEFAULT 0.00,
  `monto_recibido` decimal(10,2) NOT NULL DEFAULT 0.00
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Disparadores `traspasos`
--
DELIMITER $$
CREATE TRIGGER `trg_traspasos_ai` AFTER INSERT ON `traspasos` FOR EACH ROW BEGIN
  CALL sp_notificar_traspaso_evento(
    NEW.id_traspaso,
    'solicitud',
    CONCAT('Solicitud de traspaso #', NEW.id_traspaso),
    'Se requiere autorización y envío'
  );
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_traspasos_au_estado` AFTER UPDATE ON `traspasos` FOR EACH ROW BEGIN
  IF NEW.estado <> OLD.estado THEN
    IF NEW.estado = 'autorizado' THEN
      CALL sp_notificar_traspaso_evento(NEW.id_traspaso, 'autorizado', CONCAT('Traspaso #', NEW.id_traspaso, ' autorizado'), 'Autorizado por bodega destino. Listo para envío.');
    ELSEIF NEW.estado IN ('enviado','en_transito') THEN
      CALL sp_notificar_traspaso_evento(NEW.id_traspaso, 'enviado', CONCAT('Traspaso #', NEW.id_traspaso, ' enviado'), 'En tránsito hacia bodega destino');
    ELSEIF NEW.estado = 'recibido' THEN
      CALL sp_notificar_traspaso_evento(NEW.id_traspaso, 'recibido', CONCAT('Traspaso #', NEW.id_traspaso, ' recibido'), 'Recepción registrada en bodega destino');
    ELSEIF NEW.estado IN ('rechazado','cancelado') THEN
      CALL sp_notificar_traspaso_evento(NEW.id_traspaso, NEW.estado, CONCAT('Traspaso #', NEW.id_traspaso, ' ', NEW.estado), 'Actualización de estado');
    END IF;
    CALL sp_sync_notif_traspasos();
  END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `traspaso_auditoria`
--

CREATE TABLE `traspaso_auditoria` (
  `id_auditoria` int(11) NOT NULL,
  `id_traspaso` int(11) NOT NULL,
  `accion` varchar(50) NOT NULL,
  `fecha_accion` datetime NOT NULL DEFAULT current_timestamp(),
  `id_usuario` int(11) DEFAULT NULL,
  `observaciones` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `traspaso_detalles`
--

CREATE TABLE `traspaso_detalles` (
  `id_detalle_traspaso` int(11) NOT NULL,
  `id_traspaso` int(11) NOT NULL,
  `id_producto` int(11) NOT NULL,
  `id_variante` int(11) DEFAULT NULL,
  `Tipo` varchar(5) NOT NULL,
  `cantidad_solicitada` int(11) NOT NULL,
  `cantidad_enviada` int(11) DEFAULT 0,
  `cantidad_recibida` int(11) DEFAULT 0,
  `observaciones` text DEFAULT NULL,
  `estado_detalle` enum('pendiente','enviado','recibido','faltante') DEFAULT 'pendiente',
  `precio_unitario` decimal(10,2) NOT NULL DEFAULT 0.00,
  `subtotal` decimal(10,2) NOT NULL DEFAULT 0.00
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Disparadores `traspaso_detalles`
--
DELIMITER $$
CREATE TRIGGER `tr_auditoria_traspaso_insert` AFTER INSERT ON `traspaso_detalles` FOR EACH ROW BEGIN
    DECLARE var_id_proveedor INT;
    DECLARE var_id_usuario INT;
    
    -- Obtener proveedor de la variante
    SELECT pv.id_proveedor INTO var_id_proveedor
    FROM producto_variantes pv
    WHERE pv.id_variante = NEW.id_variante;
    
    -- Obtener usuario que solicita
    SELECT t.id_usuario_solicita INTO var_id_usuario
    FROM traspasos t
    WHERE t.id_traspaso = NEW.id_traspaso;
    
    -- Registrar SALIDA de bodega origen
    INSERT INTO auditoria_trazabilidad (
        id_variante, id_proveedor, tipo_evento, cantidad,
        id_bodega_origen, id_bodega_destino, 
        id_referencia, tipo_referencia,
        id_usuario, fecha_evento
    ) SELECT 
        NEW.id_variante, var_id_proveedor, 'traspaso_salida', NEW.cantidad_solicitada,
        t.id_bodega_origen, t.id_bodega_destino,
        NEW.id_traspaso, 'traspaso',
        var_id_usuario, NOW()
    FROM traspasos t WHERE t.id_traspaso = NEW.id_traspaso;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `usuarios`
--

CREATE TABLE `usuarios` (
  `id_usuario` int(11) NOT NULL,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `email` varchar(100) DEFAULT NULL,
  `telefono` varchar(20) DEFAULT NULL,
  `direccion` varchar(255) DEFAULT NULL,
  `rol` varchar(50) NOT NULL,
  `ubicacion` enum('tienda','bodega') NOT NULL DEFAULT 'tienda',
  `activo` tinyint(1) DEFAULT 1,
  `fecha_creacion` datetime DEFAULT current_timestamp(),
  `fecha_actualizacion` datetime DEFAULT NULL,
  `id_bodega` int(11) NOT NULL,
  `id_rol` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `ventas`
--

CREATE TABLE `ventas` (
  `id_venta` int(11) NOT NULL,
  `id_cliente` int(11) DEFAULT NULL,
  `id_usuario` int(11) NOT NULL,
  `id_caja` int(11) NOT NULL,
  `id_movimiento` int(11) NOT NULL,
  `fecha_venta` timestamp NULL DEFAULT NULL,
  `subtotal` decimal(10,2) NOT NULL,
  `descuento` decimal(10,2) DEFAULT 0.00,
  `iva` decimal(10,2) NOT NULL,
  `total` decimal(10,2) NOT NULL,
  `estado` enum('pendiente','completada','cancelada') DEFAULT 'completada',
  `tipo_pago` enum('efectivo','tarjeta','transferencia','sistecredito','nota_credito','mixto','credito') NOT NULL DEFAULT 'efectivo',
  `observaciones` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `venta_detalles`
--

CREATE TABLE `venta_detalles` (
  `id_detalle` int(11) NOT NULL,
  `id_venta` int(11) NOT NULL,
  `id_producto` int(11) NOT NULL,
  `id_variante` int(11) DEFAULT NULL,
  `cantidad` int(11) NOT NULL,
  `precio_unitario` decimal(10,2) NOT NULL,
  `descuento` decimal(10,2) DEFAULT 0.00,
  `subtotal` decimal(10,2) NOT NULL,
  `tipo_venta` varchar(50) DEFAULT NULL COMMENT 'Tipo de venta (par, caja, etc)',
  `activo` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Disparadores `venta_detalles`
--
DELIMITER $$
CREATE TRIGGER `tr_auditoria_venta_insert` AFTER INSERT ON `venta_detalles` FOR EACH ROW BEGIN
    DECLARE var_id_bodega INT;
    DECLARE var_id_proveedor INT;
    
    -- Obtener bodega de la caja
    SELECT cb.id_bodega INTO var_id_bodega
    FROM cajas cb
    JOIN ventas v ON v.id_caja = cb.id_caja
    WHERE v.id_venta = NEW.id_venta
    LIMIT 1;
    
    -- Obtener proveedor de la variante
    SELECT pv.id_proveedor INTO var_id_proveedor
    FROM producto_variantes pv
    WHERE pv.id_variante = NEW.id_variante;
    
    -- Registrar en auditoría
    INSERT INTO auditoria_trazabilidad (
        id_variante, id_proveedor, tipo_evento, cantidad,
        id_bodega_origen, id_referencia, tipo_referencia,
        id_usuario, fecha_evento
    ) SELECT 
        NEW.id_variante, var_id_proveedor, 'venta', NEW.cantidad,
        var_id_bodega, NEW.id_venta, 'venta',
        v.id_usuario, NOW()
    FROM ventas v WHERE v.id_venta = NEW.id_venta;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `venta_medios_pago`
--

CREATE TABLE `venta_medios_pago` (
  `id_medio_pago` int(11) NOT NULL,
  `id_venta` int(11) NOT NULL,
  `tipo_pago` enum('efectivo','tarjeta_credito','tarjeta_debito','transferencia','nota_credito','otro','sistecredito') NOT NULL DEFAULT 'efectivo',
  `monto` decimal(15,2) NOT NULL,
  `numero_referencia` varchar(100) DEFAULT NULL COMMENT 'Número de autorización/transacción',
  `observaciones` text DEFAULT NULL,
  `fecha_registro` datetime NOT NULL DEFAULT current_timestamp(),
  `activo` tinyint(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Medios de pago utilizados en cada venta';

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `venta_pagos`
--

CREATE TABLE `venta_pagos` (
  `id_pago` int(11) NOT NULL,
  `id_venta` int(11) NOT NULL,
  `tipo_pago` enum('efectivo','tarjeta_credito','tarjeta_debito','transferencia','cheque','credito') NOT NULL,
  `monto` decimal(10,2) NOT NULL,
  `referencia` varchar(100) DEFAULT NULL,
  `banco` varchar(100) DEFAULT NULL,
  `numero_autorizacion` varchar(50) DEFAULT NULL,
  `fecha_pago` datetime DEFAULT current_timestamp(),
  `estado` enum('pendiente','aprobado','rechazado') DEFAULT 'aprobado',
  `observaciones` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `vista_ajustes_pendientes`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `vista_ajustes_pendientes` (
`id_ajuste` int(11)
,`id_detalle_conteo` int(11)
,`id_producto` int(11)
,`codigo_barras` varchar(20)
,`producto` varchar(100)
,`tipo` varchar(50)
,`tipo_ajuste` enum('entrada caja','salida caja','ajuste caja','entrada par','salida par','ajuste par')
,`cantidad` int(11)
,`razon` enum('error_conteo','perdida','deterioro','error_registro','otra')
,`observaciones` text
,`creado_por` varchar(100)
,`fecha_creacion` datetime
,`stock_sistema` int(11)
,`stock_contado` int(11)
,`diferencia` int(11)
,`producto_completo` varchar(166)
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `vista_compras_completas`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `vista_compras_completas` (
`id_compra` int(11)
,`numero_compra` varchar(50)
,`fecha_compra` date
,`fecha_recepcion` datetime
,`numero_factura` varchar(50)
,`subtotal` decimal(10,2)
,`iva` decimal(10,2)
,`total` decimal(10,2)
,`estado` enum('pendiente','recibida','cancelada')
,`observaciones` text
,`id_proveedor` int(11)
,`nombre_proveedor` varchar(100)
,`ruc_proveedor` varchar(20)
,`id_bodega` int(11)
,`nombre_bodega` varchar(100)
,`id_usuario` int(11)
,`nombre_usuario` varchar(100)
,`total_items` bigint(21)
,`total_unidades` decimal(32,0)
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `vista_compra_detalles`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `vista_compra_detalles` (
`id_detalle_compra` int(11)
,`id_compra` int(11)
,`id_producto` int(11)
,`id_variante` int(11)
,`cantidad` int(11)
,`tipo_unidad` enum('par','caja')
,`precio_unitario` decimal(10,2)
,`subtotal` decimal(10,2)
,`codigo_modelo` varchar(50)
,`nombre_producto` varchar(100)
,`talla` varchar(10)
,`color` varchar(50)
,`sku` varchar(50)
,`ean` varchar(20)
,`precio_venta` decimal(10,2)
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `vista_conteos_activos`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `vista_conteos_activos` (
`id_conteo` int(1)
,`nombre` varchar(14)
,`tipo_conteo` varchar(7)
,`tipo` varchar(5)
,`estado` varchar(9)
,`fecha_programada` datetime /* mariadb-5.3 */
,`hora_programada` time /* mariadb-5.3 */
,`responsable` varchar(7)
,`observaciones` varchar(17)
,`productos_contados` bigint(21)
,`total_productos` bigint(21)
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `vista_cuadre_caja_diario`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `vista_cuadre_caja_diario` (
`id_caja_menor` int(11)
,`codigo_caja_menor` varchar(20)
,`nombre_caja_menor` varchar(100)
,`fecha_cuadre` date
,`saldo_inicial` decimal(10,2)
,`total_entradas` decimal(32,2)
,`total_salidas` decimal(32,2)
,`saldo_final_calculado` decimal(34,2)
,`saldo_final_real` decimal(10,2)
,`diferencia_caja` decimal(35,2)
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `vista_devoluciones_completas`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `vista_devoluciones_completas` (
`id_devolucion` int(11)
,`numero_devolucion` varchar(50)
,`fecha_devolucion` datetime
,`tipo_devolucion` enum('total','parcial')
,`motivo` enum('defecto_fabrica','talla_incorrecta','producto_dañado','insatisfaccion','error_facturacion','cambio_modelo','otros')
,`estado` enum('pendiente','procesando','aprobada','rechazada','finalizada','anulada')
,`total_devolucion` decimal(10,2)
,`fecha_venta` timestamp
,`total_venta_original` decimal(10,2)
,`cliente` varchar(100)
,`dni` varchar(20)
,`telefono` varchar(20)
,`usuario_procesa` varchar(100)
,`usuario_autoriza` varchar(100)
,`total_items` bigint(21)
,`total_cantidad_devuelta` decimal(32,0)
,`numero_nota_credito` varchar(50)
,`saldo_disponible` decimal(10,2)
,`estado_nota_credito` enum('emitida','aplicada','anulada','vencida','generada','consumida')
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `vista_ganancias_producto`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `vista_ganancias_producto` (
`id_producto` int(11)
,`nombre` varchar(100)
,`codigo_barras` varchar(20)
,`id_marca` int(11)
,`id_categoria` int(11)
,`precio_compra` decimal(10,2)
,`precio_venta` decimal(10,2)
,`ganancia_unitaria` decimal(11,2)
,`num_ventas` int(1)
,`unidades_vendidas` int(1)
,`costo_total` int(1)
,`ventas_total` int(1)
,`ganancia_total` int(1)
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `vista_inventario_cajas`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `vista_inventario_cajas` (
`id_producto` int(11)
,`nombre` varchar(100)
,`codigo_barras` varchar(20)
,`stock` decimal(32,0)
,`costo` decimal(10,2)
,`ubicacion` enum('bodega','tienda')
,`activo` tinyint(1)
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `vista_inventario_pares`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `vista_inventario_pares` (
`id_producto` int(11)
,`nombre` varchar(100)
,`codigo_barras` varchar(20)
,`stock` decimal(32,0)
,`precio` decimal(10,2)
,`ubicacion` enum('bodega','tienda')
,`activo` tinyint(1)
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `vista_inventario_pares_con_prestamos`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `vista_inventario_pares_con_prestamos` (
`id_producto` int(11)
,`nombre` varchar(100)
,`codigo_barras` varchar(20)
,`id_marca` int(11)
,`id_categoria` int(11)
,`descripcion` text
,`stock` decimal(24,0)
,`precio` decimal(10,2)
,`ubicacion` enum('bodega','tienda')
,`activo` tinyint(1)
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `vista_inventario_por_ubicacion`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `vista_inventario_por_ubicacion` (
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `vista_notas_credito_activas`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `vista_notas_credito_activas` (
`id_nota_credito` int(11)
,`numero_nota_credito` varchar(50)
,`fecha_emision` datetime
,`fecha_vencimiento` datetime
,`total` decimal(10,2)
,`saldo_disponible` decimal(10,2)
,`saldo_usado` decimal(10,2)
,`estado` enum('emitida','aplicada','anulada','vencida','generada','consumida')
,`cliente` varchar(100)
,`dni` varchar(20)
,`numero_devolucion` varchar(50)
,`dias_para_vencer` int(8)
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `vista_productos_completos`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `vista_productos_completos` (
`id_producto` int(11)
,`codigo_modelo` varchar(50)
,`nombre` varchar(100)
,`descripcion` text
,`precio_venta` decimal(10,2)
,`genero` enum('MUJER','HOMBRE','NIÃO','UNISEX')
,`activo` tinyint(1)
,`id_marca` int(11)
,`id_categoria` int(11)
,`id_variante` int(11)
,`id_color` int(11)
,`id_talla` int(11)
,`disponible` tinyint(1)
,`imagen` mediumblob
,`sku` varchar(50)
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `vista_productos_resumen`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `vista_productos_resumen` (
`id_producto` int(11)
,`codigo_modelo` varchar(50)
,`nombre` varchar(100)
,`descripcion` text
,`precio_venta` decimal(10,2)
,`genero` enum('MUJER','HOMBRE','NIÃO','UNISEX')
,`activo` tinyint(1)
,`id_categoria` int(11)
,`id_marca` int(11)
,`fecha_creacion` datetime
,`total_variantes` bigint(21)
,`variantes_disponibles` bigint(21)
,`total_colores` bigint(21)
,`total_tallas` bigint(21)
,`stock_total` decimal(33,0)
,`stock_disponible` decimal(33,0)
,`imagen_principal_id` int(11)
,`color_imagen_principal` varchar(50)
,`precio_min_variante` decimal(10,2)
,`precio_max_variante` decimal(10,2)
,`total_ventas` decimal(32,0)
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `vista_productos_stock_bajo`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `vista_productos_stock_bajo` (
`id_producto` int(11)
,`nombre` varchar(100)
,`codigo_barras` varchar(20)
,`stock_minimo` int(11)
,`stock_por_cajas` decimal(32,0)
,`stock_por_pares` decimal(32,0)
,`estado_stock` varchar(19)
,`ubicacion` enum('bodega','tienda')
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `vista_rentabilidad_diaria`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `vista_rentabilidad_diaria` (
`fecha_venta` date
,`ventas_totales` int(1)
,`costo_productos` int(1)
,`ganancia_bruta` int(1)
,`gastos_operativos` int(1)
,`utilidad_neta` int(1)
,`margen_bruto_porcentaje` int(1)
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `vista_tallas_disponibles`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `vista_tallas_disponibles` (
`id_talla` int(11)
,`numero` varchar(10)
,`sistema` enum('EU','US','UK','CM')
,`equivalencia_eu` decimal(4,1)
,`equivalencia_us` decimal(4,1)
,`equivalencia_uk` decimal(4,1)
,`equivalencia_cm` decimal(4,1)
,`genero` enum('HOMBRE','MUJER','NIÑO','UNISEX')
,`productos_disponibles` bigint(21)
,`productos_en_stock` bigint(21)
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `vw_productos_stock_pares`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `vw_productos_stock_pares` (
`id_producto` int(11)
,`nombre` varchar(100)
,`stock_pares_total` decimal(43,0)
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `vw_variantes_stock_pares`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `vw_variantes_stock_pares` (
`id_variante` int(11)
,`id_producto` int(11)
,`id_talla` int(11)
,`id_color` int(11)
,`id_bodega` int(11)
,`stock_pares_equivalentes` bigint(22)
,`stock_por_pares` int(11)
,`stock_por_cajas` int(11)
,`pares_por_caja` int(11)
,`precio_compra` decimal(10,2)
,`precio_venta` decimal(10,2)
,`sku` varchar(50)
,`ean` varchar(20)
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `v_auditoria_por_proveedor`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `v_auditoria_por_proveedor` (
`id_proveedor` int(11)
,`proveedor` varchar(100)
,`sku` varchar(50)
,`producto` varchar(100)
,`tipo_evento` enum('venta','traspaso_salida','traspaso_entrada','devolucion','compra','ajuste')
,`num_eventos` bigint(21)
,`total_cantidad` decimal(32,0)
,`ultimo_evento` datetime
,`primer_evento` datetime
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `v_cajas_convertibles_por_traspaso`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `v_cajas_convertibles_por_traspaso` (
`numero_traspaso` varchar(50)
,`id_bodega_destino` int(11)
,`id_detalle_traspaso` int(11)
,`id_variante_caja` int(11)
,`id_color` int(11)
,`color_nombre` varchar(50)
,`id_producto` int(11)
,`producto_nombre` varchar(100)
,`genero` enum('MUJER','HOMBRE','NIÃO','UNISEX')
,`talla_numero` varchar(10)
,`cajas_convertibles` decimal(33,0)
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `v_inventario_por_proveedor`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `v_inventario_por_proveedor` (
`id_proveedor` int(11)
,`proveedor` varchar(100)
,`id_producto` int(11)
,`codigo_modelo` varchar(50)
,`producto` varchar(100)
,`id_variante` int(11)
,`sku` varchar(50)
,`talla` varchar(10)
,`color` varchar(50)
,`id_bodega` int(11)
,`bodega` varchar(100)
,`Stock_par` int(11)
,`Stock_caja` int(11)
,`total_pares_equivalente` bigint(14)
,`ubicacion_especifica` varchar(100)
,`fecha_actualizacion` datetime
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `v_precio_base_producto`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `v_precio_base_producto` (
`id_producto` int(11)
,`precio_base` decimal(10,2)
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `v_productos_busqueda_optimizada`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `v_productos_busqueda_optimizada` (
`id_producto` int(11)
,`nombre` varchar(100)
,`codigo_modelo` varchar(50)
,`precio_venta` decimal(10,2)
,`marca` varchar(50)
,`categoria` varchar(50)
,`variantes_disponibles` mediumtext
,`ean_principal` varchar(20)
,`sku_principal` varchar(50)
,`stock_total` decimal(43,0)
,`bodegas_con_stock` mediumtext
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `v_productos_disponibles_bodega`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `v_productos_disponibles_bodega` (
`id_producto` int(11)
,`codigo_modelo` varchar(50)
,`nombre` varchar(100)
,`genero` enum('MUJER','HOMBRE','NIÃO','UNISEX')
,`ean` varchar(20)
,`sku` varchar(50)
,`id_bodega` int(11)
,`Stock_par` int(11)
,`Stock_caja` int(11)
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `v_promociones_activas`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `v_promociones_activas` (
`id_promocion` int(11)
,`codigo` varchar(30)
,`nombre` varchar(150)
,`descripcion` varchar(500)
,`tipo_descuento` enum('PORCENTAJE','MONTO_FIJO')
,`valor_descuento` decimal(10,2)
,`fecha_inicio` datetime
,`fecha_fin` datetime
,`activa` tinyint(1)
,`min_compra` decimal(12,2)
,`limite_uso_total` int(11)
,`limite_uso_por_usuario` int(11)
,`creado_en` datetime
,`actualizado_en` datetime
);

-- --------------------------------------------------------

--
-- Estructura Stand-in para la vista `v_promos_aplicables_producto`
-- (Véase abajo para la vista actual)
--
CREATE TABLE `v_promos_aplicables_producto` (
`id_producto` int(11)
,`producto_nombre` varchar(100)
,`precio_base` decimal(10,2)
,`id_promocion` int(11)
,`codigo` varchar(30)
,`promo_nombre` varchar(150)
,`tipo_descuento` enum('PORCENTAJE','MONTO_FIJO')
,`valor_descuento` decimal(10,2)
,`tipo_aplicacion` enum('CATEGORIA','MARCA','PRODUCTO','ROL_USUARIO','USUARIO')
,`target_producto` int(11)
,`target_marca` int(11)
,`target_categoria` int(11)
,`target_usuario` int(11)
,`target_rol` varchar(50)
,`actualizado_en` datetime
,`ahorro` decimal(19,2)
,`precio_descuento` decimal(20,2)
);

-- --------------------------------------------------------

--
-- Estructura para la vista `vista_ajustes_pendientes`
--
DROP TABLE IF EXISTS `vista_ajustes_pendientes`;

CREATE ALGORITHM=UNDEFINED DEFINER=`pma_admin`@`localhost` SQL SECURITY DEFINER VIEW `vista_ajustes_pendientes`  AS SELECT `a`.`id_ajuste` AS `id_ajuste`, `a`.`id_detalle_conteo` AS `id_detalle_conteo`, `a`.`id_producto` AS `id_producto`, `pv`.`ean` AS `codigo_barras`, `p`.`nombre` AS `producto`, `p`.`codigo_modelo` AS `tipo`, `a`.`tipo_ajuste` AS `tipo_ajuste`, `a`.`cantidad` AS `cantidad`, `a`.`razon` AS `razon`, `a`.`observaciones` AS `observaciones`, `u`.`nombre` AS `creado_por`, `a`.`fecha_creacion` AS `fecha_creacion`, `d`.`stock_sistema` AS `stock_sistema`, `d`.`stock_contado` AS `stock_contado`, `d`.`diferencia` AS `diferencia`, concat(`p`.`nombre`,' - ',`c`.`nombre`,' - ',`t`.`numero`) AS `producto_completo` FROM ((((((`ajustes_inventario` `a` join `detalles_conteo_inventario` `d` on(`a`.`id_detalle_conteo` = `d`.`id_detalle_conteo`)) join `productos` `p` on(`a`.`id_producto` = `p`.`id_producto`)) left join `producto_variantes` `pv` on(`a`.`id_variante` = `pv`.`id_variante`)) left join `colores` `c` on(`pv`.`id_color` = `c`.`id_color`)) left join `tallas` `t` on(`pv`.`id_talla` = `t`.`id_talla`)) join `usuarios` `u` on(`a`.`id_usuario_creador` = `u`.`id_usuario`)) WHERE `a`.`aprobado` = 0 ;

-- --------------------------------------------------------

--
-- Estructura para la vista `vista_compras_completas`
--
DROP TABLE IF EXISTS `vista_compras_completas`;

CREATE ALGORITHM=UNDEFINED DEFINER=`siro_admin`@`%` SQL SECURITY DEFINER VIEW `vista_compras_completas`  AS SELECT `c`.`id_compra` AS `id_compra`, `c`.`numero_compra` AS `numero_compra`, `c`.`fecha_compra` AS `fecha_compra`, `c`.`fecha_recepcion` AS `fecha_recepcion`, `c`.`numero_factura` AS `numero_factura`, `c`.`subtotal` AS `subtotal`, `c`.`iva` AS `iva`, `c`.`total` AS `total`, `c`.`estado` AS `estado`, `c`.`observaciones` AS `observaciones`, `p`.`id_proveedor` AS `id_proveedor`, `p`.`nombre` AS `nombre_proveedor`, `p`.`ruc` AS `ruc_proveedor`, `b`.`id_bodega` AS `id_bodega`, `b`.`nombre` AS `nombre_bodega`, `u`.`id_usuario` AS `id_usuario`, `u`.`nombre` AS `nombre_usuario`, count(`cd`.`id_detalle_compra`) AS `total_items`, sum(`cd`.`cantidad`) AS `total_unidades` FROM ((((`compras` `c` left join `proveedores` `p` on(`c`.`id_proveedor` = `p`.`id_proveedor`)) left join `bodegas` `b` on(`c`.`id_bodega` = `b`.`id_bodega`)) left join `usuarios` `u` on(`c`.`id_usuario` = `u`.`id_usuario`)) left join `compra_detalles` `cd` on(`c`.`id_compra` = `cd`.`id_compra`)) GROUP BY `c`.`id_compra` ;

-- --------------------------------------------------------

--
-- Estructura para la vista `vista_compra_detalles`
--
DROP TABLE IF EXISTS `vista_compra_detalles`;

CREATE ALGORITHM=UNDEFINED DEFINER=`siro_admin`@`%` SQL SECURITY DEFINER VIEW `vista_compra_detalles`  AS SELECT `cd`.`id_detalle_compra` AS `id_detalle_compra`, `cd`.`id_compra` AS `id_compra`, `cd`.`id_producto` AS `id_producto`, `cd`.`id_variante` AS `id_variante`, `cd`.`cantidad` AS `cantidad`, `cd`.`tipo_unidad` AS `tipo_unidad`, `cd`.`precio_unitario` AS `precio_unitario`, `cd`.`subtotal` AS `subtotal`, `prod`.`codigo_modelo` AS `codigo_modelo`, `prod`.`nombre` AS `nombre_producto`, `t`.`numero` AS `talla`, `col`.`nombre` AS `color`, `pv`.`sku` AS `sku`, `pv`.`ean` AS `ean`, `pv`.`precio_venta` AS `precio_venta` FROM ((((`compra_detalles` `cd` join `productos` `prod` on(`cd`.`id_producto` = `prod`.`id_producto`)) join `producto_variantes` `pv` on(`cd`.`id_variante` = `pv`.`id_variante`)) left join `tallas` `t` on(`pv`.`id_talla` = `t`.`id_talla`)) left join `colores` `col` on(`pv`.`id_color` = `col`.`id_color`)) ;

-- --------------------------------------------------------

--
-- Estructura para la vista `vista_conteos_activos`
--
DROP TABLE IF EXISTS `vista_conteos_activos`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `vista_conteos_activos`  AS SELECT 1 AS `id_conteo`, 'Conteo General' AS `nombre`, 'general' AS `tipo_conteo`, 'pares' AS `tipo`, 'pendiente' AS `estado`, current_timestamp() AS `fecha_programada`, curtime() AS `hora_programada`, 'Sistema' AS `responsable`, 'Conteo automático' AS `observaciones`, (select count(0) from `productos` where `productos`.`activo` = 1) AS `productos_contados`, (select count(0) from `productos`) AS `total_productos` ;

-- --------------------------------------------------------

--
-- Estructura para la vista `vista_cuadre_caja_diario`
--
DROP TABLE IF EXISTS `vista_cuadre_caja_diario`;

CREATE ALGORITHM=UNDEFINED DEFINER=`pma_admin`@`localhost` SQL SECURITY DEFINER VIEW `vista_cuadre_caja_diario`  AS SELECT `cmm`.`id_caja_menor` AS `id_caja_menor`, `cmm`.`codigo` AS `codigo_caja_menor`, `cmm`.`nombre` AS `nombre_caja_menor`, cast(`cmd`.`fecha_movimiento` as date) AS `fecha_cuadre`, `cm`.`monto_inicial` AS `saldo_inicial`, sum(case when `cmd`.`tipo_movimiento` in ('entrada_venta','entrada_otro','ajuste_entrada') then `cmd`.`monto` else 0 end) AS `total_entradas`, sum(case when `cmd`.`tipo_movimiento` in ('salida_gasto','salida_compra_externa','ajuste_salida') then `cmd`.`monto` else 0 end) AS `total_salidas`, `cm`.`monto_inicial`+ sum(case when `cmd`.`tipo_movimiento` in ('entrada_venta','entrada_otro','ajuste_entrada') then `cmd`.`monto` else 0 end) - sum(case when `cmd`.`tipo_movimiento` in ('salida_gasto','salida_compra_externa','ajuste_salida') then `cmd`.`monto` else 0 end) AS `saldo_final_calculado`, `cm`.`monto_final` AS `saldo_final_real`, `cm`.`monto_inicial`+ sum(case when `cmd`.`tipo_movimiento` in ('entrada_venta','entrada_otro','ajuste_entrada') then `cmd`.`monto` else 0 end) - sum(case when `cmd`.`tipo_movimiento` in ('salida_gasto','salida_compra_externa','ajuste_salida') then `cmd`.`monto` else 0 end) - coalesce(`cm`.`monto_final`,0) AS `diferencia_caja` FROM ((`caja_menor` `cmm` join `caja_movimientos` `cm` on(`cmm`.`id_caja_principal` = `cm`.`id_caja`)) left join `caja_movimiento_detalle` `cmd` on(`cm`.`id_movimiento` = `cmd`.`id_movimiento_caja` and `cmd`.`activo` = 1)) WHERE cast(`cmd`.`fecha_movimiento` as date) = curdate() OR `cmd`.`fecha_movimiento` is null GROUP BY `cmm`.`id_caja_menor`, `cmm`.`codigo`, `cmm`.`nombre`, cast(`cmd`.`fecha_movimiento` as date), `cm`.`monto_inicial`, `cm`.`monto_final` ;

-- --------------------------------------------------------

--
-- Estructura para la vista `vista_devoluciones_completas`
--
DROP TABLE IF EXISTS `vista_devoluciones_completas`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `vista_devoluciones_completas`  AS SELECT `d`.`id_devolucion` AS `id_devolucion`, `d`.`numero_devolucion` AS `numero_devolucion`, `d`.`fecha_devolucion` AS `fecha_devolucion`, `d`.`tipo_devolucion` AS `tipo_devolucion`, `d`.`motivo` AS `motivo`, `d`.`estado` AS `estado`, `d`.`total_devolucion` AS `total_devolucion`, `v`.`fecha_venta` AS `fecha_venta`, `v`.`total` AS `total_venta_original`, `c`.`nombre` AS `cliente`, `c`.`dni` AS `dni`, `c`.`telefono` AS `telefono`, `u`.`nombre` AS `usuario_procesa`, `ua`.`nombre` AS `usuario_autoriza`, count(`dd`.`id_detalle_devolucion`) AS `total_items`, sum(`dd`.`cantidad_devuelta`) AS `total_cantidad_devuelta`, `nc`.`numero_nota_credito` AS `numero_nota_credito`, `nc`.`saldo_disponible` AS `saldo_disponible`, `nc`.`estado` AS `estado_nota_credito` FROM ((((((`devoluciones` `d` join `ventas` `v` on(`d`.`id_venta` = `v`.`id_venta`)) join `clientes` `c` on(`d`.`id_cliente` = `c`.`id_cliente`)) join `usuarios` `u` on(`d`.`id_usuario_procesa` = `u`.`id_usuario`)) left join `usuarios` `ua` on(`d`.`id_usuario_autoriza` = `ua`.`id_usuario`)) left join `devolucion_detalles` `dd` on(`d`.`id_devolucion` = `dd`.`id_devolucion`)) left join `notas_credito` `nc` on(`d`.`id_devolucion` = `nc`.`id_devolucion`)) WHERE `d`.`activa` = 1 GROUP BY `d`.`id_devolucion` ;

-- --------------------------------------------------------

--
-- Estructura para la vista `vista_ganancias_producto`
--
DROP TABLE IF EXISTS `vista_ganancias_producto`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `vista_ganancias_producto`  AS SELECT `p`.`id_producto` AS `id_producto`, `p`.`nombre` AS `nombre`, `pv`.`ean` AS `codigo_barras`, `p`.`id_marca` AS `id_marca`, `p`.`id_categoria` AS `id_categoria`, `p`.`precio_compra` AS `precio_compra`, `p`.`precio_venta` AS `precio_venta`, `p`.`precio_venta`- `p`.`precio_compra` AS `ganancia_unitaria`, 0 AS `num_ventas`, 0 AS `unidades_vendidas`, 0 AS `costo_total`, 0 AS `ventas_total`, 0 AS `ganancia_total` FROM (`productos` `p` left join `producto_variantes` `pv` on(`p`.`id_producto` = `pv`.`id_producto`)) WHERE `p`.`activo` = 1 ;

-- --------------------------------------------------------

--
-- Estructura para la vista `vista_inventario_cajas`
--
DROP TABLE IF EXISTS `vista_inventario_cajas`;

CREATE ALGORITHM=UNDEFINED DEFINER=`pma_admin`@`localhost` SQL SECURITY DEFINER VIEW `vista_inventario_cajas`  AS SELECT `pv`.`id_producto` AS `id_producto`, `p`.`nombre` AS `nombre`, `pv`.`ean` AS `codigo_barras`, coalesce(sum(`ib`.`Stock_caja`),0) AS `stock`, `p`.`precio_compra` AS `costo`, `p`.`ubicacion` AS `ubicacion`, `p`.`activo` AS `activo` FROM ((`producto_variantes` `pv` join `productos` `p` on(`pv`.`id_producto` = `p`.`id_producto`)) left join `inventario_bodega` `ib` on(`ib`.`id_variante` = `pv`.`id_variante` and `ib`.`activo` = 1)) GROUP BY `pv`.`id_producto`, `pv`.`ean`, `p`.`nombre`, `p`.`precio_compra`, `p`.`ubicacion`, `p`.`activo` ;

-- --------------------------------------------------------

--
-- Estructura para la vista `vista_inventario_pares`
--
DROP TABLE IF EXISTS `vista_inventario_pares`;

CREATE ALGORITHM=UNDEFINED DEFINER=`pma_admin`@`localhost` SQL SECURITY DEFINER VIEW `vista_inventario_pares`  AS SELECT `pv`.`id_producto` AS `id_producto`, `p`.`nombre` AS `nombre`, `pv`.`ean` AS `codigo_barras`, coalesce(sum(`ib`.`Stock_par`),0) AS `stock`, `p`.`precio_venta` AS `precio`, `p`.`ubicacion` AS `ubicacion`, `p`.`activo` AS `activo` FROM ((`producto_variantes` `pv` join `productos` `p` on(`pv`.`id_producto` = `p`.`id_producto`)) left join `inventario_bodega` `ib` on(`ib`.`id_variante` = `pv`.`id_variante` and `ib`.`activo` = 1)) GROUP BY `pv`.`id_producto`, `pv`.`ean`, `p`.`nombre`, `p`.`precio_venta`, `p`.`ubicacion`, `p`.`activo` ;

-- --------------------------------------------------------

--
-- Estructura para la vista `vista_inventario_pares_con_prestamos`
--
DROP TABLE IF EXISTS `vista_inventario_pares_con_prestamos`;

CREATE ALGORITHM=UNDEFINED DEFINER=`pma_admin`@`localhost` SQL SECURITY DEFINER VIEW `vista_inventario_pares_con_prestamos`  AS SELECT `p`.`id_producto` AS `id_producto`, `p`.`nombre` AS `nombre`, `pv`.`ean` AS `codigo_barras`, `p`.`id_marca` AS `id_marca`, `p`.`id_categoria` AS `id_categoria`, `p`.`descripcion` AS `descripcion`, `ib`.`Stock_par`- coalesce(sum(case when `pr`.`pie` = 'AMBOS' then 1 else 0 end) + greatest(sum(case when `pr`.`pie` = 'DERECHO' then 1 else 0 end),sum(case when `pr`.`pie` = 'IZQUIERDO' then 1 else 0 end)),0) AS `stock`, `p`.`precio_venta` AS `precio`, `p`.`ubicacion` AS `ubicacion`, `p`.`activo` AS `activo` FROM (((`productos` `p` join `producto_variantes` `pv` on(`p`.`id_producto` = `pv`.`id_producto`)) join `inventario_bodega` `ib` on(`ib`.`id_variante` = `pv`.`id_variante` and `ib`.`activo` = 1)) left join `prestamos_zapatos` `pr` on(`pv`.`id_variante` = `pr`.`id_variante` and `pr`.`estado` = 'PRESTADO')) WHERE `p`.`activo` = 1 GROUP BY `pv`.`id_variante`, `p`.`id_producto`, `p`.`nombre`, `pv`.`ean`, `p`.`id_marca`, `p`.`id_categoria`, `p`.`descripcion`, `p`.`precio_venta`, `p`.`ubicacion`, `p`.`activo`, `ib`.`Stock_par` ;

-- --------------------------------------------------------

--
-- Estructura para la vista `vista_inventario_por_ubicacion`
--
DROP TABLE IF EXISTS `vista_inventario_por_ubicacion`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `vista_inventario_por_ubicacion`  AS SELECT `p`.`ubicacion` AS `ubicacion`, count(distinct `p`.`id_producto`) AS `total_productos`, sum(`pv`.`stock_por_cajas`) AS `total_cajas`, sum(`pv`.`stock_por_pares`) AS `total_pares`, avg(`p`.`precio_venta`) AS `precio_promedio` FROM (`productos` `p` left join `producto_variantes` `pv` on(`p`.`id_producto` = `pv`.`id_producto`)) WHERE `p`.`activo` = 1 GROUP BY `p`.`ubicacion` ;

-- --------------------------------------------------------

--
-- Estructura para la vista `vista_notas_credito_activas`
--
DROP TABLE IF EXISTS `vista_notas_credito_activas`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `vista_notas_credito_activas`  AS SELECT `nc`.`id_nota_credito` AS `id_nota_credito`, `nc`.`numero_nota_credito` AS `numero_nota_credito`, `nc`.`fecha_emision` AS `fecha_emision`, `nc`.`fecha_vencimiento` AS `fecha_vencimiento`, `nc`.`total` AS `total`, `nc`.`saldo_disponible` AS `saldo_disponible`, `nc`.`saldo_usado` AS `saldo_usado`, `nc`.`estado` AS `estado`, `c`.`nombre` AS `cliente`, `c`.`dni` AS `dni`, `d`.`numero_devolucion` AS `numero_devolucion`, to_days(`nc`.`fecha_vencimiento`) - to_days(current_timestamp()) AS `dias_para_vencer` FROM ((`notas_credito` `nc` join `clientes` `c` on(`nc`.`id_cliente` = `c`.`id_cliente`)) join `devoluciones` `d` on(`nc`.`id_devolucion` = `d`.`id_devolucion`)) WHERE `nc`.`activa` = 1 AND `nc`.`estado` = 'emitida' AND `nc`.`saldo_disponible` > 0 AND `nc`.`fecha_vencimiento` > current_timestamp() ;

-- --------------------------------------------------------

--
-- Estructura para la vista `vista_productos_completos`
--
DROP TABLE IF EXISTS `vista_productos_completos`;

CREATE ALGORITHM=UNDEFINED DEFINER=`soft_admin_xtreme`@`%` SQL SECURITY DEFINER VIEW `vista_productos_completos`  AS SELECT `p`.`id_producto` AS `id_producto`, `p`.`codigo_modelo` AS `codigo_modelo`, `p`.`nombre` AS `nombre`, `p`.`descripcion` AS `descripcion`, `p`.`precio_venta` AS `precio_venta`, `p`.`genero` AS `genero`, `p`.`activo` AS `activo`, `p`.`id_marca` AS `id_marca`, `p`.`id_categoria` AS `id_categoria`, `pv`.`id_variante` AS `id_variante`, `pv`.`id_color` AS `id_color`, `pv`.`id_talla` AS `id_talla`, `pv`.`disponible` AS `disponible`, `pv`.`imagen` AS `imagen`, `pv`.`sku` AS `sku` FROM (`productos` `p` join `producto_variantes` `pv` on(`p`.`id_producto` = `pv`.`id_producto`)) ;

-- --------------------------------------------------------

--
-- Estructura para la vista `vista_productos_resumen`
--
DROP TABLE IF EXISTS `vista_productos_resumen`;

CREATE ALGORITHM=UNDEFINED DEFINER=`soft_admin_xtreme`@`%` SQL SECURITY DEFINER VIEW `vista_productos_resumen`  AS SELECT `p`.`id_producto` AS `id_producto`, `p`.`codigo_modelo` AS `codigo_modelo`, `p`.`nombre` AS `nombre`, `p`.`descripcion` AS `descripcion`, `p`.`precio_venta` AS `precio_venta`, `p`.`genero` AS `genero`, `p`.`activo` AS `activo`, `p`.`id_categoria` AS `id_categoria`, `p`.`id_marca` AS `id_marca`, `p`.`fecha_creacion` AS `fecha_creacion`, count(distinct `pv`.`id_variante`) AS `total_variantes`, count(distinct case when `pv`.`disponible` = 1 then `pv`.`id_variante` end) AS `variantes_disponibles`, count(distinct `pv`.`id_color`) AS `total_colores`, count(distinct `pv`.`id_talla`) AS `total_tallas`, sum(coalesce(`ib`.`Stock_par`,0) + coalesce(`ib`.`Stock_caja`,0)) AS `stock_total`, sum(case when `pv`.`disponible` = 1 then coalesce(`ib`.`Stock_par`,0) + coalesce(`ib`.`Stock_caja`,0) else 0 end) AS `stock_disponible`, (select `pv2`.`id_variante` from `producto_variantes` `pv2` where `pv2`.`id_producto` = `p`.`id_producto` and `pv2`.`disponible` = 1 and `pv2`.`imagen` is not null and `pv2`.`imagen` <> '' order by `pv2`.`id_variante` limit 1) AS `imagen_principal_id`, (select `c`.`nombre` from (`producto_variantes` `pv3` join `colores` `c` on(`pv3`.`id_color` = `c`.`id_color`)) where `pv3`.`id_producto` = `p`.`id_producto` and `pv3`.`disponible` = 1 and `pv3`.`imagen` is not null and `pv3`.`imagen` <> '' order by `pv3`.`id_variante` limit 1) AS `color_imagen_principal`, (select min(`pv4`.`precio_venta`) from `producto_variantes` `pv4` where `pv4`.`id_producto` = `p`.`id_producto` and `pv4`.`disponible` = 1) AS `precio_min_variante`, (select max(`pv4`.`precio_venta`) from `producto_variantes` `pv4` where `pv4`.`id_producto` = `p`.`id_producto` and `pv4`.`disponible` = 1) AS `precio_max_variante`, (select coalesce(sum(`vd`.`cantidad`),0) from (`venta_detalles` `vd` join `ventas` `v` on(`vd`.`id_venta` = `v`.`id_venta`)) where `vd`.`id_producto` = `p`.`id_producto` and `v`.`estado` = 'completada') AS `total_ventas` FROM ((`productos` `p` left join `producto_variantes` `pv` on(`p`.`id_producto` = `pv`.`id_producto`)) left join `inventario_bodega` `ib` on(`ib`.`id_variante` = `pv`.`id_variante` and `ib`.`activo` = 1)) WHERE `p`.`activo` = 1 GROUP BY `p`.`id_producto`, `p`.`codigo_modelo`, `p`.`nombre`, `p`.`descripcion`, `p`.`precio_venta`, `p`.`genero`, `p`.`activo`, `p`.`id_categoria`, `p`.`id_marca`, `p`.`fecha_creacion` ;

-- --------------------------------------------------------

--
-- Estructura para la vista `vista_productos_stock_bajo`
--
DROP TABLE IF EXISTS `vista_productos_stock_bajo`;

CREATE ALGORITHM=UNDEFINED DEFINER=`pma_admin`@`localhost` SQL SECURITY DEFINER VIEW `vista_productos_stock_bajo`  AS SELECT `p`.`id_producto` AS `id_producto`, `p`.`nombre` AS `nombre`, max(`pv`.`ean`) AS `codigo_barras`, `p`.`stock_minimo` AS `stock_minimo`, coalesce(sum(`ib`.`Stock_caja`),0) AS `stock_por_cajas`, coalesce(sum(`ib`.`Stock_par`),0) AS `stock_por_pares`, CASE WHEN coalesce(sum(`ib`.`Stock_caja`),0) < `p`.`stock_minimo` THEN 'Stock bajo en cajas' WHEN coalesce(sum(`ib`.`Stock_par`),0) < `p`.`stock_minimo` THEN 'Stock bajo en pares' ELSE 'Stock normal' END AS `estado_stock`, `p`.`ubicacion` AS `ubicacion` FROM ((`productos` `p` left join `producto_variantes` `pv` on(`p`.`id_producto` = `pv`.`id_producto`)) left join `inventario_bodega` `ib` on(`pv`.`id_variante` = `ib`.`id_variante` and `ib`.`activo` = 1)) WHERE `p`.`activo` = 1 GROUP BY `p`.`id_producto`, `p`.`nombre`, `p`.`stock_minimo`, `p`.`ubicacion` ;

-- --------------------------------------------------------

--
-- Estructura para la vista `vista_rentabilidad_diaria`
--
DROP TABLE IF EXISTS `vista_rentabilidad_diaria`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `vista_rentabilidad_diaria`  AS SELECT curdate() AS `fecha_venta`, 0 AS `ventas_totales`, 0 AS `costo_productos`, 0 AS `ganancia_bruta`, 0 AS `gastos_operativos`, 0 AS `utilidad_neta`, 0 AS `margen_bruto_porcentaje` ;

-- --------------------------------------------------------

--
-- Estructura para la vista `vista_tallas_disponibles`
--
DROP TABLE IF EXISTS `vista_tallas_disponibles`;

CREATE ALGORITHM=UNDEFINED DEFINER=`pma_admin`@`localhost` SQL SECURITY DEFINER VIEW `vista_tallas_disponibles`  AS SELECT `t`.`id_talla` AS `id_talla`, `t`.`numero` AS `numero`, `t`.`sistema` AS `sistema`, `t`.`equivalencia_eu` AS `equivalencia_eu`, `t`.`equivalencia_us` AS `equivalencia_us`, `t`.`equivalencia_uk` AS `equivalencia_uk`, `t`.`equivalencia_cm` AS `equivalencia_cm`, `t`.`genero` AS `genero`, count(distinct `pv`.`id_producto`) AS `productos_disponibles`, count(distinct case when `pv`.`disponible` = 1 and exists(select 1 from `inventario_bodega` `ib` where `ib`.`id_variante` = `pv`.`id_variante` and `ib`.`activo` = 1 and coalesce(`ib`.`Stock_par`,0) > 0 limit 1) then `pv`.`id_producto` end) AS `productos_en_stock` FROM ((`tallas` `t` join `producto_variantes` `pv` on(`t`.`id_talla` = `pv`.`id_talla`)) join `productos` `p` on(`pv`.`id_producto` = `p`.`id_producto`)) WHERE `p`.`activo` = 1 GROUP BY `t`.`id_talla`, `t`.`numero`, `t`.`sistema`, `t`.`equivalencia_eu`, `t`.`equivalencia_us`, `t`.`equivalencia_uk`, `t`.`equivalencia_cm`, `t`.`genero` ORDER BY count(distinct case when `pv`.`disponible` = 1 and exists(select 1 from `inventario_bodega` `ib` where `ib`.`id_variante` = `pv`.`id_variante` and `ib`.`activo` = 1 and coalesce(`ib`.`Stock_par`,0) > 0 limit 1) then `pv`.`id_producto` end) DESC, `t`.`numero` ASC ;

-- --------------------------------------------------------

--
-- Estructura para la vista `vw_productos_stock_pares`
--
DROP TABLE IF EXISTS `vw_productos_stock_pares`;

CREATE ALGORITHM=UNDEFINED DEFINER=`pma_admin`@`localhost` SQL SECURITY DEFINER VIEW `vw_productos_stock_pares`  AS SELECT `p`.`id_producto` AS `id_producto`, `p`.`nombre` AS `nombre`, sum(coalesce(`ib`.`Stock_par`,0) + coalesce(`ib`.`Stock_caja`,0) * coalesce(`p`.`pares_por_caja`,1)) AS `stock_pares_total` FROM ((`productos` `p` left join `producto_variantes` `v` on(`v`.`id_producto` = `p`.`id_producto`)) left join `inventario_bodega` `ib` on(`ib`.`id_variante` = `v`.`id_variante` and `ib`.`activo` = 1)) GROUP BY `p`.`id_producto`, `p`.`nombre` ;

-- --------------------------------------------------------

--
-- Estructura para la vista `vw_variantes_stock_pares`
--
DROP TABLE IF EXISTS `vw_variantes_stock_pares`;

CREATE ALGORITHM=UNDEFINED DEFINER=`pma_admin`@`localhost` SQL SECURITY DEFINER VIEW `vw_variantes_stock_pares`  AS SELECT `ib`.`id_variante` AS `id_variante`, `v`.`id_producto` AS `id_producto`, `v`.`id_talla` AS `id_talla`, `v`.`id_color` AS `id_color`, `ib`.`id_bodega` AS `id_bodega`, coalesce(`ib`.`Stock_par`,0) + coalesce(`ib`.`Stock_caja`,0) * coalesce(`p`.`pares_por_caja`,1) AS `stock_pares_equivalentes`, coalesce(`ib`.`Stock_par`,0) AS `stock_por_pares`, coalesce(`ib`.`Stock_caja`,0) AS `stock_por_cajas`, coalesce(`p`.`pares_por_caja`,1) AS `pares_por_caja`, `v`.`precio_compra` AS `precio_compra`, `v`.`precio_venta` AS `precio_venta`, `v`.`sku` AS `sku`, `v`.`ean` AS `ean` FROM ((`producto_variantes` `v` join `productos` `p` on(`p`.`id_producto` = `v`.`id_producto`)) left join `inventario_bodega` `ib` on(`ib`.`id_variante` = `v`.`id_variante` and `ib`.`activo` = 1)) ;

-- --------------------------------------------------------

--
-- Estructura para la vista `v_auditoria_por_proveedor`
--
DROP TABLE IF EXISTS `v_auditoria_por_proveedor`;

CREATE ALGORITHM=UNDEFINED DEFINER=`siro_admin`@`%` SQL SECURITY DEFINER VIEW `v_auditoria_por_proveedor`  AS SELECT `pr`.`id_proveedor` AS `id_proveedor`, `pr`.`nombre` AS `proveedor`, `pv`.`sku` AS `sku`, `p`.`nombre` AS `producto`, `at`.`tipo_evento` AS `tipo_evento`, count(0) AS `num_eventos`, sum(`at`.`cantidad`) AS `total_cantidad`, max(`at`.`fecha_evento`) AS `ultimo_evento`, min(`at`.`fecha_evento`) AS `primer_evento` FROM (((`auditoria_trazabilidad` `at` join `producto_variantes` `pv` on(`at`.`id_variante` = `pv`.`id_variante`)) join `productos` `p` on(`pv`.`id_producto` = `p`.`id_producto`)) left join `proveedores` `pr` on(`at`.`id_proveedor` = `pr`.`id_proveedor`)) WHERE `at`.`activo` = 1 GROUP BY `pr`.`id_proveedor`, `pv`.`id_variante`, `at`.`tipo_evento` ORDER BY `pr`.`nombre` ASC, `p`.`nombre` ASC, `at`.`tipo_evento` ASC ;

-- --------------------------------------------------------

--
-- Estructura para la vista `v_cajas_convertibles_por_traspaso`
--
DROP TABLE IF EXISTS `v_cajas_convertibles_por_traspaso`;

CREATE ALGORITHM=UNDEFINED DEFINER=`pma_admin`@`localhost` SQL SECURITY DEFINER VIEW `v_cajas_convertibles_por_traspaso`  AS SELECT `t`.`numero_traspaso` AS `numero_traspaso`, `t`.`id_bodega_destino` AS `id_bodega_destino`, `td`.`id_detalle_traspaso` AS `id_detalle_traspaso`, `td`.`id_variante` AS `id_variante_caja`, `pv`.`id_color` AS `id_color`, coalesce(`c`.`nombre`,'') AS `color_nombre`, `pv`.`id_producto` AS `id_producto`, `p`.`nombre` AS `producto_nombre`, `p`.`genero` AS `genero`, `tl`.`numero` AS `talla_numero`, greatest(0,least(coalesce(`ib`.`Stock_caja`,0),coalesce(`td`.`cantidad_recibida`,`td`.`cantidad_enviada`,`td`.`cantidad_solicitada`) - coalesce(`cv`.`convertidas`,0))) AS `cajas_convertibles` FROM (((((((`traspaso_detalles` `td` join `traspasos` `t` on(`td`.`id_traspaso` = `t`.`id_traspaso`)) join `producto_variantes` `pv` on(`pv`.`id_variante` = `td`.`id_variante`)) join `productos` `p` on(`p`.`id_producto` = `pv`.`id_producto`)) left join `tallas` `tl` on(`pv`.`id_talla` = `tl`.`id_talla`)) left join `colores` `c` on(`pv`.`id_color` = `c`.`id_color`)) left join `inventario_bodega` `ib` on(`ib`.`id_bodega` = `t`.`id_bodega_destino` and `ib`.`id_variante` = `td`.`id_variante` and `ib`.`activo` = 1)) left join (select `conversion_caja_traspaso`.`id_detalle_traspaso` AS `id_detalle_traspaso`,`conversion_caja_traspaso`.`id_variante_caja` AS `id_variante_caja`,sum(`conversion_caja_traspaso`.`cajas_convertidas`) AS `convertidas` from `conversion_caja_traspaso` group by `conversion_caja_traspaso`.`id_detalle_traspaso`,`conversion_caja_traspaso`.`id_variante_caja`) `cv` on(`cv`.`id_detalle_traspaso` = `td`.`id_detalle_traspaso` and `cv`.`id_variante_caja` = `td`.`id_variante`)) WHERE lcase(`td`.`Tipo`) = 'caja' AND greatest(0,least(coalesce(`ib`.`Stock_caja`,0),coalesce(`td`.`cantidad_recibida`,`td`.`cantidad_enviada`,`td`.`cantidad_solicitada`) - coalesce(`cv`.`convertidas`,0))) > 0 ;

-- --------------------------------------------------------

--
-- Estructura para la vista `v_inventario_por_proveedor`
--
DROP TABLE IF EXISTS `v_inventario_por_proveedor`;

CREATE ALGORITHM=UNDEFINED DEFINER=`siro_admin`@`%` SQL SECURITY DEFINER VIEW `v_inventario_por_proveedor`  AS SELECT `pr`.`id_proveedor` AS `id_proveedor`, `pr`.`nombre` AS `proveedor`, `p`.`id_producto` AS `id_producto`, `p`.`codigo_modelo` AS `codigo_modelo`, `p`.`nombre` AS `producto`, `pv`.`id_variante` AS `id_variante`, `pv`.`sku` AS `sku`, `t`.`numero` AS `talla`, `c`.`nombre` AS `color`, `b`.`id_bodega` AS `id_bodega`, `b`.`nombre` AS `bodega`, `ib`.`Stock_par` AS `Stock_par`, `ib`.`Stock_caja` AS `Stock_caja`, `ib`.`Stock_par`+ `ib`.`Stock_caja` * 24 AS `total_pares_equivalente`, `ib`.`ubicacion_especifica` AS `ubicacion_especifica`, `ib`.`fecha_actualizacion` AS `fecha_actualizacion` FROM ((((((`inventario_bodega` `ib` join `producto_variantes` `pv` on(`ib`.`id_variante` = `pv`.`id_variante`)) join `productos` `p` on(`pv`.`id_producto` = `p`.`id_producto`)) left join `proveedores` `pr` on(`pv`.`id_proveedor` = `pr`.`id_proveedor`)) left join `tallas` `t` on(`pv`.`id_talla` = `t`.`id_talla`)) left join `colores` `c` on(`pv`.`id_color` = `c`.`id_color`)) join `bodegas` `b` on(`ib`.`id_bodega` = `b`.`id_bodega`)) WHERE `ib`.`activo` = 1 AND (`ib`.`Stock_par` > 0 OR `ib`.`Stock_caja` > 0) ;

-- --------------------------------------------------------

--
-- Estructura para la vista `v_precio_base_producto`
--
DROP TABLE IF EXISTS `v_precio_base_producto`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_precio_base_producto`  AS SELECT `p`.`id_producto` AS `id_producto`, coalesce(min(case when `pv`.`disponible` = 1 then `pv`.`precio_venta` end),`p`.`precio_venta`) AS `precio_base` FROM (`productos` `p` left join `producto_variantes` `pv` on(`pv`.`id_producto` = `p`.`id_producto`)) GROUP BY `p`.`id_producto`, `p`.`precio_venta` ;

-- --------------------------------------------------------

--
-- Estructura para la vista `v_productos_busqueda_optimizada`
--
DROP TABLE IF EXISTS `v_productos_busqueda_optimizada`;

CREATE ALGORITHM=UNDEFINED DEFINER=`pma_admin`@`localhost` SQL SECURITY DEFINER VIEW `v_productos_busqueda_optimizada`  AS SELECT DISTINCT `p`.`id_producto` AS `id_producto`, `p`.`nombre` AS `nombre`, `p`.`codigo_modelo` AS `codigo_modelo`, `p`.`precio_venta` AS `precio_venta`, `m`.`nombre` AS `marca`, `c`.`nombre` AS `categoria`, group_concat(distinct concat(`t`.`numero`,':',`col`.`nombre`) separator ', ') AS `variantes_disponibles`, max(`pv`.`ean`) AS `ean_principal`, max(`pv`.`sku`) AS `sku_principal`, coalesce(sum(coalesce(`ib`.`Stock_par`,0) + coalesce(`ib`.`Stock_caja`,0) * `p`.`pares_por_caja`),0) AS `stock_total`, group_concat(distinct `b`.`nombre` separator ', ') AS `bodegas_con_stock` FROM (((((((`productos` `p` left join `marcas` `m` on(`p`.`id_marca` = `m`.`id_marca`)) left join `categorias` `c` on(`p`.`id_categoria` = `c`.`id_categoria`)) left join `producto_variantes` `pv` on(`p`.`id_producto` = `pv`.`id_producto` and `pv`.`disponible` = 1)) left join `tallas` `t` on(`pv`.`id_talla` = `t`.`id_talla`)) left join `colores` `col` on(`pv`.`id_color` = `col`.`id_color`)) left join `inventario_bodega` `ib` on(`pv`.`id_variante` = `ib`.`id_variante` and (coalesce(`ib`.`Stock_par`,0) > 0 or coalesce(`ib`.`Stock_caja`,0) > 0))) left join `bodegas` `b` on(`ib`.`id_bodega` = `b`.`id_bodega`)) WHERE `p`.`activo` = 1 GROUP BY `p`.`id_producto`, `p`.`nombre`, `p`.`codigo_modelo`, `p`.`precio_venta`, `m`.`nombre`, `c`.`nombre` ;

-- --------------------------------------------------------

--
-- Estructura para la vista `v_productos_disponibles_bodega`
--
DROP TABLE IF EXISTS `v_productos_disponibles_bodega`;

CREATE ALGORITHM=UNDEFINED DEFINER=`pma_admin`@`localhost` SQL SECURITY DEFINER VIEW `v_productos_disponibles_bodega`  AS SELECT DISTINCT `p`.`id_producto` AS `id_producto`, `p`.`codigo_modelo` AS `codigo_modelo`, `p`.`nombre` AS `nombre`, `p`.`genero` AS `genero`, `pv`.`ean` AS `ean`, `pv`.`sku` AS `sku`, `ib`.`id_bodega` AS `id_bodega`, coalesce(`ib`.`Stock_par`,0) AS `Stock_par`, coalesce(`ib`.`Stock_caja`,0) AS `Stock_caja` FROM ((`inventario_bodega` `ib` join `producto_variantes` `pv` on(`pv`.`id_variante` = `ib`.`id_variante`)) join `productos` `p` on(`p`.`id_producto` = `pv`.`id_producto`)) WHERE `p`.`activo` = 1 AND `pv`.`disponible` = 1 AND `ib`.`activo` = 1 AND (coalesce(`ib`.`Stock_par`,0) > 0 OR coalesce(`ib`.`Stock_caja`,0) > 0) ;

-- --------------------------------------------------------

--
-- Estructura para la vista `v_promociones_activas`
--
DROP TABLE IF EXISTS `v_promociones_activas`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_promociones_activas`  AS SELECT `promociones`.`id_promocion` AS `id_promocion`, `promociones`.`codigo` AS `codigo`, `promociones`.`nombre` AS `nombre`, `promociones`.`descripcion` AS `descripcion`, `promociones`.`tipo_descuento` AS `tipo_descuento`, `promociones`.`valor_descuento` AS `valor_descuento`, `promociones`.`fecha_inicio` AS `fecha_inicio`, `promociones`.`fecha_fin` AS `fecha_fin`, `promociones`.`activa` AS `activa`, `promociones`.`min_compra` AS `min_compra`, `promociones`.`limite_uso_total` AS `limite_uso_total`, `promociones`.`limite_uso_por_usuario` AS `limite_uso_por_usuario`, `promociones`.`creado_en` AS `creado_en`, `promociones`.`actualizado_en` AS `actualizado_en` FROM `promociones` WHERE `promociones`.`activa` = 1 AND current_timestamp() between `promociones`.`fecha_inicio` and `promociones`.`fecha_fin` ;

-- --------------------------------------------------------

--
-- Estructura para la vista `v_promos_aplicables_producto`
--
DROP TABLE IF EXISTS `v_promos_aplicables_producto`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_promos_aplicables_producto`  AS SELECT `p`.`id_producto` AS `id_producto`, `p`.`nombre` AS `producto_nombre`, `pb`.`precio_base` AS `precio_base`, `pa`.`id_promocion` AS `id_promocion`, `pa`.`codigo` AS `codigo`, `pa`.`nombre` AS `promo_nombre`, `pa`.`tipo_descuento` AS `tipo_descuento`, `pa`.`valor_descuento` AS `valor_descuento`, `pd`.`tipo_aplicacion` AS `tipo_aplicacion`, `pd`.`id_producto` AS `target_producto`, `pd`.`id_marca` AS `target_marca`, `pd`.`id_categoria` AS `target_categoria`, `pd`.`id_usuario` AS `target_usuario`, `pd`.`rol_usuario` AS `target_rol`, `pa`.`actualizado_en` AS `actualizado_en`, CASE WHEN `pa`.`tipo_descuento` = 'PORCENTAJE' THEN round(`pb`.`precio_base` * `pa`.`valor_descuento` / 100,2) ELSE least(`pa`.`valor_descuento`,`pb`.`precio_base`) END AS `ahorro`, CASE WHEN `pa`.`tipo_descuento` = 'PORCENTAJE' THEN round(`pb`.`precio_base` * (100 - `pa`.`valor_descuento`) / 100,2) ELSE greatest(0,`pb`.`precio_base` - `pa`.`valor_descuento`) END AS `precio_descuento` FROM (((`v_promociones_activas` `pa` join `promociones_detalle` `pd` on(`pd`.`id_promocion` = `pa`.`id_promocion` and `pd`.`activo` = 1)) join `productos` `p` on(`pd`.`tipo_aplicacion` = 'PRODUCTO' and `pd`.`id_producto` = `p`.`id_producto` or `pd`.`tipo_aplicacion` = 'MARCA' and `pd`.`id_marca` = `p`.`id_marca` or `pd`.`tipo_aplicacion` = 'CATEGORIA' and `pd`.`id_categoria` = `p`.`id_categoria` or `pd`.`tipo_aplicacion` in ('USUARIO','ROL_USUARIO'))) join `v_precio_base_producto` `pb` on(`pb`.`id_producto` = `p`.`id_producto`)) ;

--
-- Índices para tablas volcadas
--

--
-- Indices de la tabla `abonos`
--
ALTER TABLE `abonos`
  ADD PRIMARY KEY (`id_abono`),
  ADD UNIQUE KEY `numero_comprobante` (`numero_comprobante`),
  ADD KEY `idx_abonos_compra` (`id_compra`),
  ADD KEY `idx_abonos_fecha` (`fecha_abono`),
  ADD KEY `idx_abonos_medio` (`medio_pago`),
  ADD KEY `idx_abonos_estado` (`estado`);

--
-- Indices de la tabla `ajustes_inventario`
--
ALTER TABLE `ajustes_inventario`
  ADD PRIMARY KEY (`id_ajuste`),
  ADD KEY `id_detalle_conteo` (`id_detalle_conteo`),
  ADD KEY `id_producto` (`id_producto`),
  ADD KEY `id_variante` (`id_variante`),
  ADD KEY `id_usuario_creador` (`id_usuario_creador`),
  ADD KEY `id_usuario_aprobador` (`id_usuario_aprobador`);

--
-- Indices de la tabla `aplicaciones_nota_credito`
--
ALTER TABLE `aplicaciones_nota_credito`
  ADD PRIMARY KEY (`id_aplicacion`),
  ADD KEY `idx_nota_credito_aplicacion` (`id_nota_credito`),
  ADD KEY `idx_venta_aplicacion` (`id_venta`),
  ADD KEY `fk_aplicacion_usuario` (`id_usuario_aplica`);

--
-- Indices de la tabla `auditoria_trazabilidad`
--
ALTER TABLE `auditoria_trazabilidad`
  ADD PRIMARY KEY (`id_auditoria`),
  ADD KEY `idx_variante_proveedor` (`id_variante`,`id_proveedor`),
  ADD KEY `idx_evento_fecha` (`tipo_evento`,`fecha_evento`),
  ADD KEY `idx_bodega_origen` (`id_bodega_origen`),
  ADD KEY `idx_bodega_destino` (`id_bodega_destino`),
  ADD KEY `idx_referencia` (`tipo_referencia`,`id_referencia`),
  ADD KEY `fk_auditoria_proveedor` (`id_proveedor`);

--
-- Indices de la tabla `bodegas`
--
ALTER TABLE `bodegas`
  ADD PRIMARY KEY (`id_bodega`),
  ADD UNIQUE KEY `codigo_bodega` (`codigo`),
  ADD KEY `idx_bodegas_nombre` (`nombre`);

--
-- Indices de la tabla `bodega_sectores`
--
ALTER TABLE `bodega_sectores`
  ADD PRIMARY KEY (`id_sector`),
  ADD UNIQUE KEY `codigo_sector_bodega` (`id_bodega`,`codigo_sector`),
  ADD KEY `idx_bodega` (`id_bodega`);

--
-- Indices de la tabla `busquedas`
--
ALTER TABLE `busquedas`
  ADD PRIMARY KEY (`id_busqueda`),
  ADD KEY `termino_busqueda` (`termino_busqueda`),
  ADD KEY `id_usuario` (`id_usuario`);

--
-- Indices de la tabla `cajas`
--
ALTER TABLE `cajas`
  ADD PRIMARY KEY (`id_caja`),
  ADD KEY `idx_caja_bodega` (`id_bodega`);

--
-- Indices de la tabla `caja_menor`
--
ALTER TABLE `caja_menor`
  ADD PRIMARY KEY (`id_caja_menor`),
  ADD UNIQUE KEY `codigo` (`codigo`),
  ADD KEY `idx_caja_principal` (`id_caja_principal`),
  ADD KEY `idx_responsable` (`responsable_actual`);

--
-- Indices de la tabla `caja_movimientos`
--
ALTER TABLE `caja_movimientos`
  ADD PRIMARY KEY (`id_movimiento`),
  ADD KEY `id_caja` (`id_caja`),
  ADD KEY `id_usuario` (`id_usuario`);

--
-- Indices de la tabla `caja_movimiento_detalle`
--
ALTER TABLE `caja_movimiento_detalle`
  ADD PRIMARY KEY (`id_detalle_movimiento`),
  ADD KEY `idx_movimiento_caja` (`id_movimiento_caja`),
  ADD KEY `idx_tipo_movimiento` (`tipo_movimiento`),
  ADD KEY `idx_fecha_movimiento` (`fecha_movimiento`),
  ADD KEY `idx_numero_comprobante` (`numero_comprobante`),
  ADD KEY `fk_caja_movimiento_detalle_usuario` (`id_usuario`);

--
-- Indices de la tabla `carrito`
--
ALTER TABLE `carrito`
  ADD PRIMARY KEY (`id_carrito`),
  ADD UNIQUE KEY `uk_usuario_producto_variante` (`usuario_id`,`id_producto`,`id_variante`),
  ADD KEY `idx_usuario_id` (`usuario_id`),
  ADD KEY `idx_producto_id` (`id_producto`),
  ADD KEY `idx_variante_id` (`id_variante`),
  ADD KEY `idx_usuario_producto` (`usuario_id`,`id_producto`,`id_variante`),
  ADD KEY `idx_session_id` (`session_id`),
  ADD KEY `idx_carrito_session` (`session_id`),
  ADD KEY `idx_carrito_user` (`usuario_id`),
  ADD KEY `idx_carrito_producto_variante` (`id_producto`,`id_variante`);

--
-- Indices de la tabla `catalogo_tipos_gasto`
--
ALTER TABLE `catalogo_tipos_gasto`
  ADD PRIMARY KEY (`id_tipo_gasto`),
  ADD UNIQUE KEY `uk_codigo_tipo_gasto` (`codigo`);

--
-- Indices de la tabla `categorias`
--
ALTER TABLE `categorias`
  ADD PRIMARY KEY (`id_categoria`),
  ADD KEY `idx_categorias_nombre` (`nombre`),
  ADD KEY `idx_categorias_nombre_activo` (`nombre`);

--
-- Indices de la tabla `clientes`
--
ALTER TABLE `clientes`
  ADD PRIMARY KEY (`id_cliente`),
  ADD KEY `fk_clientes_bodega` (`id_bodega`);

--
-- Indices de la tabla `colores`
--
ALTER TABLE `colores`
  ADD PRIMARY KEY (`id_color`),
  ADD UNIQUE KEY `unique_nombre_color` (`nombre`),
  ADD KEY `idx_activo_color` (`activo`),
  ADD KEY `idx_colores_nombre_activo` (`nombre`),
  ADD KEY `idx_colores_nombre` (`nombre`);
ALTER TABLE `colores` ADD FULLTEXT KEY `idx_fulltext_colores` (`nombre`);

--
-- Indices de la tabla `compras`
--
ALTER TABLE `compras`
  ADD PRIMARY KEY (`id_compra`),
  ADD KEY `id_proveedor` (`id_proveedor`),
  ADD KEY `id_usuario` (`id_usuario`),
  ADD KEY `idx_numero_compra` (`numero_compra`),
  ADD KEY `idx_fecha_compra` (`fecha_compra`),
  ADD KEY `idx_proveedor` (`id_proveedor`),
  ADD KEY `idx_bodega` (`id_bodega`),
  ADD KEY `idx_estado` (`estado`),
  ADD KEY `idx_compras_fecha` (`fecha_compra`);

--
-- Indices de la tabla `compras_externas`
--
ALTER TABLE `compras_externas`
  ADD PRIMARY KEY (`id_compra_externa`),
  ADD UNIQUE KEY `numero_compra` (`numero_compra`),
  ADD KEY `idx_bodega` (`id_bodega`),
  ADD KEY `idx_usuario` (`id_usuario`),
  ADD KEY `idx_fecha_compra` (`fecha_compra`),
  ADD KEY `idx_estado` (`estado`);

--
-- Indices de la tabla `compras_externas_detalles`
--
ALTER TABLE `compras_externas_detalles`
  ADD PRIMARY KEY (`id_detalle_compra_externa`),
  ADD KEY `idx_compra_externa` (`id_compra_externa`),
  ADD KEY `idx_producto` (`id_producto`),
  ADD KEY `idx_variante` (`id_variante`);

--
-- Indices de la tabla `compra_detalles`
--
ALTER TABLE `compra_detalles`
  ADD PRIMARY KEY (`id_detalle_compra`),
  ADD KEY `idx_compra` (`id_compra`),
  ADD KEY `idx_producto` (`id_producto`),
  ADD KEY `idx_variante` (`id_variante`);

--
-- Indices de la tabla `configuraciones_tallas`
--
ALTER TABLE `configuraciones_tallas`
  ADD PRIMARY KEY (`id_configuracion`),
  ADD UNIQUE KEY `nombre_configuracion` (`nombre_configuracion`),
  ADD KEY `id_usuario_creador` (`id_usuario_creador`),
  ADD KEY `idx_genero` (`genero`),
  ADD KEY `idx_activo` (`activo`);

--
-- Indices de la tabla `configuraciones_tallas_detalle`
--
ALTER TABLE `configuraciones_tallas_detalle`
  ADD PRIMARY KEY (`id_detalle`),
  ADD UNIQUE KEY `unique_config_talla` (`id_configuracion`,`id_talla`),
  ADD KEY `id_talla` (`id_talla`),
  ADD KEY `idx_configuracion` (`id_configuracion`),
  ADD KEY `idx_orden` (`orden`);

--
-- Indices de la tabla `configuracion_devoluciones`
--
ALTER TABLE `configuracion_devoluciones`
  ADD PRIMARY KEY (`id_configuracion`);

--
-- Indices de la tabla `conteos_inventario`
--
ALTER TABLE `conteos_inventario`
  ADD PRIMARY KEY (`id_conteo`),
  ADD KEY `id_usuario_responsable` (`id_usuario_responsable`),
  ADD KEY `idx_conteos_bodega` (`id_bodega`);

--
-- Indices de la tabla `conversion_caja_traspaso`
--
ALTER TABLE `conversion_caja_traspaso`
  ADD PRIMARY KEY (`id_conversion`),
  ADD KEY `idx_conv_detalle` (`id_detalle_traspaso`),
  ADD KEY `idx_conv_traspaso` (`id_traspaso`),
  ADD KEY `idx_conv_var` (`id_variante_caja`),
  ADD KEY `fk_conv_bodega` (`id_bodega_destino`);

--
-- Indices de la tabla `cotizaciones`
--
ALTER TABLE `cotizaciones`
  ADD PRIMARY KEY (`id_cotizacion`),
  ADD UNIQUE KEY `numero_cotizacion` (`numero_cotizacion`),
  ADD KEY `idx_cliente_cotizacion` (`id_cliente`),
  ADD KEY `idx_usuario_cotizacion` (`id_usuario`),
  ADD KEY `idx_estado_cotizacion` (`estado`),
  ADD KEY `idx_fecha_vencimiento` (`fecha_vencimiento`),
  ADD KEY `fk_cotizacion_venta` (`id_venta_generada`);

--
-- Indices de la tabla `cotizacion_detalles`
--
ALTER TABLE `cotizacion_detalles`
  ADD PRIMARY KEY (`id_detalle_cotizacion`),
  ADD KEY `idx_cotizacion_detalle` (`id_cotizacion`),
  ADD KEY `idx_producto_cotizacion` (`id_producto`),
  ADD KEY `idx_variante_cotizacion` (`id_variante`);

--
-- Indices de la tabla `detalles_conteo_inventario`
--
ALTER TABLE `detalles_conteo_inventario`
  ADD PRIMARY KEY (`id_detalle_conteo`),
  ADD KEY `id_conteo` (`id_conteo`),
  ADD KEY `id_producto` (`id_producto`),
  ADD KEY `id_variante` (`id_variante`),
  ADD KEY `id_usuario_contador` (`id_usuario_contador`);

--
-- Indices de la tabla `devoluciones`
--
ALTER TABLE `devoluciones`
  ADD PRIMARY KEY (`id_devolucion`),
  ADD UNIQUE KEY `numero_devolucion` (`numero_devolucion`),
  ADD KEY `idx_venta_devolucion` (`id_venta`),
  ADD KEY `idx_cliente_devolucion` (`id_cliente`),
  ADD KEY `idx_estado_devolucion` (`estado`),
  ADD KEY `idx_fecha_devolucion` (`fecha_devolucion`),
  ADD KEY `fk_devolucion_usuario_procesa` (`id_usuario_procesa`),
  ADD KEY `fk_devolucion_usuario_autoriza` (`id_usuario_autoriza`),
  ADD KEY `idx_devoluciones_fecha_estado` (`fecha_devolucion`,`estado`);

--
-- Indices de la tabla `devolucion_detalles`
--
ALTER TABLE `devolucion_detalles`
  ADD PRIMARY KEY (`id_detalle_devolucion`),
  ADD KEY `idx_devolucion_detalle` (`id_devolucion`),
  ADD KEY `idx_detalle_venta` (`id_detalle_venta`),
  ADD KEY `idx_producto_devolucion` (`id_producto`),
  ADD KEY `idx_variante_devolucion` (`id_variante`),
  ADD KEY `fk_devolucion_proveedor` (`id_proveedor_original`),
  ADD KEY `idx_devolucion_detalles_variante` (`id_variante`);

--
-- Indices de la tabla `gastos_operativos`
--
ALTER TABLE `gastos_operativos`
  ADD PRIMARY KEY (`id_gasto`),
  ADD KEY `idx_tipo_gasto` (`id_tipo_gasto`),
  ADD KEY `idx_bodega` (`id_bodega`),
  ADD KEY `idx_usuario` (`id_usuario`),
  ADD KEY `idx_movimiento_caja` (`id_movimiento_caja`),
  ADD KEY `idx_fecha_gasto` (`fecha_gasto`),
  ADD KEY `idx_estado` (`estado`);

--
-- Indices de la tabla `historial_cambios_productos`
--
ALTER TABLE `historial_cambios_productos`
  ADD PRIMARY KEY (`id_historial`),
  ADD KEY `idx_producto_historial` (`id_producto`),
  ADD KEY `idx_fecha_historial` (`fecha_cambio`);

--
-- Indices de la tabla `historial_sesiones`
--
ALTER TABLE `historial_sesiones`
  ADD PRIMARY KEY (`id_historial`),
  ADD KEY `id_sesion` (`id_sesion`),
  ADD KEY `idx_usuario_fecha` (`id_usuario`,`fecha_accion`),
  ADD KEY `idx_historial_fecha` (`fecha_accion`),
  ADD KEY `idx_historial_usuario` (`id_usuario`,`fecha_accion`),
  ADD KEY `idx_hist_sesion` (`id_sesion`),
  ADD KEY `idx_hist_usuario` (`id_usuario`);

--
-- Indices de la tabla `inventario_bodega`
--
ALTER TABLE `inventario_bodega`
  ADD PRIMARY KEY (`id_inventario_bodega`),
  ADD UNIQUE KEY `idx_variant_bodega_unique` (`id_variante`,`id_bodega`),
  ADD UNIQUE KEY `uq_ib_bodega_variante` (`id_bodega`,`id_variante`),
  ADD KEY `idx_bodega_inventario` (`id_bodega`),
  ADD KEY `idx_inventario_stock_par` (`Stock_par`),
  ADD KEY `idx_inventario_stock_caja` (`Stock_caja`),
  ADD KEY `idx_ib_bodega_variante_activo` (`id_bodega`,`id_variante`,`activo`),
  ADD KEY `idx_ib_variante_activo` (`id_variante`,`activo`),
  ADD KEY `idx_ib_bodega_activo_stock` (`id_bodega`,`activo`,`Stock_par`),
  ADD KEY `idx_ib_var_bod_act` (`id_variante`,`id_bodega`,`activo`),
  ADD KEY `idx_ib_bod_act` (`id_bodega`,`activo`),
  ADD KEY `idx_inventario_bodega_variante` (`id_variante`),
  ADD KEY `idx_inventario_bodega_bodega` (`id_bodega`),
  ADD KEY `idx_inventario_bodega_activo` (`activo`),
  ADD KEY `idx_inventario_bodega_variante_bodega` (`id_variante`,`id_bodega`),
  ADD KEY `idx_inventario_bodega_completo` (`id_variante`,`id_bodega`,`activo`),
  ADD KEY `idx_inventario_suma_stock` (`id_variante`,`Stock_par`,`Stock_caja`,`activo`),
  ADD KEY `idx_ib_bodega` (`id_bodega`),
  ADD KEY `idx_ib_variante` (`id_variante`),
  ADD KEY `idx_inventario_bodega_compuesto` (`id_bodega`,`id_variante`,`activo`),
  ADD KEY `idx_ib_activo_stock` (`activo`,`Stock_par`),
  ADD KEY `idx_ib_bodega_activo_variante` (`id_bodega`,`activo`,`id_variante`),
  ADD KEY `idx_ib_stock_par` (`Stock_par`),
  ADD KEY `idx_ib_stock_caja` (`Stock_caja`);

--
-- Indices de la tabla `inventario_historial`
--
ALTER TABLE `inventario_historial`
  ADD PRIMARY KEY (`id_historial`),
  ADD KEY `idx_producto` (`id_producto`),
  ADD KEY `idx_variante` (`id_variante`),
  ADD KEY `idx_fecha` (`fecha`);

--
-- Indices de la tabla `inventario_movimientos`
--
ALTER TABLE `inventario_movimientos`
  ADD PRIMARY KEY (`id_movimiento`),
  ADD KEY `id_producto` (`id_producto`),
  ADD KEY `id_variante` (`id_variante`),
  ADD KEY `id_usuario` (`id_usuario`),
  ADD KEY `idx_inventario_mov_busqueda` (`id_producto`,`id_variante`,`fecha_movimiento`);

--
-- Indices de la tabla `marcas`
--
ALTER TABLE `marcas`
  ADD PRIMARY KEY (`id_marca`),
  ADD KEY `idx_marcas_nombre` (`nombre`),
  ADD KEY `idx_marcas_nombre_activo` (`nombre`);
ALTER TABLE `marcas` ADD FULLTEXT KEY `idx_fulltext_marcas` (`nombre`);

--
-- Indices de la tabla `notas_credito`
--
ALTER TABLE `notas_credito`
  ADD PRIMARY KEY (`id_nota_credito`),
  ADD UNIQUE KEY `numero_nota_credito` (`numero_nota_credito`),
  ADD KEY `idx_devolucion_nota` (`id_devolucion`),
  ADD KEY `idx_cliente_nota` (`id_cliente`),
  ADD KEY `idx_estado_nota` (`estado`),
  ADD KEY `idx_fecha_vencimiento` (`fecha_vencimiento`),
  ADD KEY `fk_nota_credito_usuario` (`id_usuario_genera`),
  ADD KEY `fk_nota_credito_venta_aplicada` (`id_venta_aplicada`),
  ADD KEY `idx_notas_credito_vencimiento` (`fecha_vencimiento`,`estado`);

--
-- Indices de la tabla `notificaciones`
--
ALTER TABLE `notificaciones`
  ADD PRIMARY KEY (`id_notificacion`),
  ADD KEY `idx_usuario_destinatario` (`id_usuario_destinatario`),
  ADD KEY `idx_tipo_notificacion` (`tipo`),
  ADD KEY `idx_categoria_notificacion` (`categoria`),
  ADD KEY `idx_leida` (`leida`),
  ADD KEY `idx_fecha_creacion` (`fecha_creacion`),
  ADD KEY `idx_notif_ref_activa` (`tipo_referencia`,`id_referencia`,`activa`),
  ADD KEY `idx_notif_categoria_tipo` (`categoria`,`tipo`),
  ADD KEY `idx_notif_tr_ref` (`tipo_referencia`,`id_referencia`,`evento`,`activa`),
  ADD KEY `idx_notif_bodegas_lectura` (`id_bodega_origen`,`id_bodega_destino`,`leida`,`activa`,`evento`),
  ADD KEY `idx_notif_user_lectura` (`id_usuario_destinatario`,`leida`,`activa`),
  ADD KEY `idx_tipo_referencia` (`tipo_referencia`),
  ADD KEY `idx_evento` (`evento`);

--
-- Indices de la tabla `ordenes_reserva`
--
ALTER TABLE `ordenes_reserva`
  ADD PRIMARY KEY (`id_orden`),
  ADD KEY `idx_or_bodega_estado` (`id_bodega`,`estado`),
  ADD KEY `idx_or_usuario_fecha` (`id_usuario`,`fecha_creacion`);

--
-- Indices de la tabla `ordenes_reserva_detalle`
--
ALTER TABLE `ordenes_reserva_detalle`
  ADD PRIMARY KEY (`id_detalle`),
  ADD KEY `id_orden` (`id_orden`),
  ADD KEY `id_producto` (`id_producto`),
  ADD KEY `id_variante` (`id_variante`);

--
-- Indices de la tabla `permisos`
--
ALTER TABLE `permisos`
  ADD PRIMARY KEY (`id_permiso`),
  ADD UNIQUE KEY `modulo` (`modulo`);

--
-- Indices de la tabla `prestamos_zapatos`
--
ALTER TABLE `prestamos_zapatos`
  ADD PRIMARY KEY (`id_prestamo`),
  ADD KEY `idx_variante_estado` (`id_variante`,`estado`),
  ADD KEY `idx_bodega_variante` (`id_bodega`,`id_variante`),
  ADD KEY `idx_usuario` (`id_usuario`),
  ADD KEY `fk_prestamo_producto` (`id_producto`);

--
-- Indices de la tabla `privilegios_rol`
--
ALTER TABLE `privilegios_rol`
  ADD PRIMARY KEY (`id_privilegio`),
  ADD KEY `id_rol` (`id_rol`),
  ADD KEY `id_permiso` (`id_permiso`);

--
-- Indices de la tabla `privilegio_rol`
--
ALTER TABLE `privilegio_rol`
  ADD PRIMARY KEY (`id_privilegio`),
  ADD UNIQUE KEY `uk_rol_permiso` (`id_rol`,`id_permiso`),
  ADD KEY `idx_id_rol` (`id_rol`),
  ADD KEY `idx_id_permiso` (`id_permiso`);

--
-- Indices de la tabla `privilegio_usuario`
--
ALTER TABLE `privilegio_usuario`
  ADD PRIMARY KEY (`id_privilegio_usuario`),
  ADD UNIQUE KEY `uk_usuario_permiso` (`id_usuario`,`id_permiso`),
  ADD KEY `idx_id_usuario` (`id_usuario`),
  ADD KEY `idx_id_permiso` (`id_permiso`);

--
-- Indices de la tabla `productos`
--
ALTER TABLE `productos`
  ADD PRIMARY KEY (`id_producto`),
  ADD UNIQUE KEY `codigo_modelo` (`codigo_modelo`),
  ADD KEY `id_categoria` (`id_categoria`),
  ADD KEY `id_marca` (`id_marca`),
  ADD KEY `id_proveedor` (`id_proveedor`),
  ADD KEY `idx_genero` (`genero`),
  ADD KEY `idx_activo` (`activo`),
  ADD KEY `idx_productos_nombre_activo` (`activo`,`nombre`),
  ADD KEY `idx_productos_codigo_modelo_activo` (`codigo_modelo`,`activo`),
  ADD KEY `idx_productos_codigo_modelo` (`codigo_modelo`),
  ADD KEY `idx_productos_nombre` (`nombre`),
  ADD KEY `idx_productos_fecha_creacion` (`fecha_creacion` DESC,`activo`),
  ADD KEY `idx_productos_genero_activo` (`genero`,`activo`),
  ADD KEY `idx_productos_precio` (`precio_venta`),
  ADD KEY `idx_producto_categoria_precio` (`id_categoria`,`precio_venta`),
  ADD KEY `idx_p_activo_nombre` (`activo`,`nombre`),
  ADD KEY `idx_p_codigo_modelo_activo` (`codigo_modelo`,`activo`),
  ADD KEY `idx_p_marca_activo` (`id_marca`,`activo`),
  ADD KEY `idx_p_categoria_activo` (`id_categoria`,`activo`),
  ADD KEY `idx_p_genero_activo` (`genero`,`activo`),
  ADD KEY `idx_prod_nombre` (`nombre`),
  ADD KEY `idx_prod_modelo` (`codigo_modelo`),
  ADD KEY `idx_productos_activo` (`activo`),
  ADD KEY `idx_productos_categoria` (`id_categoria`),
  ADD KEY `idx_productos_marca` (`id_marca`),
  ADD KEY `idx_productos_busqueda_comun` (`nombre`,`codigo_modelo`,`activo`,`id_categoria`,`id_marca`),
  ADD KEY `idx_prod_fecha` (`fecha_creacion`);
ALTER TABLE `productos` ADD FULLTEXT KEY `idx_productos_nombre_fulltext` (`nombre`);
ALTER TABLE `productos` ADD FULLTEXT KEY `idx_productos_codigo_nombre_fulltext` (`codigo_modelo`,`nombre`);
ALTER TABLE `productos` ADD FULLTEXT KEY `idx_fulltext_productos` (`nombre`,`descripcion`,`codigo_modelo`);

--
-- Indices de la tabla `productos_cache`
--
ALTER TABLE `productos_cache`
  ADD PRIMARY KEY (`id_producto`),
  ADD KEY `idx_cache_activo_precio` (`activo`,`precio_venta`),
  ADD KEY `idx_cache_activo_fecha` (`activo`,`fecha_creacion`),
  ADD KEY `idx_cache_stock` (`stock_disponible`);
ALTER TABLE `productos_cache` ADD FULLTEXT KEY `idx_cache_search` (`nombre`,`descripcion`,`codigo_modelo`);

--
-- Indices de la tabla `productos_genericos`
--
ALTER TABLE `productos_genericos`
  ADD PRIMARY KEY (`id_generico`),
  ADD UNIQUE KEY `id_variante` (`id_variante`),
  ADD KEY `idx_variante` (`id_variante`);

--
-- Indices de la tabla `producto_variantes`
--
ALTER TABLE `producto_variantes`
  ADD PRIMARY KEY (`id_variante`),
  ADD UNIQUE KEY `unique_sku` (`sku`),
  ADD UNIQUE KEY `unique_ean` (`ean`),
  ADD UNIQUE KEY `unique_producto_talla_color_proveedor` (`id_producto`,`id_talla`,`id_color`,`id_proveedor`),
  ADD KEY `idx_talla` (`id_talla`),
  ADD KEY `idx_color` (`id_color`),
  ADD KEY `idx_disponible` (`disponible`),
  ADD KEY `idx_imagen` (`imagen`(191)),
  ADD KEY `idx_producto_disponible` (`id_producto`,`disponible`),
  ADD KEY `fk_variante_proveedor` (`id_proveedor`),
  ADD KEY `idx_producto_variantes_proveedor_disponible` (`id_proveedor`,`disponible`),
  ADD KEY `idx_pv_imagen` (`imagen`(3072)),
  ADD KEY `idx_pv_producto_disponible_opt` (`id_producto`,`disponible`),
  ADD KEY `idx_pv_talla_color_disp` (`id_talla`,`id_color`,`disponible`),
  ADD KEY `idx_pv_proveedor_disp` (`id_proveedor`,`disponible`),
  ADD KEY `idx_pv_prod_disp` (`id_producto`,`disponible`),
  ADD KEY `idx_pv_producto_disponible` (`id_producto`,`disponible`),
  ADD KEY `idx_producto_variantes_producto` (`id_producto`),
  ADD KEY `idx_producto_variantes_color` (`id_color`),
  ADD KEY `idx_producto_variantes_talla` (`id_talla`),
  ADD KEY `idx_producto_variantes_disponible` (`disponible`),
  ADD KEY `idx_producto_variantes_producto_disponible` (`id_producto`,`disponible`),
  ADD KEY `idx_variantes_inventario` (`id_producto`,`disponible`,`id_color`,`id_talla`),
  ADD KEY `idx_pv_producto` (`id_producto`),
  ADD KEY `idx_pv_talla` (`id_talla`),
  ADD KEY `idx_pv_color` (`id_color`),
  ADD KEY `idx_producto_variantes_ean` (`ean`),
  ADD KEY `idx_pv_proveedor` (`id_proveedor`),
  ADD KEY `idx_pv_sku` (`sku`),
  ADD KEY `idx_pv_disponible` (`disponible`);
ALTER TABLE `producto_variantes` ADD FULLTEXT KEY `idx_variantes_ean_fulltext` (`ean`);

--
-- Indices de la tabla `promociones`
--
ALTER TABLE `promociones`
  ADD PRIMARY KEY (`id_promocion`),
  ADD UNIQUE KEY `codigo` (`codigo`),
  ADD KEY `idx_prom_fechas` (`activa`,`fecha_inicio`,`fecha_fin`),
  ADD KEY `idx_prom_tipo` (`tipo_descuento`);

--
-- Indices de la tabla `promociones_detalle`
--
ALTER TABLE `promociones_detalle`
  ADD PRIMARY KEY (`id_detalle`),
  ADD UNIQUE KEY `uniq_prom_objetivo` (`id_promocion`,`id_categoria`,`id_marca`,`id_producto`,`rol_usuario`,`id_usuario`),
  ADD KEY `idx_det_tipo` (`tipo_aplicacion`,`activo`),
  ADD KEY `idx_det_categoria` (`id_categoria`),
  ADD KEY `idx_det_marca` (`id_marca`),
  ADD KEY `idx_det_producto` (`id_producto`),
  ADD KEY `idx_det_rol` (`rol_usuario`),
  ADD KEY `idx_det_usuario` (`id_usuario`),
  ADD KEY `idx_pd_tipo_prod` (`tipo_aplicacion`,`id_producto`),
  ADD KEY `idx_pd_tipo_marca` (`tipo_aplicacion`,`id_marca`),
  ADD KEY `idx_pd_tipo_cat` (`tipo_aplicacion`,`id_categoria`);

--
-- Indices de la tabla `proveedores`
--
ALTER TABLE `proveedores`
  ADD PRIMARY KEY (`id_proveedor`),
  ADD KEY `idx_proveedores_nombre` (`nombre`);

--
-- Indices de la tabla `rentabilidad_mensual`
--
ALTER TABLE `rentabilidad_mensual`
  ADD PRIMARY KEY (`id_rentabilidad`),
  ADD UNIQUE KEY `periodo` (`periodo`);

--
-- Indices de la tabla `reservas`
--
ALTER TABLE `reservas`
  ADD PRIMARY KEY (`id_reserva`),
  ADD UNIQUE KEY `numero_reserva` (`numero_reserva`),
  ADD KEY `idx_cliente_reserva` (`id_cliente`),
  ADD KEY `idx_usuario_reserva` (`id_usuario`),
  ADD KEY `idx_estado_reserva` (`estado`),
  ADD KEY `idx_fecha_vencimiento_reserva` (`fecha_vencimiento`),
  ADD KEY `fk_reserva_venta` (`id_venta_generada`);

--
-- Indices de la tabla `roles`
--
ALTER TABLE `roles`
  ADD PRIMARY KEY (`id_rol`),
  ADD UNIQUE KEY `nombre` (`nombre`);

--
-- Indices de la tabla `sesiones_activas`
--
ALTER TABLE `sesiones_activas`
  ADD PRIMARY KEY (`id_sesion`),
  ADD UNIQUE KEY `token_sesion` (`token_sesion`),
  ADD KEY `id_caja_abierta` (`id_caja_abierta`),
  ADD KEY `id_movimiento_caja` (`id_movimiento_caja`),
  ADD KEY `idx_usuario_activo` (`id_usuario`,`estado`),
  ADD KEY `idx_token` (`token_sesion`),
  ADD KEY `idx_fecha_ping` (`fecha_ultimo_ping`),
  ADD KEY `idx_sesiones_ultimo_ping` (`fecha_ultimo_ping`),
  ADD KEY `idx_sesiones_usuario_estado` (`id_usuario`,`estado`,`fecha_inicio`),
  ADD KEY `idx_sesiones_token` (`token_sesion`),
  ADD KEY `idx_sesiones_ping` (`fecha_ultimo_ping`),
  ADD KEY `idx_sesiones_usuario` (`id_usuario`);

--
-- Indices de la tabla `tallas`
--
ALTER TABLE `tallas`
  ADD PRIMARY KEY (`id_talla`),
  ADD UNIQUE KEY `unique_talla_sistema_genero` (`numero`,`sistema`,`genero`),
  ADD KEY `idx_sistema` (`sistema`),
  ADD KEY `idx_genero_talla` (`genero`),
  ADD KEY `idx_tallas_numero` (`numero`);

--
-- Indices de la tabla `tallas_bak`
--
ALTER TABLE `tallas_bak`
  ADD PRIMARY KEY (`id_talla`),
  ADD UNIQUE KEY `unique_talla_sistema_genero` (`numero`,`sistema`,`genero`),
  ADD KEY `idx_sistema` (`sistema`),
  ADD KEY `idx_genero_talla` (`genero`);

--
-- Indices de la tabla `tipos_gastos`
--
ALTER TABLE `tipos_gastos`
  ADD PRIMARY KEY (`id_tipo_gasto`),
  ADD UNIQUE KEY `codigo` (`codigo`),
  ADD KEY `idx_codigo` (`codigo`),
  ADD KEY `idx_categoria` (`categoria`);

--
-- Indices de la tabla `traspasos`
--
ALTER TABLE `traspasos`
  ADD PRIMARY KEY (`id_traspaso`),
  ADD UNIQUE KEY `numero_traspaso` (`numero_traspaso`),
  ADD KEY `idx_bodega_origen` (`id_bodega_origen`),
  ADD KEY `idx_bodega_destino` (`id_bodega_destino`),
  ADD KEY `idx_usuario_solicita` (`id_usuario_solicita`),
  ADD KEY `idx_estado_traspaso` (`estado`),
  ADD KEY `fk_traspaso_usuario_autoriza` (`id_usuario_autoriza`),
  ADD KEY `fk_traspaso_usuario_recibe` (`id_usuario_recibe`),
  ADD KEY `idx_traspasos_estado` (`estado`),
  ADD KEY `idx_traspasos_bodega_origen` (`id_bodega_origen`),
  ADD KEY `idx_traspasos_bodega_destino` (`id_bodega_destino`),
  ADD KEY `idx_traspasos_fecha_solicitud` (`fecha_solicitud`),
  ADD KEY `idx_traspasos_compuesto` (`estado`,`id_bodega_origen`,`id_bodega_destino`,`fecha_solicitud`);

--
-- Indices de la tabla `traspaso_auditoria`
--
ALTER TABLE `traspaso_auditoria`
  ADD PRIMARY KEY (`id_auditoria`),
  ADD KEY `idx_ta_traspaso` (`id_traspaso`),
  ADD KEY `idx_ta_usuario` (`id_usuario`);

--
-- Indices de la tabla `traspaso_detalles`
--
ALTER TABLE `traspaso_detalles`
  ADD PRIMARY KEY (`id_detalle_traspaso`),
  ADD KEY `idx_traspaso` (`id_traspaso`),
  ADD KEY `idx_producto_traspaso` (`id_producto`),
  ADD KEY `idx_variante_traspaso` (`id_variante`),
  ADD KEY `idx_traspaso_detalles_variante` (`id_variante`),
  ADD KEY `idx_traspaso_detalles_traspaso` (`id_traspaso`),
  ADD KEY `idx_traspaso_detalles_producto` (`id_producto`);

--
-- Indices de la tabla `usuarios`
--
ALTER TABLE `usuarios`
  ADD PRIMARY KEY (`id_usuario`),
  ADD UNIQUE KEY `username` (`username`),
  ADD KEY `fk_usuario_bodega` (`id_bodega`),
  ADD KEY `idx_usuarios_username` (`username`),
  ADD KEY `idx_usuarios_username_password` (`username`,`password`),
  ADD KEY `idx_usuarios_username_activo` (`username`,`activo`),
  ADD KEY `idx_usuarios_login` (`username`,`password`,`activo`),
  ADD KEY `id_rol` (`id_rol`);

--
-- Indices de la tabla `ventas`
--
ALTER TABLE `ventas`
  ADD PRIMARY KEY (`id_venta`),
  ADD KEY `id_cliente` (`id_cliente`),
  ADD KEY `id_usuario` (`id_usuario`),
  ADD KEY `id_caja` (`id_caja`),
  ADD KEY `id_movimiento` (`id_movimiento`),
  ADD KEY `idx_ventas_cliente_usuario` (`id_cliente`,`id_usuario`,`id_caja`),
  ADD KEY `idx_ventas_fecha_usuario` (`fecha_venta`,`id_usuario`),
  ADD KEY `idx_ventas_cliente` (`id_cliente`),
  ADD KEY `idx_ventas_fecha` (`fecha_venta`),
  ADD KEY `idx_ventas_caja` (`id_caja`),
  ADD KEY `idx_ventas_estado` (`estado`);

--
-- Indices de la tabla `venta_detalles`
--
ALTER TABLE `venta_detalles`
  ADD PRIMARY KEY (`id_detalle`),
  ADD KEY `id_venta` (`id_venta`),
  ADD KEY `id_producto` (`id_producto`),
  ADD KEY `id_variante` (`id_variante`),
  ADD KEY `idx_venta_detalles_refs` (`id_venta`,`id_producto`,`id_variante`),
  ADD KEY `idx_venta_detalles_variante` (`id_variante`),
  ADD KEY `idx_venta_detalles_venta` (`id_venta`,`id_variante`);

--
-- Indices de la tabla `venta_medios_pago`
--
ALTER TABLE `venta_medios_pago`
  ADD PRIMARY KEY (`id_medio_pago`),
  ADD KEY `idx_venta` (`id_venta`),
  ADD KEY `idx_tipo` (`tipo_pago`),
  ADD KEY `idx_fecha` (`fecha_registro`);

--
-- Indices de la tabla `venta_pagos`
--
ALTER TABLE `venta_pagos`
  ADD PRIMARY KEY (`id_pago`),
  ADD KEY `idx_venta_pago` (`id_venta`),
  ADD KEY `idx_tipo_pago` (`tipo_pago`),
  ADD KEY `idx_estado_pago` (`estado`);

--
-- AUTO_INCREMENT de las tablas volcadas
--

--
-- AUTO_INCREMENT de la tabla `abonos`
--
ALTER TABLE `abonos`
  MODIFY `id_abono` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `ajustes_inventario`
--
ALTER TABLE `ajustes_inventario`
  MODIFY `id_ajuste` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `aplicaciones_nota_credito`
--
ALTER TABLE `aplicaciones_nota_credito`
  MODIFY `id_aplicacion` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `auditoria_trazabilidad`
--
ALTER TABLE `auditoria_trazabilidad`
  MODIFY `id_auditoria` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `bodega_sectores`
--
ALTER TABLE `bodega_sectores`
  MODIFY `id_sector` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `busquedas`
--
ALTER TABLE `busquedas`
  MODIFY `id_busqueda` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `cajas`
--
ALTER TABLE `cajas`
  MODIFY `id_caja` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `caja_menor`
--
ALTER TABLE `caja_menor`
  MODIFY `id_caja_menor` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `caja_movimientos`
--
ALTER TABLE `caja_movimientos`
  MODIFY `id_movimiento` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `caja_movimiento_detalle`
--
ALTER TABLE `caja_movimiento_detalle`
  MODIFY `id_detalle_movimiento` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `carrito`
--
ALTER TABLE `carrito`
  MODIFY `id_carrito` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID único del item en el carrito';

--
-- AUTO_INCREMENT de la tabla `catalogo_tipos_gasto`
--
ALTER TABLE `catalogo_tipos_gasto`
  MODIFY `id_tipo_gasto` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `categorias`
--
ALTER TABLE `categorias`
  MODIFY `id_categoria` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `clientes`
--
ALTER TABLE `clientes`
  MODIFY `id_cliente` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `colores`
--
ALTER TABLE `colores`
  MODIFY `id_color` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `compras`
--
ALTER TABLE `compras`
  MODIFY `id_compra` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `compras_externas`
--
ALTER TABLE `compras_externas`
  MODIFY `id_compra_externa` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `compras_externas_detalles`
--
ALTER TABLE `compras_externas_detalles`
  MODIFY `id_detalle_compra_externa` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `compra_detalles`
--
ALTER TABLE `compra_detalles`
  MODIFY `id_detalle_compra` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `configuraciones_tallas`
--
ALTER TABLE `configuraciones_tallas`
  MODIFY `id_configuracion` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `configuraciones_tallas_detalle`
--
ALTER TABLE `configuraciones_tallas_detalle`
  MODIFY `id_detalle` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `configuracion_devoluciones`
--
ALTER TABLE `configuracion_devoluciones`
  MODIFY `id_configuracion` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `conteos_inventario`
--
ALTER TABLE `conteos_inventario`
  MODIFY `id_conteo` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `conversion_caja_traspaso`
--
ALTER TABLE `conversion_caja_traspaso`
  MODIFY `id_conversion` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `cotizaciones`
--
ALTER TABLE `cotizaciones`
  MODIFY `id_cotizacion` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `cotizacion_detalles`
--
ALTER TABLE `cotizacion_detalles`
  MODIFY `id_detalle_cotizacion` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `detalles_conteo_inventario`
--
ALTER TABLE `detalles_conteo_inventario`
  MODIFY `id_detalle_conteo` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `devoluciones`
--
ALTER TABLE `devoluciones`
  MODIFY `id_devolucion` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `devolucion_detalles`
--
ALTER TABLE `devolucion_detalles`
  MODIFY `id_detalle_devolucion` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `gastos_operativos`
--
ALTER TABLE `gastos_operativos`
  MODIFY `id_gasto` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `historial_cambios_productos`
--
ALTER TABLE `historial_cambios_productos`
  MODIFY `id_historial` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `historial_sesiones`
--
ALTER TABLE `historial_sesiones`
  MODIFY `id_historial` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `inventario_bodega`
--
ALTER TABLE `inventario_bodega`
  MODIFY `id_inventario_bodega` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `inventario_historial`
--
ALTER TABLE `inventario_historial`
  MODIFY `id_historial` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `inventario_movimientos`
--
ALTER TABLE `inventario_movimientos`
  MODIFY `id_movimiento` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `marcas`
--
ALTER TABLE `marcas`
  MODIFY `id_marca` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `notas_credito`
--
ALTER TABLE `notas_credito`
  MODIFY `id_nota_credito` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `notificaciones`
--
ALTER TABLE `notificaciones`
  MODIFY `id_notificacion` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `ordenes_reserva`
--
ALTER TABLE `ordenes_reserva`
  MODIFY `id_orden` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `ordenes_reserva_detalle`
--
ALTER TABLE `ordenes_reserva_detalle`
  MODIFY `id_detalle` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `permisos`
--
ALTER TABLE `permisos`
  MODIFY `id_permiso` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `prestamos_zapatos`
--
ALTER TABLE `prestamos_zapatos`
  MODIFY `id_prestamo` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `privilegios_rol`
--
ALTER TABLE `privilegios_rol`
  MODIFY `id_privilegio` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `privilegio_rol`
--
ALTER TABLE `privilegio_rol`
  MODIFY `id_privilegio` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `privilegio_usuario`
--
ALTER TABLE `privilegio_usuario`
  MODIFY `id_privilegio_usuario` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `productos`
--
ALTER TABLE `productos`
  MODIFY `id_producto` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `productos_genericos`
--
ALTER TABLE `productos_genericos`
  MODIFY `id_generico` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `producto_variantes`
--
ALTER TABLE `producto_variantes`
  MODIFY `id_variante` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `promociones`
--
ALTER TABLE `promociones`
  MODIFY `id_promocion` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `promociones_detalle`
--
ALTER TABLE `promociones_detalle`
  MODIFY `id_detalle` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `proveedores`
--
ALTER TABLE `proveedores`
  MODIFY `id_proveedor` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `rentabilidad_mensual`
--
ALTER TABLE `rentabilidad_mensual`
  MODIFY `id_rentabilidad` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `reservas`
--
ALTER TABLE `reservas`
  MODIFY `id_reserva` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `roles`
--
ALTER TABLE `roles`
  MODIFY `id_rol` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `sesiones_activas`
--
ALTER TABLE `sesiones_activas`
  MODIFY `id_sesion` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `tallas`
--
ALTER TABLE `tallas`
  MODIFY `id_talla` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `tallas_bak`
--
ALTER TABLE `tallas_bak`
  MODIFY `id_talla` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `tipos_gastos`
--
ALTER TABLE `tipos_gastos`
  MODIFY `id_tipo_gasto` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `traspasos`
--
ALTER TABLE `traspasos`
  MODIFY `id_traspaso` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `traspaso_auditoria`
--
ALTER TABLE `traspaso_auditoria`
  MODIFY `id_auditoria` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `traspaso_detalles`
--
ALTER TABLE `traspaso_detalles`
  MODIFY `id_detalle_traspaso` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `usuarios`
--
ALTER TABLE `usuarios`
  MODIFY `id_usuario` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `ventas`
--
ALTER TABLE `ventas`
  MODIFY `id_venta` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `venta_detalles`
--
ALTER TABLE `venta_detalles`
  MODIFY `id_detalle` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `venta_medios_pago`
--
ALTER TABLE `venta_medios_pago`
  MODIFY `id_medio_pago` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `venta_pagos`
--
ALTER TABLE `venta_pagos`
  MODIFY `id_pago` int(11) NOT NULL AUTO_INCREMENT;

--
-- Restricciones para tablas volcadas
--

--
-- Filtros para la tabla `abonos`
--
ALTER TABLE `abonos`
  ADD CONSTRAINT `fk_abonos_compra` FOREIGN KEY (`id_compra`) REFERENCES `compras` (`id_compra`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `ajustes_inventario`
--
ALTER TABLE `ajustes_inventario`
  ADD CONSTRAINT `ajustes_inventario_ibfk_1` FOREIGN KEY (`id_detalle_conteo`) REFERENCES `detalles_conteo_inventario` (`id_detalle_conteo`),
  ADD CONSTRAINT `ajustes_inventario_ibfk_2` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`),
  ADD CONSTRAINT `ajustes_inventario_ibfk_3` FOREIGN KEY (`id_usuario_creador`) REFERENCES `usuarios` (`id_usuario`),
  ADD CONSTRAINT `ajustes_inventario_ibfk_4` FOREIGN KEY (`id_usuario_aprobador`) REFERENCES `usuarios` (`id_usuario`),
  ADD CONSTRAINT `ajustes_inventario_ibfk_5` FOREIGN KEY (`id_variante`) REFERENCES `producto_variantes` (`id_variante`);

--
-- Filtros para la tabla `aplicaciones_nota_credito`
--
ALTER TABLE `aplicaciones_nota_credito`
  ADD CONSTRAINT `fk_aplicacion_nota_credito` FOREIGN KEY (`id_nota_credito`) REFERENCES `notas_credito` (`id_nota_credito`),
  ADD CONSTRAINT `fk_aplicacion_usuario` FOREIGN KEY (`id_usuario_aplica`) REFERENCES `usuarios` (`id_usuario`),
  ADD CONSTRAINT `fk_aplicacion_venta` FOREIGN KEY (`id_venta`) REFERENCES `ventas` (`id_venta`);

--
-- Filtros para la tabla `auditoria_trazabilidad`
--
ALTER TABLE `auditoria_trazabilidad`
  ADD CONSTRAINT `fk_auditoria_bodega_destino` FOREIGN KEY (`id_bodega_destino`) REFERENCES `bodegas` (`id_bodega`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_auditoria_bodega_origen` FOREIGN KEY (`id_bodega_origen`) REFERENCES `bodegas` (`id_bodega`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_auditoria_proveedor` FOREIGN KEY (`id_proveedor`) REFERENCES `proveedores` (`id_proveedor`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_auditoria_variante` FOREIGN KEY (`id_variante`) REFERENCES `producto_variantes` (`id_variante`) ON DELETE CASCADE;

--
-- Filtros para la tabla `bodega_sectores`
--
ALTER TABLE `bodega_sectores`
  ADD CONSTRAINT `fk_sector_bodega` FOREIGN KEY (`id_bodega`) REFERENCES `bodegas` (`id_bodega`);

--
-- Filtros para la tabla `cajas`
--
ALTER TABLE `cajas`
  ADD CONSTRAINT `fk_caja_bodega` FOREIGN KEY (`id_bodega`) REFERENCES `bodegas` (`id_bodega`) ON UPDATE CASCADE;

--
-- Filtros para la tabla `caja_menor`
--
ALTER TABLE `caja_menor`
  ADD CONSTRAINT `fk_caja_menor_caja_principal` FOREIGN KEY (`id_caja_principal`) REFERENCES `cajas` (`id_caja`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_caja_menor_responsable` FOREIGN KEY (`responsable_actual`) REFERENCES `usuarios` (`id_usuario`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Filtros para la tabla `caja_movimientos`
--
ALTER TABLE `caja_movimientos`
  ADD CONSTRAINT `caja_movimientos_ibfk_1` FOREIGN KEY (`id_caja`) REFERENCES `cajas` (`id_caja`),
  ADD CONSTRAINT `caja_movimientos_ibfk_2` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`);

--
-- Filtros para la tabla `caja_movimiento_detalle`
--
ALTER TABLE `caja_movimiento_detalle`
  ADD CONSTRAINT `fk_caja_movimiento_detalle_movimiento` FOREIGN KEY (`id_movimiento_caja`) REFERENCES `caja_movimientos` (`id_movimiento`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_caja_movimiento_detalle_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`) ON UPDATE CASCADE;

--
-- Filtros para la tabla `carrito`
--
ALTER TABLE `carrito`
  ADD CONSTRAINT `fk_carrito_producto` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_carrito_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id_usuario`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_carrito_variante` FOREIGN KEY (`id_variante`) REFERENCES `producto_variantes` (`id_variante`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `clientes`
--
ALTER TABLE `clientes`
  ADD CONSTRAINT `fk_clientes_bodega` FOREIGN KEY (`id_bodega`) REFERENCES `bodegas` (`id_bodega`);

--
-- Filtros para la tabla `compras`
--
ALTER TABLE `compras`
  ADD CONSTRAINT `compras_ibfk_1` FOREIGN KEY (`id_proveedor`) REFERENCES `proveedores` (`id_proveedor`),
  ADD CONSTRAINT `compras_ibfk_2` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`);

--
-- Filtros para la tabla `compras_externas`
--
ALTER TABLE `compras_externas`
  ADD CONSTRAINT `fk_compra_ext_bodega` FOREIGN KEY (`id_bodega`) REFERENCES `bodegas` (`id_bodega`),
  ADD CONSTRAINT `fk_compra_ext_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`);

--
-- Filtros para la tabla `compras_externas_detalles`
--
ALTER TABLE `compras_externas_detalles`
  ADD CONSTRAINT `fk_det_compra_ext_compra` FOREIGN KEY (`id_compra_externa`) REFERENCES `compras_externas` (`id_compra_externa`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_det_compra_ext_producto` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`),
  ADD CONSTRAINT `fk_det_compra_ext_variante` FOREIGN KEY (`id_variante`) REFERENCES `producto_variantes` (`id_variante`);

--
-- Filtros para la tabla `compra_detalles`
--
ALTER TABLE `compra_detalles`
  ADD CONSTRAINT `fk_detalle_compra` FOREIGN KEY (`id_compra`) REFERENCES `compras` (`id_compra`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_detalle_producto` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`),
  ADD CONSTRAINT `fk_detalle_variante` FOREIGN KEY (`id_variante`) REFERENCES `producto_variantes` (`id_variante`);

--
-- Filtros para la tabla `configuraciones_tallas`
--
ALTER TABLE `configuraciones_tallas`
  ADD CONSTRAINT `configuraciones_tallas_ibfk_1` FOREIGN KEY (`id_usuario_creador`) REFERENCES `usuarios` (`id_usuario`) ON DELETE SET NULL;

--
-- Filtros para la tabla `configuraciones_tallas_detalle`
--
ALTER TABLE `configuraciones_tallas_detalle`
  ADD CONSTRAINT `configuraciones_tallas_detalle_ibfk_1` FOREIGN KEY (`id_configuracion`) REFERENCES `configuraciones_tallas` (`id_configuracion`) ON DELETE CASCADE,
  ADD CONSTRAINT `configuraciones_tallas_detalle_ibfk_2` FOREIGN KEY (`id_talla`) REFERENCES `tallas` (`id_talla`) ON DELETE CASCADE;

--
-- Filtros para la tabla `conteos_inventario`
--
ALTER TABLE `conteos_inventario`
  ADD CONSTRAINT `conteos_inventario_ibfk_1` FOREIGN KEY (`id_usuario_responsable`) REFERENCES `usuarios` (`id_usuario`),
  ADD CONSTRAINT `fk_conteos_bodega` FOREIGN KEY (`id_bodega`) REFERENCES `bodegas` (`id_bodega`) ON UPDATE CASCADE;

--
-- Filtros para la tabla `conversion_caja_traspaso`
--
ALTER TABLE `conversion_caja_traspaso`
  ADD CONSTRAINT `fk_conv_bodega` FOREIGN KEY (`id_bodega_destino`) REFERENCES `bodegas` (`id_bodega`),
  ADD CONSTRAINT `fk_conv_detalle` FOREIGN KEY (`id_detalle_traspaso`) REFERENCES `traspaso_detalles` (`id_detalle_traspaso`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_conv_traspaso` FOREIGN KEY (`id_traspaso`) REFERENCES `traspasos` (`id_traspaso`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_conv_variante` FOREIGN KEY (`id_variante_caja`) REFERENCES `producto_variantes` (`id_variante`);

--
-- Filtros para la tabla `cotizaciones`
--
ALTER TABLE `cotizaciones`
  ADD CONSTRAINT `fk_cotizacion_cliente` FOREIGN KEY (`id_cliente`) REFERENCES `clientes` (`id_cliente`),
  ADD CONSTRAINT `fk_cotizacion_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`),
  ADD CONSTRAINT `fk_cotizacion_venta` FOREIGN KEY (`id_venta_generada`) REFERENCES `ventas` (`id_venta`);

--
-- Filtros para la tabla `cotizacion_detalles`
--
ALTER TABLE `cotizacion_detalles`
  ADD CONSTRAINT `fk_detalle_cotizacion` FOREIGN KEY (`id_cotizacion`) REFERENCES `cotizaciones` (`id_cotizacion`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_detalle_producto_cotizacion` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`),
  ADD CONSTRAINT `fk_detalle_variante_cotizacion` FOREIGN KEY (`id_variante`) REFERENCES `producto_variantes` (`id_variante`);

--
-- Filtros para la tabla `detalles_conteo_inventario`
--
ALTER TABLE `detalles_conteo_inventario`
  ADD CONSTRAINT `detalles_conteo_inventario_ibfk_1` FOREIGN KEY (`id_conteo`) REFERENCES `conteos_inventario` (`id_conteo`),
  ADD CONSTRAINT `detalles_conteo_inventario_ibfk_2` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`),
  ADD CONSTRAINT `detalles_conteo_inventario_ibfk_3` FOREIGN KEY (`id_usuario_contador`) REFERENCES `usuarios` (`id_usuario`),
  ADD CONSTRAINT `detalles_conteo_inventario_ibfk_4` FOREIGN KEY (`id_variante`) REFERENCES `producto_variantes` (`id_variante`);

--
-- Filtros para la tabla `devoluciones`
--
ALTER TABLE `devoluciones`
  ADD CONSTRAINT `fk_devolucion_cliente` FOREIGN KEY (`id_cliente`) REFERENCES `clientes` (`id_cliente`),
  ADD CONSTRAINT `fk_devolucion_usuario_autoriza` FOREIGN KEY (`id_usuario_autoriza`) REFERENCES `usuarios` (`id_usuario`),
  ADD CONSTRAINT `fk_devolucion_usuario_procesa` FOREIGN KEY (`id_usuario_procesa`) REFERENCES `usuarios` (`id_usuario`),
  ADD CONSTRAINT `fk_devolucion_venta` FOREIGN KEY (`id_venta`) REFERENCES `ventas` (`id_venta`);

--
-- Filtros para la tabla `devolucion_detalles`
--
ALTER TABLE `devolucion_detalles`
  ADD CONSTRAINT `fk_detalle_devolucion` FOREIGN KEY (`id_devolucion`) REFERENCES `devoluciones` (`id_devolucion`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_detalle_venta_devolucion` FOREIGN KEY (`id_detalle_venta`) REFERENCES `venta_detalles` (`id_detalle`),
  ADD CONSTRAINT `fk_devolucion_detalles_proveedor` FOREIGN KEY (`id_proveedor_original`) REFERENCES `proveedores` (`id_proveedor`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_producto_devolucion` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`),
  ADD CONSTRAINT `fk_variante_devolucion` FOREIGN KEY (`id_variante`) REFERENCES `producto_variantes` (`id_variante`);

--
-- Filtros para la tabla `gastos_operativos`
--
ALTER TABLE `gastos_operativos`
  ADD CONSTRAINT `fk_gastos_bodegas` FOREIGN KEY (`id_bodega`) REFERENCES `bodegas` (`id_bodega`),
  ADD CONSTRAINT `fk_gastos_movimiento_caja` FOREIGN KEY (`id_movimiento_caja`) REFERENCES `caja_movimiento_detalle` (`id_detalle_movimiento`),
  ADD CONSTRAINT `fk_gastos_tipos_gastos` FOREIGN KEY (`id_tipo_gasto`) REFERENCES `tipos_gastos` (`id_tipo_gasto`),
  ADD CONSTRAINT `fk_gastos_usuarios` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`);

--
-- Filtros para la tabla `historial_sesiones`
--
ALTER TABLE `historial_sesiones`
  ADD CONSTRAINT `fk_hist_sesion` FOREIGN KEY (`id_sesion`) REFERENCES `sesiones_activas` (`id_sesion`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_hist_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `historial_sesiones_ibfk_1` FOREIGN KEY (`id_sesion`) REFERENCES `sesiones_activas` (`id_sesion`) ON DELETE CASCADE,
  ADD CONSTRAINT `historial_sesiones_ibfk_2` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`) ON DELETE CASCADE;

--
-- Filtros para la tabla `inventario_bodega`
--
ALTER TABLE `inventario_bodega`
  ADD CONSTRAINT `fk_ib_bodega` FOREIGN KEY (`id_bodega`) REFERENCES `bodegas` (`id_bodega`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_ib_variante` FOREIGN KEY (`id_variante`) REFERENCES `producto_variantes` (`id_variante`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_inventario_bodega` FOREIGN KEY (`id_bodega`) REFERENCES `bodegas` (`id_bodega`),
  ADD CONSTRAINT `fk_inventario_variante` FOREIGN KEY (`id_variante`) REFERENCES `producto_variantes` (`id_variante`);

--
-- Filtros para la tabla `inventario_movimientos`
--
ALTER TABLE `inventario_movimientos`
  ADD CONSTRAINT `inventario_movimientos_ibfk_1` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`),
  ADD CONSTRAINT `inventario_movimientos_ibfk_2` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`),
  ADD CONSTRAINT `inventario_movimientos_ibfk_3` FOREIGN KEY (`id_variante`) REFERENCES `producto_variantes` (`id_variante`);

--
-- Filtros para la tabla `notas_credito`
--
ALTER TABLE `notas_credito`
  ADD CONSTRAINT `fk_nota_credito_cliente` FOREIGN KEY (`id_cliente`) REFERENCES `clientes` (`id_cliente`),
  ADD CONSTRAINT `fk_nota_credito_devolucion` FOREIGN KEY (`id_devolucion`) REFERENCES `devoluciones` (`id_devolucion`),
  ADD CONSTRAINT `fk_nota_credito_usuario` FOREIGN KEY (`id_usuario_genera`) REFERENCES `usuarios` (`id_usuario`),
  ADD CONSTRAINT `fk_nota_credito_venta_aplicada` FOREIGN KEY (`id_venta_aplicada`) REFERENCES `ventas` (`id_venta`);

--
-- Filtros para la tabla `notificaciones`
--
ALTER TABLE `notificaciones`
  ADD CONSTRAINT `fk_notificacion_usuario` FOREIGN KEY (`id_usuario_destinatario`) REFERENCES `usuarios` (`id_usuario`);

--
-- Filtros para la tabla `ordenes_reserva_detalle`
--
ALTER TABLE `ordenes_reserva_detalle`
  ADD CONSTRAINT `ordenes_reserva_detalle_ibfk_1` FOREIGN KEY (`id_orden`) REFERENCES `ordenes_reserva` (`id_orden`);

--
-- Filtros para la tabla `prestamos_zapatos`
--
ALTER TABLE `prestamos_zapatos`
  ADD CONSTRAINT `fk_prestamo_bodega` FOREIGN KEY (`id_bodega`) REFERENCES `bodegas` (`id_bodega`),
  ADD CONSTRAINT `fk_prestamo_producto` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`),
  ADD CONSTRAINT `fk_prestamo_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`),
  ADD CONSTRAINT `fk_prestamo_variante` FOREIGN KEY (`id_variante`) REFERENCES `producto_variantes` (`id_variante`);

--
-- Filtros para la tabla `privilegios_rol`
--
ALTER TABLE `privilegios_rol`
  ADD CONSTRAINT `privilegios_rol_ibfk_1` FOREIGN KEY (`id_rol`) REFERENCES `roles` (`id_rol`),
  ADD CONSTRAINT `privilegios_rol_ibfk_2` FOREIGN KEY (`id_permiso`) REFERENCES `permisos` (`id_permiso`);

--
-- Filtros para la tabla `privilegio_rol`
--
ALTER TABLE `privilegio_rol`
  ADD CONSTRAINT `fk_privilegio_permiso` FOREIGN KEY (`id_permiso`) REFERENCES `permisos` (`id_permiso`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_privilegio_rol` FOREIGN KEY (`id_rol`) REFERENCES `roles` (`id_rol`) ON DELETE CASCADE;

--
-- Filtros para la tabla `privilegio_usuario`
--
ALTER TABLE `privilegio_usuario`
  ADD CONSTRAINT `fk_privusuario_permiso` FOREIGN KEY (`id_permiso`) REFERENCES `permisos` (`id_permiso`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_privusuario_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`) ON DELETE CASCADE;

--
-- Filtros para la tabla `productos`
--
ALTER TABLE `productos`
  ADD CONSTRAINT `productos_ibfk_1` FOREIGN KEY (`id_categoria`) REFERENCES `categorias` (`id_categoria`),
  ADD CONSTRAINT `productos_ibfk_2` FOREIGN KEY (`id_marca`) REFERENCES `marcas` (`id_marca`),
  ADD CONSTRAINT `productos_ibfk_3` FOREIGN KEY (`id_proveedor`) REFERENCES `proveedores` (`id_proveedor`);

--
-- Filtros para la tabla `productos_genericos`
--
ALTER TABLE `productos_genericos`
  ADD CONSTRAINT `fk_gen_variante` FOREIGN KEY (`id_variante`) REFERENCES `producto_variantes` (`id_variante`) ON DELETE CASCADE;

--
-- Filtros para la tabla `producto_variantes`
--
ALTER TABLE `producto_variantes`
  ADD CONSTRAINT `fk_producto_variantes_proveedor` FOREIGN KEY (`id_proveedor`) REFERENCES `proveedores` (`id_proveedor`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_pv_color` FOREIGN KEY (`id_color`) REFERENCES `colores` (`id_color`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_pv_producto` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_pv_talla` FOREIGN KEY (`id_talla`) REFERENCES `tallas` (`id_talla`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_variante_color` FOREIGN KEY (`id_color`) REFERENCES `colores` (`id_color`),
  ADD CONSTRAINT `fk_variante_producto` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_variante_talla` FOREIGN KEY (`id_talla`) REFERENCES `tallas` (`id_talla`);

--
-- Filtros para la tabla `promociones_detalle`
--
ALTER TABLE `promociones_detalle`
  ADD CONSTRAINT `fk_det_promocion` FOREIGN KEY (`id_promocion`) REFERENCES `promociones` (`id_promocion`) ON DELETE CASCADE;

--
-- Filtros para la tabla `reservas`
--
ALTER TABLE `reservas`
  ADD CONSTRAINT `fk_reserva_cliente` FOREIGN KEY (`id_cliente`) REFERENCES `clientes` (`id_cliente`),
  ADD CONSTRAINT `fk_reserva_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`),
  ADD CONSTRAINT `fk_reserva_venta` FOREIGN KEY (`id_venta_generada`) REFERENCES `ventas` (`id_venta`);

--
-- Filtros para la tabla `sesiones_activas`
--
ALTER TABLE `sesiones_activas`
  ADD CONSTRAINT `fk_sesiones_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`) ON UPDATE CASCADE,
  ADD CONSTRAINT `sesiones_activas_ibfk_1` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`) ON DELETE CASCADE,
  ADD CONSTRAINT `sesiones_activas_ibfk_2` FOREIGN KEY (`id_caja_abierta`) REFERENCES `cajas` (`id_caja`) ON DELETE SET NULL,
  ADD CONSTRAINT `sesiones_activas_ibfk_3` FOREIGN KEY (`id_movimiento_caja`) REFERENCES `caja_movimientos` (`id_movimiento`) ON DELETE SET NULL;

--
-- Filtros para la tabla `traspasos`
--
ALTER TABLE `traspasos`
  ADD CONSTRAINT `fk_traspaso_bodega_destino` FOREIGN KEY (`id_bodega_destino`) REFERENCES `bodegas` (`id_bodega`),
  ADD CONSTRAINT `fk_traspaso_bodega_origen` FOREIGN KEY (`id_bodega_origen`) REFERENCES `bodegas` (`id_bodega`),
  ADD CONSTRAINT `fk_traspaso_usuario_autoriza` FOREIGN KEY (`id_usuario_autoriza`) REFERENCES `usuarios` (`id_usuario`),
  ADD CONSTRAINT `fk_traspaso_usuario_recibe` FOREIGN KEY (`id_usuario_recibe`) REFERENCES `usuarios` (`id_usuario`),
  ADD CONSTRAINT `fk_traspaso_usuario_solicita` FOREIGN KEY (`id_usuario_solicita`) REFERENCES `usuarios` (`id_usuario`);

--
-- Filtros para la tabla `traspaso_auditoria`
--
ALTER TABLE `traspaso_auditoria`
  ADD CONSTRAINT `fk_ta_traspaso` FOREIGN KEY (`id_traspaso`) REFERENCES `traspasos` (`id_traspaso`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_ta_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Filtros para la tabla `traspaso_detalles`
--
ALTER TABLE `traspaso_detalles`
  ADD CONSTRAINT `fk_detalle_producto_traspaso` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`),
  ADD CONSTRAINT `fk_detalle_traspaso` FOREIGN KEY (`id_traspaso`) REFERENCES `traspasos` (`id_traspaso`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_detalle_variante_traspaso` FOREIGN KEY (`id_variante`) REFERENCES `producto_variantes` (`id_variante`);

--
-- Filtros para la tabla `usuarios`
--
ALTER TABLE `usuarios`
  ADD CONSTRAINT `fk_usuario_bodega` FOREIGN KEY (`id_bodega`) REFERENCES `bodegas` (`id_bodega`),
  ADD CONSTRAINT `usuarios_ibfk_1` FOREIGN KEY (`id_rol`) REFERENCES `roles` (`id_rol`);

--
-- Filtros para la tabla `ventas`
--
ALTER TABLE `ventas`
  ADD CONSTRAINT `ventas_ibfk_1` FOREIGN KEY (`id_cliente`) REFERENCES `clientes` (`id_cliente`),
  ADD CONSTRAINT `ventas_ibfk_2` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`id_usuario`),
  ADD CONSTRAINT `ventas_ibfk_3` FOREIGN KEY (`id_caja`) REFERENCES `cajas` (`id_caja`),
  ADD CONSTRAINT `ventas_ibfk_4` FOREIGN KEY (`id_movimiento`) REFERENCES `caja_movimientos` (`id_movimiento`);

--
-- Filtros para la tabla `venta_detalles`
--
ALTER TABLE `venta_detalles`
  ADD CONSTRAINT `venta_detalles_ibfk_1` FOREIGN KEY (`id_venta`) REFERENCES `ventas` (`id_venta`),
  ADD CONSTRAINT `venta_detalles_ibfk_2` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`),
  ADD CONSTRAINT `venta_detalles_ibfk_3` FOREIGN KEY (`id_variante`) REFERENCES `producto_variantes` (`id_variante`);

--
-- Filtros para la tabla `venta_medios_pago`
--
ALTER TABLE `venta_medios_pago`
  ADD CONSTRAINT `fk_medio_pago_venta` FOREIGN KEY (`id_venta`) REFERENCES `ventas` (`id_venta`) ON UPDATE CASCADE;

--
-- Filtros para la tabla `venta_pagos`
--
ALTER TABLE `venta_pagos`
  ADD CONSTRAINT `fk_pago_venta` FOREIGN KEY (`id_venta`) REFERENCES `ventas` (`id_venta`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
