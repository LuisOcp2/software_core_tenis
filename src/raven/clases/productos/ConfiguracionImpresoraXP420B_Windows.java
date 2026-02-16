package raven.clases.productos;

import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterJob;
import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

/**
 * Configuración inteligente que preserva configuraciones del driver de Windows
 * Integra las configuraciones nativas guardadas como "PARES_SIRO"
 */
public class ConfiguracionImpresoraXP420B_Windows {
    
    // Dimensiones básicas de la XP-420B como respaldo
    public static final double ANCHO_IMPRESORA_MM = 109.1;
    public static final double ALTO_IMPRESORA_MM = 25.02;
    public static final double ANCHO_ETIQUETA_MM = 35.0;
    public static final double ALTO_ETIQUETA_MM = 25.0;
    public static final int ETIQUETAS_POR_FILA = 3;
    
    /**
     * Detecta si estamos usando la impresora XP-420B usando configuración nativa
     * @return true si se detecta la XP-420B, false en otro caso
     */
    public static boolean esImpresoraXP420B() {
        PrintService[] servicios = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService servicio : servicios) {
            String nombre = servicio.getName().toLowerCase();
            if (nombre.contains("xp420b") || 
                nombre.contains("xp-420b") || 
                nombre.contains("xprinter")) {
                System.out.println("XP-420B detectada usando configuración nativa: " + servicio.getName());
                return true;
            }
        }
        return false;
    }
    
    /**
     * Busca la configuración "PARES_SIRO" guardada en el driver
     * @return Array con [ancho_mm, alto_mm] si se encuentra, dimensiones por defecto si no
     */
    public static double[] obtenerConfiguracionPARESSIRO() {
        PrintService[] servicios = PrintServiceLookup.lookupPrintServices(null, null);
        
        for (PrintService servicio : servicios) {
            if (!esImpresoraXP420BEspecifica(servicio)) continue;
            
            try {
                System.out.println("Buscando configuración PARES_SIRO en: " + servicio.getName());
                
                // Obtener tamaño de papel configurado actualmente
                Object mediaActual = servicio.getDefaultAttributeValue(Media.class);
                if (mediaActual != null) {
                    String nombreMedia = mediaActual.toString();
                    System.out.println("Tamaño de papel actual: " + nombreMedia);
                    
                    // Verificar si contiene "PARES_SIRO" o configuración personalizada
                    if (nombreMedia.contains("PARES_SIRO") || 
                        nombreMedia.contains("personalizado") ||
                        nombreMedia.contains("custom")) {
                        
                        // Intentar obtener dimensiones del media
                        if (mediaActual instanceof MediaSizeName) {
                            MediaSizeName msn = (MediaSizeName) mediaActual;
                            MediaSize ms = MediaSize.getMediaSizeForName(msn);
                            if (ms != null) {
                                float ancho = ms.getX(MediaSize.MM);
                                float alto = ms.getY(MediaSize.MM);
                                
                                System.out.println("OK Configuración PARES_SIRO encontrada: " + ancho + "x" + alto + "mm");
                                return new double[]{ancho, alto};
                            }
                        }
                        
                        // Si no se pueden obtener dimensiones exactas, usar típicas de PARES_SIRO
                        // (basado en que el usuario mencionó que funcionaba bien)
                        System.out.println("OK Usando dimensiones conocidas de PARES_SIRO (3 etiquetas)");
                        // Ajustado a 105mm (35mm * 3) x 25mm para garantizar 3 etiquetas
                        return new double[]{105.0, 25.0}; 
                    }
                }
                
                // Si no encontramos PARES_SIRO, obtener la configuración actual del driver
                double[] dimensiones = obtenerConfiguracionActualDelDriver(servicio);
                if (dimensiones[0] > 0 && dimensiones[1] > 0) {
                     // Verificar si la dimensión es sospechosamente pequeña para 3 etiquetas
                     if (ETIQUETAS_POR_FILA == 3 && dimensiones[0] < 80.0 && dimensiones[1] < 80.0) {
                         // Si el ancho detectado es muy pequeño (ej. 35mm), asumir que es una sola etiqueta
                         // y forzar el ancho total para 3 etiquetas
                         System.out.println("WARNING Dimensión detectada (" + dimensiones[0] + "mm) parece ser de una sola etiqueta.");
                         System.out.println("Forzando ancho para 3 etiquetas: 105mm");
                         return new double[]{105.0, 25.0};
                     }

                    System.out.println("OK Usando configuración actual del driver: " + 
                                     dimensiones[0] + "x" + dimensiones[1] + "mm");
                    return dimensiones;
                }
                
            } catch (Exception e) {
                System.out.println("WARNING  Error consultando configuración: " + e.getMessage());
            }
        }
        
        System.out.println("No se encontró configuración personalizada, usando dimensiones estándar para 3 etiquetas");
        // Default para 3 etiquetas: 35mm * 3 = 105mm ancho, 25mm alto
        return new double[]{105.0, 25.0};
    }
    
    /**
     * Verifica si un servicio específico es XP-420B
     */
    private static boolean esImpresoraXP420BEspecifica(PrintService servicio) {
        if (servicio == null) return false;
        String nombre = servicio.getName().toLowerCase();
        return nombre.contains("xp420b") || 
               nombre.contains("xp-420b") || 
               nombre.contains("xprinter");
    }
    
    /**
     * Obtiene la configuración actual del driver para una impresora específica
     */
    private static double[] obtenerConfiguracionActualDelDriver(PrintService servicio) {
        try {
            // Obtener orientación
            OrientationRequested orientacion = 
                (OrientationRequested) servicio.getDefaultAttributeValue(OrientationRequested.class);
            
            // Obtener tamaño de papel
            Object media = servicio.getDefaultAttributeValue(Media.class);
            
            if (media instanceof MediaSizeName) {
                MediaSizeName msn = (MediaSizeName) media;
                MediaSize ms = MediaSize.getMediaSizeForName(msn);
                if (ms != null) {
                    float ancho = ms.getX(MediaSize.MM);
                    float alto = ms.getY(MediaSize.MM);
                    
                    // Ajustar según orientación (para etiquetas horizontales vs verticales)
                    if (orientacion == OrientationRequested.LANDSCAPE) {
                        return new double[]{alto, ancho}; // Intercambiar si está en horizontal
                    } else {
                        return new double[]{ancho, alto};
                    }
                }
            }
            
        } catch (Exception e) {
            System.out.println("WARNING  Error obteniendo configuración del driver: " + e.getMessage());
        }
        
        return new double[]{0, 0}; // No se pudo obtener
    }
    
    /**
     * Crea PageFormat que preserva completamente la configuración del driver
     */
    public static PageFormat crearPageFormatConservandoDriver(PrinterJob job) {
        if (!esImpresoraXP420B()) {
            System.out.println("No es XP-420B, usando configuración estándar del sistema");
            return job.defaultPage();
        }
        
        System.out.println("Usando configuración que preserva el driver de Windows");
        
        // Obtener configuración guardada del usuario (PARES_SIRO u otra)
        double[] dimensionesUsuario = obtenerConfiguracionPARESSIRO();
        
        PageFormat pf = new PageFormat();
        Paper paper = new Paper();
        
        // Usar las dimensiones exactas que el usuario tiene configuradas
        double anchoMm = dimensionesUsuario[0];
        double altoMm = dimensionesUsuario[1];
        
        // Convertir a puntos
        double anchoPuntos = anchoMm * 72.0 / 25.4;
        double altoPuntos = altoMm * 72.0 / 25.4;
        
        // Configurar papel con dimensiones exactas del driver
        paper.setSize(anchoPuntos, altoPuntos);
        
        // Usar márgenes muy pequeños para no interferir con la configuración del usuario
        double margen = Math.min(anchoMm, altoMm) * 0.02; // Máximo 2% del lado menor
        margen = Math.max(margen, 0.5); // Mínimo 0.5mm
        
        double anchoUtil = anchoMm - (margen * 2);
        double altoUtil = altoMm - (margen * 2);
        
        paper.setImageableArea(
            margen * 72.0 / 25.4,
            margen * 72.0 / 25.4,
            anchoUtil * 72.0 / 25.4,
            altoUtil * 72.0 / 25.4
        );
        
        pf.setPaper(paper);
        
        // Usar orientación Landscape para etiquetas (3 etiquetas por fila)
        pf.setOrientation(PageFormat.LANDSCAPE);
        
        System.out.println("PageFormat creado con configuración del driver:");
        System.out.printf("  Dimensiones: %.1f x %.1f mm%n", anchoMm, altoMm);
        System.out.printf("  Orientación: LANDSCAPE%n");
        System.out.printf("  Márgenes: %.1f mm%n", margen);
        
        return pf;
    }
    
    /**
     * Crea PrintRequestAttributeSet con la configuración del driver
     * Esto asegura que Windows use exactamente la configuración guardada
     */
    public static PrintRequestAttributeSet crearAtributosConDriver() {
        PrintRequestAttributeSet atributos = new HashPrintRequestAttributeSet();
        
        System.out.println("Creando atributos con configuración del driver de Windows");
        
        // Buscar configuración XP-420B
        PrintService[] servicios = PrintServiceLookup.lookupPrintServices(null, null);
        
        for (PrintService servicio : servicios) {
            if (!esImpresoraXP420BEspecifica(servicio)) continue;
            
            try {
                // Obtener configuración actual del servicio
                AttributeSet attrs = servicio.getAttributes();
                
                // Aplicar orientación
                OrientationRequested orientacion = 
                    (OrientationRequested) attrs.get(OrientationRequested.class);
                if (orientacion != null) {
                    atributos.add(OrientationRequested.LANDSCAPE);
                    System.out.println("  Orientación: LANDSCAPE (para etiquetas)");
                }
                
                // Aplicar tamaño de papel
                Media media = (Media) attrs.get(Media.class);
                if (media != null) {
                    atributos.add(media);
                    System.out.println("  Tamaño papel: " + media);
                }
                
                // Aplicar calidad si está disponible
                PrintQuality calidad = (PrintQuality) attrs.get(PrintQuality.class);
                if (calidad != null) {
                    atributos.add(calidad);
                    System.out.println("  Calidad: " + calidad);
                }
                
                break; // Solo aplicar de la primera XP-420B encontrada
                
            } catch (Exception e) {
                System.out.println("WARNING  Error aplicando atributos: " + e.getMessage());
            }
        }
        
        return atributos;
    }
    
    /**
     * Método de diagnóstico para mostrar la configuración detectada
     */
    public static void diagnosticarConfiguracionXP420B() {
        System.out.println("\n=== DIAGNÓSTICO CONFIGURACIÓN XP-420B ===");
        
        // 1. Verificar si hay XP-420B disponible
        boolean hayXP420B = esImpresoraXP420B();
        System.out.println("¿Hay XP-420B disponible?: " + (hayXP420B ? "Sí" : "No"));
        
        if (!hayXP420B) {
            System.out.println(" Lista de impresoras disponibles:");
            ConfiguracionImpresoraWindows.listarTodasLasImpresoras();
            return;
        }
        
        // 2. Obtener configuración PARES_SIRO
        double[] config = obtenerConfiguracionPARESSIRO();
        System.out.println("Configuración detectada: " + config[0] + "x" + config[1] + "mm");
        
        // 3. Crear PageFormat y mostrar detalles
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            PageFormat pf = crearPageFormatConservandoDriver(job);
            
            Paper paper = pf.getPaper();
            double anchoMm = paper.getWidth() * 25.4 / 72.0;
            double altoMm = paper.getHeight() * 25.4 / 72.0;
            
            System.out.println("PageFormat resultante:");
            System.out.printf("  Tamaño papel: %.1f x %.1f mm%n", anchoMm, altoMm);
            System.out.printf("  Orientación: %s%n", pf.getOrientation() == PageFormat.LANDSCAPE ? "LANDSCAPE" : "PORTRAIT");
            System.out.printf("  Área imprimible: %.1f x %.1f mm%n", 
                            paper.getImageableWidth() * 25.4 / 72.0,
                            paper.getImageableHeight() * 25.4 / 72.0);
            
        } catch (Exception e) {
            System.out.println("WARNING  Error creando PageFormat: " + e.getMessage());
        }
        
        System.out.println("=== FIN DIAGNÓSTICO ===\n");
    }
}
