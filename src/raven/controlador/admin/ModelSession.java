package raven.controlador.admin;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Modelo de datos para sesión de usuario.
 * 
 * Representa una sesión activa en el sistema con toda la información
 * necesaria para validación y auditoría.
 * 
 * @author CrisDEV
 */
public class ModelSession implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // ===================================================================
    // ENUMS
    // ===================================================================
    
    public enum EstadoSesion {
        ACTIVA("activa", "Sesión Activa"),
        BLOQUEADA("bloqueada", "Sesión Bloqueada"),
        CERRADA("cerrada", "Sesión Cerrada");
        
        private final String codigo;
        private final String descripcion;
        
        EstadoSesion(String codigo, String descripcion) {
            this.codigo = codigo;
            this.descripcion = descripcion;
        }
        
        public String getCodigo() { return codigo; }
        public String getDescripcion() { return descripcion; }
        
        public static EstadoSesion fromCodigo(String codigo) {
            for (EstadoSesion estado : values()) {
                if (estado.codigo.equals(codigo)) {
                    return estado;
                }
            }
            return CERRADA;
        }
    }
    
    // ===================================================================
    // PROPIEDADES
    // ===================================================================
    
    private int idSesion;
    private int idUsuario;
    private String tokenSesion;
    private String ipAddress;
    private String hostname;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaUltimoPing;
    private Integer idCajaAbierta;
    private Integer idMovimientoCaja;
    private EstadoSesion estado;
    private String navegador;
    private String sistemaOperativo;
    
    // ===================================================================
    // CONSTRUCTORES
    // ===================================================================
    
    public ModelSession() {
        this.estado = EstadoSesion.ACTIVA;
        this.fechaInicio = LocalDateTime.now();
        this.fechaUltimoPing = LocalDateTime.now();
    }
    
    public ModelSession(int idUsuario, String tokenSesion) {
        this();
        this.idUsuario = idUsuario;
        this.tokenSesion = tokenSesion;
    }
    
    // ===================================================================
    // GETTERS Y SETTERS
    // ===================================================================
    
    public int getIdSesion() {
        return idSesion;
    }
    
    public void setIdSesion(int idSesion) {
        this.idSesion = idSesion;
    }
    
    public int getIdUsuario() {
        return idUsuario;
    }
    
    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }
    
    public String getTokenSesion() {
        return tokenSesion;
    }
    
    public void setTokenSesion(String tokenSesion) {
        this.tokenSesion = tokenSesion;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getHostname() {
        return hostname;
    }
    
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
    
    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }
    
    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }
    
    public LocalDateTime getFechaUltimoPing() {
        return fechaUltimoPing;
    }
    
    public void setFechaUltimoPing(LocalDateTime fechaUltimoPing) {
        this.fechaUltimoPing = fechaUltimoPing;
    }
    
    public Integer getIdCajaAbierta() {
        return idCajaAbierta;
    }
    
    public void setIdCajaAbierta(Integer idCajaAbierta) {
        this.idCajaAbierta = idCajaAbierta;
    }
    
    public Integer getIdMovimientoCaja() {
        return idMovimientoCaja;
    }
    
    public void setIdMovimientoCaja(Integer idMovimientoCaja) {
        this.idMovimientoCaja = idMovimientoCaja;
    }
    
    public EstadoSesion getEstado() {
        return estado;
    }
    
    public void setEstado(EstadoSesion estado) {
        this.estado = estado;
    }
    
    public String getNavegador() {
        return navegador;
    }
    
    public void setNavegador(String navegador) {
        this.navegador = navegador;
    }
    
    public String getSistemaOperativo() {
        return sistemaOperativo;
    }
    
    public void setSistemaOperativo(String sistemaOperativo) {
        this.sistemaOperativo = sistemaOperativo;
    }
    
    // ===================================================================
    // MÉTODOS DE NEGOCIO
    // ===================================================================
    
    /**
     * Verifica si la sesión está activa.
     */
    public boolean estaActiva() {
        return estado == EstadoSesion.ACTIVA;
    }
    
    /**
     * Verifica si tiene una caja abierta.
     */
    public boolean tieneCajaAbierta() {
        return idCajaAbierta != null && idMovimientoCaja != null;
    }
    
    /**
     * Actualiza el timestamp del último ping.
     */
    public void actualizarPing() {
        this.fechaUltimoPing = LocalDateTime.now();
    }
    
    /**
     * Verifica si la sesión ha expirado por inactividad.
     * 
     * @param minutosInactividad Minutos de inactividad permitidos
     * @return true si la sesión expiró
     */
    public boolean haExpirado(int minutosInactividad) {
        if (fechaUltimoPing == null) {
            return true;
        }
        
        LocalDateTime limiteExpiracion = LocalDateTime.now().minusMinutes(minutosInactividad);
        return fechaUltimoPing.isBefore(limiteExpiracion);
    }
    
    // ===================================================================
    // OVERRIDE
    // ===================================================================
    
    @Override
    public String toString() {
        return String.format("Session{id=%d, user=%d, estado=%s, caja=%s}",
            idSesion, idUsuario, estado, tieneCajaAbierta() ? "SI" : "NO");
    }
}