package raven.clases.principal;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import raven.controlador.principal.ModelMedioPago;
import raven.controlador.principal.ModelMedioPago.TipoMedioPago;
import raven.controlador.principal.conexion;

/**
 * Servicio para gestionar medios de pago en la base de datos.
 * 
 * PATRÓN: Repository Pattern
 * PRINCIPIO: Separation of Concerns (lógica de BD aislada)
 * 
 * MODIFICADO: Maneja ventas pendientes (fiados) sin medios de pago
 * 
 * @author lmog2
 */
public class ServiceMedioPago {
    
    /**
     * Guarda múltiples medios de pago de una venta en la base de datos.
     * 
     * MODIFICADO: Valida que haya medios de pago antes de guardar
     * 
     * @param idVenta ID de la venta
     * @param mediosPago Lista de medios de pago
     * @throws SQLException Si hay error en la BD
     */
    public void guardarMediosPago(int idVenta, List<ModelMedioPago> mediosPago) 
            throws SQLException {
        
        // ================================================================
        // VALIDACIÓN: Si la lista está vacía, no hacer nada (caso de fiado)
        // ================================================================
        if (mediosPago == null || mediosPago.isEmpty()) {
            System.out.println("No hay medios de pago para guardar (posible fiado)");
            return;
        }
        
        String sql = "INSERT INTO venta_medios_pago "
                   + "(id_venta, tipo_pago, monto, numero_referencia, observaciones) "
                   + "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            for (ModelMedioPago medio : mediosPago) {
                // Validar que el medio tenga datos válidos
                if (medio.getTipo() == null || medio.getMonto() == null) {
                    System.err.println("Medio de pago inválido, se omite: " + medio);
                    continue;
                }
                
                ps.setInt(1, idVenta);
                ps.setString(2, medio.getTipo().getCodigo());
                ps.setBigDecimal(3, medio.getMonto());
                ps.setString(4, medio.getNumeroReferencia());
                ps.setString(5, medio.getObservaciones());
                ps.addBatch();
            }
            
            int[] resultados = ps.executeBatch();
            int insertados = resultados.length;
            
            System.out.println(insertados + " medio(s) de pago guardado(s)");
        }
    }
    
    /**
     * Obtiene todos los medios de pago de una venta.
     * 
     * @param idVenta ID de la venta
     * @return Lista de medios de pago (puede estar vacía si es fiado)
     * @throws SQLException Si hay error en la BD
     */
    public List<ModelMedioPago> obtenerMediosPago(int idVenta) throws SQLException {
        List<ModelMedioPago> lista = new ArrayList<>();
        
        String sql = "SELECT * FROM venta_medios_pago "
                   + "WHERE id_venta = ? AND activo = 1 "
                   + "ORDER BY fecha_registro";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, idVenta);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ModelMedioPago medio = new ModelMedioPago();
                    medio.setIdMedioPago(rs.getInt("id_medio_pago"));
                    medio.setIdVenta(rs.getInt("id_venta"));
                    medio.setTipo(TipoMedioPago.fromCodigo(rs.getString("tipo_pago")));
                    medio.setMonto(rs.getBigDecimal("monto"));
                    medio.setNumeroReferencia(rs.getString("numero_referencia"));
                    medio.setObservaciones(rs.getString("observaciones"));
                    medio.setFechaRegistro(rs.getTimestamp("fecha_registro").toLocalDateTime());
                    medio.setActivo(rs.getBoolean("activo"));
                    
                    lista.add(medio);
                }
            }
        }
        
        // Informar si no hay medios de pago (caso de fiado)
        if (lista.isEmpty()) {
            System.out.println("No hay medios de pago para venta #" + idVenta + " (posible fiado)");
        }
        
        return lista;
    }
    
    /**
     * Calcula el total pagado en una venta.
     * 
     * IMPORTANTE: Retorna 0 para ventas pendientes (fiados)
     * 
     * @param idVenta ID de la venta
     * @return Total pagado (0 si es fiado)
     * @throws SQLException Si hay error en la BD
     */
    public BigDecimal calcularTotalPagado(int idVenta) throws SQLException {
        String sql = "SELECT COALESCE(SUM(monto), 0) as total "
                   + "FROM venta_medios_pago "
                   + "WHERE id_venta = ? AND activo = 1";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, idVenta);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal total = rs.getBigDecimal("total");
                    
                    if (total.compareTo(BigDecimal.ZERO) == 0) {
                        System.out.println("Venta #" + idVenta + " sin pagos registrados (fiado)");
                    }
                    
                    return total;
                }
            }
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Elimina (lógicamente) un medio de pago.
     * 
     * @param idMedioPago ID del medio de pago a eliminar
     * @throws SQLException Si hay error en la BD
     */
    public void eliminarMedioPago(int idMedioPago) throws SQLException {
        String sql = "UPDATE venta_medios_pago SET activo = 0 WHERE id_medio_pago = ?";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, idMedioPago);
            int filasAfectadas = ps.executeUpdate();
            
            if (filasAfectadas > 0) {
                System.out.println("Medio de pago #" + idMedioPago + " eliminado");
            } else {
                System.out.println("No se encontró medio de pago #" + idMedioPago);
            }
        }
    }
    
    /**
     * Obtiene un resumen de medios de pago por tipo para una venta.
     * 
     * @param idVenta ID de la venta
     * @return Mapa con tipo de pago y monto total (vacío si es fiado)
     * @throws SQLException Si hay error en la BD
     */
    public Map<TipoMedioPago, BigDecimal> obtenerResumenPorTipo(int idVenta) 
            throws SQLException {
        
        Map<TipoMedioPago, BigDecimal> resumen = new HashMap<>();
        
        String sql = "SELECT tipo_pago, SUM(monto) as total "
                   + "FROM venta_medios_pago "
                   + "WHERE id_venta = ? AND activo = 1 "
                   + "GROUP BY tipo_pago";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, idVenta);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TipoMedioPago tipo = TipoMedioPago.fromCodigo(rs.getString("tipo_pago"));
                    BigDecimal total = rs.getBigDecimal("total");
                    resumen.put(tipo, total);
                }
            }
        }
        
        if (resumen.isEmpty()) {
            System.out.println("Sin resumen de pagos para venta #" + idVenta + " (fiado)");
        }
        
        return resumen;
    }
    
    // ====================================================================
    // NUEVOS MÉTODOS PARA GESTIÓN DE VENTAS PENDIENTES
    // ====================================================================
    
    /**
     * Verifica si una venta es un fiado (sin medios de pago).
     * 
     * @param idVenta ID de la venta
     * @return true si es fiado, false si tiene pagos
     * @throws SQLException Si hay error en la BD
     */
    public boolean esFiado(int idVenta) throws SQLException {
        String sql = "SELECT COUNT(*) as total "
                   + "FROM venta_medios_pago "
                   + "WHERE id_venta = ? AND activo = 1";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, idVenta);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total");
                    boolean esFiado = (total == 0);
                    
                    if (esFiado) {
                        System.out.println("Venta #" + idVenta + " es un FIADO");
                    }
                    
                    return esFiado;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Registra un pago parcial para una venta pendiente.
     * 
     * Útil para cuando el cliente abona parte de su deuda.
     * 
     * @param idVenta ID de la venta
     * @param medioPago Medio de pago a registrar
     * @return ID del medio de pago insertado
     * @throws SQLException Si hay error en la BD
     */
    public int registrarPagoParcial(int idVenta, ModelMedioPago medioPago) 
            throws SQLException {
        
        String sql = "INSERT INTO venta_medios_pago "
                   + "(id_venta, tipo_pago, monto, numero_referencia, observaciones) "
                   + "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setInt(1, idVenta);
            ps.setString(2, medioPago.getTipo().getCodigo());
            ps.setBigDecimal(3, medioPago.getMonto());
            ps.setString(4, medioPago.getNumeroReferencia());
            ps.setString(5, medioPago.getObservaciones());
            
            ps.executeUpdate();
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int idMedioPago = rs.getInt(1);
                    System.out.println("Pago parcial registrado - ID: " + idMedioPago);
                    return idMedioPago;
                }
            }
        }
        
        throw new SQLException("No se pudo registrar el pago parcial");
    }
    
    /**
     * Calcula el saldo pendiente de una venta.
     * 
     * @param idVenta ID de la venta
     * @param totalVenta Total original de la venta
     * @return Saldo pendiente
     * @throws SQLException Si hay error en la BD
     */
    public BigDecimal calcularSaldoPendiente(int idVenta, BigDecimal totalVenta) 
            throws SQLException {
        
        BigDecimal totalPagado = calcularTotalPagado(idVenta);
        BigDecimal saldoPendiente = totalVenta.subtract(totalPagado);
        
        System.out.println("Venta #" + idVenta + ":");
        System.out.println("   Total: " + totalVenta);
        System.out.println("   Pagado: " + totalPagado);
        System.out.println("   Pendiente: " + saldoPendiente);
        
        return saldoPendiente;
    }
    
    /**
     * Obtiene todas las ventas con saldo pendiente (fiados).
     * 
     * @return Lista de IDs de ventas pendientes
     * @throws SQLException Si hay error en la BD
     */
    public List<Integer> obtenerVentasPendientes() throws SQLException {
        List<Integer> ventasPendientes = new ArrayList<>();
        
        String sql = "SELECT v.id_venta "
                   + "FROM ventas v "
                   + "WHERE v.estado = 'pendiente' "
                   + "ORDER BY v.fecha_venta DESC";
        
        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                ventasPendientes.add(rs.getInt("id_venta"));
            }
        }
        
        System.out.println(ventasPendientes.size() + " ventas pendientes encontradas");
        return ventasPendientes;
    }
}
