package raven.clases.productos;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Winspool;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.Memory;
import java.awt.print.*;
import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

/**
 * * @author CrisDEV
 * Clase para leer configuraciones nativas del driver de Windows
 * Compatible con JNA 5.5.0
 */
public class ConfiguracionImpresoraWindows {
    
    /**
     * Lista todas las impresoras disponibles en el sistema
     */
    public static void listarTodasLasImpresoras() {
        System.out.println("\n========================================");
        System.out.println("    IMPRESORAS DISPONIBLES EN EL SISTEMA");
        System.out.println("========================================");
        
        PrintService[] servicios = PrintServiceLookup.lookupPrintServices(null, null);
        
        if (servicios.length == 0) {
            System.out.println("WARNING  No se encontraron impresoras instaladas");
            return;
        }
        
        for (int i = 0; i < servicios.length; i++) {
            PrintService ps = servicios[i];
            System.out.println("\n" + (i+1) + ". " + ps.getName());
            System.out.println("   ---------------------------------");
            
            // Verificar si está disponible
            try {
                AttributeSet attrs = ps.getAttributes();
                System.out.println("   Estado: Disponible");
                
                // Mostrar formatos soportados
                DocFlavor[] flavors = ps.getSupportedDocFlavors();
                System.out.println("   Formatos soportados: " + flavors.length);
                
                // Verificar si soporta impresión de imágenes
                boolean soportaImagenes = false;
                for (DocFlavor flavor : flavors) {
                    if (flavor.toString().contains("image") || 
                        flavor.toString().contains("PRINTABLE")) {
                        soportaImagenes = true;
                        break;
                    }
                }
                System.out.println("   Soporta imágenes/gráficos: " + (soportaImagenes ? "Sí" : "No"));
                
            } catch (Exception e) {
                System.out.println("   Estado: Error al consultar - " + e.getMessage());
            }
        }
        
        // Impresora por defecto
        System.out.println("\n========================================");
        PrintService defaultPS = PrintServiceLookup.lookupDefaultPrintService();
        if (defaultPS != null) {
            System.out.println("IMPRESORA POR DEFECTO: " + defaultPS.getName());
        } else {
            System.out.println("No hay impresora por defecto configurada");
        }
        System.out.println("========================================\n");
    }
    
    /**
     * Busca una impresora por nombre (búsqueda flexible)
     */
    public static PrintService buscarImpresora(String nombreBusqueda) {
        System.out.println("\nBuscar Buscando impresora: " + nombreBusqueda);
        
        PrintService[] servicios = PrintServiceLookup.lookupPrintServices(null, null);
        
        // Búsqueda exacta primero
        for (PrintService servicio : servicios) {
            if (servicio.getName().equalsIgnoreCase(nombreBusqueda)) {
                System.out.println("OK Encontrada (coincidencia exacta): " + servicio.getName());
                return servicio;
            }
        }
        
        // Búsqueda parcial (contiene)
        for (PrintService servicio : servicios) {
            if (servicio.getName().toLowerCase().contains(nombreBusqueda.toLowerCase())) {
                System.out.println("OK Encontrada (coincidencia parcial): " + servicio.getName());
                return servicio;
            }
        }
        
        System.out.println("WARNING  No se encontró la impresora: " + nombreBusqueda);
        System.out.println(" Usa listarTodasLasImpresoras() para ver las disponibles");
        return null;
    }
    
    /**
     * Lista los tamaños de papel soportados por una impresora
     */
    public static void listarTamanosPapel(PrintService servicio) {
        if (servicio == null) {
            System.out.println("WARNING  Servicio de impresión es null");
            return;
        }
        
        System.out.println("\nDocumento TAMAÑOS DE PAPEL SOPORTADOS");
        System.out.println("Impresora: " + servicio.getName());
        System.out.println("---------------------------------");
        
        try {
            Media[] medias = (Media[]) servicio.getSupportedAttributeValues(
                Media.class, 
                DocFlavor.SERVICE_FORMATTED.PRINTABLE, 
                null
            );
            
            if (medias == null || medias.length == 0) {
                System.out.println("WARNING  No se pudieron obtener tamaños de papel");
                System.out.println(" El driver puede no exponer esta información via Java Print API");
                return;
            }
            
            for (Media media : medias) {
                System.out.println("\n• " + media.toString());
                
                if (media instanceof MediaSizeName) {
                    MediaSizeName msn = (MediaSizeName) media;
                    MediaSize ms = MediaSize.getMediaSizeForName(msn);
                    if (ms != null) {
                        float ancho = ms.getX(MediaSize.MM);
                        float alto = ms.getY(MediaSize.MM);
                        System.out.printf("  Dimensiones: %.1f mm x %.1f mm%n", ancho, alto);
                    }
                }
            }
            
        } catch (Exception e) {
            System.out.println("WARNING  Error listando tamaños: " + e.getMessage());
        }
        
        System.out.println("---------------------------------\n");
    }
    
    /**
     * Obtiene la orientación predeterminada del driver
     */
    public static OrientationRequested obtenerOrientacionPredeterminada(PrintService servicio) {
        if (servicio == null) return OrientationRequested.PORTRAIT;
        
        try {
            Object orientacion = servicio.getDefaultAttributeValue(OrientationRequested.class);
            if (orientacion instanceof OrientationRequested) {
                System.out.println("OK Orientación del driver: " + orientacion);
                return (OrientationRequested) orientacion;
            }
        } catch (Exception e) {
            System.out.println("WARNING  No se pudo obtener orientación: " + e.getMessage());
        }
        
        return OrientationRequested.PORTRAIT;
    }
    
    /**
     * Crea un PrintRequestAttributeSet con configuraciones del driver
     */
    public static PrintRequestAttributeSet obtenerAtributosDelDriver(PrintService servicio) {
        PrintRequestAttributeSet atributos = new HashPrintRequestAttributeSet();
        
        if (servicio == null) {
            System.out.println("WARNING  No se pueden obtener atributos: servicio es null");
            return atributos;
        }
        
        System.out.println("\n OBTENIENDO CONFIGURACIÓN DEL DRIVER");
        System.out.println("Impresora: " + servicio.getName());
        System.out.println("---------------------------------");
        
        try {
            // 1. Orientación
            Object orientacion = servicio.getDefaultAttributeValue(OrientationRequested.class);
            if (orientacion instanceof OrientationRequested) {
                atributos.add((OrientationRequested) orientacion);
                System.out.println("OK Orientación: " + orientacion);
            } else {
                System.out.println("WARNING  Orientación no disponible, usando LANDSCAPE por defecto");
                atributos.add(OrientationRequested.LANDSCAPE);
            }
            
            // 2. Tamaño de papel (Media)
            Object media = servicio.getDefaultAttributeValue(Media.class);
            if (media instanceof Media) {
                atributos.add((Media) media);
                System.out.println("OK Papel: " + media);
            } else {
                System.out.println("WARNING  Tamaño de papel no disponible");
            }
            
            // 3. Calidad de impresión
            Object calidad = servicio.getDefaultAttributeValue(PrintQuality.class);
            if (calidad instanceof PrintQuality) {
                atributos.add((PrintQuality) calidad);
                System.out.println("OK Calidad: " + calidad);
            }
            
            // 4. Número de copias
            Object copias = servicio.getDefaultAttributeValue(Copies.class);
            if (copias instanceof Copies) {
                atributos.add((Copies) copias);
                System.out.println("OK Copias: " + copias);
            }
            
        } catch (Exception e) {
            System.out.println("WARNING  Error obteniendo atributos: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("---------------------------------\n");
        return atributos;
    }
    
    /**
     * Configura un PageFormat personalizado para etiquetas
     */
    public static PageFormat crearPageFormatEtiqueta(PrinterJob job, double anchoMM, double altoMM) {
        PageFormat pf = job.defaultPage();
        Paper paper = new Paper();
        
        // Convertir mm a puntos (1 punto = 1/72 pulgada)
        double MM_TO_POINTS = 2.83465;
        double anchoPuntos = anchoMM * MM_TO_POINTS;
        double altoPuntos = altoMM * MM_TO_POINTS;
        
        System.out.println("\n CONFIGURANDO TAMAÑO PERSONALIZADO");
        System.out.printf("Tamaño: %.1f mm x %.1f mm%n", anchoMM, altoMM);
        System.out.printf("En puntos: %.1f x %.1f%n", anchoPuntos, altoPuntos);
        
        paper.setSize(anchoPuntos, altoPuntos);
        paper.setImageableArea(0, 0, anchoPuntos, altoPuntos);
        pf.setPaper(paper);
        pf.setOrientation(PageFormat.LANDSCAPE);
        
        System.out.println("OK PageFormat configurado");
        System.out.println("---------------------------------\n");
        
        return pf;
    }
    
    /**
     * Verifica si una impresora está lista para imprimir
     */
    public static boolean verificarEstadoImpresora(PrintService servicio) {
        if (servicio == null) return false;
        
        try {
            AttributeSet attrs = servicio.getAttributes();
            
            // Verificar si hay errores
            PrinterState estado = (PrinterState) attrs.get(PrinterState.class);
            if (estado != null) {
                System.out.println("Estado de impresora: " + estado);
                return estado != PrinterState.STOPPED;
            }
            
            return true;
            
        } catch (Exception e) {
            System.out.println("WARNING  No se pudo verificar estado: " + e.getMessage());
            return true; // Asumir que está disponible
        }
    }
}
