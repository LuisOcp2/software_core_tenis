/*
 * TraspasoService - VERSIÃ“N CORREGIDA
 * Corrige manejo de stock usando inventario_bodega y producto_variantes
 */
package raven.clases.productos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import raven.controlador.principal.conexion;
import raven.utils.NotificacionesService;
import raven.clases.admin.UserSession;

public class TraspasoService {

    public List<Bodega> obtenerBodegasActivas() throws SQLException {
        List<Bodega> bodegas = new ArrayList<>();
        String sql = "SELECT id_bodega, codigo, nombre, direccion, telefono, responsable, " +
                "tipo, capacidad_maxima, activa FROM bodegas WHERE activa = 1 ORDER BY nombre";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Bodega bodega = new Bodega();
                bodega.setIdBodega(rs.getInt("id_bodega"));
                bodega.setCodigo(rs.getString("codigo"));
                bodega.setNombre(rs.getString("nombre"));
                bodega.setDireccion(rs.getString("direccion"));
                bodega.setTelefono(rs.getString("telefono"));
                bodega.setResponsable(rs.getString("responsable"));
                bodega.setTipo(rs.getString("tipo"));

                // CORRECCIÃ“N: Manejar capacidad_maxima null
                Object capacidadObj = rs.getObject("capacidad_maxima");
                if (capacidadObj != null) {
                    bodega.setCapacidadMaxima(rs.getInt("capacidad_maxima"));
                }

                bodega.setActiva(rs.getBoolean("activa"));
                bodegas.add(bodega);
            }
        } finally {
            if (rs != null)
                rs.close();
            if (stmt != null)
                stmt.close();
            if (conn != null)
                conn.close();
        }
        return bodegas;
    }

    /**
     * Obtener el nombre de una bodega por su ID
     */
    public String obtenerNombreBodegaPorId(Integer idBodega) throws SQLException {
        String sql = "SELECT nombre FROM bodegas WHERE id_bodega = ? AND activa = 1";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, idBodega);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("nombre");
            }
        } finally {
            if (rs != null)
                rs.close();
            if (stmt != null)
                stmt.close();
            if (conn != null)
                conn.close();
        }
        return null;
    }

    public boolean existeNumeroTraspaso(String numero) throws SQLException {
        String sql = "SELECT 1 FROM traspasos WHERE numero_traspaso = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, numero);
            rs = stmt.executeQuery();
            return rs.next();
        } finally {
            if (rs != null)
                rs.close();
            if (stmt != null)
                stmt.close();
            if (conn != null)
                conn.close();
        }
    }

    public List<Object[]> buscarTraspasosPorProducto(String busqueda) throws SQLException {
        List<Object[]> resultados = new ArrayList<>();
        String sql = "SELECT t.numero_traspaso, t.fecha_envio, bo.nombre AS origen, bd.nombre AS destino, " +
                "p.nombre AS producto, COALESCE(pv.ean, 'N/A') AS ean, td.cantidad_enviada " +
                "FROM traspaso_detalles td " +
                "JOIN traspasos t ON td.id_traspaso = t.id_traspaso " +
                "JOIN productos p ON td.id_producto = p.id_producto " +
                "LEFT JOIN producto_variantes pv ON td.id_variante = pv.id_variante " +
                "JOIN bodegas bo ON t.id_bodega_origen = bo.id_bodega " +
                "JOIN bodegas bd ON t.id_bodega_destino = bd.id_bodega " +
                "WHERE (p.nombre LIKE ? OR pv.ean LIKE ? OR p.codigo_modelo LIKE ?) " +
                "ORDER BY t.fecha_envio DESC";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);
            String parametro = "%" + busqueda + "%";
            stmt.setString(1, parametro);
            stmt.setString(2, parametro);
            stmt.setString(3, parametro);

            rs = stmt.executeQuery();
            while (rs.next()) {
                resultados.add(new Object[] {
                        rs.getString("numero_traspaso"),
                        rs.getTimestamp("fecha_envio"),
                        rs.getString("origen"),
                        rs.getString("destino"),
                        rs.getString("producto"),
                        rs.getString("ean"),
                        rs.getInt("cantidad_enviada")
                });
            }
        } finally {
            if (rs != null)
                rs.close();
            if (stmt != null)
                stmt.close();
            if (conn != null)
                conn.close();
        }
        return resultados;
    }

    public String generarNumeroTraspaso() throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(numero_traspaso, 3) AS UNSIGNED)), 0) + 1 as next_number " +
                "FROM traspasos WHERE numero_traspaso LIKE 'TR%'";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            if (rs.next()) {
                int nextNumber = rs.getInt("next_number");
                return String.format("TR%06d", nextNumber);
            }
        } finally {
            if (rs != null)
                rs.close();
            if (stmt != null)
                stmt.close();
            if (conn != null)
                conn.close();
        }
        return "TR000001";
    }

    /**
     * MÃ‰TODO CORREGIDO - Valida stock usando inventario_bodega primero, luego
     * producto_variantes
     */
    public boolean validarStockBodega(Integer idBodega, Integer idProducto, Integer idVariante,
            String tipo, int cantidad) throws SQLException {

        // ESTRATEGIA DUAL: Intentar inventario_bodega primero, luego producto_variantes
        int stockDisponible = obtenerStockInventarioBodega(idBodega, idProducto, idVariante, tipo);

        if (stockDisponible == 0) {
            // Fallback: usar producto_variantes (stock general)
            stockDisponible = obtenerStockProductoVariantes(idProducto, idVariante, tipo);
            System.out.println("ðŸ“¦ Usando stock fallback desde producto_variantes: " + stockDisponible);
        } else {
            System.out.println("ðŸ“¦ Usando stock desde inventario_bodega: " + stockDisponible);
        }

        System.out.println("ðŸ” Stock disponible: " + stockDisponible + " " + tipo);
        System.out.println("   Cantidad solicitada: " + cantidad + " " + tipo);

        return stockDisponible >= cantidad;
    }

    /**
     * MÃ‰TODO NUEVO - Obtener stock desde inventario_bodega
     */
    private int obtenerStockInventarioBodega(Integer idBodega, Integer idProducto, Integer idVariante, String tipo)
            throws SQLException {
        String columna = "caja".equalsIgnoreCase(tipo) ? "COALESCE(ib.Stock_caja,0)" : "COALESCE(ib.Stock_par,0)";
        String sql;
        if (idVariante != null) {
            sql = "SELECT " + columna + " AS stock FROM inventario_bodega ib "
                    + "WHERE ib.id_bodega = ? AND ib.id_variante = ? AND ib.activo = 1";
        } else {
            sql = "SELECT COALESCE(SUM(" + columna + "),0) AS stock FROM inventario_bodega ib "
                    + "INNER JOIN producto_variantes pv ON ib.id_variante = pv.id_variante "
                    + "WHERE ib.id_bodega = ? AND pv.id_producto = ? AND ib.activo = 1";
        }
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);
            int idx = 1;
            stmt.setInt(idx++, idBodega);
            if (idVariante != null) {
                stmt.setInt(idx++, idVariante);
            } else {
                stmt.setInt(idx++, idProducto);
            }
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("stock");
            }
        } finally {
            if (rs != null)
                rs.close();
            if (stmt != null)
                stmt.close();
            if (conn != null)
                conn.close();
        }
        return 0;
    }

    /**
     * MÃ‰TODO NUEVO - Obtener stock desde producto_variantes (fallback)
     */
    private int obtenerStockProductoVariantes(Integer idProducto, Integer idVariante, String tipo) throws SQLException {
        // ... implementation existing ...
        String columna = "caja".equalsIgnoreCase(tipo) ? "COALESCE(ib.Stock_caja,0)" : "COALESCE(ib.Stock_par,0)";
        String sql;
        if (idVariante != null) {
            sql = "SELECT COALESCE(SUM(" + columna + "),0) AS stock " +
                    "FROM inventario_bodega ib " +
                    "WHERE ib.id_variante = ? AND ib.activo = 1";
        } else {
            sql = "SELECT COALESCE(SUM(" + columna + "),0) AS stock " +
                    "FROM inventario_bodega ib " +
                    "INNER JOIN producto_variantes pv ON ib.id_variante = pv.id_variante " +
                    "WHERE pv.id_producto = ? AND ib.activo = 1";
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);
            if (idVariante != null) {
                stmt.setInt(1, idVariante);
            } else {
                stmt.setInt(1, idProducto);
            }
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("stock");
            }
        } finally {
            if (rs != null)
                rs.close();
            if (stmt != null)
                stmt.close();
            if (conn != null)
                conn.close();
        }
        return 0;
    }

    /**
     * Helper to get price
     */
    private java.math.BigDecimal obtenerPrecioVenta(Integer idProducto, Integer idVariante) {
        String sql = "SELECT precio_venta FROM producto_variantes WHERE id_variante = ?";
        if (idVariante == null) {
            // Fallback or generic product price if needed, but for now assuming variant
            // exists or 0
            // actually if variant is null, we might check products table but usually
            // variants handles price
            return java.math.BigDecimal.ZERO;
        }

        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idVariante);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("precio_venta");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting price: " + e.getMessage());
        }
        return java.math.BigDecimal.ZERO;
    }

    /**
     * MÉTODO CORREGIDO - Guarda traspaso con manejo mejorado de variantes
     * MODIFICADO: Integra validación de permisos al crear traspaso
     */
    public int guardarTraspaso(TraspasoDatos traspasoData, int idUsuario) throws SQLException {
        // ===========================================================================
        // VALIDACION DE PERMISOS PARA CREAR TRASPASO
        // ===========================================================================
        TraspasoPermissionValidator validator = new TraspasoPermissionValidator();
        if (!validator.canCreate()) {
            throw new SQLException("No tienes permiso para crear traspasos");
        }

        Connection conn = null;
        PreparedStatement stmtTraspaso = null;
        PreparedStatement stmtDetalle = null;
        int idTraspaso = -1;

        try {
            conn = conexion.getInstance().createConnection();
            conn.setAutoCommit(false);

            // Determinar usuario solicitante v\u00e1lido
            int idSolicita = (traspasoData.getIdUsuarioSolicita() != null && traspasoData.getIdUsuarioSolicita() > 0)
                    ? traspasoData.getIdUsuarioSolicita()
                    : idUsuario;
            idSolicita = ensureUsuarioIdExists(conn, idSolicita);

            String sqlTraspaso = "INSERT INTO traspasos (numero_traspaso, id_bodega_origen, id_bodega_destino, " +
                    "id_usuario_solicita, fecha_solicitud, estado, motivo, observaciones, total_productos, monto_total) "
                    +
                    "VALUES (?, ?, ?, ?, NOW(), 'pendiente', ?, ?, ?, ?)";

            stmtTraspaso = conn.prepareStatement(sqlTraspaso, Statement.RETURN_GENERATED_KEYS);
            stmtTraspaso.setString(1, traspasoData.getNumeroTraspaso());
            stmtTraspaso.setInt(2, traspasoData.getIdBodegaOrigen());
            stmtTraspaso.setInt(3, traspasoData.getIdBodegaDestino());
            stmtTraspaso.setInt(4, idSolicita);
            String motivoFinal = traspasoData.getMotivoTraspaso();
            if (motivoFinal == null || motivoFinal.trim().isEmpty()) {
                motivoFinal = traspasoData.getTipoTraspaso();
            }
            stmtTraspaso.setString(5, motivoFinal);
            stmtTraspaso.setString(6, traspasoData.getObservaciones());
            stmtTraspaso.setInt(7, traspasoData.getProductos().size());

            // Calculate total first if needed, but TraspasoDatos should have it
            traspasoData.calcularTotales();
            stmtTraspaso.setBigDecimal(8, traspasoData.getMontoTotal());

            System.out.println("ðŸ§¾ Insert traspaso: usuario_solicita=" + idSolicita + ", numero="
                    + traspasoData.getNumeroTraspaso());
            int filasAfectadas = stmtTraspaso.executeUpdate();

            if (filasAfectadas > 0) {
                ResultSet rsKeys = stmtTraspaso.getGeneratedKeys();
                if (rsKeys.next()) {
                    idTraspaso = rsKeys.getInt(1);
                }
                rsKeys.close();

                // CORRECCIÓN: Insertar detalles con mejor manejo de variantes NULL y PRECIOS
                String sqlDetalle = "INSERT INTO traspaso_detalles (id_traspaso, id_producto, id_variante, " +
                        "cantidad_solicitada, Tipo, observaciones, estado_detalle, precio_unitario, subtotal) VALUES (?, ?, ?, ?, ?, ?, 'pendiente', ?, ?)";

                stmtDetalle = conn.prepareStatement(sqlDetalle);

                for (ProductoTraspasoItem producto : traspasoData.getProductos()) {
                    System.out.println("💾 Guardando producto detalle:");
                    System.out.println("   - ID Traspaso: " + idTraspaso);
                    System.out.println("   - ID Producto: " + producto.getIdProducto());

                    // Fetch price if missing
                    if (producto.getPrecioUnitario() == null
                            || producto.getPrecioUnitario().compareTo(java.math.BigDecimal.ZERO) == 0) {
                        java.math.BigDecimal price = obtenerPrecioVenta(producto.getIdProducto(),
                                producto.getIdVariante());
                        producto.setPrecioUnitario(price);
                        producto.calcularSubtotal();
                    }

                    stmtDetalle.setInt(1, idTraspaso);
                    stmtDetalle.setInt(2, producto.getIdProducto());

                    if (producto.getIdVariante() != null && producto.getIdVariante() > 0) {
                        stmtDetalle.setInt(3, producto.getIdVariante());
                    } else {
                        stmtDetalle.setNull(3, Types.INTEGER);
                    }

                    stmtDetalle.setInt(4, producto.getCantidadSolicitada());
                    String tipo = producto.getTipo();
                    if (tipo == null || tipo.trim().isEmpty())
                        tipo = "par";
                    tipo = tipo.trim().toLowerCase();
                    if (!"par".equals(tipo) && !"caja".equals(tipo))
                        tipo = "par";
                    stmtDetalle.setString(5, tipo);
                    stmtDetalle.setString(6, producto.getObservaciones());

                    // New columns
                    stmtDetalle.setBigDecimal(7, producto.getPrecioUnitario());
                    stmtDetalle.setBigDecimal(8, producto.getSubtotal());

                    stmtDetalle.addBatch();
                }

                int[] resultados = stmtDetalle.executeBatch();
                System.out.println("Traspaso: detalles guardados " + resultados.length + " productos");

                conn.commit();
                System.out.println("Traspaso guardado exitosamente con ID: " + idTraspaso);

                // Notificar solicitud de traspaso a la bodega destino
                try {
                    if (idTraspaso > 0) {
                        new NotificacionesService().notificarTraspasoEvento(
                                idTraspaso,
                                "solicitud",
                                "Solicitud de Traspaso",
                                "Nuevo traspaso " + traspasoData.getNumeroTraspaso() + " pendiente");
                    }
                } catch (Exception notifyEx) {
                    System.err.println("No se pudo notificar el traspaso recién creado: " + notifyEx.getMessage());
                }
            }

        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            System.err.println("âŒ Error guardando traspaso: " + e.getMessage());
            throw e;
        } finally {
            if (stmtDetalle != null)
                stmtDetalle.close();
            if (stmtTraspaso != null)
                stmtTraspaso.close();
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }

        return idTraspaso;
    }

    private int ensureUsuarioIdExists(Connection con, int candidate) throws SQLException {
        if (candidate > 0 && existsUsuario(con, candidate))
            return candidate;
        // Intentar tomar usuario de sesiÃ³n
        int sid = raven.controlador.admin.SessionManager.getInstance().getCurrentUserId();
        if (sid > 0 && existsUsuario(con, sid))
            return sid;
        // Fallback: primer usuario activo
        try (PreparedStatement ps = con
                .prepareStatement("SELECT id_usuario FROM usuarios WHERE activo = 1 ORDER BY id_usuario ASC LIMIT 1");
                ResultSet rs = ps.executeQuery()) {
            if (rs.next())
                return rs.getInt(1);
        }
        throw new SQLException("Usuario solicitante invÃ¡lido o inexistente");
    }

    private boolean existsUsuario(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con
                .prepareStatement("SELECT 1 FROM usuarios WHERE id_usuario = ? AND activo = 1")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * MÃ‰TODO NUEVO - Actualizar stock en inventario_bodega tras confirmar traspaso
     */
    private void actualizarStockInventario(TraspasoDatos traspasoData, Connection conn) throws SQLException {
        String sqlUpdatePar = "UPDATE inventario_bodega SET Stock_par = Stock_par - ?, fecha_ultimo_movimiento = NOW() "
                +
                "WHERE id_bodega = ? AND id_variante = ? AND activo = 1";
        String sqlUpdateCaja = "UPDATE inventario_bodega SET Stock_caja = Stock_caja - ?, fecha_ultimo_movimiento = NOW() "
                +
                "WHERE id_bodega = ? AND id_variante = ? AND activo = 1";

        String sqlInsert = "INSERT INTO inventario_movimientos (id_producto, id_variante, tipo_movimiento, " +
                "cantidad, fecha_movimiento, id_referencia, tipo_referencia, id_usuario, observaciones) " +
                "VALUES (?, ?, ?, ?, NOW(), ?, 'traspaso', ?, ?)";

        PreparedStatement stmtUpdatePar = null;
        PreparedStatement stmtUpdateCaja = null;
        PreparedStatement stmtInsert = null;

        try {
            stmtUpdatePar = conn.prepareStatement(sqlUpdatePar);
            stmtUpdateCaja = conn.prepareStatement(sqlUpdateCaja);
            stmtInsert = conn.prepareStatement(sqlInsert);

            for (ProductoTraspasoItem producto : traspasoData.getProductos()) {
                if (producto.getIdVariante() != null) {
                    boolean esPar = "par".equalsIgnoreCase(producto.getTipo());
                    PreparedStatement upd = esPar ? stmtUpdatePar : stmtUpdateCaja;
                    upd.setInt(1, producto.getCantidadSolicitada());
                    upd.setInt(2, traspasoData.getIdBodegaOrigen());
                    upd.setInt(3, producto.getIdVariante());
                    upd.addBatch();

                    // Registrar movimiento con tipo dinÃ¡mico
                    stmtInsert.setInt(1, producto.getIdProducto());
                    stmtInsert.setInt(2, producto.getIdVariante());
                    stmtInsert.setString(3, esPar ? "salida par" : "salida caja");
                    stmtInsert.setInt(4, producto.getCantidadSolicitada());
                    stmtInsert.setInt(5, 0); // ID de referencia (ID del traspaso si se requiere)
                    stmtInsert.setInt(6, traspasoData.getIdUsuarioSolicita());
                    stmtInsert.setString(7, "Traspaso a " + traspasoData.getNombreBodegaDestino());
                    stmtInsert.addBatch();
                }
            }

            if (stmtUpdatePar != null)
                stmtUpdatePar.executeBatch();
            if (stmtUpdateCaja != null)
                stmtUpdateCaja.executeBatch();
            stmtInsert.executeBatch();

            System.out.println("âœ… Stock de inventario actualizado");

        } finally {
            if (stmtUpdatePar != null)
                stmtUpdatePar.close();
            if (stmtUpdateCaja != null)
                stmtUpdateCaja.close();
            if (stmtInsert != null)
                stmtInsert.close();
        }
    }

    /**
     * Obtener lista de traspasos con filtros
     */
    public List<Object[]> obtenerTraspasos(String estado, Integer idBodegaOrigen,
            Integer idBodegaDestino, String fechaDesde,
            String fechaHasta) throws SQLException {
        List<Object[]> traspasos = new ArrayList<>();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT t.numero_traspaso, bo.nombre as bodega_origen, bd.nombre as bodega_destino, ");
        sql.append("DATE_FORMAT(t.fecha_solicitud, '%Y-%m-%d') as fecha_solicitud, ");
        sql.append("t.estado, t.total_productos, t.id_traspaso, t.monto_total ");
        sql.append("FROM traspasos t ");
        sql.append("INNER JOIN bodegas bo ON t.id_bodega_origen = bo.id_bodega ");
        sql.append("INNER JOIN bodegas bd ON t.id_bodega_destino = bd.id_bodega ");
        sql.append("WHERE 1=1 ");

        List<Object> parametros = new ArrayList<>();

        if (estado != null && !estado.isEmpty() && !"Seleccionar".equals(estado)) {
            sql.append("AND t.estado = ? ");
            parametros.add(estado);
        }

        if (idBodegaOrigen != null) {
            sql.append("AND t.id_bodega_origen = ? ");
            parametros.add(idBodegaOrigen);
        }

        if (idBodegaDestino != null) {
            sql.append("AND t.id_bodega_destino = ? ");
            parametros.add(idBodegaDestino);
        }

        if (fechaDesde != null && !fechaDesde.isEmpty()) {
            sql.append("AND DATE(t.fecha_solicitud) >= ? ");
            parametros.add(fechaDesde);
        }

        if (fechaHasta != null && !fechaHasta.isEmpty()) {
            sql.append("AND DATE(t.fecha_solicitud) <= ? ");
            parametros.add(fechaHasta);
        }

        sql.append("ORDER BY t.fecha_solicitud DESC, t.id_traspaso DESC LIMIT 100");

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql.toString());

            for (int i = 0; i < parametros.size(); i++) {
                stmt.setObject(i + 1, parametros.get(i));
            }

            rs = stmt.executeQuery();

            while (rs.next()) {
                Object[] fila = new Object[8];
                fila[0] = rs.getString("numero_traspaso");
                fila[1] = rs.getString("bodega_origen");
                fila[2] = rs.getString("bodega_destino");
                fila[3] = rs.getString("fecha_solicitud");
                fila[4] = rs.getString("estado");
                fila[5] = rs.getInt("total_productos");

                // Format amount
                java.math.BigDecimal monto = rs.getBigDecimal("monto_total");
                if (monto == null)
                    monto = java.math.BigDecimal.ZERO;
                fila[6] = java.text.NumberFormat.getCurrencyInstance().format(monto);

                fila[7] = "Ver/Editar"; // Moved to last index
                traspasos.add(fila);
            }

        } finally {
            if (rs != null)
                rs.close();
            if (stmt != null)
                stmt.close();
            if (conn != null)
                conn.close();
        }

        return traspasos;
    }

    /**
     * Cambiar estado de un traspaso
     * MODIFICADO: Integra validaciones de permisos según el estado destino
     */
    public boolean cambiarEstadoTraspaso(String numeroTraspaso, String nuevoEstado,
            int idUsuario) throws SQLException {
        // ===========================================================================
        // VALIDACION DE PERMISOS SEGUN ESTADO DESTINO
        // ===========================================================================
        TraspasoPermissionValidator validator = new TraspasoPermissionValidator();

        // Obtener estado actual y bodegas del traspaso
        String estadoActual = null;
        Integer bodegaOrigen = null;
        Integer bodegaDestino = null;

        String sqlConsulta = "SELECT estado, id_bodega_origen, id_bodega_destino FROM traspasos WHERE numero_traspaso = ?";
        try (Connection connConsulta = conexion.getInstance().createConnection();
                PreparedStatement psConsulta = connConsulta.prepareStatement(sqlConsulta)) {
            psConsulta.setString(1, numeroTraspaso);
            try (ResultSet rs = psConsulta.executeQuery()) {
                if (rs.next()) {
                    estadoActual = rs.getString("estado");
                    bodegaOrigen = rs.getInt("id_bodega_origen");
                    bodegaDestino = rs.getInt("id_bodega_destino");
                } else {
                    throw new SQLException("Traspaso no encontrado: " + numeroTraspaso);
                }
            }
        }

        // Validar permiso según el nuevo estado
        boolean tienePermiso = false;
        String mensajeError = "";

        switch (nuevoEstado.toLowerCase()) {
            case "autorizado":
                tienePermiso = validator.canAuthorize(estadoActual, bodegaOrigen);
                mensajeError = "No tienes permiso para autorizar este traspaso. Debes pertenecer a la bodega origen.";
                break;

            case "en_transito":
            case "enviado":
                tienePermiso = validator.canSend(estadoActual, bodegaOrigen);
                mensajeError = "No tienes permiso para enviar este traspaso. Debes pertenecer a la bodega origen.";
                break;

            case "recibido":
                tienePermiso = validator.canReceive(estadoActual, bodegaDestino);
                mensajeError = "No tienes permiso para recibir este traspaso. Debes pertenecer a la bodega destino.";
                break;

            case "cancelado":
                tienePermiso = validator.canCancel(estadoActual, bodegaOrigen, bodegaDestino);
                mensajeError = "No tienes permiso para cancelar este traspaso.";
                break;

            default:
                throw new SQLException("Estado no válido: " + nuevoEstado);
        }

        if (!tienePermiso) {
            throw new SQLException(mensajeError);
        }
        String sql = "UPDATE traspasos SET estado = ?, fecha_autorizacion = NOW(), " +
                "id_usuario_autoriza = ? WHERE numero_traspaso = ?";

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, idUsuario);
            stmt.setString(3, numeroTraspaso);

            int filasAfectadas = stmt.executeUpdate();
            return filasAfectadas > 0;

        } finally {
            if (stmt != null)
                stmt.close();
            if (conn != null)
                conn.close();
        }
    }

    public boolean actualizarTraspaso(TraspasoDatos traspasoData, String numeroTraspasoOriginal) throws SQLException {
        Connection conn = null;
        PreparedStatement stmtTraspaso = null;
        PreparedStatement stmtDeleteDetalles = null;
        PreparedStatement stmtDetalle = null;

        try {
            conn = conexion.getInstance().createConnection();
            conn.setAutoCommit(false);

            System.out.println("ðŸ”„ Actualizando traspaso: " + numeroTraspasoOriginal);

            // 1. Actualizar datos principales del traspaso
            String sqlUpdateTraspaso = "UPDATE traspasos SET " +
                    "id_bodega_origen = ?, id_bodega_destino = ?, " +
                    "motivo = ?, observaciones = ?, total_productos = ?, monto_total = ? " +
                    "WHERE numero_traspaso = ?";

            traspasoData.calcularTotales();

            stmtTraspaso = conn.prepareStatement(sqlUpdateTraspaso);
            stmtTraspaso.setInt(1, traspasoData.getIdBodegaOrigen());
            stmtTraspaso.setInt(2, traspasoData.getIdBodegaDestino());
            stmtTraspaso.setString(3, traspasoData.getMotivoTraspaso());
            stmtTraspaso.setString(4, traspasoData.getObservaciones());
            stmtTraspaso.setInt(5, traspasoData.getProductos().size());
            stmtTraspaso.setBigDecimal(6, traspasoData.getMontoTotal());
            stmtTraspaso.setString(7, numeroTraspasoOriginal);

            int filasAfectadasTraspaso = stmtTraspaso.executeUpdate();

            if (filasAfectadasTraspaso > 0) {
                System.out.println("✅ Datos principales del traspaso actualizados");

                // 2. Obtener ID del traspaso
                int idTraspaso = obtenerIdTraspaso(numeroTraspasoOriginal, conn);

                if (idTraspaso > 0) {
                    // 3. Eliminar detalles existentes
                    String sqlDeleteDetalles = "DELETE FROM traspaso_detalles WHERE id_traspaso = ?";
                    stmtDeleteDetalles = conn.prepareStatement(sqlDeleteDetalles);
                    stmtDeleteDetalles.setInt(1, idTraspaso);
                    int detallesEliminados = stmtDeleteDetalles.executeUpdate();

                    System.out.println("🗑️ Detalles anteriores eliminados: " + detallesEliminados);

                    // 4. Insertar nuevos detalles
                    String sqlDetalle = "INSERT INTO traspaso_detalles (id_traspaso, id_producto, id_variante, " +
                            "cantidad_solicitada, Tipo, observaciones, estado_detalle, precio_unitario, subtotal) " +
                            "VALUES (?, ?, ?, ?, ?, ?, 'pendiente', ?, ?)";

                    stmtDetalle = conn.prepareStatement(sqlDetalle);

                    for (ProductoTraspasoItem producto : traspasoData.getProductos()) {

                        // Fetch price if missing
                        if (producto.getPrecioUnitario() == null
                                || producto.getPrecioUnitario().compareTo(java.math.BigDecimal.ZERO) == 0) {
                            java.math.BigDecimal price = obtenerPrecioVenta(producto.getIdProducto(),
                                    producto.getIdVariante());
                            producto.setPrecioUnitario(price);
                            producto.calcularSubtotal();
                        }

                        stmtDetalle.setInt(1, idTraspaso);
                        stmtDetalle.setInt(2, producto.getIdProducto());

                        if (producto.getIdVariante() != null && producto.getIdVariante() > 0) {
                            stmtDetalle.setInt(3, producto.getIdVariante());
                        } else {
                            stmtDetalle.setNull(3, Types.INTEGER);
                        }

                        stmtDetalle.setInt(4, producto.getCantidadSolicitada());

                        // CORRECCIÓN: Agregar campo Tipo
                        String tipo = producto.getTipo();
                        if (tipo == null || tipo.trim().isEmpty())
                            tipo = "par";
                        tipo = tipo.trim().toLowerCase();
                        if (!"par".equals(tipo) && !"caja".equals(tipo))
                            tipo = "par";
                        stmtDetalle.setString(5, tipo);

                        stmtDetalle.setString(6, producto.getObservaciones());

                        stmtDetalle.setBigDecimal(7, producto.getPrecioUnitario());
                        stmtDetalle.setBigDecimal(8, producto.getSubtotal());

                        stmtDetalle.addBatch();
                    }

                    int[] resultadosDetalles = stmtDetalle.executeBatch();
                    System.out.println("âœ… Nuevos detalles insertados: " + resultadosDetalles.length);

                    // 5. Registrar movimiento de actualizaciÃ³n
                    registrarMovimientoActualizacion(idTraspaso, traspasoData, conn);

                    conn.commit();
                    System.out.println("âœ… Traspaso actualizado exitosamente");
                    return true;
                } else {
                    System.err.println("âŒ No se pudo obtener ID del traspaso");
                    conn.rollback();
                    return false;
                }
            } else {
                System.err.println("âŒ No se pudo actualizar el traspaso principal");
                conn.rollback();
                return false;
            }

        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            System.err.println("âŒ Error actualizando traspaso: " + e.getMessage());
            throw e;
        } finally {
            if (stmtDetalle != null)
                stmtDetalle.close();
            if (stmtDeleteDetalles != null)
                stmtDeleteDetalles.close();
            if (stmtTraspaso != null)
                stmtTraspaso.close();
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    /**
     * Obtiene el ID de un traspaso por su nÃºmero
     */
    private int obtenerIdTraspaso(String numeroTraspaso, Connection conn) throws SQLException {
        String sql = "SELECT id_traspaso FROM traspasos WHERE numero_traspaso = ?";

        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, numeroTraspaso);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("id_traspaso");
            }
            return -1;

        } finally {
            if (rs != null)
                rs.close();
            if (stmt != null)
                stmt.close();
        }
    }

    /**
     * Registra un movimiento de actualizaciÃ³n para auditorÃ­a
     */
    private void registrarMovimientoActualizacion(int idTraspaso, TraspasoDatos traspasoData, Connection conn) {
        String sql = "INSERT INTO traspaso_auditoria (id_traspaso, accion, fecha_accion, id_usuario, observaciones) " +
                "VALUES (?, 'actualización', NOW(), ?, ?)";

        PreparedStatement stmt = null;

        try {
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, idTraspaso);
            stmt.setInt(2, traspasoData.getIdUsuarioSolicita());
            stmt.setString(3, "Traspaso actualizado - Total productos: " + traspasoData.getProductos().size());

            stmt.executeUpdate();
            System.out.println("✅ Movimiento de auditoría registrado");

        } catch (SQLException e) {
            System.err.println("⚠️ Error registrando auditoría (no crítico): " + e.getMessage());
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Obtiene informaciÃ³n de cambios en traspasos para verificaciÃ³n rÃ¡pida
     * 
     * @return Object[] con [ÃºltimaModificaciÃ³n (Timestamp), totalTraspasos (int)]
     */
    public Object[] obtenerEstadoTraspasos() throws SQLException {
        String sql = "SELECT " +
                "COALESCE(MAX(GREATEST(fecha_creacion, COALESCE(fecha_modificacion, fecha_creacion))), NOW()) as ultima_modificacion, "
                +
                "COUNT(*) as total " +
                "FROM traspasos";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            if (rs.next()) {
                Timestamp ultimaModificacion = rs.getTimestamp("ultima_modificacion");
                int total = rs.getInt("total");
                return new Object[] { ultimaModificacion, total };
            }
        } finally {
            if (rs != null)
                rs.close();
            if (stmt != null)
                stmt.close();
            if (conn != null)
                conn.close();
        }

        return new Object[] { new Timestamp(System.currentTimeMillis()), 0 };
    }
}
