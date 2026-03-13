package raven.clases.principal;

import raven.controlador.principal.conexion;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import raven.controlador.principal.ModelGastoOperativo;
import raven.controlador.principal.ModelTipoGasto;
import raven.controlador.principal.ModelGastoOperativo.EstadoGasto;

/**
 * Servicio para gestión de gastos operativos.
 * 
 * Implementa Repository Pattern + Service Layer.
 * 
 * Responsabilidades:
 * - CRUD de gastos operativos
 * - Registro en movimientos de caja
 * - Consultas y reportes
 * 
 * Principios aplicados:
 * - Single Responsibility: solo operaciones de gastos
 * - Dependency Injection ready: usa conexión centralizada
 * - Transaction management: operaciones atómicas
 * 
 * @author CrisDEV
 * @version 1.0
 */
public class ServiceGastoOperativo {

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTANTES SQL
    // ═══════════════════════════════════════════════════════════════════════════

    private static final String SQL_INSERT = """
            INSERT INTO gastos_operativos
            (id_tipo_gasto, concepto, monto, proveedor_persona, numero_recibo,
             observaciones, id_bodega, id_usuario, id_movimiento_caja, estado, fecha_gasto)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SQL_UPDATE = """
            UPDATE gastos_operativos SET
                id_tipo_gasto = ?,
                concepto = ?,
                monto = ?,
                proveedor_persona = ?,
                numero_recibo = ?,
                observaciones = ?,
                estado = ?,
                fecha_actualizacion = NOW()
            WHERE id_gasto = ?
            """;

    private static final String SQL_DELETE = """
            UPDATE gastos_operativos SET estado = 'anulado', fecha_actualizacion = NOW()
            WHERE id_gasto = ?
            """;

    private static final String SQL_SELECT_BY_ID = """
            SELECT g.*, t.nombre as nombre_tipo_gasto, b.nombre as nombre_bodega, u.nombre as nombre_usuario
            FROM gastos_operativos g
            LEFT JOIN tipos_gastos t ON g.id_tipo_gasto = t.id_tipo_gasto
            LEFT JOIN bodegas b ON g.id_bodega = b.id_bodega
            LEFT JOIN usuarios u ON g.id_usuario = u.id_usuario
            WHERE g.id_gasto = ?
            """;

    private static final String SQL_SELECT_BY_MOVIMIENTO = """
            SELECT g.*, t.nombre as nombre_tipo_gasto, b.nombre as nombre_bodega, u.nombre as nombre_usuario
            FROM gastos_operativos g
            LEFT JOIN tipos_gastos t ON g.id_tipo_gasto = t.id_tipo_gasto
            LEFT JOIN bodegas b ON g.id_bodega = b.id_bodega
            LEFT JOIN usuarios u ON g.id_usuario = u.id_usuario
            INNER JOIN caja_movimiento_detalle cmd ON g.id_movimiento_caja = cmd.id_detalle_movimiento
            WHERE cmd.id_movimiento_caja = ? AND g.estado != 'anulado'
            ORDER BY g.fecha_gasto DESC
            """;

    private static final String SQL_SELECT_BY_FECHA = """
            SELECT g.*, t.nombre as nombre_tipo_gasto, b.nombre as nombre_bodega, u.nombre as nombre_usuario
            FROM gastos_operativos g
            LEFT JOIN tipos_gastos t ON g.id_tipo_gasto = t.id_tipo_gasto
            LEFT JOIN bodegas b ON g.id_bodega = b.id_bodega
            LEFT JOIN usuarios u ON g.id_usuario = u.id_usuario
            WHERE DATE(g.fecha_gasto) BETWEEN ? AND ?
            AND g.id_bodega = ?
            AND g.estado != 'anulado'
            ORDER BY g.fecha_gasto DESC
            """;

    private static final String SQL_INSERT_MOVIMIENTO_CAJA = """
            INSERT INTO caja_movimiento_detalle
            (id_movimiento_caja, tipo_movimiento, concepto, monto,
             id_referencia, tipo_referencia, numero_comprobante, id_usuario, observaciones)
            VALUES (?, 'salida_gasto', ?, ?, ?, 'gasto_interno', ?, ?, ?)
            """;

    private static final String SQL_TOTAL_GASTOS_MOVIMIENTO = """
            SELECT COALESCE(SUM(monto), 0) as total
            FROM gastos_operativos
            WHERE id_movimiento_caja = ? AND estado != 'anulado'
            """;

    private static final String SQL_TIPOS_GASTO = """
            SELECT * FROM tipos_gastos WHERE activo = 1 ORDER BY nombre
            """;

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTODOS CRUD
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Registra un nuevo gasto operativo con movimiento en caja.
     * 
     * TRANSACCIÓN:
     * 1. Inserta en gastos_operativos
     * 2. Inserta en caja_movimiento_detalle
     * 3. Actualiza referencia en gasto
     * 
     * @param gasto            Datos del gasto
     * @param idMovimientoCaja ID del movimiento de caja activo
     * @return ID del gasto creado
     * @throws SQLException si falla la operación
     */
    public int registrarGasto(ModelGastoOperativo gasto, int idMovimientoCaja) throws SQLException {
        Connection conn = null;
        PreparedStatement psGasto = null;
        PreparedStatement psMovimiento = null;
        PreparedStatement psUpdate = null;
        ResultSet rs = null;

        try {
            conn = conexion.getConnectionStatic();
            conn.setAutoCommit(false);

            // 1. Validar gasto
            gasto.validar();

            // 2. Insertar gasto
            psGasto = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            psGasto.setInt(1, gasto.getIdTipoGasto());
            psGasto.setString(2, gasto.getConcepto());
            psGasto.setBigDecimal(3, gasto.getMonto());
            psGasto.setString(4, gasto.getProveedorPersona());
            psGasto.setString(5, gasto.getNumeroRecibo());
            psGasto.setString(6, gasto.getObservaciones());
            psGasto.setInt(7, gasto.getIdBodega());
            psGasto.setInt(8, gasto.getIdUsuario());
            psGasto.setNull(9, Types.INTEGER); // Se actualiza después
            psGasto.setString(10, EstadoGasto.REGISTRADO.getValor());
            psGasto.setTimestamp(11, Timestamp.valueOf(
                    gasto.getFechaGasto() != null ? gasto.getFechaGasto() : LocalDateTime.now()));

            int affected = psGasto.executeUpdate();
            if (affected == 0) {
                throw new SQLException("No se pudo insertar el gasto");
            }

            rs = psGasto.getGeneratedKeys();
            if (!rs.next()) {
                throw new SQLException("No se obtuvo ID del gasto insertado");
            }
            int idGasto = rs.getInt(1);
            gasto.setIdGasto(idGasto);

            // 3. Insertar movimiento de caja
            String comprobante = generarNumeroComprobante(idGasto);
            psMovimiento = conn.prepareStatement(SQL_INSERT_MOVIMIENTO_CAJA, Statement.RETURN_GENERATED_KEYS);
            psMovimiento.setInt(1, idMovimientoCaja);
            psMovimiento.setString(2, "Gasto: " + gasto.getConcepto());
            psMovimiento.setBigDecimal(3, gasto.getMonto());
            psMovimiento.setInt(4, idGasto);
            psMovimiento.setString(5, comprobante);
            psMovimiento.setInt(6, gasto.getIdUsuario());
            psMovimiento.setString(7, gasto.getObservaciones());

            affected = psMovimiento.executeUpdate();
            if (affected == 0) {
                throw new SQLException("No se pudo registrar movimiento de caja");
            }

            rs = psMovimiento.getGeneratedKeys();
            if (!rs.next()) {
                throw new SQLException("No se obtuvo ID del movimiento de caja");
            }
            int idDetalleMovimiento = rs.getInt(1);

            // 4. Actualizar gasto con referencia al movimiento
            psUpdate = conn.prepareStatement(
                    "UPDATE gastos_operativos SET id_movimiento_caja = ? WHERE id_gasto = ?");
            psUpdate.setInt(1, idDetalleMovimiento);
            psUpdate.setInt(2, idGasto);
            psUpdate.executeUpdate();

            gasto.setIdMovimientoCaja(idDetalleMovimiento);

            conn.commit();

            System.out.println("SUCCESS  Gasto registrado: ID " + idGasto + " - $" + gasto.getMonto());

            return idGasto;

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
            cerrarRecursos(conn, rs, psGasto, psMovimiento, psUpdate);
        }
    }

    /**
     * Obtiene un gasto por su ID.
     */
    public Optional<ModelGastoOperativo> obtenerPorId(int idGasto) throws SQLException {
        try (Connection conn = conexion.getConnectionStatic();
                PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_ID)) {

            ps.setInt(1, idGasto);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearGasto(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Lista gastos por movimiento de caja.
     */
    public List<ModelGastoOperativo> listarPorMovimiento(int idMovimientoCaja) throws SQLException {
        List<ModelGastoOperativo> gastos = new ArrayList<>();

        try (Connection conn = conexion.getConnectionStatic();
                PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_MOVIMIENTO)) {

            ps.setInt(1, idMovimientoCaja);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    gastos.add(mapearGasto(rs));
                }
            }
        }

        return gastos;
    }

    /**
     * Lista gastos por rango de fechas y bodega.
     */
    public List<ModelGastoOperativo> listarPorFecha(LocalDate fechaInicio, LocalDate fechaFin,
            int idBodega) throws SQLException {
        List<ModelGastoOperativo> gastos = new ArrayList<>();

        try (Connection conn = conexion.getConnectionStatic();
                PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_FECHA)) {

            ps.setDate(1, Date.valueOf(fechaInicio));
            ps.setDate(2, Date.valueOf(fechaFin));
            ps.setInt(3, idBodega);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    gastos.add(mapearGasto(rs));
                }
            }
        }

        return gastos;
    }

    /**
     * Anula un gasto (soft delete).
     * También anula el movimiento de caja asociado.
     */
    public boolean anularGasto(int idGasto, int idUsuario) throws SQLException {
        Connection conn = null;

        try {
            conn = conexion.getConnectionStatic();
            conn.setAutoCommit(false);

            // Obtener gasto actual
            Optional<ModelGastoOperativo> gastoOpt = obtenerPorId(idGasto);
            if (gastoOpt.isEmpty()) {
                throw new SQLException("Gasto no encontrado: " + idGasto);
            }

            ModelGastoOperativo gasto = gastoOpt.get();
            if (!gasto.puedeAnularse()) {
                throw new SQLException("El gasto no puede anularse en su estado actual");
            }

            // Anular gasto
            try (PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
                ps.setInt(1, idGasto);
                ps.executeUpdate();
            }

            // Anular movimiento de caja si existe
            if (gasto.getIdMovimientoCaja() != null) {
                String sqlAnularMov = "UPDATE caja_movimiento_detalle SET activo = 0 WHERE id_detalle_movimiento = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlAnularMov)) {
                    ps.setInt(1, gasto.getIdMovimientoCaja());
                    ps.executeUpdate();
                }
            }

            conn.commit();

            System.out.println("SUCCESS  Gasto anulado: ID " + idGasto);
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    /* ignore */ }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    /* ignore */ }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTODOS DE CONSULTA
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Obtiene el total de gastos para un movimiento de caja.
     */
    public BigDecimal obtenerTotalGastos(int idMovimientoCaja) throws SQLException {
        try (Connection conn = conexion.getConnectionStatic();
                PreparedStatement ps = conn.prepareStatement(SQL_TOTAL_GASTOS_MOVIMIENTO)) {

            ps.setInt(1, idMovimientoCaja);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("total");
                }
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * Lista todos los tipos de gasto activos.
     */
    public List<ModelTipoGasto> listarTiposGasto() throws SQLException {
        List<ModelTipoGasto> tipos = new ArrayList<>();

        try (Connection conn = conexion.getConnectionStatic();
                PreparedStatement ps = conn.prepareStatement(SQL_TIPOS_GASTO);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                tipos.add(mapearTipoGasto(rs));
            }
        }

        return tipos;
    }

    /**
     * Obtiene un tipo de gasto por ID.
     */
    public Optional<ModelTipoGasto> obtenerTipoGasto(int idTipoGasto) throws SQLException {
        String sql = "SELECT * FROM tipos_gastos WHERE id_tipo_gasto = ?";

        try (Connection conn = conexion.getConnectionStatic();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idTipoGasto);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearTipoGasto(rs));
                }
            }
        }
        return Optional.empty();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTODOS CRUD PARA TIPOS DE GASTO (ADMIN)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Lista TODOS los tipos de gasto (activos e inactivos) para administración.
     */
    public List<ModelTipoGasto> listarTodosTiposGasto() throws SQLException {
        List<ModelTipoGasto> tipos = new ArrayList<>();
        String sql = "SELECT * FROM tipos_gastos ORDER BY activo DESC, nombre ASC";

        try (Connection conn = conexion.getConnectionStatic();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                tipos.add(mapearTipoGasto(rs));
            }
        }

        return tipos;
    }

    /**
     * Busca tipos de gasto por nombre o código.
     */
    public List<ModelTipoGasto> buscarTiposGasto(String busqueda) throws SQLException {
        List<ModelTipoGasto> tipos = new ArrayList<>();
        String sql = """
                SELECT * FROM tipos_gastos
                WHERE (nombre LIKE ? OR codigo LIKE ?)
                ORDER BY activo DESC, nombre ASC
                """;

        try (Connection conn = conexion.getConnectionStatic();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            String patron = "%" + busqueda + "%";
            ps.setString(1, patron);
            ps.setString(2, patron);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tipos.add(mapearTipoGasto(rs));
                }
            }
        }

        return tipos;
    }

    /**
     * Crea un nuevo tipo de gasto.
     * 
     * @param tipo Datos del tipo de gasto
     * @return ID del tipo creado
     */
    public int crearTipoGasto(ModelTipoGasto tipo) throws SQLException {
        String sql = """
                INSERT INTO tipos_gastos
                (codigo, nombre, descripcion, categoria, requiere_autorizacion,
                 monto_maximo_sin_autorizacion, cuenta_contable, activo)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1)
                """;

        try (Connection conn = conexion.getConnectionStatic();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, tipo.getCodigo());
            ps.setString(2, tipo.getNombre());
            ps.setString(3, tipo.getDescripcion());
            ps.setString(4, tipo.getCategoria().getValor());
            ps.setBoolean(5, tipo.isRequiereAutorizacion());
            ps.setBigDecimal(6, tipo.getMontoMaximoSinAutorizacion());
            ps.setString(7, tipo.getCuentaContable());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("No se pudo crear el tipo de gasto");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    tipo.setIdTipoGasto(id);
                    System.out.println("SUCCESS  Tipo de gasto creado: " + tipo.getNombre() + " (ID: " + id + ")");
                    return id;
                }
            }

            throw new SQLException("No se obtuvo ID del tipo de gasto creado");
        }
    }

    /**
     * Actualiza un tipo de gasto existente.
     */
    public void actualizarTipoGasto(ModelTipoGasto tipo) throws SQLException {
        String sql = """
                UPDATE tipos_gastos SET
                    codigo = ?,
                    nombre = ?,
                    descripcion = ?,
                    categoria = ?,
                    requiere_autorizacion = ?,
                    monto_maximo_sin_autorizacion = ?,
                    cuenta_contable = ?,
                    fecha_actualizacion = NOW()
                WHERE id_tipo_gasto = ?
                """;

        try (Connection conn = conexion.getConnectionStatic();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, tipo.getCodigo());
            ps.setString(2, tipo.getNombre());
            ps.setString(3, tipo.getDescripcion());
            ps.setString(4, tipo.getCategoria().getValor());
            ps.setBoolean(5, tipo.isRequiereAutorizacion());
            ps.setBigDecimal(6, tipo.getMontoMaximoSinAutorizacion());
            ps.setString(7, tipo.getCuentaContable());
            ps.setInt(8, tipo.getIdTipoGasto());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("No se encontró el tipo de gasto con ID: " + tipo.getIdTipoGasto());
            }

            System.out.println("SUCCESS  Tipo de gasto actualizado: " + tipo.getNombre());
        }
    }

    /**
     * Desactiva un tipo de gasto (soft delete).
     */
    public void desactivarTipoGasto(int idTipoGasto) throws SQLException {
        String sql = "UPDATE tipos_gastos SET activo = 0, fecha_actualizacion = NOW() WHERE id_tipo_gasto = ?";

        try (Connection conn = conexion.getConnectionStatic();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idTipoGasto);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("No se encontró el tipo de gasto con ID: " + idTipoGasto);
            }

            System.out.println("SUCCESS  Tipo de gasto desactivado: ID " + idTipoGasto);
        }
    }

    /**
     * Reactiva un tipo de gasto.
     */
    public void activarTipoGasto(int idTipoGasto) throws SQLException {
        String sql = "UPDATE tipos_gastos SET activo = 1, fecha_actualizacion = NOW() WHERE id_tipo_gasto = ?";

        try (Connection conn = conexion.getConnectionStatic();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idTipoGasto);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("No se encontró el tipo de gasto con ID: " + idTipoGasto);
            }

            System.out.println("SUCCESS  Tipo de gasto activado: ID " + idTipoGasto);
        }
    }

    /**
     * Verifica si existe un tipo de gasto con el código dado.
     */
    public boolean existeCodigoTipoGasto(String codigo, Integer excluirId) throws SQLException {
        String sql = excluirId != null
                ? "SELECT COUNT(*) FROM tipos_gastos WHERE codigo = ? AND id_tipo_gasto != ?"
                : "SELECT COUNT(*) FROM tipos_gastos WHERE codigo = ?";

        try (Connection conn = conexion.getConnectionStatic();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, codigo);
            if (excluirId != null) {
                ps.setInt(2, excluirId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTODOS PRIVADOS DE MAPEO
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Mapea ResultSet a ModelGastoOperativo.
     */
    private ModelGastoOperativo mapearGasto(ResultSet rs) throws SQLException {
        ModelGastoOperativo gasto = new ModelGastoOperativo();

        gasto.setIdGasto(rs.getInt("id_gasto"));
        gasto.setIdTipoGasto(rs.getInt("id_tipo_gasto"));
        gasto.setConcepto(rs.getString("concepto"));
        gasto.setMonto(rs.getBigDecimal("monto"));
        gasto.setProveedorPersona(rs.getString("proveedor_persona"));
        gasto.setNumeroRecibo(rs.getString("numero_recibo"));
        gasto.setObservaciones(rs.getString("observaciones"));
        gasto.setIdBodega(rs.getInt("id_bodega"));
        gasto.setIdUsuario(rs.getInt("id_usuario"));

        int idMov = rs.getInt("id_movimiento_caja");
        if (!rs.wasNull()) {
            gasto.setIdMovimientoCaja(idMov);
        }

        gasto.setEstadoFromString(rs.getString("estado"));

        Timestamp tsGasto = rs.getTimestamp("fecha_gasto");
        if (tsGasto != null) {
            gasto.setFechaGasto(tsGasto.toLocalDateTime());
        }

        Timestamp tsCreacion = rs.getTimestamp("fecha_creacion");
        if (tsCreacion != null) {
            gasto.setFechaCreacion(tsCreacion.toLocalDateTime());
        }

        Timestamp tsActualizacion = rs.getTimestamp("fecha_actualizacion");
        if (tsActualizacion != null) {
            gasto.setFechaActualizacion(tsActualizacion.toLocalDateTime());
        }

        // Campos de relación (si existen en el query)
        try {
            gasto.setNombreTipoGasto(rs.getString("nombre_tipo_gasto"));
            gasto.setNombreBodega(rs.getString("nombre_bodega"));
            gasto.setNombreUsuario(rs.getString("nombre_usuario"));
        } catch (SQLException e) {
            // Columnas opcionales
        }

        return gasto;
    }

    /**
     * Mapea ResultSet a ModelTipoGasto.
     */
    private ModelTipoGasto mapearTipoGasto(ResultSet rs) throws SQLException {
        ModelTipoGasto tipo = new ModelTipoGasto();

        tipo.setIdTipoGasto(rs.getInt("id_tipo_gasto"));
        tipo.setCodigo(rs.getString("codigo"));
        tipo.setNombre(rs.getString("nombre"));
        tipo.setDescripcion(rs.getString("descripcion"));
        tipo.setCategoriaFromString(rs.getString("categoria"));
        tipo.setRequiereAutorizacion(rs.getBoolean("requiere_autorizacion"));
        tipo.setMontoMaximoSinAutorizacion(rs.getBigDecimal("monto_maximo_sin_autorizacion"));
        tipo.setCuentaContable(rs.getString("cuenta_contable"));
        tipo.setActivo(rs.getBoolean("activo"));

        Timestamp tsCreacion = rs.getTimestamp("fecha_creacion");
        if (tsCreacion != null) {
            tipo.setFechaCreacion(tsCreacion.toLocalDateTime());
        }

        return tipo;
    }

    /**
     * Genera número de comprobante para gasto.
     */
    private String generarNumeroComprobante(int idGasto) {
        LocalDateTime ahora = LocalDateTime.now();
        return String.format("GAS-%d%02d%02d-%05d",
                ahora.getYear(),
                ahora.getMonthValue(),
                ahora.getDayOfMonth(),
                idGasto);
    }

    /**
     * Cierra recursos de BD de forma segura.
     */
    private void cerrarRecursos(Connection conn, ResultSet rs, PreparedStatement... statements) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                /* ignore */ }
        }
        for (PreparedStatement ps : statements) {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    /* ignore */ }
            }
        }
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException e) {
                /* ignore */ }
        }
    }
}
