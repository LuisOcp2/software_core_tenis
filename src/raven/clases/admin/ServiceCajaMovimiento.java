package raven.clases.admin;

import raven.controlador.admin.ModelCaja;
import raven.controlador.admin.ModelCajaMovimiento;
import raven.controlador.admin.ResumenCierreCaja;
import raven.controlador.principal.conexion;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import raven.controlador.admin.ModelUser;

/**
 * Servicio para gestión de movimientos de caja.
 *
 * Aplica el Principio de Responsabilidad Única (SRP) - solo maneja lógica de
 * movimientos. Aplica el Principio Abierto/Cerrado (OCP) - extensible sin
 * modificar código existente.
 *
 * @author Sistema
 * @version 2.0
 */
public class ServiceCajaMovimiento {

    // ==================== CONSTANTES SQL ====================
    private static final String SQL_INSERTAR_MOVIMIENTO = "INSERT INTO caja_movimientos (id_caja, id_usuario, fecha_apertura, monto_inicial, observaciones) "
            + "VALUES (?, ?, NOW(), ?, ?)";
    private static final String SQL_INSERTAR_MOVIMIENTO_CON_FECHA = "INSERT INTO caja_movimientos (id_caja, id_usuario, fecha_apertura, monto_inicial, observaciones) "
            + "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_OBTENER_MOVIMIENTO_ABIERTO = "SELECT cm.*, c.nombre as nombre_caja, u.nombre as nombre_usuario "
            + "FROM caja_movimientos cm "
            + "INNER JOIN cajas c ON cm.id_caja = c.id_caja "
            + "INNER JOIN usuarios u ON cm.id_usuario = u.id_usuario "
            + "WHERE cm.id_caja = ? AND cm.fecha_cierre IS NULL "
            + "ORDER BY cm.fecha_apertura DESC LIMIT 1";

    private static final String SQL_CERRAR_MOVIMIENTO = "UPDATE caja_movimientos "
            + "SET fecha_cierre = ?, monto_final = ?, observaciones = ? "
            + "WHERE id_movimiento = ?";

    private static final String SQL_OBTENER_TOTAL_VENTAS = "SELECT COALESCE(SUM(total), 0) as total_ventas "
            + "FROM ventas "
            + "WHERE id_movimiento = ? AND estado = 'completada'";

    private static final String SQL_HISTORIAL_MOVIMIENTOS = "SELECT cm.*, c.nombre as nombre_caja, u.nombre as nombre_usuario "
            + "FROM caja_movimientos cm "
            + "INNER JOIN cajas c ON cm.id_caja = c.id_caja "
            + "INNER JOIN usuarios u ON cm.id_usuario = u.id_usuario "
            + "WHERE cm.id_caja = ? "
            + "ORDER BY cm.fecha_apertura DESC "
            + "LIMIT ?";

    private static final String SQL_OBTENER_MOVIMIENTO_POR_ID = "SELECT cm.*, c.nombre as nombre_caja, u.nombre as nombre_usuario "
            + "FROM caja_movimientos cm "
            + "INNER JOIN cajas c ON cm.id_caja = c.id_caja "
            + "INNER JOIN usuarios u ON cm.id_usuario = u.id_usuario "
            + "WHERE cm.id_movimiento = ?";
    // ==================== NUEVO SQL ====================
    private static final String SQL_OBTENER_MOVIMIENTO_ABIERTO_CU = "SELECT cm.*, c.nombre as nombre_caja, u.nombre as nombre_usuario "
            + "FROM caja_movimientos cm "
            + "INNER JOIN cajas c ON cm.id_caja = c.id_caja "
            + "INNER JOIN usuarios u ON cm.id_usuario = u.id_usuario "
            + "WHERE cm.id_caja = ? AND cm.id_usuario = ? AND cm.fecha_cierre IS NULL "
            + "ORDER BY cm.fecha_apertura DESC LIMIT 1";

    // ==================== SQL PARA EFECTIVO DISPONIBLE ====================
    // Ventas en efectivo completadas (desde venta_medios_pago para capturar pagos
    // mixtos)
    private static final String SQL_VENTAS_EFECTIVO = "SELECT COALESCE(SUM(vmp.monto), 0) as total_efectivo " +
            "FROM venta_medios_pago vmp " +
            "INNER JOIN ventas v ON vmp.id_venta = v.id_venta " +
            "WHERE v.id_movimiento = ? AND v.estado = 'completada' " +
            "AND vmp.tipo_pago = 'efectivo' AND vmp.activo = 1";

    // Gastos operativos vigentes del movimiento
    private static final String SQL_GASTOS_VIGENTES = "SELECT COALESCE(SUM(go.monto), 0) as total_gastos " +
            "FROM gastos_operativos go " +
            "INNER JOIN caja_movimiento_detalle cmd ON go.id_movimiento_caja = cmd.id_detalle_movimiento " +
            "WHERE cmd.id_movimiento_caja = ? AND go.estado != 'anulado' AND cmd.activo = 1";

    // Compras externas vigentes del movimiento
    private static final String SQL_COMPRAS_VIGENTES = "SELECT COALESCE(SUM(ce.total), 0) as total_compras " +
            "FROM compras_externas ce " +
            "INNER JOIN caja_movimiento_detalle cmd ON cmd.id_referencia = ce.id_compra_externa " +
            "AND cmd.tipo_referencia = 'compra_externa' " +
            "WHERE cmd.id_movimiento_caja = ? AND ce.estado != 'cancelada' AND cmd.activo = 1";

    // ==================== MÉTODOS DE APERTURA ====================
    /**
     * Abre un nuevo movimiento de caja.
     *
     * @param idCaja        ID de la caja a abrir
     * @param idUsuario     ID del usuario que abre
     * @param montoInicial  Monto base inicial
     * @param observaciones Observaciones de apertura
     * @return ModelCajaMovimiento creado con su ID asignado
     * @throws SQLException Si hay error en BD o la caja ya está abierta
     */
    public ModelCajaMovimiento abrirCaja(
            Integer idCaja,
            Integer idUsuario,
            BigDecimal montoInicial,
            String observaciones) throws SQLException {

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conexion.getInstance().connectToDatabase();
            conn = conexion.getInstance().getConnection();

            // 1. Validar que no haya un movimiento abierto
            ModelCajaMovimiento movimientoExistente = obtenerMovimientoAbierto(idCaja);
            if (movimientoExistente != null) {
                throw new SQLException(
                        "La caja ya tiene un movimiento abierto desde "
                                + movimientoExistente.getFechaApertura());
            }

            // 2. Crear nuevo movimiento
            ps = conn.prepareStatement(SQL_INSERTAR_MOVIMIENTO, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, idCaja);
            ps.setInt(2, idUsuario);
            ps.setBigDecimal(3, montoInicial);
            ps.setString(4, observaciones);

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("Error al crear el movimiento de caja");
            }

            // 3. Obtener ID generado
            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int idMovimiento = rs.getInt(1);

                // 4. Retornar modelo completo
                return obtenerMovimientoPorId(idMovimiento);
            }

            throw new SQLException("No se pudo obtener el ID del movimiento creado");

        } finally {
            conexion.getInstance().close(rs, ps, conn);
        }
    }

    public ModelCajaMovimiento abrirCajaConFecha(
            Integer idCaja,
            Integer idUsuario,
            BigDecimal montoInicial,
            String observaciones,
            java.time.LocalDateTime fechaApertura) throws SQLException {

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conexion.getInstance().connectToDatabase();
            conn = conexion.getInstance().getConnection();

            ModelCajaMovimiento movimientoExistente = obtenerMovimientoAbierto(idCaja);
            if (movimientoExistente != null) {
                throw new SQLException(
                        "La caja ya tiene un movimiento abierto desde "
                                + movimientoExistente.getFechaApertura());
            }

            ps = conn.prepareStatement(SQL_INSERTAR_MOVIMIENTO_CON_FECHA, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, idCaja);
            ps.setInt(2, idUsuario);
            java.sql.Timestamp ts = java.sql.Timestamp.valueOf(
                    fechaApertura != null ? fechaApertura : java.time.LocalDateTime.now());
            ps.setTimestamp(3, ts);
            ps.setBigDecimal(4, montoInicial);
            ps.setString(5, observaciones);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Error al crear el movimiento de caja");
            }

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int idMovimiento = rs.getInt(1);
                return obtenerMovimientoPorId(idMovimiento);
            }
            throw new SQLException("No se pudo obtener el ID del movimiento creado");
        } finally {
            conexion.getInstance().close(rs, ps, conn);
        }
    }

    // ==================== NUEVA SOBRECARGA ====================
    /** Obtiene el movimiento abierto filtrando por caja y usuario. */
    public ModelCajaMovimiento obtenerMovimientoAbierto(int idCaja, int idUsuario) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = conexion.getInstance().getConnection();
            ps = conn.prepareStatement(SQL_OBTENER_MOVIMIENTO_ABIERTO_CU);
            ps.setInt(1, idCaja);
            ps.setInt(2, idUsuario);

            rs = ps.executeQuery();
            if (rs.next()) {
                return mapearMovimiento(rs);
            }
            return null;

        } finally {
            conexion.getInstance().close(rs, ps, conn);
        }
    }

    /**
     * Obtiene el movimiento de caja actualmente abierto.
     *
     * @param idCaja ID de la caja
     * @return ModelCajaMovimiento abierto, o null si no hay ninguno
     * @throws SQLException Si hay error en BD
     */
    public ModelCajaMovimiento obtenerMovimientoAbierto(Integer idCaja) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = conexion.getInstance().getConnection();
            ps = conn.prepareStatement(SQL_OBTENER_MOVIMIENTO_ABIERTO);
            ps.setInt(1, idCaja);

            rs = ps.executeQuery();

            if (rs.next()) {
                return mapearMovimiento(rs);
            }

            return null;

        } finally {
            conexion.getInstance().close(rs, ps, conn);
        }
    }

    // ==================== MÉTODOS DE CIERRE ====================
    /**
     * Cierra un movimiento de caja.
     *
     * @param idMovimiento  ID del movimiento a cerrar
     * @param montoFinal    Monto final contado
     * @param observaciones Observaciones del cierre
     * @return ModelCajaMovimiento actualizado con datos de cierre
     * @throws SQLException Si hay error en BD
     */
    public ModelCajaMovimiento cerrarCaja(
            Integer idMovimiento,
            BigDecimal montoFinal,
            String observaciones) throws SQLException {

        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = conexion.getInstance().getConnection();
            conn.setAutoCommit(false);

            // 1. Validar que el movimiento existe y está abierto
            ModelCajaMovimiento movimiento = obtenerMovimientoPorId(idMovimiento);
            if (movimiento == null) {
                throw new SQLException("Movimiento no encontrado");
            }
            if (!movimiento.estaAbierto()) {
                throw new SQLException("El movimiento ya está cerrado");
            }

            // 2. Obtener total de ventas
            BigDecimal totalVentas = obtenerTotalVentas(idMovimiento);
            movimiento.setTotalVentas(totalVentas);

            // 3. Actualizar movimiento con datos de cierre
            ps = conn.prepareStatement(SQL_CERRAR_MOVIMIENTO);
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setBigDecimal(2, montoFinal);
            ps.setString(3, observaciones);
            ps.setInt(4, idMovimiento);

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("Error al cerrar el movimiento");
            }

            conn.commit();

            // 4. Retornar modelo actualizado
            return obtenerMovimientoPorId(idMovimiento);

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error en rollback: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
            }
            conexion.getInstance().close(ps, conn);
        }
    }

    /**
     * Obtiene el total de ventas de un movimiento de caja.
     *
     * @param idMovimiento ID del movimiento
     * @return Total de ventas completadas
     * @throws SQLException Si hay error en BD
     */
    private BigDecimal obtenerTotalVentas(Integer idMovimiento) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = conexion.getInstance().getConnection();
            ps = conn.prepareStatement(SQL_OBTENER_TOTAL_VENTAS);
            ps.setInt(1, idMovimiento);

            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("total_ventas");
            }

            return BigDecimal.ZERO;

        } finally {
            conexion.getInstance().close(rs, ps, conn);
        }
    }

    // ==================== MÉTODOS DE CONSULTA ====================
    /**
     * Obtiene un movimiento por su ID.
     *
     * @param idMovimiento ID del movimiento
     * @return ModelCajaMovimiento encontrado, o null
     * @throws SQLException Si hay error en BD
     */
    public ModelCajaMovimiento obtenerMovimientoPorId(Integer idMovimiento) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = conexion.getInstance().getConnection();
            ps = conn.prepareStatement(SQL_OBTENER_MOVIMIENTO_POR_ID);
            ps.setInt(1, idMovimiento);

            rs = ps.executeQuery();

            if (rs.next()) {
                ModelCajaMovimiento movimiento = mapearMovimiento(rs);

                // Calcular total de ventas siempre (abierto o cerrado) para reportes
                movimiento.setTotalVentas(obtenerTotalVentas(idMovimiento));

                return movimiento;
            }

            return null;

        } finally {
            conexion.getInstance().close(rs, ps, conn);
        }
    }

    /**
     * Obtiene el historial de movimientos de una caja.
     *
     * @param idCaja ID de la caja
     * @param limite Cantidad máxima de registros
     * @return Lista de movimientos históricos
     * @throws SQLException Si hay error en BD
     */
    public List<ModelCajaMovimiento> obtenerHistorialMovimientos(
            Integer idCaja, int limite) throws SQLException {

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = conexion.getInstance().getConnection();
            ps = conn.prepareStatement(SQL_HISTORIAL_MOVIMIENTOS);
            ps.setInt(1, idCaja);
            ps.setInt(2, limite);

            rs = ps.executeQuery();

            List<ModelCajaMovimiento> movimientos = new ArrayList<>();
            while (rs.next()) {
                movimientos.add(mapearMovimiento(rs));
            }

            return movimientos;

        } finally {
            conexion.getInstance().close(rs, ps, conn);
        }
    }

    // ==================== MÉTODO PARA VALIDACIÓN DE EFECTIVO ====================
    /**
     * Calcula el efectivo disponible en caja para un movimiento.
     * 
     * Fórmula: montoInicial + ventasEfectivo - gastosOperativos - comprasExternas
     * 
     * Este método es crucial para validar si hay fondos suficientes antes de
     * registrar gastos o compras externas.
     * 
     * @param idMovimiento ID del movimiento de caja
     * @return Efectivo disponible como BigDecimal (nunca null, mínimo ZERO)
     * @throws SQLException Si hay error en BD
     */
    public BigDecimal obtenerEfectivoDisponible(Integer idMovimiento) throws SQLException {
        if (idMovimiento == null || idMovimiento <= 0) {
            return BigDecimal.ZERO;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = conexion.getInstance().getConnection();

            // 1. Obtener monto inicial del movimiento
            ModelCajaMovimiento mov = obtenerMovimientoPorId(idMovimiento);
            if (mov == null) {
                System.err.println("WARNING  Movimiento no encontrado: " + idMovimiento);
                return BigDecimal.ZERO;
            }
            BigDecimal montoInicial = BigDecimal.valueOf(mov.getMontoInicial());

            // 2. Obtener ventas en efectivo
            BigDecimal ventasEfectivo = BigDecimal.ZERO;
            ps = conn.prepareStatement(SQL_VENTAS_EFECTIVO);
            ps.setInt(1, idMovimiento);
            rs = ps.executeQuery();
            if (rs.next()) {
                ventasEfectivo = rs.getBigDecimal("total_efectivo");
                if (ventasEfectivo == null)
                    ventasEfectivo = BigDecimal.ZERO;
            }
            rs.close();
            ps.close();

            // 3. Obtener gastos operativos vigentes
            BigDecimal gastosVigentes = BigDecimal.ZERO;
            ps = conn.prepareStatement(SQL_GASTOS_VIGENTES);
            ps.setInt(1, idMovimiento);
            rs = ps.executeQuery();
            if (rs.next()) {
                gastosVigentes = rs.getBigDecimal("total_gastos");
                if (gastosVigentes == null)
                    gastosVigentes = BigDecimal.ZERO;
            }
            rs.close();
            ps.close();

            // 4. Obtener compras externas vigentes
            BigDecimal comprasVigentes = BigDecimal.ZERO;
            ps = conn.prepareStatement(SQL_COMPRAS_VIGENTES);
            ps.setInt(1, idMovimiento);
            rs = ps.executeQuery();
            if (rs.next()) {
                comprasVigentes = rs.getBigDecimal("total_compras");
                if (comprasVigentes == null)
                    comprasVigentes = BigDecimal.ZERO;
            }
            rs.close();
            ps.close();

            // 5. Calcular efectivo disponible
            BigDecimal efectivoDisponible = montoInicial
                    .add(ventasEfectivo)
                    .subtract(gastosVigentes)
                    .subtract(comprasVigentes);

            System.out.println("Efectivo Cálculo de efectivo disponible:");
            System.out.println("   Monto Inicial:     $" + montoInicial);
            System.out.println("   + Ventas Efectivo: $" + ventasEfectivo);
            System.out.println("   - Gastos:          $" + gastosVigentes);
            System.out.println("   - Compras:         $" + comprasVigentes);
            System.out.println("   = DISPONIBLE:      $" + efectivoDisponible);

            // No retornar valores negativos
            return efectivoDisponible.compareTo(BigDecimal.ZERO) < 0
                    ? BigDecimal.ZERO
                    : efectivoDisponible;

        } finally {
            conexion.getInstance().close(rs, ps, conn);
        }
    }

    public BigDecimal obtenerSaldoTotalDisponible(Integer idMovimiento) throws SQLException {
        if (idMovimiento == null || idMovimiento <= 0) {
            return BigDecimal.ZERO;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = conexion.getInstance().getConnection();

            ModelCajaMovimiento mov = obtenerMovimientoPorId(idMovimiento);
            if (mov == null) {
                System.err.println("WARNING  Movimiento no encontrado: " + idMovimiento);
                return BigDecimal.ZERO;
            }
            BigDecimal montoInicial = BigDecimal.valueOf(mov.getMontoInicial());

            BigDecimal totalVentas = BigDecimal.ZERO;
            ps = conn.prepareStatement(SQL_OBTENER_TOTAL_VENTAS);
            ps.setInt(1, idMovimiento);
            rs = ps.executeQuery();
            if (rs.next()) {
                totalVentas = rs.getBigDecimal("total_ventas");
                if (totalVentas == null) {
                    totalVentas = BigDecimal.ZERO;
                }
            }
            rs.close();
            ps.close();

            BigDecimal gastosVigentes = BigDecimal.ZERO;
            ps = conn.prepareStatement(SQL_GASTOS_VIGENTES);
            ps.setInt(1, idMovimiento);
            rs = ps.executeQuery();
            if (rs.next()) {
                gastosVigentes = rs.getBigDecimal("total_gastos");
                if (gastosVigentes == null) {
                    gastosVigentes = BigDecimal.ZERO;
                }
            }
            rs.close();
            ps.close();

            BigDecimal comprasVigentes = BigDecimal.ZERO;
            ps = conn.prepareStatement(SQL_COMPRAS_VIGENTES);
            ps.setInt(1, idMovimiento);
            rs = ps.executeQuery();
            if (rs.next()) {
                comprasVigentes = rs.getBigDecimal("total_compras");
                if (comprasVigentes == null) {
                    comprasVigentes = BigDecimal.ZERO;
                }
            }
            rs.close();
            ps.close();

            BigDecimal saldoDisponible = montoInicial
                    .add(totalVentas)
                    .subtract(gastosVigentes)
                    .subtract(comprasVigentes);

            System.out.println("Saldo Cálculo de saldo total disponible:");
            System.out.println("   Monto Inicial:     $" + montoInicial);
            System.out.println("   + Ventas Totales:  $" + totalVentas);
            System.out.println("   - Gastos:          $" + gastosVigentes);
            System.out.println("   - Compras:         $" + comprasVigentes);
            System.out.println("   = DISPONIBLE:      $" + saldoDisponible);

            return saldoDisponible.compareTo(BigDecimal.ZERO) < 0
                    ? BigDecimal.ZERO
                    : saldoDisponible;

        } finally {
            conexion.getInstance().close(rs, ps, conn);
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================
    /**
     * Mapea un ResultSet a un ModelCajaMovimiento.
     *
     * @param rs ResultSet con datos del movimiento
     * @return ModelCajaMovimiento mapeado
     * @throws SQLException Si hay error al leer el ResultSet
     */
    /**
     * Mapea un ResultSet a un objeto ModelCajaMovimiento.
     *
     * PRINCIPIO: Single Responsibility - Solo mapea datos, no valida lógica de
     * negocio CORRECCIÓN: Adaptado a los atributos reales del modelo
     * (montoInicial/montoFinal)
     *
     * @param rs ResultSet con los datos del movimiento
     * @return ModelCajaMovimiento mapeado correctamente
     * @throws SQLException si hay errores de base de datos
     */
    private ModelCajaMovimiento mapearMovimiento(ResultSet rs) throws SQLException {
        if (rs == null) {
            throw new IllegalArgumentException("ResultSet no puede ser nulo");
        }

        ModelCajaMovimiento movimiento = new ModelCajaMovimiento();

        try {
            // =====================================================
            // 1. MAPEAR DATOS BÁSICOS DEL MOVIMIENTO
            // =====================================================
            movimiento.setIdMovimiento(rs.getInt("id_movimiento"));

            // =====================================================
            // 2. MAPEAR CAJA - VALIDACIÓN DEFENSIVA
            // =====================================================
            int idCaja = rs.getInt("id_caja");

            if (rs.wasNull() || idCaja <= 0) {
                throw new SQLException(
                        "Movimiento sin caja asignada - ID Movimiento: "
                                + movimiento.getIdMovimiento());
            }

            // Crear objeto ModelCaja correctamente
            ModelCaja caja = new ModelCaja();
            caja.setIdCaja(idCaja);

            // Intentar obtener nombre de caja (si está en el JOIN)
            try {
                String nombreCaja = rs.getString("nombre_caja");
                if (nombreCaja != null && !nombreCaja.isEmpty()) {
                    caja.setNombre(nombreCaja);
                    movimiento.setNombreCaja(nombreCaja); // SUCCESS Sincronizar con atributo auxiliar
                } else {
                    String nombreDefault = "Caja #" + idCaja;
                    caja.setNombre(nombreDefault);
                    movimiento.setNombreCaja(nombreDefault);
                }
            } catch (SQLException e) {
                // La columna nombre_caja no existe en el JOIN
                String nombreDefault = "Caja #" + idCaja;
                caja.setNombre(nombreDefault);
                movimiento.setNombreCaja(nombreDefault);
            }

            movimiento.setCaja(caja);

            // =====================================================
            // 3. MAPEAR USUARIO
            // =====================================================
            int idUsuario = rs.getInt("id_usuario");

            if (!rs.wasNull() && idUsuario > 0) {
                ModelUser usuario = new ModelUser();
                usuario.setIdUsuario(idUsuario);

                // Intentar obtener nombre de usuario (si está en el JOIN)
                try {
                    String nombreUsuario = rs.getString("nombre_usuario");
                    if (nombreUsuario != null && !nombreUsuario.isEmpty()) {
                        usuario.setNombre(nombreUsuario);
                        movimiento.setNombreUsuario(nombreUsuario); // SUCCESS Sincronizar
                    } else {
                        String nombreDefault = "Usuario #" + idUsuario;
                        usuario.setNombre(nombreDefault);
                        movimiento.setNombreUsuario(nombreDefault);
                    }
                } catch (SQLException e) {
                    String nombreDefault = "Usuario #" + idUsuario;
                    usuario.setNombre(nombreDefault);
                    movimiento.setNombreUsuario(nombreDefault);
                }

                movimiento.setUsuario(usuario);
            }

            // =====================================================
            // 4. MAPEAR FECHAS
            // =====================================================
            Timestamp tsApertura = rs.getTimestamp("fecha_apertura");
            movimiento.setFechaApertura(
                    tsApertura != null ? tsApertura.toLocalDateTime() : null);

            Timestamp tsCierre = rs.getTimestamp("fecha_cierre");
            movimiento.setFechaCierre(
                    tsCierre != null ? tsCierre.toLocalDateTime() : null);

            // =====================================================
            // 5. MAPEAR MONTOS (CORRECCIÓN CRÍTICA)
            // =====================================================
            // IMPORTANTE: Usar montoInicial y montoFinal (double)
            // NO usar montoApertura/montoCierre que no existen
            double montoInicial = rs.getDouble("monto_inicial");
            if (!rs.wasNull()) {
                movimiento.setMontoInicial(montoInicial);
            } else {
                movimiento.setMontoInicial(0.0);
            }

            double montoFinal = rs.getDouble("monto_final");
            if (!rs.wasNull()) {
                movimiento.setMontoFinal(montoFinal);
            } else {
                movimiento.setMontoFinal(0.0);
            }

            // =====================================================
            // 6. MAPEAR CAMPOS ADICIONALES (BIGDECIMAL)
            // =====================================================
            // Total de ventas (si existe en la consulta)
            try {
                BigDecimal totalVentas = rs.getBigDecimal("total_ventas");
                if (totalVentas != null) {
                    movimiento.setTotalVentas(totalVentas);
                }
            } catch (SQLException e) {
                // Columna no existe, ignorar
            }

            // Diferencia (si existe en la consulta)
            try {
                BigDecimal diferencia = rs.getBigDecimal("diferencia");
                if (diferencia != null) {
                    movimiento.setDiferencia(diferencia);
                } else {
                    // Calcular diferencia si hay datos suficientes
                    if (movimiento.getFechaCierre() != null) {
                        movimiento.calcularDiferencia();
                    }
                }
            } catch (SQLException e) {
                // Columna no existe, calcular si está cerrado
                if (movimiento.getFechaCierre() != null) {
                    movimiento.calcularDiferencia();
                }
            }

            // =====================================================
            // 7. MAPEAR OBSERVACIONES
            // =====================================================
            String observaciones = rs.getString("observaciones");
            movimiento.setObservaciones(observaciones);

            System.out.println("SUCCESS  Movimiento mapeado correctamente - ID: "
                    + movimiento.getIdMovimiento()
                    + " | Caja: " + movimiento.getNombreCaja()
                    + " | Usuario: " + movimiento.getNombreUsuario());

            return movimiento;

        } catch (SQLException e) {
            System.err.println("ERROR  Error mapeando movimiento: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-lanzar para manejo superior
        }
    }

    // ==================== MÉTODOS PARA RESUMEN DE CIERRE ====================

    // SQL para obtener resumen de ventas por tipo de pago
    private static final String SQL_RESUMEN_VENTAS_POR_TIPO = "SELECT tipo_pago, COUNT(*) as cantidad, SUM(total) as total "
            +
            "FROM ventas " +
            "WHERE id_movimiento = ? AND estado = 'completada' " +
            "GROUP BY tipo_pago";

    // SQL para obtener desglose detallado de medios de pago (para pagos mixtos)
    private static final String SQL_DESGLOSE_MEDIOS_PAGO = "SELECT vmp.tipo_pago, COUNT(*) as cantidad, SUM(vmp.monto) as total "
            +
            "FROM venta_medios_pago vmp " +
            "INNER JOIN ventas v ON vmp.id_venta = v.id_venta " +
            "WHERE v.id_movimiento = ? AND v.estado = 'completada' AND vmp.activo = 1 " +
            "GROUP BY vmp.tipo_pago";

    // SQL para contar ventas totales
    private static final String SQL_CONTAR_VENTAS = "SELECT COUNT(*) as total_ventas FROM ventas " +
            "WHERE id_movimiento = ? AND estado = 'completada'";

    // SQL para obtener resumen de productos vendidos por movimiento
    private static final String SQL_RESUMEN_PRODUCTOS_VENDIDOS = "SELECT p.nombre, SUM(vd.cantidad) as cantidad, " +
            "vd.tipo_venta, SUM(vd.subtotal) as total " +
            "FROM venta_detalles vd " +
            "INNER JOIN ventas v ON vd.id_venta = v.id_venta " +
            "INNER JOIN productos p ON vd.id_producto = p.id_producto " +
            "WHERE v.id_movimiento = ? AND v.estado = 'completada' " +
            "GROUP BY p.id_producto, p.nombre, vd.tipo_venta";

    // SQL para obtener resumen de gastos operativos por movimiento de caja
    // Los gastos se vinculan a caja_movimiento_detalle con tipo_movimiento =
    // 'salida_gasto'
    private static final String SQL_RESUMEN_GASTOS = "SELECT COUNT(go.id_gasto) as total_gastos, " +
            "COALESCE(SUM(go.monto), 0) as monto_total " +
            "FROM gastos_operativos go " +
            "INNER JOIN caja_movimiento_detalle cmd ON go.id_movimiento_caja = cmd.id_detalle_movimiento " +
            "WHERE cmd.id_movimiento_caja = ? AND go.estado != 'anulado'";

    // SQL para obtener resumen de compras externas por movimiento de caja
    // Las compras se vinculan a caja_movimiento_detalle con tipo_movimiento =
    // 'salida_compra_externa'
    private static final String SQL_RESUMEN_COMPRAS = "SELECT COUNT(DISTINCT ce.id_compra_externa) as total_compras, " +
            "COALESCE(SUM(ce.total), 0) as monto_total " +
            "FROM compras_externas ce " +
            "INNER JOIN caja_movimiento_detalle cmd ON cmd.id_referencia = ce.id_compra_externa " +
            "AND cmd.tipo_referencia = 'compra_externa' " +
            "WHERE cmd.id_movimiento_caja = ? AND ce.estado != 'cancelada' AND cmd.activo = 1";

    /**
     * Obtiene un resumen completo del cierre de caja con desglose por tipo de pago.
     * 
     * @param idMovimiento ID del movimiento de caja
     * @return ResumenCierreCaja con información detallada
     * @throws SQLException Si hay error en la BD
     */
    public ResumenCierreCaja obtenerResumenCierreCaja(Integer idMovimiento) throws SQLException {
        ResumenCierreCaja resumen = new ResumenCierreCaja(idMovimiento);

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = conexion.getInstance().getConnection();

            // 1. Obtener conteo total de ventas
            ps = conn.prepareStatement(SQL_CONTAR_VENTAS);
            ps.setInt(1, idMovimiento);
            rs = ps.executeQuery();
            if (rs.next()) {
                resumen.setTotalVentas(rs.getInt("total_ventas"));
            }
            rs.close();
            ps.close();

            // 2. Obtener monto total de ventas
            BigDecimal totalVentas = obtenerTotalVentas(idMovimiento);
            resumen.setMontoTotalVentas(totalVentas);

            // 3. Obtener desglose por tipo de pago de ventas
            ps = conn.prepareStatement(SQL_RESUMEN_VENTAS_POR_TIPO);
            ps.setInt(1, idMovimiento);
            rs = ps.executeQuery();

            while (rs.next()) {
                String tipoPago = rs.getString("tipo_pago");
                int cantidad = rs.getInt("cantidad");
                BigDecimal monto = rs.getBigDecimal("total");

                if (monto != null) {
                    resumen.agregarDetallePago(tipoPago, cantidad, monto);
                }
            }
            rs.close();
            ps.close();

            // 4. Obtener desglose de medios de pago (para ventas mixtas)
            ps = conn.prepareStatement(SQL_DESGLOSE_MEDIOS_PAGO);
            ps.setInt(1, idMovimiento);
            rs = ps.executeQuery();

            int totalPagos = 0;
            while (rs.next()) {
                // Solo contamos los pagos, el desglose ya está en el resumen por tipo de venta
                int cantidad = rs.getInt("cantidad");
                totalPagos += cantidad;
            }
            rs.close();
            ps.close();

            // Si hay pagos mixtos registrados, usar ese conteo
            if (totalPagos > 0) {
                resumen.setTotalPagosRecibidos(totalPagos);
            } else {
                // Si no hay venta_medios_pago, calcular desde detalles
                resumen.calcularTotalPagosRecibidos();
            }

            // 5. Obtener resumen de productos vendidos
            ps = conn.prepareStatement(SQL_RESUMEN_PRODUCTOS_VENDIDOS);
            ps.setInt(1, idMovimiento);
            rs = ps.executeQuery();
            while (rs.next()) {
                String nombre = rs.getString("nombre");
                int cantidad = rs.getInt("cantidad");
                String unidad = rs.getString("tipo_venta");
                BigDecimal monto = rs.getBigDecimal("total");

                if (unidad == null || unidad.trim().isEmpty()) {
                    unidad = "pares";
                } else if (unidad.toLowerCase().contains("par")) {
                    unidad = "pares";
                } else if (unidad.toLowerCase().contains("caja")) {
                    unidad = "cajas";
                }

                resumen.agregarProductoVendido(new ResumenCierreCaja.DetalleProducto(
                        nombre, cantidad, unidad, monto));
            }
            rs.close();
            ps.close();

            System.out.println("SUCCESS  Resumen de cierre obtenido: ");
            System.out.println(resumen);

            return resumen;

        } finally {
            conexion.getInstance().close(rs, ps, conn);
        }
    }

    /**
     * Obtiene resumen de cierre con información de movimientos de caja detalle.
     * Incluye gastos operativos y compras externas registradas en
     * caja_movimiento_detalle.
     * 
     * @param idMovimiento ID del movimiento de caja
     * @return ResumenCierreCaja con toda la información incluyendo gastos y compras
     * @throws SQLException Si hay error en BD
     */
    public ResumenCierreCaja obtenerResumenCompletoConMovimientos(Integer idMovimiento) throws SQLException {
        ResumenCierreCaja resumen = obtenerResumenCierreCaja(idMovimiento);

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = conexion.getInstance().getConnection();

            // ========================================
            // 1. OBTENER RESUMEN DE GASTOS OPERATIVOS
            // ========================================
            ps = conn.prepareStatement(SQL_RESUMEN_GASTOS);
            ps.setInt(1, idMovimiento);
            rs = ps.executeQuery();

            if (rs.next()) {
                resumen.setTotalGastos(rs.getInt("total_gastos"));
                resumen.setMontoTotalGastos(rs.getBigDecimal("monto_total"));
            }
            rs.close();
            ps.close();

            // ========================================
            // 2. OBTENER RESUMEN DE COMPRAS EXTERNAS
            // ========================================
            ps = conn.prepareStatement(SQL_RESUMEN_COMPRAS);
            ps.setInt(1, idMovimiento);
            rs = ps.executeQuery();

            if (rs.next()) {
                resumen.setTotalCompras(rs.getInt("total_compras"));
                resumen.setMontoTotalCompras(rs.getBigDecimal("monto_total"));
            }
            rs.close();
            ps.close();

            System.out.println("SUCCESS  Resumen completo con gastos y compras obtenido:");
            System.out.println("   - Gastos: " + resumen.getTotalGastos() + " → $" + resumen.getMontoTotalGastos());
            System.out.println("   - Compras: " + resumen.getTotalCompras() + " → $" + resumen.getMontoTotalCompras());

        } finally {
            conexion.getInstance().close(rs, ps, conn);
        }

        return resumen;
    }
}
