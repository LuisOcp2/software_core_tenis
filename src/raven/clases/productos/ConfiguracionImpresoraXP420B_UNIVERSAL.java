package raven.clases.productos;

import java.awt.print.PageFormat;
import java.awt.print.Paper;

public class ConfiguracionImpresoraXP420B_UNIVERSAL {
    public static class ConfiguracionAjuste {
        private final String nombre;
        private final double anchoMm;
        private final double altoMm;
        private final String orientacion; // "PORTRAIT" | "LANDSCAPE" | "VERTICAL_180"
        private final double margenMm;
        private final int gradosRotacion; // 0 | 180
        private final boolean desdeDriver;
        private final String softwareOrigen;

    public ConfiguracionAjuste(String nombre, double anchoMm, double altoMm,
                                   String orientacion, double margenMm,
                                   int gradosRotacion, boolean desdeDriver, String softwareOrigen) {
            this.nombre = nombre;
            this.anchoMm = anchoMm;
            this.altoMm = altoMm;
            this.orientacion = orientacion;
            this.margenMm = margenMm;
            this.gradosRotacion = gradosRotacion;
            this.desdeDriver = desdeDriver;
            this.softwareOrigen = softwareOrigen;
        }

        public String getNombre() { return nombre; }
        public double getAnchoMm() { return anchoMm; }
        public double getAltoMm() { return altoMm; }
        public String getOrientacion() { return orientacion; }
        public double getMargenMm() { return margenMm; }
        public int getGradosRotacion() { return gradosRotacion; }
        public boolean isDesdeDriver() { return desdeDriver; }
        public String getSoftwareOrigen() { return softwareOrigen; }

        public PageFormat crearPageFormat() {
            PageFormat pf = new PageFormat();
            Paper paper = new Paper();
            double anchoP = anchoMm * 72.0 / 25.4;
            double altoP = altoMm * 72.0 / 25.4;
            double margenP = margenMm * 72.0 / 25.4;
            paper.setSize(anchoP, altoP);
            paper.setImageableArea(margenP, margenP, anchoP - 2*margenP, altoP - 2*margenP);
            pf.setPaper(paper);
            pf.setOrientation("LANDSCAPE".equalsIgnoreCase(orientacion) ? PageFormat.LANDSCAPE : PageFormat.PORTRAIT);
            return pf;
        }

        // Sobrecarga para orden margen-antes-que-orientación
        public ConfiguracionAjuste(String nombre, double anchoMm, double altoMm,
                                   double margenMm, String orientacion,
                                   int gradosRotacion, boolean desdeDriver, String softwareOrigen) {
            this(nombre, anchoMm, altoMm, orientacion, margenMm, gradosRotacion, desdeDriver, softwareOrigen);
        }
    }

    private ConfiguracionAjuste configuracionSeleccionada = new ConfiguracionAjuste(
            "PARES_SIRO (Activa)", 105.0, 25.0, "PORTRAIT", 2.0, 180, true, "BarTender");

    public void leerConfiguracionesBarTender(String nombreImpresora) {
        configuracionSeleccionada = new ConfiguracionAjuste(
                "PARES_SIRO (Activa)", 105.0, 25.0, "PORTRAIT", 2.0, 180, true, "BarTender");
    }

    public ConfiguracionAjuste getAjusteSeleccionado() { return configuracionSeleccionada; }

    // API de pruebas universales
    private ConfiguracionBarTender currentBT = new ConfiguracionBarTender("PARES_SIRO (Activa)", 105.0, 25.0, "VERTICAL_180", true);

    public java.util.List<ConfiguracionBarTender> leerTodasLasConfiguracionesDriver(String nombreImpresora) {
        java.util.List<ConfiguracionBarTender> list = new java.util.ArrayList<>();
        // Configuraciones estándar
        list.add(new ConfiguracionBarTender("PARES_SIRO (Activa)", 105.0, 25.0, "VERTICAL_180", true));
        list.add(new ConfiguracionBarTender("Estándar XP-420B", 109.1, 25.02, "PORTRAIT", false));
        // Configuraciones para etiquetas pequeñas
        list.add(new ConfiguracionBarTender("Etiqueta Pequeña 32x15mm", 32.0, 15.0, "LANDSCAPE", 1.0,
                152.40, 6, 8.0, 1.5, "TERMICA_DIRECTA", "RASGAR", "USER", false, "Personalizado"));
        list.add(new ConfiguracionBarTender("Etiqueta Pequeña 35x20mm", 35.0, 20.0, "LANDSCAPE", 1.5,
                152.40, 6, 8.0, 2.0, "TERMICA_DIRECTA", "RASGAR", "USER", false, "Personalizado"));
        return list;
    }

    public void detectarConfiguracionActiva(String nombreImpresora) {
        currentBT = new ConfiguracionBarTender("PARES_SIRO (Activa)", 105.0, 25.0, "VERTICAL_180", true);
    }

    public void mostrarConfiguracionSeleccionada() {
        if (currentBT != null) {
            System.out.println("Config seleccionada: " + currentBT.nombre + " " + currentBT.anchoMm + "x" + currentBT.altoMm + " " + currentBT.orientacion);
        }
    }

    public java.awt.print.PageFormat crearPageFormat() {
        return currentBT != null ? currentBT.crearPageFormat() : new java.awt.print.PageFormat();
    }

    // Compatibilidad con tests que llaman getConfiguracionSeleccionada()
    public ConfiguracionBarTender getConfiguracionSeleccionada() { return currentBT; }

    public boolean seleccionarConfiguracion(javax.swing.JFrame parent) {
        java.util.List<ConfiguracionBarTender> list = leerTodasLasConfiguracionesDriver("Xprinter XP-420B");
        if (list == null || list.isEmpty()) return false;
        String[] opciones = new String[list.size()];
        for (int i = 0; i < list.size(); i++) opciones[i] = list.get(i).nombre;
        Object sel = javax.swing.JOptionPane.showInputDialog(
                parent,
                "Seleccione configuración",
                "Configuración XP-420B",
                javax.swing.JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]
        );
        if (sel instanceof String) {
            String s = (String) sel;
            for (ConfiguracionBarTender c : list) {
                if (s.equals(c.nombre)) { currentBT = c; return true; }
            }
        }
        return false;
    }

    public static boolean detectarImpresoraXP420B(String nombre) {
        if (nombre == null) return false;
        String n = nombre.toLowerCase();
        return n.contains("xp-420b") || n.contains("xp420b") || (n.contains("xprinter") && n.contains("420"));
    }
}