package raven.clases.productos;

import java.util.Objects;

/**
 * Clase que representa un perfil de configuración de impresión
 * para guardar y reutilizar configuraciones comunes de impresión de etiquetas
 */
public class PerfilImpresion {
    private String nombre;
    private double anchoEtiquetaMm;
    private double altoEtiquetaMm;
    private double margenMm;
    private String orientacion; // "PORTRAIT", "LANDSCAPE", "VERTICAL_180", "HORIZONTAL_180"
    private boolean autoFit;
    private double escala;
    private int rotacionGrados; // 0, 90, -90, 180
    private double offsetXMm;
    private double offsetYMM;
    private String tipoEtiqueta; // "Caja" o "Par"
    private boolean usarConfiguracionXP420B;
    
    // Constructor por defecto
    public PerfilImpresion() {
        this.nombre = "Perfil por defecto";
        this.anchoEtiquetaMm = 40.0;
        this.altoEtiquetaMm = 20.0;
        this.margenMm = 2.0;
        this.orientacion = "LANDSCAPE";
        this.autoFit = true;
        this.escala = 1.0;
        this.rotacionGrados = 0;
        this.offsetXMm = 0.0;
        this.offsetYMM = 0.0;
        this.tipoEtiqueta = "Par";
        this.usarConfiguracionXP420B = false;
    }
    
    // Constructor con parámetros
    public PerfilImpresion(String nombre, double anchoEtiquetaMm, double altoEtiquetaMm, 
                          double margenMm, String orientacion, boolean autoFit, double escala,
                          int rotacionGrados, double offsetXMm, double offsetYMM, 
                          String tipoEtiqueta, boolean usarConfiguracionXP420B) {
        this.nombre = nombre != null ? nombre : "Perfil sin nombre";
        this.anchoEtiquetaMm = anchoEtiquetaMm > 0 ? anchoEtiquetaMm : 40.0;
        this.altoEtiquetaMm = altoEtiquetaMm > 0 ? altoEtiquetaMm : 20.0;
        this.margenMm = margenMm >= 0 ? margenMm : 2.0;
        this.orientacion = orientacion != null ? orientacion : "LANDSCAPE";
        this.autoFit = autoFit;
        this.escala = escala > 0 ? escala : 1.0;
        this.rotacionGrados = rotacionGrados;
        this.offsetXMm = offsetXMm;
        this.offsetYMM = offsetYMM;
        this.tipoEtiqueta = tipoEtiqueta != null ? tipoEtiqueta : "Par";
        this.usarConfiguracionXP420B = usarConfiguracionXP420B;
    }
    
    // Getters y setters
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre != null ? nombre : "Perfil sin nombre";
    }
    
    public double getAnchoEtiquetaMm() {
        return anchoEtiquetaMm;
    }
    
    public void setAnchoEtiquetaMm(double anchoEtiquetaMm) {
        this.anchoEtiquetaMm = anchoEtiquetaMm > 0 ? anchoEtiquetaMm : 40.0;
    }
    
    public double getAltoEtiquetaMm() {
        return altoEtiquetaMm;
    }
    
    public void setAltoEtiquetaMm(double altoEtiquetaMm) {
        this.altoEtiquetaMm = altoEtiquetaMm > 0 ? altoEtiquetaMm : 20.0;
    }
    
    public double getMargenMm() {
        return margenMm;
    }
    
    public void setMargenMm(double margenMm) {
        this.margenMm = margenMm >= 0 ? margenMm : 2.0;
    }
    
    public String getOrientacion() {
        return orientacion;
    }
    
    public void setOrientacion(String orientacion) {
        this.orientacion = orientacion != null ? orientacion : "LANDSCAPE";
    }
    
    public boolean isAutoFit() {
        return autoFit;
    }
    
    public void setAutoFit(boolean autoFit) {
        this.autoFit = autoFit;
    }
    
    public double getEscala() {
        return escala;
    }
    
    public void setEscala(double escala) {
        this.escala = escala > 0 ? escala : 1.0;
    }
    
    public int getRotacionGrados() {
        return rotacionGrados;
    }
    
    public void setRotacionGrados(int rotacionGrados) {
        this.rotacionGrados = rotacionGrados;
    }
    
    public double getOffsetXMm() {
        return offsetXMm;
    }
    
    public void setOffsetXMm(double offsetXMm) {
        this.offsetXMm = offsetXMm;
    }
    
    public double getOffsetYMM() {
        return offsetYMM;
    }
    
    public void setOffsetYMM(double offsetYMM) {
        this.offsetYMM = offsetYMM;
    }
    
    public String getTipoEtiqueta() {
        return tipoEtiqueta;
    }
    
    public void setTipoEtiqueta(String tipoEtiqueta) {
        this.tipoEtiqueta = tipoEtiqueta != null ? tipoEtiqueta : "Par";
    }
    
    public boolean isUsarConfiguracionXP420B() {
        return usarConfiguracionXP420B;
    }
    
    public void setUsarConfiguracionXP420B(boolean usarConfiguracionXP420B) {
        this.usarConfiguracionXP420B = usarConfiguracionXP420B;
    }
    
    /**
     * Aplica este perfil a un objeto ImpresorTermicaPOSDIG2406T
     */
    public void aplicarA(ImpresorTermicaPOSDIG2406T impresor) {
        if (impresor == null) return;
        
        impresor.setCustomPaperSizeMM(anchoEtiquetaMm, altoEtiquetaMm);
        impresor.setAutoFit(autoFit);
        if (!autoFit) {
            impresor.setScaleFactor(escala);
        }
        impresor.setRotationDegrees(rotacionGrados);
        impresor.setContenidoOffsetMM(offsetXMm);
        impresor.setContenidoOffsetYMM(offsetYMM);
        impresor.setMargenes(margenMm, margenMm, margenMm, margenMm);
        impresor.setUsarConfiguracionXP420B(usarConfiguracionXP420B);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PerfilImpresion that = (PerfilImpresion) o;
        return Double.compare(that.anchoEtiquetaMm, anchoEtiquetaMm) == 0 &&
               Double.compare(that.altoEtiquetaMm, altoEtiquetaMm) == 0 &&
               Double.compare(that.margenMm, margenMm) == 0 &&
               autoFit == that.autoFit &&
               Double.compare(that.escala, escala) == 0 &&
               rotacionGrados == that.rotacionGrados &&
               Double.compare(that.offsetXMm, offsetXMm) == 0 &&
               Double.compare(that.offsetYMM, offsetYMM) == 0 &&
               usarConfiguracionXP420B == that.usarConfiguracionXP420B &&
               Objects.equals(nombre, that.nombre) &&
               Objects.equals(orientacion, that.orientacion) &&
               Objects.equals(tipoEtiqueta, that.tipoEtiqueta);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(nombre, anchoEtiquetaMm, altoEtiquetaMm, margenMm, orientacion, 
                           autoFit, escala, rotacionGrados, offsetXMm, offsetYMM, tipoEtiqueta, usarConfiguracionXP420B);
    }
    
    @Override
    public String toString() {
        return String.format("%s (%.1f x %.1f mm)", nombre, anchoEtiquetaMm, altoEtiquetaMm);
    }
    
    /**
     * Crea una copia de este perfil
     */
    public PerfilImpresion copiar() {
        return new PerfilImpresion(nombre, anchoEtiquetaMm, altoEtiquetaMm, margenMm, 
                                  orientacion, autoFit, escala, rotacionGrados, offsetXMm, 
                                  offsetYMM, tipoEtiqueta, usarConfiguracionXP420B);
    }
}