package raven.application.form.productos;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import java.awt.Font;
import javax.swing.UIManager;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.swing.FontIcon;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import raven.application.form.productos.creates.CreateInventario;
import raven.application.form.productos.creates.RealizarConteoForm;
import raven.componentes.CheckBoxTableHeaderRenderer;
import raven.controlador.inventario.InventarioController;
import raven.modal.component.SimpleModalBorder;
import raven.modal.listener.ModalController;
import raven.clases.admin.UserSession;
import raven.utils.ModalDialog;

public class InventarioForm extends javax.swing.JPanel {

    private InventarioController controller;
    private final FontIcon iconAjustar;
    private final FontIcon iconNuevo;
    private final FontIcon iconNext;
    private static final String Camposdetexto = "arc:15;background:lighten($Menu.background,25%)";

    public InventarioForm() {
        initComponents();
        lb.putClientProperty(FlatClientProperties.STYLE, "font:$h1.font");
        Color tabTextColor = UIManager.getColor("TabbedPane.foreground");
        iconNuevo = createColoredIcon(FontAwesomeSolid.EXTERNAL_LINK_ALT, tabTextColor);
        iconAjustar = createColoredIcon(FontAwesomeSolid.DOLLY_FLATBED, tabTextColor);
        iconNext = createColoredIcon(FontAwesomeSolid.ARROW_RIGHT, tabTextColor);
        btnNuevoInventarioCajas.setIcon(iconNuevo);
        btnNuevoInventarioPares.setIcon(iconNuevo);
        btnContinuarAjusteCaja.setIcon(iconNext);
        btnContinuarAjustePar.setIcon(iconNext);
        btnAjustPar.setIcon(iconAjustar);
        btnAjustCaja.setIcon(iconAjustar);

        // Inicializar controlador
        controller = new InventarioController();

        init();
        FlatRobotoFont.install();
        UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));

        // Cargar datos iniciales
        cargarDatosIniciales();

        // Configurar eventos de la tabla
        configurarEventosTabla();

    }

    private FontIcon createColoredIcon(Ikon icon, Color color) {
        FontIcon fontIcon = FontIcon.of(icon);
        fontIcon.setIconSize(18); // Tamaño del icono
        fontIcon.setIconColor(color);
        return fontIcon;
    }

    // Método de inicialización personalizado para componentes UI
    private void init() {
        configurarEstiloTabla();
        buscarInventarioNombre.putClientProperty(FlatClientProperties.STYLE, Camposdetexto);
        buscarInventarioNombrePares.putClientProperty(FlatClientProperties.STYLE, Camposdetexto);
        btnNuevoInventarioCajas.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:25;" // Radio de esquina de 25px
                + "background:#28CD41"); // Usa color de fondo de tabla
        btnNuevoInventarioPares.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:25;" // Radio de esquina de 25px
                + "background:#28CD41"); // Usa color de fondo de tabla
        btnAjustCaja.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:25;" // Radio de esquina de 25px
                + "background:#28CD41"); // Usa color de fondo de tabla
        btnAjustPar.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:25;" // Radio de esquina de 25px
                + "background:#28CD41"); // Usa color de fondo de tabla
        panelup.putClientProperty(FlatClientProperties.STYLE, "arc:25;background:$Table.gridColor;");
        panelup2.putClientProperty(FlatClientProperties.STYLE, "arc:25;background:$Table.gridColor;");
        // panel.putClientProperty(FlatClientProperties.STYLE,
        // "arc:25;background:$Login.background;");
        pestañaInventarioPares.putClientProperty(FlatClientProperties.STYLE,
                "tabAlignment:center;tabHeight:40;tabInsets:10,20,10,20;tabAreaAlignment:center;tabAreaInsets:15,0,0,0;hoverColor:$App.accent.blue;hoverForeground:#FFF;font:bold +10;");
        Color tabTextColor = UIManager.getColor("TabbedPane.foreground");
        FontIcon boxIcon = createColoredIcon(FontAwesomeSolid.BOX, tabTextColor);
        FontIcon shoesIcon = createColoredIcon(FontAwesomeSolid.SHOE_PRINTS, tabTextColor);
        pestañaInventarioPares.setIconAt(0, boxIcon);
        pestañaInventarioPares.setIconAt(1, shoesIcon);
        panelInventarioPares.putClientProperty(FlatClientProperties.STYLE, "arc:25;background:$Login.background;");
        inventarioCajasPanel.putClientProperty(FlatClientProperties.STYLE, "arc:25;background:$Login.background;");
        // Configurar campos de búsqueda
        buscarInventarioNombre.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar conteos...");
        // txtBuscarAjusteCaja.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
        // "Buscar ajustes...");
        buscarInventarioNombrePares.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar conteos...");
        // txtBuscarAjustePar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
        // "Buscar ajustes...");

        // Configurar renderers de tablas para mostrar botones y checkboxes
        configurarRenderersTablas();
    }

    /**
     * Carga datos iniciales en las tablas
     */
    private void cargarDatosIniciales() {
        // Cargar datos en un hilo separado para no bloquear la UI
        new Thread(() -> {
            try {
                // Cargar conteos activos
                controller.cargarConteosActivos(tablaConteosCajas, true);
                controller.cargarConteosActivos(TablaConteosPares, false);

                // Cargar ajustes pendientes
                controller.cargarAjustesPendientes(tablaAjustesCajas, true);
                controller.cargarAjustesPendientes(tablaAjustesPares, false);
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    Logger.getLogger(InventarioForm.class.getName()).log(Level.SEVERE, null, e);
                });
            }
        }).start();
    }

    private void configurarEstiloTabla() {
        // Estilo general de la tabla
        tablaConteosCajas.putClientProperty(FlatClientProperties.STYLE,
                "showHorizontalLines:true;"
                        + "showVerticalLines:false;"
                        + "rowHeight:40;"
                        + "intercellSpacing:10,5");
        // Estilo del encabezado de la tabla
        tablaConteosCajas.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "hoverBackground:$Table.background;"
                        + "height:40;"
                        + "separatorColor:$TableHeader.background;"
                        + "font:bold $h4.font");
        // Estilo general de la tabla
        tablaAjustesCajas.putClientProperty(FlatClientProperties.STYLE,
                "showHorizontalLines:true;"
                        + "showVerticalLines:false;"
                        + "rowHeight:40;"
                        + "intercellSpacing:10,5");

        // Estilo del encabezado de la tabla
        tablaAjustesCajas.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "hoverBackground:$Table.background;"
                        + "height:40;"
                        + "separatorColor:$TableHeader.background;"
                        + "font:bold $h4.font");
        // Estilo general de la tabla
        TablaConteosPares.putClientProperty(FlatClientProperties.STYLE,
                "showHorizontalLines:true;"
                        + "showVerticalLines:false;"
                        + "rowHeight:40;"
                        + "intercellSpacing:10,5");

        // Estilo del encabezado de la tabla
        TablaConteosPares.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "hoverBackground:$Table.background;"
                        + "height:40;"
                        + "separatorColor:$TableHeader.background;"
                        + "font:bold $h4.font");
        // Estilo general de la tabla
        tablaAjustesPares.putClientProperty(FlatClientProperties.STYLE,
                "showHorizontalLines:true;"
                        + "showVerticalLines:false;"
                        + "rowHeight:40;"
                        + "intercellSpacing:10,5");

        // Estilo del encabezado de la tabla
        tablaAjustesPares.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                "hoverBackground:$Table.background;"
                        + "height:40;"
                        + "separatorColor:$TableHeader.background;"
                        + "font:bold $h4.font");
    }

    /**
     * Configura los eventos de click en las tablas
     */
    private void configurarEventosTabla() {
        // Tabla conteos cajas
        tablaConteosCajas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int fila = tablaConteosCajas.rowAtPoint(e.getPoint());
                int columna = tablaConteosCajas.columnAtPoint(e.getPoint());

                if (columna == 7) { // Columna de acciones
                    int idConteo = (int) tablaConteosCajas.getValueAt(fila, 0);
                    abrirDetalleConteo(idConteo, true);
                }
            }
        });
        // Tabla conteos pares
        TablaConteosPares.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int fila = TablaConteosPares.rowAtPoint(e.getPoint());
                int columna = TablaConteosPares.columnAtPoint(e.getPoint());

                if (columna == 7) { // Columna de acciones
                    int idConteo = (int) TablaConteosPares.getValueAt(fila, 0);
                    abrirDetalleConteo(idConteo, false);
                }
            }
        });
    }

    private void configurarSelectorRazonAjuste(JTable tabla) {
        try {
            // Definir las opciones que corresponden a las razones de ajuste en la tabla
            // ajustes_inventario
            String[] opcionesRazon = { "error_conteo", "perdida", "deterioro", "error_registro", "otra" };

            // Crear un ComboBox con las opciones
            JComboBox<String> comboBox = new JComboBox<>(opcionesRazon);
            // Aplicar el editor de celdas a la columna 6 (razon)
            tabla.getColumnModel().getColumn(6).setCellEditor(new DefaultCellEditor(comboBox));

            // Configurar el renderizador personalizado
            tabla.getColumnModel().getColumn(6).setCellRenderer(
                    new javax.swing.table.DefaultTableCellRenderer() {
                        @Override
                        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                                boolean isSelected, boolean hasFocus, int row, int column) {
                            // Llamar al renderizador por defecto para mantener los colores y estilos
                            java.awt.Component comp = super.getTableCellRendererComponent(
                                    table, value, isSelected, hasFocus, row, column);

                            return comp;
                        }
                    });

        } catch (Exception ex) {
            System.err.println("Error al configurar selector de razón: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Configura los renderers de las tablas para mostrar botones y checkboxes
     */
    private void configurarRenderersTablas() {
        // Configurar botones en tablas de conteos
        // Para columna "acciones" en tabla conteos cajas
        // Configurar renderizador para columna de diferencia en tablaAjustesCajas
        // (columna 4)
        tablaAjustesCajas.getColumnModel().getColumn(4).setCellRenderer(
                new javax.swing.table.DefaultTableCellRenderer() {
                    @Override
                    public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                            boolean isSelected, boolean hasFocus, int row, int column) {
                        // Llamar al renderizador por defecto para obtener el componente
                        java.awt.Component comp = super.getTableCellRendererComponent(
                                table, value, isSelected, hasFocus, row, column);

                        // Establecer color de texto basado en el valor
                        if (value instanceof Integer) {
                            int diferencia = (Integer) value;
                            if (diferencia < 0) {
                                comp.setForeground(new Color(255, 0, 0)); // Rojo
                            } else if (diferencia > 0) {
                                comp.setForeground(new Color(0, 128, 0)); // Verde
                            } else {
                                comp.setForeground(table.getForeground()); // Color por defecto
                            }
                        } else if (value instanceof String) {
                            try {
                                int diferencia = Integer.parseInt(value.toString());
                                if (diferencia < 0) {
                                    comp.setForeground(new Color(255, 0, 0)); // Rojo
                                } else if (diferencia > 0) {
                                    comp.setForeground(new Color(0, 128, 0)); // Verde
                                } else {
                                    comp.setForeground(table.getForeground()); // Color por defecto
                                }
                            } catch (NumberFormatException e) {
                                comp.setForeground(table.getForeground()); // Color por defecto si no es un número
                            }
                        } else {
                            comp.setForeground(table.getForeground()); // Color por defecto
                        }

                        // Centrar el texto
                        setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

                        return comp;
                    }
                });

        // Configurar renderizador para columna de diferencia en tablaAjustesPares
        // (columna 4)
        tablaAjustesPares.getColumnModel().getColumn(4).setCellRenderer(
                new javax.swing.table.DefaultTableCellRenderer() {
                    @Override
                    public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                            boolean isSelected, boolean hasFocus, int row, int column) {
                        // Llamar al renderizador por defecto para obtener el componente
                        java.awt.Component comp = super.getTableCellRendererComponent(
                                table, value, isSelected, hasFocus, row, column);

                        // Establecer color de texto basado en el valor
                        if (value instanceof Integer) {
                            int diferencia = (Integer) value;
                            if (diferencia < 0) {
                                comp.setForeground(new Color(255, 0, 0)); // Rojo
                            } else if (diferencia > 0) {
                                comp.setForeground(new Color(0, 128, 0)); // Verde
                            } else {
                                comp.setForeground(table.getForeground()); // Color por defecto
                            }
                        } else if (value instanceof String) {
                            try {
                                int diferencia = Integer.parseInt(value.toString());
                                if (diferencia < 0) {
                                    comp.setForeground(new Color(255, 0, 0)); // Rojo
                                } else if (diferencia > 0) {
                                    comp.setForeground(new Color(0, 128, 0)); // Verde
                                } else {
                                    comp.setForeground(table.getForeground()); // Color por defecto
                                }
                            } catch (NumberFormatException e) {
                                comp.setForeground(table.getForeground()); // Color por defecto si no es un número
                            }
                        }

                        // Centrar el texto
                        setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

                        return comp;
                    }
                });

        // Configurar renderizador de checkbox en encabezado para la columna de
        // "Seleccionar todos"
        // Asumiendo que la columna de selección es la columna 7
        try {
            int columnaSeleccion = 7; // Ajustar según el índice correcto
            tablaAjustesCajas.getColumnModel().getColumn(columnaSeleccion)
                    .setHeaderRenderer(new CheckBoxTableHeaderRenderer(tablaAjustesCajas, columnaSeleccion));
            tablaAjustesPares.getColumnModel().getColumn(columnaSeleccion)
                    .setHeaderRenderer(new CheckBoxTableHeaderRenderer(tablaAjustesPares, columnaSeleccion));

            // Configurar editor para la columna de "Razón"
            setUpRazonColumnEditor(tablaAjustesCajas);
            setUpRazonColumnEditor(tablaAjustesPares);
        } catch (Exception e) {
            // Ignorar si la columna no existe aún
        }
        tablaAjustesCajas.getColumnModel().getColumn(8).setMinWidth(0);
        tablaAjustesCajas.getColumnModel().getColumn(8).setMaxWidth(0);
        tablaAjustesCajas.getColumnModel().getColumn(8).setWidth(0);
        tablaAjustesPares.getColumnModel().getColumn(8).setMinWidth(0);
        tablaAjustesPares.getColumnModel().getColumn(8).setMaxWidth(0);
        tablaAjustesPares.getColumnModel().getColumn(8).setWidth(0);
    }

    private void setUpRazonColumnEditor(JTable table) {
        try {
            // El índice de la columna "razon" es el 6 según el modelo
            // "Codigo", "Producto", "Stock sistema", "Stock contado", "diferencia", "tipo
            // de ajuste", "razon", ...
            int razonColumnIndex = 6;

            JComboBox<String> comboBox = new JComboBox<>();
            comboBox.addItem("error_conteo");
            comboBox.addItem("perdida");
            comboBox.addItem("deterioro");
            comboBox.addItem("error_registro");
            comboBox.addItem("otra");

            table.getColumnModel().getColumn(razonColumnIndex).setCellEditor(new DefaultCellEditor(comboBox));
        } catch (Exception e) {
            System.err.println("Error al configurar editor de razón: " + e.getMessage());
        }
    }

    /**
     * Abre el diálogo de detalle de un conteo
     *
     * @param idConteo ID del conteo
     * @param esCajas  true si es conteo de cajas, false si es de pares
     */
    private void abrirDetalleConteo(int idConteo, boolean esCajas) {
        try {
            // Crear el formulario de realizar conteo
            RealizarConteoForm formConteo = new RealizarConteoForm(idConteo, esCajas);

            // Mostrar el formulario como un diálogo modal
            ModalDialog.showModal(formConteo,
                    "Realizar Conteo de " + (esCajas ? "Cajas" : "Pares"),
                    java.awt.Dialog.ModalityType.APPLICATION_MODAL,
                    () -> {
                        // Recargar los datos cuando se cierre el diálogo
                        cargarDatosIniciales();

                        // Verificar si el conteo sigue activo en segundo plano
                        new Thread(() -> {
                            boolean sigueActivo = false;
                            JTable tablaVerificar = esCajas ? tablaConteosCajas : TablaConteosPares;
                            DefaultTableModel model = (DefaultTableModel) tablaVerificar.getModel();

                            for (int i = 0; i < model.getRowCount(); i++) {
                                Object idObj = model.getValueAt(i, 0);
                                int idEnTabla = 0;
                                if (idObj instanceof Integer) {
                                    idEnTabla = (Integer) idObj;
                                } else {
                                    try {
                                        idEnTabla = Integer.parseInt(idObj.toString());
                                    } catch (Exception e) {
                                    }
                                }

                                if (idEnTabla == idConteo) {
                                    sigueActivo = true;
                                    break;
                                }
                            }

                            if (!sigueActivo) {
                                // Agendar cambio de pestaña en el EDT para asegurar que la UI se actualice
                                SwingUtilities.invokeLater(() -> {
                                    try {
                                        if (esCajas) {
                                            // Asegurar que estamos en el tab principal correcto (Cajas = 0)
                                            pestañaInventarioPares.setSelectedIndex(0);
                                            // Cambiar al tab de Ajustes
                                            deslizanteInventarioCajas.setSelectedIndex(1);
                                        } else {
                                            // Asegurar que estamos en el tab principal correcto (Pares = 1)
                                            pestañaInventarioPares.setSelectedIndex(1);
                                            // Cambiar al tab de Ajustes
                                            deslizanteInventarioCajas1.setSelectedIndex(1);
                                        }

                                        // Verificar si hay ajustes para mostrar
                                        JTable tablaAjustes = esCajas ? tablaAjustesCajas : tablaAjustesPares;
                                        if (tablaAjustes.getRowCount() == 0) {
                                            JOptionPane.showMessageDialog(InventarioForm.this,
                                                    "El conteo finalizó correctamente sin diferencias.\nNo se requieren ajustes.",
                                                    "Conteo Perfecto", JOptionPane.INFORMATION_MESSAGE);
                                        }
                                    } catch (Exception e) {
                                        Logger.getLogger(InventarioForm.class.getName()).log(Level.SEVERE, null, e);
                                    }
                                });
                            }
                        }).start();
                    });
        } catch (Exception e) {
            Logger.getLogger(InventarioForm.class.getName()).log(Level.SEVERE, null, e);
            JOptionPane.showMessageDialog(this,
                    "Error al abrir detalle del conteo: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Aplica los ajustes seleccionados en la tabla
     *
     * @param tabla   Tabla de ajustes
     * @param esCajas true si son ajustes de cajas, false si son de pares
     */
    /*
     * private void aplicarAjustes(JTable tabla, boolean esCajas) {
     * DefaultTableModel modelo = (DefaultTableModel) tabla.getModel();
     * java.util.List<Integer> ajustesSeleccionados = new java.util.ArrayList<>();
     * java.util.Map<Integer, String> razonesSeleccionadas = new
     * java.util.HashMap<>();
     * // Check if the ID column exists
     * int columnCount = tabla.getColumnCount();
     * boolean hasIdColumn = columnCount > 8;
     * 
     * // Recorrer la tabla para encontrar ajustes seleccionados
     * for (int i = 0; i < modelo.getRowCount(); i++) {
     * Object value = modelo.getValueAt(i, 7); // Get the value in the checkbox
     * column
     * boolean seleccionado = false;
     * 
     * // Handle different types of values
     * if (value instanceof Boolean) {
     * seleccionado = (Boolean) value;
     * } else if (value instanceof String) {
     * // Try to convert String to Boolean
     * seleccionado = Boolean.parseBoolean(value.toString());
     * }
     * 
     * if (seleccionado) {
     * // Get the ID of the adjustment
     * int idAjuste;
     * 
     * if (hasIdColumn) {
     * // If we have a hidden ID column, use it
     * Object idObject = tabla.getValueAt(i, 8);
     * 
     * if (idObject instanceof Integer) {
     * idAjuste = (Integer) idObject;
     * } else {
     * try {
     * idAjuste = Integer.parseInt(idObject.toString());
     * } catch (NumberFormatException e) {
     * System.err.println("Error al convertir ID de ajuste: " + idObject);
     * continue;
     * }
     * }
     * } else {
     * // If we don't have a hidden ID column, we need to retrieve the ID
     * differently
     * // Option 1: Use row index +1 as a fallback (CAUTION: only if IDs are
     * sequential and match row position)
     * // idAjuste = i + 1;
     * 
     * // Option 2: Use data from another column to identify the adjustment (better
     * approach)
     * // For example, if first column contains a unique code
     * String codigo = tabla.getValueAt(i, 1).toString();
     * 
     * // Now we need to find the adjustment ID based on this code
     * // This would require a method in your DAO or controller
     * try {
     * // This is a placeholder for your actual lookup logic
     * // You need to implement this method in your controller
     * idAjuste = controller.buscarIdAjustePorCodigo(codigo, esCajas);
     * } catch (Exception e) {
     * JOptionPane.showMessageDialog(this,
     * "Error al identificar el ajuste con código: " + codigo,
     * "Error", JOptionPane.ERROR_MESSAGE);
     * continue;
     * }
     * }
     * 
     * ajustesSeleccionados.add(idAjuste);
     * }
     * }
     * 
     * // Rest of the method remains the same
     * if (ajustesSeleccionados.isEmpty()) {
     * JOptionPane.showMessageDialog(this,
     * "Debe seleccionar al menos un ajuste para aplicar.",
     * "Validación", JOptionPane.WARNING_MESSAGE);
     * return;
     * }
     * 
     * // Confirmar la acción
     * int confirmar = JOptionPane.showConfirmDialog(this,
     * "¿Está seguro de aplicar los ajustes seleccionados?",
     * "Confirmar Ajustes", JOptionPane.YES_NO_OPTION);
     * 
     * if (confirmar == JOptionPane.YES_OPTION) {
     * // Obtener el usuario actual
     * int usuarioId = SesionUsuario.getInstance().getUsuarioActual().getId();
     * 
     * // Aplicar ajustes
     * boolean resultado = controller.aprobarAjustes(ajustesSeleccionados,
     * usuarioId);
     * 
     * if (resultado) {
     * JOptionPane.showMessageDialog(this,
     * "Ajustes aplicados correctamente.",
     * "Éxito", JOptionPane.INFORMATION_MESSAGE);
     * // Actualizar las tablas
     * controller.cargarAjustesPendientes(tablaAjustesCajas, true);
     * controller.cargarAjustesPendientes(tablaAjustesPares, false);
     * } else {
     * JOptionPane.showMessageDialog(this,
     * "Error al aplicar los ajustes.",
     * "Error", JOptionPane.ERROR_MESSAGE);
     * }
     * }
     * }
     */
    private void aplicarAjustes(JTable tabla, boolean esCajas) {
        DefaultTableModel modelo = (DefaultTableModel) tabla.getModel();
        java.util.List<Integer> ajustesSeleccionados = new java.util.ArrayList<>();
        java.util.Map<Integer, String> razonesSeleccionadas = new java.util.HashMap<>();

        // Recorrer la tabla para encontrar ajustes seleccionados
        for (int i = 0; i < modelo.getRowCount(); i++) {
            Object value = modelo.getValueAt(i, 7); // Valor en la columna de checkbox
            boolean seleccionado = false;

            // Manejar diferentes tipos de valores para el checkbox
            if (value instanceof Boolean) {
                seleccionado = (Boolean) value;
            } else if (value instanceof String) {
                seleccionado = Boolean.parseBoolean(value.toString());
            }

            if (seleccionado) {
                // Obtener el ID del ajuste (columna oculta)
                int idAjuste = 0;
                try {
                    Object idObj = tabla.getValueAt(i, 8); // Columna oculta con ID
                    if (idObj instanceof Integer) {
                        idAjuste = (Integer) idObj;
                    } else {
                        idAjuste = Integer.parseInt(idObj.toString());
                    }
                } catch (Exception e) {
                    System.err.println("Error al obtener ID de ajuste: " + e.getMessage());
                    continue;
                }

                // Obtener la razón seleccionada (columna 6)
                String razon = modelo.getValueAt(i, 6).toString();

                // Guardar ID y razón
                ajustesSeleccionados.add(idAjuste);
                razonesSeleccionadas.put(idAjuste, razon);
            }
        }

        if (ajustesSeleccionados.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Debe seleccionar al menos un ajuste para aplicar.",
                    "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Confirmar la acción
        int confirmar = JOptionPane.showConfirmDialog(this,
                "¿Está seguro de aplicar los ajustes seleccionados?",
                "Confirmar Ajustes", JOptionPane.YES_NO_OPTION);

        if (confirmar == JOptionPane.YES_OPTION) {
            // Obtener el usuario actual
            int usuarioId = UserSession.getInstance().getCurrentUser().getIdUsuario();

            // Aplicar ajustes con las razones seleccionadas
            boolean resultado = controller.aprobarAjustes(ajustesSeleccionados, razonesSeleccionadas, usuarioId);

            if (resultado) {
                JOptionPane.showMessageDialog(this,
                        "Ajustes aplicados correctamente.",
                        "Éxito", JOptionPane.INFORMATION_MESSAGE);

                // Actualizar las tablas
                controller.cargarAjustesPendientes(tablaAjustesCajas, true);
                controller.cargarAjustesPendientes(tablaAjustesPares, false);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Error al aplicar los ajustes.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Filtra la tabla de conteos según el término de búsqueda
     *
     * @param termino Término de búsqueda
     * @param tabla   Tabla a filtrar
     * @param esCajas true si es la tabla de cajas, false si es de pares
     */
    private void filtrarTablaConteos(String termino, JTable tabla, boolean esCajas) {
        if (termino.isEmpty()) {
            // Si el término está vacío, mostrar todos los conteos
            controller.cargarConteosActivos(tabla, esCajas);
        } else {
            try {
                // Filtrar conteos según el término
                controller.buscarConteosActivos(tabla, termino, esCajas);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error al filtrar conteos: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lb = new javax.swing.JLabel();
        pestañaInventarioPares = new javax.swing.JTabbedPane();
        inventarioCajasPanel = new javax.swing.JPanel();
        deslizanteInventarioCajas = new javax.swing.JTabbedPane();
        panelConteosActivosCajas = new javax.swing.JPanel();
        panelup = new javax.swing.JPanel();
        buscarInventarioNombre = new javax.swing.JTextField();
        btnNuevoInventarioCajas = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tablaConteosCajas = new javax.swing.JTable();
        btnContinuarAjusteCaja = new javax.swing.JButton();
        panelAjustesCajas = new javax.swing.JPanel();
        panelup1 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tablaAjustesCajas = new javax.swing.JTable();
        btnAjustCaja = new javax.swing.JButton();
        panelInventarioPares = new javax.swing.JPanel();
        deslizanteInventarioCajas1 = new javax.swing.JTabbedPane();
        panelConteosActivosCajas1 = new javax.swing.JPanel();
        panelup2 = new javax.swing.JPanel();
        buscarInventarioNombrePares = new javax.swing.JTextField();
        btnNuevoInventarioPares = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        TablaConteosPares = new javax.swing.JTable();
        btnContinuarAjustePar = new javax.swing.JButton();
        panelAjustesCajas1 = new javax.swing.JPanel();
        panelup3 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        tablaAjustesPares = new javax.swing.JTable();
        btnAjustPar = new javax.swing.JButton();

        lb.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lb.setText("GESTION DE INVENTARIO");

        pestañaInventarioPares.setToolTipText("");
        pestañaInventarioPares.setInheritsPopupMenu(true);
        pestañaInventarioPares.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                pestañaInventarioParesMouseClicked(evt);
            }
        });

        buscarInventarioNombre.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                buscarInventarioNombreKeyReleased(evt);
            }
        });

        btnNuevoInventarioCajas.setText("NUEVO CONTEO");
        btnNuevoInventarioCajas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNuevoInventarioCajasActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelupLayout = new javax.swing.GroupLayout(panelup);
        panelup.setLayout(panelupLayout);
        panelupLayout.setHorizontalGroup(
                panelupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelupLayout.createSequentialGroup()
                                .addGap(21, 21, 21)
                                .addComponent(buscarInventarioNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 196,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnNuevoInventarioCajas)
                                .addGap(54, 54, 54)));
        panelupLayout.setVerticalGroup(
                panelupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelupLayout.createSequentialGroup()
                                .addContainerGap(10, Short.MAX_VALUE)
                                .addGroup(panelupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(btnNuevoInventarioCajas, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(buscarInventarioNombre, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                36, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(10, 10, 10)));

        tablaConteosCajas.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {

                },
                new String[] {
                        "id", "Nombre", "fecha", "tipo de conteo", "responsable", "observaciones", "estado", "acciones"
                }) {
            boolean[] canEdit = new boolean[] {
                    false, false, false, true, false, false, true, true
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        tablaConteosCajas.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tablaConteosCajasMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(tablaConteosCajas);

        btnContinuarAjusteCaja.setText("Continuar con ajustes");
        btnContinuarAjusteCaja.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnContinuarAjusteCajaActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelConteosActivosCajasLayout = new javax.swing.GroupLayout(panelConteosActivosCajas);
        panelConteosActivosCajas.setLayout(panelConteosActivosCajasLayout);
        panelConteosActivosCajasLayout.setHorizontalGroup(
                panelConteosActivosCajasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(panelup, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(panelConteosActivosCajasLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelConteosActivosCajasLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 785,
                                                Short.MAX_VALUE)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                                panelConteosActivosCajasLayout.createSequentialGroup()
                                                        .addGap(0, 0, Short.MAX_VALUE)
                                                        .addComponent(btnContinuarAjusteCaja,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 172,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap()));
        panelConteosActivosCajasLayout.setVerticalGroup(
                panelConteosActivosCajasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelConteosActivosCajasLayout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(panelup, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(20, 20, 20)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 385,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(btnContinuarAjusteCaja, javax.swing.GroupLayout.PREFERRED_SIZE, 44,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 103, Short.MAX_VALUE)));

        deslizanteInventarioCajas.addTab("CONTEOS ACTIVOS", panelConteosActivosCajas);

        javax.swing.GroupLayout panelup1Layout = new javax.swing.GroupLayout(panelup1);
        panelup1.setLayout(panelup1Layout);
        panelup1Layout.setHorizontalGroup(
                panelup1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 797, Short.MAX_VALUE));
        panelup1Layout.setVerticalGroup(
                panelup1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 48, Short.MAX_VALUE));

        tablaAjustesCajas.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {

                },
                new String[] {
                        "Codigo", "Producto", "Stock sistema", "Stock contado", "diferencia", "tipo de ajuste", "razon",
                        "Aprobar", "id"
                }) {
            Class[] types = new Class[] {
                    java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class,
                    java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Boolean.class,
                    java.lang.Object.class
            };
            boolean[] canEdit = new boolean[] {
                    false, false, false, false, false, true, true, true, false
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        jScrollPane2.setViewportView(tablaAjustesCajas);

        btnAjustCaja.setText("Aplicar ajustes");
        btnAjustCaja.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAjustCajaActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelAjustesCajasLayout = new javax.swing.GroupLayout(panelAjustesCajas);
        panelAjustesCajas.setLayout(panelAjustesCajasLayout);
        panelAjustesCajasLayout.setHorizontalGroup(
                panelAjustesCajasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(panelup1, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(panelAjustesCajasLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelAjustesCajasLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 785,
                                                Short.MAX_VALUE)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                                panelAjustesCajasLayout.createSequentialGroup()
                                                        .addGap(0, 0, Short.MAX_VALUE)
                                                        .addComponent(btnAjustCaja,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 172,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap()));
        panelAjustesCajasLayout.setVerticalGroup(
                panelAjustesCajasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelAjustesCajasLayout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(panelup1, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 418,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(12, 12, 12)
                                .addComponent(btnAjustCaja, javax.swing.GroupLayout.PREFERRED_SIZE, 44,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 81, Short.MAX_VALUE)));

        deslizanteInventarioCajas.addTab("AJUSTES", panelAjustesCajas);

        javax.swing.GroupLayout inventarioCajasPanelLayout = new javax.swing.GroupLayout(inventarioCajasPanel);
        inventarioCajasPanel.setLayout(inventarioCajasPanelLayout);
        inventarioCajasPanelLayout.setHorizontalGroup(
                inventarioCajasPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(deslizanteInventarioCajas));
        inventarioCajasPanelLayout.setVerticalGroup(
                inventarioCajasPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(deslizanteInventarioCajas));

        pestañaInventarioPares.addTab("INVENTARIO CAJAS", inventarioCajasPanel);

        buscarInventarioNombrePares.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buscarInventarioNombreParesActionPerformed(evt);
            }
        });
        buscarInventarioNombrePares.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                buscarInventarioNombreParesKeyReleased(evt);
            }
        });

        btnNuevoInventarioPares.setText("NUEVO CONTEO");
        btnNuevoInventarioPares.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNuevoInventarioParesActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelup2Layout = new javax.swing.GroupLayout(panelup2);
        panelup2.setLayout(panelup2Layout);
        panelup2Layout.setHorizontalGroup(
                panelup2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelup2Layout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addComponent(buscarInventarioNombrePares, javax.swing.GroupLayout.PREFERRED_SIZE, 196,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnNuevoInventarioPares)
                                .addGap(54, 54, 54)));
        panelup2Layout.setVerticalGroup(
                panelup2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelup2Layout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addGroup(panelup2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(buscarInventarioNombrePares, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                36, Short.MAX_VALUE)
                                        .addComponent(btnNuevoInventarioPares, javax.swing.GroupLayout.DEFAULT_SIZE, 36,
                                                Short.MAX_VALUE))
                                .addGap(10, 10, 10)));

        TablaConteosPares.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {

                },
                new String[] {
                        "id", "Nombre", "fecha", "tipo de conteo", "responsable", "observaciones", "estado", "acciones"
                }) {
            boolean[] canEdit = new boolean[] {
                    false, false, false, true, false, false, true, true
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        TablaConteosPares.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                TablaConteosParesMouseClicked(evt);
            }
        });
        jScrollPane3.setViewportView(TablaConteosPares);

        btnContinuarAjustePar.setText("Continuar con ajustes");
        btnContinuarAjustePar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnContinuarAjusteParActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelConteosActivosCajas1Layout = new javax.swing.GroupLayout(
                panelConteosActivosCajas1);
        panelConteosActivosCajas1.setLayout(panelConteosActivosCajas1Layout);
        panelConteosActivosCajas1Layout.setHorizontalGroup(
                panelConteosActivosCajas1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(panelup2, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(panelConteosActivosCajas1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelConteosActivosCajas1Layout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 785,
                                                Short.MAX_VALUE)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                                panelConteosActivosCajas1Layout.createSequentialGroup()
                                                        .addGap(0, 0, Short.MAX_VALUE)
                                                        .addComponent(btnContinuarAjustePar,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 172,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap()));
        panelConteosActivosCajas1Layout.setVerticalGroup(
                panelConteosActivosCajas1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelConteosActivosCajas1Layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(panelup2, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 385,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(12, 12, 12)
                                .addComponent(btnContinuarAjustePar, javax.swing.GroupLayout.PREFERRED_SIZE, 44,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 105, Short.MAX_VALUE)));

        deslizanteInventarioCajas1.addTab("CONTEOS ACTIVOS", panelConteosActivosCajas1);

        javax.swing.GroupLayout panelup3Layout = new javax.swing.GroupLayout(panelup3);
        panelup3.setLayout(panelup3Layout);
        panelup3Layout.setHorizontalGroup(
                panelup3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 797, Short.MAX_VALUE));
        panelup3Layout.setVerticalGroup(
                panelup3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 48, Short.MAX_VALUE));

        tablaAjustesPares.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {

                },
                new String[] {
                        "Codigo", "Producto", "Stock sistema", "Stock contado", "diferencia", "tipo de ajuste", "razon",
                        "Aprobar", "id"
                }) {
            Class[] types = new Class[] {
                    java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class,
                    java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Boolean.class,
                    java.lang.Object.class
            };
            boolean[] canEdit = new boolean[] {
                    false, false, false, false, false, true, true, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        jScrollPane4.setViewportView(tablaAjustesPares);

        btnAjustPar.setText("Aplicar ajustes");
        btnAjustPar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAjustParActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelAjustesCajas1Layout = new javax.swing.GroupLayout(panelAjustesCajas1);
        panelAjustesCajas1.setLayout(panelAjustesCajas1Layout);
        panelAjustesCajas1Layout.setHorizontalGroup(
                panelAjustesCajas1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(panelup3, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(panelAjustesCajas1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelAjustesCajas1Layout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 785,
                                                Short.MAX_VALUE)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                                panelAjustesCajas1Layout.createSequentialGroup()
                                                        .addGap(0, 0, Short.MAX_VALUE)
                                                        .addComponent(btnAjustPar,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 172,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap()));
        panelAjustesCajas1Layout.setVerticalGroup(
                panelAjustesCajas1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelAjustesCajas1Layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(panelup3, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 418,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(12, 12, 12)
                                .addComponent(btnAjustPar, javax.swing.GroupLayout.PREFERRED_SIZE, 44,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 81, Short.MAX_VALUE)));

        deslizanteInventarioCajas1.addTab("AJUSTES", panelAjustesCajas1);

        javax.swing.GroupLayout panelInventarioParesLayout = new javax.swing.GroupLayout(panelInventarioPares);
        panelInventarioPares.setLayout(panelInventarioParesLayout);
        panelInventarioParesLayout.setHorizontalGroup(
                panelInventarioParesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(deslizanteInventarioCajas1));
        panelInventarioParesLayout.setVerticalGroup(
                panelInventarioParesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(deslizanteInventarioCajas1));

        pestañaInventarioPares.addTab("INVENTARIO PARES", panelInventarioPares);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(lb, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(pestañaInventarioPares))
                                .addContainerGap()));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lb)
                                .addGap(40, 40, 40)
                                .addComponent(pestañaInventarioPares)
                                .addContainerGap()));
    }// </editor-fold>//GEN-END:initComponents

    private void pestañaInventarioParesMouseClicked(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_pestañaInventarioParesMouseClicked
        if (pestañaInventarioPares.getSelectedComponent() == panelInventarioPares) {

            pestañaInventarioPares.putClientProperty(FlatClientProperties.STYLE, ""
                    + "tabAlignment:center;" // Centra horizontalmente
                    + "tabHeight:40;" // Altura de las pestañas
                    + "tabInsets:10,20,10,20;" // Padding interno (arriba,izquierda,abajo,derecha)
                    + "tabAreaAlignment:center;" // Alinea el área de pestañas al centro
                    + "tabAreaInsets:15,0,0,0;" // Margen del área de pestañas (arriba,izq,abajo,der)
                    + "hoverColor:$App.accent.blue;"
                    + "hoverForeground:#FFF;"
                    + "tabAlignment:center;" // Centra las pestañas
                    + "selectedBackground:#28CD41;"
                    + "selectedForeground:#FFF;"
                    + "font:bold +10;"); // Usa color de fondo de tabla

        } else {
            pestañaInventarioPares.putClientProperty(FlatClientProperties.STYLE, ""
                    + "tabAlignment:center;" // Centra horizontalmente
                    + "tabHeight:40;" // Altura de las pestañas
                    + "tabInsets:10,20,10,20;" // Padding interno (arriba,izquierda,abajo,derecha)
                    + "tabAreaAlignment:center;" // Alinea el área de pestañas al centro
                    + "tabAreaInsets:15,0,0,0;" // Margen del área de pestañas (arriba,izq,abajo,der)
                    + "hoverColor:$App.accent.blue;"
                    + "hoverForeground:#FFF;"
                    + "tabAlignment:center;" // Centra las pestañas
                    + "selectedBackground:#0A84FF;"
                    + "selectedForeground:#FFF;"
                    + "font:bold +10;"); // Usa color de fondo de tabla
        }
    }// GEN-LAST:event_pestañaInventarioParesMouseClicked

    private void btnContinuarAjusteCajaActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnContinuarAjusteCajaActionPerformed
        deslizanteInventarioCajas.setSelectedIndex(1);
    }// GEN-LAST:event_btnContinuarAjusteCajaActionPerformed

    private void btnContinuarAjusteParActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnContinuarAjusteParActionPerformed
        deslizanteInventarioCajas1.setSelectedIndex(1);
    }// GEN-LAST:event_btnContinuarAjusteParActionPerformed

    private void btnAjustCajaActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnAjustCajaActionPerformed
        aplicarAjustes(tablaAjustesCajas, true);
    }// GEN-LAST:event_btnAjustCajaActionPerformed

    private void btnAjustParActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnAjustParActionPerformed
        aplicarAjustes(tablaAjustesPares, false);
    }// GEN-LAST:event_btnAjustParActionPerformed

    private void tablaConteosCajasMouseClicked(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_tablaConteosCajasMouseClicked

    }// GEN-LAST:event_tablaConteosCajasMouseClicked

    // Método auxiliar para obtener el ID del detalle de conteo
    private void TablaConteosParesMouseClicked(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_TablaConteosParesMouseClicked

    }// GEN-LAST:event_TablaConteosParesMouseClicked

    private void btnNuevoInventarioCajasActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnNuevoInventarioCajasActionPerformed
        // Abrir formulario para crear nuevo conteo de cajas
        CreateInventario formCreate = new CreateInventario(true);
        formCreate.selectCajasButton(); // Asegurar que esté seleccionado conteo de cajas

        SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                new SimpleModalBorder.Option("Guardar", SimpleModalBorder.OK_OPTION), };

        raven.modal.ModalDialog.showModal(
                this,
                new SimpleModalBorder(formCreate, "Crear Nuevo Conteo de Cajas", options,
                        (ModalController mc, int i) -> {
                            if (i == SimpleModalBorder.OK_OPTION) {
                                try {
                                    // En vez de usar guardarConteo() que podría cerrar el modal,
                                    // llamar directamente a crearConteo() y solo cerrar si tiene éxito
                                    boolean success = formCreate.crearConteo();
                                    if (success) {
                                        // Solo cerrar el modal si la validación tuvo éxito
                                        mc.close();
                                        // Actualizar solo la tabla de CAJAS
                                        controller.cargarConteosActivos(tablaConteosCajas, true);
                                    }
                                    // Si la validación falló, mc.closeModal() no se llama,
                                    // así que el modal permanece abierto
                                } catch (Exception e) {
                                    Logger.getLogger(InventarioForm.class.getName()).log(Level.SEVERE, null, e);
                                }
                            } else if (i == SimpleModalBorder.CANCEL_OPTION) {
                                // Siempre cerrar el modal cuando se hace clic en Cancelar
                                mc.close();
                            } else if (i == SimpleModalBorder.OPENED) {
                                formCreate.selectCajasButton();
                            }
                        }));

    }// GEN-LAST:event_btnNuevoInventarioCajasActionPerformed

    private void buscarInventarioNombreParesKeyReleased(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_buscarInventarioNombreParesKeyReleased
        filtrarTablaConteos(buscarInventarioNombrePares.getText().trim(), TablaConteosPares, false);
    }// GEN-LAST:event_buscarInventarioNombreParesKeyReleased

    private void buscarInventarioNombreKeyReleased(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_buscarInventarioNombreKeyReleased
        filtrarTablaConteos(buscarInventarioNombre.getText().trim(), tablaConteosCajas, true);

    }// GEN-LAST:event_buscarInventarioNombreKeyReleased

    private void btnNuevoInventarioParesActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnNuevoInventarioParesActionPerformed

        CreateInventario formCreate = new CreateInventario(false);
        formCreate.selectParesButton();// Asegurar que esté seleccionado conteo de cajas
        SimpleModalBorder.Option[] options = new SimpleModalBorder.Option[] {
                new SimpleModalBorder.Option("Cancelar", SimpleModalBorder.CANCEL_OPTION),
                new SimpleModalBorder.Option("Guardar", SimpleModalBorder.OK_OPTION)
        };

        raven.modal.ModalDialog.showModal(
                this,
                new SimpleModalBorder(formCreate, "Crear Nuevo Conteo de Pares", options,
                        (ModalController mc, int i) -> {
                            if (i == SimpleModalBorder.OK_OPTION) {
                                try {
                                    // En vez de usar guardarConteo() que podría cerrar el modal,
                                    // llamar directamente a crearConteo() y solo cerrar si tiene éxito
                                    boolean success = formCreate.crearConteo();
                                    if (success) {
                                        // Solo cerrar el modal si la validación tuvo éxito
                                        mc.close();
                                        // Actualizar solo la tabla de PARES
                                        controller.cargarConteosActivos(TablaConteosPares, false);
                                    }
                                    // Si la validación falló, mc.closeModal() no se llama,
                                    // así que el modal permanece abierto
                                } catch (Exception e) {
                                    Logger.getLogger(InventarioForm.class.getName()).log(Level.SEVERE, null, e);
                                }
                            } else if (i == SimpleModalBorder.CANCEL_OPTION) {
                                // Siempre cerrar el modal cuando se hace clic en Cancelar
                                mc.close();
                            } else if (i == SimpleModalBorder.OPENED) {
                                formCreate.selectParesButton();
                            }
                        }));
    }// GEN-LAST:event_btnNuevoInventarioParesActionPerformed

    private void buscarInventarioNombreParesActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_buscarInventarioNombreParesActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_buscarInventarioNombreParesActionPerformed
     // Declara un método que devuelve una lista de objetos ModelProduct

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable TablaConteosPares;
    private javax.swing.JButton btnAjustCaja;
    private javax.swing.JButton btnAjustPar;
    private javax.swing.JButton btnContinuarAjusteCaja;
    private javax.swing.JButton btnContinuarAjustePar;
    private javax.swing.JButton btnNuevoInventarioCajas;
    private javax.swing.JButton btnNuevoInventarioPares;
    private javax.swing.JTextField buscarInventarioNombre;
    private javax.swing.JTextField buscarInventarioNombrePares;
    private javax.swing.JTabbedPane deslizanteInventarioCajas;
    private javax.swing.JTabbedPane deslizanteInventarioCajas1;
    private javax.swing.JPanel inventarioCajasPanel;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JLabel lb;
    private javax.swing.JPanel panelAjustesCajas;
    private javax.swing.JPanel panelAjustesCajas1;
    private javax.swing.JPanel panelConteosActivosCajas;
    private javax.swing.JPanel panelConteosActivosCajas1;
    private javax.swing.JPanel panelInventarioPares;
    private javax.swing.JPanel panelup;
    private javax.swing.JPanel panelup1;
    private javax.swing.JPanel panelup2;
    private javax.swing.JPanel panelup3;
    private javax.swing.JTabbedPane pestañaInventarioPares;
    private javax.swing.JTable tablaAjustesCajas;
    private javax.swing.JTable tablaAjustesPares;
    private javax.swing.JTable tablaConteosCajas;
    // End of variables declaration//GEN-END:variables

}
