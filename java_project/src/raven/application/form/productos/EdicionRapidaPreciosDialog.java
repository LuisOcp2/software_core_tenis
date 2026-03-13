package raven.application.form.productos;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import raven.controlador.principal.conexion;
import raven.controlador.productos.ModelProduct;
import raven.utils.ProductImageOptimizer;

/**
 * Diálogo para edición rápida de precios de venta de las variantes de un
 * producto.
 * Permite editar múltiples precios de forma eficiente con tabla editable
 * inline.
 *
 * @author CrisDEV
 */
public class EdicionRapidaPreciosDialog extends JDialog {

    // Componentes
    private JTable tablaVariantes;
    private DefaultTableModel modeloTabla;
    private JLabel lblProducto;
    private JLabel lblTotalVariantes;
    private JTextField txtAjusteGlobal;
    private JComboBox<String> cbxTipoAjuste;
    private JButton btnAplicarAjuste;
    private JButton btnGuardar;
    private JButton btnCancelar;

    // Datos
    private int idProducto;
    private String nombreProducto;
    private List<VariantePrecio> variantes;
    private boolean cambiosGuardados = false;

    // Formateador de moneda colombiana
    private DecimalFormat formatoMoneda;
    private DecimalFormat formatoEdicion;

    /**
     * Clase interna para representar una variante con su precio
     */
    private static class VariantePrecio {
        int idVariante;
        String proveedor;
        String color;
        String talla;
        BigDecimal precioActual;
        BigDecimal precioNuevo;

        VariantePrecio(int idVariante, String proveedor, String color, String talla, BigDecimal precio) {
            this.idVariante = idVariante;
            this.proveedor = proveedor != null ? proveedor : "Sin proveedor";
            this.color = color != null ? color : "Sin color";
            this.talla = talla != null ? talla : "Sin talla";
            this.precioActual = precio != null ? precio : BigDecimal.ZERO;
            this.precioNuevo = this.precioActual;
        }
    }

    /**
     * Constructor del diálogo
     */
    public EdicionRapidaPreciosDialog(Frame parent, int idProducto, String nombreProducto) {
        super(parent, "Edición Rápida de Precios - " + nombreProducto, true);
        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.variantes = new ArrayList<>();

        // Configurar formato de moneda colombiano
        configurarFormatoMoneda();

        initComponents();
        cargarVariantes();
        configurarEdicionRapida();
    }

    /**
     * Configura el formato de moneda colombiano
     */
    private void configurarFormatoMoneda() {
        // Formato para visualización: $1.234.567,89
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(new Locale("es", "CO"));
        simbolos.setGroupingSeparator('.'); // Separador de miles
        simbolos.setDecimalSeparator(','); // Separador de decimales
        simbolos.setCurrencySymbol("$");

        formatoMoneda = new DecimalFormat("$#,##0.00", simbolos);

        // Formato para edición: permite entrada flexible
        formatoEdicion = new DecimalFormat("#,##0.00", simbolos);
    }

    /**
     * Inicializa los componentes del diálogo
     */
    private void initComponents() {
        setSize(950, 650);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout(8, 8));

        // Configurar fondo del diálogo con color del tema
        getRootPane().putClientProperty(FlatClientProperties.STYLE,
                "background:$Panel.background");

        // Panel superior con información del producto
        add(crearPanelSuperior(), BorderLayout.NORTH);

        // Panel central con tabla de variantes
        add(crearPanelCentral(), BorderLayout.CENTER);

        // Panel inferior con botones de acción
        add(crearPanelInferior(), BorderLayout.SOUTH);

        aplicarEstilos();
    }

    /**
     * Crea el panel superior con información del producto
     */
    private JPanel crearPanelSuperior() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));

        // Usar colores del tema para el panel
        panel.setBackground(UIManager.getColor("Panel.background"));

        // Información del producto
        JPanel panelInfo = new JPanel(new GridLayout(2, 1, 4, 4));
        panelInfo.setOpaque(false); // Transparente para heredar fondo del padre

        lblProducto = new JLabel("Producto: " + nombreProducto);
        lblProducto.setFont(lblProducto.getFont().deriveFont(Font.BOLD, 16f));
        lblProducto.setForeground(UIManager.getColor("Label.foreground"));

        lblTotalVariantes = new JLabel("Cargando variantes...");
        lblTotalVariantes.setFont(lblTotalVariantes.getFont().deriveFont(14f));
        lblTotalVariantes.setForeground(UIManager.getColor("Label.foreground"));

        panelInfo.add(lblProducto);
        panelInfo.add(lblTotalVariantes);

        panel.add(panelInfo, BorderLayout.WEST);

        // Panel de ajuste global con colores del tema
        JPanel panelAjuste = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panelAjuste.setBorder(BorderFactory.createTitledBorder("Ajuste Global de Precios"));
        panelAjuste.setBackground(UIManager.getColor("Panel.background"));

        cbxTipoAjuste = new JComboBox<>(new String[] {
                "Aumentar %",
                "Disminuir %",
                "Aumentar $",
                "Disminuir $",
                "Establecer precio fijo"
        });

        txtAjusteGlobal = new JTextField(8);
        txtAjusteGlobal.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Valor");

        // Crear iconos SVG para los botones
        Icon iconoSeleccionados = crearIconoFlecha();
        Icon iconoTodos = crearIconoGlobal();

        btnAplicarAjuste = new JButton("Aplicar a Seleccionados", iconoSeleccionados);
        btnAplicarAjuste.setToolTipText("Aplica el ajuste solo a las variantes seleccionadas en la tabla");
        btnAplicarAjuste.addActionListener(e -> aplicarAjusteSeleccionados());

        JButton btnAplicarTodos = new JButton("Aplicar a Todos", iconoTodos);
        btnAplicarTodos.setToolTipText("Aplica el ajuste a todas las variantes");
        btnAplicarTodos.addActionListener(e -> aplicarAjusteGlobal());

        panelAjuste.add(new JLabel("Tipo:"));
        panelAjuste.add(cbxTipoAjuste);
        panelAjuste.add(new JLabel("Valor:"));
        panelAjuste.add(txtAjusteGlobal);
        panelAjuste.add(btnAplicarAjuste);
        panelAjuste.add(btnAplicarTodos);

        panel.add(panelAjuste, BorderLayout.EAST);

        return panel;
    }

    /**
     * Crea el panel central con la tabla de variantes
     */
    private JPanel crearPanelCentral() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

        // Panel de instrucciones con estilo profesional y colores del tema
        JPanel panelInstrucciones = new JPanel(new BorderLayout());
        panelInstrucciones.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Component.borderColor")),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        // Icono de información
        Icon iconoInfo = crearIconoInfo();

        JLabel lblInstrucciones = new JLabel(
                " Doble clic para editar | Ctrl+Clic para seleccionar múltiples | Enter para confirmar",
                iconoInfo,
                JLabel.LEFT);
        lblInstrucciones.setFont(lblInstrucciones.getFont().deriveFont(Font.PLAIN, 12f));
        lblInstrucciones.setForeground(UIManager.getColor("Label.foreground"));

        // Usar color de fondo adaptativo al tema
        boolean isDark = UIManager.getBoolean("laf.dark");
        if (isDark) {
            panelInstrucciones.setBackground(UIManager.getColor("Panel.background"));
        } else {
            Color infoBg = UIManager.getColor("TextField.background");
            panelInstrucciones.setBackground(infoBg != null ? infoBg : UIManager.getColor("Panel.background"));
        }

        panelInstrucciones.add(lblInstrucciones, BorderLayout.CENTER);
        panel.add(panelInstrucciones, BorderLayout.NORTH);

        // Tabla de variantes
        String[] columnas = {
                "Imagen",
                "ID Variante",
                "Proveedor",
                "Color",
                "Talla",
                "Precio Actual",
                "Precio Nuevo"
        };

        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0)
                    return ModelProduct.class;
                if (columnIndex == 1)
                    return Integer.class;
                if (columnIndex == 5 || columnIndex == 6)
                    return Double.class;
                return String.class;
            }
        };

        tablaVariantes = new JTable(modeloTabla);
        tablaVariantes.setRowHeight(70);
        tablaVariantes.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Configuración profesional de la tabla
        tablaVariantes.setShowGrid(true);
        tablaVariantes.setGridColor(UIManager.getColor("Table.gridColor"));
        tablaVariantes.setIntercellSpacing(new Dimension(1, 1));
        tablaVariantes.setFillsViewportHeight(true);

        // Aplicar estilos de FlatLaf
        tablaVariantes.putClientProperty(FlatClientProperties.STYLE,
                "showHorizontalLines:true;" +
                        "showVerticalLines:true;" +
                        "intercellSpacing:1,1;" +
                        "rowHeight:36");

        // Habilitar alternancia de colores de fila
        tablaVariantes.putClientProperty(FlatClientProperties.STYLE_CLASS, "striped");

        // Configurar anchos de columnas
        tablaVariantes.getColumnModel().getColumn(0).setPreferredWidth(90); // Imagen
        tablaVariantes.getColumnModel().getColumn(0).setMaxWidth(110);
        tablaVariantes.getColumnModel().getColumn(1).setPreferredWidth(80); // ID
        tablaVariantes.getColumnModel().getColumn(1).setMaxWidth(100);
        tablaVariantes.getColumnModel().getColumn(2).setPreferredWidth(150); // Proveedor
        tablaVariantes.getColumnModel().getColumn(3).setPreferredWidth(120); // Color
        tablaVariantes.getColumnModel().getColumn(4).setPreferredWidth(80); // Talla
        tablaVariantes.getColumnModel().getColumn(5).setPreferredWidth(120); // Precio Actual
        tablaVariantes.getColumnModel().getColumn(6).setPreferredWidth(120); // Precio Nuevo

        // Renderizador profesional para precios (formato moneda colombiano)
        DefaultTableCellRenderer precioRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // Colores del tema
                Color bgDefault = UIManager.getColor("Table.background");
                Color bgAlternate = UIManager.getColor("Table.alternateRowColor");
                Color fgDefault = UIManager.getColor("Table.foreground");
                Color selectionBg = UIManager.getColor("Table.selectionBackground");
                Color selectionFg = UIManager.getColor("Table.selectionForeground");

                if (value instanceof Double) {
                    // Usar formato colombiano: $1.234.567,89
                    setText(formatoMoneda.format((Double) value));
                    setHorizontalAlignment(JLabel.RIGHT);
                }

                // Establecer colores base según selección y alternancia
                if (isSelected) {
                    setBackground(selectionBg);
                    setForeground(selectionFg);
                } else {
                    // Alternancia de colores (zebra striping)
                    setBackground(row % 2 == 0 ? bgDefault : bgAlternate);
                    setForeground(fgDefault);
                }

                if (column == 6 && row < variantes.size()) {
                    VariantePrecio v = variantes.get(row);
                    if (v.precioNuevo.compareTo(v.precioActual) != 0) {
                        setFont(getFont().deriveFont(Font.BOLD, 13f));

                        if (!isSelected) {
                            // Color de resaltado que se adapta al tema
                            boolean isDark = UIManager.getBoolean("laf.dark");
                            if (isDark) {
                                // Tema oscuro: amarillo oscuro
                                setBackground(new Color(102, 77, 0)); // Amarillo oscuro
                                setForeground(new Color(255, 224, 130)); // Amarillo claro para texto
                            } else {
                                // Tema claro: amarillo claro
                                setBackground(new Color(255, 248, 220)); // Amarillo muy claro
                                setForeground(new Color(204, 102, 0)); // Naranja oscuro para texto
                            }
                        } else {
                            // Cuando está seleccionado, usar color de selección con texto en negrita
                            setForeground(selectionFg);
                        }
                    } else {
                        setFont(getFont().deriveFont(Font.PLAIN, 13f));
                    }
                } else {
                    setFont(getFont().deriveFont(Font.PLAIN, 13f));
                }

                // Borde para mejor definición
                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

                return this;
            }
        };

        tablaVariantes.getColumnModel().getColumn(5).setCellRenderer(precioRenderer);
        tablaVariantes.getColumnModel().getColumn(6).setCellRenderer(precioRenderer);

        // Renderizador profesional para columnas de texto
        DefaultTableCellRenderer textRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // Colores del tema
                Color bgDefault = UIManager.getColor("Table.background");
                Color bgAlternate = UIManager.getColor("Table.alternateRowColor");
                Color fgDefault = UIManager.getColor("Table.foreground");
                Color selectionBg = UIManager.getColor("Table.selectionBackground");
                Color selectionFg = UIManager.getColor("Table.selectionForeground");

                // Establecer colores según selección y alternancia
                if (isSelected) {
                    setBackground(selectionBg);
                    setForeground(selectionFg);
                } else {
                    setBackground(row % 2 == 0 ? bgDefault : bgAlternate);
                    setForeground(fgDefault);
                }

                setFont(getFont().deriveFont(Font.PLAIN, 13f));
                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

                return this;
            }
        };

        // Renderizador para ID (centrado)
        DefaultTableCellRenderer idRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // Colores del tema
                Color bgDefault = UIManager.getColor("Table.background");
                Color bgAlternate = UIManager.getColor("Table.alternateRowColor");
                Color fgDefault = UIManager.getColor("Table.foreground");
                Color selectionBg = UIManager.getColor("Table.selectionBackground");
                Color selectionFg = UIManager.getColor("Table.selectionForeground");

                setHorizontalAlignment(JLabel.CENTER);

                if (isSelected) {
                    setBackground(selectionBg);
                    setForeground(selectionFg);
                } else {
                    setBackground(row % 2 == 0 ? bgDefault : bgAlternate);
                    setForeground(fgDefault);
                }

                setFont(getFont().deriveFont(Font.BOLD, 12f));
                setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

                return this;
            }
        };

        // Aplicar renderizadores a cada columna
        tablaVariantes.getColumnModel().getColumn(0)
                .setCellRenderer(new ProductImageOptimizer.OptimizedProductRenderer());
        tablaVariantes.getColumnModel().getColumn(1).setCellRenderer(idRenderer); // ID
        tablaVariantes.getColumnModel().getColumn(2).setCellRenderer(textRenderer); // Proveedor
        tablaVariantes.getColumnModel().getColumn(3).setCellRenderer(textRenderer); // Color
        tablaVariantes.getColumnModel().getColumn(4).setCellRenderer(textRenderer); // Talla

        // Configurar encabezado de tabla con estilo profesional
        tablaVariantes.getTableHeader().setReorderingAllowed(false);
        tablaVariantes.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "height:32;" +
                        "font:bold 13;" +
                        "background:$Table.background;" +
                        "separatorColor:$Table.gridColor");

        JScrollPane scroll = new JScrollPane(tablaVariantes);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Crea el panel inferior con botones de acción
     */
    private JPanel crearPanelInferior() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
        panel.setBackground(UIManager.getColor("Panel.background"));

        // Panel izquierdo con información
        JPanel panelInfo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelInfo.setOpaque(false);

        Icon iconoGuardar = crearIconoGuardar();
        JLabel lblAyuda = new JLabel(
                " Los cambios se guardarán al presionar 'Guardar Cambios'",
                iconoGuardar,
                JLabel.LEFT);
        lblAyuda.setFont(lblAyuda.getFont().deriveFont(Font.ITALIC, 11f));
        lblAyuda.setForeground(UIManager.getColor("Label.foreground"));
        panelInfo.add(lblAyuda);
        panel.add(panelInfo, BorderLayout.WEST);

        // Panel derecho con botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panelBotones.setOpaque(false);

        Icon iconoGuardarBtn = crearIconoGuardar();
        Icon iconoCancelar = crearIconoCancelar();

        btnGuardar = new JButton("Guardar Cambios", iconoGuardarBtn);
        btnGuardar.addActionListener(e -> guardarCambios());

        btnCancelar = new JButton("Cancelar", iconoCancelar);
        btnCancelar.addActionListener(e -> {
            if (hayPendientes()) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Hay cambios sin guardar. ¿Desea salir sin guardar?",
                        "Confirmar",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    dispose();
                }
            } else {
                dispose();
            }
        });

        panelBotones.add(btnGuardar);
        panelBotones.add(btnCancelar);

        panel.add(panelBotones, BorderLayout.EAST);

        return panel;
    }

    /**
     * Aplica estilos modernos
     */
    private void aplicarEstilos() {
        // Botón principal - Guardar (Verde)
        btnGuardar.putClientProperty(FlatClientProperties.STYLE,
                "arc:8;background:#4CAF50;foreground:#fff;font:bold 14;borderWidth:0;margin:8,16,8,16");

        // Botón secundario - Cancelar (Gris)
        btnCancelar.putClientProperty(FlatClientProperties.STYLE,
                "arc:8;background:#757575;foreground:#fff;font:bold 13;borderWidth:0;margin:8,16,8,16");

        // Botón destacado - Aplicar a Seleccionados (Naranja)
        btnAplicarAjuste.putClientProperty(FlatClientProperties.STYLE,
                "arc:8;background:#FF6F00;foreground:#fff;font:bold 12;borderWidth:0;margin:6,12,6,12");

        // Buscar el botón "Aplicar a Todos" y aplicar estilo
        Component[] components = ((JPanel) btnAplicarAjuste.getParent()).getComponents();
        for (Component comp : components) {
            if (comp instanceof JButton && ((JButton) comp).getText().equals("Aplicar a Todos")) {
                // comp.putClientProperty(FlatClientProperties.STYLE,"arc:8;background:#2196F3;foreground:#fff;font:bold
                // 12;borderWidth:0;margin:6,12,6,12");
            }
        }

        txtAjusteGlobal.putClientProperty(FlatClientProperties.STYLE, "arc:8;font:14");
        cbxTipoAjuste.putClientProperty(FlatClientProperties.STYLE, "arc:8;font:13");
    }

    /**
     * Configura la edición rápida en la tabla
     */
    private void configurarEdicionRapida() {
        // Editor personalizado para la columna de precio con formato flexible
        JTextField editorField = new JTextField();
        editorField.setHorizontalAlignment(JTextField.RIGHT);

        DefaultCellEditor editor = new DefaultCellEditor(editorField) {
            @Override
            public boolean stopCellEditing() {
                String value = editorField.getText().trim();

                // Validar y parsear el número (permite múltiples formatos)
                try {
                    if (value.isEmpty()) {
                        editorField.setText("0");
                        return super.stopCellEditing();
                    }

                    // Limpiar el valor: remover símbolos y convertir coma a punto
                    value = value.replace("$", "")
                            .replace(" ", "")
                            .replace(".", "") // Remover puntos de miles
                            .replace(",", ".") // Convertir coma decimal a punto
                            .trim();

                    double precio = Double.parseDouble(value);

                    if (precio < 0) {
                        JOptionPane.showMessageDialog(EdicionRapidaPreciosDialog.this,
                                "El precio no puede ser negativo",
                                "Error de validación",
                                JOptionPane.ERROR_MESSAGE);
                        return false;
                    }

                    // Establecer el valor parseado
                    editorField.setText(String.valueOf(precio));

                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(EdicionRapidaPreciosDialog.this,
                            "Ingrese un número válido.\nEjemplos: 50000 | 50.000 | 50000,50 | $50.000,50",
                            "Error de validación",
                            JOptionPane.ERROR_MESSAGE);
                    return false;
                }

                return super.stopCellEditing();
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value,
                    boolean isSelected, int row, int column) {
                Component c = super.getTableCellEditorComponent(table, value, isSelected, row, column);
                if (value instanceof Double) {
                    // Mostrar el número sin formato para fácil edición
                    editorField.setText(String.format("%.0f", (Double) value));
                    editorField.selectAll(); // Seleccionar todo para fácil reemplazo
                }
                return c;
            }
        };

        tablaVariantes.getColumnModel().getColumn(6).setCellEditor(editor);

        // Listener para actualizar el objeto VariantePrecio cuando se edita
        modeloTabla.addTableModelListener(e -> {
            if (e.getColumn() == 6) {
                int row = e.getFirstRow();
                if (row >= 0 && row < variantes.size()) {
                    Object valorObj = modeloTabla.getValueAt(row, 6);
                    if (valorObj != null) {
                        double valorDouble;
                        if (valorObj instanceof String) {
                            String valorStr = ((String) valorObj).replace("$", "").replace(",", "").trim();
                            valorDouble = Double.parseDouble(valorStr);
                        } else {
                            valorDouble = ((Number) valorObj).doubleValue();
                        }
                        variantes.get(row).precioNuevo = BigDecimal.valueOf(valorDouble);
                    }
                }
                tablaVariantes.repaint(); // Repintar para mostrar el resaltado
            }
        });
    }

    /**
     * Carga las variantes del producto desde la base de datos
     */
    private void cargarVariantes() {
        variantes.clear();
        modeloTabla.setRowCount(0);
        ModelProduct productoImagen = new ModelProduct();
        productoImagen.setProductId(idProducto);
        productoImagen.setName("");

        String sql = "SELECT " +
                "pv.id_variante, " +
                "COALESCE(p.nombre, 'Sin proveedor') AS proveedor, " +
                "COALESCE(c.nombre, 'Sin color') AS color, " +
                "COALESCE(t.numero, 'S/T') AS talla, " +
                "pv.precio_venta " +
                "FROM producto_variantes pv " +
                "LEFT JOIN proveedores p ON pv.id_proveedor = p.id_proveedor " +
                "LEFT JOIN colores c ON pv.id_color = c.id_color " +
                "LEFT JOIN tallas t ON pv.id_talla = t.id_talla " +
                "WHERE pv.id_producto = ? " +
                "ORDER BY p.nombre, c.nombre, t.numero";

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setInt(1, idProducto);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    int idVariante = rs.getInt("id_variante");
                    String proveedor = rs.getString("proveedor");
                    String color = rs.getString("color");
                    String talla = rs.getString("talla");
                    BigDecimal precio = rs.getBigDecimal("precio_venta");
                    if (precio == null)
                        precio = BigDecimal.ZERO;

                    VariantePrecio vp = new VariantePrecio(idVariante, proveedor, color, talla, precio);
                    variantes.add(vp);

                    modeloTabla.addRow(new Object[] {
                            productoImagen,
                            idVariante,
                            proveedor,
                            color,
                            talla,
                            precio.doubleValue(),
                            precio.doubleValue()
                    });
                }
            }

            lblTotalVariantes.setText(String.format("Total de variantes: %d", variantes.size()));

            if (variantes.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Este producto no tiene variantes registradas",
                        "Sin variantes",
                        JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error al cargar variantes: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Aplica un ajuste solo a las variantes seleccionadas
     */
    private void aplicarAjusteSeleccionados() {
        int[] filasSeleccionadas = tablaVariantes.getSelectedRows();

        if (filasSeleccionadas.length == 0) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione al menos una variante en la tabla.\n" +
                            "Use Ctrl+Clic para seleccionar múltiples variantes.",
                    "Sin selección",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String valorStr = txtAjusteGlobal.getText().trim();
        if (valorStr.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Ingrese un valor para el ajuste",
                    "Campo requerido",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            double valor = Double.parseDouble(valorStr);
            String tipoAjuste = (String) cbxTipoAjuste.getSelectedItem();

            // Aplicar solo a las filas seleccionadas
            for (int row : filasSeleccionadas) {
                if (row >= 0 && row < variantes.size()) {
                    VariantePrecio vp = variantes.get(row);
                    BigDecimal precioActual = vp.precioNuevo;
                    BigDecimal precioNuevo = calcularPrecioAjustado(precioActual, valor, tipoAjuste);

                    vp.precioNuevo = precioNuevo;
                    modeloTabla.setValueAt(precioNuevo.doubleValue(), row, 6);
                }
            }

            tablaVariantes.repaint();
            txtAjusteGlobal.setText("");

            JOptionPane.showMessageDialog(this,
                    String.format("Ajuste aplicado a %d variante(s) seleccionada(s)", filasSeleccionadas.length),
                    "Ajuste completado",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Ingrese un valor numérico válido",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Aplica un ajuste global a todos los precios
     */
    private void aplicarAjusteGlobal() {
        String valorStr = txtAjusteGlobal.getText().trim();
        if (valorStr.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Ingrese un valor para el ajuste",
                    "Campo requerido",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            double valor = Double.parseDouble(valorStr);
            String tipoAjuste = (String) cbxTipoAjuste.getSelectedItem();

            for (int i = 0; i < variantes.size(); i++) {
                VariantePrecio vp = variantes.get(i);
                BigDecimal precioActual = vp.precioNuevo;
                BigDecimal precioNuevo = calcularPrecioAjustado(precioActual, valor, tipoAjuste);

                vp.precioNuevo = precioNuevo;
                modeloTabla.setValueAt(precioNuevo.doubleValue(), i, 6);
            }

            tablaVariantes.repaint();
            txtAjusteGlobal.setText("");

            JOptionPane.showMessageDialog(this,
                    String.format("Ajuste aplicado a %d variantes", variantes.size()),
                    "Ajuste completado",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Ingrese un valor numérico válido",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Calcula el precio ajustado según el tipo de ajuste
     */
    private BigDecimal calcularPrecioAjustado(BigDecimal precioActual, double valor, String tipoAjuste) {
        BigDecimal precioNuevo;

        switch (tipoAjuste) {
            case "Aumentar %":
                precioNuevo = precioActual.multiply(BigDecimal.valueOf(1 + valor / 100));
                break;
            case "Disminuir %":
                precioNuevo = precioActual.multiply(BigDecimal.valueOf(1 - valor / 100));
                break;
            case "Aumentar $":
                precioNuevo = precioActual.add(BigDecimal.valueOf(valor));
                break;
            case "Disminuir $":
                precioNuevo = precioActual.subtract(BigDecimal.valueOf(valor));
                break;
            case "Establecer precio fijo":
                precioNuevo = BigDecimal.valueOf(valor);
                break;
            default:
                precioNuevo = precioActual;
        }

        // No permitir precios negativos
        if (precioNuevo.compareTo(BigDecimal.ZERO) < 0) {
            precioNuevo = BigDecimal.ZERO;
        }

        return precioNuevo;
    }

    /**
     * Verifica si hay cambios pendientes de guardar
     */
    private boolean hayPendientes() {
        for (VariantePrecio vp : variantes) {
            if (vp.precioNuevo.compareTo(vp.precioActual) != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Guarda los cambios en la base de datos
     */
    private void guardarCambios() {
        // Contar cuántos precios cambiaron
        int cambios = 0;
        for (VariantePrecio vp : variantes) {
            if (vp.precioNuevo.compareTo(vp.precioActual) != 0) {
                cambios++;
            }
        }

        if (cambios == 0) {
            JOptionPane.showMessageDialog(this,
                    "No hay cambios para guardar",
                    "Sin cambios",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Confirmar
        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("¿Desea guardar los cambios en %d variante(s)?", cambios),
                "Confirmar guardado",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Guardar en la base de datos
        String sql = "UPDATE producto_variantes SET precio_venta = ? WHERE id_variante = ?";

        try (Connection con = conexion.getInstance().createConnection();
                PreparedStatement pst = con.prepareStatement(sql)) {

            int actualizados = 0;

            for (VariantePrecio vp : variantes) {
                if (vp.precioNuevo.compareTo(vp.precioActual) != 0) {
                    pst.setBigDecimal(1, vp.precioNuevo);
                    pst.setInt(2, vp.idVariante);
                    pst.addBatch();
                }
            }

            int[] resultados = pst.executeBatch();
            for (int resultado : resultados) {
                if (resultado > 0)
                    actualizados++;
            }

            if (actualizados > 0) {
                cambiosGuardados = true;
                JOptionPane.showMessageDialog(this,
                        String.format("Se actualizaron %d precios correctamente", actualizados),
                        "Guardado exitoso",
                        JOptionPane.INFORMATION_MESSAGE);

                // Actualizar precios actuales con los nuevos
                for (VariantePrecio vp : variantes) {
                    vp.precioActual = vp.precioNuevo;
                }

                // Recargar la tabla para quitar el resaltado
                for (int i = 0; i < variantes.size(); i++) {
                    modeloTabla.setValueAt(variantes.get(i).precioActual.doubleValue(), i, 5);
                    modeloTabla.setValueAt(variantes.get(i).precioNuevo.doubleValue(), i, 6);
                }
                tablaVariantes.repaint();

                dispose();
            } else {
                JOptionPane.showMessageDialog(this,
                        "No se pudo actualizar ningún precio",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error al guardar cambios: " + e.getMessage(),
                    "Error de base de datos",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Verifica si se guardaron cambios
     */
    public boolean isCambiosGuardados() {
        return cambiosGuardados;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MÉTODOS PARA CREAR ICONOS SVG
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Crea un icono de flecha para "Aplicar a Seleccionados"
     */
    private Icon crearIconoFlecha() {
        return new FlatSVGIcon("raven/icon/svg/menu_right.svg", 16, 16);
    }

    /**
     * Crea un icono global para "Aplicar a Todos"
     */
    private Icon crearIconoGlobal() {
        return new ImageIcon(new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB) {
            {
                Graphics2D g = createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(UIManager.getColor("Actions.Blue"));
                // Dibujar círculo
                g.drawOval(2, 2, 12, 12);
                // Dibujar líneas de latitud
                g.drawLine(2, 8, 14, 8);
                // Dibujar líneas de longitud
                g.drawArc(5, 2, 6, 12, 0, 180);
                g.drawArc(5, 2, 6, 12, 180, 180);
                g.dispose();
            }
        });
    }

    /**
     * Crea un icono de información
     */
    private Icon crearIconoInfo() {
        return new ImageIcon(new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB) {
            {
                Graphics2D g = createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color colorInfo = UIManager.getColor("Actions.Blue");
                if (colorInfo == null)
                    colorInfo = new Color(33, 150, 243);
                g.setColor(colorInfo);
                // Círculo
                g.fillOval(2, 2, 12, 12);
                g.setColor(Color.WHITE);
                // i
                g.fillRect(7, 5, 2, 2);
                g.fillRect(7, 8, 2, 6);
                g.dispose();
            }
        });
    }

    /**
     * Crea un icono de guardar (diskette)
     */
    private Icon crearIconoGuardar() {
        return new ImageIcon(new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB) {
            {
                Graphics2D g = createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color colorGuardar = UIManager.getColor("Actions.Green");
                if (colorGuardar == null)
                    colorGuardar = new Color(76, 175, 80);
                g.setColor(colorGuardar);
                // Rectángulo principal
                g.fillRect(2, 2, 12, 12);
                g.setColor(Color.WHITE);
                // Etiqueta superior
                g.fillRect(4, 3, 8, 4);
                // Línea de escritura
                g.fillRect(4, 10, 8, 2);
                g.dispose();
            }
        });
    }

    /**
     * Crea un icono de cancelar (X)
     */
    private Icon crearIconoCancelar() {
        return new ImageIcon(new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB) {
            {
                Graphics2D g = createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color colorCancelar = UIManager.getColor("Actions.Red");
                if (colorCancelar == null)
                    colorCancelar = new Color(244, 67, 54);
                g.setColor(colorCancelar);
                g.setStroke(new BasicStroke(2));
                // X
                g.drawLine(4, 4, 12, 12);
                g.drawLine(12, 4, 4, 12);
                g.dispose();
            }
        });
    }
}
