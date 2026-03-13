package raven.controlador.reportes;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Modelo de tabla personalizado para el dashboard
 * Incluye renderizadores y formateadores para mejorar la visualización
 * de los datos en las tablas del dashboard, especialmente para alertas
 */
public class DashboardTableModel extends DefaultTableModel {

    /**
     * Constructor del modelo de tabla
     * 
     * @param columnNames Nombres de las columnas
     * @param rowCount Número inicial de filas
     */
    public DashboardTableModel(Object[] columnNames, int rowCount) {
        super(columnNames, rowCount);
    }
    
    /**
     * Determina si una celda es editable
     * Para el dashboard, ninguna celda debe ser editable
     * 
     * @param row Fila de la celda
     * @param column Columna de la celda
     * @return false siempre, para evitar la edición de celdas
     */
    @Override
    public boolean isCellEditable(int row, int column) {
        return false; // Todas las celdas son de solo lectura
    }
    
    /**
     * Determina el tipo de clase de una columna
     * Útil para aplicar formatos específicos según el tipo de dato
     * 
     * @param columnIndex Índice de la columna
     * @return Clase del tipo de dato en la columna
     */
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        // Si hay filas, usar el tipo de la primera celda de la columna
        if (getRowCount() > 0 && getValueAt(0, columnIndex) != null) {
            return getValueAt(0, columnIndex).getClass();
        }
        // Por defecto, usar Object
        return Object.class;
    }
    /**
 * Corrige el método getTableCellRendererComponent en AlertaInventarioRenderer
 * para manejar valores nulos o vacíos en la tabla
 */
public static class AlertaInventarioRenderer extends DefaultTableCellRenderer {
    
    /**
     * Personaliza la apariencia de cada celda en la tabla de alertas
     * previniendo errores cuando hay valores vacíos
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, 
            boolean isSelected, boolean hasFocus, int row, int column) {
        
        // Obtener el componente base del renderizador
        Component component = super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
        
        // Solo personalizar etiquetas (JLabel)
        if (component instanceof JLabel) {
            JLabel label = (JLabel) component;
            
            // Aplicar colores según el contenido y la columna
            if (column == 4 && "Bajo Stock".equals(value)) { 
                // Columna de Estado - Rojo para "Bajo Stock"
                label.setForeground(new Color(255, 59, 48));
            } else if (column == 2) { 
                // Columna de Stock Actual - Color según nivel
                try {
                    // Verificar que value no sea nulo y sea convertible a entero
                    if (value != null && !value.toString().trim().isEmpty()) {
                        int stockActual = Integer.parseInt(value.toString().trim());
                        
                        // Verificar que el valor del stock mínimo no sea nulo
                        Object stockMinimoObj = table.getValueAt(row, 3);
                        if (stockMinimoObj != null && !stockMinimoObj.toString().trim().isEmpty()) {
                            int stockMinimo = Integer.parseInt(stockMinimoObj.toString().trim());
                            
                            if (stockActual <= stockMinimo) {
                                // Rojo para stock por debajo del mínimo
                                label.setForeground(new Color(255, 59, 48));
                            } else if (stockActual <= stockMinimo * 1.2) {
                                // Naranja para stock apenas por encima del mínimo (20%)
                                label.setForeground(new Color(255, 149, 0));
                            } else {
                                // Color normal para stock adecuado
                                if (isSelected) {
                                    label.setForeground(table.getSelectionForeground());
                                } else {
                                    label.setForeground(table.getForeground());
                                }
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    // En caso de error de conversión, usar color estándar
                    if (isSelected) {
                        label.setForeground(table.getSelectionForeground());
                    } else {
                        label.setForeground(table.getForeground());
                    }
                }
            } else {
                // Para otras columnas, mantener colores estándar
                if (isSelected) {
                    label.setForeground(table.getSelectionForeground());
                } else {
                    label.setForeground(table.getForeground());
                }
            }
        }
        
        return component;
    }
}

    /**
     * Método estático para aplicar todos los renderizadores y estilos
     * a una tabla de alertas de inventario
     * 
     * @param table Tabla a personalizar
     */
    public static void aplicarRenderizadores(JTable table) {
        // Aplicar el renderizador personalizado para las alertas
        table.setDefaultRenderer(Object.class, new AlertaInventarioRenderer());
        
        // Configurar altura de las filas para mejor visualización
        table.setRowHeight(25);
        
        // Evitar que el usuario pueda reorganizar las columnas
        table.getTableHeader().setReorderingAllowed(false);
        
        // Mostrar líneas de cuadrícula para mejorar la legibilidad
        table.setShowGrid(true);
        
        // Establecer un color suave para las líneas de cuadrícula
        table.setGridColor(new Color(230, 230, 230));
    }
}