package raven.application.form.principal;

import raven.application.form.productos.*;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import java.awt.Color;
import java.awt.Font;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.application.form.productos.creates.CreateColor;
import raven.clases.productos.ServiceColor;
import raven.componentes.CheckBoxTableHeaderRenderer;
import raven.componentes.TableHeaderAlignment;
import raven.controlador.principal.conexion;
import raven.controlador.productos.ModelColor;
import raven.modal.ModalDialog;
import raven.modal.Toast;
import raven.modal.component.SimpleModalBorder;
import raven.modal.listener.ModalController;
import raven.modal.option.BorderOption;

/**
 *
 * @author CrisDEV
 */
public class FinanzasForm extends javax.swing.JPanel {

    /**
     * Creates new form ColorForm
     */
    
     private final ServiceColor service = new ServiceColor();
// Constructor del formulario de gestión de productos

    private final FontIcon iconAjustar;
    private final FontIcon iconNuevo;
    private final FontIcon iconDesactivar;
    public FinanzasForm() {
       initComponents();

        // Aplica estilo personalizado a la etiqueta (usando propiedades FlatLaf)
        lb.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$h1.font");  // Usa estilo de fuente h1

        // Inicializa configuraciones personalizadas
        init();

        // Instala la fuente Roboto (extensión de FlatLaf)
        FlatRobotoFont.install();

        // Establece fuente predeterminada para todos los componentes
        UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));
        //Diseño de Botones
        Color tabTextColor = UIManager.getColor("TabbedPane.foreground");
        iconNuevo = createColoredIcon(FontAwesomeSolid.PLUS_SQUARE, tabTextColor);
        iconAjustar = createColoredIcon(FontAwesomeSolid.EDIT, tabTextColor);
        iconDesactivar = createColoredIcon(FontAwesomeSolid.TRASH_ALT, tabTextColor);

        btn_nuevo.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:#28CD41");  // Color de fondo

        //Fin de diseño de botones        
    }
    
    private void init() {
        // Estiliza el panel principal con bordes redondeados y color de fondo
        panel.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:20;" // Radio de esquina de 20px
                + "background:$Login.background;"  // Usa color de fondo de tabla
                + "border:0,0,0,0;"); // Sin borde (equivalente al uso previo de 'margin')

        // Estiliza el encabezado de la tabla
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, ""
                + "height:30;" // Altura del encabezado
                + "hoverBackground:null;" // Desactiva efecto hover
                + "pressedBackground:null;" // Desactiva efecto al presionar
                + "separatorColor:$Login.background;" // Color del separador
                + "font:bold;");  // Texto en negrita

        // Estiliza la tabla
        table.putClientProperty(FlatClientProperties.STYLE, ""
                + "rowHeight:70;" // Altura de filas
                + "showHorizontalLines:true;" // Muestra líneas horizontales
                + "intercellSpacing:0,1;" // Espaciado entre celdas
                + "cellFocusColor:$TableHeader.hoverBackground;" // Color de enfoque
                + "selectionBackground:$TableHeader.hoverBackground;" // Fondo de selección
                + "selectionForeground:$Table.foreground;"
                + "background:$Login.background;");  // Texto de selección

        // Estiliza la barra de desplazamiento
        scroll.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE, ""
                + "trackArc:999;" // Barra completamente redondeada
                + "trackInsets:3,3,3,3;" // Relleno de la barra
                + "thumbInsets:3,3,3,3;" // Relleno del control deslizante
                + "background:$Table.background;");  // Color de fondo

        // Estiliza el título
        lbTitle.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:bold +5;");  // Texto en negrita y más grande

        // Configura el campo de búsqueda
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar...");
        txtSearch.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new FlatSVGIcon("raven/icon/svg/search.svg", 0.8f));  // Ícono de búsqueda
        txtSearch.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:15;" // Esquinas redondeadas
                + "borderWidth:0;" // Sin borde
                + "focusWidth:0;" // Sin borde de enfoque
                + "innerFocusWidth:0;" // Sin enfoque interno
                + "margin:5,20,5,20;" // Márgenes
                + "background:$Panel.background");  // Color de fondo

        // Configura renderizadores personalizados para columnas
        table.getColumnModel().getColumn(0).setHeaderRenderer(new CheckBoxTableHeaderRenderer(table, 0));
        table.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(table));

        // Configuración predeterminada para diálogos modales
        ModalDialog.getDefaultOption()
                .setOpacity(0.3f) // Opacidad del fondo
                .getLayoutOption().setAnimateScale(0.1f);  // Escala de animación
        ModalDialog.getDefaultOption()
                .getBorderOption()
                .setShadow(BorderOption.Shadow.MEDIUM);  // Sombra

        // Conecta a la base de datos y carga datos iniciales
        try {
            conexion.getInstance().connectToDatabase();
            loadData();
            conexion.getInstance().close();

        } catch (SQLException e) {
            // Manejo silencioso de errores (debería mejorarse)
        }
    }
    
     private FontIcon createColoredIcon(Ikon icon, Color color) {
        FontIcon fontIcon = FontIcon.of(icon);
        fontIcon.setIconSize(18);
        fontIcon.setIconColor(color);
        return fontIcon;
    }

     private List<ModelColor> getSelectedData() {

        // Crea una nueva lista vacía para almacenar los colores seleccionados
        List<ModelColor> list = new ArrayList<>();

        // Recorre todas las filas de la tabla (desde la fila 0 hasta la última)
        for (int i = 0; i < table.getRowCount(); i++) {

            // Verifica si el valor de la primera columna (columna 0) es true (marcado/seleccionado)
            if ((boolean) table.getValueAt(i, 0)) {

                // Obtiene el objeto ModelColor de la tercera columna (columna 2) de la fila actual
                ModelColor data = (ModelColor) table.getValueAt(i, 2);
                list.add(data);
            }
        }

        // Devuelve la lista con todos los colores que estaban seleccionados en la tabla
        return list;
    }
     
     
// Carga datos desde el servicio a la tabla
    private void loadData() throws SQLException {
        try {
            DefaultTableModel model = (DefaultTableModel) table.getModel(); // Manejo silencioso de errores (debería mejorarse)
            // Detiene cualquier edición de celda activa
            if (table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
            // Limpia las filas existentes
            model.setRowCount(0);
            // Obtiene todos los colores del servicio
            List<ModelColor> list = service.getAll();

            // Añade cada color como fila en la tabla
            for (ModelColor d : list) {
                model.addRow(d.toTableRow(table.getRowCount() + 1));

            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error al cargar datos: " + e.getMessage());

        }

    }

// Busca datos según el texto ingresado
    private void searchData(String search) throws SQLException {
        DefaultTableModel model = (DefaultTableModel) table.getModel(); // Manejo silencioso de errores (debería mejorarse)
        // Detiene cualquier edición de celda activa
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        // Limpia las filas existentes
        model.setRowCount(0);
        // Obtiene resultados de búsqueda del servicio
        List<ModelColor> list = service.search(search);
        // Añade colores coincidentes como filas en la tabla
        for (ModelColor d : list) {
            model.addRow(d.toTableRow(table.getRowCount() + 1));
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lb = new javax.swing.JLabel();
        panel = new javax.swing.JPanel();
        scroll = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        jSeparator1 = new javax.swing.JSeparator();
        txtSearch = new javax.swing.JTextField();
        lbTitle = new javax.swing.JLabel();
        btn_nuevo = new raven.componentes.ButtonAction();

        lb.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lb.setText("GESTION DE COMPRAS");

        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "SELECT", "N°", "NOMBRE", "ID", "DESCRIPCION"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        table.getTableHeader().setReorderingAllowed(false);
        scroll.setViewportView(table);

        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtSearchKeyReleased(evt);
            }
        });

        lbTitle.setText("Compras");

        btn_nuevo.setText("NUEVA ENTRADA");
        btn_nuevo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_nuevoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelLayout = new javax.swing.GroupLayout(panel);
        panel.setLayout(panelLayout);
        panelLayout.setHorizontalGroup(
            panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator1)
            .addGroup(panelLayout.createSequentialGroup()
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelLayout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelLayout.createSequentialGroup()
                                .addComponent(lbTitle)
                                .addGap(0, 721, Short.MAX_VALUE))
                            .addGroup(panelLayout.createSequentialGroup()
                                .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 239, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btn_nuevo, javax.swing.GroupLayout.PREFERRED_SIZE, 163, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(scroll))
                .addContainerGap())
        );
        panelLayout.setVerticalGroup(
            panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(lbTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btn_nuevo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 624, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lb, javax.swing.GroupLayout.DEFAULT_SIZE, 794, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(lb)
                .addGap(0, 732, Short.MAX_VALUE))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(31, 31, 31)
                    .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap()))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void txtSearchKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSearchKeyReleased
        try {
            searchData(txtSearch.getText().trim());
        } catch (SQLException ex) {
            Logger.getLogger(FinanzasForm.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_txtSearchKeyReleased

    private void btn_nuevoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_nuevoActionPerformed
        // Crea una nueva instancia de la clase CreateColor
        CreateColor cr = new CreateColor();

        // Carga los datos en la instancia 'cr' utilizando el servicio proporcionado y un valor nulo
        cr.loadData(service, null);

        // Crea un arreglo de opciones para el modal, que incluye un botón de "Cancelar" y otro de "Guardar"
        SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[]{
            new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION), // Opción para cancelar
            new SimpleModalBorder.Option("Guardar", SimpleModalBorder.OK_OPTION) // Opción para guardar
        };

        // Muestra un diálogo modal con el contenido especificado
        ModalDialog.showModal(this, // El contexto actual (probablemente una ventana o formulario)
            new SimpleModalBorder(cr, "Nuevo color", options, (ModalController mc, int i) -> { // Crea un nuevo SimpleModalBorder
                // Maneja la acción cuando se cierra el modal
                if (i == SimpleModalBorder.OK_OPTION) { // Si se selecciona la opción "Guardar"
                    // Validar los datos antes de crear
                    if (cr.validateData(service)) {
                        try {
                            // Intenta crear un nuevo color utilizando los datos de 'cr'
                            service.create(cr.getData());
                            // Muestra un mensaje de éxito si el color se crea correctamente
                            Toast.show(FinanzasForm.this, Toast.Type.SUCCESS, "Color creado exitosamente");
                            // Recarga los datos en el formulario
                            loadData();
                            // Cierra el modal
                            mc.close();
                        } catch (SQLException e) { // Captura cualquier excepción de SQL que ocurra
                            // Muestra un mensaje de error si hay un problema al crear el color
                            Toast.show(FinanzasForm.this, Toast.Type.ERROR, "Error al crear color: " + e.getMessage());
                        }
                    }
                    // Si la validación falla, no cerrar el modal para que el usuario pueda corregir
                } else if (i == SimpleModalBorder.OPENED) { // Si el modal se abre
                    // Inicializa el formulario 'cr' para que esté listo para la entrada de datos
                    cr.init();
                }
            })
        );
    }//GEN-LAST:event_btn_nuevoActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private raven.componentes.ButtonAction btn_nuevo;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel lb;
    private javax.swing.JLabel lbTitle;
    private javax.swing.JPanel panel;
    private javax.swing.JScrollPane scroll;
    private javax.swing.JTable table;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables
}
