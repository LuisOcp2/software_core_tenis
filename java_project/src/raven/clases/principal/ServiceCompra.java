package raven.clases.principal;

import raven.controlador.principal.conexion;
import raven.controlador.principal.ModelCompra;
import raven.controlador.principal.ModelCompra.EstadoCompra;
import raven.controlador.principal.ModelCompraDetalle;
import raven.controlador.principal.ModelCompraDetalle.TipoUnidad;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para gestión de compras a proveedores.
 * 
 * Responsabilidades:
 * - CRUD de compras y detalles
 * - Actualización de inventario
 * - Registro de movimientos de inventario
 * - Auditoría de compras
 * 
 * @author CrisDEV
 * @version 1.0
 */
public class ServiceCompra {

    // ═══════════════════════════════════════════════════════════════════════════
    // SQL COMPRAS
    // ═══════════════════════════════════════════════════════════════════════════

    private static final String SQL_INSERT_COMPRA = """
            INSERT INTO compras
            (numero_compra, id_proveedor, id_usuario, id_bodega, fecha_compra,
             numero_factura, subtotal, iva, total, estado, observaciones,
             total_abonado, saldo_pendiente, estado_pago)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SQL_INSERT_DETALLE = """
            INSERT INTO compra_detalles
            (id_compra, id_producto, id_variante, cantidad, tipo_unidad,
             precio_unitario, subtotal, observaciones)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SQL_SELECT_COMPRA = """
            SELECT c.*,
                   p.nombre AS nombre_proveedor, p.ruc AS ruc_proveedor,
                   b.nombre AS nombre_bodega,
                   u.nombre AS nombre_usuario
            FROM compras c
            LEFT JOIN proveedores p ON c.id_proveedor = p.id_proveedor
            LEFT JOIN bodegas b ON c.id_bodega = b.id_bodega
            LEFT JOIN usuarios u ON c.id_usuario = u.id_usuario
            WHERE c.id_compra = ?
            """;

    private static final String SQL_SELECT_DETALLES = """
            SELECT cd.*,
                   prod.codigo_modelo, prod.nombre AS nombre_producto,
                   t.numero AS talla, col.nombre AS color,
                   pv.sku, pv.ean, pv.precio_venta
            FROM compra_detalles cd
            JOIN productos prod ON cd.id_producto = prod.id_producto
            JOIN producto_variantes pv ON cd.id_variante = pv.id_variante
            LEFT JOIN tallas t ON pv.id_talla = t.id_talla
            LEFT JOIN colores col ON pv.id_color = col.id_color
            WHERE cd.id_compra = ?
            ORDER BY cd.id_detalle_compra
            """;

    private static final String SQL_LISTAR_COMPRAS = """
            SELECT c.*,
                   p.nombre AS nombre_proveedor, p.ruc AS ruc_proveedor,
                   b.nombre AS nombre_bodega,
                   u.nombre AS nombre_usuario,
                   (SELECT COUNT(*) FROM compra_detalles cd WHERE cd.id_compra = c.id_compra) AS total_items,
                   (SELECT SUM(cd.cantidad) FROM compra_detalles cd WHERE cd.id_compra = c.id_compra) AS total_unidades
            FROM compras c
            LEFT JOIN proveedores p ON c.id_proveedor = p.id_proveedor
            LEFT JOIN bodegas b ON c.id_bodega = b.id_bodega
            LEFT JOIN usuarios u ON c.id_usuario = u.id_usuario
            WHERE 1=1
            """;

    private static final String SQL_SIGUIENTE_NUMERO = """
            SELECT COALESCE(MAX(CAST(SUBSTRING(numero_compra, 13) AS UNSIGNED)), 0) + 1 AS siguiente
            FROM compras
            WHERE numero_compra LIKE CONCAT('CP-', DATE_FORMAT(NOW(), '%Y%m%d'), '-%')
            """;

    private static final String SQL_UPDATE_INVENTARIO_PAR = """
            INSERT INTO inventario_bodega (id_bodega, id_variante, Stock_par, Stock_caja, fecha_ultimo_movimiento, activo)
            VALUES (?, ?, ?, 0, NOW(), 1)
            ON DUPLICATE KEY UPDATE
            Stock_par = Stock_par + VALUES(Stock_par),
            fecha_ultimo_movimiento = NOW(),
            activo = 1
            """;

    private static final String SQL_UPDATE_INVENTARIO_CAJA = """
            INSERT INTO inventario_bodega (id_bodega, id_variante, Stock_par, Stock_caja, fecha_ultimo_movimiento, activo)
            VALUES (?, ?, 0, ?, NOW(), 1)
            ON DUPLICATE KEY UPDATE
            Stock_caja = Stock_caja + VALUES(Stock_caja),
            fecha_ultimo_movimiento = NOW(),
            activo = 1
            """;

    private static final String SQL_INSERT_MOVIMIENTO_INVENTARIO = """
            INSERT INTO inventario_movimientos
            (id_producto, id_variante, tipo_movimiento, cantidad, cantidad_pares,
             fecha_movimiento, id_referencia, tipo_referencia, id_usuario, observaciones)
            VALUES (?, ?, ?, ?, ?, NOW(), ?, 'compra', ?, ?)
            """;

    private static final String SQL_ANULAR_COMPRA = """
            UPDATE compras SET estado = 'cancelada', fecha_actualizacion = NOW()
            WHERE id_compra = ?
            """;

    private static final String SQL_REVERTIR_INVENTARIO_PAR = """
            UPDATE inventario_bodega
            SET Stock_par = Stock_par - ?, fecha_ultimo_movimiento = NOW()
            WHERE id_bodega = ? AND id_variante = ?
            """;

    private static final String SQL_REVERTIR_INVENTARIO_CAJA = """
            UPDATE inventario_bodega
            SET Stock_caja = Stock_caja - ?, fecha_ultimo_movimiento = NOW()
            WHERE id_bodega = ? AND id_variante = ?
            """;

    // ═══════════════════════════════════════════════════════════════════════════
    // SQL BÚSQUEDA DE PRODUCTOS
    // ═══════════════════════════════════════════════════════════════════════════

    private static final String SQL_BUSCAR_PRODUCTOS = """
            SELECT DISTINCT
                p.id_producto, p.codigo_modelo, p.nombre, p.precio_compra, p.precio_venta,
                m.nombre AS marca, c.nombre AS categoria
            FROM productos p
            LEFT JOIN marcas m ON p.id_marca = m.id_marca
            LEFT JOIN categorias c ON p.id_categoria = c.id_categoria
            JOIN producto_variantes pv ON p.id_producto = pv.id_producto
            WHERE p.activo = 1
              AND pv.disponible = 1
              AND (
                  LOWER(p.nombre) LIKE LOWER(?) OR
                  LOWER(p.codigo_modelo) LIKE LOWER(?) OR
                  LOWER(pv.ean) LIKE LOWER(?) OR
                  LOWER(pv.sku) LIKE LOWER(?)
              )
            ORDER BY p.nombre
            LIMIT 50
            """;

    private static final String SQL_OBTENER_VARIANTES = """
            SELECT pv.id_variante, pv.id_producto, pv.sku, pv.ean,
                   pv.precio_compra, pv.precio_venta,
                   t.id_talla, t.numero AS talla,
                   col.id_color, col.nombre AS color,
                   COALESCE(ib.Stock_par, 0) AS stock_pares,
                   COALESCE(ib.Stock_caja, 0) AS stock_cajas,
                   pv.id_proveedor AS id_proveedor,
                   prov.nombre AS nombre_proveedor
            FROM producto_variantes pv
            LEFT JOIN tallas t ON pv.id_talla = t.id_talla
            LEFT JOIN colores col ON pv.id_color = col.id_color
            LEFT JOIN inventario_bodega ib ON pv.id_variante = ib.id_variante AND ib.id_bodega = ?
            LEFT JOIN proveedores prov ON pv.id_proveedor = prov.id_proveedor
            WHERE pv.id_producto = ? AND pv.disponible = 1
            ORDER BY t.numero, col.nombre
            """;

    private static final String SQL_OBTENER_VARIANTES_POR_PROVEEDOR = """
            SELECT pv.id_variante, pv.id_producto, pv.sku, pv.ean,
                   pv.precio_compra, pv.precio_venta,
                   t.id_talla, t.numero AS talla,
                   col.id_color, col.nombre AS color,
                   COALESCE(ib.Stock_par, 0) AS stock_pares,
                   COALESCE(ib.Stock_caja, 0) AS stock_cajas,
                   pv.id_proveedor AS id_proveedor,
                   prov.nombre AS nombre_proveedor
            FROM producto_variantes pv
            LEFT JOIN tallas t ON pv.id_talla = t.id_talla
            LEFT JOIN colores col ON pv.id_color = col.id_color
            LEFT JOIN inventario_bodega ib ON pv.id_variante = ib.id_variante AND ib.id_bodega = ?
            LEFT JOIN proveedores prov ON pv.id_proveedor = prov.id_proveedor
            WHERE pv.id_producto = ? AND pv.disponible = 1 AND pv.id_proveedor = ?
            ORDER BY t.numero, col.nombre
            """;

    // Variantes creadas específicamente en la bodega seleccionada (incluye stock 0)
    private static final String SQL_OBTENER_VARIANTES_EN_BODEGA = """
            SELECT pv.id_variante, pv.id_producto, pv.sku, pv.ean,
                   pv.precio_compra, pv.precio_venta,
                   t.id_talla, t.numero AS talla,
                   col.id_color, col.nombre AS color,
                   COALESCE(ib.Stock_par, 0) AS stock_pares,
                   COALESCE(ib.Stock_caja, 0) AS stock_cajas,
                   pv.id_proveedor AS id_proveedor,
                   prov.nombre AS nombre_proveedor
            FROM producto_variantes pv
            INNER JOIN tallas t ON pv.id_talla = t.id_talla
            INNER JOIN colores col ON pv.id_color = col.id_color
            INNER JOIN inventario_bodega ib ON pv.id_variante = ib.id_variante
            LEFT JOIN proveedores prov ON pv.id_proveedor = prov.id_proveedor
            WHERE ib.id_bodega = ? AND pv.id_producto = ? AND pv.disponible = 1
            ORDER BY t.numero, col.nombre
            """;

    private static final String SQL_BUSCAR_POR_EAN = """
            SELECT pv.id_variante, pv.id_producto, pv.sku, pv.ean,
                   pv.precio_compra, pv.precio_venta,
                   p.codigo_modelo, p.nombre AS nombre_producto,
                   t.numero AS talla, col.nombre AS color,
                   COALESCE(ib.Stock_par, 0) AS stock_pares,
                   COALESCE(ib.Stock_caja, 0) AS stock_cajas,
                   pv.id_proveedor AS id_proveedor,
                   prov.nombre AS nombre_proveedor
            FROM producto_variantes pv
            JOIN productos p ON pv.id_producto = p.id_producto
            LEFT JOIN tallas t ON pv.id_talla = t.id_talla
            LEFT JOIN colores col ON pv.id_color = col.id_color
            LEFT JOIN inventario_bodega ib ON pv.id_variante = ib.id_variante AND ib.id_bodega = ?
            LEFT JOIN proveedores prov ON pv.id_proveedor = prov.id_proveedor
            WHERE pv.ean = ? AND pv.disponible = 1 AND p.activo = 1
            """;

    private static final String SQL_UPDATE_COMPRA = """
            UPDATE compras
            SET id_proveedor = ?, id_usuario = ?, id_bodega = ?, fecha_compra = ?,
                numero_factura = ?, subtotal = ?, iva = ?, total = ?, observaciones = ?,
                saldo_pendiente = total - total_abonado, fecha_actualizacion = NOW()
            WHERE id_compra = ?
            """;

    private static final String SQL_DELETE_DETALLES = "DELETE FROM compra_detalles WHERE id_compra = ?";

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTODOS PRINCIPALES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Actualiza una compra existente:
     * 1. Revertir inventario actual
     * 2. Borrar detalles anteriores
     * 3. Actualizar cabecera
     * 4. Insertar nuevos detalles y actualizar inventario
     */
    public void actualizarCompra(ModelCompra compra) throws SQLException {
        Connection conn = null;

        try {
            conn = conexion.getConnectionStatic();
            conn.setAutoCommit(false);

            // 1. Validar compra
            compra.validarParaGuardar();
            
            if (compra.getIdCompra() == null || compra.getIdCompra() <= 0) {
                throw new SQLException("ID de compra no válido para actualización");
            }

            // 2. Obtener estado anterior para revertir inventario
            Optional<ModelCompra> optOld = obtenerCompra(compra.getIdCompra());
            if (optOld.isEmpty()) {
                throw new SQLException("La compra a actualizar no existe");
            }
            ModelCompra oldCompra = optOld.get();

            // 3. Revertir inventario de detalles anteriores
            // Usamos el usuario actual (de la nueva compra) para el registro del movimiento de reversión
            for (ModelCompraDetalle detalle : oldCompra.getDetalles()) {
                revertirInventario(conn, oldCompra.getIdBodega(), detalle, compra.getIdUsuario());
            }

            // 4. Borrar detalles anteriores
            try (PreparedStatement ps = conn.prepareStatement(SQL_DELETE_DETALLES)) {
                ps.setInt(1, compra.getIdCompra());
                ps.executeUpdate();
            }

            // 5. Actualizar cabecera
            try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_COMPRA)) {
                ps.setInt(1, compra.getIdProveedor());
                ps.setInt(2, compra.getIdUsuario());
                ps.setInt(3, compra.getIdBodega());
                ps.setDate(4, Date.valueOf(compra.getFechaCompra()));
                ps.setString(5, compra.getNumeroFactura());
                ps.setBigDecimal(6, compra.getSubtotal());
                ps.setBigDecimal(7, compra.getIva());
                ps.setBigDecimal(8, compra.getTotal());
                ps.setString(9, compra.getObservaciones());
                ps.setInt(10, compra.getIdCompra());
                
                ps.executeUpdate();
            }

            // 6. Procesar nuevos detalles (Insertar + Sumar Inventario)
            for (ModelCompraDetalle detalle : compra.getDetalles()) {
                procesarDetalle(conn, compra, detalle);
            }

            conn.commit();
            System.out.println("SUCCESS  Compra actualizada: " + compra.getNumeroCompra());

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("WARNING  Rollback en actualización ejecutado");
                } catch (SQLException ex) {
                    System.err.println("Error en rollback: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    /* ignore */
                }
            }
        }
    }

    /**
     * Registra una compra completa con actualización de inventario.
     * 
     * @param compra ModelCompra con los datos de la compra
     * @return ID de la compra registrada
     */
    public int registrarCompra(ModelCompra compra) throws SQLException {
        Connection conn = null;

        try {
            conn = conexion.getConnectionStatic();
            conn.setAutoCommit(false);

            // 1. Validar compra
            compra.validarParaGuardar();

            // 2. Generar número de compra
            String numeroCompra = generarNumeroCompra(conn);
            compra.setNumeroCompra(numeroCompra);

            // 3. Insertar cabecera
            int idCompra = insertarCabecera(conn, compra);
            compra.setIdCompra(idCompra);

            // 4. Procesar cada detalle
            for (ModelCompraDetalle detalle : compra.getDetalles()) {
                procesarDetalle(conn, compra, detalle);
            }

            // 5. Marcar como recibida
            marcarRecibida(conn, idCompra);

            conn.commit();

            System.out.println("SUCCESS  Compra a proveedor registrada: " + numeroCompra + " - $" + compra.getTotal());

            return idCompra;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("WARNING  Rollback ejecutado");
                } catch (SQLException ex) {
                    System.err.println("Error en rollback: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    /* ignore */
                }
            }
        }
    }

    /**
     * Obtiene una compra con sus detalles.
     */
    public Optional<ModelCompra> obtenerCompra(int idCompra) throws SQLException {
        try (Connection conn = conexion.getConnectionStatic()) {

            ModelCompra compra = null;
            try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_COMPRA)) {
                ps.setInt(1, idCompra);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        compra = mapearCompra(rs);
                    }
                }
            }

            if (compra == null) {
                return Optional.empty();
            }

            // Cargar detalles
            List<ModelCompraDetalle> detalles = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_DETALLES)) {
                ps.setInt(1, idCompra);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        detalles.add(mapearDetalle(rs));
                    }
                }
            }
            compra.setDetalles(detalles);

            return Optional.of(compra);
        }
    }

    /**
     * Lista compras con filtros opcionales.
     */
    public List<ModelCompra> listarCompras(LocalDate fechaInicio, LocalDate fechaFin,
            Integer idProveedor, Integer idBodega, String estado) throws SQLException {

        StringBuilder sql = new StringBuilder(SQL_LISTAR_COMPRAS);
        List<Object> params = new ArrayList<>();

        if (fechaInicio != null && fechaFin != null) {
            sql.append(" AND c.fecha_compra BETWEEN ? AND ?");
            params.add(Date.valueOf(fechaInicio));
            params.add(Date.valueOf(fechaFin));
        }

        if (idProveedor != null && idProveedor > 0) {
            sql.append(" AND c.id_proveedor = ?");
            params.add(idProveedor);
        }

        if (idBodega != null && idBodega > 0) {
            sql.append(" AND c.id_bodega = ?");
            params.add(idBodega);
        }

        if (estado != null && !estado.isEmpty()) {
            sql.append(" AND c.estado = ?");
            params.add(estado);
        }

        sql.append(" ORDER BY c.fecha_compra DESC, c.id_compra DESC");

        List<ModelCompra> compras = new ArrayList<>();

        try (Connection conn = conexion.getConnectionStatic();
                PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    compras.add(mapearCompraResumen(rs));
                }
            }
        }

        return compras;
    }

    /**
     * Anula una compra y revierte el inventario.
     */
    public boolean anularCompra(int idCompra, int idUsuario) throws SQLException {
        Connection conn = null;

        try {
            conn = conexion.getConnectionStatic();
            conn.setAutoCommit(false);

            // 1. Obtener compra con detalles
            Optional<ModelCompra> optCompra = obtenerCompra(idCompra);
            if (optCompra.isEmpty()) {
                throw new SQLException("Compra no encontrada");
            }

            ModelCompra compra = optCompra.get();
            if (!compra.puedeAnularse()) {
                throw new SQLException("La compra ya está anulada");
            }

            // 2. Revertir inventario por cada detalle
            for (ModelCompraDetalle detalle : compra.getDetalles()) {
                revertirInventario(conn, compra.getIdBodega(), detalle, idUsuario);
            }

            // 3. Marcar compra como cancelada
            try (PreparedStatement ps = conn.prepareStatement(SQL_ANULAR_COMPRA)) {
                ps.setInt(1, idCompra);
                ps.executeUpdate();
            }

            conn.commit();
            System.out.println("SUCCESS  Compra anulada: " + compra.getNumeroCompra());
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    /* ignore */
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    /* ignore */
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTODOS DE BÚSQUEDA DE PRODUCTOS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Busca productos por término (nombre, código, EAN, SKU).
     */
    public List<ProductoBusqueda> buscarProductos(String termino) throws SQLException {
        List<ProductoBusqueda> productos = new ArrayList<>();
        String patron = "%" + termino.toLowerCase() + "%";

        try (Connection conn = conexion.getConnectionStatic();
                PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR_PRODUCTOS)) {

            ps.setString(1, patron);
            ps.setString(2, patron);
            ps.setString(3, patron);
            ps.setString(4, patron);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProductoBusqueda p = new ProductoBusqueda();
                    p.idProducto = rs.getInt("id_producto");
                    p.codigoModelo = rs.getString("codigo_modelo");
                    p.nombre = rs.getString("nombre");
                    p.precioCompra = rs.getBigDecimal("precio_compra");
                    p.precioVenta = rs.getBigDecimal("precio_venta");
                    p.marca = rs.getString("marca");
                    p.categoria = rs.getString("categoria");
                    productos.add(p);
                }
            }
        }

        return productos;
    }

    /**
     * Obtiene las variantes de un producto con stock.
     */
    public List<VarianteBusqueda> obtenerVariantesProducto(int idProducto, int idBodega) throws SQLException {
        List<VarianteBusqueda> variantes = new ArrayList<>();

        try (Connection conn = conexion.getConnectionStatic();
                PreparedStatement ps = conn.prepareStatement(SQL_OBTENER_VARIANTES)) {

            ps.setInt(1, idBodega);
            ps.setInt(2, idProducto);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    VarianteBusqueda v = new VarianteBusqueda();
                    v.idVariante = rs.getInt("id_variante");
                    v.idProducto = rs.getInt("id_producto");
                    v.sku = rs.getString("sku");
                    v.ean = rs.getString("ean");
                    v.idTalla = rs.getInt("id_talla");
                    v.talla = rs.getString("talla");
                    v.idColor = rs.getInt("id_color");
                    v.color = rs.getString("color");
                    v.precioCompra = rs.getBigDecimal("precio_compra");
                    v.precioVenta = rs.getBigDecimal("precio_venta");
                    v.stockPares = rs.getInt("stock_pares");
                    v.stockCajas = rs.getInt("stock_cajas");
                    v.idProveedor = rs.getInt("id_proveedor");
                    v.nombreProveedor = rs.getString("nombre_proveedor");
                    variantes.add(v);
                }
            }
        }

        return variantes;
    }

    public List<VarianteBusqueda> obtenerVariantesProducto(int idProducto, int idBodega, int idProveedor) throws SQLException {
        if (idProveedor <= 0) {
            return obtenerVariantesProducto(idProducto, idBodega);
        }
        List<VarianteBusqueda> variantes = new ArrayList<>();

        try (Connection conn = conexion.getConnectionStatic();
                PreparedStatement ps = conn.prepareStatement(SQL_OBTENER_VARIANTES_POR_PROVEEDOR)) {

            ps.setInt(1, idBodega);
            ps.setInt(2, idProducto);
            ps.setInt(3, idProveedor);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    VarianteBusqueda v = new VarianteBusqueda();
                    v.idVariante = rs.getInt("id_variante");
                    v.idProducto = rs.getInt("id_producto");
                    v.sku = rs.getString("sku");
                    v.ean = rs.getString("ean");
                    v.idTalla = rs.getInt("id_talla");
                    v.talla = rs.getString("talla");
                    v.idColor = rs.getInt("id_color");
                    v.color = rs.getString("color");
                    v.precioCompra = rs.getBigDecimal("precio_compra");
                    v.precioVenta = rs.getBigDecimal("precio_venta");
                    v.stockPares = rs.getInt("stock_pares");
                    v.stockCajas = rs.getInt("stock_cajas");
                    v.idProveedor = rs.getInt("id_proveedor");
                    v.nombreProveedor = rs.getString("nombre_proveedor");
                    variantes.add(v);
                }
            }
        }

        return variantes;
    }

    /**
     * Obtiene las variantes de un producto que existen en la bodega indicada.
     * Solo muestra variantes que tienen registro en inventario_bodega para esa bodega,
     * incluso si su stock es 0.
     */
    public List<VarianteBusqueda> obtenerVariantesProductoEnBodega(int idProducto, int idBodega) throws SQLException {
        List<VarianteBusqueda> variantes = new ArrayList<>();

        try (Connection conn = conexion.getConnectionStatic();
                PreparedStatement ps = conn.prepareStatement(SQL_OBTENER_VARIANTES_EN_BODEGA)) {

            ps.setInt(1, idBodega);
            ps.setInt(2, idProducto);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    VarianteBusqueda v = new VarianteBusqueda();
                    v.idVariante = rs.getInt("id_variante");
                    v.idProducto = rs.getInt("id_producto");
                    v.sku = rs.getString("sku");
                    v.ean = rs.getString("ean");
                    v.idTalla = rs.getInt("id_talla");
                    v.talla = rs.getString("talla");
                    v.idColor = rs.getInt("id_color");
                    v.color = rs.getString("color");
                    v.precioCompra = rs.getBigDecimal("precio_compra");
                    v.precioVenta = rs.getBigDecimal("precio_venta");
                    v.stockPares = rs.getInt("stock_pares");
                    v.stockCajas = rs.getInt("stock_cajas");
                    v.idProveedor = rs.getInt("id_proveedor");
                    v.nombreProveedor = rs.getString("nombre_proveedor");
                    variantes.add(v);
                }
            }
        }

        return variantes;
    }

    /**
     * Busca una variante por código EAN.
     */
    public Optional<VarianteBusqueda> buscarPorEan(String ean, int idBodega) throws SQLException {
        try (Connection conn = conexion.getConnectionStatic();
                PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR_POR_EAN)) {

            ps.setInt(1, idBodega);
            ps.setString(2, ean);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    VarianteBusqueda v = new VarianteBusqueda();
                    v.idVariante = rs.getInt("id_variante");
                    v.idProducto = rs.getInt("id_producto");
                    v.sku = rs.getString("sku");
                    v.ean = rs.getString("ean");
                    v.talla = rs.getString("talla");
                    v.color = rs.getString("color");
                    v.precioCompra = rs.getBigDecimal("precio_compra");
                    v.precioVenta = rs.getBigDecimal("precio_venta");
                    v.codigoModelo = rs.getString("codigo_modelo");
                    v.nombreProducto = rs.getString("nombre_producto");
                    v.stockPares = rs.getInt("stock_pares");
                    v.stockCajas = rs.getInt("stock_cajas");
                    return Optional.of(v);
                }
            }
        }

        return Optional.empty();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTODOS PRIVADOS
    // ═══════════════════════════════════════════════════════════════════════════

    private String generarNumeroCompra(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SIGUIENTE_NUMERO);
                ResultSet rs = ps.executeQuery()) {

            int siguiente = 1;
            if (rs.next()) {
                siguiente = rs.getInt("siguiente");
            }

            return ModelCompra.generarNumeroCompra(siguiente);
        }
    }

    private int insertarCabecera(Connection conn, ModelCompra compra) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_COMPRA, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, compra.getNumeroCompra());
            ps.setInt(2, compra.getIdProveedor());
            ps.setInt(3, compra.getIdUsuario());
            ps.setInt(4, compra.getIdBodega());
            ps.setDate(5, Date.valueOf(compra.getFechaCompra()));
            ps.setString(6, compra.getNumeroFactura());
            ps.setBigDecimal(7, compra.getSubtotal());
            ps.setBigDecimal(8, compra.getIva());
            ps.setBigDecimal(9, compra.getTotal());
            ps.setString(10, EstadoCompra.PENDIENTE.getValor());
            ps.setString(11, compra.getObservaciones());
            
            // Campos de saldo
            ps.setBigDecimal(12, BigDecimal.ZERO); // total_abonado
            ps.setBigDecimal(13, compra.getTotal()); // saldo_pendiente = total
            ps.setString(14, "pendiente"); // estado_pago

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("No se pudo obtener ID de compra insertada");
    }

    private void procesarDetalle(Connection conn, ModelCompra compra,
            ModelCompraDetalle detalle) throws SQLException {

        detalle.setIdCompra(compra.getIdCompra());

        // Insertar detalle
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_DETALLE, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, detalle.getIdCompra());
            ps.setInt(2, detalle.getIdProducto());
            ps.setInt(3, detalle.getIdVariante());
            ps.setInt(4, detalle.getCantidad());
            ps.setString(5, detalle.getTipoUnidad().getValor());
            ps.setBigDecimal(6, detalle.getPrecioUnitario());
            ps.setBigDecimal(7, detalle.getSubtotal());
            ps.setString(8, detalle.getObservaciones());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    detalle.setIdDetalleCompra(rs.getInt(1));
                }
            }
        }

        // Actualizar inventario
        actualizarInventario(conn, compra.getIdBodega(), detalle, compra.getIdUsuario());
    }

    private void actualizarInventario(Connection conn, int idBodega,
            ModelCompraDetalle detalle, int idUsuario) throws SQLException {

        String sqlInventario = (detalle.getTipoUnidad() == TipoUnidad.CAJA)
                ? SQL_UPDATE_INVENTARIO_CAJA
                : SQL_UPDATE_INVENTARIO_PAR;

        try (PreparedStatement ps = conn.prepareStatement(sqlInventario)) {
            ps.setInt(1, idBodega);
            ps.setInt(2, detalle.getIdVariante());
            ps.setInt(3, detalle.getCantidad());
            ps.executeUpdate();
        }

        // Registrar movimiento de inventario
        String tipoMovimiento = (detalle.getTipoUnidad() == TipoUnidad.CAJA)
                ? "entrada caja"
                : "entrada par";

        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_MOVIMIENTO_INVENTARIO)) {
            ps.setInt(1, detalle.getIdProducto());
            ps.setInt(2, detalle.getIdVariante());
            ps.setString(3, tipoMovimiento);
            ps.setInt(4, detalle.getCantidad());
            int cantidadPares = (detalle.getTipoUnidad() == TipoUnidad.CAJA)
                    ? detalle.getCantidad() * 24
                    : detalle.getCantidad();
            ps.setInt(5, cantidadPares);
            ps.setInt(6, detalle.getIdCompra());
            ps.setInt(7, idUsuario);
            ps.setString(8, "Ingreso por compra a proveedor: " + detalle.getNombreProducto());
            ps.executeUpdate();
        }
    }

    private void revertirInventario(Connection conn, int idBodega,
            ModelCompraDetalle detalle, int idUsuario) throws SQLException {

        String sqlRevertir = (detalle.getTipoUnidad() == TipoUnidad.CAJA)
                ? SQL_REVERTIR_INVENTARIO_CAJA
                : SQL_REVERTIR_INVENTARIO_PAR;

        try (PreparedStatement ps = conn.prepareStatement(sqlRevertir)) {
            ps.setInt(1, detalle.getCantidad());
            ps.setInt(2, idBodega);
            ps.setInt(3, detalle.getIdVariante());
            ps.executeUpdate();
        }

        // Registrar movimiento de reversión
        String tipoMovimiento = (detalle.getTipoUnidad() == TipoUnidad.CAJA)
                ? "salida caja"
                : "salida par";

        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_MOVIMIENTO_INVENTARIO)) {
            ps.setInt(1, detalle.getIdProducto());
            ps.setInt(2, detalle.getIdVariante());
            ps.setString(3, tipoMovimiento);
            ps.setInt(4, detalle.getCantidad());
            int cantidadParesSalida = (detalle.getTipoUnidad() == TipoUnidad.CAJA)
                    ? detalle.getCantidad() * 24
                    : detalle.getCantidad();
            ps.setInt(5, cantidadParesSalida);
            ps.setInt(6, detalle.getIdCompra());
            ps.setInt(7, idUsuario);
            ps.setString(8, "Reversión por anulación de compra");
            ps.executeUpdate();
        }
    }

    private void marcarRecibida(Connection conn, int idCompra) throws SQLException {
        String sql = "UPDATE compras SET estado = 'recibida', fecha_recepcion = NOW() WHERE id_compra = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCompra);
            ps.executeUpdate();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTODOS DE MAPEO
    // ═══════════════════════════════════════════════════════════════════════════

    private ModelCompra mapearCompra(ResultSet rs) throws SQLException {
        ModelCompra compra = new ModelCompra();

        compra.setIdCompra(rs.getInt("id_compra"));
        compra.setNumeroCompra(rs.getString("numero_compra"));
        compra.setIdProveedor(rs.getInt("id_proveedor"));
        compra.setIdUsuario(rs.getInt("id_usuario"));
        compra.setIdBodega(rs.getInt("id_bodega"));

        Date fechaCompra = rs.getDate("fecha_compra");
        if (fechaCompra != null) {
            compra.setFechaCompra(fechaCompra.toLocalDate());
        }

        compra.setNumeroFactura(rs.getString("numero_factura"));
        compra.setSubtotal(rs.getBigDecimal("subtotal"));
        compra.setIva(rs.getBigDecimal("iva"));
        compra.setTotal(rs.getBigDecimal("total"));
        compra.setEstadoFromString(rs.getString("estado"));
        compra.setObservaciones(rs.getString("observaciones"));

        Timestamp fechaRecepcion = rs.getTimestamp("fecha_recepcion");
        if (fechaRecepcion != null) {
            compra.setFechaRecepcion(fechaRecepcion.toLocalDateTime());
        }

        try {
            compra.setNombreProveedor(rs.getString("nombre_proveedor"));
            compra.setRucProveedor(rs.getString("ruc_proveedor"));
            compra.setNombreBodega(rs.getString("nombre_bodega"));
            compra.setNombreUsuario(rs.getString("nombre_usuario"));
        } catch (SQLException e) {
            // Columnas opcionales
        }

        try {
            compra.setTotalAbonado(rs.getBigDecimal("total_abonado"));
            compra.setSaldoPendiente(rs.getBigDecimal("saldo_pendiente"));
            compra.setEstadoPago(rs.getString("estado_pago"));
        } catch (SQLException e) {
        }

        return compra;
    }

    private ModelCompra mapearCompraResumen(ResultSet rs) throws SQLException {
        ModelCompra compra = mapearCompra(rs);

        // Campos adicionales del resumen
        try {
            int totalItems = rs.getInt("total_items");
            int totalUnidades = rs.getInt("total_unidades");
            compra.setTotalItemsResumen(totalItems);
            compra.setTotalUnidadesResumen(totalUnidades);
        } catch (SQLException e) {
            // Columnas opcionales
        }

        return compra;
    }

    private ModelCompraDetalle mapearDetalle(ResultSet rs) throws SQLException {
        ModelCompraDetalle detalle = new ModelCompraDetalle();

        detalle.setIdDetalleCompra(rs.getInt("id_detalle_compra"));
        detalle.setIdCompra(rs.getInt("id_compra"));
        detalle.setIdProducto(rs.getInt("id_producto"));
        detalle.setIdVariante(rs.getInt("id_variante"));
        detalle.setCantidad(rs.getInt("cantidad"));
        detalle.setTipoUnidadFromString(rs.getString("tipo_unidad"));
        detalle.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
        detalle.setSubtotal(rs.getBigDecimal("subtotal"));

        try {
            detalle.setCodigoModelo(rs.getString("codigo_modelo"));
            detalle.setNombreProducto(rs.getString("nombre_producto"));
            detalle.setNombreTalla(rs.getString("talla"));
            detalle.setNombreColor(rs.getString("color"));
            detalle.setSku(rs.getString("sku"));
            detalle.setEan(rs.getString("ean"));
            detalle.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        } catch (SQLException e) {
            // Columnas opcionales
        }

        return detalle;
    }

    public static class AbonoResultado {
        public int idAbono;
        public BigDecimal saldoNuevo;
        public String estadoPago;
    }

    public static class AbonoView {
        public int idAbono;
        public Timestamp fechaAbono;
        public BigDecimal monto;
        public String medioPago;
        public String numeroComprobante;
        public String estado;
        public String evidenciaUrl;
        public String evidenciaNombre;
        public String evidenciaMime;
        public byte[] evidenciaBytes;
    }

    /**
     * Lista los abonos registrados para una compra, incluyendo evidencia.
     */
    public List<AbonoView> listarAbonosCompra(int idCompra) throws SQLException {
        String sql = """
                SELECT id_abono, fecha_abono, monto, medio_pago, numero_comprobante, estado,
                       evidencia_url, evidencia_nombre, evidencia_mime, evidencia_bytes
                FROM abonos
                WHERE id_compra = ?
                ORDER BY fecha_abono DESC
                """;
        List<AbonoView> list = new ArrayList<>();
        try (Connection con = conexion.getConnectionStatic();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCompra);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AbonoView a = new AbonoView();
                    a.idAbono = rs.getInt("id_abono");
                    a.fechaAbono = rs.getTimestamp("fecha_abono");
                    a.monto = rs.getBigDecimal("monto");
                    a.medioPago = rs.getString("medio_pago");
                    a.numeroComprobante = rs.getString("numero_comprobante");
                    a.estado = rs.getString("estado");
                    a.evidenciaUrl = rs.getString("evidencia_url");
                    a.evidenciaNombre = rs.getString("evidencia_nombre");
                    a.evidenciaMime = rs.getString("evidencia_mime");
                    a.evidenciaBytes = rs.getBytes("evidencia_bytes");
                    list.add(a);
                }
            }
        }
        return list;
    }

    public AbonoResultado registrarAbonoConEvidencia(int idCompra,
                                                     BigDecimal monto,
                                                     String medioPago,
                                                     String evidenciaUrl,
                                                     byte[] evidenciaBytes,
                                                     String evidenciaMime,
                                                     String evidenciaNombre) throws SQLException {
        try (Connection conn = conexion.getConnectionStatic()) {
            String call = "{CALL sp_registrar_abono_con_evidencia(?,?,?,?,?,?,?, ?, ?, ?)}";
            try (CallableStatement cs = conn.prepareCall(call)) {
                cs.setInt(1, idCompra);
                cs.setBigDecimal(2, monto);
                cs.setString(3, medioPago);
                cs.setString(4, evidenciaUrl);
                if (evidenciaBytes != null) {
                    cs.setBytes(5, evidenciaBytes);
                } else {
                    cs.setNull(5, Types.BLOB);
                }
                cs.setString(6, evidenciaMime);
                cs.setString(7, evidenciaNombre);
                cs.registerOutParameter(8, Types.INTEGER);
                cs.registerOutParameter(9, Types.DECIMAL);
                cs.registerOutParameter(10, Types.VARCHAR);
                cs.execute();

                AbonoResultado r = new AbonoResultado();
                r.idAbono = cs.getInt(8);
                r.saldoNuevo = cs.getBigDecimal(9);
                r.estadoPago = cs.getString(10);
                return r;
            } catch (SQLException ex) {
                String sqlRs = "CALL sp_registrar_abono_con_evidencia_rs(?,?,?,?,?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlRs)) {
                    ps.setInt(1, idCompra);
                    ps.setBigDecimal(2, monto);
                    ps.setString(3, medioPago);
                    ps.setString(4, evidenciaUrl);
                    if (evidenciaBytes != null) {
                        ps.setBytes(5, evidenciaBytes);
                    } else {
                        ps.setNull(5, Types.BLOB);
                    }
                    ps.setString(6, evidenciaMime);
                    ps.setString(7, evidenciaNombre);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            AbonoResultado r = new AbonoResultado();
                            r.idAbono = rs.getInt("id_abono");
                            r.saldoNuevo = rs.getBigDecimal("saldo_nuevo");
                            r.estadoPago = rs.getString("estado_pago");
                            return r;
                        } else {
                            throw ex;
                        }
                    }
                }
            }
        }
    }
    
    
    
     // ═══════════════════════════════════════════════════════════════════════════
    // CLASES INTERNAS PARA BÚSQUEDA
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * DTO para resultados de búsqueda de productos.
     */
    public static class ProductoBusqueda {
        public int idProducto;
        public String codigoModelo;
        public String nombre;
        public BigDecimal precioCompra;
        public BigDecimal precioVenta;
        public String marca;
        public String categoria;

        public String getDescripcion() {
            return String.format("%s - %s", codigoModelo, nombre);
        }
    }

    /**
     * DTO para resultados de búsqueda de variantes.
     */
    public static class VarianteBusqueda {
        public int idVariante;
        public int idProducto;
        public String sku;
        public String ean;
        public int idTalla;
        public String talla;
        public int idColor;
        public String color;
        public BigDecimal precioCompra;
        public BigDecimal precioVenta;
        public int stockPares;
        public int stockCajas;
        public String codigoModelo;
        public String nombreProducto;
        public int idProveedor;
        public String nombreProveedor;

        public String getDescripcionVariante() {
            return String.format("Talla: %s - Color: %s", talla, color);
        }

        public String getDescripcionCompleta() {
            return String.format("%s (%s - %s)", nombreProducto, talla, color);
        }
    }


}

   
