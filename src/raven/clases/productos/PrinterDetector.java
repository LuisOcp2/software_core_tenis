package raven.clases.productos;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.HashAttributeSet;
import javax.print.attribute.standard.PrinterName;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase utilitaria para detectar impresoras conectadas al sistema,
 * especialmente impresoras USB.
 */
public class PrinterDetector {

    /**
     * Verifica si hay al menos una impresora disponible en el sistema
     * @return true si hay impresoras disponibles, false en caso contrario
     */
    public static boolean hayImpresorasDisponibles() {
        PrintService[] servicios = PrintServiceLookup.lookupPrintServices(null, null);
        return servicios != null && servicios.length > 0;
    }

    /**
     * Obtiene la lista de todas las impresoras disponibles
     * @return Lista de nombres de impresoras disponibles
     */
    public static List<String> obtenerImpresorasDisponibles() {
        List<String> impresoras = new ArrayList<>();
        PrintService[] servicios = PrintServiceLookup.lookupPrintServices(null, null);

        if (servicios != null) {
            for (PrintService servicio : servicios) {
                impresoras.add(servicio.getName());
            }
        }

        return impresoras;
    }

    /**
     * Obtiene información detallada de las impresoras disponibles
     * @return Lista con información de cada impresora
     */
    public static List<InfoImpresora> obtenerInfoImpresoras() {
        List<InfoImpresora> infoImpresoras = new ArrayList<>();
        PrintService[] servicios = PrintServiceLookup.lookupPrintServices(null, null);

        if (servicios != null) {
            for (PrintService servicio : servicios) {
                InfoImpresora info = new InfoImpresora();
                info.nombre = servicio.getName();
                info.esImpresoraPorDefecto = servicio.equals(PrintServiceLookup.lookupDefaultPrintService());

                // Detectar si es impresora USB
                String nombreLower = info.nombre.toLowerCase();
                info.esUSB = nombreLower.contains("usb") ||
                            nombreLower.contains("xp-") ||
                            nombreLower.contains("xprinter") ||
                            nombreLower.contains("termica") ||
                            nombreLower.contains("thermal");

                infoImpresoras.add(info);
            }
        }

        return infoImpresoras;
    }

    /**
     * Busca una impresora específica por nombre (búsqueda parcial)
     * @param nombreParcial Parte del nombre de la impresora a buscar
     * @return PrintService de la impresora encontrada, o null si no existe
     */
    public static PrintService buscarImpresora(String nombreParcial) {
        if (nombreParcial == null || nombreParcial.trim().isEmpty()) {
            return null;
        }

        PrintService[] servicios = PrintServiceLookup.lookupPrintServices(null, null);

        if (servicios != null) {
            for (PrintService servicio : servicios) {
                if (servicio.getName().toLowerCase().contains(nombreParcial.toLowerCase())) {
                    return servicio;
                }
            }
        }

        return null;
    }

    /**
     * Obtiene la impresora por defecto del sistema
     * @return PrintService de la impresora por defecto, o null si no hay
     */
    public static PrintService obtenerImpresoraPorDefecto() {
        return PrintServiceLookup.lookupDefaultPrintService();
    }

    /**
     * Verifica si una impresora específica está disponible
     * @param nombreImpresora Nombre exacto de la impresora
     * @return true si la impresora está disponible
     */
    public static boolean impresoraDisponible(String nombreImpresora) {
        if (nombreImpresora == null || nombreImpresora.trim().isEmpty()) {
            return false;
        }

        PrintService[] servicios = PrintServiceLookup.lookupPrintServices(null, null);

        if (servicios != null) {
            for (PrintService servicio : servicios) {
                if (servicio.getName().equalsIgnoreCase(nombreImpresora)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Imprime en consola la lista de impresoras disponibles (para debugging)
     */
    public static void mostrarImpresorasDisponibles() {
        System.out.println("=== IMPRESORAS DISPONIBLES ===");

        if (!hayImpresorasDisponibles()) {
            System.out.println("WARNING   NO SE DETECTARON IMPRESORAS CONECTADAS");
            System.out.println("Por favor, verifica que:");
            System.out.println("  1. La impresora esté conectada por USB");
            System.out.println("  2. La impresora esté encendida");
            System.out.println("  3. Los drivers estén instalados correctamente");
            System.out.println("  4. Windows reconozca la impresora en Configuración > Dispositivos > Impresoras");
            return;
        }

        List<InfoImpresora> impresoras = obtenerInfoImpresoras();
        for (int i = 0; i < impresoras.size(); i++) {
            InfoImpresora info = impresoras.get(i);
            System.out.println((i + 1) + ". " + info.nombre);
            System.out.println("   - Por defecto: " + (info.esImpresoraPorDefecto ? "Sí" : "No"));
            System.out.println("   - USB: " + (info.esUSB ? "Probable" : "No detectado"));
        }

        System.out.println("==============================");
    }

    /**
     * Clase interna para almacenar información de una impresora
     */
    public static class InfoImpresora {
        public String nombre;
        public boolean esImpresoraPorDefecto;
        public boolean esUSB;

        @Override
        public String toString() {
            return nombre +
                   (esImpresoraPorDefecto ? " [Por defecto]" : "") +
                   (esUSB ? " [USB]" : "");
        }
    }
}

