package raven.clases.comercial;

import java.sql.*;
import raven.clases.admin.ServiceUser;
import raven.controlador.admin.ModelUser;
import raven.controlador.principal.conexion;
import raven.controlador.comercial.ModelDevolucion;
import raven.controlador.comercial.ModelNotaCredito;

/**
 * Servicio para manejo de autorizaciones con validación segura de credenciales
 * 
 * PRINCIPIOS APLICADOS:
 * - SRP: Responsabilidad única de gestionar autorizaciones
 * - Seguridad: Validación de credenciales sin exponer información sensible
 * - SOLID: Separación de concerns entre autenticación y autorización
 * 
 * @author Sistema
 * @version 1.1 - ACTUALIZADO con integración a ServiceDevolucion
 */
public class ServiceAutorizacion {
    
    // SUCCESS  AGREGAR: Instancia de ServiceDevolucion
    private final ServiceDevolucion serviceDevolucion;
    
    // SUCCESS  AGREGAR: Constructor
    public ServiceAutorizacion() {
        this.serviceDevolucion = new ServiceDevolucion();
    }
    
    /**
     * Valida credenciales de administrador para autorización
     * 
     * @param username Nombre de usuario
     * @param password Contraseña del usuario
     * @return ID del usuario si las credenciales son válidas y es admin, 0 si no
     */
    public int validarCredencialesAdministrador(String username, String password) {
        // Validaciones preventivas
        if (username == null || username.trim().isEmpty()) {
            System.err.println("WARNING  Username vacío en validación");
            return 0;
        }
        
        if (password == null || password.trim().isEmpty()) {
            System.err.println("WARNING  Password vacío en validación");
            return 0;
        }

        try {
            ServiceUser serviceUser = new ServiceUser();
            ModelUser user = serviceUser.authenticate(username.trim(), password);
            if (user == null) {
                System.err.println("ERROR  Credenciales inválidas");
                return 0;
            }

            String rol = user.getRol();
            if (!"admin".equalsIgnoreCase(rol) && !"gerente".equalsIgnoreCase(rol)) {
                System.err.println("ERROR  Usuario sin permisos");
                return 0;
            }

            System.out.println("SUCCESS  Autorización exitosa - Usuario: " + user.getNombre() +
                    " (ID: " + user.getIdUsuario() + "), Rol: " + rol);
            return user.getIdUsuario();

        } catch (SQLException e) {
            System.err.println("ERROR  Error validando credenciales: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * SUCCESS  MODIFICADO: Autoriza devolución usando ServiceDevolucion y retorna nota de crédito
     * 
     * CAMBIO CRÍTICO: Ahora usa ServiceDevolucion.autorizarDevolucion() 
     * que maneja TODO el proceso (inventario + nota de crédito)
     * 
     * @param idDevolucion ID de la devolución
     * @param idUsuarioAutoriza ID del usuario que autoriza
     * @param aprobada true para aprobar, false para rechazar
     * @param observaciones Observaciones de la autorización
     * @return ModelNotaCredito generada si se aprobó, null si se rechazó o hubo error
     */
    public ModelNotaCredito autorizarYObtenerNotaCredito(int idDevolucion, 
                                                         int idUsuarioAutoriza, 
                                                         boolean aprobada, 
                                                         String observaciones) {
        try {
            System.out.println(" ServiceAutorizacion: Iniciando proceso de autorización...");
            System.out.println("   - ID Devolución: " + idDevolucion);
            System.out.println("   - Usuario: " + idUsuarioAutoriza);
            System.out.println("   - Acción: " + (aprobada ? "APROBAR" : "RECHAZAR"));
            
            // SUCCESS  CAMBIO CRÍTICO: Usar ServiceDevolucion.autorizarDevolucion()
            // Este método maneja TODO: inventario, nota de crédito, movimientos
            boolean exito = serviceDevolucion.autorizarDevolucion(
                    idDevolucion, 
                    idUsuarioAutoriza, 
                    aprobada, 
                    observaciones);
            
            if (!exito) {
                System.err.println("ERROR  Error en ServiceDevolucion.autorizarDevolucion()");
                return null;
            }
            
            System.out.println("SUCCESS  ServiceDevolucion.autorizarDevolucion() completado exitosamente");
            
            // Si fue aprobada, obtener la nota de crédito que se generó
            if (aprobada) {
                ModelNotaCredito notaCredito = obtenerNotaCreditoPorDevolucion(idDevolucion);
                
                if (notaCredito != null) {
                    System.out.println("SUCCESS  Nota de crédito obtenida: " + 
                                     notaCredito.getNumeroNotaCredito());
                } else {
                    System.err.println("WARNING  No se pudo obtener la nota de crédito generada");
                }
                
                return notaCredito;
            }
            
            // Si fue rechazada, retornar null (no hay nota de crédito)
            return null;
            
        } catch (Exception e) {
            System.err.println("ERROR  Error en autorizarYObtenerNotaCredito: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * SUCCESS  NUEVO MÉTODO: Obtiene la nota de crédito generada para una devolución
     * Incluye información adicional del cliente y devolución
     * 
     * @param idDevolucion ID de la devolución
     * @return ModelNotaCredito si existe, null si no
     */
    private ModelNotaCredito obtenerNotaCreditoPorDevolucion(int idDevolucion) {
        String sql = "SELECT nc.*, " +
                    "c.nombre as cliente_nombre, " +
                    "c.dni as cliente_dni, " +
                    "d.numero_devolucion " +
                    "FROM notas_credito nc " +
                    "LEFT JOIN clientes c ON nc.id_cliente = c.id_cliente " +
                    "LEFT JOIN devoluciones d ON nc.id_devolucion = d.id_devolucion " +
                    "WHERE nc.id_devolucion = ? AND nc.activa = 1 " +
                    "ORDER BY nc.fecha_emision DESC LIMIT 1";
        
        try (Connection conn = conexion.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, idDevolucion);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearNotaCredito(rs);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("ERROR  Error obteniendo nota de crédito: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * SUCCESS  ADAPTADO: Mapea ResultSet a ModelNotaCredito usando ENUMS
     */
    private ModelNotaCredito mapearNotaCredito(ResultSet rs) throws SQLException {
        ModelNotaCredito nota = new ModelNotaCredito();
        
        // IDs
        nota.setIdNotaCredito(rs.getInt("id_nota_credito"));
        nota.setIdDevolucion(rs.getInt("id_devolucion"));
        nota.setIdCliente(rs.getInt("id_cliente"));
        nota.setIdUsuarioGenera(rs.getInt("id_usuario_genera"));
        
        // Números y referencias
        nota.setNumeroNotaCredito(rs.getString("numero_nota_credito"));
        
        // Fechas
        Timestamp fechaEmision = rs.getTimestamp("fecha_emision");
        if (fechaEmision != null) {
            nota.setFechaEmision(fechaEmision.toLocalDateTime());
        }
        
        Timestamp fechaVencimiento = rs.getTimestamp("fecha_vencimiento");
        if (fechaVencimiento != null) {
            nota.setFechaVencimiento(fechaVencimiento.toLocalDateTime());
        }
        
        Timestamp fechaAplicacion = rs.getTimestamp("fecha_aplicacion");
        if (fechaAplicacion != null) {
            nota.setFechaAplicacion(fechaAplicacion.toLocalDateTime());
        }
        
        // SUCCESS  ENUMS - Usar los métodos fromString()
        String tipoNotaStr = rs.getString("tipo_nota");
        if (tipoNotaStr != null) {
            nota.setTipoNota(ModelNotaCredito.TipoNota.fromString(tipoNotaStr));
        }
        
        String estadoStr = rs.getString("estado");
        if (estadoStr != null) {
            nota.setEstado(ModelNotaCredito.EstadoNota.fromString(estadoStr));
        }
        
        // Montos
        nota.setSubtotal(rs.getBigDecimal("subtotal"));
        nota.setIva(rs.getBigDecimal("iva"));
        nota.setTotal(rs.getBigDecimal("total"));
        nota.setSaldoDisponible(rs.getBigDecimal("saldo_disponible"));
        nota.setSaldoUsado(rs.getBigDecimal("saldo_usado"));
        
        // Observaciones y referencias
        nota.setObservaciones(rs.getString("observaciones"));
        
        Integer idVentaAplicada = (Integer) rs.getObject("id_venta_aplicada");
        nota.setIdVentaAplicada(idVentaAplicada);
        
        nota.setActiva(rs.getBoolean("activa"));
        
        // Datos adicionales para visualización
        nota.setClienteNombre(rs.getString("cliente_nombre"));
        nota.setClienteDni(rs.getString("cliente_dni"));
        nota.setNumeroDevolucion(rs.getString("numero_devolucion"));
        
        return nota;
    }
    
    /**
     * Obtiene información de devolución para mostrar en autorización
     */
    public ModelDevolucion obtenerDevolucionParaAutorizar(int idDevolucion) throws SQLException {
        String sql = "SELECT d.*, " +
                    "c.nombre as cliente_nombre, " +
                    "v.total as venta_total " +
                    "FROM devoluciones d " +
                    "LEFT JOIN clientes c ON d.id_cliente = c.id_cliente " +
                    "LEFT JOIN ventas v ON d.id_venta = v.id_venta " +
                    "WHERE d.id_devolucion = ? AND d.activa = 1";
        
        try (Connection conn = conexion.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, idDevolucion);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearDevolucion(rs);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Mapea ResultSet a ModelDevolucion
     */
    private ModelDevolucion mapearDevolucion(ResultSet rs) throws SQLException {
        ModelDevolucion devolucion = new ModelDevolucion();
        
        devolucion.setIdDevolucion(rs.getInt("id_devolucion"));
        devolucion.setNumeroDevolucion(rs.getString("numero_devolucion"));
        devolucion.setIdVenta(rs.getInt("id_venta"));
        devolucion.setTotalDevolucion(rs.getBigDecimal("total_devolucion"));
        devolucion.setMotivo(ModelDevolucion.MotivoDevolucion.fromString(
            rs.getString("motivo")));
        devolucion.setObservaciones(rs.getString("observaciones"));
        
        Timestamp fechaDev = rs.getTimestamp("fecha_devolucion");
        if (fechaDev != null) {
            devolucion.setFechaDevolucion(fechaDev.toLocalDateTime());
        }
        
        return devolucion;
    }
}
