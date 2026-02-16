package raven.clases.comercial;

import java.sql.*;
import raven.controlador.principal.conexion;
import raven.controlador.comercial.ModelNotaCredito;

/**
 * Servicio para gestión de Notas de Crédito
 * 
 * @author Sistema
 * @version 1.0
 */
public class ServiceNotaCredito {
    
    /**
     * Obtiene la nota de crédito asociada a una devolución
     * 
     * @param idDevolucion ID de la devolución
     * @return Nota de crédito o null si no existe
     * @throws SQLException Si hay error de acceso a datos
     */
    public ModelNotaCredito obtenerNotaCreditoPorDevolucion(int idDevolucion) throws SQLException {
        String sql = "SELECT nc.*, " +
                    "c.nombre as cliente_nombre, " +
                    "c.dni as cliente_dni, " +
                    "d.numero_devolucion " +
                    "FROM notas_credito nc " +
                    "INNER JOIN clientes c ON nc.id_cliente = c.id_cliente " +
                    "INNER JOIN devoluciones d ON nc.id_devolucion = d.id_devolucion " +
                    "WHERE nc.id_devolucion = ? " +
                    "AND nc.activa = 1 " +
                    "ORDER BY nc.fecha_emision DESC " +
                    "LIMIT 1";
        
        try (Connection conn = conexion.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, idDevolucion);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearNotaCredito(rs);
                }
            }
        }
        
        return null;
    }
    /**
 * Busca una nota de crédito por su número
 * 
 * @param numeroNotaCredito Número de la nota de crédito
 * @return Nota de crédito o null si no existe
 * @throws SQLException Si hay error de acceso a datos
 */
public ModelNotaCredito buscarNotaCreditoPorNumero(String numeroNotaCredito) throws SQLException {
    String sql = "SELECT nc.*, " +
                "c.nombre as cliente_nombre, " +
                "c.dni as cliente_dni, " +
                "d.numero_devolucion " +
                "FROM notas_credito nc " +
                "INNER JOIN clientes c ON nc.id_cliente = c.id_cliente " +
                "INNER JOIN devoluciones d ON nc.id_devolucion = d.id_devolucion " +
                "WHERE nc.numero_nota_credito = ? " +
                "AND nc.activa = 1";
    
    try (Connection conn = conexion.getInstance().getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        
        stmt.setString(1, numeroNotaCredito);
        
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return mapearNotaCredito(rs);
            }
        }
    }
    
    return null;
}
/**
 * Aplica una nota de crédito a una venta
 * Actualiza saldos y registra la aplicación
 */
public void aplicarNotaCreditoAVenta(int idNotaCredito, int idVenta, 
                                     java.math.BigDecimal montoAplicado, 
                                     int idUsuario) throws SQLException {
    Connection conn = null;
    
    try {
        conn = conexion.getInstance().getConnection();
        conn.setAutoCommit(false);
        
        // 1. Verificar saldo disponible actual
        String sqlVerificar = "SELECT saldo_disponible, total FROM notas_credito " +
                             "WHERE id_nota_credito = ? AND activa = 1";
        
        java.math.BigDecimal saldoDisponible = null;
        java.math.BigDecimal totalNota = null;
        
        try (PreparedStatement psVerif = conn.prepareStatement(sqlVerificar)) {
            psVerif.setInt(1, idNotaCredito);
            try (ResultSet rs = psVerif.executeQuery()) {
                if (rs.next()) {
                    saldoDisponible = rs.getBigDecimal("saldo_disponible");
                    totalNota = rs.getBigDecimal("total");
                } else {
                    throw new SQLException("Nota de crédito no encontrada o inactiva");
                }
            }
        }
        
        // 2. Validar que hay saldo suficiente
        if (saldoDisponible.compareTo(montoAplicado) < 0) {
            throw new SQLException(
                String.format("Saldo insuficiente. Disponible: $%s, Solicitado: $%s",
                    saldoDisponible.toPlainString(),
                    montoAplicado.toPlainString()
                )
            );
        }
        
        System.out.println("=== APLICANDO NOTA DE CRÉDITO ===");
        System.out.println("Saldo disponible actual: $" + saldoDisponible);
        System.out.println("Monto a aplicar: $" + montoAplicado);
        
        // 3. Registrar aplicación en tabla de aplicaciones
        String sqlAplicacion = "INSERT INTO aplicaciones_nota_credito " +
                              "(id_nota_credito, id_venta, monto_aplicado, id_usuario_aplica) " +
                              "VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sqlAplicacion)) {
            stmt.setInt(1, idNotaCredito);
            stmt.setInt(2, idVenta);
            stmt.setBigDecimal(3, montoAplicado);
            stmt.setInt(4, idUsuario);
            stmt.executeUpdate();
            
            System.out.println("Registro de aplicación creado");
        }
        
        // 4. Calcular nuevo saldo disponible
        java.math.BigDecimal nuevoSaldoDisponible = saldoDisponible.subtract(montoAplicado);
        
        // 5. Determinar nuevo estado
        String nuevoEstado;
        if (nuevoSaldoDisponible.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            nuevoEstado = "aplicada";
        } else {
            nuevoEstado = "emitida"; // Mantener estado actual si aún hay saldo
        }
        
        System.out.println("Nuevo saldo disponible: $" + nuevoSaldoDisponible);
        System.out.println("Nuevo estado: " + nuevoEstado);
        
        // 6. Actualizar saldos y estado de la nota de crédito
        String sqlUpdate = "UPDATE notas_credito SET " +
                          "saldo_usado = saldo_usado + ?, " +
                          "saldo_disponible = ?, " +
                          "estado = ?, " +
                          "id_venta_aplicada = ?, " +
                          "fecha_aplicacion = NOW() " +
                          "WHERE id_nota_credito = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
            stmt.setBigDecimal(1, montoAplicado);
            stmt.setBigDecimal(2, nuevoSaldoDisponible);
            stmt.setString(3, nuevoEstado);
            stmt.setInt(4, idVenta);
            stmt.setInt(5, idNotaCredito);
            
            int filasActualizadas = stmt.executeUpdate();
            
            if (filasActualizadas == 0) {
                throw new SQLException("No se pudo actualizar la nota de crédito");
            }
            
            System.out.println("Nota de crédito actualizada correctamente");
        }
        
        // 7. Verificar actualización
        String sqlVerifFinal = "SELECT saldo_usado, saldo_disponible, estado " +
                              "FROM notas_credito WHERE id_nota_credito = ?";
        
        try (PreparedStatement psVerif = conn.prepareStatement(sqlVerifFinal)) {
            psVerif.setInt(1, idNotaCredito);
            try (ResultSet rs = psVerif.executeQuery()) {
                if (rs.next()) {
                    System.out.println("=== VERIFICACIÓN FINAL ===");
                    System.out.println("Saldo usado: $" + rs.getBigDecimal("saldo_usado"));
                    System.out.println("Saldo disponible: $" + rs.getBigDecimal("saldo_disponible"));
                    System.out.println("Estado: " + rs.getString("estado"));
                    System.out.println("========================");
                }
            }
        }
        
        conn.commit();
        
    } catch (SQLException e) {
        if (conn != null) {
            try {
                conn.rollback();
                System.err.println("Rollback ejecutado debido a error");
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
            } catch (SQLException ex) {
                System.err.println("Error cerrando conexión: " + ex.getMessage());
            }
        }
    }
}
    /**
     * Mapea ResultSet a ModelNotaCredito
     */
    private ModelNotaCredito mapearNotaCredito(ResultSet rs) throws SQLException {
        ModelNotaCredito nota = new ModelNotaCredito();
        
        nota.setIdNotaCredito(rs.getInt("id_nota_credito"));
        nota.setNumeroNotaCredito(rs.getString("numero_nota_credito"));
        nota.setIdDevolucion(rs.getInt("id_devolucion"));
        nota.setIdCliente(rs.getInt("id_cliente"));
        nota.setIdUsuarioGenera(rs.getInt("id_usuario_genera"));
        
        Timestamp fechaEmision = rs.getTimestamp("fecha_emision");
        if (fechaEmision != null) {
            nota.setFechaEmision(fechaEmision.toLocalDateTime());
        }
        
        nota.setTipoNota(ModelNotaCredito.TipoNota.fromString(rs.getString("tipo_nota")));
        nota.setSubtotal(rs.getBigDecimal("subtotal"));
        nota.setIva(rs.getBigDecimal("iva"));
        nota.setTotal(rs.getBigDecimal("total"));
        nota.setEstado(ModelNotaCredito.EstadoNota.fromString(rs.getString("estado")));
        
        Timestamp fechaVencimiento = rs.getTimestamp("fecha_vencimiento");
        if (fechaVencimiento != null) {
            nota.setFechaVencimiento(fechaVencimiento.toLocalDateTime());
        }
        
        nota.setSaldoDisponible(rs.getBigDecimal("saldo_disponible"));
        nota.setSaldoUsado(rs.getBigDecimal("saldo_usado"));
        nota.setObservaciones(rs.getString("observaciones"));
        nota.setActiva(rs.getBoolean("activa"));
        
        // Datos adicionales
        nota.setClienteNombre(rs.getString("cliente_nombre"));
        nota.setClienteDni(rs.getString("cliente_dni"));
        nota.setNumeroDevolucion(rs.getString("numero_devolucion"));
        
        return nota;
    }
}