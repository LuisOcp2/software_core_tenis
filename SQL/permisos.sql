-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Servidor: localhost
-- Tiempo de generación: 26-01-2026 a las 16:10:26
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

--
-- Volcado de datos para la tabla `permisos`
--

INSERT INTO `permisos` (`id_permiso`, `modulo`, `nombre_mostrar`, `descripcion`, `categoria`, `requiere_admin`, `activo`) VALUES
(1, 'generar venta', 'Generar Ventas', NULL, 'comercial', 0, 1),
(2, 'clientes', 'Gestión de Clientes', NULL, 'comercial', 0, 1),
(3, 'cuentas_por_cobrar', 'Cuentas por Cobrar', NULL, 'comercial', 0, 1),
(4, 'devoluciones', 'Gestión de Devoluciones', NULL, 'comercial', 0, 1),
(5, 'gestion de productos', 'Gestión de Productos', NULL, 'inventario', 0, 1),
(6, 'inventario', 'Gestión de Inventario', NULL, 'inventario', 0, 1),
(7, 'traspasos', 'Traspasos entre Bodegas', NULL, 'inventario', 0, 1),
(8, 'movimientos', 'Movimientos de Inventario', NULL, 'caja', 0, 1),
(9, 'compras', 'Gestión de Compras', NULL, 'compras', 0, 1),
(10, 'proveedores', 'Gestión de Proveedores', NULL, 'compras', 0, 1),
(11, 'reportes_ventas', 'Reportes de Ventas', NULL, 'reportes', 0, 1),
(12, 'reportes_inventario', 'Reportes de Inventario', NULL, 'reportes', 0, 1),
(13, 'rotulacion', 'Rotulación', NULL, 'otros', 0, 1),
(14, 'configuracion', 'Configuración del Sistema', NULL, 'admin', 0, 1),
(15, 'usuarios', 'Gestión de Usuarios', NULL, 'admin', 0, 1),
(31, 'pedidos_web', 'Pedidos Web', 'Gestión de Pedidos desde la Web', 'comercial', 0, 1),
(32, 'promociones', 'Promociones', 'Gestión de Promociones y Descuentos', 'comercial', 0, 1),
(33, 'marcas', 'Gestión de Marcas', 'Gestión de Marcas de Productos', 'inventario', 0, 1),
(34, 'categorias', 'Gestión de Categorías', 'Gestión de Categorías de Productos', 'inventario', 0, 1),
(35, 'bodegas', 'Gestión de Bodegas', 'Gestión de Bodegas del Sistema', 'admin', 0, 1),
(36, 'cajas', 'Gestión de Cajas', 'Gestión de Cajas del Sistema', 'admin', 0, 1),
(37, 'tipos_gasto', 'Tipos de Gasto', 'Gestión de Tipos de Gasto', 'admin', 0, 1),
(38, 'reportes', 'Acceso a Reportes', 'Acceso a Sección de Reportes', 'reportes', 0, 1),
(39, 'colores', 'Gestión de Colores', 'Administrar colores de productos', 'Inventario', 0, 1),
(40, 'prestamos', 'Gestión de Préstamos', 'Administrar préstamos de productos', 'Inventario', 0, 1),
(41, 'ver_productos', 'Ver Productos', 'Visualizar catálogo de productos', 'Inventario', 0, 1),
(42, 'reporte_inventario', 'Reporte de Inventario', 'Generar reportes de inventario', 'Reportes', 0, 1),
(43, 'reporte_compras', 'Reporte de Compras', 'Generar reportes de compras', 'Reportes', 0, 1),
(44, 'reporte_gastos', 'Reporte de Gastos', 'Generar reportes de gastos', 'Reportes', 0, 1),
(45, 'reporte_devoluciones', 'Reporte de Devoluciones', 'Generar reportes de devoluciones', 'Reportes', 0, 1),
(46, 'reporte_traspasos', 'Reporte de Traspasos', 'Generar reportes de traspasos entre bodegas', 'Reportes', 0, 1),
(47, 'reporte_clientes', 'Reporte de Clientes', 'Generar reportes de clientes', 'Reportes', 0, 1),
(48, 'reporte_auditoria', 'Reporte de Auditoría', 'Generar reportes de auditoría del sistema', 'Reportes', 1, 1),
(49, 'cambio_talla', 'Cambio de Talla', 'Realizar cambios de talla en ventas', 'Comercial', 0, 1),
(50, 'monitor_cajas', 'Monitor de Cajas', 'Monitorear estado de todas las cajas', 'Admin', 1, 1),
(51, 'movimientos_caja', 'Movimientos de Caja', 'Ver y gestionar movimientos de caja', 'Admin', 0, 1),
(52, 'notificaciones', 'Notificaciones', 'Ver notificaciones del sistema', 'Admin', 0, 1),
(53, 'generar_venta', 'Generar Ventas', 'Crear y procesar ventas', 'Comercial', 0, 1),
(62, 'gestion_de_productos', 'Gestión de Productos', 'Crear y editar productos', 'Inventario', 0, 1),
(161, 'ver_traspasos', 'Ver Traspasos', 'Ver y consultar traspasos de la bodega asignada', 'Traspasos', 0, 1),
(162, 'crear_traspaso', 'Crear Traspaso', 'Crear nuevos traspasos entre bodegas', 'Traspasos', 0, 1),
(163, 'autorizar_traspaso', 'Autorizar Traspaso', 'Autorizar traspasos de la bodega origen', 'Traspasos', 0, 1),
(164, 'enviar_traspaso', 'Enviar Traspaso', 'Marcar traspasos como enviados desde bodega origen', 'Traspasos', 0, 1),
(165, 'recibir_traspaso', 'Recibir Traspaso', 'Confirmar recepción en bodega destino', 'Traspasos', 0, 1),
(166, 'cancelar_traspaso', 'Cancelar Traspaso', 'Cancelar traspasos en estados permitidos', 'Traspasos', 0, 1),
(167, 'editar_traspaso', 'Editar Traspaso', 'Modificar traspasos pendientes', 'Traspasos', 0, 1),
(168, 'admin_traspasos', 'Administrar Traspasos', 'Control total sobre todos los traspasos', 'Traspasos Admin', 1, 1),
(169, 'ver_todos_traspasos', 'Ver Todos Traspasos', 'Ver traspasos de todas las bodegas', 'Traspasos Admin', 1, 1);

--
-- Índices para tablas volcadas
--

--
-- Indices de la tabla `permisos`
--
ALTER TABLE `permisos`
  ADD PRIMARY KEY (`id_permiso`),
  ADD UNIQUE KEY `modulo` (`modulo`);

--
-- AUTO_INCREMENT de las tablas volcadas
--

--
-- AUTO_INCREMENT de la tabla `permisos`
--
ALTER TABLE `permisos`
  MODIFY `id_permiso` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=188;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
