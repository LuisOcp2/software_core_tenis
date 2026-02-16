package raven.application.form.productos;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import java.awt.Font;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.JComboBox;
import javax.swing.DefaultCellEditor;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import raven.application.form.productos.creates.CreatePrestamo;
import raven.clases.productos.PrestamoZapatoService;
import raven.componentes.CheckBoxTableHeaderRenderer;
import raven.componentes.TableHeaderAlignment;
import raven.controlador.principal.conexion;
import raven.controlador.admin.SessionManager;
import raven.modal.ModalDialog;
import raven.modal.Toast;
import raven.modal.component.SimpleModalBorder;
import raven.modal.option.BorderOption;
import java.util.Map;
import raven.clases.admin.ServiceUser;
import raven.modal.listener.ModalController;

/**
 *
 * @author CrisDEV
 */
public class PrestamoForm extends javax.swing.JPanel {

    private final PrestamoZapatoService prestamoService = new PrestamoZapatoService();

    public PrestamoForm() {
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
    }

// Método de inicialización personalizado para componentes UI
    private void init() {
        // Estiliza el panel principal con bordes redondeados y color de fondo
        panel.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:25;" // Radio de esquina de 25px
                + "background:$Login.background;");  // Usa color de fondo de tabla

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
// Personalización individual para cada botón
        btn_nuevo.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btn_editar.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        btn_editar.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:$Accent.yellow");  // Color de fondo
        btn_eliminar.putClientProperty(FlatClientProperties.STYLE, ""
                + "background:$App.accent.red");  // Color de fondo

        btn_eliminar.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
// 1. Cargar y ajustar tamaño del icono
        FlatSVGIcon icon = new FlatSVGIcon(getClass().getResource("/raven/menu/icon/10.svg"));
        FlatSVGIcon icon1 = new FlatSVGIcon(getClass().getResource("/raven/menu/icon/11.svg"));
        FlatSVGIcon icon2 = new FlatSVGIcon(getClass().getResource("/raven/menu/icon/12.svg"));

        icon = icon.derive(16, 16); // Tamaño fijo para el icono
        icon1 = icon1.derive(16, 16); // Tamaño fijo para el icono
        icon2 = icon2.derive(16, 16); // Tamaño fijo para el icono

        btn_nuevo.setIcon(icon);
        btn_editar.setIcon(icon1);
        btn_eliminar.setIcon(icon2);

// 3. Opcional: Margen para separar icono y texto
        btn_nuevo.setMargin(new Insets(2, 5, 2, 5));
        btn_editar.setMargin(new Insets(2, 5, 2, 5));
        btn_eliminar.setMargin(new Insets(2, 5, 2, 5));
        // Configura renderizadores personalizados para columnas

        // Habilitar Imprimir solo si hay 1 selección (checkbox)
        btnImprimir.setEnabled(false);
        ((DefaultTableModel) table.getModel()).addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE) {
                    actualizarEstadoBotonImprimir();
                }
            }
        });

        // Editor combo para columna Estado (índice 8)
        try {
            java.util.List<String> estados = prestamoService.listarEstadosDisponibles();
            // Editor con filtro dinámico según estado actual
            table.getColumnModel().getColumn(8).setCellEditor(new EstadoCellEditor(estados));
            table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

            // Persistir cambios al editar la celda
            ((DefaultTableModel) table.getModel()).addTableModelListener(new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 8) {
                        int row = e.getFirstRow();
                        Object idObj = table.getValueAt(row, 1);
                        Object estadoObj = table.getValueAt(row, 8);
                        if (idObj != null && estadoObj != null) {
                            try {
                                int idPrestamo = ((Number) idObj).intValue();
                                String nuevoEstado = String.valueOf(estadoObj);
                                boolean ok = prestamoService.cambiarEstadoPrestamo(idPrestamo, nuevoEstado);
                                if (ok) {
                                    Toast.show(panel, Toast.Type.SUCCESS, "Estado actualizado: " + nuevoEstado);
                                } else {
                                    Toast.show(panel, Toast.Type.WARNING, "No se pudo actualizar el estado");
                                }
                            } catch (Exception ex) {
                                Toast.show(panel, Toast.Type.ERROR, "Error actualizando estado: " + ex.getMessage());
                            }
                        }
                    }
                }
            });
            // Renderer de la columna Estado con estilo de badge
            configurarColumnaEstadoConColor();
        } catch (Exception ex) {
            // Si falla, no bloquear la UI
        }
        table.getColumnModel().getColumn(0).setHeaderRenderer(new CheckBoxTableHeaderRenderer(table, 0));
        table.getTableHeader().setDefaultRenderer(new TableHeaderAlignment(table));
        // Altura de fila suficiente para el badge
        table.setRowHeight(Math.max(table.getRowHeight(), 28));

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
            actualizarEstadoBotonImprimir();
            conexion.getInstance().close();

        } catch (SQLException e) {
            // Manejo silencioso de errores (debería mejorarse)
        }
    }

// Carga datos desde el servicio a la tabla
    private void loadData() throws SQLException {
        try {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            if (table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
            model.setRowCount(0);

            List<Map<String, Object>> filas = prestamoService.listarPrestamosParaTabla(null);
            for (Map<String, Object> fila : filas) {
                ImageIcon icon = crearIconoDesdeBytes(fila.get("imagenVariante"), 60);
                Object[] row = new Object[]{
                    false,
                    fila.get("id"),
                    fila.get("producto"),
                    icon,
                    fila.get("talla"),
                    fila.get("direccion"),
                    fila.get("pie"),
                    fila.get("cliente"),
                    fila.get("estado")
                };
                model.addRow(row);
            }
            configurarColumnaImagen();
            actualizarEstadoBotonImprimir();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.show(this, Toast.Type.ERROR, "Error al cargar préstamos: " + e.getMessage());
        }
    }

    // Editor de celda que ajusta opciones del combo según el estado actual
    private class EstadoCellEditor extends DefaultCellEditor {

        private final JComboBox<String> combo;
        private final java.util.List<String> baseEstados;

        public EstadoCellEditor(java.util.List<String> estados) {
            super(new JComboBox<>());
            this.combo = (JComboBox<String>) getComponent();
            this.baseEstados = new java.util.ArrayList<>(estados);
            // Mantener el renderer tipo badge en el combo del editor
            this.combo.setRenderer(new BadgeListRenderer());
        }

        @Override
        public java.awt.Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            String actual = value != null ? String.valueOf(value).trim().toUpperCase() : "";
            javax.swing.DefaultComboBoxModel<String> model = new javax.swing.DefaultComboBoxModel<>();

            if ("DEVUELTO".equals(actual)) {
                // No mostrar PRESTADO cuando estado actual es DEVUELTO
                model.addElement("DEVUELTO");
                model.addElement("PERDIDO");
                model.addElement("DANADO");
            } else if ("PERDIDO".equals(actual) || "DANADO".equals(actual) || "DAÑADO".equals(actual)) {
                // En PERDIDO/DAÑADO no permitir PRESTADO ni DEVUELTO
                model.addElement("PERDIDO");
                model.addElement("DANADO");
            } else {
                // Estado PRESTADO u otros: mostrar lista base completa
                for (String e : baseEstados) {
                    model.addElement(e);
                }
            }

            combo.setModel(model);
            // Delegar selección al DefaultCellEditor
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
    }

// Busca datos según el texto ingresado
    private void searchData(String search) throws SQLException {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        model.setRowCount(0);

        List<Map<String, Object>> filas = prestamoService.listarPrestamosParaTabla(null);
        String term = search == null ? "" : search.toLowerCase();
        for (Map<String, Object> fila : filas) {
            String texto = (fila.get("cliente") + " " + fila.get("producto") + " " + fila.get("bodega") + " " + fila.get("talla") + " " + fila.get("direccion") + " " + fila.get("pie") + " " + fila.get("estado")).toLowerCase();
            String idStr = String.valueOf(fila.get("id"));
            if (term.isBlank() || texto.contains(term) || idStr.contains(term)) {
                ImageIcon icon = crearIconoDesdeBytes(fila.get("imagenVariante"), 60);
                Object[] row = new Object[]{
                    false,
                    fila.get("id"),
                    fila.get("producto"),
                    icon,
                    fila.get("talla"),
                    fila.get("direccion"),
                    fila.get("pie"),
                    fila.get("cliente"),
                    fila.get("estado")
                };
                model.addRow(row);
            }
        }
        configurarColumnaImagen();
        actualizarEstadoBotonImprimir();
    }

    // Convierte bytes a ImageIcon escalado
    private ImageIcon crearIconoDesdeBytes(Object bytesObj, int alturaMaxima) {
        if (!(bytesObj instanceof byte[])) {
            return null;
        }
        byte[] bytes = (byte[]) bytesObj;
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        ImageIcon original = new ImageIcon(bytes);
        java.awt.Image img = original.getImage();
        int h = alturaMaxima;
        int w = img.getWidth(null);
        int originalH = img.getHeight(null);
        if (w <= 0 || originalH <= 0) {
            return original;
        }
        int newW = (int) Math.round((double) w * h / (double) originalH);
        java.awt.Image escalada = img.getScaledInstance(newW, h, java.awt.Image.SCALE_SMOOTH);
        return new ImageIcon(escalada);
    }

    // Configura columna "Zapato" para renderizar imágenes centradas y ancho adecuado
    private void configurarColumnaImagen() {
        try {
            javax.swing.table.TableColumn col = table.getColumnModel().getColumn(3);
            col.setPreferredWidth(70);
            javax.swing.table.DefaultTableCellRenderer renderer = new javax.swing.table.DefaultTableCellRenderer() {
                @Override
                protected void setValue(Object value) {
                    if (value instanceof ImageIcon) {
                        setIcon((ImageIcon) value);
                        setText("");
                    } else {
                        setIcon(null);
                        setText("");
                    }
                }
            };
            renderer.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            col.setCellRenderer(renderer);
        } catch (Exception ignore) {
            // Evitar romper la UI si el índice cambia
        }
    }

    // Configura columna "Estado" con estilo de badge coloreado por valor
    private void configurarColumnaEstadoConColor() {
        try {
            javax.swing.table.TableColumn col = table.getColumnModel().getColumn(8);
            col.setCellRenderer(new EstadoBadgeRenderer());
            col.setPreferredWidth(100);
        } catch (Exception ignore) {
            // Evitar romper la UI si el índice cambia
        }
    }

    // Mapa de colores por estado
    private Color colorEstado(String estado) {
        if (estado == null) {
            return new Color(128, 128, 128);
        }
        String e = estado.trim().toUpperCase();
        switch (e) {
            case "PRESTADO":
                return new Color(255, 159, 10);    // Naranja tipo "pendiente"
            case "DEVUELTO":
                return new Color(40, 205, 65);     // Verde
            case "PERDIDO":
                return new Color(255, 159, 10);    // Naranja
            case "DANADO":
            case "DAÑADO":
                return new Color(255, 59, 48);     // Rojo
            default:
                return new Color(128, 128, 128);   // Gris
        }
    }

    // Renderer para mostrar un badge redondeado en la columna Estado
    private class EstadoBadgeRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
            setText(value != null ? String.valueOf(value).toLowerCase() : "");
            setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            setOpaque(false); // Pintamos solo el badge; el fondo de la tabla se mantiene
            setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            String estado = getText();
            Color bg = colorEstado(estado);
            Color fg = Color.WHITE;

            Font font = getFont().deriveFont(Font.BOLD);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics(font);
            int textW = fm.stringWidth(estado);
            int badgeW = textW + 16; // padding horizontal
            int badgeH = Math.max(20, fm.getHeight() + 6); // alto del badge
            int x = (getWidth() - badgeW) / 2;
            int y = (getHeight() - badgeH) / 2;

            g2.setColor(bg);
            g2.fillRoundRect(x, y, badgeW, badgeH, 12, 12);

            g2.setColor(fg);
            int tx = x + (badgeW - textW) / 2;
            int ty = y + (badgeH + fm.getAscent() - fm.getDescent()) / 2 - 1;
            g2.drawString(estado, tx, ty);
            g2.dispose();
        }
    }

    // Renderer para items del combo como badge
    private class BadgeListRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            String estado = value != null ? String.valueOf(value).toLowerCase() : "";
            setText(estado);
            setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            String estado = getText();
            Color bg = colorEstado(estado);
            Color fg = Color.WHITE;

            Font font = getFont().deriveFont(Font.BOLD);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics(font);
            int textW = fm.stringWidth(estado);
            int badgeW = Math.min(getWidth() - 12, textW + 16);
            int badgeH = Math.max(20, fm.getHeight() + 6);
            int x = (getWidth() - badgeW) / 2;
            int y = (getHeight() - badgeH) / 2;

            g2.setColor(bg);
            g2.fillRoundRect(x, y, badgeW, badgeH, 12, 12);

            g2.setColor(fg);
            int tx = x + (badgeW - textW) / 2;
            int ty = y + (badgeH + fm.getAscent() - fm.getDescent()) / 2 - 1;
            g2.drawString(estado, tx, ty);
            g2.dispose();
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        lb = new javax.swing.JLabel();
        panel = new javax.swing.JPanel();
        scroll = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        jSeparator1 = new javax.swing.JSeparator();
        txtSearch = new javax.swing.JTextField();
        lbTitle = new javax.swing.JLabel();
        btn_nuevo = new javax.swing.JButton();
        btn_editar = new javax.swing.JButton();
        btn_eliminar = new javax.swing.JButton();
        btnImprimir = new javax.swing.JToggleButton();

        lb.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lb.setText("GESTION DE PRESTAMOS");

        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "SELECT", "Codigo", "Modelo", "Zapato", "Talla", "Dirección", "Pie Prestado", "Nombre", "Estado"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                true, false, false, false, false, false, false, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                if (columnIndex == 8) {
                    Object estadoObj = getValueAt(rowIndex, 8);
                    if (estadoObj != null) {
                        String estado = String.valueOf(estadoObj).trim().toUpperCase();
                        if ("DEVUELTO".equals(estado)) {
                            return false; // Bloquear edición cuando el préstamo ya está DEVUELTO
                        }
                    }
                }
                return canEdit [columnIndex];
            }
        });
        table.getTableHeader().setReorderingAllowed(false);
        scroll.setViewportView(table);
        if (table.getColumnModel().getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setPreferredWidth(20);
            table.getColumnModel().getColumn(1).setPreferredWidth(30);
            table.getColumnModel().getColumn(2).setPreferredWidth(160);
            table.getColumnModel().getColumn(3).setPreferredWidth(30);
            table.getColumnModel().getColumn(4).setPreferredWidth(90);
        }

        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtSearchKeyReleased(evt);
            }
        });

        lbTitle.setText("Prestamos");

        btn_nuevo.setText("CREAR");
        buttonGroup1.add(btn_nuevo);
        btn_nuevo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_nuevoActionPerformed(evt);
            }
        });

        btn_editar.setText("EDITAR");
        buttonGroup1.add(btn_editar);
        btn_editar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_editarActionPerformed(evt);
            }
        });

        btn_eliminar.setText("ELIMINAR");
        buttonGroup1.add(btn_eliminar);
        btn_eliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_eliminarActionPerformed(evt);
            }
        });

        btnImprimir.setText("Imprimir");
        btnImprimir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImprimirActionPerformed(evt);
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
                                .addGap(425, 425, 425))
                            .addGroup(panelLayout.createSequentialGroup()
                                .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 239, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 122, Short.MAX_VALUE)
                                .addComponent(btnImprimir)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btn_nuevo, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(2, 2, 2)
                                .addComponent(btn_editar, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(2, 2, 2)))
                        .addComponent(btn_eliminar, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(16, 16, 16))
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
                    .addComponent(btn_nuevo)
                    .addComponent(btn_editar)
                    .addComponent(btn_eliminar)
                    .addComponent(btnImprimir))
                .addGap(18, 18, 18)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 625, Short.MAX_VALUE))
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
            actualizarEstadoBotonImprimir();
        } catch (SQLException ex) {
            Logger.getLogger(PrestamoForm.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_txtSearchKeyReleased

    private void btnImprimirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImprimirActionPerformed
// REEMPLAZA todo tu código original de impresión con esto:

// Determinar el préstamo a imprimir: 1 checkbox en SELECT o fila seleccionada
        List<Integer> ids = getSelectedPrestamoIds();
        Integer idPrestamo = null;
        int row = -1;

        if (ids.size() == 1) {
            idPrestamo = ids.get(0);
            // Ubicar la fila correspondiente al id para usar valores de respaldo
            for (int i = 0; i < table.getRowCount(); i++) {
                Object idObj = table.getValueAt(i, 1);
                if (idObj != null && String.valueOf(idObj).equals(String.valueOf(idPrestamo))) {
                    row = i;
                    break;
                }
            }
        } else {
            row = table.getSelectedRow();
            if (row < 0) {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "Seleccione 1 préstamo marcando el checkbox SELECT o seleccione la fila.",
                        "Imprimir Préstamo",
                        javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }
            Object codigoObj = table.getValueAt(row, 1);
            try {
                idPrestamo = Integer.parseInt(String.valueOf(codigoObj));
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "ID de préstamo inválido",
                        "Imprimir Préstamo",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        if (idPrestamo == null) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Seleccione 1 préstamo marcando el checkbox SELECT o seleccione la fila.",
                    "Imprimir Préstamo",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

// ============================================================
// NUEVA LÓGICA: Generar recibo de préstamo en PDF
// ============================================================
        try {
            // Crear instancia de la clase ReciboPrestamo
            raven.clases.reportes.ReciboPrestamo recibo = new raven.clases.reportes.ReciboPrestamo();

            // Generar el recibo en PDF
            boolean exitoso = recibo.generarRecibo(idPrestamo);

            if (exitoso) {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "Recibo de préstamo generado exitosamente.\nEl archivo PDF se abrirá automáticamente.",
                        "Imprimir Préstamo",
                        javax.swing.JOptionPane.INFORMATION_MESSAGE);
            } else {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "No se pudo generar el recibo. Verifique que el préstamo existe.",
                        "Imprimir Préstamo",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            Logger.getLogger(PrestamoForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al generar el recibo: " + ex.getMessage(),
                    "Imprimir Préstamo",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnImprimirActionPerformed

    private void btn_nuevoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_nuevoActionPerformed
        // Crear panel de préstamo
        CreatePrestamo cr = new CreatePrestamo();

        // Opciones del modal
        SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[]{
            new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION), // Opción para cancelar
            new SimpleModalBorder.Option("Guardar", SimpleModalBorder.OK_OPTION) // Opción para guardar
        };

        // Mostrar modal
        ModalDialog.showModal(this, // El contexto actual (probablemente una ventana o formulario)
                new SimpleModalBorder(cr, "Nuevo Préstamo", options, (ModalController mc, int i) -> { // Crea un nuevo SimpleModalBorder
                    // Maneja la acción cuando se cierra el modal
                    if (i == SimpleModalBorder.OK_OPTION) { // Si se selecciona la opción "Guardar"
                        try {
                            // Crear préstamo
                            raven.modelos.PrestamoZapato data = cr.getData();
                            if (data == null) {
                                Toast.show(PrestamoForm.this, Toast.Type.WARNING, "Revisa los datos: producto y fecha");
                                return;
                            }
                            prestamoService.crearPrestamo(data);
                            Toast.show(PrestamoForm.this, Toast.Type.SUCCESS, "Préstamo creado exitosamente");
                            loadData();
                        } catch (Exception e) {
                            Toast.show(PrestamoForm.this, Toast.Type.ERROR, "Error al crear préstamo: " + e.getMessage());
                        }
                    } else if (i == SimpleModalBorder.OPENED) { // Si el modal se abre
                        // Inicializa el formulario 'cr' para que esté listo para la entrada de datos
                        cr.init();
                    }
                })
        );
    }//GEN-LAST:event_btn_nuevoActionPerformed

    private void btn_editarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_editarActionPerformed
        try {
            // Elegir préstamo a editar: checkbox seleccionado o fila seleccionada
            List<Integer> ids = getSelectedPrestamoIds();
            Integer idToEdit = null;
            if (ids.size() == 1) {
                idToEdit = ids.get(0);
            } else {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    Object idObj = table.getValueAt(row, 1);
                    if (idObj instanceof Number) {
                        idToEdit = ((Number) idObj).intValue();
                    }
                }
            }

            if (idToEdit == null) {
                Toast.show(this, Toast.Type.WARNING, "Seleccione 1 préstamo para editar");
                return;
            }

            // Obtener préstamo desde servicio
            raven.modelos.PrestamoZapato prestamo = prestamoService.obtenerPrestamo(idToEdit);
            if (prestamo == null) {
                Toast.show(this, Toast.Type.ERROR, "No se encontró el préstamo seleccionado");
                return;
            }

            // Crear panel y mostrar modal de edición
            CreatePrestamo cr = new CreatePrestamo();
            SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[]{
                new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                new SimpleModalBorder.Option("Guardar", SimpleModalBorder.OK_OPTION)
            };

            ModalDialog.showModal(
                    this,
                    new SimpleModalBorder(cr, "Editar Préstamo", options, (mc, i) -> {
                        if (i == SimpleModalBorder.OK_OPTION) {
                            try {
                                raven.modelos.PrestamoZapato data = cr.getData();
                                if (data == null) {
                                    Toast.show(PrestamoForm.this, Toast.Type.WARNING, "Revisa los datos del formulario");
                                    return;
                                }
                                // Mantener el id del préstamo y la fecha original de préstamo
                                // Usar el id del objeto prestamo para evitar capturar variable no-final
                                data.setIdPrestamo(prestamo.getIdPrestamo());
                                data.setFechaPrestamo(prestamo.getFechaPrestamo());

                                boolean ok = prestamoService.actualizarDatosPrestamo(data);
                                if (ok) {
                                    Toast.show(PrestamoForm.this, Toast.Type.SUCCESS, "Préstamo actualizado");
                                    loadData();
                                } else {
                                    Toast.show(PrestamoForm.this, Toast.Type.WARNING, "No se pudo actualizar el préstamo");
                                }
                            } catch (Exception ex) {
                                Toast.show(PrestamoForm.this, Toast.Type.ERROR, "Error al actualizar: " + ex.getMessage());
                            }
                        } else if (i == SimpleModalBorder.OPENED) {
                            try {
                                cr.init();
                                cr.cargarDesdePrestamo(prestamo);
                            } catch (Exception ex) {
                                Toast.show(PrestamoForm.this, Toast.Type.ERROR, "Error cargando datos: " + ex.getMessage());
                            }
                        }
                    })
            );
        } catch (Exception e) {
            Toast.show(this, Toast.Type.ERROR, "Error en edición: " + e.getMessage());
        }
    }//GEN-LAST:event_btn_editarActionPerformed

    private void btn_eliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_eliminarActionPerformed
        // Validar rol: solo admin y gerentes pueden eliminar préstamos
        SessionManager sm = SessionManager.getInstance();
        boolean permitido = sm.hasRole("admin") || sm.hasRole("gerente");
        if (!permitido) {
            Toast.show(this, Toast.Type.WARNING, "Solo admin o gerentes pueden eliminar préstamos");
            return;
        }

        // Eliminar préstamos seleccionados, reponiendo stock si corresponde
        List<Integer> ids = getSelectedPrestamoIds();
        if (!ids.isEmpty()) {
            SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[]{
                new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                new SimpleModalBorder.Option("Eliminar", SimpleModalBorder.OK_OPTION)
            };

            JLabel label = new JLabel("¿Eliminar " + ids.size() + " préstamo(s)? Se repondrá stock si corresponde.");
            label.setBorder(new EmptyBorder(5, 25, 5, 25));

            ModalDialog.showModal(
                    this,
                    new SimpleModalBorder(
                            label,
                            "Confirmar Eliminación",
                            options,
                            (mc, i) -> {
                                if (i == SimpleModalBorder.OK_OPTION) {
                                    try {
                                        Integer idUsuario = sm.getCurrentUserId();
                                        for (Integer id : ids) {
                                            prestamoService.eliminarPrestamo(id, idUsuario);
                                        }
                                        Toast.show(this, Toast.Type.SUCCESS, "Préstamo(s) eliminado(s)");
                                        loadData();
                                    } catch (Exception e) {
                                        Toast.show(this, Toast.Type.ERROR, "Error al eliminar préstamo(s): " + e.getMessage());
                                    }
                                }
                            }
                    )
            );
        } else {
            Toast.show(this, Toast.Type.WARNING, "Seleccione al menos un préstamo");
        }
    }//GEN-LAST:event_btn_eliminarActionPerformed
    // Obtener IDs de préstamos seleccionados en la tabla
    private List<Integer> getSelectedPrestamoIds() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < table.getRowCount(); i++) {
            if (Boolean.TRUE.equals(table.getValueAt(i, 0))) {
                Object idVal = table.getValueAt(i, 1);
                try {
                    list.add(Integer.parseInt(String.valueOf(idVal)));
                } catch (NumberFormatException ignore) {
                }
            }
        }
        return list;
    }

    private void actualizarEstadoBotonImprimir() {
        btnImprimir.setEnabled(getSelectedPrestamoIds().size() == 1);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToggleButton btnImprimir;
    public javax.swing.JButton btn_editar;
    public javax.swing.JButton btn_eliminar;
    public javax.swing.JButton btn_nuevo;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel lb;
    private javax.swing.JLabel lbTitle;
    private javax.swing.JPanel panel;
    private javax.swing.JScrollPane scroll;
    public javax.swing.JTable table;
    public javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables
}
