package raven.clases.comercial;

/**
 * Clase para encapsular resultados de validación de elegibilidad para devoluciones
 * Proporciona información detallada sobre si una venta puede ser devuelta
 * 
 * APLICANDO PRINCIPIOS SOLID:
 * - SRP: Responsabilidad única de contener resultado de validación
 * - OCP: Extensible para nuevas reglas de validación
 * - ISP: Interface específica para validación de elegibilidad
 */
public class ValidacionElegibilidad {
    
    // ====================================================================
    // CRITERIOS DE VALIDACIÓN
    // ====================================================================
    private final boolean estadoValido;           // Estado 'completada'
    private final boolean periodoValido;          // Dentro de 30 días
    private final boolean sinDevolucionesPrevias; // No tiene devoluciones
    private final boolean montoValido;            // Total > 0
    
    // ====================================================================
    // INFORMACIÓN ADICIONAL
    // ====================================================================
    private final int diasTranscurridos;
    private final String mensaje;
    private final NivelValidacion nivel;
    
    // ====================================================================
    // CAMPOS OPCIONALES PARA REGLAS ESPECÍFICAS
    // ====================================================================
    private boolean requiereAutorizacionEspecial;
    private String razonAutorizacion;
    private boolean aplicaExcepcion;
    private String detalleExcepcion;
    
    /**
     * Constructor principal para validación de elegibilidad
     * 
     * @param estadoValido Si el estado de la venta es válido
     * @param periodoValido Si está dentro del período permitido
     * @param sinDevolucionesPrevias Si no tiene devoluciones previas
     * @param montoValido Si el monto es válido para devolución
     * @param diasTranscurridos Días transcurridos desde la venta
     * @param mensaje Mensaje descriptivo del resultado
     */
    public ValidacionElegibilidad(boolean estadoValido, boolean periodoValido, 
                                 boolean sinDevolucionesPrevias, boolean montoValido,
                                 int diasTranscurridos, String mensaje) {
        
        this.estadoValido = estadoValido;
        this.periodoValido = periodoValido;
        this.sinDevolucionesPrevias = sinDevolucionesPrevias;
        this.montoValido = montoValido;
        this.diasTranscurridos = Math.max(0, diasTranscurridos);
        this.mensaje = mensaje != null ? mensaje : "Sin mensaje";
        
        // Determinar nivel de validación
        this.nivel = determinarNivelValidacion();
        
        // Inicializar campos opcionales
        this.requiereAutorizacionEspecial = false;
        this.razonAutorizacion = "";
        this.aplicaExcepcion = false;
        this.detalleExcepcion = "";
    }

    ValidacionElegibilidad(boolean elegible, boolean elegibleConAutorizacion, int diasTranscurridos, String mensaje) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
    // ====================================================================
    // MÉTODOS DE VALIDACIÓN PRINCIPALES
    // ====================================================================
    
    /**
     * Verifica si la venta es completamente elegible para devolución
     * @return true si cumple todos los criterios
     */
    public boolean isElegible() {
        return estadoValido && periodoValido && sinDevolucionesPrevias && montoValido;
    }
    
    /**
     * Verifica si la venta es elegible con autorización especial
     * @return true si puede ser elegible con autorización
     */
    public boolean isElegibleConAutorizacion() {
        return estadoValido && montoValido && sinDevolucionesPrevias;
        // El período puede ser excedido con autorización
    }
    
    /**
     * Verifica si es elegible para devolución parcial
     * @return true si permite devolución parcial
     */
    public boolean isElegibleParcial() {
        return estadoValido && montoValido;
        // Criterios más flexibles para devolución parcial
    }
    
    // ====================================================================
    // GETTERS PRINCIPALES
    // ====================================================================
    
    public boolean isEstadoValido() {
        return estadoValido;
    }
    
    public boolean isPeriodoValido() {
        return periodoValido;
    }
    
    public boolean isSinDevolucionesPrevias() {
        return sinDevolucionesPrevias;
    }
    
    public boolean isMontoValido() {
        return montoValido;
    }
    
    public int getDiasTranscurridos() {
        return diasTranscurridos;
    }
    
    public String getMensaje() {
        return mensaje;
    }
    
    public NivelValidacion getNivel() {
        return nivel;
    }
    
    // ====================================================================
    // GETTERS/SETTERS CAMPOS OPCIONALES
    // ====================================================================
    
    public boolean isRequiereAutorizacionEspecial() {
        return requiereAutorizacionEspecial;
    }
    
    public void setRequiereAutorizacionEspecial(boolean requiereAutorizacionEspecial, String razon) {
        this.requiereAutorizacionEspecial = requiereAutorizacionEspecial;
        this.razonAutorizacion = razon != null ? razon : "";
    }
    
    public String getRazonAutorizacion() {
        return razonAutorizacion;
    }
    
    public boolean isAplicaExcepcion() {
        return aplicaExcepcion;
    }
    
    public void setAplicaExcepcion(boolean aplicaExcepcion, String detalle) {
        this.aplicaExcepcion = aplicaExcepcion;
        this.detalleExcepcion = detalle != null ? detalle : "";
    }
    
    public String getDetalleExcepcion() {
        return detalleExcepcion;
    }
    
    // ====================================================================
    // MÉTODOS DE ANÁLISIS Y PRESENTACIÓN
    // ====================================================================
    
    /**
     * Obtiene un resumen completo de la validación
     * @return Resumen detallado de todos los criterios
     */
    public String getResumenCompleto() {
        StringBuilder resumen = new StringBuilder();
        
        resumen.append("VALIDACIÓN DE ELEGIBILIDAD\n");
        resumen.append("========================\n\n");
        
        resumen.append("Estado de venta: ").append(estadoValido ? "OK Válido" : " Inválido").append("\n");
        resumen.append("Período de devolución: ").append(periodoValido ? "OK Dentro del límite" : " Expirado").append("\n");
        resumen.append("Devoluciones previas: ").append(sinDevolucionesPrevias ? "OK Sin devoluciones" : " Ya tiene devoluciones").append("\n");
        resumen.append("Monto válido: ").append(montoValido ? "OK Válido" : " Inválido").append("\n");
        
        resumen.append("\nDías transcurridos: ").append(diasTranscurridos).append("\n");
        resumen.append("Nivel de validación: ").append(nivel.getDescripcion()).append("\n");
        
        if (requiereAutorizacionEspecial) {
            resumen.append("\nWARNING  REQUIERE AUTORIZACIÓN ESPECIAL\n");
            resumen.append("Razón: ").append(razonAutorizacion).append("\n");
        }
        
        if (aplicaExcepcion) {
            resumen.append("\n APLICA EXCEPCIÓN\n");
            resumen.append("Detalle: ").append(detalleExcepcion).append("\n");
        }
        
        resumen.append("\nResultado: ").append(mensaje);
        
        return resumen.toString();
    }
    
    /**
     * Obtiene los criterios que fallan
     * @return Lista de criterios no cumplidos
     */
    public String getCriteriosFallidos() {
        StringBuilder fallos = new StringBuilder();
        
        if (!estadoValido) {
            fallos.append("• Estado de venta inválido\n");
        }
        if (!periodoValido) {
            fallos.append("• Período de devolución expirado (").append(diasTranscurridos).append(" días)\n");
        }
        if (!sinDevolucionesPrevias) {
            fallos.append("• Ya tiene devoluciones procesadas\n");
        }
        if (!montoValido) {
            fallos.append("• Monto de venta inválido\n");
        }
        
        return fallos.length() > 0 ? fallos.toString() : "Todos los criterios se cumplen";
    }
    
    /**
     * Obtiene recomendaciones de acción
     * @return Recomendaciones basadas en el estado de validación
     */
    public String getRecomendaciones() {
        if (isElegible()) {
            return "OK Proceder con la devolución normal";
        }
        
        if (isElegibleConAutorizacion()) {
            return "WARNING  Requiere autorización gerencial para proceder";
        }
        
        if (isElegibleParcial()) {
            return " Considerar devolución parcial con condiciones especiales";
        }
        
        StringBuilder recomendaciones = new StringBuilder();
        recomendaciones.append(" No se puede procesar devolución:\n");
        
        if (!estadoValido) {
            recomendaciones.append("• Verificar estado de la venta\n");
        }
        if (!periodoValido && diasTranscurridos > 30) {
            recomendaciones.append("• Consultar políticas de excepción para ventas expiradas\n");
        }
        if (!sinDevolucionesPrevias) {
            recomendaciones.append("• Revisar devoluciones previas\n");
        }
        if (!montoValido) {
            recomendaciones.append("• Verificar monto de la venta\n");
        }
        
        return recomendaciones.toString();
    }
    
    /**
     * Obtiene el color recomendado para UI según el nivel de validación
     * @return Código hexadecimal del color
     */
    public String getColorUI() {
        return nivel.getColorHex();
    }
    
    /**
     * Obtiene el ícono recomendado para UI
     * @return Carácter de ícono Unicode
     */
    public String getIconoUI() {
        return nivel.getIcono();
    }
    
    // ====================================================================
    // MÉTODOS PRIVADOS DE ANÁLISIS
    // ====================================================================
    
    /**
     * Determina el nivel de validación basado en los criterios
     * @return Nivel de validación correspondiente
     */
    private NivelValidacion determinarNivelValidacion() {
        if (isElegible()) {
            return NivelValidacion.VALIDO;
        }
        
        if (isElegibleConAutorizacion()) {
            return NivelValidacion.REQUIERE_AUTORIZACION;
        }
        
        if (isElegibleParcial()) {
            return NivelValidacion.PARCIALMENTE_ELEGIBLE;
        }
        
        // Verificar si es por período expirado únicamente
        if (estadoValido && montoValido && sinDevolucionesPrevias && !periodoValido) {
            return NivelValidacion.PERIODO_EXPIRADO;
        }
        
        return NivelValidacion.NO_ELEGIBLE;
    }
    
    // ====================================================================
    // MÉTODOS DE UTILIDAD
    // ====================================================================
    
    /**
     * Crea una validación para venta completamente elegible
     * @param diasTranscurridos Días desde la venta
     * @return Validación exitosa
     */
    public static ValidacionElegibilidad crearElegible(int diasTranscurridos) {
        return new ValidacionElegibilidad(
            true, true, true, true, 
            diasTranscurridos, 
            "Venta elegible para devolución completa"
        );
    }
    
    /**
     * Crea una validación para venta no elegible por período expirado
     * @param diasTranscurridos Días desde la venta
     * @return Validación con período expirado
     */
    public static ValidacionElegibilidad crearPeriodoExpirado(int diasTranscurridos) {
        ValidacionElegibilidad validacion = new ValidacionElegibilidad(
            true, false, true, true,
            diasTranscurridos,
            String.format("Período de devolución expirado (%d días)", diasTranscurridos)
        );
        
        validacion.setRequiereAutorizacionEspecial(true, 
            "Período de 30 días excedido - Requiere autorización gerencial");
        
        return validacion;
    }
    
    /**
     * Crea una validación para venta con devoluciones previas
     * @param diasTranscurridos Días desde la venta
     * @return Validación con devoluciones previas
     */
    public static ValidacionElegibilidad crearConDevolucionesPrevias(int diasTranscurridos) {
        return new ValidacionElegibilidad(
            true, true, false, true,
            diasTranscurridos,
            "Esta venta ya tiene devoluciones procesadas"
        );
    }
    
    @Override
    public String toString() {
        return String.format("ValidacionElegibilidad{elegible=%s, nivel=%s, dias=%d, mensaje='%s'}",
                           isElegible(), nivel, diasTranscurridos, mensaje);
    }
    
    // ====================================================================
    // ENUMERACIÓN PARA NIVELES DE VALIDACIÓN
    // ====================================================================
    
    /**
     * Niveles de validación de elegibilidad
     */
    public enum NivelValidacion {
        VALIDO("Válido", "#4CAF50", "OK"),
        REQUIERE_AUTORIZACION("Requiere Autorización", "#FF9800", "!"),
        PARCIALMENTE_ELEGIBLE("Parcialmente Elegible", "#2196F3", "i"),
        PERIODO_EXPIRADO("Período Expirado", "#FF5722", "T"),
        NO_ELEGIBLE("No Elegible", "#F44336", "X");
        
        private final String descripcion;
        private final String colorHex;
        private final String icono;
        
        NivelValidacion(String descripcion, String colorHex, String icono) {
            this.descripcion = descripcion;
            this.colorHex = colorHex;
            this.icono = icono;
        }
        
        public String getDescripcion() {
            return descripcion;
        }
        
        public String getColorHex() {
            return colorHex;
        }
        
        public String getIcono() {
            return icono;
        }
    }
}

