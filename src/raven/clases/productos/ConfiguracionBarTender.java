package raven.clases.productos;

import java.awt.print.PageFormat;
import java.awt.print.Paper;

/**
 * ConfiguracionBarTender
 * 
 * Clase que representa una configuración completa de BarTender
 * con todos los parámetros técnicos necesarios para impresión
 */
public class ConfiguracionBarTender {
    
    // Propiedades principales
    public String nombre;
    public double anchoMm;
    public double altoMm;
    public String orientacion;  // "VERTICAL_180", "PORTRAIT", "LANDSCAPE", "HORIZONTAL_180"
    public double margenMm;
    
    // Parámetros técnicos específicos
    public double velocidadMmSeg;      // 152.40 mm/seg (detectado)
    public int oscuridad;              // 0-8, actual: 6-7
    public double resolucionPpmm;      // 8.0 ppmm (puntos por milímetro)
    public double alturaEspacioMm;     // 3.0 mm (detección de brecha)
    public String metodoImpresion;     // "TERMICA_DIRECTA"
    public String accionPostImpresion; // "RASGAR", "CORTAR", "ESPERAR"
    
    // Información adicional
    public String preestablecido;      // "ETIQUETA", "USER", "DEFAULT"
    public boolean esActiva;           // Si es la configuración actualmente activa
    public String softwareOrigen;      // "BarTender", "Windows", etc.
    public String tipoMaterial;        // "Etiquetas con espacios", etc.
    
    // Constructor completo
    public ConfiguracionBarTender(String nombre, double anchoMm, double altoMm, 
                                String orientacion, double margenMm,
                                double velocidadMmSeg, int oscuridad, 
                                double resolucionPpmm, double alturaEspacioMm,
                                String metodoImpresion, String accionPostImpresion,
                                String preestablecido, boolean esActiva, 
                                String softwareOrigen) {
        this.nombre = nombre;
        this.anchoMm = anchoMm;
        this.altoMm = altoMm;
        this.orientacion = orientacion;
        this.margenMm = margenMm;
        this.velocidadMmSeg = velocidadMmSeg;
        this.oscuridad = oscuridad;
        this.resolucionPpmm = resolucionPpmm;
        this.alturaEspacioMm = alturaEspacioMm;
        this.metodoImpresion = metodoImpresion;
        this.accionPostImpresion = accionPostImpresion;
        this.preestablecido = preestablecido;
        this.esActiva = esActiva;
        this.softwareOrigen = softwareOrigen;
        this.tipoMaterial = "Etiquetas con espacios";
    }
    
    /**
     * Constructor simplificado para configuraciones básicas
     */
    public ConfiguracionBarTender(String nombre, double anchoMm, double altoMm, 
                                String orientacion, boolean esActiva) {
        this(nombre, anchoMm, altoMm, orientacion, 2.0,  // Margen por defecto
             152.40, 6, 8.0, 3.0, "TERMICA_DIRECTA", "RASGAR",  // Parámetros técnicos por defecto
             "DEFAULT", esActiva, "BarTender");
    }
    
    /**
     * Crea PageFormat basado en esta configuración
     */
    public PageFormat crearPageFormat() {
        PageFormat pf = new PageFormat();
        Paper paper = new Paper();
        
        // Convertir mm a puntos (1 inch = 72 puntos, 1 mm = 25.4 puntos)
        double anchoPuntos = anchoMm * 72.0 / 25.4;
        double altoPuntos = altoMm * 72.0 / 25.4;
        double margenPuntos = margenMm * 72.0 / 25.4;
        
        // Configurar tamaño del papel
        paper.setSize(anchoPuntos, altoPuntos);
        
        // Configurar área imprimible
        double imagenableX = margenPuntos;
        double imagenableY = margenPuntos;
        double imagenableWidth = anchoPuntos - (margenPuntos * 2);
        double imagenableHeight = altoPuntos - (margenPuntos * 2);
        
        paper.setImageableArea(imagenableX, imagenableY, 
                             imagenableWidth, imagenableHeight);
        
        pf.setPaper(paper);
        
        // Configurar orientación base
        if ("LANDSCAPE".equalsIgnoreCase(orientacion)) {
            pf.setOrientation(PageFormat.LANDSCAPE);
        } else if ("HORIZONTAL_180".equals(orientacion)) {
            pf.setOrientation(PageFormat.LANDSCAPE);
        } else {
            pf.setOrientation(PageFormat.PORTRAIT);
        }
        
        return pf;
    }
    
    /**
     * Aplica la configuración en un ImpresorTermicaPOSDIG2406T
     */
    public void aplicarConfiguracionImpresor(ImpresorTermicaPOSDIG2406T impresor) {
        try {
            // Aplicar márgenes
            impresor.setMargenes(margenMm, margenMm/2.0, margenMm, margenMm/2.0);
            
            // Configurar PageFormat
            PageFormat pf = crearPageFormat();
            impresor.setPageFormat(pf);
            
            // Aplicar configuraciones específicas si el impresor las soporta
            aplicarConfiguracionesEspecificas(impresor);
            
            System.out.println("SUCCESS  Configuración aplicada en impresor: " + nombre);
            
        } catch (Exception e) {
            System.err.println("ERROR  Error aplicando configuración en impresor: " + e.getMessage());
            throw new RuntimeException("Error aplicando configuración", e);
        }
    }
    
    /**
     * Aplica configuraciones específicas adicionales
     */
    private void aplicarConfiguracionesEspecificas(ImpresorTermicaPOSDIG2406T impresor) {
        // Intentar aplicar configuraciones técnicas específicas
        // (esto requeriría que el impresor soporte estas configuraciones)
        
        try {
            // Aplicar orientación específica
            if ("VERTICAL_180".equals(orientacion) || "HORIZONTAL_180".equals(orientacion)) {
                System.out.println("    Aplicando rotación de 180°");
                // Nota: La rotación real se aplicaría en el método de impresión
            }
            
            // Configurar parámetros técnicos si están disponibles
            if (metodoImpresion.equals("TERMICA_DIRECTA")) {
                System.out.println("    Método: Térmica directa");
            }
            
            if (accionPostImpresion.equals("RASGAR")) {
                System.out.println("    Acción post-impresión: Rasgar");
            }
            
        } catch (Exception e) {
            System.out.println("   WARNING   Algunas configuraciones específicas no pudieron aplicarse");
        }
    }
    
    /**
     * Valida que la configuración sea correcta
     */
    public boolean validarConfiguracion() {
        // Validar dimensiones
        if (anchoMm <= 0 || altoMm <= 0) {
            System.err.println("ERROR  Dimensiones inválidas: " + anchoMm + " x " + altoMm);
            return false;
        }
        
        // Validar orientación
        if (!esOrientacionValida(orientacion)) {
            System.err.println("ERROR  Orientación inválida: " + orientacion);
            return false;
        }
        
        // Validar parámetros técnicos
        if (velocidadMmSeg <= 0 || velocidadMmSeg > 500) {
            System.err.println("ERROR  Velocidad inválida: " + velocidadMmSeg);
            return false;
        }
        
        if (oscuridad < 0 || oscuridad > 8) {
            System.err.println("ERROR  Oscuridad inválida: " + oscuridad);
            return false;
        }
        
        if (resolucionPpmm <= 0) {
            System.err.println("ERROR  Resolución inválida: " + resolucionPpmm);
            return false;
        }
        
        // Validaciones adicionales específicas para XP-420B
        if (nombre.toLowerCase().contains("pares_siro")) {
            // Validar dimensiones específicas de PARES_SIRO
            if (Math.abs(anchoMm - 105.0) > 1.0 || Math.abs(altoMm - 25.0) > 1.0) {
                System.err.println("WARNING   Dimensiones de PARES_SIRO diferentes a las esperadas");
            }
        }
        
        return true;
    }

   

//    public static ConfiguracionBarTender crearDesdeJson(String json) {
//        // Reconstrucción mínima: devuelve objeto base con valores por defecto
//        return new ConfiguracionBarTender("PARES_SIRO", 105.0, 25.0, "VERTICAL_180", true);
//    }

 
    
    /**
     * Verifica si la orientación es válida
     */
    private boolean esOrientacionValida(String orientacion) {
        return orientacion.equals("PORTRAIT") ||
               orientacion.equals("LANDSCAPE") ||
               orientacion.equals("VERTICAL_180") ||
               orientacion.equals("HORIZONTAL_180");
    }
    
    /**
     * Obtiene la rotación específica para aplicar al contenido
     */
    public double obtenerRotacionGrados() {
        switch (orientacion) {
            case "VERTICAL_180": return 180.0;
            case "HORIZONTAL_180": return 180.0;
            case "PORTRAIT": return 0.0;
            case "LANDSCAPE": return 0.0;
            default: return 0.0;
        }
    }
    
    /**
     * Determina si necesita rotación en el contenido
     */
    public boolean requiereRotacionContenido() {
        return "VERTICAL_180".equals(orientacion) || "HORIZONTAL_180".equals(orientacion);
    }
    
    /**
     * Obtiene información resumida para mostrar al usuario
     */
    public String obtenerInformacionResumen() {
        return String.format("%s: %.1f×%.1fmm (%s)%s", 
            nombre, anchoMm, altoMm, orientacion, 
            esActiva ? " [ACTIVA]" : "");
    }
    
    /**
     * Convierte a string con información completa
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(nombre);
        sb.append(": ").append(String.format("%.1f", anchoMm))
          .append("×").append(String.format("%.1f", altoMm)).append("mm");
        sb.append(" (").append(orientacion).append(")");
        
        if (esActiva) {
            sb.append(" [ACTIVA]");
        }
        
        sb.append(" [").append(softwareOrigen).append("]");
        
        return sb.toString();
    }
    
    /**
     * Convierte a JSON para persistencia
     */
    public String convertirAJson() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"nombre\": \"").append(nombre).append("\",");
        json.append("\"anchoMm\": ").append(anchoMm).append(",");
        json.append("\"altoMm\": ").append(altoMm).append(",");
        json.append("\"orientacion\": \"").append(orientacion).append("\",");
        json.append("\"margenMm\": ").append(margenMm).append(",");
        json.append("\"velocidadMmSeg\": ").append(velocidadMmSeg).append(",");
        json.append("\"oscuridad\": ").append(oscuridad).append(",");
        json.append("\"resolucionPpmm\": ").append(resolucionPpmm).append(",");
        json.append("\"alturaEspacioMm\": ").append(alturaEspacioMm).append(",");
        json.append("\"metodoImpresion\": \"").append(metodoImpresion).append("\",");
        json.append("\"accionPostImpresion\": \"").append(accionPostImpresion).append("\",");
        json.append("\"preestablecido\": \"").append(preestablecido).append("\",");
        json.append("\"esActiva\": ").append(esActiva).append(",");
        json.append("\"softwareOrigen\": \"").append(softwareOrigen).append("\",");
        json.append("\"tipoMaterial\": \"").append(tipoMaterial).append("\"");
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * Crea configuración desde JSON
     */
    public static ConfiguracionBarTender crearDesdeJson(String json) {
        // Implementación básica - en producción usar librería JSON
        try {
            // Parseo básico (mejor usar librerías como Gson o Jackson)
            String[] pares = json.replaceAll("[{}]", "").split(",");
            
            // Valores por defecto
            String nombre = "Configuración JSON";
            double anchoMm = 105.0, altoMm = 25.0;
            String orientacion = "VERTICAL_180";
            double margenMm = 2.0;
            double velocidadMmSeg = 152.40;
            int oscuridad = 6;
            double resolucionPpmm = 8.0;
            double alturaEspacioMm = 3.0;
            String metodoImpresion = "TERMICA_DIRECTA";
            String accionPostImpresion = "RASGAR";
            String preestablecido = "DEFAULT";
            boolean esActiva = false;
            String softwareOrigen = "BarTender";
            
            // Parsear valores (simplificado)
            for (String par : pares) {
                String[] keyValue = par.split(":");
                if (keyValue.length == 2) {
                    String key = keyValue[0].replaceAll("[\"\\s]", "");
                    String value = keyValue[1].replaceAll("[\"\\s]", "");
                    
                    switch (key) {
                        case "nombre": nombre = value; break;
                        case "anchoMm": anchoMm = Double.parseDouble(value); break;
                        case "altoMm": altoMm = Double.parseDouble(value); break;
                        case "orientacion": orientacion = value; break;
                        case "margenMm": margenMm = Double.parseDouble(value); break;
                        case "esActiva": esActiva = Boolean.parseBoolean(value); break;
                        case "softwareOrigen": softwareOrigen = value; break;
                    }
                }
            }
            
            return new ConfiguracionBarTender(nombre, anchoMm, altoMm, orientacion, margenMm,
                velocidadMmSeg, oscuridad, resolucionPpmm, alturaEspacioMm,
                metodoImpresion, accionPostImpresion, preestablecido, esActiva, softwareOrigen);
                
        } catch (Exception e) {
            System.err.println("ERROR  Error parseando JSON: " + e.getMessage());
            // Retornar configuración por defecto
            return new ConfiguracionBarTender("Configuración Error", 105.0, 25.0, 
                "VERTICAL_180", true);
        }
    }
}
