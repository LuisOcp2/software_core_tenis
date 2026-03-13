package raven.clases.comercial;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Modelo de datos para resultados de búsqueda de ventas
 * Optimizado para el sistema de devoluciones
 * 
 * APLICANDO PRINCIPIOS SOLID:
 * - SRP: Responsabilidad única de contener datos de resultado de búsqueda
 * - OCP: Extensible para nuevos campos sin modificar código existente
 * - ISP: Interface segregada - solo los datos necesarios para búsqueda
 */
public class resultadosBusqueda {
    
    // ====================================================================
    // CONSTANTES DE FORMATO
    // ====================================================================
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    // ====================================================================
    // DATOS BÁSICOS DE LA VENTA
    // ====================================================================
    private final int idVenta;
    private final LocalDateTime fechaVenta;
    private final double total;
    private final String estado;
    private final String tipoPago;
    
    // ====================================================================
    // INFORMACIÓN DEL CLIENTE
    // ====================================================================
    private final Integer idCliente; // Puede ser null para cliente general
    private final String clienteNombre;
    private final String clienteDni;
    private final String clienteTelefono;
    
    // ====================================================================
    // INFORMACIÓN DEL VENDEDOR
    // ====================================================================
    private final int idUsuario;
    private final String usuarioNombre;
    
    // ====================================================================
    // INFORMACIÓN ESPECÍFICA PARA DEVOLUCIONES
    // ====================================================================
    private final int diasTranscurridos;
    private final boolean elegibleDevolucion;
    private final int totalProductos;
    private final int totalItems;
    
    // ====================================================================
    // CAMPOS OPCIONALES PARA EXTENSIBILIDAD
    // ====================================================================
    private String detallesPago; // Para pagos mixtos
    private String observacionesElegibilidad; // Razones de elegibilidad
    private boolean requiereAutorizacion; // Para montos altos
    
    /**
     * Constructor principal para resultados de búsqueda
     * 
     * @param idVenta ID único de la venta
     * @param fechaVenta Fecha y hora de la venta
     * @param total Monto total de la venta
     * @param estado Estado actual de la venta
     * @param tipoPago Método de pago utilizado
     * @param idCliente ID del cliente (puede ser null)
     * @param clienteNombre Nombre del cliente
     * @param clienteDni DNI del cliente
     * @param clienteTelefono Teléfono del cliente
     * @param idUsuario ID del usuario vendedor
     * @param usuarioNombre Nombre del vendedor
     * @param diasTranscurridos Días desde la venta
     * @param elegibleDevolucion Si es elegible para devolución
     * @param totalProductos Cantidad de productos diferentes
     * @param totalItems Cantidad total de items vendidos
     */
    public resultadosBusqueda(int idVenta, LocalDateTime fechaVenta, double total, 
                                 String estado, String tipoPago,
                                 Integer idCliente, String clienteNombre, String clienteDni, String clienteTelefono,
                                 int idUsuario, String usuarioNombre,
                                 int diasTranscurridos, boolean elegibleDevolucion,
                                 int totalProductos, int totalItems) {
        
        // VALIDACIONES BÁSICAS
        if (idVenta <= 0) {
            throw new IllegalArgumentException("ID de venta debe ser mayor a 0");
        }
        if (fechaVenta == null) {
            throw new IllegalArgumentException("Fecha de venta no puede ser null");
        }
        if (usuarioNombre == null || usuarioNombre.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de usuario no puede estar vacío");
        }
        
        // ASIGNACIÓN DE VALORES
        this.idVenta = idVenta;
        this.fechaVenta = fechaVenta;
        this.total = total;
        this.estado = estado != null ? estado : "desconocido";
        this.tipoPago = tipoPago != null ? tipoPago : "no especificado";
        
        this.idCliente = idCliente;
        this.clienteNombre = clienteNombre != null ? clienteNombre : "Cliente General";
        this.clienteDni = clienteDni;
        this.clienteTelefono = clienteTelefono;
        
        this.idUsuario = idUsuario;
        this.usuarioNombre = usuarioNombre;
        
        this.diasTranscurridos = Math.max(0, diasTranscurridos);
        this.elegibleDevolucion = elegibleDevolucion;
        this.totalProductos = Math.max(0, totalProductos);
        this.totalItems = Math.max(0, totalItems);
        
        // INICIALIZAR CAMPOS OPCIONALES
        this.detallesPago = "";
        this.observacionesElegibilidad = "";
        this.requiereAutorizacion = total > 500.00; // Regla de negocio configurable
    }
    
    // ====================================================================
    // GETTERS PRINCIPALES
    // ====================================================================
    
    public int getIdVenta() {
        return idVenta;
    }
    
    public LocalDateTime getFechaVenta() {
        return fechaVenta;
    }
    
    public double getTotal() {
        return total;
    }
    
    public String getEstado() {
        return estado;
    }
    
    public String getTipoPago() {
        return tipoPago;
    }
    
    public Integer getIdCliente() {
        return idCliente;
    }
    
    public String getClienteNombre() {
        return clienteNombre;
    }
    
    public String getClienteDni() {
        return clienteDni;
    }
    
    public String getClienteTelefono() {
        return clienteTelefono;
    }
    
    public int getIdUsuario() {
        return idUsuario;
    }
    
    public String getUsuarioNombre() {
        return usuarioNombre;
    }
    
    public int getDiasTranscurridos() {
        return diasTranscurridos;
    }
    
    public boolean isElegibleDevolucion() {
        return elegibleDevolucion;
    }
    
    public int getTotalProductos() {
        return totalProductos;
    }
    
    public int getTotalItems() {
        return totalItems;
    }
    
    // ====================================================================
    // GETTERS/SETTERS CAMPOS OPCIONALES
    // ====================================================================
    
    public String getDetallesPago() {
        return detallesPago;
    }
    
    public void setDetallesPago(String detallesPago) {
        this.detallesPago = detallesPago != null ? detallesPago : "";
    }
    
    public String getObservacionesElegibilidad() {
        return observacionesElegibilidad;
    }
    
    public void setObservacionesElegibilidad(String observacionesElegibilidad) {
        this.observacionesElegibilidad = observacionesElegibilidad != null ? observacionesElegibilidad : "";
    }
    
    public boolean isRequiereAutorizacion() {
        return requiereAutorizacion;
    }
    
    public void setRequiereAutorizacion(boolean requiereAutorizacion) {
        this.requiereAutorizacion = requiereAutorizacion;
    }
    
    // ====================================================================
    // MÉTODOS DE PRESENTACIÓN
    // ====================================================================
    
    /**
     * Obtiene el número de venta formateado para mostrar en UI
     * @return Número de venta con formato VEN-XXXXXX
     */
    public String getNumeroVentaFormateado() {
        return String.format("VEN-%06d", idVenta);
    }
    
    /**
     * Obtiene la fecha formateada para mostrar en UI
     * @return Fecha en formato dd/MM/yyyy
     */
    public String getFechaVentaFormateada() {
        return fechaVenta.format(FORMATO_FECHA);
    }
    
    /**
     * Obtiene fecha y hora formateadas para detalles
     * @return Fecha y hora en formato dd/MM/yyyy HH:mm
     */
    public String getFechaHoraFormateada() {
        return fechaVenta.format(FORMATO_FECHA_HORA);
    }
    
    /**
     * Obtiene el total formateado como moneda
     * @return Total con formato $X,XXX.XX
     */
    public String getTotalFormateado() {
        return String.format("$%,.2f", total);
    }
    
    /**
     * Obtiene información del cliente concatenada
     * @return Cliente con DNI si existe, solo nombre si no
     */
    public String getClienteCompleto() {
        if (clienteDni != null && !clienteDni.trim().isEmpty()) {
            return String.format("%s (DNI: %s)", clienteNombre, clienteDni);
        }
        return clienteNombre;
    }
    
    /**
     * Obtiene información de contacto del cliente
     * @return Información de contacto disponible
     */
    public String getContactoCliente() {
        StringBuilder contacto = new StringBuilder();
        
        if (clienteDni != null && !clienteDni.trim().isEmpty()) {
            contacto.append("DNI: ").append(clienteDni);
        }
        
        if (clienteTelefono != null && !clienteTelefono.trim().isEmpty()) {
            if (contacto.length() > 0) contacto.append(" | ");
            contacto.append("Tel: ").append(clienteTelefono);
        }
        
        return contacto.length() > 0 ? contacto.toString() : "Sin información de contacto";
    }
    
    /**
     * Obtiene resumen de productos vendidos
     * @return Descripción de cantidad de productos e items
     */
    public String getResumenProductos() {
        if (totalProductos == totalItems) {
            return String.format("%d producto%s", totalProductos, totalProductos != 1 ? "s" : "");
        } else {
            return String.format("%d productos (%d items)", totalProductos, totalItems);
        }
    }
    
    /**
     * Obtiene el estado de elegibilidad con descripción
     * @return Estado descriptivo de elegibilidad
     */
    public String getEstadoElegibilidad() {
        if (elegibleDevolucion) {
            return String.format("OK Elegible (%d días)", diasTranscurridos);
        } else {
            if (diasTranscurridos > 30) {
                return String.format(" Expirada (%d días)", diasTranscurridos);
            } else {
                return " No elegible";
            }
        }
    }
    
    /**
     * Obtiene información del tipo de pago con detalles si es mixto
     * @return Información completa del pago
     */
    public String getInfoPagoCompleta() {
        if ("mixto".equals(tipoPago) && !detallesPago.isEmpty()) {
            return String.format("%s (%s)", tipoPago, detallesPago);
        }
        return tipoPago;
    }
    
    // ====================================================================
    // MÉTODOS DE VALIDACIÓN Y ESTADO
    // ====================================================================
    
    /**
     * Verifica si la venta tiene cliente específico (no es cliente general)
     * @return true si tiene cliente específico
     */
    public boolean tieneClienteEspecifico() {
        return idCliente != null && idCliente > 0;
    }
    
    /**
     * Verifica si la venta está dentro del período de devolución
     * @return true si está dentro del período permitido
     */
    public boolean dentroDelPeriodoDevolucion() {
        return diasTranscurridos <= 30; // Configurable según reglas de negocio
    }
    
    /**
     * Verifica si es una venta de alto valor que requiere autorización
     * @return true si requiere autorización especial
     */
    public boolean esVentaAltoValor() {
        return total > 500.00; // Configurable según reglas de negocio
    }
    
    /**
     * Obtiene la prioridad de la venta para procesamiento
     * @return Nivel de prioridad (ALTA, MEDIA, BAJA)
     */
    public PrioridadVenta getPrioridad() {
        if (!elegibleDevolucion) {
            return PrioridadVenta.BAJA;
        }
        
        if (diasTranscurridos > 25 || total > 1000.00) {
            return PrioridadVenta.ALTA;
        }
        
        if (diasTranscurridos > 15 || total > 500.00) {
            return PrioridadVenta.MEDIA;
        }
        
        return PrioridadVenta.BAJA;
    }
    
    /**
     * Obtiene mensaje de advertencia si corresponde
     * @return Mensaje de advertencia o cadena vacía
     */
    public String getMensajeAdvertencia() {
        StringBuilder advertencias = new StringBuilder();
        
        if (diasTranscurridos > 25 && elegibleDevolucion) {
            advertencias.append("Advertencia: Próximo a vencer (").append(30 - diasTranscurridos).append(" días restantes). ");
        }
        
        if (total > 1000.00) {
            advertencias.append("Advertencia: Venta de alto valor - Revisar políticas especiales. ");
        }
        
        if ("cancelada".equals(estado)) {
            advertencias.append("Advertencia: Venta previamente cancelada. ");
        }
        
        if (!observacionesElegibilidad.isEmpty()) {
            advertencias.append("Info: ").append(observacionesElegibilidad);
        }
        
        return advertencias.toString().trim();
    }
    
    // ====================================================================
    // MÉTODOS DE COMPARACIÓN Y ORDENAMIENTO
    // ====================================================================
    
    /**
     * Compara por fecha de venta (más reciente primero)
     */
    public int compararPorFecha(resultadosBusqueda otro) {
        return otro.fechaVenta.compareTo(this.fechaVenta);
    }
    
    /**
     * Compara por total de venta (mayor primero)
     */
    public int compararPorTotal(resultadosBusqueda otro) {
        return Double.compare(otro.total, this.total);
    }
    
    /**
     * Compara por elegibilidad (elegibles primero)
     */
    public int compararPorElegibilidad(resultadosBusqueda otro) {
        if (this.elegibleDevolucion && !otro.elegibleDevolucion) return -1;
        if (!this.elegibleDevolucion && otro.elegibleDevolucion) return 1;
        
        // Si ambos tienen la misma elegibilidad, ordenar por días transcurridos
        return Integer.compare(this.diasTranscurridos, otro.diasTranscurridos);
    }
    
    // ====================================================================
    // MÉTODOS DE UTILIDAD GENERAL
    // ====================================================================
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        resultadosBusqueda that = (resultadosBusqueda) obj;
        return idVenta == that.idVenta;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(idVenta);
    }
    
    @Override
    public String toString() {
        return String.format("ResultadoBusquedaVenta{id=%d, cliente='%s', total=%.2f, dias=%d, elegible=%s}",
                           idVenta, clienteNombre, total, diasTranscurridos, elegibleDevolucion);
    }
    
    /**
     * Crea una representación JSON simplificada para logging o APIs
     * @return String JSON con información básica
     */
    public String toJsonString() {
        return String.format(
            "{\"idVenta\":%d,\"fecha\"%s\",\"total\":%.2f,\"cliente\":\"%s\",\"elegible\":%s,\"dias\":%d}",
            idVenta, fechaVenta.format(DateTimeFormatter.ISO_LOCAL_DATE), total, 
            clienteNombre.replace("\"", "'"), elegibleDevolucion, diasTranscurridos
        );
    }
    
    // ====================================================================
    // ENUMERACIONES AUXILIARES
    // ====================================================================
    
    /**
     * Enum para niveles de prioridad en procesamiento de devoluciones
     */
    public enum PrioridadVenta {
        ALTA("Alta", "#FF4444"),
        MEDIA("Media", "#FFAA00"), 
        BAJA("Baja", "#44AA44");
        
        private final String descripcion;
        private final String colorHex;
        
        PrioridadVenta(String descripcion, String colorHex) {
            this.descripcion = descripcion;
            this.colorHex = colorHex;
        }
        
        public String getDescripcion() {
            return descripcion;
        }
        
        public String getColorHex() {
            return colorHex;
        }
    }
}

