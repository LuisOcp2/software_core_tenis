package raven.utils;

import java.util.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Utilidad para calcular diferencias entre fechas
 * Se utiliza en el dashboard para gestionar rangos de fechas y cálculos temporales
 * en las gráficas y reportes estadísticos
 */
public class DateCalculator {
    
    // Las fechas entre las que se calculará la diferencia
    private final Date startDate;  // Fecha de inicio
    private final Date endDate;    // Fecha de fin
    
    /**
     * Constructor que establece las fechas de inicio y fin
     * 
     * @param startDate Fecha de inicio del rango
     * @param endDate Fecha de fin del rango
     */
    public DateCalculator(Date startDate, Date endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    /**
     * Calcula la diferencia en días entre las dos fechas
     * Útil para determinar el espaciado entre etiquetas en gráficos de línea
     * 
     * @return El número de días entre las fechas (valor positivo)
     */
    public long getDifferenceDays() {
        // Convertir las fechas a LocalDate para usar ChronoUnit
        LocalDate start = convertToLocalDate(startDate);
        LocalDate end = convertToLocalDate(endDate);
        // Calcular la diferencia en días
        return ChronoUnit.DAYS.between(start, end);
    }
    
    /**
     * Calcula la diferencia en meses entre las dos fechas
     * Útil para gráficos que muestran datos mensuales
     * 
     * @return El número de meses entre las fechas (valor positivo)
     */
    public long getDifferenceMonths() {
        // Convertir las fechas a LocalDate para usar ChronoUnit
        LocalDate start = convertToLocalDate(startDate);
        LocalDate end = convertToLocalDate(endDate);
        // Calcular la diferencia en meses
        return ChronoUnit.MONTHS.between(start, end);
    }
    
    /**
     * Método auxiliar para convertir un java.util.Date a java.time.LocalDate
     * Necesario para usar las API modernas de fecha y tiempo
     * 
     * @param date La fecha a convertir
     * @return LocalDate equivalente
     */
    private LocalDate convertToLocalDate(Date date) {
        // Convertir Date a LocalDate usando las API de java.time
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
    /**
 * Configura selectores de fecha con valores predeterminados útiles
 * y maneja eventos de cambio para actualizar el dashboard
 */
private void configurarSelectorFechas() {
    // Aquí implementarías la configuración de componentes JDateChooser
    // o similares si deseas añadir filtrado por fechas al dashboard
    
    // Ejemplo de implementación (asumiendo que tienes componentes de fecha):
    /*
    // Configurar fechas predeterminadas
    Date fechaFin = new Date();
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.MONTH, -1); // Un mes atrás
    Date fechaInicio = cal.getTime();
    
    // Establecer fechas en los selectores
    dateChooserInicio.setDate(fechaInicio);
    dateChooserFin.setDate(fechaFin);
    
    // Configurar listener para actualizar datos cuando cambian las fechas
    PropertyChangeListener dateChangeListener = evt -> {
        if ("date".equals(evt.getPropertyName())) {
            Date inicio = dateChooserInicio.getDate();
            Date fin = dateChooserFin.getDate();
            
            if (inicio != null && fin != null) {
                DateCalculator calculator = new DateCalculator(inicio, fin);
                if (calculator.getDifferenceDays() >= 0) {
                    actualizarDashboardPorFechas(inicio, fin);
                }
            }
        }
    };
    
    dateChooserInicio.addPropertyChangeListener(dateChangeListener);
    dateChooserFin.addPropertyChangeListener(dateChangeListener);
    */
}
    
    
    /**
     * Verifica si una fecha específica está dentro del rango establecido
     * Útil para filtrar datos en los gráficos
     * 
     * @param date La fecha a verificar
     * @return true si la fecha está dentro del rango, false en caso contrario
     */
    public boolean isDateInRange(Date date) {
        // Comprobar si la fecha es posterior o igual a la fecha de inicio
        // Y anterior o igual a la fecha de fin
        return !date.before(startDate) && !date.after(endDate);
    }
}