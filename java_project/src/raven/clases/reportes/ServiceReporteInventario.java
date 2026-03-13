package raven.clases.reportes;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import raven.controlador.principal.conexion;
import raven.dao.ConteoInventarioDAO;
import raven.modelos.ConteoInventario;
import raven.controlador.productos.ModelProduct;
import java.util.Date;
import java.time.ZoneId;

/**
 * Servicio para generar reportes de inventario
 * Proporciona análisis de stock, rotación y valorización
 */
public class ServiceReporteInventario {

    private static final Logger LOGGER = Logger.getLogger(ServiceReporteInventario.class.getName());

    public List<Map<String, Object>> getStockPorBodega(int idBodega) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT
                    ib.id_inventario_bodega,
                    b.nombre AS bodega,
                    p.id_producto,
                    pv.id_variante,
                    p.nombre AS producto,
                    p.codigo_modelo,
                    m.nombre AS marca,
                    c.nombre AS categoria,
                    t.numero AS talla,
                    col.nombre AS color,
                    pv.ean,
                    pv.sku,
                    ib.Stock_par,
                    ib.Stock_caja,
                    p.pares_por_caja,
                    (ib.Stock_par + (ib.Stock_caja * COALESCE(p.pares_por_caja, 24))) AS stock_total_pares,
                    COALESCE(pv.precio_compra, p.precio_compra) AS precio_compra,
                    COALESCE(pv.precio_venta, p.precio_venta) AS precio_venta,
                    ib.fecha_ultimo_movimiento
                FROM inventario_bodega ib
                JOIN bodegas b ON ib.id_bodega = b.id_bodega
                JOIN producto_variantes pv ON ib.id_variante = pv.id_variante
                JOIN productos p ON pv.id_producto = p.id_producto
                LEFT JOIN marcas m ON p.id_marca = m.id_marca
                LEFT JOIN categorias c ON p.id_categoria = c.id_categoria
                LEFT JOIN tallas t ON pv.id_talla = t.id_talla
                LEFT JOIN colores col ON pv.id_color = col.id_color
                WHERE ib.activo = 1
                AND (? = 0 OR ib.id_bodega = ?)
                ORDER BY p.nombre, t.numero
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idBodega);
            ps.setInt(2, idBodega);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id_inventario", rs.getInt("id_inventario_bodega"));
                    row.put("bodega", rs.getString("bodega"));
                    row.put("id_producto", rs.getInt("id_producto"));
                    row.put("id_variante", rs.getInt("id_variante"));
                    row.put("producto", rs.getString("producto"));
                    row.put("codigo_modelo", rs.getString("codigo_modelo"));
                    row.put("marca", rs.getString("marca"));
                    row.put("categoria", rs.getString("categoria"));
                    row.put("talla", rs.getString("talla"));
                    row.put("color", rs.getString("color"));
                    row.put("ean", rs.getString("ean"));
                    row.put("sku", rs.getString("sku"));
                    row.put("stock_pares", rs.getInt("Stock_par"));
                    row.put("stock_cajas", rs.getInt("Stock_caja"));
                    row.put("pares_por_caja", rs.getInt("pares_por_caja"));
                    row.put("stock_total_pares", rs.getInt("stock_total_pares"));
                    row.put("precio_compra", rs.getBigDecimal("precio_compra"));
                    row.put("precio_venta", rs.getBigDecimal("precio_venta"));
                    row.put("fecha_ultimo_movimiento", rs.getTimestamp("fecha_ultimo_movimiento"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo stock por bodega", e);
        }
        return resultados;
    }

    public byte[] getImagenVariante(int idVariante) {
        String sql = "SELECT imagen FROM producto_variantes WHERE id_variante = ?";
        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idVariante);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBytes("imagen");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo imagen de variante: " + idVariante, e);
        }
        return null;
    }

    public List<Map<String, Object>> getProductosConStock(int idBodega) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT
                    ib.id_inventario_bodega,
                    b.nombre AS bodega,
                    p.id_producto,
                    pv.id_variante,
                    p.nombre AS producto,
                    p.codigo_modelo,
                    m.nombre AS marca,
                    c.nombre AS categoria,
                    t.numero AS talla,
                    col.nombre AS color,
                    pv.ean,
                    pv.sku,
                    ib.Stock_par,
                    ib.Stock_caja,
                    p.pares_por_caja,
                    (ib.Stock_par + (ib.Stock_caja * COALESCE(p.pares_por_caja, 24))) AS stock_total_pares,
                    COALESCE(pv.precio_compra, p.precio_compra) AS precio_compra,
                    COALESCE(pv.precio_venta, p.precio_venta) AS precio_venta,
                    ib.fecha_ultimo_movimiento
                FROM inventario_bodega ib
                JOIN bodegas b ON ib.id_bodega = b.id_bodega
                JOIN producto_variantes pv ON ib.id_variante = pv.id_variante
                JOIN productos p ON pv.id_producto = p.id_producto
                LEFT JOIN marcas m ON p.id_marca = m.id_marca
                LEFT JOIN categorias c ON p.id_categoria = c.id_categoria
                LEFT JOIN tallas t ON pv.id_talla = t.id_talla
                LEFT JOIN colores col ON pv.id_color = col.id_color
                WHERE ib.activo = 1
                AND (? = 0 OR ib.id_bodega = ?)
                AND (ib.Stock_par > 0 OR ib.Stock_caja > 0)
                ORDER BY p.nombre, t.numero
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idBodega);
            ps.setInt(2, idBodega);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id_inventario", rs.getInt("id_inventario_bodega"));
                    row.put("bodega", rs.getString("bodega"));
                    row.put("id_producto", rs.getInt("id_producto"));
                    row.put("id_variante", rs.getInt("id_variante"));
                    row.put("producto", rs.getString("producto"));
                    row.put("codigo_modelo", rs.getString("codigo_modelo"));
                    row.put("marca", rs.getString("marca"));
                    row.put("categoria", rs.getString("categoria"));
                    row.put("talla", rs.getString("talla"));
                    row.put("color", rs.getString("color"));
                    row.put("ean", rs.getString("ean"));
                    row.put("sku", rs.getString("sku"));
                    row.put("stock_pares", rs.getInt("Stock_par"));
                    row.put("stock_cajas", rs.getInt("Stock_caja"));
                    row.put("pares_por_caja", rs.getInt("pares_por_caja"));
                    row.put("stock_total_pares", rs.getInt("stock_total_pares"));
                    row.put("precio_compra", rs.getBigDecimal("precio_compra"));
                    row.put("precio_venta", rs.getBigDecimal("precio_venta"));
                    row.put("fecha_ultimo_movimiento", rs.getTimestamp("fecha_ultimo_movimiento"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo productos con stock", e);
        }
        return resultados;
    }

    /**
     * Obtiene productos con stock bajo el umbral mínimo
     */
    public List<Map<String, Object>> getProductosStockBajo(int idBodega, int umbralPersonalizado) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT
                    p.id_producto,
                    p.nombre AS producto,
                    p.codigo_modelo,
                    m.nombre AS marca,
                    c.nombre AS categoria,
                    t.numero AS talla,
                    col.nombre AS color,
                    b.nombre AS bodega,
                    ib.Stock_par,
                    ib.Stock_caja,
                    (ib.Stock_par + (ib.Stock_caja * COALESCE(p.pares_por_caja, 24))) AS stock_total,
                    COALESCE(pv.stock_minimo_variante, p.stock_minimo, 5) AS stock_minimo,
                    CASE
                        WHEN (ib.Stock_par + (ib.Stock_caja * COALESCE(p.pares_por_caja, 24))) = 0 THEN 'SIN STOCK'
                        WHEN (ib.Stock_par + (ib.Stock_caja * COALESCE(p.pares_por_caja, 24))) <=
                             COALESCE(pv.stock_minimo_variante, p.stock_minimo, 5) / 2 THEN 'CRÍTICO'
                        ELSE 'BAJO'
                    END AS estado_stock
                FROM inventario_bodega ib
                JOIN bodegas b ON ib.id_bodega = b.id_bodega
                JOIN producto_variantes pv ON ib.id_variante = pv.id_variante
                JOIN productos p ON pv.id_producto = p.id_producto
                LEFT JOIN marcas m ON p.id_marca = m.id_marca
                LEFT JOIN categorias c ON p.id_categoria = c.id_categoria
                LEFT JOIN tallas t ON pv.id_talla = t.id_talla
                LEFT JOIN colores col ON pv.id_color = col.id_color
                WHERE ib.activo = 1
                AND (? = 0 OR ib.id_bodega = ?)
                AND (ib.Stock_par + (ib.Stock_caja * COALESCE(p.pares_por_caja, 24))) <=
                    CASE WHEN ? > 0 THEN ? ELSE COALESCE(pv.stock_minimo_variante, p.stock_minimo, 5) END
                ORDER BY stock_total ASC, p.nombre
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idBodega);
            ps.setInt(2, idBodega);
            ps.setInt(3, umbralPersonalizado);
            ps.setInt(4, umbralPersonalizado);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id_producto", rs.getInt("id_producto"));
                    row.put("producto", rs.getString("producto"));
                    row.put("codigo_modelo", rs.getString("codigo_modelo"));
                    row.put("marca", rs.getString("marca"));
                    row.put("categoria", rs.getString("categoria"));
                    row.put("talla", rs.getString("talla"));
                    row.put("color", rs.getString("color"));
                    row.put("bodega", rs.getString("bodega"));
                    row.put("stock_pares", rs.getInt("Stock_par"));
                    row.put("stock_cajas", rs.getInt("Stock_caja"));
                    row.put("stock_total", rs.getInt("stock_total"));
                    row.put("stock_minimo", rs.getInt("stock_minimo"));
                    row.put("estado_stock", rs.getString("estado_stock"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo productos con stock bajo", e);
        }
        return resultados;
    }

    /**
     * Calcula la rotación de inventario para un período
     */
    public List<Map<String, Object>> getRotacionInventario(int idBodega, LocalDate fechaInicio, LocalDate fechaFin) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT
                    p.id_producto,
                    p.nombre AS producto,
                    p.codigo_modelo,
                    m.nombre AS marca,
                    c.nombre AS categoria,
                    COALESCE(SUM(vd.cantidad), 0) AS unidades_vendidas,
                    AVG(ib.Stock_par + (ib.Stock_caja * COALESCE(p.pares_por_caja, 24))) AS stock_promedio,
                    CASE
                        WHEN AVG(ib.Stock_par + (ib.Stock_caja * COALESCE(p.pares_por_caja, 24))) > 0
                        THEN ROUND(COALESCE(SUM(vd.cantidad), 0) /
                             AVG(ib.Stock_par + (ib.Stock_caja * COALESCE(p.pares_por_caja, 24))), 2)
                        ELSE 0
                    END AS indice_rotacion,
                    CASE
                        WHEN COALESCE(SUM(vd.cantidad), 0) > 0
                        THEN ROUND(DATEDIFF(?, ?) / (COALESCE(SUM(vd.cantidad), 0) /
                             NULLIF(AVG(ib.Stock_par + (ib.Stock_caja * COALESCE(p.pares_por_caja, 24))), 0)), 0)
                        ELSE 999
                    END AS dias_inventario
                FROM productos p
                JOIN producto_variantes pv ON p.id_producto = pv.id_producto
                JOIN inventario_bodega ib ON pv.id_variante = ib.id_variante
                LEFT JOIN marcas m ON p.id_marca = m.id_marca
                LEFT JOIN categorias c ON p.id_categoria = c.id_categoria
                LEFT JOIN venta_detalles vd ON vd.id_variante = pv.id_variante AND vd.activo = 1
                LEFT JOIN ventas v ON vd.id_venta = v.id_venta
                    AND v.fecha_venta BETWEEN ? AND ?
                    AND v.estado = 'completada'
                WHERE ib.activo = 1
                AND (? = 0 OR ib.id_bodega = ?)
                GROUP BY p.id_producto, p.nombre, p.codigo_modelo, m.nombre, c.nombre
                ORDER BY indice_rotacion DESC
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, fechaFin);
            ps.setObject(2, fechaInicio);
            ps.setObject(3, fechaInicio);
            ps.setObject(4, fechaFin);
            ps.setInt(5, idBodega);
            ps.setInt(6, idBodega);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id_producto", rs.getInt("id_producto"));
                    row.put("producto", rs.getString("producto"));
                    row.put("codigo_modelo", rs.getString("codigo_modelo"));
                    row.put("marca", rs.getString("marca"));
                    row.put("categoria", rs.getString("categoria"));
                    row.put("unidades_vendidas", rs.getInt("unidades_vendidas"));
                    row.put("stock_promedio", rs.getBigDecimal("stock_promedio"));
                    row.put("indice_rotacion", rs.getBigDecimal("indice_rotacion"));
                    row.put("dias_inventario", rs.getInt("dias_inventario"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error calculando rotación de inventario", e);
        }
        return resultados;
    }

    /**
     * Obtiene la valorización del inventario
     */
    public Map<String, Object> getValorizacionInventario(int idBodega) {
        Map<String, Object> resultado = new HashMap<>();
        String sql = """
                SELECT
                    COUNT(DISTINCT p.id_producto) AS total_productos,
                    COUNT(DISTINCT pv.id_variante) AS total_variantes,
                    SUM(ib.Stock_par + (ib.Stock_caja * COALESCE(p.pares_por_caja, 24))) AS total_unidades,
                    SUM((ib.Stock_par + (ib.Stock_caja * COALESCE(p.pares_por_caja, 24))) *
                        COALESCE(pv.precio_compra, p.precio_compra)) AS valor_costo,
                    SUM((ib.Stock_par + (ib.Stock_caja * COALESCE(p.pares_por_caja, 24))) *
                        COALESCE(pv.precio_venta, p.precio_venta)) AS valor_venta,
                    SUM((ib.Stock_par + (ib.Stock_caja * COALESCE(p.pares_por_caja, 24))) *
                        (COALESCE(pv.precio_venta, p.precio_venta) - COALESCE(pv.precio_compra, p.precio_compra))) AS margen_potencial
                FROM inventario_bodega ib
                JOIN producto_variantes pv ON ib.id_variante = pv.id_variante
                JOIN productos p ON pv.id_producto = p.id_producto
                WHERE ib.activo = 1
                AND (? = 0 OR ib.id_bodega = ?)
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idBodega);
            ps.setInt(2, idBodega);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    resultado.put("total_productos", rs.getInt("total_productos"));
                    resultado.put("total_variantes", rs.getInt("total_variantes"));
                    resultado.put("total_unidades", rs.getInt("total_unidades"));
                    resultado.put("valor_costo", rs.getBigDecimal("valor_costo"));
                    resultado.put("valor_venta", rs.getBigDecimal("valor_venta"));
                    resultado.put("margen_potencial", rs.getBigDecimal("margen_potencial"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error calculando valorización de inventario", e);
        }
        return resultado;
    }

    /**
     * Obtiene productos sin movimiento en los últimos N días
     */
    public List<Map<String, Object>> getProductosSinMovimiento(int idBodega, int dias) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT
                    p.id_producto,
                    p.nombre AS producto,
                    p.codigo_modelo,
                    m.nombre AS marca,
                    c.nombre AS categoria,
                    t.numero AS talla,
                    col.nombre AS color,
                    b.nombre AS bodega,
                    ib.Stock_par,
                    ib.Stock_caja,
                    (ib.Stock_par + (ib.Stock_caja * COALESCE(p.pares_por_caja, 24))) AS stock_total,
                    ib.fecha_ultimo_movimiento,
                    DATEDIFF(CURDATE(), ib.fecha_ultimo_movimiento) AS dias_sin_movimiento,
                    (ib.Stock_par + (ib.Stock_caja * COALESCE(p.pares_por_caja, 24))) *
                        COALESCE(pv.precio_compra, p.precio_compra) AS valor_inmovilizado
                FROM inventario_bodega ib
                JOIN bodegas b ON ib.id_bodega = b.id_bodega
                JOIN producto_variantes pv ON ib.id_variante = pv.id_variante
                JOIN productos p ON pv.id_producto = p.id_producto
                LEFT JOIN marcas m ON p.id_marca = m.id_marca
                LEFT JOIN categorias c ON p.id_categoria = c.id_categoria
                LEFT JOIN tallas t ON pv.id_talla = t.id_talla
                LEFT JOIN colores col ON pv.id_color = col.id_color
                WHERE ib.activo = 1
                AND (? = 0 OR ib.id_bodega = ?)
                AND (ib.Stock_par + ib.Stock_caja) > 0
                AND (ib.fecha_ultimo_movimiento IS NULL OR
                     DATEDIFF(CURDATE(), ib.fecha_ultimo_movimiento) >= ?)
                ORDER BY dias_sin_movimiento DESC, valor_inmovilizado DESC
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idBodega);
            ps.setInt(2, idBodega);
            ps.setInt(3, dias);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id_producto", rs.getInt("id_producto"));
                    row.put("producto", rs.getString("producto"));
                    row.put("codigo_modelo", rs.getString("codigo_modelo"));
                    row.put("marca", rs.getString("marca"));
                    row.put("categoria", rs.getString("categoria"));
                    row.put("talla", rs.getString("talla"));
                    row.put("color", rs.getString("color"));
                    row.put("bodega", rs.getString("bodega"));
                    row.put("stock_pares", rs.getInt("Stock_par"));
                    row.put("stock_cajas", rs.getInt("Stock_caja"));
                    row.put("stock_total", rs.getInt("stock_total"));
                    row.put("fecha_ultimo_movimiento", rs.getTimestamp("fecha_ultimo_movimiento"));
                    row.put("dias_sin_movimiento", rs.getInt("dias_sin_movimiento"));
                    row.put("valor_inmovilizado", rs.getBigDecimal("valor_inmovilizado"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo productos sin movimiento", e);
        }
        return resultados;
    }

    /**
     * Obtiene resumen de inventario por categoría
     */
    public List<Map<String, Object>> getResumenPorCategoria(int idBodega) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = """
                SELECT
                    c.id_categoria,
                    c.nombre AS categoria,
                    COUNT(DISTINCT p.id_producto) AS total_productos,
                    SUM(ib.Stock_par + (ib.Stock_caja * COALESCE(p.pares_por_caja, 24))) AS total_unidades,
                    SUM((ib.Stock_par + (ib.Stock_caja * COALESCE(p.pares_por_caja, 24))) *
                        COALESCE(pv.precio_compra, p.precio_compra)) AS valor_costo,
                    SUM((ib.Stock_par + (ib.Stock_caja * COALESCE(p.pares_por_caja, 24))) *
                        COALESCE(pv.precio_venta, p.precio_venta)) AS valor_venta
                FROM inventario_bodega ib
                JOIN producto_variantes pv ON ib.id_variante = pv.id_variante
                JOIN productos p ON pv.id_producto = p.id_producto
                LEFT JOIN categorias c ON p.id_categoria = c.id_categoria
                WHERE ib.activo = 1
                AND (? = 0 OR ib.id_bodega = ?)
                GROUP BY c.id_categoria, c.nombre
                ORDER BY valor_venta DESC
                """;

        try (Connection con = conexion.getConnectionStatic();
                PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idBodega);
            ps.setInt(2, idBodega);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id_categoria", rs.getInt("id_categoria"));
                    row.put("categoria", rs.getString("categoria"));
                    row.put("total_productos", rs.getInt("total_productos"));
                    row.put("total_unidades", rs.getInt("total_unidades"));
                    row.put("valor_costo", rs.getBigDecimal("valor_costo"));
                    row.put("valor_venta", rs.getBigDecimal("valor_venta"));
                    resultados.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo resumen por categoría", e);
        }
        return resultados;
    }

    /**
     * Obtiene lista de bodegas para filtros
     */
    public List<Map<String, Object>> getBodegas() {
        List<Map<String, Object>> resultados = new ArrayList<>();
        String sql = "SELECT id_bodega, nombre, codigo FROM bodegas WHERE activa = 1 ORDER BY nombre";
        try (Connection con = conexion.getInstance().getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id_bodega", rs.getInt("id_bodega"));
                row.put("nombre", rs.getString("nombre"));
                row.put("codigo", rs.getString("codigo"));
                resultados.add(row);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo bodegas", e);
        }
        return resultados;
    }

    public List<Map<String, Object>> getHistorialConteos(int idBodega, LocalDate inicio, LocalDate fin) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        ConteoInventarioDAO dao = new ConteoInventarioDAO();

        try {
            Date dateInicio = Date.from(inicio.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date dateFin = Date.from(fin.atStartOfDay(ZoneId.systemDefault()).toInstant());

            List<ConteoInventario> conteos = dao.obtenerHistorialConteos(dateInicio, dateFin, idBodega);

            for (ConteoInventario c : conteos) {
                Map<String, Object> row = new HashMap<>();
                row.put("id_conteo", c.getId());
                row.put("nombre", c.getNombre());
                row.put("fecha", c.getFechaProgramada());
                row.put("tipo", c.getTipo());
                row.put("estado", c.getEstado());
                row.put("responsable", c.getResponsable().getNombre());
                row.put("total_productos", c.getTotalProductos());
                row.put("productos_contados", c.getProductosContados());
                row.put("pendientes", c.getTotalProductos() - c.getProductosContados());

                resultados.add(row);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo historial de conteos", e);
        }

        return resultados;
    }

    public List<Map<String, Object>> getDetalleConteo(int idConteo) {
        List<Map<String, Object>> resultados = new ArrayList<>();
        ConteoInventarioDAO dao = new ConteoInventarioDAO();

        try {
            List<raven.modelos.DetalleConteoInventario> detalles = dao.obtenerDetallesConteo(idConteo);

            for (raven.modelos.DetalleConteoInventario d : detalles) {
                Map<String, Object> row = new HashMap<>();
                ModelProduct p = d.getProducto();
                if (p != null) {
                    row.put("producto", p.getName() != null ? p.getName() : "");
                    row.put("sku", p.getBarcode() != null ? p.getBarcode() : "");
                    row.put("marca",
                            (p.getBrand() != null && p.getBrand().getName() != null) ? p.getBrand().getName() : "");
                    row.put("categoria",
                            (p.getCategory() != null && p.getCategory().getName() != null) ? p.getCategory().getName()
                                    : "");
                    row.put("talla", p.getSize() != null ? p.getSize() : "");
                    row.put("color", p.getColor() != null ? p.getColor() : "");
                    row.put("ubicacion", p.getUbicacion() != null ? p.getUbicacion() : "");
                } else {
                    row.put("producto", "Desconocido");
                    row.put("sku", "");
                    row.put("marca", "");
                    row.put("categoria", "");
                    row.put("talla", "");
                    row.put("color", "");
                    row.put("ubicacion", "");
                }

                row.put("stock_sistema", d.getStockSistema());
                row.put("stock_contado", d.getStockContado());
                row.put("diferencia", d.getDiferencia());
                row.put("estado", d.getEstado());

                resultados.add(row);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error obteniendo detalles de conteo: " + idConteo, e);
        }

        return resultados;
    }
}
