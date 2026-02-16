package raven.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import raven.controlador.principal.conexion;
import raven.controlador.productos.ModelBrand;
import raven.controlador.productos.ModelCategory;
import raven.controlador.productos.ModelProduct;
import raven.modelos.ConteoInventario;
import raven.modelos.DetalleConteoInventario;
import raven.modelos.Usuario;

/**
 * DAO para operaciones relacionadas con conteos de inventario. Gestiona el
 * acceso a datos en las tablas conteos_inventario, detalles_conteo_inventario y
 * ajustes_inventario.
 */
public class ConteoInventarioDAO {

    private final conexion db;

    public ConteoInventarioDAO() {
        this.db = conexion.getInstance();
    }

    /**
     * Obtiene los conteos activos (pendientes o en proceso)
     *
     * @param tipo Tipo de conteo (cajas o pares)
     * @return Lista de conteos activos
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<ConteoInventario> obtenerConteosActivos(String tipo) throws SQLException {
        List<ConteoInventario> conteos = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            // Changed from view to direct query to ensure data consistency
            String sql = "SELECT c.*, u.nombre as responsable, " +
                    "(SELECT COUNT(*) FROM detalles_conteo_inventario d WHERE d.id_conteo = c.id_conteo) as total_productos, "
                    +
                    "(SELECT COUNT(*) FROM detalles_conteo_inventario d WHERE d.id_conteo = c.id_conteo AND d.estado != 'pendiente') as productos_contados "
                    +
                    "FROM conteos_inventario c " +
                    "INNER JOIN usuarios u ON c.id_usuario_responsable = u.id_usuario " +
                    "WHERE c.tipo = ? AND c.estado != 'completado' " +
                    "ORDER BY c.fecha_programada DESC";
            stmt = con.prepareStatement(sql);
            stmt.setString(1, tipo);
            rs = stmt.executeQuery();

            while (rs.next()) {
                ConteoInventario conteo = new ConteoInventario();
                conteo.setId(rs.getInt("id_conteo"));
                conteo.setNombre(rs.getString("nombre"));
                conteo.setFechaProgramada(rs.getTimestamp("fecha_programada"));
                conteo.setHoraProgramada(rs.getString("hora_programada"));
                conteo.setTipoConteo(rs.getString("tipo_conteo"));
                conteo.setEstado(rs.getString("estado"));

                Usuario responsable = new Usuario();
                responsable.setNombre(rs.getString("responsable"));
                conteo.setResponsable(responsable);

                conteo.setTipo(rs.getString("tipo"));
                conteo.setObservaciones(rs.getString("observaciones"));
                conteo.setTotalProductos(rs.getInt("total_productos"));
                conteo.setProductosContados(rs.getInt("productos_contados"));

                conteos.add(conteo);
            }

            return conteos;

        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Obtiene la lista de ajustes pendientes de aprobación
     *
     * @param tipo Tipo de ajuste (cajas o pares)
     * @return Lista de detalles de conteo con ajustes pendientes
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<DetalleConteoInventario> obtenerAjustesPendientes(String tipo) throws SQLException {
        List<DetalleConteoInventario> ajustes = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            // Utilizar una consulta directa en lugar de la vista
            String sql = "SELECT a.id_ajuste, a.id_detalle_conteo, a.id_producto, d.id_variante AS id_variante_ajuste, "
                    + "pv.ean as codigo_barras, t.numero as talla, c.nombre as color, p.nombre as producto, ci.tipo, "
                    + "a.tipo_ajuste, a.cantidad, a.razon, a.observaciones, "
                    + "u.nombre as creado_por, a.fecha_creacion, "
                    + "d.stock_sistema, d.stock_contado, d.diferencia "
                    + "FROM ajustes_inventario a "
                    + "JOIN detalles_conteo_inventario d ON a.id_detalle_conteo = d.id_detalle_conteo "
                    + "JOIN productos p ON a.id_producto = p.id_producto "
                    + "LEFT JOIN producto_variantes pv ON d.id_variante = pv.id_variante "
                    + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                    + "LEFT JOIN colores c ON pv.id_color = c.id_color "
                    + "JOIN conteos_inventario ci ON d.id_conteo = ci.id_conteo "
                    + "JOIN usuarios u ON a.id_usuario_creador = u.id_usuario "
                    + "WHERE a.aprobado = 0 AND ci.tipo LIKE ? "
                    + "ORDER BY a.fecha_creacion DESC";

            stmt = con.prepareStatement(sql);
            stmt.setString(1, "%" + tipo + "%");
            rs = stmt.executeQuery();

            while (rs.next()) {
                DetalleConteoInventario detalle = new DetalleConteoInventario();
                detalle.setId(rs.getInt("id_detalle_conteo"));
                detalle.setIdAjuste(rs.getInt("id_ajuste"));

                ModelProduct producto = new ModelProduct();
                producto.setProductId(rs.getInt("id_producto"));
                producto.setVariantId(rs.getInt("id_variante_ajuste"));
                producto.setBarcode(rs.getString("codigo_barras"));
                producto.setName(rs.getString("producto"));
                producto.setSize(rs.getString("talla"));
                producto.setColor(rs.getString("color"));
                detalle.setProducto(producto);

                detalle.setStockSistema(rs.getInt("stock_sistema"));
                detalle.setStockContado(rs.getInt("stock_contado"));
                detalle.setDiferencia(rs.getInt("diferencia"));
                detalle.setTipoAjuste(rs.getString("tipo_ajuste"));
                detalle.setCantidadAjuste(rs.getInt("cantidad"));
                detalle.setRazonAjuste(rs.getString("razon"));

                ajustes.add(detalle);
            }

            return ajustes;

        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Obtiene los detalles que ya han sido contados en un conteo
     *
     * @param idConteo ID del conteo
     * @return Lista de detalles contados
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<DetalleConteoInventario> obtenerDetallesContados(int idConteo) throws SQLException {
        List<DetalleConteoInventario> detalles = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            String sql = "SELECT * FROM detalles_conteo_inventario "
                    + "WHERE id_conteo = ? AND estado = 'contado'";

            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idConteo);
            rs = stmt.executeQuery();

            while (rs.next()) {
                DetalleConteoInventario detalle = new DetalleConteoInventario();
                detalle.setId(rs.getInt("id_detalle_conteo"));

                ModelProduct producto = new ModelProduct();
                producto.setProductId(rs.getInt("id_producto"));
                detalle.setProducto(producto);

                detalle.setStockSistema(rs.getInt("stock_sistema"));
                detalle.setStockContado(rs.getInt("stock_contado"));
                detalle.setDiferencia(rs.getInt("diferencia"));

                detalles.add(detalle);
            }

            return detalles;

        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Crea un nuevo conteo de inventario
     *
     * @param conteo Objeto con datos del conteo a crear
     * @return ID del conteo creado, o -1 si hubo error
     * @throws SQLException Si ocurre un error de base de datos
     */
    public int crearConteoInventario(ConteoInventario conteo) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            String sql = "INSERT INTO conteos_inventario (nombre, fecha_programada, hora_programada, "
                    + "tipo, tipo_conteo, estado, id_usuario_responsable, prioridad, observaciones, id_bodega) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            stmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, conteo.getNombre());
            stmt.setTimestamp(2, new Timestamp(conteo.getFechaProgramada().getTime()));
            stmt.setString(3, conteo.getHoraProgramada());
            stmt.setString(4, conteo.getTipo());
            stmt.setString(5, conteo.getTipoConteo());
            stmt.setString(6, conteo.getEstado());
            stmt.setInt(7, conteo.getResponsable().getId());
            stmt.setString(8, conteo.getPrioridad());
            stmt.setString(9, conteo.getObservaciones());
            stmt.setInt(10, conteo.getIdBodega());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            return -1;

        } finally {
            db.close(rs, stmt, con);
        }
    }

    public boolean eliminarConteo(int idConteo) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;

        try {
            con = db.createConnection();
            con.setAutoCommit(false); // Transactional delete

            // 1. Delete details first
            String sqlDetails = "DELETE FROM detalles_conteo_inventario WHERE id_conteo = ?";
            stmt = con.prepareStatement(sqlDetails);
            stmt.setInt(1, idConteo);
            stmt.executeUpdate();
            stmt.close();

            // 2. Delete header
            String sql = "DELETE FROM conteos_inventario WHERE id_conteo = ?";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idConteo);

            int affectedRows = stmt.executeUpdate();

            con.commit();
            return affectedRows > 0;

        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (con != null) {
                con.setAutoCommit(true);
            }
            db.close(stmt, con);
        }
    }

    /**
     * Agrega detalles (productos) a un conteo
     *
     * @param detalles Lista de detalles a agregar
     * @return true si se agregaron correctamente, false en caso contrario
     * @throws SQLException Si ocurre un error de base de datos
     */
    public boolean agregarDetallesConteo(List<DetalleConteoInventario> detalles) throws SQLException {
        if (detalles.isEmpty()) {
            return false;
        }

        Connection con = null;
        PreparedStatement stmt = null;

        try {
            con = db.createConnection();
            con.setAutoCommit(false);

            String sql = "INSERT INTO detalles_conteo_inventario (id_conteo, id_producto, id_variante, stock_sistema) "
                    + "VALUES (?, ?, ?, ?)";

            stmt = con.prepareStatement(sql);

            for (DetalleConteoInventario detalle : detalles) {
                stmt.setInt(1, detalle.getIdConteo());
                stmt.setInt(2, detalle.getProducto().getProductId());
                stmt.setInt(3, detalle.getProducto().getVariantId());
                stmt.setInt(4, detalle.getStockSistema());
                stmt.addBatch();
            }

            int[] results = stmt.executeBatch();

            // Verify all results
            for (int result : results) {
                if (result == java.sql.Statement.EXECUTE_FAILED) {
                    // Check for common error codes, but typically EXECUTE_FAILED is -3
                    con.rollback();
                    return false;
                }
                // SUCCESS_NO_INFO is -2, which is fine
            }

            con.commit();
            return true;

        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    throw new SQLException("Error al revertir transacción: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (con != null) {
                con.setAutoCommit(true);
            }
            db.close(stmt, con);
        }
    }

    /**
     * Obtiene los detalles de un conteo específico
     *
     * @param idConteo ID del conteo
     * @return Lista de detalles del conteo
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<DetalleConteoInventario> obtenerDetallesConteo(int idConteo) throws SQLException {
        List<DetalleConteoInventario> detalles = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            // JOIN with conteos_inventario to get id_bodega
            // LEFT JOIN with inventario_bodega to get specific location
            String sql = "SELECT d.*, pv.ean as codigo_barras, t.numero as talla, co.nombre as color, p.nombre as nombre_producto, "
                    + "c.nombre as nombre_categoria, m.nombre as nombre_marca, "
                    + "COALESCE(NULLIF(TRIM(ib.ubicacion_especifica), ''), CONCAT('[Gen] ', p.ubicacion)) as ubicacion_final "
                    + "FROM detalles_conteo_inventario d "
                    + "JOIN conteos_inventario ci ON d.id_conteo = ci.id_conteo "
                    + "JOIN productos p ON d.id_producto = p.id_producto "
                    + "LEFT JOIN producto_variantes pv ON d.id_variante = pv.id_variante "
                    + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                    + "LEFT JOIN colores co ON pv.id_color = co.id_color "
                    + "JOIN categorias c ON p.id_categoria = c.id_categoria "
                    + "JOIN marcas m ON p.id_marca = m.id_marca "
                    + "LEFT JOIN inventario_bodega ib ON ci.id_bodega = ib.id_bodega AND d.id_variante = ib.id_variante AND ib.activo = 1 "
                    + "WHERE d.id_conteo = ? "
                    + "ORDER BY d.estado, p.nombre";

            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idConteo);
            rs = stmt.executeQuery();

            while (rs.next()) {
                DetalleConteoInventario detalle = new DetalleConteoInventario();
                detalle.setId(rs.getInt("id_detalle_conteo"));
                detalle.setIdConteo(rs.getInt("id_conteo"));

                ModelProduct producto = new ModelProduct();
                producto.setProductId(rs.getInt("id_producto"));
                producto.setVariantId(rs.getInt("id_variante")); // Set variant ID from DB
                producto.setBarcode(rs.getString("codigo_barras"));
                producto.setName(rs.getString("nombre_producto"));
                producto.setSize(rs.getString("talla"));
                producto.setColor(rs.getString("color"));
                ModelCategory categoria = new ModelCategory();
                categoria.setName(rs.getString("nombre_categoria"));
                producto.setCategory(categoria);

                ModelBrand marca = new ModelBrand();
                marca.setName(rs.getString("nombre_marca"));
                producto.setBrand(marca);

                // Use the resolved location (defaults to product location if specific is empty)
                String ubicacionFinal = rs.getString("ubicacion_final");
                // DEBUG LOG
                System.out.println("DEBUG: Prod=" + producto.getName() + " | VarID=" + producto.getVariantId()
                        + " | UbicacionFinal='" + ubicacionFinal + "'");

                producto.setUbicacion(ubicacionFinal);

                detalle.setProducto(producto);
                detalle.setStockSistema(rs.getInt("stock_sistema"));
                detalle.setStockContado(rs.getInt("stock_contado"));
                detalle.setDiferencia(rs.getInt("diferencia"));
                detalle.setEstado(rs.getString("estado"));

                if (rs.getTimestamp("fecha_conteo") != null) {
                    detalle.setFechaConteo(new Date(rs.getTimestamp("fecha_conteo").getTime()));
                }

                detalles.add(detalle);
            }

            return detalles;

        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Actualiza el stock contado para un producto en un conteo
     *
     * @param idDetalleConteo   ID del detalle de conteo
     * @param stockContado      Cantidad contada
     * @param usuarioContadorId ID del usuario que realiza el conteo
     * @return true si se actualizó correctamente, false en caso contrario
     * @throws SQLException Si ocurre un error de base de datos
     */
    public boolean actualizarStockContado(int idDetalleConteo, int stockContado, int usuarioContadorId)
            throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;

        try {
            con = db.createConnection();
            String sql = "UPDATE detalles_conteo_inventario SET stock_contado = ?, "
                    + "diferencia = ? - stock_sistema, "
                    + "id_usuario_contador = ?, estado = 'contado', fecha_conteo = NOW() "
                    + "WHERE id_detalle_conteo = ?";

            stmt = con.prepareStatement(sql);
            stmt.setInt(1, stockContado);
            stmt.setInt(2, stockContado);
            stmt.setInt(3, usuarioContadorId);
            stmt.setInt(4, idDetalleConteo);

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;

        } finally {
            db.close(stmt, con);
        }
    }

    /**
     * Cuenta la cantidad de productos pendientes en un conteo
     *
     * @param idConteo ID del conteo
     * @return Cantidad de productos pendientes
     * @throws SQLException Si ocurre un error de base de datos
     */
    public int contarProductosPendientes(int idConteo) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            String sql = "SELECT COUNT(*) FROM detalles_conteo_inventario "
                    + "WHERE id_conteo = ? AND estado = 'pendiente'";

            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idConteo);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

            return 0;

        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Actualiza el estado de un conteo
     *
     * @param idConteo ID del conteo
     * @param estado   Nuevo estado
     * @return true si se actualizó correctamente, false en caso contrario
     * @throws SQLException Si ocurre un error de base de datos
     */
    public boolean actualizarEstadoConteo(int idConteo, String estado) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;

        try {
            con = db.createConnection();
            String sql = "UPDATE conteos_inventario SET estado = ? WHERE id_conteo = ?";

            System.out.println("DEBUG DAO: Updating conteo status. ID=" + idConteo + ", NewState=" + estado);
            stmt = con.prepareStatement(sql);
            stmt.setString(1, estado);
            stmt.setInt(2, idConteo);

            int affectedRows = stmt.executeUpdate();
            System.out.println("DEBUG DAO: Status update affected rows: " + affectedRows);
            return affectedRows > 0;

        } finally {
            db.close(stmt, con);
        }
    }

    /**
     * Obtiene los detalles de conteo que tienen diferencias
     *
     * @param idConteo ID del conteo
     * @return Lista de detalles con diferencias
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<DetalleConteoInventario> obtenerDetallesConDiferencias(int idConteo) throws SQLException {
        System.out.println("DEBUG DAO: Getting details with differences for conteo ID: " + idConteo);
        List<DetalleConteoInventario> detalles = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            String sql = "SELECT d.*, pv.ean as codigo_barras, t.numero as talla, co.nombre as color, p.nombre as nombre_producto, "
                    + "c.nombre as nombre_categoria, m.nombre as nombre_marca, p.ubicacion "
                    + "FROM detalles_conteo_inventario d "
                    + "JOIN productos p ON d.id_producto = p.id_producto "
                    + "LEFT JOIN producto_variantes pv ON d.id_variante = pv.id_variante "
                    + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                    + "LEFT JOIN colores co ON pv.id_color = co.id_color "
                    + "JOIN categorias c ON p.id_categoria = c.id_categoria "
                    + "JOIN marcas m ON p.id_marca = m.id_marca "
                    + "WHERE d.id_conteo = ? AND d.diferencia != 0 AND d.diferencia IS NOT NULL "
                    + "ORDER BY d.estado, p.nombre";

            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idConteo);
            rs = stmt.executeQuery();

            while (rs.next()) {
                DetalleConteoInventario detalle = new DetalleConteoInventario();
                detalle.setId(rs.getInt("id_detalle_conteo"));
                detalle.setIdConteo(rs.getInt("id_conteo"));

                ModelProduct producto = new ModelProduct();
                producto.setProductId(rs.getInt("id_producto"));
                producto.setVariantId(rs.getInt("id_variante"));
                producto.setBarcode(rs.getString("codigo_barras"));
                producto.setName(rs.getString("nombre_producto"));
                producto.setSize(rs.getString("talla"));
                producto.setColor(rs.getString("color"));
                ModelCategory categoria = new ModelCategory();
                categoria.setName(rs.getString("nombre_categoria"));
                producto.setCategory(categoria);

                ModelBrand marca = new ModelBrand();
                marca.setName(rs.getString("nombre_marca"));
                producto.setBrand(marca);
                producto.setUbicacion(rs.getString("ubicacion"));

                detalle.setProducto(producto);
                detalle.setStockSistema(rs.getInt("stock_sistema"));
                detalle.setStockContado(rs.getInt("stock_contado"));
                detalle.setDiferencia(rs.getInt("diferencia"));
                detalle.setEstado(rs.getString("estado"));

                if (rs.getTimestamp("fecha_conteo") != null) {
                    detalle.setFechaConteo(new Date(rs.getTimestamp("fecha_conteo").getTime()));
                }

                detalles.add(detalle);
            }

            return detalles;

        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Crea un ajuste automático para un detalle de conteo
     *
     * @param detalle   Detalle de conteo con diferencia
     * @param usuarioId ID del usuario que crea el ajuste
     * @return true si se creó correctamente, false en caso contrario
     * @throws SQLException Si ocurre un error de base de datos
     */
    public boolean crearAjusteAutomatico(DetalleConteoInventario detalle, int usuarioId) throws SQLException {
        Connection con = null;
        PreparedStatement stmt = null;

        try {
            con = db.createConnection();

            // Determinar el tipo de ajuste
            ConteoInventario conteo = obtenerTipoConteo(detalle.getIdConteo(), con);
            String tipoAjuste;

            if (conteo.getTipo().equals("cajas")) {
                tipoAjuste = detalle.getDiferencia() > 0 ? "entrada caja" : "salida caja";
            } else {
                tipoAjuste = detalle.getDiferencia() > 0 ? "entrada par" : "salida par";
            }

            // Insertar el ajuste
            String sql = "INSERT INTO ajustes_inventario (id_detalle_conteo, id_producto, id_variante, "
                    + "tipo_ajuste, cantidad, razon, observaciones, id_usuario_creador) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            stmt = con.prepareStatement(sql);
            stmt.setInt(1, detalle.getId());
            stmt.setInt(2, detalle.getProducto().getProductId());
            stmt.setInt(3, detalle.getProducto().getVariantId());
            stmt.setString(4, tipoAjuste);
            stmt.setInt(5, Math.abs(detalle.getDiferencia()));
            stmt.setString(6, "error_conteo");
            stmt.setString(7, "Ajuste automático por diferencia en conteo físico");
            stmt.setInt(8, usuarioId);

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;

        } finally {
            db.close(stmt, con);
        }
    }

    /**
     * Obtiene el tipo de un conteo (cajas o pares)
     *
     * @param idConteo ID del conteo
     * @param con      Conexión a la base de datos
     * @return Objeto ConteoInventario con el tipo
     * @throws SQLException Si ocurre un error de base de datos
     */
    private ConteoInventario obtenerTipoConteo(int idConteo, Connection con) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT tipo FROM conteos_inventario WHERE id_conteo = ?";
            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idConteo);
            rs = stmt.executeQuery();

            if (rs.next()) {
                ConteoInventario conteo = new ConteoInventario();
                conteo.setTipo(rs.getString("tipo"));
                return conteo;
            }

            throw new SQLException("No se encontró el conteo con ID: " + idConteo);

        } finally {
            db.close(rs, stmt);
        }
    }

    /**
     * Aprueba ajustes de inventario
     *
     * @param idsAjustes         Lista de IDs de ajustes a aprobar
     * @param usuarioAprobadorId ID del usuario que aprueba
     * @return true si se aprobaron correctamente, false en caso contrario
     * @throws SQLException Si ocurre un error de base de datos
     */
    /**
     * Aprueba ajustes de inventario y actualiza sus razones
     *
     * @param idsAjustes         Lista de IDs de ajustes a aprobar
     * @param razones            Mapa de ID ajuste -> razón seleccionada
     * @param usuarioAprobadorId ID del usuario que aprueba
     * @return true si se aprobaron correctamente, false en caso contrario
     * @throws SQLException Si ocurre un error de base de datos
     */
    public boolean aprobarAjustes(List<Integer> idsAjustes, Map<Integer, String> razones,
            int usuarioAprobadorId) throws SQLException {
        if (idsAjustes.isEmpty()) {
            return false;
        }

        Connection con = null;
        PreparedStatement stmtUpdate = null;

        try {
            con = db.createConnection();
            con.setAutoCommit(false);

            // SQL para actualizar la razón y aprobar el ajuste
            String sql = "UPDATE ajustes_inventario SET aprobado = 1, razon = ?, "
                    + "id_usuario_aprobador = ?, fecha_aprobacion = NOW() "
                    + "WHERE id_ajuste = ?";

            stmtUpdate = con.prepareStatement(sql);

            // Prepare statements for stock update
            String sqlInfo = "SELECT ci.id_bodega, d.id_variante, d.diferencia, ci.tipo " +
                    "FROM ajustes_inventario a " +
                    "JOIN detalles_conteo_inventario d ON a.id_detalle_conteo = d.id_detalle_conteo " +
                    "JOIN conteos_inventario ci ON d.id_conteo = ci.id_conteo " +
                    "WHERE a.id_ajuste = ?";
            PreparedStatement stmtInfo = con.prepareStatement(sqlInfo);

            String sqlUpdateStock = "UPDATE inventario_bodega SET " +
                    "Stock_par = Stock_par + ?, " +
                    "Stock_caja = Stock_caja + ?, " +
                    "fecha_ultimo_movimiento = NOW() " +
                    "WHERE id_bodega = ? AND id_variante = ?";
            PreparedStatement stmtStock = con.prepareStatement(sqlUpdateStock);

            for (Integer idAjuste : idsAjustes) {
                // 1. Update adjustment status
                String razon = razones.getOrDefault(idAjuste, "error_conteo");
                stmtUpdate.setString(1, razon);
                stmtUpdate.setInt(2, usuarioAprobadorId);
                stmtUpdate.setInt(3, idAjuste);
                stmtUpdate.addBatch();

                // 2. Get info for stock update
                stmtInfo.setInt(1, idAjuste);
                try (ResultSet rs = stmtInfo.executeQuery()) {
                    if (rs.next()) {
                        int idBodega = rs.getInt("id_bodega");
                        int idVariante = rs.getInt("id_variante");
                        int diferencia = rs.getInt("diferencia");
                        String tipo = rs.getString("tipo");

                        int diffPar = 0;
                        int diffCaja = 0;

                        if ("cajas".equalsIgnoreCase(tipo)) {
                            diffCaja = diferencia;
                        } else {
                            diffPar = diferencia;
                        }

                        // Add to batch or execute immediately?
                        // Since we have multiple statements, let's just execute update immediately for
                        // safety/simplicity in this context
                        // or add to a separate batch if we want. Let's do batch for performance if
                        // possible,
                        // but mixing batches with different statements can be tricky.
                        // Safe approach: execute stock update immediately per item.
                        stmtStock.setInt(1, diffPar);
                        stmtStock.setInt(2, diffCaja);
                        stmtStock.setInt(3, idBodega);
                        stmtStock.setInt(4, idVariante);
                        stmtStock.executeUpdate();

                        // 3. Insert audit record
                        String sqlAudit = "INSERT INTO auditoria_trazabilidad (" +
                                "tipo_evento, tipo_referencia, id_referencia, " +
                                "cantidad, id_variante, id_usuario, " +
                                "observaciones, fecha_evento) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";

                        try (PreparedStatement stmtAudit = con.prepareStatement(sqlAudit)) {
                            // Usar 'ajuste' que es el valor permitido por el ENUM de la base de datos
                            String tipoEvento = "ajuste";
                            String direccion = (diferencia > 0) ? "ENTRADA" : "SALIDA";
                            String detalle = "Ajuste inventario " + direccion + " (ID: " + idAjuste + ", Razón: "
                                    + razon + ")";

                            stmtAudit.setString(1, tipoEvento);
                            stmtAudit.setString(2, "ajustes_inventario");
                            stmtAudit.setInt(3, idAjuste);
                            stmtAudit.setInt(4, Math.abs(diferencia));
                            stmtAudit.setInt(5, idVariante);
                            stmtAudit.setInt(6, usuarioAprobadorId);
                            stmtAudit.setString(7, detalle);
                            stmtAudit.executeUpdate();
                        }
                    }
                }
            }

            int[] results = stmtUpdate.executeBatch();
            con.commit();

            // Close extra statements
            stmtInfo.close();
            stmtStock.close();

            // Verify update results
            for (int result : results) {
                if (result <= 0) {
                    return false;
                }
            }

            return true;

        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    throw new SQLException("Error al revertir transacción: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (con != null) {
                con.setAutoCommit(true);
            }
            db.close(stmtUpdate, con);
        }
    }

    /**
     * Busca detalles de conteo por término
     *
     * @param idConteo ID del conteo
     * @param termino  Término de búsqueda
     * @return Lista de detalles que coinciden con la búsqueda
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<DetalleConteoInventario> buscarDetallesConteo(int idConteo, String termino) throws SQLException {
        List<DetalleConteoInventario> detalles = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            // Modificado para usar los campos 'ean' de la tabla producto_variantes y
            // obtener talla y color de sus respectivas tablas
            String sql = "SELECT d.*, pv.ean as codigo_barras, t.numero as talla, co.nombre as color, p.nombre as nombre_producto, "
                    + "c.nombre as nombre_categoria, m.nombre as nombre_marca, p.ubicacion "
                    + "FROM detalles_conteo_inventario d "
                    + "JOIN productos p ON d.id_producto = p.id_producto "
                    + "LEFT JOIN producto_variantes pv ON d.id_variante = pv.id_variante "
                    + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                    + "LEFT JOIN colores co ON pv.id_color = co.id_color "
                    + "JOIN categorias c ON p.id_categoria = c.id_categoria "
                    + "JOIN marcas m ON p.id_marca = m.id_marca "
                    + "WHERE d.id_conteo = ? AND ("
                    + "    pv.ean LIKE ? OR "
                    + "    p.nombre LIKE ? OR "
                    + "    c.nombre LIKE ? OR "
                    + "    m.nombre LIKE ?) "
                    + "ORDER BY d.estado, p.nombre";

            stmt = con.prepareStatement(sql);
            stmt.setInt(1, idConteo);
            String param = "%" + termino + "%";
            stmt.setString(2, param);
            stmt.setString(3, param);
            stmt.setString(4, param);
            stmt.setString(5, param);
            rs = stmt.executeQuery();

            while (rs.next()) {
                DetalleConteoInventario detalle = new DetalleConteoInventario();
                detalle.setId(rs.getInt("id_detalle_conteo"));
                detalle.setIdConteo(rs.getInt("id_conteo"));

                ModelProduct producto = new ModelProduct();
                producto.setProductId(rs.getInt("id_producto"));
                producto.setVariantId(rs.getInt("id_variante"));
                producto.setBarcode(rs.getString("codigo_barras"));
                producto.setName(rs.getString("nombre_producto"));
                producto.setSize(rs.getString("talla"));
                producto.setColor(rs.getString("color"));

                ModelCategory categoria = new ModelCategory();
                categoria.setName(rs.getString("nombre_categoria"));
                producto.setCategory(categoria);

                ModelBrand marca = new ModelBrand();
                marca.setName(rs.getString("nombre_marca"));
                producto.setBrand(marca);
                producto.setUbicacion(rs.getString("ubicacion"));

                detalle.setProducto(producto);
                detalle.setStockSistema(rs.getInt("stock_sistema"));
                detalle.setStockContado(rs.getInt("stock_contado"));
                detalle.setDiferencia(rs.getInt("diferencia"));
                detalle.setEstado(rs.getString("estado"));

                if (rs.getTimestamp("fecha_conteo") != null) {
                    detalle.setFechaConteo(new Date(rs.getTimestamp("fecha_conteo").getTime()));
                }

                detalles.add(detalle);
            }

            return detalles;

        } finally {
            db.close(rs, stmt, con);
        }
    }

    /**
     * Obtiene el historial de conteos de inventario con filtros
     * 
     * @param inicio   Fecha inicio del rango
     * @param fin      Fecha fin del rango
     * @param idBodega ID de la bodega (0 para todas)
     * @return Lista de conteos que cumplen los criterios
     * @throws SQLException Si ocurre un error de base de datos
     */
    public List<ConteoInventario> obtenerHistorialConteos(Date inicio, Date fin, int idBodega) throws SQLException {
        List<ConteoInventario> conteos = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = db.createConnection();
            String sql = "SELECT c.*, u.nombre as responsable, " +
                    "(SELECT COUNT(*) FROM detalles_conteo_inventario d WHERE d.id_conteo = c.id_conteo) as total_productos, "
                    +
                    "(SELECT COUNT(*) FROM detalles_conteo_inventario d WHERE d.id_conteo = c.id_conteo AND d.estado != 'pendiente') as productos_contados "
                    +
                    "FROM conteos_inventario c " +
                    "INNER JOIN usuarios u ON c.id_usuario_responsable = u.id_usuario " +
                    "WHERE DATE(c.fecha_programada) BETWEEN ? AND ? " +
                    "AND (? = 0 OR c.id_bodega = ?) " +
                    "ORDER BY c.fecha_programada DESC";

            stmt = con.prepareStatement(sql);
            stmt.setDate(1, new java.sql.Date(inicio.getTime()));
            stmt.setDate(2, new java.sql.Date(fin.getTime()));
            stmt.setInt(3, idBodega);
            stmt.setInt(4, idBodega);

            rs = stmt.executeQuery();

            while (rs.next()) {
                ConteoInventario conteo = new ConteoInventario();
                conteo.setId(rs.getInt("id_conteo"));
                conteo.setNombre(rs.getString("nombre"));
                conteo.setFechaProgramada(rs.getTimestamp("fecha_programada"));
                conteo.setHoraProgramada(rs.getString("hora_programada"));
                conteo.setTipoConteo(rs.getString("tipo_conteo"));
                conteo.setEstado(rs.getString("estado"));
                conteo.setIdBodega(rs.getInt("id_bodega"));

                Usuario responsable = new Usuario();
                responsable.setNombre(rs.getString("responsable"));
                conteo.setResponsable(responsable);

                conteo.setTipo(rs.getString("tipo"));
                conteo.setObservaciones(rs.getString("observaciones"));
                conteo.setTotalProductos(rs.getInt("total_productos"));
                conteo.setProductosContados(rs.getInt("productos_contados"));

                conteos.add(conteo);
            }

            return conteos;

        } finally {
            db.close(rs, stmt, con);
        }
    }
}
