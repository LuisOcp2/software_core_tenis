package raven.application.form.productos;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import java.awt.Component;
import java.util.function.Consumer;
import java.awt.Container;
import java.awt.Color;
import java.awt.Font;
import java.awt.Window;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.application.form.productos.creates.CreateColor;
import raven.clases.productos.ServiceColor;
import raven.componentes.CheckBoxTableHeaderRenderer;
import raven.componentes.LoadingOverlayHelper;
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
public class ColorForm extends javax.swing.JPanel {

    /**
     * Creates new form ColorForm
     */

    private final ServiceColor service = new ServiceColor();
    // Constructor del formulario de gestión de productos

    private final FontIcon iconAjustar;
    private final FontIcon iconNuevo;
    private final FontIcon iconDesactivar;

    public ColorForm() {
        initComponents();

        // Aplica estilo personalizado a la etiqueta (usando propiedades FlatLaf)
        lb.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$h1.font"); // Usa estilo de fuente h1

        // Inicializa configuraciones personalizadas
        init();

        // Instala la fuente Roboto (extensión de FlatLaf)
        FlatRobotoFont.install();

        // Establece fuente predeterminada para todos los componentes
        UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));
        // Diseño de Botones
        Color tabTextColor = UIManager.getColor("TabbedPane.foreground");
        iconNuevo = createColoredIcon(FontAwesomeSolid.PLUS_SQUARE, tabTextColor);
        iconAjustar = createColoredIcon(FontAwesomeSolid.EDIT, tabTextColor);
        iconDesactivar = createColoredIcon(FontAwesomeSolid.TRASH_ALT, tabTextColor);

        btn_nuevo.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:#28CD41"); // Color de fondo
        btn_editar.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:#FFCC00"); // Color de fondo
        btn_eliminar.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:#FF453A"); // Color de fondo

        btn_nuevo.setIcon(iconNuevo);
        btn_editar.setIcon(iconAjustar);
        btn_eliminar.setIcon(iconDesactivar);

        // Fin de diseño de botones
    }

    private static FlatSVGIcon dashboardIcon(String name, int size, Color color) {
        FlatSVGIcon icon = new FlatSVGIcon("raven/icon/svg/dashboard/" + name, size, size);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> color));
        return icon;
    }

    private static void styleActionButton(JButton button, Color bg, Color hover, Color pressed, FlatSVGIcon icon) {
        if (button == null) {
            return;
        }
        button.setIcon(icon);
        button.setIconTextGap(8);
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc:15;"
                        + "background:#" + toHex(bg) + ";"
                        + "foreground:#ffffff;"
                        + "hoverBackground:#" + toHex(hover) + ";"
                        + "pressedBackground:#" + toHex(pressed) + ";"
                        + "borderWidth:0;"
                        + "focusWidth:0;"
                        + "font:bold 13;"
                        + "margin:8,16,8,16");
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
    }

    private static String toHex(Color c) {
        return String.format("%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static JButton findButtonByText(Component root, String text) {
        if (root == null || text == null) {
            return null;
        }
        String target = text.trim();
        Deque<Component> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Component c = stack.pop();
            if (c instanceof JButton) {
                String t = ((JButton) c).getText();
                if (t != null && t.trim().equalsIgnoreCase(target)) {
                    return (JButton) c;
                }
            }
            if (c instanceof Container) {
                Component[] children = ((Container) c).getComponents();
                for (int i = children.length - 1; i >= 0; i--) {
                    stack.push(children[i]);
                }
            }
        }
        return null;
    }

    private static void setEnabledSafe(JButton button, boolean enabled) {
        if (button != null) {
            button.setEnabled(enabled);
        }
    }

    private void runModalActionWithButtonLoading(JButton button, String message, Runnable task, Runnable onSuccess,
            Consumer<Exception> onError) {
        if (button instanceof JComponent) {
            LoadingOverlayHelper.showLoading((JComponent) button, message);
        }
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    task.run();
                } catch (Exception ex) {
                    error = ex;
                }
                return null;
            }

            @Override
            protected void done() {
                if (button instanceof JComponent) {
                    LoadingOverlayHelper.hideLoading((JComponent) button);
                }
                if (error == null) {
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                } else {
                    if (onError != null) {
                        onError.accept(error);
                    }
                }
            }
        };
        worker.execute();
    }

    private void init() {
        // Estiliza el panel principal con bordes redondeados y color de fondo
        panel.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:20;" // Radio de esquina de 20px
                + "background:$Login.background;" // Usa color de fondo de tabla
                + "border:0,0,0,0;"); // Sin borde (equivalente al uso previo de 'margin')

        // Estiliza el encabezado de la tabla
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, ""
                + "height:30;" // Altura del encabezado
                + "hoverBackground:null;" // Desactiva efecto hover
                + "pressedBackground:null;" // Desactiva efecto al presionar
                + "separatorColor:$Login.background;" // Color del separador
                + "font:bold;"); // Texto en negrita

        // Estiliza la tabla
        table.putClientProperty(FlatClientProperties.STYLE, ""
                + "rowHeight:70;" // Altura de filas
                + "showHorizontalLines:true;" // Muestra líneas horizontales
                + "intercellSpacing:0,1;" // Espaciado entre celdas
                + "cellFocusColor:$TableHeader.hoverBackground;" // Color de enfoque
                + "selectionBackground:$TableHeader.hoverBackground;" // Fondo de selección
                + "selectionForeground:$Table.foreground;"
                + "background:$Login.background;"); // Texto de selección

        // Estiliza la barra de desplazamiento
        scroll.getVerticalScrollBar().putClientProperty(FlatClientProperties.STYLE, ""
                + "trackArc:999;" // Barra completamente redondeada
                + "trackInsets:3,3,3,3;" // Relleno de la barra
                + "thumbInsets:3,3,3,3;" // Relleno del control deslizante
                + "background:$Table.background;"); // Color de fondo

        // Estiliza el título
        lbTitle.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:bold +5;"); // Texto en negrita y más grande

        // Configura el campo de búsqueda
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar...");
        txtSearch.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON,
                new FlatSVGIcon("raven/icon/svg/search.svg", 0.8f)); // Ícono de búsqueda
        txtSearch.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:15;" // Esquinas redondeadas
                + "borderWidth:0;" // Sin borde
                + "focusWidth:0;" // Sin borde de enfoque
                + "innerFocusWidth:0;" // Sin enfoque interno
                + "margin:5,20,5,20;" // Márgenes
                + "background:$Panel.background"); // Color de fondo

        // Configura renderizadores personalizados para columnas
        table.getColumnModel().getColumn(0).setHeaderRenderer(new CheckBoxTableHeaderRenderer(table, 0));
        table.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(table));

        // Configuración predeterminada para diálogos modales
        ModalDialog.getDefaultOption()
                .setOpacity(0.3f) // Opacidad del fondo
                .getLayoutOption().setAnimateScale(0.1f); // Escala de animación
        ModalDialog.getDefaultOption()
                .getBorderOption()
                .setShadow(BorderOption.Shadow.MEDIUM); // Sombra

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

            // Verifica si el valor de la primera columna (columna 0) es true
            // (marcado/seleccionado)
            if ((boolean) table.getValueAt(i, 0)) {

                // Obtiene el objeto ModelColor de la tercera columna (columna 2) de la fila
                // actual
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
            DefaultTableModel model = (DefaultTableModel) table.getModel(); // Manejo silencioso de errores (debería
                                                                            // mejorarse)
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
        DefaultTableModel model = (DefaultTableModel) table.getModel(); // Manejo silencioso de errores (debería
                                                                        // mejorarse)
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
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lb = new javax.swing.JLabel();
        panel = new javax.swing.JPanel();
        scroll = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        jSeparator1 = new javax.swing.JSeparator();
        txtSearch = new javax.swing.JTextField();
        lbTitle = new javax.swing.JLabel();
        btn_nuevo = new raven.componentes.ButtonAction();
        btn_editar = new raven.componentes.ButtonAction();
        btn_eliminar = new raven.componentes.ButtonAction();

        lb.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lb.setText("GESTION DE COLORES");

        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        table.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {

                },
                new String[] {
                        "SELECT", "N°", "NOMBRE", "ID", "DESCRIPCION"
                }) {
            Class[] types = new Class[] {
                    java.lang.Boolean.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class,
                    java.lang.Object.class
            };
            boolean[] canEdit = new boolean[] {
                    true, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        table.getTableHeader().setReorderingAllowed(false);
        scroll.setViewportView(table);

        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtSearchKeyReleased(evt);
            }
        });

        lbTitle.setText("Colores");

        btn_nuevo.setText("CREAR");
        btn_nuevo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_nuevoActionPerformed(evt);
            }
        });

        btn_editar.setText("EDITAR");
        btn_editar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_editarActionPerformed(evt);
            }
        });

        btn_eliminar.setText("ELIMINAR");
        btn_eliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_eliminarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelLayout = new javax.swing.GroupLayout(panel);
        panel.setLayout(panelLayout);
        panelLayout.setHorizontalGroup(
                panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 988,
                                                Short.MAX_VALUE)
                                        .addComponent(jSeparator1)
                                        .addGroup(panelLayout.createSequentialGroup()
                                                .addGroup(panelLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(lbTitle)
                                                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                300, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(btn_nuevo, javax.swing.GroupLayout.PREFERRED_SIZE, 101,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(btn_editar, javax.swing.GroupLayout.PREFERRED_SIZE, 101,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(btn_eliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 101,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap()));
        panelLayout.setVerticalGroup(
                panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lbTitle)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btn_nuevo, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btn_editar, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btn_eliminar, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 624, Short.MAX_VALUE)
                                .addContainerGap()));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(lb, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE)
                        .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lb, javax.swing.GroupLayout.PREFERRED_SIZE, 30,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(panel, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
    }// </editor-fold>//GEN-END:initComponents

    private void txtSearchKeyReleased(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_txtSearchKeyReleased
        try {
            searchData(txtSearch.getText().trim());
        } catch (SQLException ex) {
            Logger.getLogger(ColorForm.class.getName()).log(Level.SEVERE, null, ex);
        }
    }// GEN-LAST:event_txtSearchKeyReleased

    private void btn_nuevoActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btn_nuevoActionPerformed
        CreateColor cr = new CreateColor();
        cr.loadData(service, null);
        SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                new SimpleModalBorder.Option("Guardar", SimpleModalBorder.OK_OPTION)
        };

        JButton[] btnGuardarRef = new JButton[1];
        JButton[] btnCancelarRef = new JButton[1];

        SimpleModalBorder border = new SimpleModalBorder(cr, "Nuevo color", options, (ModalController mc, int i) -> {
            if (i == SimpleModalBorder.OK_OPTION) {
                if (!cr.validateData(service)) {
                    return;
                }
                JButton btnGuardar = btnGuardarRef[0];
                JButton btnCancelar = btnCancelarRef[0];
                setEnabledSafe(btnGuardar, false);
                setEnabledSafe(btnCancelar, false);
                runModalActionWithButtonLoading(
                        btnGuardar,
                        "Creando...",
                        () -> {
                            try {
                                service.create(cr.getData());
                            } catch (SQLException ex) {
                                throw new RuntimeException(ex);
                            }
                        },
                        () -> {
                            Toast.show(ColorForm.this, Toast.Type.SUCCESS, "Color creado exitosamente");
                            try {
                                loadData();
                            } catch (SQLException ex) {
                                Toast.show(ColorForm.this, Toast.Type.ERROR,
                                        "Error al cargar datos: " + ex.getMessage());
                            }
                            mc.close();
                        },
                        (ex) -> {
                            setEnabledSafe(btnGuardar, true);
                            setEnabledSafe(btnCancelar, true);
                            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                            Toast.show(ColorForm.this, Toast.Type.ERROR, "Error: " + cause.getMessage());
                        });
            } else if (i == SimpleModalBorder.OPENED) {
                cr.init();
                Window w = SwingUtilities.getWindowAncestor(cr);
                btnGuardarRef[0] = findButtonByText(w, "Guardar");
                btnCancelarRef[0] = findButtonByText(w, "Cancelar");
                styleActionButton(
                        btnGuardarRef[0],
                        new Color(40, 205, 65),
                        new Color(52, 199, 89),
                        new Color(32, 185, 58),
                        dashboardIcon("check-circle.svg", 16, Color.WHITE));
                styleActionButton(
                        btnCancelarRef[0],
                        new Color(255, 69, 58),
                        new Color(255, 92, 84),
                        new Color(233, 58, 50),
                        dashboardIcon("x-circle.svg", 16, Color.WHITE));
            }
        });

        ModalDialog.showModal(this, border);
    }// GEN-LAST:event_btn_nuevoActionPerformed

    private void btn_editarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btn_editarActionPerformed
        // Obtiene una lista de colores seleccionados llamando al método
        // getSelectedData()
        List<ModelColor> list = getSelectedData();

        // Verifica si la lista no está vacía (si hay colores seleccionados)
        if (!list.isEmpty()) {
            // Comprueba si solo hay un color seleccionado (tamaño de lista = 1)
            if (list.size() == 1) {
                // Obtiene el primer (y único) color de la lista
                ModelColor data = list.get(0);
                // Crea una nueva instancia del formulario de creación/edición
                CreateColor createC = new CreateColor();
                // Carga los datos del color seleccionado en el formulario de edición
                createC.loadData(service, data);

                // Define las opciones para el diálogo modal (Cancelar y Actualizar)
                SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                        new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                        new SimpleModalBorder.Option("Actualizar", SimpleModalBorder.OK_OPTION)
                };

                JButton[] btnActualizarRef = new JButton[1];
                JButton[] btnCancelarRef = new JButton[1];

                // Muestra el diálogo modal para editar el color
                ModalDialog.showModal(this, // Componente padre (probablemente un JFrame o JPanel)
                        new SimpleModalBorder(
                                createC, // Panel de contenido (el formulario de edición)
                                "Editar Color: " + data.getNombre(), // Título del diálogo
                                options, // Opciones de botones
                                // Manejador de eventos para los botones
                                (mc, i) -> {
                                    // Si se hace clic en el botón OK (Actualizar)
                                    if (i == SimpleModalBorder.OK_OPTION) {
                                        // Validar los datos antes de actualizar
                                        if (!createC.validateDataForEdit(service, data)) {
                                            return;
                                        }
                                        JButton btnActualizar = btnActualizarRef[0];
                                        JButton btnCancelar = btnCancelarRef[0];
                                        setEnabledSafe(btnActualizar, false);
                                        setEnabledSafe(btnCancelar, false);
                                        runModalActionWithButtonLoading(
                                                btnActualizar,
                                                "Actualizando...",
                                                () -> {
                                                    try {
                                                        ModelColor dataEdit = createC.getData();
                                                        dataEdit.setColorId(data.getColorId());
                                                        service.update(dataEdit);
                                                    } catch (SQLException ex) {
                                                        throw new RuntimeException(ex);
                                                    }
                                                },
                                                () -> {
                                                    Toast.show(this, Toast.Type.SUCCESS, "Color actualizado");
                                                    try {
                                                        loadData();
                                                    } catch (SQLException ex) {
                                                        Toast.show(this, Toast.Type.ERROR,
                                                                "Error al cargar datos: " + ex.getMessage());
                                                    }
                                                    mc.close();
                                                },
                                                (ex) -> {
                                                    setEnabledSafe(btnActualizar, true);
                                                    setEnabledSafe(btnCancelar, true);
                                                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                                                    Toast.show(this, Toast.Type.ERROR, "Error: " + cause.getMessage());
                                                });
                                    } // Cuando el diálogo se abre (evento OPENED)
                                    else if (i == SimpleModalBorder.OPENED) {
                                        // Inicializa el formulario
                                        createC.init();
                                        Window w = SwingUtilities.getWindowAncestor(createC);
                                        btnActualizarRef[0] = findButtonByText(w, "Actualizar");
                                        btnCancelarRef[0] = findButtonByText(w, "Cancelar");
                                        styleActionButton(
                                                btnActualizarRef[0],
                                                new Color(40, 205, 65),
                                                new Color(52, 199, 89),
                                                new Color(32, 185, 58),
                                                dashboardIcon("refresh.svg", 16, Color.WHITE));
                                        styleActionButton(
                                                btnCancelarRef[0],
                                                new Color(255, 69, 58),
                                                new Color(255, 92, 84),
                                                new Color(233, 58, 50),
                                                dashboardIcon("x-circle.svg", 16, Color.WHITE));
                                    }
                                }));
            } // Si hay más de un color seleccionado
            else {
                // Muestra advertencia indicando que solo se puede editar un color a la vez
                Toast.show(this, Toast.Type.WARNING, "Seleccione solo un color");
            }
        } // Si no hay colores seleccionados
        else {
            // Muestra advertencia indicando que se debe seleccionar un color para editar
            Toast.show(this, Toast.Type.WARNING, "Seleccione un color para editar");
        }
    }// GEN-LAST:event_btn_editarActionPerformed

    private void btn_eliminarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btn_eliminarActionPerformed
        // Obtiene la lista de colores seleccionados (probablemente de una tabla o
        // vista)
        List<ModelColor> list = getSelectedData();
        if (!list.isEmpty()) {
            SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                    new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                    new SimpleModalBorder.Option("Eliminar", SimpleModalBorder.OK_OPTION)
            };

            JButton[] btnEliminarRef = new JButton[1];
            JButton[] btnCancelarRef = new JButton[1];

            JLabel label = new JLabel("¿Está seguro de eliminar " + list.size() + " color(es)?");
            label.setBorder(new EmptyBorder(5, 25, 5, 25));

            ModalDialog.showModal(
                    this,
                    new SimpleModalBorder(
                            label,
                            "Confirmar Eliminación",
                            options,
                            (mc, i) -> {
                                if (i == SimpleModalBorder.OK_OPTION) {
                                    JButton btnEliminar = btnEliminarRef[0];
                                    JButton btnCancelar = btnCancelarRef[0];
                                    setEnabledSafe(btnEliminar, false);
                                    setEnabledSafe(btnCancelar, false);
                                    runModalActionWithButtonLoading(
                                            btnEliminar,
                                            "Eliminando...",
                                            () -> {
                                                try {
                                                    for (ModelColor d : list) {
                                                        service.delete(d.getColorId());
                                                    }
                                                } catch (SQLException ex) {
                                                    throw new RuntimeException(ex);
                                                }
                                            },
                                            () -> {
                                                Toast.show(this, Toast.Type.SUCCESS, "Color(es) eliminado(s)");
                                                try {
                                                    loadData();
                                                } catch (SQLException ex) {
                                                    Toast.show(this, Toast.Type.ERROR,
                                                            "Error al cargar datos: " + ex.getMessage());
                                                }
                                                mc.close();
                                            },
                                            (ex) -> {
                                                setEnabledSafe(btnEliminar, true);
                                                setEnabledSafe(btnCancelar, true);
                                                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                                                Toast.show(this, Toast.Type.ERROR,
                                                        "Error al eliminar: " + cause.getMessage());
                                            });
                                } else if (i == SimpleModalBorder.OPENED) {
                                    Window w = SwingUtilities.getWindowAncestor(label);
                                    btnEliminarRef[0] = findButtonByText(w, "Eliminar");
                                    btnCancelarRef[0] = findButtonByText(w, "Cancelar");
                                    styleActionButton(
                                            btnEliminarRef[0],
                                            new Color(255, 69, 58),
                                            new Color(255, 92, 84),
                                            new Color(233, 58, 50),
                                            dashboardIcon("minus-circle.svg", 16, Color.WHITE));
                                    styleActionButton(
                                            btnCancelarRef[0],
                                            new Color(75, 85, 99),
                                            new Color(55, 65, 81),
                                            new Color(31, 41, 55),
                                            dashboardIcon("x-circle.svg", 16, Color.WHITE));
                                }
                            }));
        } else {
            Toast.show(this, Toast.Type.WARNING, "Seleccione al menos un color");
        }
    }// GEN-LAST:event_btn_eliminarActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private raven.componentes.ButtonAction btn_editar;
    private raven.componentes.ButtonAction btn_eliminar;
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
