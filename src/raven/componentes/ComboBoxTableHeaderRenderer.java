package raven.componentes;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellRenderer;

/**
 * Renderizador personalizado para encabezados de tabla que muestra un combobox
 * para filtrar y actualizar valores de columna en la base de datos.
 * 
 * @author Adaptado de RAVEN
 */
public class ComboBoxTableHeaderRenderer extends JComboBox<String> implements TableCellRenderer {

    private static final Logger LOGGER = Logger.getLogger(ComboBoxTableHeaderRenderer.class.getName());
    
    private final JTable table;
    private final int column;
    private final String[] options;
    private final Connection connection;
    private final String tableName;
    private final String idColumnName;
    private final String stateColumnName;

    /**
     * Constructor completo para el ComboBoxTableHeaderRenderer
     * 
     * @param table La tabla JTable que contiene este renderer
     * @param column La columna donde se colocará el combobox
     * @param options Las opciones que mostrará el combobox
     * @param connection La conexión a la base de datos
     * @param tableName El nombre de la tabla en la base de datos
     * @param idColumnName El nombre de la columna ID en la base de datos
     * @param stateColumnName El nombre de la columna de estado en la base de datos
     */
    public ComboBoxTableHeaderRenderer(JTable table, int column, String[] options, 
            Connection connection, String tableName, String idColumnName, String stateColumnName) {
        this.table = table;
        this.column = column;
        this.options = options;
        this.connection = connection;
        this.tableName = tableName;
        this.idColumnName = idColumnName;
        this.stateColumnName = stateColumnName;
        
        initialize();
    }

    /**
     * Inicializa el componente y configura los listeners
     */
    private void initialize() {
        // Configurar estilo visual
        putClientProperty(FlatClientProperties.STYLE, "background:$Table.background");
        
        // Agregar las opciones al ComboBox
        for (String option : options) {
            addItem(option);
        }
        
        // Configurar listeners y comportamiento
        setupListeners();
    }
    
    /**
     * Configura todos los listeners necesarios
     */
    private void setupListeners() {
        // Listener para detectar cambios en la selección del combobox
        addActionListener((ActionEvent e) -> {
            String selectedValue = (String) getSelectedItem();
            if (selectedValue != null) {
                setAllRowsToSelectedValue(selectedValue);
                updateDatabaseWithSelectedValue(selectedValue);
            }
        });
        
        // Listener para el clic en el encabezado de la tabla
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                if (SwingUtilities.isLeftMouseButton(me)) {
                    int col = table.columnAtPoint(me.getPoint());
                    if (col == column) {
                        // Mostrar el dropdown del combobox cuando se hace clic en el encabezado
                        showPopup();
                    }
                }
            }
        });
        
        // Listener para actualizar el estado cuando cambia el modelo de la tabla
        table.getModel().addTableModelListener((TableModelEvent tme) -> {
            if (tme.getType() == TableModelEvent.UPDATE && tme.getColumn() == column) {
                // Una celda fue actualizada en la columna del combobox
                int row = tme.getFirstRow();
                if (row >= 0) {
                    String newValue = table.getValueAt(row, column).toString();
                    // Actualizar el valor en la base de datos
                    updateDatabaseForRow(row, newValue);
                }
            } else if (tme.getType() == TableModelEvent.DELETE) {
                syncComboBoxWithTableState();
            }
        });
    }
    
    /**
     * Actualiza el estado en la base de datos para todas las filas cuando
     * se cambia la selección desde el encabezado
     * 
     * @param selectedValue El nuevo estado seleccionado
     */
    private void updateDatabaseWithSelectedValue(String selectedValue) {
        int updatedCount = 0;
        int failedCount = 0;
        
        try {
            // Actualizar cada fila en la base de datos
            for (int i = 0; i < table.getRowCount(); i++) {
                int id = getRowId(i);
                if (id > 0) {
                    boolean success = updateStateInDatabase(id, selectedValue);
                    if (success) {
                        updatedCount++;
                    } else {
                        failedCount++;
                    }
                }
            }
            
            // Informar al usuario sobre el resultado de la operación
            if (updatedCount > 0) {
                String message = String.format("Se actualizaron %d registros a estado: %s", 
                        updatedCount, selectedValue);
                if (failedCount > 0) {
                    message += String.format(". %d registros no pudieron ser actualizados.", failedCount);
                }
                JOptionPane.showMessageDialog(null, message, 
                        "Actualización Exitosa", JOptionPane.INFORMATION_MESSAGE);
            } else if (failedCount > 0) {
                JOptionPane.showMessageDialog(null, 
                        "No se pudo actualizar ningún registro.", 
                        "Error de Actualización", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al actualizar estados en la base de datos", ex);
            JOptionPane.showMessageDialog(null, 
                    "Error al actualizar la base de datos: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Actualiza el estado en la base de datos para una fila específica
     * 
     * @param row La fila en la tabla
     * @param newValue El nuevo valor del estado
     */
    private void updateDatabaseForRow(int row, String newValue) {
        try {
            int id = getRowId(row);
            if (id > 0) {
                if (updateStateInDatabase(id, newValue)) {
                    LOGGER.log(Level.INFO, "Actualizado registro {0} a estado {1}", new Object[]{id, newValue});
                } else {
                    LOGGER.log(Level.WARNING, "No se pudo actualizar el registro {0}", id);
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Error al actualizar estado en la base de datos", ex);
            JOptionPane.showMessageDialog(null, 
                    "Error al actualizar la base de datos: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Obtiene el ID del registro de una fila específica
     * Asume que el ID está en la primera columna (0)
     * 
     * @param row La fila en la tabla
     * @return El ID del registro o -1 si no es válido
     */
    private int getRowId(int row) {
        // El ID normalmente está en la primera columna (índice 0)
        try {
            Object idObj = table.getValueAt(row, 0);
            if (idObj != null) {
                return Integer.parseInt(idObj.toString());
            }
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Error al convertir ID de la fila {0}: {1}", new Object[]{row, e.getMessage()});
        }
        return -1; // Retorna -1 si no pudo obtener un ID válido
    }
    
    /**
     * Actualiza el estado en la base de datos
     * 
     * @param id El ID del registro a actualizar
     * @param newState El nuevo estado
     * @return true si la actualización fue exitosa, false en caso contrario
     * @throws SQLException Si ocurre un error en la base de datos
     */
    private boolean updateStateInDatabase(int id, String newState) throws SQLException {
        String sql = "UPDATE " + tableName + " SET " + stateColumnName + " = ? WHERE " + idColumnName + " = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newState);
            stmt.setInt(2, id);
            int rowsAffected = stmt.executeUpdate();
            
            return rowsAffected > 0;
        }
    }
    
    /**
     * Sincroniza el estado del combobox con el contenido actual de la tabla
     */
    private void syncComboBoxWithTableState() {
        if (table.getRowCount() == 0) {
            return;
        }
        
        // Verificar si todos los valores en la columna son iguales
        Object initialValue = table.getValueAt(0, column);
        boolean allSame = true;
        
        for (int i = 1; i < table.getRowCount(); i++) {
            Object currentValue = table.getValueAt(i, column);
            if (currentValue == null || initialValue == null || !initialValue.equals(currentValue)) {
                allSame = false;
                break;
            }
        }
        
        if (allSame) {
            // Si todos los valores son iguales, seleccionar ese valor en el combobox
            for (int i = 0; i < options.length; i++) {
                if (options[i].equals(initialValue.toString())) {
                    setSelectedIndex(i);
                    break;
                }
            }
        } else {
            // Si hay valores diferentes, establecer un valor por defecto
            setSelectedIndex(0);
        }
        
        // Asegurar que se actualice visualmente
        table.getTableHeader().repaint();
    }
    
    /**
     * Establece el mismo valor para todas las filas en la columna
     * 
     * @param value El valor a establecer
     */
    private void setAllRowsToSelectedValue(String value) {
        for (int i = 0; i < table.getRowCount(); i++) {
            table.setValueAt(value, i, column);
        }
    }

    @Override
    public Component getTableCellRendererComponent(JTable jtable, Object value, 
            boolean isSelected, boolean hasFocus, int row, int column) {
        return this;
    }

    @Override
    protected void paintComponent(Graphics grphcs) {
        // Dibujar el borde inferior del encabezado de columna
        Graphics2D g2 = (Graphics2D) grphcs.create();
        g2.setColor(UIManager.getColor("TableHeader.bottomSeparatorColor"));
        float size = UIScale.scale(1f);
        g2.fill(new Rectangle2D.Float(0, getHeight() - size, getWidth(), size));
        g2.dispose();
        
        super.paintComponent(grphcs);
    }
}