/*
 * TraspasoService - VERSIÓN CORREGIDA
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
import raven.controlador.principal.conexion;
import raven.utils.NotificacionesService;

public class TraspasoService {

    /**
     * Clase interna para representar el stock de un producto o variante
     */
    public static class StockInfo {
        private final int stockCaja;
        private final int stockPar;

        public StockInfo(int stockCaja, int stockPar) {
            this.stockCaja = stockCaja;
            this.stockPar = stockPar;
        }

        public int getStockCaja() { return stockCaja; }
        public int getStockPar() { return stockPar; }
        public int getStock(String tipo) {
            return "caja".equalsIgnoreCase(tipo) ? stockCaja : stockPar;
        }
    }


    public List<Bodega> obtenerBodegasActivas() throws SQLException {
        List<Bodega> bodegas = new ArrayList<>();
        String sql = "SELECT id_bodega, codigo, nombre, direccion, telefono, responsable, " +
                "tipo, capacidad_maxima, activa FROM bodegas WHERE activa = 1 ORDER BY nombre";

        try (Connection conn = conexion.getInstance().createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Bodega bodega = new Bodega();
                bodega.setIdBodega(rs.getInt("id_bodega"));
                bodega.setCodigo(rs.getString("codigo"));
                bodega.setNombre(rs.getString("nombre"));
                bodega.setDireccion(rs.getString("direccion"));
                bodega.setTelefono(rs.getString("telefono"));
                bodega.setResponsable(rs.getString("responsable"));
                bodega.setTipo(rs.getString("tipo"));

                Object capacidadObj = rs.getObject("capacidad_maxima");
                if (capacidadObj != null) {
                    bodega.setCapacidadMaxima(rs.getInt("capacidad_maxima"));
                }

                bodega.setActiva(rs.getBoolean("activa"));
                bodegas.add(bodega);
            }
        }
        return bodegas;
    }

    /**
     * Obtener el nombre de una bodega por su ID
     */
    public String obtenerNombreBodegaPorId(Integer idBodega) throws SQLException {
        String sql = "SELECT nombre FROM bodegas WHERE id_bodega = ? AND activa = 1";

        try (Connection conn = conexion.getInstance().createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idBodega);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("nombre");
                }
            }
        }
        return null;
    }

    public boolean existeNumeroTraspaso(String numero) throws SQLException {
        String sql = "SELECT 1 FROM traspasos WHERE numero_traspaso = ?";
        try (Connection conn = conexion.getInstance().createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, numero);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
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

        try (Connection conn = conexion.getInstance().createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String parametro = "%" + busqueda + "%";
            stmt.setString(1, parametro);
            stmt.setString(2, parametro);
            stmt.setString(3, parametro);

            try (ResultSet rs = stmt.executeQuery()) {
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
            }
        }
        return resultados;
    }

    public String generarNumeroTraspaso() throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(numero_traspaso, 3) AS UNSIGNED)), 0) + 1 as next_number " +
                "FROM traspasos WHERE numero_traspaso LIKE 'TR%'";

        try (Connection conn = conexion.getInstance().createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                int nextNumber = rs.getInt("next_number");
                return String.format("TR%06d", nextNumber);
            }
        }
        return "TR000001";
    }

    /**
     * Valida si hay stock suficiente en una bodega específica o globalmente.
     */
    public boolean validarStockBodega(Integer idBodega, Integer idProducto, Integer idVariante, String tipo,
            int cantidadSolicitada) throws SQLException {
        int stockDisponible = getStockDisponible(idBodega, idProducto, idVariante, tipo);
        
        System.out.println("🔍 Validación Stock - Bodega: " + idBodega + ", Disponible: " + stockDisponible + ", Solicitado: " + cantidadSolicitada);
        
        return stockDisponible >= cantidadSolicitada;
    }

    /**
     * Obtiene info de stock detallada para una bodega o global.
     */
    public StockInfo obtenerStockBodega(Integer idBodega, Integer idProducto, Integer idVariante) throws SQLException {
        int stockCaja = getStockDisponible(idBodega, idProducto, idVariante, "caja");
        int stockPar = getStockDisponible(idBodega, idProducto, idVariante, "par");
        return new StockInfo(stockCaja, stockPar);
    }

    /**
     * Obtiene el stock disponible. Si idBodega es null o <= 0 (como NO_BODEGA = -1), 
     * devuelve el stock global (suma de todas las bodegas).
     */
    public int getStockDisponible(Integer idBodega, Integer idProducto, Integer idVariante, String tipo) throws SQLException {
        if (idBodega == null || idBodega <= 0) {
            return obtenerStockGlobalVariante(idProducto, idVariante, tipo);
        }
        return obtenerStockInventarioBodega(idBodega, idProducto, idVariante, tipo);
    }

    /**
     * MÉTODO NUEVO - Obtener stock desde inventario_bodega
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
        try (Connection conn = conexion.getInstance().createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int idx = 1;
            stmt.setInt(idx++, idBodega);
            if (idVariante != null) {
                stmt.setInt(idx++, idVariante);
            } else {
                stmt.setInt(idx++, idProducto);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("stock");
                }
            }
        }
        return 0;
    }

    /**
     * MÉTODO NUEVO - Obtener stock desde producto_variantes (fallback)
     */
    private int obtenerStockGlobalVariante(Integer idProducto, Integer idVariante, String tipo) throws SQLException {
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

        try (Connection conn = conexion.getInstance().createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (idVariante != null) {
                stmt.setInt(1, idVariante);
            } else {
                stmt.setInt(1, idProducto);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("stock");
                }
            }
        }
        return 0;
    }

    /**
     * Helper to get price
     */
    private java.math.BigDecimal obtenerPrecioVenta(Integer idVariante) {
        String sql = "SELECT precio_venta FROM producto_variantes WHERE id_variante = ?";
        if (idVariante == null) {
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
        if (!TraspasoPermissionValidator.puedeCrearTraspaso()) {
            throw new SQLException("No tienes permiso para crear traspasos");
        }

        try (Connection conn = conexion.getInstance().createConnection()) {
            conn.setAutoCommit(false);

            Integer idSolicitaRaw = traspasoData.getIdUsuarioSolicita();
            int idSolicita = (idSolicitaRaw != null && idSolicitaRaw > 0) ? idSolicitaRaw : idUsuario;
            idSolicita = ensureUsuarioIdExists(conn, idSolicita);

            String sqlTraspaso = "INSERT INTO traspasos (numero_traspaso, id_bodega_origen, id_bodega_destino, " +
                    "id_usuario_solicita, id_usuario_crea, fecha_solicitud, estado, motivo, observaciones, total_productos, monto_total) " +
                    "VALUES (?, ?, ?, ?, ?, NOW(), 'pendiente', ?, ?, ?, ?)";

            int idTraspaso = -1;
            try (PreparedStatement stmtTraspaso = conn.prepareStatement(sqlTraspaso, Statement.RETURN_GENERATED_KEYS)) {
                stmtTraspaso.setString(1, traspasoData.getNumeroTraspaso());
                stmtTraspaso.setInt(2, traspasoData.getIdBodegaOrigen());
                stmtTraspaso.setInt(3, traspasoData.getIdBodegaDestino());
                stmtTraspaso.setInt(4, idSolicita);
                stmtTraspaso.setInt(5, idUsuario);
                String motivoFinal = traspasoData.getMotivoTraspaso();
                if (motivoFinal == null || motivoFinal.trim().isEmpty()) {
                    motivoFinal = traspasoData.getTipoTraspaso();
                }
                stmtTraspaso.setString(5, motivoFinal);
                stmtTraspaso.setString(6, traspasoData.getObservaciones());
                stmtTraspaso.setInt(7, traspasoData.getProductos().size());

                traspasoData.calcularTotales();
                stmtTraspaso.setBigDecimal(8, traspasoData.getMontoTotal());

                int filasAfectadas = stmtTraspaso.executeUpdate();

                if (filasAfectadas > 0) {
                    try (ResultSet rsKeys = stmtTraspaso.getGeneratedKeys()) {
                        if (rsKeys.next()) {
                            idTraspaso = rsKeys.getInt(1);
                        }
                    }

                    String sqlDetalle = "INSERT INTO traspaso_detalles (id_traspaso, id_producto, id_variante, " +
                            "cantidad_solicitada, Tipo, observaciones, estado_detalle, precio_unitario, subtotal) " +
                            "VALUES (?, ?, ?, ?, ?, ?, 'pendiente', ?, ?)";

                    try (PreparedStatement stmtDetalle = conn.prepareStatement(sqlDetalle)) {
                        for (ProductoTraspasoItem producto : traspasoData.getProductos()) {
                            if (producto.getPrecioUnitario() == null
                                    || producto.getPrecioUnitario().compareTo(java.math.BigDecimal.ZERO) == 0) {
                                java.math.BigDecimal price = obtenerPrecioVenta(producto.getIdVariante());
                                producto.setPrecioUnitario(price);
                                producto.calcularSubtotal();
                            }

                            stmtDetalle.setInt(1, idTraspaso);
                            stmtDetalle.setInt(2, producto.getIdProducto());
                            if (producto.getIdVariante() != null && producto.getIdVariante() > 0) {
                                stmtDetalle.setInt(3, producto.getIdVariante());
                            } else {
                                stmtDetalle.setNull(3, java.sql.Types.INTEGER);
                            }
                            stmtDetalle.setInt(4, producto.getCantidadSolicitada());
                            String tipo = producto.getTipo();
                            if (tipo == null || tipo.trim().isEmpty()) tipo = "par";
                            tipo = tipo.trim().toLowerCase();
                            if (!"par".equals(tipo) && !"caja".equals(tipo)) tipo = "par";
                            stmtDetalle.setString(5, tipo);
                            stmtDetalle.setString(6, producto.getObservaciones());
                            stmtDetalle.setBigDecimal(7, producto.getPrecioUnitario());
                            stmtDetalle.setBigDecimal(8, producto.getSubtotal());

                            stmtDetalle.addBatch();
                        }
                        stmtDetalle.executeBatch();
                    }

                    conn.commit();
                    try {
                        if (idTraspaso > 0) {
                            new NotificacionesService().notificarTraspasoEvento(
                                    idTraspaso, "solicitud", "Solicitud de Traspaso",
                                    "Nuevo traspaso " + traspasoData.getNumeroTraspaso() + " pendiente");
                        }
                    } catch (SQLException notifyEx) {
                        System.err.println("Notification failed: " + notifyEx.getMessage());
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            return idTraspaso;
        }
    }

    private int ensureUsuarioIdExists(Connection con, int candidate) throws SQLException {
        if (candidate > 0 && existsUsuario(con, candidate))
            return candidate;
        int sid = raven.controlador.admin.SessionManager.getInstance().getCurrentUserId();
        if (sid > 0 && existsUsuario(con, sid))
            return sid;
        try (PreparedStatement ps = con
                .prepareStatement("SELECT id_usuario FROM usuarios WHERE activo = 1 ORDER BY id_usuario ASC LIMIT 1");
                ResultSet rs = ps.executeQuery()) {
            if (rs.next())
                return rs.getInt(1);
        }
        throw new SQLException("Usuario solicitante inválido o inexistente");
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

        try (Connection conn = conexion.getInstance().createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < parametros.size(); i++) {
                stmt.setObject(i + 1, parametros.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Object[] fila = new Object[8];
                    fila[0] = rs.getString("numero_traspaso");
                    fila[1] = rs.getString("bodega_origen");
                    fila[2] = rs.getString("bodega_destino");
                    fila[3] = rs.getString("fecha_solicitud");
                    fila[4] = rs.getString("estado");
                    fila[5] = rs.getInt("total_productos");

                    java.math.BigDecimal monto = rs.getBigDecimal("monto_total");
                    if (monto == null) monto = java.math.BigDecimal.ZERO;
                    fila[6] = java.text.NumberFormat.getCurrencyInstance().format(monto);
                    fila[7] = "Ver/Editar";
                    traspasos.add(fila);
                }
            }
        }
        return traspasos;
    }

    /**
     * Cambiar estado de un traspaso
     * MODIFICADO: Integra validaciones de permisos según el estado destino
     */
    public boolean cambiarEstadoTraspaso(String numeroTraspaso, String nuevoEstado,
            int idUsuario) throws SQLException {
        String estadoActual = null;
        Integer bodegaOrigen = null;
        Integer bodegaDestino = null;

        String sqlConsulta = "SELECT estado, id_bodega_origen, id_bodega_destino FROM traspasos WHERE numero_traspaso = ?";
        try (Connection conn = conexion.getInstance().createConnection();
             PreparedStatement ps = conn.prepareStatement(sqlConsulta)) {
            ps.setString(1, numeroTraspaso);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    estadoActual = rs.getString("estado");
                    bodegaOrigen = rs.getInt("id_bodega_origen");
                    bodegaDestino = rs.getInt("id_bodega_destino");
                } else {
                    throw new SQLException("Traspaso no encontrado: " + numeroTraspaso);
                }
            }
        }

        boolean tienePermiso = false;
        String mensajeError = "";

        switch (nuevoEstado.toLowerCase()) {
            case "autorizado" -> {
                tienePermiso = TraspasoPermissionValidator.puedeAutorizarTraspaso(estadoActual, bodegaDestino);
                mensajeError = "No tienes permiso para autorizar este traspaso. Debes pertenecer a la bodega destino.";
            }
            case "en_transito", "enviado" -> {
                tienePermiso = TraspasoPermissionValidator.puedeEnviarTraspaso(estadoActual, bodegaOrigen);
                mensajeError = "No tienes permiso para enviar este traspaso. Debes pertenecer a la bodega origen.";
            }
            case "recibido" -> {
                tienePermiso = TraspasoPermissionValidator.puedeRecibirTraspaso(estadoActual, bodegaDestino);
                mensajeError = "No tienes permiso para recibir este traspaso. Debes pertenecer a la bodega destino.";
            }
            case "cancelado" -> {
                tienePermiso = TraspasoPermissionValidator.puedeCancelarTraspaso(estadoActual, bodegaOrigen, bodegaDestino);
                mensajeError = "No tienes permiso para cancelar este traspaso.";
            }
            default -> throw new SQLException("Estado no válido: " + nuevoEstado);
        }

        if (!tienePermiso) {
            throw new SQLException(mensajeError);
        }

        String sql = "UPDATE traspasos SET estado = ?, fecha_autorizacion = NOW(), " +
                "id_usuario_autoriza = ? WHERE numero_traspaso = ?";

        try (Connection conn = conexion.getInstance().createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, idUsuario);
            stmt.setString(3, numeroTraspaso);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean actualizarTraspaso(TraspasoDatos traspasoData, String numeroTraspasoOriginal) throws SQLException {
        try (Connection conn = conexion.getInstance().createConnection()) {
            // VALIDACIÓN DE PERMISOS PARA EDICIÓN
            if (!TraspasoPermissionValidator.puedeEditarTraspaso(numeroTraspasoOriginal, conn)) {
                throw new SQLException("No tiene permisos para editar el traspaso " + numeroTraspasoOriginal 
                        + " en su estado actual.");
            }
            
            conn.setAutoCommit(false);
            try {
                System.out.println("🔄 Actualizando traspaso: " + numeroTraspasoOriginal);

                String sqlUpdateTraspaso = "UPDATE traspasos SET " +
                        "id_bodega_origen = ?, id_bodega_destino = ?, " +
                        "motivo = ?, observaciones = ?, total_productos = ?, monto_total = ? " +
                        "WHERE numero_traspaso = ?";

                traspasoData.calcularTotales();

                try (PreparedStatement stmtTraspaso = conn.prepareStatement(sqlUpdateTraspaso)) {
                    stmtTraspaso.setInt(1, traspasoData.getIdBodegaOrigen());
                    stmtTraspaso.setInt(2, traspasoData.getIdBodegaDestino());
                    stmtTraspaso.setString(3, traspasoData.getMotivoTraspaso());
                    stmtTraspaso.setString(4, traspasoData.getObservaciones());
                    stmtTraspaso.setInt(5, traspasoData.getProductos().size());
                    stmtTraspaso.setBigDecimal(6, traspasoData.getMontoTotal());
                    stmtTraspaso.setString(7, numeroTraspasoOriginal);

                    if (stmtTraspaso.executeUpdate() > 0) {
                        System.out.println("✅ Datos principales del traspaso actualizados");

                        int idTraspaso = obtenerIdTraspaso(numeroTraspasoOriginal, conn);
                        if (idTraspaso > 0) {
                            String sqlDeleteDetalles = "DELETE FROM traspaso_detalles WHERE id_traspaso = ?";
                            try (PreparedStatement stmtDeleteDetalles = conn.prepareStatement(sqlDeleteDetalles)) {
                                stmtDeleteDetalles.setInt(1, idTraspaso);
                                int detallesEliminados = stmtDeleteDetalles.executeUpdate();
                                System.out.println("🗑️ Detalles anteriores eliminados: " + detallesEliminados);
                            }

                            String sqlDetalle = "INSERT INTO traspaso_detalles (id_traspaso, id_producto, id_variante, " +
                                    "cantidad_solicitada, Tipo, observaciones, estado_detalle, precio_unitario, subtotal) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, 'pendiente', ?, ?)";

                            try (PreparedStatement stmtDetalle = conn.prepareStatement(sqlDetalle)) {
                                for (ProductoTraspasoItem producto : traspasoData.getProductos()) {
                                    if (producto.getPrecioUnitario() == null
                                            || producto.getPrecioUnitario().compareTo(java.math.BigDecimal.ZERO) == 0) {
                                        java.math.BigDecimal price = obtenerPrecioVenta(producto.getIdVariante());
                                        producto.setPrecioUnitario(price);
                                        producto.calcularSubtotal();
                                    }

                                    stmtDetalle.setInt(1, idTraspaso);
                                    stmtDetalle.setInt(2, producto.getIdProducto());
                                    if (producto.getIdVariante() != null && producto.getIdVariante() > 0) {
                                        stmtDetalle.setInt(3, producto.getIdVariante());
                                    } else {
                                        stmtDetalle.setNull(3, java.sql.Types.INTEGER);
                                    }
                                    stmtDetalle.setInt(4, producto.getCantidadSolicitada());

                                    String tipo = producto.getTipo();
                                    if (tipo == null || tipo.trim().isEmpty()) tipo = "par";
                                    tipo = tipo.trim().toLowerCase();
                                    if (!"par".equals(tipo) && !"caja".equals(tipo)) tipo = "par";
                                    stmtDetalle.setString(5, tipo);

                                    stmtDetalle.setString(6, producto.getObservaciones());
                                    stmtDetalle.setBigDecimal(7, producto.getPrecioUnitario());
                                    stmtDetalle.setBigDecimal(8, producto.getSubtotal());
                                    stmtDetalle.addBatch();
                                }
                                stmtDetalle.executeBatch();
                            }

                            registrarMovimientoActualizacion(idTraspaso, traspasoData, conn);
                            conn.commit();
                            System.out.println("✅ Traspaso actualizado exitosamente");
                            return true;
                        } else {
                            conn.rollback();
                            return false;
                        }
                    } else {
                        conn.rollback();
                        return false;
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private int obtenerIdTraspaso(String numeroTraspaso, Connection conn) throws SQLException {
        String sql = "SELECT id_traspaso FROM traspasos WHERE numero_traspaso = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, numeroTraspaso);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_traspaso");
                }
            }
        }
        return -1;
    }

    private void registrarMovimientoActualizacion(int idTraspaso, TraspasoDatos traspasoData, Connection conn) {
        String sql = "INSERT INTO traspaso_auditoria (id_traspaso, accion, fecha_accion, id_usuario, observaciones) " +
                "VALUES (?, 'actualización', NOW(), ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idTraspaso);
            Integer idSolicita = traspasoData.getIdUsuarioSolicita();
            
            if (idSolicita == null || idSolicita == 0) {
                try {
                    idSolicita = raven.clases.admin.UserSession.getInstance().getCurrentUser().getIdUsuario();
                } catch (Exception e) {
                    idSolicita = 0;
                }
            }
            
            stmt.setInt(2, idSolicita);
            stmt.setString(3, "Traspaso actualizado - Total productos: " + traspasoData.getProductos().size());

            stmt.executeUpdate();
            System.out.println("✅ Movimiento de auditoría registrado");
        } catch (SQLException e) {
            System.err.println("⚠️ Error registrando auditoría (no crítico): " + e.getMessage());
        }
    }

    public Object[] obtenerEstadoTraspasos() throws SQLException {
        String sql = "SELECT " +
                "COALESCE(MAX(GREATEST(fecha_creacion, COALESCE(fecha_modificacion, fecha_creacion))), NOW()) as ultima_modificacion, "
                +
                "COUNT(*) as total " +
                "FROM traspasos";

        try (Connection conn = conexion.getInstance().createConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                Timestamp ultimaModificacion = rs.getTimestamp("ultima_modificacion");
                int total = rs.getInt("total");
                return new Object[] { ultimaModificacion, total };
            }
        }
        return new Object[] { new Timestamp(System.currentTimeMillis()), 0 };
    }
}
