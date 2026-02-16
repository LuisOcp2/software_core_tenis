package raven.clases.reportes;

import raven.controlador.admin.ModelCaja;
import raven.controlador.admin.ModelCajaMovimiento;
import raven.controlador.admin.ResumenCierreCaja;
import raven.controlador.principal.conexion;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio para reportes y monitoreo de cajas.
 * 
 * Proporciona métodos para:
 * - Consultar cajas activas con su estado actual
 * - Obtener detalle de movimientos abiertos
 * - Historial de cierres con filtros
 * - Estadísticas agregadas
 * 
 * @author Sistema
 * @version 1.0
 */
public class ServiceReporteCaja {

    // ==================== SQL CAJAS ACTIVAS ====================
    private static final String SQL_CAJAS_ACTIVAS = "SELECT c.id_caja, c.nombre AS nombre_caja, c.ubicacion, " +
            "       cm.id_movimiento, cm.id_usuario, u.nombre AS nombre_usuario, " +
            "       cm.fecha_apertura, cm.monto_inicial, " +
            "       COALESCE((SELECT SUM(v.total) FROM ventas v WHERE v.id_movimiento = cm.id_movimiento AND v.estado = 'completada'), 0) AS total_ventas, "
            +
            "       COALESCE((SELECT COUNT(*) FROM ventas v WHERE v.id_movimiento = cm.id_movimiento AND v.estado = 'completada'), 0) AS cantidad_ventas, "
            +
            "       COALESCE((SELECT SUM(go.monto) FROM gastos_operativos go " +
            "                 INNER JOIN caja_movimiento_detalle cmd ON go.id_movimiento_caja = cmd.id_detalle_movimiento "
            +
            "                 WHERE cmd.id_movimiento_caja = cm.id_movimiento AND go.estado != 'anulado' AND cmd.activo = 1), 0) AS total_gastos, "
            +
            "       COALESCE((SELECT SUM(ce.total) FROM compras_externas ce " +
            "                 INNER JOIN caja_movimiento_detalle cmd ON cmd.id_referencia = ce.id_compra_externa " +
            "                 AND cmd.tipo_referencia = 'compra_externa' " +
            "                 WHERE cmd.id_movimiento_caja = cm.id_movimiento AND ce.estado != 'cancelada' AND cmd.activo = 1), 0) AS total_compras "
            +
            "FROM cajas c " +
            "INNER JOIN caja_movimientos cm ON c.id_caja = cm.id_caja " +
            "INNER JOIN usuarios u ON cm.id_usuario = u.id_usuario " +
            "WHERE c.activa = 1 AND cm.fecha_cierre IS NULL " +
            "ORDER BY c.nombre";

    // SQL para historial de cierres
    private static final String SQL_HISTORIAL_CIERRES_BASE = "SELECT cm.id_movimiento, c.id_caja, c.nombre AS nombre_caja, "
            +
            "       u.nombre AS nombre_usuario, cm.fecha_apertura, cm.fecha_cierre, " +
            "       cm.monto_inicial, cm.monto_final, " +
            "       COALESCE((SELECT SUM(v.total) FROM ventas v WHERE v.id_movimiento = cm.id_movimiento AND v.estado = 'completada'), 0) AS total_ventas, "
            +
            "       COALESCE((SELECT COUNT(*) FROM ventas v WHERE v.id_movimiento = cm.id_movimiento AND v.estado = 'completada'), 0) AS cantidad_ventas, "
            +
            "       COALESCE((SELECT SUM(go.monto) FROM gastos_operativos go " +
            "                 INNER JOIN caja_movimiento_detalle cmd ON go.id_movimiento_caja = cmd.id_detalle_movimiento "
            +
            "                 WHERE cmd.id_movimiento_caja = cm.id_movimiento AND go.estado != 'anulado' AND cmd.activo = 1), 0) AS total_gastos, "
            +
            "       COALESCE((SELECT SUM(ce.total) FROM compras_externas ce " +
            "                 INNER JOIN caja_movimiento_detalle cmd ON cmd.id_referencia = ce.id_compra_externa " +
            "                 AND cmd.tipo_referencia = 'compra_externa' " +
            "                 WHERE cmd.id_movimiento_caja = cm.id_movimiento AND ce.estado != 'cancelada' AND cmd.activo = 1), 0) AS total_compras "
            +
            "FROM caja_movimientos cm " +
            "INNER JOIN cajas c ON cm.id_caja = c.id_caja " +
            "INNER JOIN usuarios u ON cm.id_usuario = u.id_usuario " +
            "WHERE cm.fecha_cierre IS NOT NULL ";

    // SQL para desglose de pagos de un movimiento
    private static final String SQL_DESGLOSE_PAGOS = "SELECT vmp.tipo_pago, COUNT(*) AS cantidad, SUM(vmp.monto) AS total "
            +
            "FROM venta_medios_pago vmp " +
            "INNER JOIN ventas v ON vmp.id_venta = v.id_venta " +
            "WHERE v.id_movimiento = ? AND v.estado = 'completada' AND vmp.activo = 1 " +
            "GROUP BY vmp.tipo_pago " +
            "ORDER BY total DESC";

    // ==================== MÉTODOS PÚBLICOS ====================

    /**
     * Obtiene la lista de cajas activas (con movimiento abierto).
     * 
     * @return Lista de datos de cajas activas con sus montos actuales
     */
    public List<Map<String, Object>> obtenerCajasActivas() throws SQLException {
        List<Map<String, Object>> cajasActivas = new ArrayList<>();

        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement ps = conn.prepareStatement(SQL_CAJAS_ACTIVAS);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> cajaData = new HashMap<>();

                cajaData.put("idCaja", rs.getInt("id_caja"));
                cajaData.put("nombreCaja", rs.getString("nombre_caja"));
                cajaData.put("ubicacion", rs.getString("ubicacion"));
                cajaData.put("idMovimiento", rs.getInt("id_movimiento"));
                cajaData.put("nombreUsuario", rs.getString("nombre_usuario"));

                Timestamp tsApertura = rs.getTimestamp("fecha_apertura");
                cajaData.put("fechaApertura", tsApertura != null ? tsApertura.toLocalDateTime() : null);

                BigDecimal montoInicial = rs.getBigDecimal("monto_inicial");
                BigDecimal totalVentas = rs.getBigDecimal("total_ventas");
                BigDecimal totalGastos = rs.getBigDecimal("total_gastos");
                BigDecimal totalCompras = rs.getBigDecimal("total_compras");

                cajaData.put("montoInicial", montoInicial);
                cajaData.put("totalVentas", totalVentas);
                cajaData.put("cantidadVentas", rs.getInt("cantidad_ventas"));
                cajaData.put("totalGastos", totalGastos);
                cajaData.put("totalCompras", totalCompras);

                // Calcular egresos totales y monto actual
                BigDecimal totalEgresos = totalGastos.add(totalCompras);
                BigDecimal montoActual = montoInicial.add(totalVentas).subtract(totalEgresos);

                cajaData.put("totalEgresos", totalEgresos);
                cajaData.put("montoActual", montoActual);

                // Calcular duración en minutos
                if (tsApertura != null) {
                    long minutos = java.time.Duration.between(
                            tsApertura.toLocalDateTime(),
                            LocalDateTime.now()).toMinutes();
                    cajaData.put("duracionMinutos", minutos);
                }

                cajasActivas.add(cajaData);
            }
        }

        System.out.println("SUCCESS  Cajas activas encontradas: " + cajasActivas.size());
        return cajasActivas;
    }

    /**
     * Obtiene el historial de cierres filtrado.
     * 
     * @param fechaInicio  Fecha inicial del rango (inclusive)
     * @param fechaFin     Fecha final del rango (inclusive)
     * @param idCaja       ID de caja específica (null para todas)
     * @param estadoCuadre Estado del cuadre: "CUADRADO", "SOBRANTE", "FALTANTE" o
     *                     null para todos
     * @return Lista de cierres que cumplen los criterios
     */
    public List<Map<String, Object>> obtenerHistorialCierres(
            LocalDate fechaInicio, LocalDate fechaFin,
            Integer idCaja, String estadoCuadre) throws SQLException {

        List<Map<String, Object>> cierres = new ArrayList<>();
        List<Object> parametros = new ArrayList<>();

        StringBuilder sql = new StringBuilder(SQL_HISTORIAL_CIERRES_BASE);

        // Filtro por rango de fechas
        if (fechaInicio != null) {
            sql.append(" AND DATE(cm.fecha_cierre) >= ? ");
            parametros.add(Date.valueOf(fechaInicio));
        }
        if (fechaFin != null) {
            sql.append(" AND DATE(cm.fecha_cierre) <= ? ");
            parametros.add(Date.valueOf(fechaFin));
        }

        // Filtro por caja
        if (idCaja != null && idCaja > 0) {
            sql.append(" AND c.id_caja = ? ");
            parametros.add(idCaja);
        }

        sql.append(" ORDER BY cm.fecha_cierre DESC LIMIT 500");

        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            // Establecer parámetros
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> cierre = mapearCierre(rs);

                    // Filtrar por estado de cuadre si se especificó
                    if (estadoCuadre != null && !estadoCuadre.isEmpty()) {
                        String estadoActual = (String) cierre.get("estadoCuadre");
                        if (!estadoCuadre.equalsIgnoreCase(estadoActual)) {
                            continue;
                        }
                    }

                    cierres.add(cierre);
                }
            }
        }

        System.out.println("SUCCESS  Cierres encontrados: " + cierres.size());
        return cierres;
    }

    /**
     * Obtiene el detalle completo de un movimiento (abierto o cerrado).
     * Incluye desglose de pagos y egresos.
     * 
     * @param idMovimiento ID del movimiento
     * @return Mapa con toda la información del movimiento
     */
    public Map<String, Object> obtenerDetalleMovimiento(int idMovimiento) throws SQLException {
        Map<String, Object> detalle = new HashMap<>();

        String sql = "SELECT cm.*, c.nombre AS nombre_caja, c.ubicacion, u.nombre AS nombre_usuario, " +
                "       COALESCE((SELECT SUM(v.total) FROM ventas v WHERE v.id_movimiento = cm.id_movimiento AND v.estado = 'completada'), 0) AS total_ventas, "
                +
                "       COALESCE((SELECT COUNT(*) FROM ventas v WHERE v.id_movimiento = cm.id_movimiento AND v.estado = 'completada'), 0) AS cantidad_ventas, "
                +
                "       COALESCE((SELECT SUM(go.monto) FROM gastos_operativos go " +
                "                 INNER JOIN caja_movimiento_detalle cmd ON go.id_movimiento_caja = cmd.id_detalle_movimiento "
                +
                "                 WHERE cmd.id_movimiento_caja = cm.id_movimiento AND go.estado != 'anulado' AND cmd.activo = 1), 0) AS total_gastos, "
                +
                "       COALESCE((SELECT SUM(ce.total) FROM compras_externas ce " +
                "                 INNER JOIN caja_movimiento_detalle cmd ON cmd.id_referencia = ce.id_compra_externa " +
                "                 AND cmd.tipo_referencia = 'compra_externa' " +
                "                 WHERE cmd.id_movimiento_caja = cm.id_movimiento AND ce.estado != 'cancelada' AND cmd.activo = 1), 0) AS total_compras "
                +
                "FROM caja_movimientos cm " +
                "INNER JOIN cajas c ON cm.id_caja = c.id_caja " +
                "INNER JOIN usuarios u ON cm.id_usuario = u.id_usuario " +
                "WHERE cm.id_movimiento = ?";

        try (Connection conn = conexion.getInstance().createConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idMovimiento);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    detalle.put("idMovimiento", rs.getInt("id_movimiento"));
                    detalle.put("idCaja", rs.getInt("id_caja"));
                    detalle.put("nombreCaja", rs.getString("nombre_caja"));
                    detalle.put("ubicacion", rs.getString("ubicacion"));
                    detalle.put("nombreUsuario", rs.getString("nombre_usuario"));

                    Timestamp tsApertura = rs.getTimestamp("fecha_apertura");
                    Timestamp tsCierre = rs.getTimestamp("fecha_cierre");

                    detalle.put("fechaApertura", tsApertura != null ? tsApertura.toLocalDateTime() : null);
                    detalle.put("fechaCierre", tsCierre != null ? tsCierre.toLocalDateTime() : null);
                    detalle.put("estaAbierto", tsCierre == null);

                    BigDecimal montoInicial = rs.getBigDecimal("monto_inicial");
                    BigDecimal montoFinal = rs.getBigDecimal("monto_final");
                    BigDecimal totalVentas = rs.getBigDecimal("total_ventas");
                    BigDecimal totalGastos = rs.getBigDecimal("total_gastos");
                    BigDecimal totalCompras = rs.getBigDecimal("total_compras");

                    detalle.put("montoInicial", montoInicial != null ? montoInicial : BigDecimal.ZERO);
                    detalle.put("montoFinal", montoFinal != null ? montoFinal : BigDecimal.ZERO);
                    detalle.put("totalVentas", totalVentas);
                    detalle.put("cantidadVentas", rs.getInt("cantidad_ventas"));
                    detalle.put("totalGastos", totalGastos);
                    detalle.put("totalCompras", totalCompras);
                    detalle.put("observaciones", rs.getString("observaciones"));

                    // Calcular totales
                    BigDecimal totalEgresos = totalGastos.add(totalCompras);
                    BigDecimal montoEsperado = montoInicial.add(totalVentas).subtract(totalEgresos);

                    detalle.put("totalEgresos", totalEgresos);
                    detalle.put("montoEsperado", montoEsperado);

                    // Si está cerrado, calcular diferencia y estado
                    if (tsCierre != null && montoFinal != null) {
                        BigDecimal diferencia = montoFinal.subtract(montoEsperado);
                        detalle.put("diferencia", diferencia);

                        int cmp = diferencia.compareTo(BigDecimal.ZERO);
                        String estado = cmp == 0 ? "CUADRADO" : (cmp > 0 ? "SOBRANTE" : "FALTANTE");
                        detalle.put("estadoCuadre", estado);
                    }

                    // Calcular duración
                    if (tsApertura != null) {
                        LocalDateTime fin = tsCierre != null ? tsCierre.toLocalDateTime() : LocalDateTime.now();
                        long minutos = java.time.Duration.between(tsApertura.toLocalDateTime(), fin).toMinutes();
                        detalle.put("duracionMinutos", minutos);
                    }
                }
            }

            // Obtener desglose de pagos
            detalle.put("desglosePagos", obtenerDesglosePagos(conn, idMovimiento));
        }

        return detalle;
    }

    /**
     * Obtiene estadísticas agregadas de cuadres.
     */
    public Map<String, Object> obtenerEstadisticasCuadres(
            LocalDate fechaInicio, LocalDate fechaFin, Integer idCaja) throws SQLException {

        Map<String, Object> stats = new HashMap<>();

        List<Map<String, Object>> cierres = obtenerHistorialCierres(fechaInicio, fechaFin, idCaja, null);

        int totalCierres = cierres.size();
        int cuadrados = 0, sobrantes = 0, faltantes = 0;
        BigDecimal sumaDiferencias = BigDecimal.ZERO;
        BigDecimal sumaVentas = BigDecimal.ZERO;

        for (Map<String, Object> cierre : cierres) {
            String estado = (String) cierre.get("estadoCuadre");
            BigDecimal diferencia = (BigDecimal) cierre.get("diferencia");
            BigDecimal ventas = (BigDecimal) cierre.get("totalVentas");

            if ("CUADRADO".equals(estado))
                cuadrados++;
            else if ("SOBRANTE".equals(estado))
                sobrantes++;
            else if ("FALTANTE".equals(estado))
                faltantes++;

            if (diferencia != null)
                sumaDiferencias = sumaDiferencias.add(diferencia);
            if (ventas != null)
                sumaVentas = sumaVentas.add(ventas);
        }

        stats.put("totalCierres", totalCierres);
        stats.put("cuadrados", cuadrados);
        stats.put("sobrantes", sobrantes);
        stats.put("faltantes", faltantes);
        stats.put("sumaDiferencias", sumaDiferencias);
        stats.put("sumaVentas", sumaVentas);

        if (totalCierres > 0) {
            stats.put("porcentajeCuadrados", (cuadrados * 100.0) / totalCierres);
            stats.put("promedioDiferencia", sumaDiferencias.divide(
                    BigDecimal.valueOf(totalCierres), 2, java.math.RoundingMode.HALF_UP));
        } else {
            stats.put("porcentajeCuadrados", 0.0);
            stats.put("promedioDiferencia", BigDecimal.ZERO);
        }

        return stats;
    }

    // ==================== MÉTODOS PRIVADOS ====================

    private Map<String, Object> mapearCierre(ResultSet rs) throws SQLException {
        Map<String, Object> cierre = new HashMap<>();

        cierre.put("idMovimiento", rs.getInt("id_movimiento"));
        cierre.put("idCaja", rs.getInt("id_caja"));
        cierre.put("nombreCaja", rs.getString("nombre_caja"));
        cierre.put("nombreUsuario", rs.getString("nombre_usuario"));

        Timestamp tsApertura = rs.getTimestamp("fecha_apertura");
        Timestamp tsCierre = rs.getTimestamp("fecha_cierre");

        cierre.put("fechaApertura", tsApertura != null ? tsApertura.toLocalDateTime() : null);
        cierre.put("fechaCierre", tsCierre != null ? tsCierre.toLocalDateTime() : null);

        BigDecimal montoInicial = rs.getBigDecimal("monto_inicial");
        BigDecimal montoFinal = rs.getBigDecimal("monto_final");
        BigDecimal totalVentas = rs.getBigDecimal("total_ventas");
        BigDecimal totalGastos = rs.getBigDecimal("total_gastos");
        BigDecimal totalCompras = rs.getBigDecimal("total_compras");

        // Normalizar posibles nulos para cálculos seguros
        montoInicial = montoInicial != null ? montoInicial : BigDecimal.ZERO;
        montoFinal = montoFinal != null ? montoFinal : BigDecimal.ZERO;
        totalVentas = totalVentas != null ? totalVentas : BigDecimal.ZERO;
        totalGastos = totalGastos != null ? totalGastos : BigDecimal.ZERO;
        totalCompras = totalCompras != null ? totalCompras : BigDecimal.ZERO;

        cierre.put("montoInicial", montoInicial);
        cierre.put("montoFinal", montoFinal);
        cierre.put("totalVentas", totalVentas);
        cierre.put("cantidadVentas", rs.getInt("cantidad_ventas"));
        cierre.put("totalGastos", totalGastos);
        cierre.put("totalCompras", totalCompras);

        // Calcular totales
        BigDecimal totalEgresos = totalGastos.add(totalCompras);
        BigDecimal montoEsperado = montoInicial.add(totalVentas).subtract(totalEgresos);
        BigDecimal diferencia = montoFinal.subtract(montoEsperado);

        cierre.put("totalEgresos", totalEgresos);
        cierre.put("montoEsperado", montoEsperado);
        cierre.put("diferencia", diferencia);

        // Determinar estado del cuadre
        int cmp = diferencia.compareTo(BigDecimal.ZERO);
        String estado = cmp == 0 ? "CUADRADO" : (cmp > 0 ? "SOBRANTE" : "FALTANTE");
        cierre.put("estadoCuadre", estado);

        // Calcular duración
        if (tsApertura != null && tsCierre != null) {
            long minutos = java.time.Duration.between(
                    tsApertura.toLocalDateTime(),
                    tsCierre.toLocalDateTime()).toMinutes();
            cierre.put("duracionMinutos", minutos);
        }

        return cierre;
    }

    private List<Map<String, Object>> obtenerDesglosePagos(Connection conn, int idMovimiento) throws SQLException {
        List<Map<String, Object>> desglose = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(SQL_DESGLOSE_PAGOS)) {
            ps.setInt(1, idMovimiento);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> pago = new HashMap<>();
                    pago.put("tipoPago", rs.getString("tipo_pago"));
                    pago.put("cantidad", rs.getInt("cantidad"));
                    pago.put("total", rs.getBigDecimal("total"));
                    desglose.add(pago);
                }
            }
        }

        return desglose;
    }
}

