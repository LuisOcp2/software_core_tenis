package raven.application.form.productos.creates;

import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import raven.controlador.principal.conexion;
import raven.clases.admin.UserSession;
import raven.modal.Toast;

/**
 * Diálogo para crear y editar configuraciones de tallas para conversión caja→pares
 *
 * @author CrisDEV
 */
public class ConfiguracionTallasDialog extends JDialog {

    private JTextField txtNombre;
    private JComboBox<String> cbxGenero;
    private JTextArea txtDescripcion;
    private JTable tablaTallas;
    private DefaultTableModel modeloTabla;
    private JLabel lblTotal;
    private JButton btnGuardar;
    private JButton btnCancelar;
    private JButton btnCargarTallas;

    private Integer idConfiguracion; // null = nueva configuración, != null = editar
    private boolean guardadoExitoso = false;
    private List<TallaConfig> tallasDisponibles;
    private boolean actualizandoTabla = false; // Bandera para evitar recursión

    // Clase interna para representar una talla con configuración
    private static class TallaConfig {
        int idTalla;
        String numeroTalla;
        String sistema;
        String genero;
        int cantidad;
        boolean seleccionada;

        public TallaConfig(int idTalla, String numeroTalla, String sistema, String genero) {
            this.idTalla = idTalla;
            this.numeroTalla = numeroTalla;
            this.sistema = sistema;
            this.genero = genero;
            this.cantidad = 0;
            this.seleccionada = false;
        }

        public String getDisplayName() {
            String genAbbr;
            switch (genero.toUpperCase()) {
                case "MUJER": genAbbr = "M"; break;
                case "HOMBRE": genAbbr = "H"; break;
                case "NIÑO": genAbbr = "N"; break;
                case "UNISEX": genAbbr = "U"; break;
                default: genAbbr = ""; break;
            }
            return numeroTalla + " " + sistema + (genAbbr.isEmpty() ? "" : (" " + genAbbr));
        }
    }

    /**
     * Constructor para nueva configuración
     */
    public ConfiguracionTallasDialog(Frame parent) {
        this(parent, null);
    }

    /**
     * Constructor para editar configuración existente
     */
    public ConfiguracionTallasDialog(Frame parent, Integer idConfiguracion) {
        super(parent, idConfiguracion == null ? "Nueva Configuración de Tallas" : "Editar Configuración", true);
        this.idConfiguracion = idConfiguracion;
        this.tallasDisponibles = new ArrayList<>();

        initComponents();
        configurarVentana();

        if (idConfiguracion != null) {
            cargarConfiguracion(idConfiguracion);
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // ===== PANEL SUPERIOR: Datos básicos =====
        JPanel panelSuperior = new JPanel(new GridBagLayout());
        panelSuperior.setBorder(BorderFactory.createTitledBorder("Información General"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Nombre de la configuración
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        panelSuperior.add(new JLabel("Nombre:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        txtNombre = new JTextField(30);
        panelSuperior.add(txtNombre, gbc);

        // Género
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        panelSuperior.add(new JLabel("Género:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        cbxGenero = new JComboBox<>(new String[]{"HOMBRE", "MUJER", "NIÑO", "UNISEX"});
        cbxGenero.addActionListener(e -> cargarTallasDisponibles());
        panelSuperior.add(cbxGenero, gbc);

        // Botón para cargar tallas
        gbc.gridx = 2; gbc.weightx = 0.0;
        btnCargarTallas = new JButton("Cargar Tallas");
        btnCargarTallas.addActionListener(e -> cargarTallasDisponibles());
        panelSuperior.add(btnCargarTallas, gbc);

        // Descripción
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        panelSuperior.add(new JLabel("Descripción:"), gbc);

        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        txtDescripcion = new JTextArea(3, 30);
        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);
        JScrollPane scrollDesc = new JScrollPane(txtDescripcion);
        panelSuperior.add(scrollDesc, gbc);

        add(panelSuperior, BorderLayout.NORTH);

        // ===== PANEL CENTRAL: Tabla de tallas =====
        JPanel panelCentral = new JPanel(new BorderLayout());
        panelCentral.setBorder(BorderFactory.createTitledBorder("Tallas y Cantidades"));

        modeloTabla = new DefaultTableModel(
            new String[]{"Seleccionar", "Talla", "Cantidad"}, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                switch (column) {
                    case 0: return Boolean.class;
                    case 1: return String.class;
                    case 2: return Integer.class;
                    default: return Object.class;
                }
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 || column == 2; // Solo selección y cantidad editables
            }
        };

        tablaTallas = new JTable(modeloTabla);
        tablaTallas.setRowHeight(30);
        tablaTallas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Centrar contenido
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tablaTallas.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        tablaTallas.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);

        // Listener para actualizar total (con protección contra recursión)
        modeloTabla.addTableModelListener(e -> {
            if (!actualizandoTabla) {
                actualizarTotal();
                actualizarSelecciones();
            }
        });

        JScrollPane scrollTabla = new JScrollPane(tablaTallas);
        panelCentral.add(scrollTabla, BorderLayout.CENTER);

        // Panel de total
        JPanel panelTotal = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelTotal.add(new JLabel("Total pares:"));
        lblTotal = new JLabel("0 / 24");
        lblTotal.setFont(new Font("Arial", Font.BOLD, 16));
        lblTotal.setForeground(Color.RED);
        panelTotal.add(lblTotal);
        panelCentral.add(panelTotal, BorderLayout.SOUTH);

        add(panelCentral, BorderLayout.CENTER);

        // ===== PANEL INFERIOR: Botones =====
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        btnGuardar = new JButton("Guardar");
        btnGuardar.setBackground(new Color(46, 204, 113));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setFocusPainted(false);
        btnGuardar.addActionListener(e -> guardarConfiguracion());

        btnCancelar = new JButton("Cancelar");
        btnCancelar.setBackground(new Color(231, 76, 60));
        btnCancelar.setForeground(Color.WHITE);
        btnCancelar.setFocusPainted(false);
        btnCancelar.addActionListener(e -> dispose());

        panelBotones.add(btnCancelar);
        panelBotones.add(btnGuardar);

        add(panelBotones, BorderLayout.SOUTH);
    }

    private void configurarVentana() {
        setSize(700, 600);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    /**
     * Carga las tallas disponibles según el género seleccionado
     */
    private void cargarTallasDisponibles() {
        String generoSeleccionado = (String) cbxGenero.getSelectedItem();
        if (generoSeleccionado == null) {
            return;
        }

        tallasDisponibles.clear();
        modeloTabla.setRowCount(0);

        // Determinar géneros permitidos
        List<String> generosPermitidos = new ArrayList<>();
        String genNorm = normalizarGenero(generoSeleccionado);
        switch (genNorm) {
            case "UNISEX":
                generosPermitidos.add("MUJER");
                generosPermitidos.add("HOMBRE");
                generosPermitidos.add("UNISEX");
                break;
            case "HOMBRE":
                generosPermitidos.add("HOMBRE");
                generosPermitidos.add("UNISEX");
                break;
            case "MUJER":
                generosPermitidos.add("MUJER");
                generosPermitidos.add("UNISEX");
                break;
            case "NIÑO":
                generosPermitidos.add("NIÑO");
                break;
            default:
                generosPermitidos.add(genNorm);
        }

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < generosPermitidos.size(); i++) {
            placeholders.append(i > 0 ? ", ?" : "?");
        }

        String sql = "SELECT id_talla, numero, sistema, genero " +
                     "FROM tallas " +
                     "WHERE activo = 1 AND TRIM(numero) <> '00' " +
                     "AND genero IN (" + placeholders + ") " +
                     "ORDER BY CAST(numero AS UNSIGNED) ASC";

        try (Connection con = conexion.getInstance().createConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            for (int i = 0; i < generosPermitidos.size(); i++) {
                pst.setString(i + 1, generosPermitidos.get(i));
            }

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    TallaConfig talla = new TallaConfig(
                        rs.getInt("id_talla"),
                        rs.getString("numero"),
                        rs.getString("sistema"),
                        rs.getString("genero")
                    );
                    tallasDisponibles.add(talla);

                    modeloTabla.addRow(new Object[]{
                        false, // Seleccionada
                        talla.getDisplayName(),
                        0 // Cantidad
                    });
                }
            }

            if (tallasDisponibles.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "No se encontraron tallas para el género seleccionado",
                    "Sin tallas",
                    JOptionPane.WARNING_MESSAGE);
            } else {
                System.out.println("SUCCESS  Cargadas " + tallasDisponibles.size() + " tallas para género: " + generoSeleccionado);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error al cargar tallas: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private String normalizarGenero(String genero) {
        if (genero == null) return "";
        String g = genero.trim().toUpperCase();
        if ("NINO".equals(g)) g = "NIÑO";
        if ("CABALLERO".equals(g)) g = "HOMBRE";
        if ("DAMAS".equals(g) || "DAMA".equals(g)) g = "MUJER";
        if ("VARON".equals(g)) g = "HOMBRE";
        if ("MUJERES".equals(g)) g = "MUJER";
        if ("NIÑA".equals(g)) g = "NIÑO";
        if ("H".equals(g)) g = "HOMBRE";
        if ("M".equals(g)) g = "MUJER";
        if ("N".equals(g)) g = "NIÑO";
        if ("U".equals(g)) g = "UNISEX";
        return g;
    }

    /**
     * Actualiza el total de pares y cambia el color según el estado
     */
    private void actualizarTotal() {
        int total = 0;
        for (int i = 0; i < modeloTabla.getRowCount(); i++) {
            Object valor = modeloTabla.getValueAt(i, 2);
            if (valor != null) {
                try {
                    total += Integer.parseInt(valor.toString());
                } catch (NumberFormatException ignore) {}
            }
        }

        lblTotal.setText(total + " / 24");

        if (total == 24) {
            lblTotal.setForeground(new Color(46, 204, 113)); // Verde
        } else if (total > 24) {
            lblTotal.setForeground(Color.RED); // Rojo
        } else {
            lblTotal.setForeground(Color.ORANGE); // Naranja
        }
    }

    /**
     * Auto-marca el checkbox cuando la cantidad > 0
     */
    private void actualizarSelecciones() {
        actualizandoTabla = true; // Activar bandera para evitar recursión
        try {
            for (int i = 0; i < modeloTabla.getRowCount(); i++) {
                Object cantidad = modeloTabla.getValueAt(i, 2);
                int cant = 0;
                try {
                    cant = Integer.parseInt(cantidad.toString());
                } catch (Exception ignore) {}

                modeloTabla.setValueAt(cant > 0, i, 0);

                if (i < tallasDisponibles.size()) {
                    tallasDisponibles.get(i).cantidad = cant;
                    tallasDisponibles.get(i).seleccionada = cant > 0;
                }
            }
        } finally {
            actualizandoTabla = false; // Desactivar bandera siempre
        }
    }

    /**
     * Guarda la configuración en la base de datos
     */
    private void guardarConfiguracion() {
        // Validaciones
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Debe ingresar un nombre para la configuración",
                "Validación",
                JOptionPane.WARNING_MESSAGE);
            txtNombre.requestFocus();
            return;
        }

        // Calcular total
        int total = 0;
        List<TallaConfig> tallasSeleccionadas = new ArrayList<>();
        for (int i = 0; i < modeloTabla.getRowCount(); i++) {
            Boolean seleccionada = (Boolean) modeloTabla.getValueAt(i, 0);
            if (seleccionada != null && seleccionada) {
                Object cantidad = modeloTabla.getValueAt(i, 2);
                int cant = 0;
                try {
                    cant = Integer.parseInt(cantidad.toString());
                } catch (Exception ignore) {}

                if (cant > 0 && i < tallasDisponibles.size()) {
                    total += cant;
                    TallaConfig tc = tallasDisponibles.get(i);
                    tc.cantidad = cant;
                    tc.seleccionada = true;
                    tallasSeleccionadas.add(tc);
                }
            }
        }

        if (total != 24) {
            JOptionPane.showMessageDialog(this,
                "El total de pares debe ser exactamente 24.\nTotal actual: " + total,
                "Validación",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (tallasSeleccionadas.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Debe seleccionar al menos una talla",
                "Validación",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Guardar en base de datos
        try (Connection con = conexion.getInstance().createConnection()) {
            con.setAutoCommit(false);

            try {
                int idConfig;
                Integer idUsuario = null;
                if (UserSession.getInstance().getCurrentUser() != null) {
                    idUsuario = UserSession.getInstance().getCurrentUser().getIdUsuario();
                }
                String genero = (String) cbxGenero.getSelectedItem();
                String descripcion = txtDescripcion.getText().trim();

                if (idConfiguracion == null) {
                    // INSERT - Nueva configuración
                    String sqlInsert = "INSERT INTO configuraciones_tallas " +
                                      "(nombre_configuracion, genero, descripcion, total_pares, id_usuario_creador) " +
                                      "VALUES (?, ?, ?, 24, ?)";

                    try (PreparedStatement pst = con.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                        pst.setString(1, nombre);
                        pst.setString(2, genero);
                        pst.setString(3, descripcion.isEmpty() ? null : descripcion);
                        pst.setObject(4, idUsuario);
                        pst.executeUpdate();

                        try (ResultSet rs = pst.getGeneratedKeys()) {
                            if (rs.next()) {
                                idConfig = rs.getInt(1);
                            } else {
                                throw new SQLException("No se pudo obtener el ID de la configuración");
                            }
                        }
                    }
                } else {
                    // UPDATE - Editar configuración existente
                    String sqlUpdate = "UPDATE configuraciones_tallas " +
                                      "SET nombre_configuracion = ?, genero = ?, descripcion = ? " +
                                      "WHERE id_configuracion = ?";

                    try (PreparedStatement pst = con.prepareStatement(sqlUpdate)) {
                        pst.setString(1, nombre);
                        pst.setString(2, genero);
                        pst.setString(3, descripcion.isEmpty() ? null : descripcion);
                        pst.setInt(4, idConfiguracion);
                        pst.executeUpdate();
                    }

                    // Eliminar detalles anteriores
                    String sqlDelete = "DELETE FROM configuraciones_tallas_detalle WHERE id_configuracion = ?";
                    try (PreparedStatement pst = con.prepareStatement(sqlDelete)) {
                        pst.setInt(1, idConfiguracion);
                        pst.executeUpdate();
                    }

                    idConfig = idConfiguracion;
                }

                // Insertar detalles de tallas
                String sqlDetalle = "INSERT INTO configuraciones_tallas_detalle " +
                                   "(id_configuracion, id_talla, cantidad_pares, orden) " +
                                   "VALUES (?, ?, ?, ?)";

                try (PreparedStatement pst = con.prepareStatement(sqlDetalle)) {
                    int orden = 1;
                    for (TallaConfig tc : tallasSeleccionadas) {
                        pst.setInt(1, idConfig);
                        pst.setInt(2, tc.idTalla);
                        pst.setInt(3, tc.cantidad);
                        pst.setInt(4, orden++);
                        pst.addBatch();
                    }
                    pst.executeBatch();
                }

                con.commit();
                guardadoExitoso = true;

                JOptionPane.showMessageDialog(this,
                    "Configuración guardada exitosamente",
                    "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);

                dispose();

            } catch (SQLException e) {
                con.rollback();
                throw e;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error al guardar la configuración: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Carga una configuración existente para editarla
     */
    private void cargarConfiguracion(int idConfig) {
        try (Connection con = conexion.getInstance().createConnection()) {
            // Cargar datos principales
            String sql = "SELECT nombre_configuracion, genero, descripcion " +
                        "FROM configuraciones_tallas WHERE id_configuracion = ?";

            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, idConfig);

                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        txtNombre.setText(rs.getString("nombre_configuracion"));
                        cbxGenero.setSelectedItem(rs.getString("genero"));
                        txtDescripcion.setText(rs.getString("descripcion"));
                    }
                }
            }

            // Cargar tallas disponibles
            cargarTallasDisponibles();

            // Cargar detalles (cantidades)
            String sqlDetalle = "SELECT id_talla, cantidad_pares " +
                               "FROM configuraciones_tallas_detalle " +
                               "WHERE id_configuracion = ? " +
                               "ORDER BY orden ASC";

            try (PreparedStatement pst = con.prepareStatement(sqlDetalle)) {
                pst.setInt(1, idConfig);

                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        int idTalla = rs.getInt("id_talla");
                        int cantidad = rs.getInt("cantidad_pares");

                        // Buscar la talla en la lista y actualizar
                        for (int i = 0; i < tallasDisponibles.size(); i++) {
                            if (tallasDisponibles.get(i).idTalla == idTalla) {
                                modeloTabla.setValueAt(true, i, 0);
                                modeloTabla.setValueAt(cantidad, i, 2);
                                tallasDisponibles.get(i).cantidad = cantidad;
                                tallasDisponibles.get(i).seleccionada = true;
                                break;
                            }
                        }
                    }
                }
            }

            actualizarTotal();

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error al cargar la configuración: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isGuardadoExitoso() {
        return guardadoExitoso;
    }

    /**
     * Muestra el diálogo para crear una nueva configuración
     */
    public static boolean mostrarDialogoNuevo(Frame parent) {
        ConfiguracionTallasDialog dialog = new ConfiguracionTallasDialog(parent);
        dialog.setVisible(true);
        return dialog.isGuardadoExitoso();
    }

    /**
     * Muestra el diálogo para editar una configuración existente
     */
    public static boolean mostrarDialogoEditar(Frame parent, int idConfiguracion) {
        ConfiguracionTallasDialog dialog = new ConfiguracionTallasDialog(parent, idConfiguracion);
        dialog.setVisible(true);
        return dialog.isGuardadoExitoso();
    }
}

