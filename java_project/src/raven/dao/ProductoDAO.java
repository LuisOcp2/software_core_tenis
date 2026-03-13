package raven.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import raven.controlador.comercial.ModelSupplier;
import raven.controlador.principal.conexion;
import raven.controlador.productos.ModelBrand;
import raven.controlador.productos.ModelCategory;
import raven.controlador.productos.ModelProduct;

/**
 * DAO para operaciones relacionadas con productos.
 * Proporciona acceso a las vistas de inventario para conteos.
 */
public class ProductoDAO {
    
    private final conexion db;
    
    private static String toDbGenero(String genero) {
        if (genero == null) return null;
        String g = genero.trim().toUpperCase();
        if ("NIÑO".equals(g)) return "NIÃO";
        return g;
    }
    
    public ProductoDAO() {
        this.db = conexion.getInstance();
    }
    
    /**
     * Obtiene productos desde una vista específica de inventario
     * @param vistaInventario Nombre de la vista (vista_inventario_cajas o vista_inventario_pares)
     * @return Lista de productos
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<ModelProduct> obtenerProductosPorVista(String vistaInventario) throws SQLException {
        List<ModelProduct> productos = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            con = db.createConnection();
            String sql;
            if ("vista_inventario_cajas".equals(vistaInventario)) {
                sql = "SELECT v.id_producto, v.nombre, v.codigo_barras, p.descripcion, "
                    + "c.nombre AS categoria, m.nombre AS marca, t.numero AS talla, "
                    + "v.stock, v.costo, NULL AS precio, v.ubicacion, v.activo "
                    + "FROM vista_inventario_cajas v "
                    + "JOIN productos p ON v.id_producto = p.id_producto "
                    + "LEFT JOIN categorias c ON p.id_categoria = c.id_categoria "
                    + "LEFT JOIN marcas m ON p.id_marca = m.id_marca "
                    + "LEFT JOIN producto_variantes pv ON pv.ean = v.codigo_barras "
                    + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                    + "LIMIT 100";
            } else {
                sql = "SELECT v.id_producto, v.nombre, v.codigo_barras, p.descripcion, "
                    + "c.nombre AS categoria, m.nombre AS marca, t.numero AS talla, "
                    + "v.stock, NULL AS costo, v.precio, v.ubicacion, v.activo "
                    + "FROM vista_inventario_pares v "
                    + "JOIN productos p ON v.id_producto = p.id_producto "
                    + "LEFT JOIN categorias c ON p.id_categoria = c.id_categoria "
                    + "LEFT JOIN marcas m ON p.id_marca = m.id_marca "
                    + "LEFT JOIN producto_variantes pv ON pv.ean = v.codigo_barras "
                    + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                    + "LIMIT 100";
            }
            stmt = con.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                productos.add(mapearProductoDesdeVista(rs, vistaInventario));
            }
            
            return productos;
            
        } finally {
            db.close(rs, stmt, con);
        }
    }
    
    /**
     * Busca productos por término en una vista específica de inventario
     * @param termino Término de búsqueda
     * @param vistaInventario Nombre de la vista (vista_inventario_cajas o vista_inventario_pares)
     * @return Lista de productos que coinciden con la búsqueda
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<ModelProduct> buscarProductosPorVista(String termino, String vistaInventario) throws SQLException {
        List<ModelProduct> productos = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            con = db.createConnection();
            String sql;
            if ("vista_inventario_cajas".equals(vistaInventario)) {
                sql = "SELECT v.id_producto, v.nombre, v.codigo_barras, p.descripcion, "
                    + "c.nombre AS categoria, m.nombre AS marca, t.numero AS talla, "
                    + "v.stock, v.costo, NULL AS precio, v.ubicacion, v.activo "
                    + "FROM vista_inventario_cajas v "
                    + "JOIN productos p ON v.id_producto = p.id_producto "
                    + "LEFT JOIN categorias c ON p.id_categoria = c.id_categoria "
                    + "LEFT JOIN marcas m ON p.id_marca = m.id_marca "
                    + "LEFT JOIN producto_variantes pv ON pv.ean = v.codigo_barras "
                    + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                    + "WHERE v.codigo_barras LIKE ? OR v.nombre LIKE ? OR c.nombre LIKE ? OR m.nombre LIKE ?";
            } else {
                sql = "SELECT v.id_producto, v.nombre, v.codigo_barras, p.descripcion, "
                    + "c.nombre AS categoria, m.nombre AS marca, t.numero AS talla, "
                    + "v.stock, NULL AS costo, v.precio, v.ubicacion, v.activo "
                    + "FROM vista_inventario_pares v "
                    + "JOIN productos p ON v.id_producto = p.id_producto "
                    + "LEFT JOIN categorias c ON p.id_categoria = c.id_categoria "
                    + "LEFT JOIN marcas m ON p.id_marca = m.id_marca "
                    + "LEFT JOIN producto_variantes pv ON pv.ean = v.codigo_barras "
                    + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                    + "WHERE v.codigo_barras LIKE ? OR v.nombre LIKE ? OR c.nombre LIKE ? OR m.nombre LIKE ?";
            }
            
            stmt = con.prepareStatement(sql);
            String param = "%" + termino + "%";
            stmt.setString(1, param);
            stmt.setString(2, param);
            stmt.setString(3, param);
            stmt.setString(4, param);
            
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                productos.add(mapearProductoDesdeVista(rs, vistaInventario));
            }
            
            return productos;
            
        } finally {
            db.close(rs, stmt, con);
        }
    }

    public int crearProducto(ModelProduct p) throws SQLException {
        String sql = "INSERT INTO productos (codigo_modelo, codigo_barras, nombre, descripcion, id_categoria, id_marca, id_proveedor, precio_compra, precio_venta, stock_minimo, genero, ubicacion, pares_por_caja, activo) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,TRUE)";
        try (Connection con = db.createConnection(); PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getModelCode());
            ps.setString(2, p.getBarcode());
            ps.setString(3, p.getName());
            ps.setString(4, p.getDescription());
            ps.setObject(5, p.getCategory()!=null? p.getCategory().getCategoryId(): null, java.sql.Types.INTEGER);
            ps.setObject(6, p.getBrand()!=null? p.getBrand().getBrandId(): null, java.sql.Types.INTEGER);
            ps.setObject(7, p.getSupplier()!=null? p.getSupplier().getSupplierId(): null, java.sql.Types.INTEGER);
            ps.setDouble(8, p.getPurchasePrice());
            ps.setDouble(9, p.getSalePrice());
            ps.setInt(10, p.getMinStock());
            ps.setString(11, toDbGenero(p.getGender()));
            ps.setString(12, p.getUbicacion());
            ps.setInt(13, p.getPairsPerBox());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { if (rs.next()) return rs.getInt(1); }
        }
        return 0;
    }

    public int crearVarianteConInventario(ModelBrand brand, ModelCategory category, ModelProduct product, raven.controlador.productos.ModelProductVariant v, int idBodega, int stockPar, int stockCaja) throws SQLException {
        String sqlV = "INSERT INTO producto_variantes (id_producto, id_talla, id_color, sku, ean, precio_compra, precio_venta, stock_minimo_variante, disponible) VALUES (?,?,?,?,?,?,?,?,TRUE)";
        try (Connection con = db.createConnection(); PreparedStatement psV = con.prepareStatement(sqlV, PreparedStatement.RETURN_GENERATED_KEYS)) {
            psV.setInt(1, product.getProductId());
            psV.setInt(2, v.getSizeId());
            psV.setInt(3, v.getColorId());
            psV.setString(4, v.getSku());
            psV.setString(5, v.getEan());
            psV.setBigDecimal(6, v.getPurchasePrice()!=null? java.math.BigDecimal.valueOf(v.getPurchasePrice()): java.math.BigDecimal.ZERO);
            psV.setBigDecimal(7, v.getSalePrice()!=null? java.math.BigDecimal.valueOf(v.getSalePrice()): java.math.BigDecimal.ZERO);
            psV.setInt(8, v.getMinStock()!=null? v.getMinStock(): 0);
            psV.executeUpdate();
            int idVariante = 0;
            try (ResultSet rs = psV.getGeneratedKeys()) { if (rs.next()) idVariante = rs.getInt(1); }
            if (idVariante == 0) return 0;
            String sqlI = "INSERT INTO inventario_bodega (id_bodega, id_variante, Stock_par, Stock_caja, stock_reservado, fecha_ultimo_movimiento, activo) VALUES (?,?,?,?,0,NOW(),1)";
            try (PreparedStatement psI = con.prepareStatement(sqlI)) { psI.setInt(1, idBodega); psI.setInt(2, idVariante); psI.setInt(3, Math.max(0, stockPar)); psI.setInt(4, Math.max(0, stockCaja)); psI.executeUpdate(); }

            // Registrar movimientos en inventario_movimientos
            Integer idUsuario = null;
            try {
                raven.clases.admin.UserSession us = raven.clases.admin.UserSession.getInstance();
                if (us != null && us.getCurrentUser() != null) {
                    idUsuario = us.getCurrentUser().getIdUsuario();
                }
            } catch (Exception ignore) {}

            // Registrar movimiento de entrada para pares (si hay stock de pares)
            if (stockPar > 0) {
                String sqlMovPar = "INSERT INTO inventario_movimientos (id_producto, id_variante, tipo_movimiento, cantidad, cantidad_pares, fecha_movimiento, tipo_referencia, id_usuario, observaciones) VALUES (?,?,?,?,?,NOW(),?,?,?)";
                try (PreparedStatement psMov = con.prepareStatement(sqlMovPar)) {
                    psMov.setInt(1, product.getProductId());
                    psMov.setInt(2, idVariante);
                    psMov.setString(3, "entrada par");
                    psMov.setInt(4, stockPar);
                    psMov.setInt(5, stockPar);
                    psMov.setString(6, "creacion_variante");
                    if (idUsuario != null) {
                        psMov.setInt(7, idUsuario);
                    } else {
                        psMov.setNull(7, java.sql.Types.INTEGER);
                    }
                    psMov.setString(8, "Inventario inicial al crear variante");
                    psMov.executeUpdate();
                }
            }

            // Registrar movimiento de entrada para cajas (si hay stock de cajas)
            if (stockCaja > 0) {
                String sqlMovCaja = "INSERT INTO inventario_movimientos (id_producto, id_variante, tipo_movimiento, cantidad, cantidad_pares, fecha_movimiento, tipo_referencia, id_usuario, observaciones) VALUES (?,?,?,?,?,NOW(),?,?,?)";
                try (PreparedStatement psMov = con.prepareStatement(sqlMovCaja)) {
                    psMov.setInt(1, product.getProductId());
                    psMov.setInt(2, idVariante);
                    psMov.setString(3, "entrada caja");
                    psMov.setInt(4, stockCaja);
                    psMov.setNull(5, java.sql.Types.INTEGER);
                    psMov.setString(6, "creacion_variante");
                    if (idUsuario != null) {
                        psMov.setInt(7, idUsuario);
                    } else {
                        psMov.setNull(7, java.sql.Types.INTEGER);
                    }
                    psMov.setString(8, "Inventario inicial al crear variante");
                    psMov.executeUpdate();
                }
            }

            return idVariante;
        }
    }

    public boolean actualizarInventarioVariante(int idVariante, Integer idBodega, int deltaPar, int deltaCaja) throws SQLException {
        String sql = "UPDATE inventario_bodega SET Stock_par=GREATEST(0, Stock_par+?), Stock_caja=GREATEST(0, Stock_caja+?), fecha_ultimo_movimiento=NOW() WHERE id_variante=?" + (idBodega!=null? " AND id_bodega=?" : "");
        try (Connection con = db.createConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            int idx=1; ps.setInt(idx++, deltaPar); ps.setInt(idx++, deltaCaja); ps.setInt(idx++, idVariante); if (idBodega!=null) ps.setInt(idx++, idBodega);
            return ps.executeUpdate() > 0;
        }
    }

    public int[] obtenerStockConsolidadoProducto(int idProducto, Integer idBodega) throws SQLException {
        String base = "SELECT COALESCE(SUM(ib.Stock_par),0) AS pares, COALESCE(SUM(ib.Stock_caja),0) AS cajas FROM inventario_bodega ib INNER JOIN producto_variantes pv ON ib.id_variante=pv.id_variante WHERE pv.id_producto=? AND ib.activo=1";
        String sql = idBodega!=null? base+" AND ib.id_bodega=?" : base;
        try (Connection con = db.createConnection(); PreparedStatement ps = con.prepareStatement(sql)) { int idx=1; ps.setInt(idx++, idProducto); if (idBodega!=null) ps.setInt(idx++, idBodega); try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return new int[]{rs.getInt(1), rs.getInt(2)}; } }
        return new int[]{0,0};
    }

    public boolean eliminarProductoYInventario(int idProducto) throws SQLException {
        try (Connection con = db.createConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps1 = con.prepareStatement("UPDATE productos SET activo=FALSE WHERE id_producto=?"); PreparedStatement ps2 = con.prepareStatement("UPDATE inventario_bodega ib INNER JOIN producto_variantes pv ON ib.id_variante=pv.id_variante SET ib.activo=0 WHERE pv.id_producto=?")) {
                ps1.setInt(1, idProducto); ps1.executeUpdate(); ps2.setInt(1, idProducto); ps2.executeUpdate();
                con.commit(); return true;
            } catch (SQLException e) { con.rollback(); throw e; }
        }
    }
    
    /**
     * Mapea un ResultSet a un objeto ModelProduct según la vista
     * @param rs ResultSet con datos del producto
     * @param vistaInventario Nombre de la vista usada
     * @return Objeto ModelProduct mapeado
     * @throws SQLException Si ocurre un error de base de datos
     */
    private ModelProduct mapearProductoDesdeVista(ResultSet rs, String vistaInventario) throws SQLException {
        ModelProduct producto = new ModelProduct();
        producto.setProductId(rs.getInt("id_producto"));
        producto.setBarcode(rs.getString("codigo_barras"));
        producto.setName(rs.getString("nombre"));
        producto.setDescription(rs.getString("descripcion"));
        // Algunas bases de datos o vistas podrían no exponer la columna 'talla'.
        // Mapeamos de forma defensiva para evitar errores "Column 'talla' not found".
        String tallaValor = null;
        try {
            tallaValor = rs.getString("talla");
        } catch (SQLException ignore) {
            tallaValor = null;
        }
        producto.setSize(tallaValor != null ? tallaValor : "");
        ModelCategory categoria = new ModelCategory();
        categoria.setName(rs.getString("categoria"));
        producto.setCategory(categoria);
        
        ModelBrand marca = new ModelBrand();
        marca.setName(rs.getString("marca"));
        producto.setBrand(marca);
        
        if (vistaInventario.equals("vista_inventario_cajas")) {
            producto.setBoxesStock(rs.getInt("stock"));
            producto.setPurchasePrice(rs.getDouble("costo"));
        } else {
            producto.setPairsStock(rs.getInt("stock"));
            producto.setSalePrice(rs.getDouble("precio"));
        }
        
        producto.setActive(rs.getBoolean("activo"));
        
        return producto;
    }
    
    // No se expone públicamente; utilitario por si se requiere en el futuro
    @SuppressWarnings("unused")
    private boolean hasColumn(ResultSet rs, String columnLabel) throws SQLException {
        try {
            rs.findColumn(columnLabel);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
