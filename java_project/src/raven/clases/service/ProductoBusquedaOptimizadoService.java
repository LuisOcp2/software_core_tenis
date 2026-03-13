package raven.clases.service;

import raven.application.form.other.buscador.dto.BusquedaCriteria;
import raven.application.form.other.buscador.dto.ProductoDTO;
import raven.application.form.other.buscador.dto.VarianteDTO;
import raven.componentes.repository.ProductoRepository;
import raven.controlador.principal.conexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

public class ProductoBusquedaOptimizadoService {
    private final ProductoRepository repository;

    public ProductoBusquedaOptimizadoService(ProductoRepository repository) {
        this.repository = repository;
    }

    public List<ProductoDTO> buscarProductosParaTraspaso(BusquedaCriteria criteria) {
        if (criteria.getIdBodega() == null || criteria.getIdBodega() <= 0) {
            return new ArrayList<>();
        }
        String textoBusqueda = criteria.getTextoBusqueda();
        if (esCodigoEAN(textoBusqueda)) {
            List<ProductoDTO> r = buscarPorEANExacto(textoBusqueda, criteria.getIdBodega());
            if (!r.isEmpty()) return r;
        }
        if (esIDProducto(textoBusqueda)) {
            List<ProductoDTO> r = buscarPorIDProducto(textoBusqueda, criteria.getIdBodega());
            if (!r.isEmpty()) return r;
        }
        return buscarConScoring(criteria);
    }

    private List<ProductoDTO> buscarPorEANExacto(String ean, Integer idBodega) {
        String sql = "SELECT DISTINCT " +
                "    p.id_producto, " +
                "    p.codigo_modelo, " +
                "    p.nombre, " +
                "    p.genero, " +
                "    p.imagen AS imagen_principal " +
                "FROM producto_variantes pv " +
                "INNER JOIN productos p ON p.id_producto = pv.id_producto " +
                "INNER JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante " +
                "WHERE pv.ean = ? " +
                "  AND ib.id_bodega = ? " +
                "  AND ib.activo = 1 " +
                "  AND p.activo = 1 " +
                "  AND pv.disponible = 1 " +
                "  AND (ib.Stock_par > 0 OR ib.Stock_caja > 0) " +
                "LIMIT 1";
        return ejecutarConsultaProductos(sql, ean, idBodega);
    }

    private List<ProductoDTO> buscarPorIDProducto(String idTexto, Integer idBodega) {
        try {
            int idProducto = Integer.parseInt(idTexto);
            String sql = "SELECT DISTINCT " +
                    "    p.id_producto, " +
                    "    p.codigo_modelo, " +
                    "    p.nombre, " +
                    "    p.genero, " +
                    "    p.imagen AS imagen_principal " +
                    "FROM productos p " +
                    "INNER JOIN producto_variantes pv ON p.id_producto = pv.id_producto " +
                    "INNER JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante " +
                    "WHERE p.id_producto = ? " +
                    "  AND ib.id_bodega = ? " +
                    "  AND ib.activo = 1 " +
                    "  AND p.activo = 1 " +
                    "  AND pv.disponible = 1 " +
                    "  AND (ib.Stock_par > 0 OR ib.Stock_caja > 0) " +
                    "LIMIT 1";
            return ejecutarConsultaProductos(sql, idProducto, idBodega);
        } catch (NumberFormatException e) {
            return new ArrayList<>();
        }
    }

    private List<ProductoDTO> buscarConScoring(BusquedaCriteria criteria) {
        String texto = criteria.getTextoBusqueda();
        Integer idBodega = criteria.getIdBodega();
        Integer limite = criteria.getLimite() != null ? criteria.getLimite() : 50;
        String textoLower = texto != null ? texto.toLowerCase().trim() : "";
        String textoLike = "%" + textoLower + "%";
        String textoInicio = textoLower + "%";
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT ");
        sql.append("    p.id_producto, ");
        sql.append("    p.codigo_modelo, ");
        sql.append("    p.nombre, ");
        sql.append("    p.genero, ");
        sql.append("    p.imagen AS imagen_principal, ");
        sql.append("    CASE ");
        sql.append("        WHEN LOWER(p.codigo_modelo) = ? THEN 100 ");
        sql.append("        WHEN LOWER(p.nombre) = ? THEN 90 ");
        sql.append("        WHEN LOWER(p.codigo_modelo) LIKE ? THEN 80 ");
        sql.append("        WHEN LOWER(p.nombre) LIKE ? THEN 75 ");
        sql.append("        WHEN LOWER(m.nombre) LIKE ? THEN 70 ");
        sql.append("        WHEN LOWER(p.genero) LIKE ? THEN 60 ");
        sql.append("        WHEN LOWER(p.descripcion) LIKE ? THEN 55 ");
        sql.append("        ELSE 50 ");
        sql.append("    END AS relevancia ");
        sql.append("FROM productos p ");
        sql.append("INNER JOIN producto_variantes pv ON p.id_producto = pv.id_producto ");
        sql.append("INNER JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante ");
        sql.append("LEFT JOIN marcas m ON p.id_marca = m.id_marca ");
        sql.append("WHERE ib.id_bodega = ? ");
        sql.append("  AND ib.activo = 1 ");
        sql.append("  AND p.activo = 1 ");
        sql.append("  AND pv.disponible = 1 ");
        sql.append("  AND (ib.Stock_par > 0 OR ib.Stock_caja > 0) ");
        sql.append("  AND ( ");
        sql.append("      LOWER(p.codigo_modelo) LIKE ? ");
        sql.append("   OR LOWER(p.nombre) LIKE ? ");
        sql.append("   OR LOWER(p.descripcion) LIKE ? ");
        sql.append("   OR LOWER(m.nombre) LIKE ? ");
        sql.append("   OR LOWER(p.genero) LIKE ? ");
        sql.append("   OR LOWER(pv.sku) LIKE ? ");
        sql.append("   OR LOWER(pv.ean) LIKE ? ");
        sql.append("  ) ");
        sql.append("ORDER BY relevancia DESC, p.nombre ASC ");
        sql.append("LIMIT ?");

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<ProductoDTO> productos = new ArrayList<>();
        try {
            conn = conexion.getInstance().createConnection();
            ps = conn.prepareStatement(sql.toString());
            int idx = 1;
            ps.setString(idx++, textoLower);
            ps.setString(idx++, textoLower);
            ps.setString(idx++, textoInicio);
            ps.setString(idx++, textoInicio);
            ps.setString(idx++, textoLike);
            ps.setString(idx++, textoLike);
            ps.setString(idx++, textoLike);
            ps.setInt(idx++, idBodega);
            ps.setString(idx++, textoLike);
            ps.setString(idx++, textoLike);
            ps.setString(idx++, textoLike);
            ps.setString(idx++, textoLike);
            ps.setString(idx++, textoLike);
            ps.setString(idx++, textoLike);
            ps.setInt(idx++, limite);
            rs = ps.executeQuery();
            while (rs.next()) {
                ProductoDTO producto = mapearProductoDesdeResultSet(rs);
                cargarVariantesPorBodega(producto, idBodega, conn);
                productos.add(producto);
            }
        } catch (SQLException e) {
        } finally {
            cerrarRecursos(rs, ps, conn);
        }
        return productos;
    }

    public List<ProductoDTO> obtenerSugerenciasPorBodega(Integer idBodega, int limite) {
        String sql = "SELECT DISTINCT " +
                "    p.id_producto, " +
                "    p.codigo_modelo, " +
                "    p.nombre, " +
                "    p.genero, " +
                "    p.imagen AS imagen_principal " +
                "FROM productos p " +
                "INNER JOIN producto_variantes pv ON p.id_producto = pv.id_producto " +
                "INNER JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante " +
                "WHERE ib.id_bodega = ? " +
                "  AND ib.activo = 1 " +
                "  AND p.activo = 1 " +
                "  AND pv.disponible = 1 " +
                "  AND (ib.Stock_par > 0 OR ib.Stock_caja > 0) " +
                "ORDER BY p.fecha_creacion DESC " +
                "LIMIT ?";
        return ejecutarConsultaProductos(sql, idBodega, limite);
    }

    private boolean esCodigoEAN(String texto) {
        if (texto == null || texto.trim().isEmpty()) return false;
        String limpio = texto.trim();
        return limpio.matches("\\d{8,20}");
    }

    private boolean esIDProducto(String texto) {
        if (texto == null || texto.trim().isEmpty()) return false;
        String limpio = texto.trim();
        return limpio.matches("\\d{1,10}");
    }

    private List<ProductoDTO> ejecutarConsultaProductos(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<ProductoDTO> productos = new ArrayList<>();
        try {
            conn = conexion.getInstance().createConnection();
            ps = conn.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                ProductoDTO producto = mapearProductoDesdeResultSet(rs);
                Integer idBodega = obtenerIdBodegaDeParametros(params);
                if (idBodega != null) {
                    cargarVariantesPorBodega(producto, idBodega, conn);
                }
                productos.add(producto);
            }
        } catch (SQLException e) {
        } finally {
            cerrarRecursos(rs, ps, conn);
        }
        return productos;
    }

    private ProductoDTO mapearProductoDesdeResultSet(ResultSet rs) throws SQLException {
        ProductoDTO producto = new ProductoDTO();
        producto.setIdProducto(rs.getInt("id_producto"));
        producto.setCodigoModelo(rs.getString("codigo_modelo"));
        producto.setNombre(rs.getString("nombre"));
        producto.setGenero(rs.getString("genero"));
        byte[] imagen = rs.getBytes("imagen_principal");
        producto.setImagenPrincipal(imagen);
        return producto;
    }

    private VarianteDTO mapearVarianteDesdeResultSet(ResultSet rs) throws SQLException {
        VarianteDTO variante = new VarianteDTO();
        variante.setIdVariante(rs.getInt("id_variante"));
        variante.setIdProducto(rs.getInt("id_producto"));
        variante.setTalla(rs.getString("talla"));
        variante.setColor(rs.getString("color"));
        variante.setEan(rs.getString("ean"));
        variante.setSku(rs.getString("sku"));
        BigDecimal precioCompra = rs.getBigDecimal("precio_compra");
        BigDecimal precioVenta = rs.getBigDecimal("precio_venta");
        variante.setPrecioCompra(precioCompra);
        variante.setPrecioVenta(precioVenta);
        Integer stockDisponible = rs.getInt("stock_disponible");
        Integer stockCaja = rs.getInt("stock_caja");
        Integer stockReservado = rs.getInt("stock_reservado");
        variante.setStockDisponible(stockDisponible);
        variante.setStockCaja(stockCaja);
        variante.setStockReservado(stockReservado);
        int paresPorCaja = 24;
        int stockPares = stockDisponible + (stockCaja * paresPorCaja);
        variante.setStockPares(stockPares);
        String ubicacion = rs.getString("ubicacion_especifica");
        variante.setUbicacionEspecifica(ubicacion);
        boolean disponible = rs.getBoolean("disponible");
        variante.setDisponible(disponible);
        return variante;
    }

    private void cargarVariantesPorBodega(ProductoDTO producto, Integer idBodega, Connection conn) {
        String sql = "SELECT " +
                "    pv.id_variante, " +
                "    pv.id_producto, " +
                "    t.numero AS talla, " +
                "    c.nombre AS color, " +
                "    pv.ean, " +
                "    pv.sku, " +
                "    pv.precio_compra, " +
                "    pv.precio_venta, " +
                "    ib.Stock_par AS stock_disponible, " +
                "    ib.Stock_caja AS stock_caja, " +
                "    ib.stock_reservado, " +
                "    ib.ubicacion_especifica, " +
                "    pv.disponible " +
                "FROM producto_variantes pv " +
                "INNER JOIN inventario_bodega ib ON ib.id_variante = pv.id_variante " +
                "LEFT JOIN tallas t ON pv.id_talla = t.id_talla " +
                "LEFT JOIN colores c ON pv.id_color = c.id_color " +
                "WHERE pv.id_producto = ? " +
                "  AND ib.id_bodega = ? " +
                "  AND ib.activo = 1 " +
                "  AND pv.disponible = 1 " +
                "  AND (ib.Stock_par > 0 OR ib.Stock_caja > 0) " +
                "ORDER BY t.numero ASC, c.nombre ASC";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1, producto.getIdProducto());
            ps.setInt(2, idBodega);
            rs = ps.executeQuery();
            while (rs.next()) {
                VarianteDTO v = mapearVarianteDesdeResultSet(rs);
                producto.agregarVariante(v);
            }
        } catch (SQLException e) {
        } finally {
            cerrarRecursos(rs, ps, null);
        }
    }

    private Integer obtenerIdBodegaDeParametros(Object[] params) {
        for (Object p : params) {
            if (p instanceof Integer) return (Integer) p;
        }
        return null;
    }

    private void cerrarRecursos(ResultSet rs, PreparedStatement ps, Connection conn) {
        try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
        try { if (ps != null) ps.close(); } catch (SQLException ignored) {}
        try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
    }
}
