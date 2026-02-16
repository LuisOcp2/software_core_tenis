package raven.application.form.comercial;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import raven.application.form.comercial.devolucion.DialogoAutorizacion;
import raven.application.form.comercial.devolucion.nuevaDevolucion123;
import raven.clases.comercial.ServiceDevolucion;
import raven.controlador.comercial.ModelDevolucion;

// AGREGAR ESTAS IMPORTACIONES AL INICIO DEL ARCHIVO:ontrollerDevolucion;
import net.miginfocom.swing.MigLayout;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import java.awt.Color;

public class devolucionMainForm extends javax.swing.JPanel {

        private static final String PANEL_STYLE = "arc:25;background:$Login.background;";

        // Campo para botón refrescar
        private javax.swing.JButton btnRefrescar;

        // Servicios MVC
        private ServiceDevolucion serviceDevolucion;
        private DefaultTableModel modeloTabla;
        private DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        private boolean inicializadoMVC = false;

        public devolucionMainForm() {
                initComponents();
                inicializarServicios();
                configurarUI();
                configurarTabla();
                inicializarMVC();
                cargarDatosIniciales();
                btnAutorizar.putClientProperty(FlatClientProperties.STYLE,
                                "arc:20;background:$App.accent.default;");
                panelGenReport.setVisible(false);
        }

        /**
         * Configuración completa de la interfaz de usuario
         */
        public void configurarUI() {
                try {
                        FlatRobotoFont.install();
                        UIManager.put("defaultFont", new Font(FlatRobotoFont.FAMILY, Font.PLAIN, 13));
                        aplicarEstiloPaneles();
                        reconfigurarPanelFiltros();

                        // NUEVO: Reconfigurar layout principal y estilizar alertas
                        reconfigurarLayoutPrincipal();
                        estilizarAlertas();
                        estilizarAccionesRapidas();

                        System.out.println("SUCCESS  UI configurada correctamente");
                } catch (Exception e) {
                        System.err.println("ERROR  Error configurando UI: " + e.getMessage());
                }
        }

        /**
         * Aplica estilos al panel de acciones rápidas
         */
        private void estilizarAccionesRapidas() {
                try {
                        panelAcR.setLayout(new MigLayout("insets 15, fillx, gap 10", "[grow]", "[]10[]10[]"));
                        panelAcR.removeAll();
                        panelAcR.putClientProperty(FlatClientProperties.STYLE, "arc:20;background:$Panel.background;");

                        // Título
                        jLabel14.setText("Acciones Rápidas");
                        jLabel14.putClientProperty(FlatClientProperties.STYLE,
                                        "font: bold 16;foreground:$Text.color;");
                        panelAcR.add(jLabel14, "cell 0 0, growx");

                        // Botones de acción (Reconfigurados como tarjetas pequeñas)
                        configurarBotonAccion(paneNuevo1, jButton3, jLabel15, jLabel16,
                                        "Nueva Devolución", "Crear solicitud", FontAwesomeSolid.PLUS, "#E8F5E9",
                                        "#2E7D32");

                        configurarBotonAccion(panelGenReport, jButton4, jLabel17, jLabel18,
                                        "Generar Reporte", "Análisis de periodo", FontAwesomeSolid.FILE_ALT, "#F3E5F5",
                                        "#7B1FA2");

                        panelAcR.add(paneNuevo1, "cell 0 1, growx");
                        panelAcR.add(panelGenReport, "cell 0 2, growx");

                } catch (Exception e) {
                        System.err.println("ERROR Estilizando acciones rápidas: " + e.getMessage());
                }
        }

        private void configurarBotonAccion(javax.swing.JPanel panel, javax.swing.JButton btn,
                        javax.swing.JLabel titulo, javax.swing.JLabel desc,
                        String textoTitulo, String textoDesc, org.kordamp.ikonli.Ikon iconCode,
                        String bgColor, String accentColor) {

                panel.setLayout(new MigLayout("insets 10, fill", "[50!][grow]", "[][]"));
                panel.putClientProperty(FlatClientProperties.STYLE, "arc:15;background:" + bgColor + ";");
                panel.removeAll();

                // Botón (Icono)
                btn.setText("");
                btn.setIcon(FontIcon.of(iconCode, 20, Color.WHITE));
                btn.putClientProperty(FlatClientProperties.STYLE,
                                "arc:10;background:" + accentColor + ";borderWidth:0;focusWidth:0;");
                panel.add(btn, "cell 0 0 1 2, grow, w 40!, h 40!");

                // Textos
                titulo.setText(textoTitulo);
                titulo.putClientProperty(FlatClientProperties.STYLE, "font: bold 13;foreground:" + accentColor);

                desc.setText(textoDesc);
                desc.putClientProperty(FlatClientProperties.STYLE,
                                "font: 11;foreground:darken(" + accentColor + ", 10%)");

                panel.add(titulo, "cell 1 0, growx, aligny bottom");
                panel.add(desc, "cell 1 1, growx, aligny top");
        }

        /**
         * Reorganiza el layout principal usando MigLayout para mejor control de
         * proporciones
         */
        private void reconfigurarLayoutPrincipal() {
                try {
                        panelMain.removeAll();
                        // Sidebar aumentado a 350px para mejor proporción de alertas y acciones
                        panelMain.setLayout(new MigLayout("fill, insets 20, gap 15", "[grow][350!]", "[][][grow]"));

                        // Fila 1: Título
                        panelMain.add(panelTitulo, "cell 0 0, span, growx, h 60!");

                        // Fila 2: Estadísticas
                        panelMain.add(panelSt, "cell 0 1, span, growx");

                        // Fila 3, Columna 1 (Izquierda): Filtros y Tabla
                        // Usamos un panel contenedor para filtros y tabla para asegurar que fluyan bien
                        javax.swing.JPanel leftPanel = new javax.swing.JPanel(
                                        new MigLayout("insets 0, fill, gap 10", "[grow]", "[][grow]"));
                        leftPanel.setOpaque(false);
                        leftPanel.add(panelFilt, "cell 0 0, growx");
                        leftPanel.add(panelDevo, "cell 0 1, grow");

                        panelMain.add(leftPanel, "cell 0 2, grow");

                        // Fila 3, Columna 2 (Derecha): Acciones Rápidas y Alertas
                        // Reducimos el ancho y apilamos verticalmente
                        javax.swing.JPanel rightPanel = new javax.swing.JPanel(
                                        new MigLayout("insets 0, fill, gap 15", "[grow]", "[][grow]"));
                        rightPanel.setOpaque(false);
                        rightPanel.add(panelAcR, "cell 0 0, growx"); // Acciones rápidas
                        rightPanel.add(panelAlerts, "cell 0 1, grow"); // Alertas

                        panelMain.add(rightPanel, "cell 1 2, growy, w 350!"); // Ancho fijo aumentado

                        panelMain.revalidate();
                        panelMain.repaint();

                } catch (Exception e) {
                        System.err.println("ERROR Reconfigurando layout principal: " + e.getMessage());
                }
        }

        /**
         * Aplica estilos modernos a los paneles de alerta
         */
        private void estilizarAlertas() {
                try {
                        // Configurar layout de panelAlerts para asegurar que los hijos se expandan
                        panelAlerts.setLayout(new MigLayout("fillx, insets 10, gap 10", "[grow]", "[]"));
                        panelAlerts.removeAll(); // Vamos a readjuntar los paneles

                        // 1. Autorización Pendiente (Azul/Info)
                        configurarPanelAlerta(panelApendi, jLabel20, txtAutoPendientes,
                                        "Autorización Pendiente", FontAwesomeSolid.USER_CLOCK, "#E3F2FD", "#1565C0");
                        panelAlerts.add(panelApendi, "growx, wrap");

                        // 2. Notas de Crédito (Amarillo/Warning)
                        configurarPanelAlerta(panelAnotsFv, jLabel22, txtFechaNotas,
                                        "Notas de Crédito", FontAwesomeSolid.FILE_INVOICE_DOLLAR, "#FFF8E1", "#F57F17");
                        panelAlerts.add(panelAnotsFv, "growx, wrap");

                        // 3. Stock Alto (Rojo/Danger)
                        configurarPanelAlerta(panelStAl, jLabel25, txtStockAltoDevolucion,
                                        "Stock Alto", FontAwesomeSolid.EXCLAMATION_TRIANGLE, "#FFEBEE", "#C62828");
                        panelAlerts.add(panelStAl, "growx, wrap");

                } catch (Exception e) {
                        System.err.println("ERROR Estilizando alertas: " + e.getMessage());
                }
        }

        private void configurarPanelAlerta(javax.swing.JPanel panel, javax.swing.JLabel titulo,
                        javax.swing.JLabel contenido,
                        String textoTitulo, org.kordamp.ikonli.Ikon icono, String bgColor, String accentColor) {

                panel.setLayout(new MigLayout("insets 10, fill", "[][grow]", "[]0[]"));
                panel.putClientProperty(FlatClientProperties.STYLE, "arc:15;background:" + bgColor);
                panel.removeAll();

                // Icono
                FontIcon icon = FontIcon.of(icono, 24, Color.decode(accentColor));
                javax.swing.JLabel iconLabel = new javax.swing.JLabel(icon);
                panel.add(iconLabel, "cell 0 0 1 2, aligny top, gapright 10");

                // Título
                titulo.setText(textoTitulo);
                titulo.putClientProperty(FlatClientProperties.STYLE, "font: bold 13;foreground:" + accentColor);
                panel.add(titulo, "cell 1 0, growx");

                // Contenido
                contenido.putClientProperty(FlatClientProperties.STYLE, "font: 12;foreground:" + accentColor);
                panel.add(contenido, "cell 1 1, growx");
        }

        /**
         * Reconfigura el panel de filtros para usar MigLayout y agregar botón refrescar
         */
        private void reconfigurarPanelFiltros() {
                try {
                        if (panelFilt == null)
                                return;

                        // Actualizar textos para reflejar nueva funcionalidad
                        if (jLabel10 != null)
                                jLabel10.setText("Buscar (N°, Cliente, DNI):");
                        if (txtNumero != null)
                                txtNumero.setToolTipText("Ingrese número de devolución, nombre de cliente o DNI");

                        // Limpiar panel existente
                        panelFilt.removeAll();

                        // Establecer MigLayout
                        panelFilt.setLayout(new MigLayout("insets 10, fillx, gap 10", "[grow][grow][grow][150!][50!]",
                                        "[][bottom]"));

                        // Etiquetas
                        panelFilt.add(jLabel10, "cell 0 0");
                        panelFilt.add(jLabel11, "cell 1 0");
                        panelFilt.add(jLabel12, "cell 2 0");

                        // Componentes
                        panelFilt.add(txtNumero, "cell 0 1, growx, h 40!");
                        panelFilt.add(cbxEstado, "cell 1 1, growx, h 40!");
                        panelFilt.add(cbxRango, "cell 2 1, growx, h 40!");
                        panelFilt.add(btnFiltrar, "cell 3 1, growx, h 40!");

                        // Botón Refrescar
                        btnRefrescar = new javax.swing.JButton();
                        FontIcon iconRefresh = FontIcon.of(FontAwesomeSolid.SYNC_ALT, 20, Color.WHITE);
                        btnRefrescar.setIcon(iconRefresh);
                        // Usar un color fijo o variable estándar de FlatLaf para evitar errores
                        btnRefrescar.putClientProperty(FlatClientProperties.STYLE,
                                        "arc:20;background:#007BFF;foreground:#FFFFFF;");
                        btnRefrescar.setToolTipText("Refrescar datos");
                        btnRefrescar.addActionListener(e -> actualizarDatos());

                        panelFilt.add(btnRefrescar, "cell 4 1, growx, h 40!");

                        panelFilt.revalidate();
                        panelFilt.repaint();

                } catch (Exception e) {
                        System.err.println("ERROR Reconfigurando panel filtros: " + e.getMessage());
                }
        }

        /**
         * Carga todos los datos iniciales del dashboard
         */
        private void cargarDatosIniciales() {
                try {
                        cargarEstadisticas();
                        cargarDevolucionesRecientes();
                        cargarAlertas();
                        System.out.println("SUCCESS  Datos iniciales cargados correctamente");
                } catch (Exception e) {
                        System.err.println("ERROR  Error cargando datos iniciales: " + e.getMessage());
                        mostrarError("Error cargando datos del dashboard: " + e.getMessage());
                }
        }

        // ====================================================================
        // INICIALIZACIÓN MVC - MÉTODO AGREGADO
        // ====================================================================
        /**
         * Inicializa los servicios necesarios con manejo de errores
         */
        private void inicializarServicios() {
                try {
                        this.serviceDevolucion = new ServiceDevolucion();
                        System.out.println("SUCCESS  ServiceDevolucion inicializado correctamente");
                } catch (Exception e) {
                        System.err.println("ERROR  Error inicializando ServiceDevolucion: " + e.getMessage());
                        e.printStackTrace();
                        mostrarError("Error inicializando servicios de devolución: " + e.getMessage());
                }
        }

        /**
         * Inicialización del patrón MVC
         */
        private void inicializarMVC() {
                if (inicializadoMVC) {
                        return;
                }

                try {
                        configurarComboBoxes();
                        configurarEventosMVC();
                        configurarMenuContextual();
                        inicializadoMVC = true;
                        System.out.println("SUCCESS  MVC inicializado correctamente");

                } catch (Exception e) {
                        System.err.println("ERROR  Error inicializando MVC: " + e.getMessage());
                        mostrarError("Error inicializando sistema: " + e.getMessage());
                }
        }

        /**
         * Configura los eventos del patrón MVC
         */
        private void configurarEventosMVC() {
                try {
                        // Evento para el botón de búsqueda
                        if (btnFiltrar != null) {
                                btnFiltrar.addActionListener(e -> buscarDevoluciones());
                        }

                        // Agregar botón de exportar o refrescar si existe en la UI, si no, simularlo
                        // TODO: Si el usuario quiere nuevas funciones visuales, deberíamos agregar
                        // botones en initComponents

                        // Agregar atajos de teclado o acciones globales
                        // Ejemplo: F5 para refrescar
                        this.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                                        javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0),
                                        "refrescar");
                        this.getActionMap().put("refrescar", new javax.swing.AbstractAction() {
                                @Override
                                public void actionPerformed(java.awt.event.ActionEvent e) {
                                        actualizarDatos();
                                        System.out.println("Acción F5: Datos actualizados");
                                }
                        });

                        // Evento para botón nueva devolución (ya existe en initComponents)
                        System.out.println("SUCCESS  Eventos MVC configurados");
                } catch (Exception e) {
                        System.err.println("ERROR  Error configurando eventos: " + e.getMessage());
                }
        }

        /**
         * MÉTODO AGREGADO: Cargar datos iniciales
         */
        private void cargarDatos() {
                cargarEstadisticas();
                cargarDevolucionesRecientes();
                cargarAlertas();
        }

        /**
         * MÉTODO CORREGIDO: Carga estadísticas con validación de servicio
         */
        private void cargarEstadisticas() {
                try {
                        if (serviceDevolucion == null) {
                                System.err.println(
                                                "WARNING  ServiceDevolucion no inicializado para cargar estadísticas");
                                establecerEstadisticasPorDefecto();
                                return;
                        }

                        ServiceDevolucion.EstadisticasDevolucion stats = serviceDevolucion
                                        .obtenerEstadisticasDevolucion();

                        // Actualizar labels de estadísticas con validación
                        actualizarLabelSeguro(jLabel2, String.valueOf(stats.getPendientes()));
                        actualizarLabelSeguro(jLabel4, String.valueOf(stats.getProcesando()));
                        actualizarLabelSeguro(jLabel6, String.valueOf(stats.getAprobadasHoy()));
                        actualizarLabelSeguro(jLabel8, String.valueOf(stats.getRechazadas()));

                        System.out.printf(
                                        "SUCCESS  Estadísticas cargadas: %d pendientes, %d procesando, %d aprobadas hoy, %d rechazadas%n",
                                        stats.getPendientes(), stats.getProcesando(), stats.getAprobadasHoy(),
                                        stats.getRechazadas());

                } catch (SQLException e) {
                        System.err.println("ERROR  Error SQL cargando estadísticas: " + e.getMessage());
                        establecerEstadisticasPorDefecto();
                } catch (Exception e) {
                        System.err.println("ERROR  Error general cargando estadísticas: " + e.getMessage());
                        establecerEstadisticasPorDefecto();
                }
        }

        /**
         * Actualiza un label de forma segura
         */
        private void actualizarLabelSeguro(javax.swing.JLabel label, String texto) {
                if (label != null && texto != null) {
                        label.setText(texto);
                }
        }

        /**
         * Establece estadísticas por defecto en caso de error
         */
        private void establecerEstadisticasPorDefecto() {
                actualizarLabelSeguro(jLabel2, "0");
                actualizarLabelSeguro(jLabel4, "0");
                actualizarLabelSeguro(jLabel6, "0");
                actualizarLabelSeguro(jLabel8, "0");
                System.out.println("WARNING  Estadísticas establecidas por defecto");
        }

        /**
         * Establece alertas por defecto en caso de error
         */
        private void establecerAlertasPorDefecto() {
                actualizarLabelSeguro(txtAutoPendientes, "0 devoluciones requieren aprobación gerencial");
                actualizarLabelSeguro(txtFechaNotas, "0 notas vencen en los próximos 7 días");
                actualizarLabelSeguro(txtStockAltoDevolucion, "Revisar productos con alta devolución");
                System.out.println("WARNING  Alertas establecidas por defecto");
        }

        /**
         * Muestra mensajes de error al usuario
         */
        private void mostrarError(String mensaje) {
                if (mensaje != null && !mensaje.trim().isEmpty()) {
                        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
                }
        }

        /**
         * Verifica si el MVC está inicializado
         */
        public boolean isInicializadoMVC() {
                return inicializadoMVC;
        }

        /**
         * MÉTODO CORREGIDO: Carga devoluciones con validación completa
         */
        private void cargarDevolucionesRecientes() {
                try {
                        if (serviceDevolucion == null) {
                                System.err.println(
                                                "WARNING  ServiceDevolucion no inicializado para cargar devoluciones");
                                return;
                        }

                        if (modeloTabla == null) {
                                System.err.println("WARNING  Modelo de tabla no inicializado");
                                configurarTabla(); // Intentar configurar la tabla
                        }

                        List<ModelDevolucion> devoluciones = serviceDevolucion.obtenerDevolucionesRecientes(20);

                        // Limpiar tabla
                        modeloTabla.setRowCount(0);

                        if (devoluciones.isEmpty()) {
                                System.out.println("ℹ No se encontraron devoluciones recientes");
                                return;
                        }

                        // Cargar devoluciones en la tabla
                        for (ModelDevolucion devolucion : devoluciones) {
                                try {
                                        Object[] fila = crearFilaDevolucion(devolucion);
                                        if (fila != null) {
                                                modeloTabla.addRow(fila);
                                        }
                                } catch (Exception e) {
                                        System.err.println("ERROR  Error procesando devolución "
                                                        + devolucion.getNumeroDevolucion() + ": "
                                                        + e.getMessage());
                                }
                        }

                        System.out.println("SUCCESS  Cargadas " + devoluciones.size() + " devoluciones en la tabla");

                } catch (SQLException e) {
                        System.err.println("ERROR  Error SQL cargando devoluciones: " + e.getMessage());
                        mostrarError("Error de base de datos cargando devoluciones: " + e.getMessage());
                } catch (Exception e) {
                        System.err.println("ERROR  Error general cargando devoluciones: " + e.getMessage());
                        mostrarError("Error cargando devoluciones: " + e.getMessage());
                }
        }

        /**
         * Crea una fila de datos para la tabla de devoluciones
         */
        private Object[] crearFilaDevolucion(ModelDevolucion devolucion) {
                try {
                        Object[] fila = new Object[9];

                        fila[0] = devolucion.getIdDevolucion(); // ID Oculto
                        fila[1] = devolucion.getNumeroDevolucion() != null ? devolucion.getNumeroDevolucion() : "N/A";
                        fila[2] = devolucion.getNombreCliente() != null ? devolucion.getNombreCliente()
                                        : "ID: " + devolucion.getIdCliente();
                        fila[3] = devolucion.getFechaDevolucion() != null
                                        ? devolucion.getFechaDevolucion().format(formatoFecha)
                                        : "N/A";
                        // Guardamos el objeto Estado completo para el renderer, o el string si el
                        // renderer espera string
                        // ComprasForm usa toString() en el renderer, así que pasamos la descripcion o
                        // el valor
                        fila[4] = devolucion.getEstado() != null ? devolucion.getEstado().getValor() : "pendiente";

                        fila[5] = devolucion.getTotalDevolucion() != null ? devolucion.getTotalDevolucion()
                                        : BigDecimal.ZERO;
                        fila[6] = devolucion.getNombreUsuarioProcesa() != null ? devolucion.getNombreUsuarioProcesa()
                                        : "ID: " + devolucion.getIdUsuarioProcesa();
                        fila[7] = devolucion.getSaldoNotaCredito() != null ? devolucion.getSaldoNotaCredito()
                                        : BigDecimal.ZERO;
                        fila[8] = null; // Acciones (se maneja con render/editor)

                        return fila;
                } catch (Exception e) {
                        System.err.println("ERROR  Error creando fila para devolución: " + e.getMessage());
                        return null;
                }
        }

        /**
         * Aplica estilos a todos los paneles
         */
        private void aplicarEstiloPaneles() {
                try {
                        // Configurar estilos de botones
                        if (btnFiltrar != null) {
                                btnFiltrar.putClientProperty(FlatClientProperties.STYLE,
                                                "arc:20;background:$App.accent.default;");
                        }

                        // Configurar estilos de paneles de alertas
                        if (panelApendi != null) {
                                panelApendi.putClientProperty(FlatClientProperties.STYLE,
                                                "arc:20;background:#CCE5FF");
                        }
                        if (panelAnotsFv != null) {
                                panelAnotsFv.putClientProperty(FlatClientProperties.STYLE,
                                                "arc:20;background:#FFF3CD;");
                        }
                        if (panelStAl != null) {
                                panelStAl.putClientProperty(FlatClientProperties.STYLE,
                                                "arc:20;background:#F8D7DA;");
                        }

                        // Aplicar estilo a todos los paneles principales
                        javax.swing.JPanel[] paneles = {
                                        panelTitulo, panelAcR, panelFilt, panelDevo, panelAlerts,
                                        panelSt1, panelSt2, panelSt3, panelSt4
                        };

                        for (javax.swing.JPanel panel : paneles) {
                                if (panel != null) {
                                        panel.putClientProperty(FlatClientProperties.STYLE, PANEL_STYLE);
                                }
                        }

                        System.out.println("SUCCESS  Estilos de paneles aplicados");
                } catch (Exception e) {
                        System.err.println("ERROR  Error aplicando estilos: " + e.getMessage());
                }
        }

        /**
         * Abre el diálogo de autorización para la devolución seleccionada
         */
        private void autorizarDevolucionSeleccionada() {
                try {
                        // 1. Obtener devolución seleccionada de la tabla
                        int filaSeleccionada = tablaDevoluciones.getSelectedRow();

                        if (filaSeleccionada < 0) {
                                JOptionPane.showMessageDialog(this,
                                                "Debe seleccionar una devolución de la tabla",
                                                "Selección Requerida", JOptionPane.WARNING_MESSAGE);
                                return;
                        }

                        // 2. Obtener número de devolución de la fila seleccionada
                        String numeroDevolucion = (String) tablaDevoluciones.getValueAt(filaSeleccionada, 1); // Columna
                                                                                                              // 1 es el
                                                                                                              // número
                                                                                                              // de
                                                                                                              // devolución

                        // 3. Buscar la devolución completa
                        List<ModelDevolucion> devoluciones = serviceDevolucion.buscarDevoluciones(
                                        numeroDevolucion, null, null, null);

                        if (devoluciones.isEmpty()) {
                                JOptionPane.showMessageDialog(this,
                                                "No se encontró la devolución seleccionada",
                                                "Error", JOptionPane.ERROR_MESSAGE);
                                return;
                        }

                        ModelDevolucion devolucion = devoluciones.get(0);

                        // 4. Verificar que esté pendiente
                        if (!"pendiente".equals(devolucion.getEstado().getValor())) {
                                JOptionPane.showMessageDialog(this,
                                                "Solo se pueden autorizar devoluciones en estado PENDIENTE.\n" +
                                                                "Estado actual: "
                                                                + devolucion.getEstado().getDescripcion(),
                                                "Estado Inválido", JOptionPane.WARNING_MESSAGE);
                                return;
                        }

                        // 5. Abrir diálogo de autorización
                        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
                        DialogoAutorizacion dialogo = new DialogoAutorizacion(parentFrame, devolucion);
                        dialogo.setVisible(true);

                        // 6. Actualizar datos si se autorizó
                        if (dialogo.isAutorizado()) {
                                actualizarDatos();

                                String resultado = dialogo.isAprobada() ? "APROBADA" : "RECHAZADA";
                                System.out.println("SUCCESS  Devolución " + numeroDevolucion + " " + resultado);
                        }

                } catch (Exception e) {
                        System.err.println("ERROR  Error autorizando devolución: " + e.getMessage());
                        JOptionPane.showMessageDialog(this,
                                        "Error autorizando devolución: " + e.getMessage(),
                                        "Error", JOptionPane.ERROR_MESSAGE);
                }
        }

        /**
         * Carga alertas del sistema con validación
         */
        private void cargarAlertas() {
                try {
                        if (serviceDevolucion == null) {
                                establecerAlertasPorDefecto();
                                return;
                        }

                        ServiceDevolucion.EstadisticasDevolucion stats = serviceDevolucion
                                        .obtenerEstadisticasDevolucion();

                        // Actualizar alertas con validación
                        actualizarLabelSeguro(txtAutoPendientes,
                                        stats.getAutorizacionPendiente()
                                                        + " devoluciones requieren aprobación gerencial");

                        actualizarLabelSeguro(txtFechaNotas,
                                        stats.getNotasPorVencer() + " notas vencen en los próximos 7 días");

                        actualizarLabelSeguro(txtStockAltoDevolucion,
                                        "Revisar productos con alta devolución");

                        System.out.println("SUCCESS  Alertas cargadas correctamente");

                } catch (SQLException e) {
                        System.err.println("ERROR  Error SQL cargando alertas: " + e.getMessage());
                        establecerAlertasPorDefecto();
                } catch (Exception e) {
                        System.err.println("ERROR  Error general cargando alertas: " + e.getMessage());
                        establecerAlertasPorDefecto();
                }
        }

        /**
         * Actualiza los datos del dashboard
         */
        public void actualizarDatos() {
                try {
                        cargarDatosIniciales();
                        System.out.println("SUCCESS  Datos actualizados correctamente");
                } catch (Exception e) {
                        System.err.println("ERROR  Error actualizando datos: " + e.getMessage());
                }
        }

        /**
         * Realiza búsqueda de devoluciones con validación mejorada
         */
        private void buscarDevoluciones() {
                try {
                        if (serviceDevolucion == null) {
                                mostrarError("Servicio de devoluciones no disponible");
                                return;
                        }

                        String numeroDevolucion = txtNumero.getText().trim();
                        String estado = (String) cbxEstado.getSelectedItem();

                        // Convertir "Todos" a null para el servicio
                        if ("Todos".equals(estado)) {
                                estado = null;
                        }

                        List<ModelDevolucion> resultados = serviceDevolucion.buscarDevoluciones(
                                        numeroDevolucion.isEmpty() ? null : numeroDevolucion,
                                        estado,
                                        null, // fechaInicio
                                        null // fechaFin
                        );

                        // Limpiar y cargar resultados
                        modeloTabla.setRowCount(0);

                        for (ModelDevolucion devolucion : resultados) {
                                Object[] fila = crearFilaDevolucion(devolucion);
                                if (fila != null) {
                                        modeloTabla.addRow(fila);
                                }
                        }

                        System.out.println("SUCCESS  Búsqueda completada: " + resultados.size() + " resultados");

                } catch (SQLException e) {
                        System.err.println("ERROR  Error SQL en búsqueda: " + e.getMessage());
                        mostrarError("Error de base de datos en búsqueda: " + e.getMessage());
                } catch (Exception e) {
                        System.err.println("ERROR  Error general en búsqueda: " + e.getMessage());
                        mostrarError("Error realizando búsqueda: " + e.getMessage());
                }
        }

        /**
         * Configura los ComboBoxes con valores predeterminados
         */
        private void configurarComboBoxes() {
                try {
                        // Configurar ComboBox de estados
                        if (cbxEstado != null) {
                                cbxEstado.removeAllItems();
                                cbxEstado.addItem("Todos");
                                cbxEstado.addItem("pendiente");
                                cbxEstado.addItem("procesando");
                                cbxEstado.addItem("aprobada");
                                cbxEstado.addItem("rechazada");
                                cbxEstado.addItem("finalizada");
                        }

                        // Configurar ComboBox de rangos de fecha
                        if (cbxRango != null) {
                                cbxRango.removeAllItems();
                                cbxRango.addItem("Últimos 7 días");
                                cbxRango.addItem("Últimos 30 días");
                                cbxRango.addItem("Últimos 3 meses");
                                cbxRango.addItem("Todo el año");
                        }

                        System.out.println("SUCCESS  ComboBoxes configurados");
                } catch (Exception e) {
                        System.err.println("ERROR  Error configurando ComboBoxes: " + e.getMessage());
                }
        }

        /**
         * MÉTODO CORREGIDO: Configuración robusta de la tabla
         */
        /**
         * MÉTODO CORREGIDO: Configuración robusta de la tabla
         */
        private void configurarTabla() {
                try {
                        // 1. Inicializar Modelo (Fix NPE)
                        String[] columnas = { "ID", "N° Devolución", "Cliente", "Fecha", "Estado", "Total", "Usuario",
                                        "Saldo", "Acciones" };
                        modeloTabla = new DefaultTableModel(columnas, 0) {
                                @Override
                                public boolean isCellEditable(int row, int column) {
                                        return column == 8; // Solo acciones editables
                                }

                                @Override
                                public Class<?> getColumnClass(int columnIndex) {
                                        if (columnIndex == 5 || columnIndex == 7) {
                                                return BigDecimal.class;
                                        }
                                        return Object.class;
                                }
                        };

                        if (tablaDevoluciones != null) {
                                tablaDevoluciones.setModel(modeloTabla); // Asignar modelo a la tabla existente

                                configurarAnchoColumnas();
                                configurarEstiloTabla();

                                // 1. Ocultar Columna ID (Índice 0)
                                tablaDevoluciones.getColumnModel().getColumn(0).setMinWidth(0);
                                tablaDevoluciones.getColumnModel().getColumn(0).setMaxWidth(0);
                                tablaDevoluciones.getColumnModel().getColumn(0).setWidth(0);

                                // 2. Renderers para Moneda (Total, Saldo)
                                javax.swing.table.DefaultTableCellRenderer currencyRenderer = new javax.swing.table.DefaultTableCellRenderer() {
                                        private final java.text.NumberFormat format = java.text.NumberFormat
                                                        .getCurrencyInstance(java.util.Locale.of("es", "CO"));

                                        @Override
                                        public java.awt.Component getTableCellRendererComponent(
                                                        javax.swing.JTable table, Object value, boolean isSelected,
                                                        boolean hasFocus, int row, int column) {
                                                java.awt.Component c = super.getTableCellRendererComponent(table, value,
                                                                isSelected, hasFocus, row, column);
                                                if (value instanceof BigDecimal) {
                                                        setText(format.format(value));
                                                }
                                                setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
                                                return c;
                                        }
                                };
                                tablaDevoluciones.getColumnModel().getColumn(5).setCellRenderer(currencyRenderer); // Total
                                tablaDevoluciones.getColumnModel().getColumn(7).setCellRenderer(currencyRenderer); // Saldo

                                // 3. Renderer para Estado (Chip) - MEJORADO (Más pequeño)
                                tablaDevoluciones.getColumnModel().getColumn(4)
                                                .setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
                                                        @Override
                                                        public java.awt.Component getTableCellRendererComponent(
                                                                        javax.swing.JTable table, Object value,
                                                                        boolean isSelected, boolean hasFocus, int row,
                                                                        int column) {
                                                                javax.swing.JLabel l = (javax.swing.JLabel) super.getTableCellRendererComponent(
                                                                                table, value, isSelected, hasFocus, row,
                                                                                column);
                                                                String estado = value != null
                                                                                ? value.toString().toLowerCase()
                                                                                : "";
                                                                java.awt.Color bg = switch (estado) {
                                                                        case "aprobada", "completado" ->
                                                                                new java.awt.Color(40, 167, 69); // Verde
                                                                        case "pendiente" ->
                                                                                new java.awt.Color(255, 193, 7); // Amarillo
                                                                        case "procesando", "parcial" ->
                                                                                new java.awt.Color(0, 122, 255); // Azul
                                                                        case "rechazada", "cancelado" ->
                                                                                new java.awt.Color(220, 53, 69); // Rojo
                                                                        default -> table.getBackground();
                                                                };

                                                                if (!isSelected) {
                                                                        javax.swing.JPanel p = new javax.swing.JPanel(
                                                                                        new java.awt.FlowLayout(
                                                                                                        java.awt.FlowLayout.CENTER,
                                                                                                        0, 0));
                                                                        p.setOpaque(false);
                                                                        javax.swing.JLabel chip = new javax.swing.JLabel(
                                                                                        value != null ? value.toString()
                                                                                                        .toUpperCase()
                                                                                                        : "");
                                                                        chip.setHorizontalAlignment(
                                                                                        javax.swing.SwingConstants.CENTER);
                                                                        chip.setForeground(java.awt.Color.WHITE);
                                                                        chip.setFont(chip.getFont().deriveFont(10f)); // Fuente
                                                                                                                      // más
                                                                                                                      // pequeña
                                                                        chip.setBorder(javax.swing.BorderFactory
                                                                                        .createEmptyBorder(2, 8, 2, 8)); // Padding
                                                                                                                         // reducido
                                                                        chip.putClientProperty(
                                                                                        FlatClientProperties.STYLE,
                                                                                        "arc:10;background:" +
                                                                                                        String.format("#%02x%02x%02x",
                                                                                                                        bg.getRed(),
                                                                                                                        bg.getGreen(),
                                                                                                                        bg.getBlue()));
                                                                        p.add(chip);
                                                                        return p;
                                                                }
                                                                return l;
                                                        }
                                                });

                                // 4. Configurar Acciones (Índice 8)
                                raven.cell.TableActionDevolucionEvent event = new raven.cell.TableActionDevolucionEvent() {
                                        @Override
                                        public void onView(int row) {
                                                try {
                                                        // 1. Obtener datos de la fila
                                                        String numeroDevolucion = (String) tablaDevoluciones
                                                                        .getValueAt(row, 1);

                                                        // 2. Buscar cabecera
                                                        List<ModelDevolucion> devoluciones = serviceDevolucion
                                                                        .buscarDevoluciones(numeroDevolucion, null,
                                                                                        null, null);

                                                        if (!devoluciones.isEmpty()) {
                                                                ModelDevolucion dev = devoluciones.get(0);

                                                                // 3. Obtener detalles incluyendo nombres de productos
                                                                List<raven.controlador.comercial.ModelDetalleDevolucion> detalles = serviceDevolucion
                                                                                .getDetallesConProductos(
                                                                                                dev.getIdDevolucion());

                                                                // 4. Construir HTML (Dark Mode)
                                                                StringBuilder html = new StringBuilder();
                                                                html.append("<html><body style='width: 500px; font-family: sans-serif; color: #e0e0e0;'>");

                                                                // Encabezado
                                                                html.append("<h2 style='color: #64b5f6; border-bottom: 2px solid #0d47a1; padding-bottom: 5px;'>Devolución #")
                                                                                .append(dev.getNumeroDevolucion())
                                                                                .append("</h2>");

                                                                html.append("<table style='width: 100%; margin-bottom: 10px; color: #e0e0e0;'>");
                                                                html.append("<tr><td><b>Fecha:</b> ")
                                                                                .append(dev.getFechaDevolucion()
                                                                                                .format(formatoFecha))
                                                                                .append("</td>");
                                                                html.append("<td><b>Estado:</b> <span style='color: ")
                                                                                .append(getColorHex(dev.getEstado()
                                                                                                .getValor()))
                                                                                .append("'>")
                                                                                .append(dev.getEstado().getDescripcion()
                                                                                                .toUpperCase())
                                                                                .append("</span></td></tr>");

                                                                // Motivo y Observaciones (Fondo oscuro, texto claro)
                                                                html.append("<div style='background-color: #424242; padding: 10px; border-radius: 5px; margin-bottom: 15px; border: 1px solid #616161;'>");
                                                                html.append("<b>Motivo:</b> <span style='color: #ffcc80;'>")
                                                                                .append(dev.getMotivo()
                                                                                                .getDescripcion())
                                                                                .append("</span><br>");
                                                                html.append("<b>Observaciones:</b> ")
                                                                                .append(dev.getObservaciones() != null
                                                                                                ? dev.getObservaciones()
                                                                                                : "Ninguna");
                                                                html.append("</div>");

                                                                // Información de Venta y Cliente
                                                                html.append("<table style='width: 100%; margin-bottom: 15px; color: #e0e0e0;'>");
                                                                html.append("<tr><td><b>Venta #ID:</b> ")
                                                                                .append(dev.getIdVenta())
                                                                                .append(" | <b>Total:</b> ").append(
                                                                                                String.format("$%.2f",
                                                                                                                dev.getTotalVenta() != null
                                                                                                                                ? dev.getTotalVenta()
                                                                                                                                : BigDecimal.ZERO))
                                                                                .append("</td>");
                                                                html.append("<td><b>Vendedor:</b> ").append(dev
                                                                                .getNombreUsuarioVenta() != null ? dev
                                                                                                .getNombreUsuarioVenta()
                                                                                                : "N/A")
                                                                                .append("</td></tr>");
                                                                html.append("<tr><td><b>Cliente:</b> ")
                                                                                .append(dev.getNombreCliente() != null
                                                                                                ? dev.getNombreCliente()
                                                                                                : "ID: " + dev.getIdCliente())
                                                                                .append("</td>"); // TODO: Nombre
                                                                                                  // cliente
                                                                html.append("<td><b>Total Devolución:</b> <span style='color: #64b5f6'>")
                                                                                .append(String.format("$%.2f", dev
                                                                                                .getTotalDevolucion()))
                                                                                .append("</span></td></tr>");
                                                                html.append("</table>");

                                                                // Tabla de Productos
                                                                html.append("<h3 style='color: #90caf9; margin-top: 0;'>Productos Devueltos</h3>");
                                                                html.append("<table border='1' cellspacing='0' cellpadding='5' style='width: 100%; border-collapse: collapse; border-color: #616161; color: #e0e0e0;'>");
                                                                html.append("<tr style='background-color: #333333; color: #ffffff;'><th>Producto</th><th>Cant</th><th>Estado</th><th align='right'>Subtotal</th></tr>");

                                                                for (raven.controlador.comercial.ModelDetalleDevolucion det : detalles) {
                                                                        String nombreCompleto = det.getNombreProducto();
                                                                        if (det.getNombreVariante() != null && !det
                                                                                        .getNombreVariante()
                                                                                        .equals("N/A")) {
                                                                                nombreCompleto += " (" + det
                                                                                                .getNombreVariante()
                                                                                                + ")";
                                                                        }

                                                                        html.append("<tr>");
                                                                        html.append("<td>").append(nombreCompleto)
                                                                                        .append("</td>");
                                                                        html.append("<td align='center'>").append(
                                                                                        det.getCantidadDevuelta())
                                                                                        .append("</td>");
                                                                        html.append("<td>").append(det
                                                                                        .getCondicionProducto()
                                                                                        .getDescripcion())
                                                                                        .append("</td>");
                                                                        html.append("<td align='right'>").append(
                                                                                        String.format("$%.2f", det
                                                                                                        .getSubtotalDevolucion()))
                                                                                        .append("</td>");
                                                                        html.append("</tr>");
                                                                }
                                                                html.append("</table>");

                                                                // Footer (Info Autorización si existe)
                                                                if (dev.getIdUsuarioAutoriza() != null
                                                                                && dev.getIdUsuarioAutoriza() > 0) {
                                                                        html.append("<div style='margin-top: 15px; font-size: 0.9em; color: #666;'>");
                                                                        html.append("Autorizado por ID: ").append(
                                                                                        dev.getIdUsuarioAutoriza());
                                                                        html.append(" | ").append(dev
                                                                                        .getFechaAutorizacion() != null
                                                                                                        ? dev.getFechaAutorizacion()
                                                                                                                        .format(formatoFecha)
                                                                                                        : "");
                                                                        html.append("</div>");
                                                                }

                                                                html.append("</body></html>");

                                                                JOptionPane.showMessageDialog(devolucionMainForm.this,
                                                                                html.toString(),
                                                                                "Detalle de Devolución",
                                                                                JOptionPane.PLAIN_MESSAGE);
                                                        }
                                                } catch (Exception e) {
                                                        e.printStackTrace();
                                                }
                                        }

                                        @Override
                                        public void onAuthorize(int row) {
                                                procesarAccionAutorizacion(row, true);
                                        }

                                        @Override
                                        public void onReject(int row) {
                                                procesarAccionAutorizacion(row, false);
                                        }

                                        @Override
                                        public void onAnulate(int row) {
                                                try {
                                                        String numeroDevolucion = (String) tablaDevoluciones
                                                                        .getValueAt(row, 1);
                                                        List<ModelDevolucion> devoluciones = serviceDevolucion
                                                                        .buscarDevoluciones(numeroDevolucion, null,
                                                                                        null, null);
                                                        if (devoluciones.isEmpty())
                                                                return;

                                                        ModelDevolucion dev = devoluciones.get(0);

                                                        int confirm = javax.swing.JOptionPane.showConfirmDialog(
                                                                        devolucionMainForm.this,
                                                                        "¿Está seguro de querer anular la devolución "
                                                                                        + numeroDevolucion + "?\n" +
                                                                                        "Esta acción revertirá los cambios en el inventario y anulará la Nota de Crédito.",
                                                                        "Confirmar Anulación",
                                                                        javax.swing.JOptionPane.YES_NO_OPTION,
                                                                        javax.swing.JOptionPane.WARNING_MESSAGE);

                                                        if (confirm == javax.swing.JOptionPane.YES_OPTION) {
                                                                String motivo = javax.swing.JOptionPane.showInputDialog(
                                                                                devolucionMainForm.this,
                                                                                "Ingrese el motivo de la anulación:",
                                                                                "Motivo",
                                                                                javax.swing.JOptionPane.PLAIN_MESSAGE);

                                                                if (motivo != null && !motivo.trim().isEmpty()) {
                                                                        boolean exito = serviceDevolucion
                                                                                        .anularDevolucion(
                                                                                                        dev.getIdDevolucion(),
                                                                                                        raven.clases.admin.UserSession
                                                                                                                        .getInstance()
                                                                                                                        .getCurrentUser()
                                                                                                                        .getIdUsuario(),
                                                                                                        motivo);

                                                                        if (exito) {
                                                                                new raven.application.form.comercial.devolucion.components.ModernAlert(
                                                                                                raven.application.form.comercial.devolucion.components.ModernAlert.AlertType.SUCCESS,
                                                                                                "Éxito",
                                                                                                "Devolución anulada correctamente")
                                                                                                .setVisible(true);
                                                                                cargarDatos(); // Recargar tabla
                                                                        } else {
                                                                                new raven.application.form.comercial.devolucion.components.ModernAlert(
                                                                                                raven.application.form.comercial.devolucion.components.ModernAlert.AlertType.ERROR,
                                                                                                "Error",
                                                                                                "No se pudo anular la devolución")
                                                                                                .setVisible(true);
                                                                        }
                                                                }
                                                        }
                                                } catch (Exception e) {
                                                        e.printStackTrace();
                                                }
                                        }
                                };

                                tablaDevoluciones.getColumnModel().getColumn(8)
                                                .setCellRenderer(new raven.cell.TableActionCellDevolucionRender());
                                tablaDevoluciones.getColumnModel().getColumn(8)
                                                .setCellEditor(new raven.cell.TableActionCellDevolucionEditor(event));

                        } else {
                                System.err.println("WARNING  tablaDevoluciones es null, no se puede configurar");
                        }

                } catch (Exception e) {
                        System.err.println("ERROR  Error configurando tabla: " + e.getMessage());
                        e.printStackTrace();
                }
        }

        // Helper para color de estado en HTML
        private String getColorHex(String estado) {
                return switch (estado) {
                        case "aprobada", "completado" -> "#28a745";
                        case "pendiente" -> "#ffc107";
                        case "procesando", "parcial" -> "#007bff";
                        case "rechazada", "cancelado" -> "#dc3545";
                        default -> "#333333";
                };
        }

        private void procesarAccionAutorizacion(int row, boolean aprobar) {
                try {
                        String numeroDevolucion = (String) tablaDevoluciones.getValueAt(row, 1); // Columna 1 es el
                                                                                                 // número de devolución
                        List<ModelDevolucion> devoluciones = serviceDevolucion.buscarDevoluciones(numeroDevolucion,
                                        null, null, null);

                        if (devoluciones.isEmpty())
                                return;

                        ModelDevolucion devolucion = devoluciones.get(0);

                        // Verificar estado
                        if (!"pendiente".equals(devolucion.getEstado().getValor())) {
                                JOptionPane.showMessageDialog(this,
                                                "Solo se pueden procesar devoluciones en estado PENDIENTE.\nEstado actual: "
                                                                + devolucion.getEstado().getDescripcion(),
                                                "Estado Inválido", JOptionPane.WARNING_MESSAGE);
                                return;
                        }

                        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
                        DialogoAutorizacion dialogo = new DialogoAutorizacion(parentFrame, devolucion);

                        // Si es rechazo directo, podríamos saltar el diálogo o pre-configurarlo,
                        // pero por seguridad usamos el mismo diálogo de autorización que requiere
                        // credenciales
                        dialogo.setTitle(aprobar ? "Autorizar Devolución" : "Rechazar Devolución");
                        dialogo.setVisible(true);

                        if (dialogo.isAutorizado()) {
                                actualizarDatos();
                        }

                } catch (Exception e) {
                        e.printStackTrace();
                        mostrarError("Error procesando acción: " + e.getMessage());
                }
        }

        /**
         * Configura el ancho de las columnas de la tabla
         */
        private void configurarAnchoColumnas() {
                try {
                        if (tablaDevoluciones.getColumnModel().getColumnCount() >= 9) {
                                // 0: ID (Oculto)
                                // 1: N° Devolución (120)
                                // 2: Cliente (180)
                                // 3: Fecha (110)
                                // 4: Estado (120)
                                // 5: Total (100)
                                // 6: Usuario (100)
                                // 7: Saldo (100)
                                // 8: Acciones (150)

                                tablaDevoluciones.getColumnModel().getColumn(1).setPreferredWidth(120);
                                tablaDevoluciones.getColumnModel().getColumn(2).setPreferredWidth(180);
                                tablaDevoluciones.getColumnModel().getColumn(3).setPreferredWidth(110);
                                tablaDevoluciones.getColumnModel().getColumn(4).setPreferredWidth(120);
                                tablaDevoluciones.getColumnModel().getColumn(5).setPreferredWidth(100);
                                tablaDevoluciones.getColumnModel().getColumn(6).setPreferredWidth(100);
                                tablaDevoluciones.getColumnModel().getColumn(7).setPreferredWidth(100);
                                tablaDevoluciones.getColumnModel().getColumn(8).setPreferredWidth(200);
                        }
                } catch (Exception e) {
                        System.err.println("ERROR  Error configurando ancho de columnas: " + e.getMessage());
                }
        }

        /**
         * Aplica estilos a la tabla
         */
        private void configurarEstiloTabla() {
                try {
                        // Estilo "Clean" (Productos/Compras style)
                        tablaDevoluciones.setRowHeight(50);
                        tablaDevoluciones.setShowHorizontalLines(true);
                        tablaDevoluciones.setShowVerticalLines(false); // Limpio
                        tablaDevoluciones.setIntercellSpacing(new java.awt.Dimension(0, 1));

                        tablaDevoluciones.putClientProperty(FlatClientProperties.STYLE,
                                        "font: bold 14;" +
                                                        "selectionBackground: $Table.selectionBackground;" +
                                                        "selectionForeground: $Table.selectionForeground;" +
                                                        "gridColor: $Table.gridColor;" +
                                                        "showHorizontalLines: true;" +
                                                        "showVerticalLines: false;" +
                                                        "rowHeight: 50;" +
                                                        "cellFocusColor: $Table.selectionBackground");

                        tablaDevoluciones.getTableHeader().putClientProperty(FlatClientProperties.STYLE,
                                        "height: 50;" +
                                                        "font: bold 16;" +
                                                        "hoverBackground: $Table.gridColor;" +
                                                        "separatorColor: $Table.background;" + // Invisible separators
                                                        "bottomSeparatorColor: $Table.selectionBackground;" +
                                                        "foreground: $Table.foreground");

                        tablaDevoluciones.getTableHeader().setReorderingAllowed(false);

                } catch (Exception e) {
                        System.err.println("ERROR  Error aplicando estilos a tabla: " + e.getMessage());
                }
        }

        /**
         * Configura el menú contextual para operaciones con clic derecho
         */
        private void configurarMenuContextual() {
                JPopupMenu popupMenu = new JPopupMenu();

                JMenuItem itemCopiarDev = new JMenuItem("Copiar Número de Devolución");
                JMenuItem itemCopiarNC = new JMenuItem("Copiar Número de Nota de Crédito");
                JMenuItem itemVerificarSaldo = new JMenuItem("Verificar Saldo");

                itemCopiarDev.addActionListener(e -> {
                        int row = tablaDevoluciones.getSelectedRow();
                        if (row != -1) {
                                String numDev = (String) tablaDevoluciones.getValueAt(row, 0); // Columna 0: Número
                                                                                               // Devolución
                                copyToClipboard(numDev);
                        }
                });

                itemCopiarNC.addActionListener(e -> {
                        int row = tablaDevoluciones.getSelectedRow();
                        if (row != -1) {
                                try {
                                        String idDevolucionStr = tablaDevoluciones.getValueAt(row, 1).toString(); // N°
                                                                                                                  // Devolución
                                        // Buscar devolución para obtener datos frescos
                                        List<ModelDevolucion> devs = serviceDevolucion
                                                        .buscarDevoluciones(idDevolucionStr, null, null, null);
                                        if (!devs.isEmpty()) {
                                                ModelDevolucion dev = devs.get(0);
                                                String numNC = dev.getNumeroNotaCredito();
                                                if (numNC != null && !numNC.isEmpty()) {
                                                        copyToClipboard(numNC);
                                                } else {
                                                        mostrarError("Esta devolución no tiene Nota de Crédito asignada");
                                                }
                                        }
                                } catch (Exception ex) {
                                        System.err.println("Error obteniendo NC: " + ex.getMessage());
                                }
                        }
                });

                itemVerificarSaldo.addActionListener(e -> {
                        int row = tablaDevoluciones.getSelectedRow();
                        if (row != -1) {
                                try {
                                        String idDevolucionStr = tablaDevoluciones.getValueAt(row, 1).toString(); // N°
                                                                                                                  // Devolución
                                        List<ModelDevolucion> devs = serviceDevolucion
                                                        .buscarDevoluciones(idDevolucionStr, null, null, null);
                                        if (!devs.isEmpty()) {
                                                ModelDevolucion dev = devs.get(0);
                                                String numNC = dev.getNumeroNotaCredito();
                                                BigDecimal saldo = dev.getSaldoNotaCredito();

                                                if (numNC != null && !numNC.isEmpty()) {
                                                        JOptionPane.showMessageDialog(this,
                                                                        "Nota de Crédito: " + numNC
                                                                                        + "\nSaldo Disponible: "
                                                                                        + String.format("$%,.2f",
                                                                                                        saldo),
                                                                        "Verificación de Saldo",
                                                                        JOptionPane.INFORMATION_MESSAGE);
                                                } else {
                                                        mostrarError("No hay Nota de Crédito para verificar");
                                                }
                                        }
                                } catch (Exception ex) {
                                        ex.printStackTrace();
                                }
                        }
                });

                popupMenu.add(itemCopiarDev);
                popupMenu.add(itemCopiarNC);
                popupMenu.addSeparator();
                popupMenu.add(itemVerificarSaldo);

                tablaDevoluciones.setComponentPopupMenu(popupMenu);
        }

        private void copyToClipboard(String text) {
                StringSelection selection = new StringSelection(text);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                System.out.println("Copiado al portapapeles: " + text);
        }

        @SuppressWarnings("unchecked")
        // <editor-fold defaultstate="collapsed" desc="Generated
        // Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                panelMain = new javax.swing.JPanel();
                panelTitulo = new javax.swing.JPanel();
                jLabel1 = new javax.swing.JLabel();
                btnNueva1 = new javax.swing.JButton();
                btnAutorizar = new javax.swing.JButton();
                panelSt = new javax.swing.JPanel();
                panelSt1 = new javax.swing.JPanel();
                jLabel2 = new javax.swing.JLabel();
                jLabel3 = new javax.swing.JLabel();
                panelSt2 = new javax.swing.JPanel();
                jLabel4 = new javax.swing.JLabel();
                jLabel5 = new javax.swing.JLabel();
                panelSt3 = new javax.swing.JPanel();
                jLabel6 = new javax.swing.JLabel();
                jLabel7 = new javax.swing.JLabel();
                panelSt4 = new javax.swing.JPanel();
                jLabel8 = new javax.swing.JLabel();
                jLabel9 = new javax.swing.JLabel();
                panelFilt = new javax.swing.JPanel();
                jLabel10 = new javax.swing.JLabel();
                txtNumero = new javax.swing.JTextField();
                jLabel11 = new javax.swing.JLabel();
                cbxEstado = new javax.swing.JComboBox<>();
                jLabel12 = new javax.swing.JLabel();
                cbxRango = new javax.swing.JComboBox<>();
                btnFiltrar = new javax.swing.JButton();
                panelAcR = new javax.swing.JPanel();
                jLabel14 = new javax.swing.JLabel();
                paneNuevo1 = new javax.swing.JPanel();
                jButton3 = new javax.swing.JButton();
                jLabel15 = new javax.swing.JLabel();
                jLabel16 = new javax.swing.JLabel();
                panelGenReport = new javax.swing.JPanel();
                jButton4 = new javax.swing.JButton();
                jLabel17 = new javax.swing.JLabel();
                jLabel18 = new javax.swing.JLabel();
                panelDevo = new javax.swing.JPanel();
                panelTitulodevo = new javax.swing.JPanel();
                jLabel13 = new javax.swing.JLabel();
                jScrollPane1 = new javax.swing.JScrollPane();
                tablaDevoluciones = new javax.swing.JTable();
                panelAlerts = new javax.swing.JPanel();
                jLabel19 = new javax.swing.JLabel();
                panelApendi = new javax.swing.JPanel();
                jLabel20 = new javax.swing.JLabel();
                txtAutoPendientes = new javax.swing.JLabel();
                panelAnotsFv = new javax.swing.JPanel();
                jLabel22 = new javax.swing.JLabel();
                txtFechaNotas = new javax.swing.JLabel();
                panelStAl = new javax.swing.JPanel();
                txtStockAltoDevolucion = new javax.swing.JLabel();
                jLabel25 = new javax.swing.JLabel();

                panelTitulo.setBackground(new java.awt.Color(204, 255, 204));

                jLabel1.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
                jLabel1.setText("Gestión de Devoluciones ");

                btnNueva1.setText("+ Nueva devolucion");
                btnNueva1.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                btnNueva1ActionPerformed(evt);
                        }
                });

                btnAutorizar.setText("Autorizar");
                btnAutorizar.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                btnAutorizarActionPerformed(evt);
                        }
                });

                javax.swing.GroupLayout panelTituloLayout = new javax.swing.GroupLayout(panelTitulo);
                panelTitulo.setLayout(panelTituloLayout);
                panelTituloLayout.setHorizontalGroup(
                                panelTituloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelTituloLayout.createSequentialGroup()
                                                                .addGap(49, 49, 49)
                                                                .addComponent(jLabel1,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                                                0, Short.MAX_VALUE)
                                                                .addComponent(btnAutorizar,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                181,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addComponent(btnNueva1,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                247,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(28, 28, 28)));
                panelTituloLayout.setVerticalGroup(
                                panelTituloLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                Short.MAX_VALUE)
                                                .addGroup(panelTituloLayout.createSequentialGroup()
                                                                .addGap(19, 19, 19)
                                                                .addGroup(panelTituloLayout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.TRAILING)
                                                                                .addComponent(btnAutorizar,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(btnNueva1,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                45,
                                                                                                Short.MAX_VALUE))
                                                                .addGap(23, 23, 23)));

                jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
                jLabel2.setText("0");

                jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
                jLabel3.setText("pendientes");

                javax.swing.GroupLayout panelSt1Layout = new javax.swing.GroupLayout(panelSt1);
                panelSt1.setLayout(panelSt1Layout);
                panelSt1Layout.setHorizontalGroup(
                                panelSt1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelSt1Layout.createSequentialGroup()
                                                                .addGap(33, 33, 33)
                                                                .addGroup(panelSt1Layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(jLabel3,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(jLabel2,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE))
                                                                .addContainerGap()));
                panelSt1Layout.setVerticalGroup(
                                panelSt1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelSt1Layout.createSequentialGroup()
                                                                .addGap(15, 15, 15)
                                                                .addComponent(jLabel2,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel3,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));

                jLabel4.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
                jLabel4.setText("0");

                jLabel5.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
                jLabel5.setText("Procesando");

                javax.swing.GroupLayout panelSt2Layout = new javax.swing.GroupLayout(panelSt2);
                panelSt2.setLayout(panelSt2Layout);
                panelSt2Layout.setHorizontalGroup(
                                panelSt2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelSt2Layout.createSequentialGroup()
                                                                .addGap(33, 33, 33)
                                                                .addGroup(panelSt2Layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(jLabel5,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(jLabel4,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE))
                                                                .addContainerGap()));
                panelSt2Layout.setVerticalGroup(
                                panelSt2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelSt2Layout.createSequentialGroup()
                                                                .addGap(15, 15, 15)
                                                                .addComponent(jLabel4,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel5,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));

                jLabel6.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
                jLabel6.setText("0");

                jLabel7.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
                jLabel7.setText("Aprobadas Hoy ");

                javax.swing.GroupLayout panelSt3Layout = new javax.swing.GroupLayout(panelSt3);
                panelSt3.setLayout(panelSt3Layout);
                panelSt3Layout.setHorizontalGroup(
                                panelSt3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelSt3Layout.createSequentialGroup()
                                                                .addGap(33, 33, 33)
                                                                .addGroup(panelSt3Layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(jLabel7,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(jLabel6,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE))
                                                                .addContainerGap()));
                panelSt3Layout.setVerticalGroup(
                                panelSt3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelSt3Layout.createSequentialGroup()
                                                                .addGap(15, 15, 15)
                                                                .addComponent(jLabel6,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel7,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));

                jLabel8.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
                jLabel8.setText("0");

                jLabel9.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
                jLabel9.setText("Rechazadas ");

                javax.swing.GroupLayout panelSt4Layout = new javax.swing.GroupLayout(panelSt4);
                panelSt4.setLayout(panelSt4Layout);
                panelSt4Layout.setHorizontalGroup(
                                panelSt4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelSt4Layout.createSequentialGroup()
                                                                .addGap(33, 33, 33)
                                                                .addGroup(panelSt4Layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(jLabel9,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(jLabel8,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE))
                                                                .addContainerGap()));
                panelSt4Layout.setVerticalGroup(
                                panelSt4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelSt4Layout.createSequentialGroup()
                                                                .addGap(15, 15, 15)
                                                                .addComponent(jLabel8,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                50, Short.MAX_VALUE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel9,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));

                javax.swing.GroupLayout panelStLayout = new javax.swing.GroupLayout(panelSt);
                panelSt.setLayout(panelStLayout);
                panelStLayout.setHorizontalGroup(
                                panelStLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelStLayout.createSequentialGroup()
                                                                .addComponent(panelSt1,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addGap(30, 30, 30)
                                                                .addComponent(panelSt2,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addGap(30, 30, 30)
                                                                .addComponent(panelSt3,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addGap(30, 30, 30)
                                                                .addComponent(panelSt4,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));
                panelStLayout.setVerticalGroup(
                                panelStLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelStLayout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addGroup(panelStLayout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(panelSt2,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(panelSt1,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(panelSt3,
                                                                                                javax.swing.GroupLayout.Alignment.TRAILING,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(panelSt4,
                                                                                                javax.swing.GroupLayout.Alignment.TRAILING,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE))
                                                                .addContainerGap()));

                panelFilt.setBackground(new java.awt.Color(153, 255, 153));

                jLabel10.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
                jLabel10.setText("Buscar");

                jLabel11.setText("Estado");

                cbxEstado.setModel(
                                new javax.swing.DefaultComboBoxModel<>(
                                                new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

                jLabel12.setText("Fecha");

                cbxRango.setModel(
                                new javax.swing.DefaultComboBoxModel<>(
                                                new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

                btnFiltrar.setText("Buscar");
                btnFiltrar.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                btnFiltrarActionPerformed(evt);
                        }
                });

                javax.swing.GroupLayout panelFiltLayout = new javax.swing.GroupLayout(panelFilt);
                panelFilt.setLayout(panelFiltLayout);
                panelFiltLayout.setHorizontalGroup(
                                panelFiltLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelFiltLayout.createSequentialGroup()
                                                                .addGap(26, 26, 26)
                                                                .addGroup(panelFiltLayout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addGroup(panelFiltLayout
                                                                                                .createSequentialGroup()
                                                                                                .addComponent(jLabel10,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                Short.MAX_VALUE)
                                                                                                .addGap(0, 0, Short.MAX_VALUE))
                                                                                .addGroup(panelFiltLayout
                                                                                                .createSequentialGroup()
                                                                                                .addComponent(txtNumero,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                Short.MAX_VALUE)
                                                                                                .addGap(18, 18, 18)))
                                                                .addGroup(panelFiltLayout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(jLabel11,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addGroup(panelFiltLayout
                                                                                                .createSequentialGroup()
                                                                                                .addComponent(cbxEstado,
                                                                                                                0, 155,
                                                                                                                Short.MAX_VALUE)
                                                                                                .addGap(21, 21, 21)))
                                                                .addGap(18, 18, 18)
                                                                .addGroup(panelFiltLayout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addGroup(panelFiltLayout
                                                                                                .createSequentialGroup()
                                                                                                .addComponent(cbxRango,
                                                                                                                0, 135,
                                                                                                                Short.MAX_VALUE)
                                                                                                .addGap(18, 18, 18))
                                                                                .addComponent(jLabel12,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE))
                                                                .addGap(18, 18, 18)
                                                                .addComponent(btnFiltrar,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                241,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addContainerGap()));
                panelFiltLayout.setVerticalGroup(
                                panelFiltLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelFiltLayout.createSequentialGroup()
                                                                .addGap(20, 20, 20)
                                                                .addGroup(
                                                                                panelFiltLayout.createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                                .addComponent(jLabel10)
                                                                                                .addComponent(jLabel11)
                                                                                                .addComponent(jLabel12,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                Short.MAX_VALUE))
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addGroup(panelFiltLayout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                false)
                                                                                .addComponent(cbxRango)
                                                                                .addComponent(cbxEstado)
                                                                                .addComponent(txtNumero)
                                                                                .addComponent(btnFiltrar,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                40,
                                                                                                Short.MAX_VALUE))
                                                                .addContainerGap(19, Short.MAX_VALUE)));

                panelAcR.setBackground(new java.awt.Color(153, 255, 153));

                jLabel14.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
                jLabel14.setText("Acciones Rápidas ");

                jButton3.setText("+");
                jButton3.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                jButton3ActionPerformed(evt);
                        }
                });

                jLabel15.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
                jLabel15.setText("Nueva devolución");

                jLabel16.setText("crear solicitud");

                javax.swing.GroupLayout paneNuevo1Layout = new javax.swing.GroupLayout(paneNuevo1);
                paneNuevo1.setLayout(paneNuevo1Layout);
                paneNuevo1Layout.setHorizontalGroup(
                                paneNuevo1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(paneNuevo1Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(jButton3,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                50,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addGroup(
                                                                                paneNuevo1Layout.createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                .addComponent(jLabel15,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                Short.MAX_VALUE)
                                                                                                .addComponent(jLabel16,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                Short.MAX_VALUE))
                                                                .addContainerGap()));
                paneNuevo1Layout.setVerticalGroup(
                                paneNuevo1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(paneNuevo1Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addGroup(paneNuevo1Layout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(jButton3,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addGroup(paneNuevo1Layout
                                                                                                .createSequentialGroup()
                                                                                                .addComponent(jLabel15,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                30,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addPreferredGap(
                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                Short.MAX_VALUE)
                                                                                                .addComponent(jLabel16)))
                                                                .addContainerGap()));

                jButton4.setText("+");

                jLabel17.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
                jLabel17.setText("Generar Reporte");

                jLabel18.setText("Análisis de periodo");

                javax.swing.GroupLayout panelGenReportLayout = new javax.swing.GroupLayout(panelGenReport);
                panelGenReport.setLayout(panelGenReportLayout);
                panelGenReportLayout.setHorizontalGroup(
                                panelGenReportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelGenReportLayout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(jButton4,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                51,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addGroup(panelGenReportLayout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(jLabel17,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(jLabel18,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE))
                                                                .addContainerGap()));
                panelGenReportLayout.setVerticalGroup(
                                panelGenReportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelGenReportLayout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addGroup(panelGenReportLayout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(jButton4,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addGroup(panelGenReportLayout
                                                                                                .createSequentialGroup()
                                                                                                .addComponent(jLabel17,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                30,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addPreferredGap(
                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                Short.MAX_VALUE)
                                                                                                .addComponent(jLabel18)))
                                                                .addContainerGap()));

                javax.swing.GroupLayout panelAcRLayout = new javax.swing.GroupLayout(panelAcR);
                panelAcR.setLayout(panelAcRLayout);
                panelAcRLayout.setHorizontalGroup(
                                panelAcRLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelAcRLayout.createSequentialGroup()
                                                                .addGap(16, 16, 16)
                                                                .addGroup(panelAcRLayout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                false)
                                                                                .addComponent(jLabel14,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(paneNuevo1,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(panelGenReport,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE))
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));
                panelAcRLayout.setVerticalGroup(
                                panelAcRLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelAcRLayout.createSequentialGroup()
                                                                .addGap(16, 16, 16)
                                                                .addComponent(jLabel14,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(paneNuevo1,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(panelGenReport,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addContainerGap(66, Short.MAX_VALUE)));

                panelDevo.setBackground(new java.awt.Color(153, 255, 153));

                jLabel13.setText("Devoluciones Recientes ");

                javax.swing.GroupLayout panelTitulodevoLayout = new javax.swing.GroupLayout(panelTitulodevo);
                panelTitulodevo.setLayout(panelTitulodevoLayout);
                panelTitulodevoLayout.setHorizontalGroup(
                                panelTitulodevoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelTitulodevoLayout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(jLabel13,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addContainerGap()));
                panelTitulodevoLayout.setVerticalGroup(
                                panelTitulodevoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelTitulodevoLayout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(jLabel13,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                42, Short.MAX_VALUE)
                                                                .addContainerGap()));

                tablaDevoluciones.setModel(new javax.swing.table.DefaultTableModel(
                                new Object[][] {

                                },
                                new String[] {
                                                "", ""
                                }) {
                        boolean[] canEdit = new boolean[] {
                                        true, false
                        };

                        public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return canEdit[columnIndex];
                        }
                });
                jScrollPane1.setViewportView(tablaDevoluciones);

                javax.swing.GroupLayout panelDevoLayout = new javax.swing.GroupLayout(panelDevo);
                panelDevo.setLayout(panelDevoLayout);
                panelDevoLayout.setHorizontalGroup(
                                panelDevoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(panelTitulodevo, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
                panelDevoLayout.setVerticalGroup(
                                panelDevoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelDevoLayout.createSequentialGroup()
                                                                .addComponent(panelTitulodevo,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addComponent(jScrollPane1)
                                                                .addGap(0, 0, Short.MAX_VALUE)));

                panelAlerts.setBackground(new java.awt.Color(153, 255, 153));

                jLabel19.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
                jLabel19.setText("Alertas");

                jLabel20.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
                jLabel20.setForeground(new java.awt.Color(102, 77, 3));
                jLabel20.setText("Autorización Pendiente ");

                txtAutoPendientes.setForeground(new java.awt.Color(102, 77, 3));
                txtAutoPendientes.setText("0 devoluciones requieren aprobación gerencial ");

                javax.swing.GroupLayout panelApendiLayout = new javax.swing.GroupLayout(panelApendi);
                panelApendi.setLayout(panelApendiLayout);
                panelApendiLayout.setHorizontalGroup(
                                panelApendiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelApendiLayout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addGroup(
                                                                                panelApendiLayout.createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                .addComponent(jLabel20,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                Short.MAX_VALUE)
                                                                                                .addComponent(txtAutoPendientes,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                Short.MAX_VALUE))
                                                                .addContainerGap()));
                panelApendiLayout.setVerticalGroup(
                                panelApendiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelApendiLayout.createSequentialGroup()
                                                                .addGap(16, 16, 16)
                                                                .addComponent(jLabel20,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(txtAutoPendientes,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addContainerGap(16, Short.MAX_VALUE)));

                jLabel22.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
                jLabel22.setForeground(new java.awt.Color(5, 81, 96));
                jLabel22.setText("Notas de Crédito ");

                txtFechaNotas.setForeground(new java.awt.Color(5, 81, 96));
                txtFechaNotas.setText("0 notas vencen en ..");

                javax.swing.GroupLayout panelAnotsFvLayout = new javax.swing.GroupLayout(panelAnotsFv);
                panelAnotsFv.setLayout(panelAnotsFvLayout);
                panelAnotsFvLayout.setHorizontalGroup(
                                panelAnotsFvLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelAnotsFvLayout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addGroup(panelAnotsFvLayout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(jLabel22,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(txtFechaNotas,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                408,
                                                                                                Short.MAX_VALUE))
                                                                .addContainerGap()));
                panelAnotsFvLayout.setVerticalGroup(
                                panelAnotsFvLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelAnotsFvLayout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(jLabel22,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(txtFechaNotas,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addContainerGap(16, Short.MAX_VALUE)));

                txtStockAltoDevolucion.setForeground(new java.awt.Color(132, 32, 41));
                txtStockAltoDevolucion.setText("producto");

                jLabel25.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
                jLabel25.setForeground(new java.awt.Color(132, 32, 41));
                jLabel25.setText("Stock Alto ");

                javax.swing.GroupLayout panelStAlLayout = new javax.swing.GroupLayout(panelStAl);
                panelStAl.setLayout(panelStAlLayout);
                panelStAlLayout.setHorizontalGroup(
                                panelStAlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelStAlLayout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addGroup(panelStAlLayout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(jLabel25,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(txtStockAltoDevolucion,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                408,
                                                                                                Short.MAX_VALUE))
                                                                .addContainerGap()));
                panelStAlLayout.setVerticalGroup(
                                panelStAlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelStAlLayout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(jLabel25,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(txtStockAltoDevolucion,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addContainerGap(16, Short.MAX_VALUE)));

                javax.swing.GroupLayout panelAlertsLayout = new javax.swing.GroupLayout(panelAlerts);
                panelAlerts.setLayout(panelAlertsLayout);
                panelAlertsLayout.setHorizontalGroup(
                                panelAlertsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelAlertsLayout.createSequentialGroup()
                                                                .addGap(16, 16, 16)
                                                                .addGroup(panelAlertsLayout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(jLabel19,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addGroup(panelAlertsLayout
                                                                                                .createSequentialGroup()
                                                                                                .addGroup(panelAlertsLayout
                                                                                                                .createParallelGroup(
                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                                                false)
                                                                                                                .addComponent(panelStAl,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                Short.MAX_VALUE)
                                                                                                                .addComponent(panelAnotsFv,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                Short.MAX_VALUE)
                                                                                                                .addComponent(panelApendi,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                Short.MAX_VALUE))
                                                                                                .addGap(0, 0, Short.MAX_VALUE)))
                                                                .addContainerGap()));
                panelAlertsLayout.setVerticalGroup(
                                panelAlertsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelAlertsLayout.createSequentialGroup()
                                                                .addGap(14, 14, 14)
                                                                .addComponent(jLabel19,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(panelApendi,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(panelAnotsFv,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(panelStAl,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addContainerGap(40, Short.MAX_VALUE)));

                javax.swing.GroupLayout panelMainLayout = new javax.swing.GroupLayout(panelMain);
                panelMain.setLayout(panelMainLayout);
                panelMainLayout.setHorizontalGroup(
                                panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelMainLayout.createSequentialGroup()
                                                                .addGap(18, 18, 18)
                                                                .addGroup(panelMainLayout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addGroup(panelMainLayout
                                                                                                .createSequentialGroup()
                                                                                                .addGroup(panelMainLayout
                                                                                                                .createParallelGroup(
                                                                                                                                javax.swing.GroupLayout.Alignment.TRAILING)
                                                                                                                .addComponent(panelDevo,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                Short.MAX_VALUE)
                                                                                                                .addComponent(panelFilt,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                Short.MAX_VALUE))
                                                                                                .addGap(18, 18, 18)
                                                                                                .addGroup(panelMainLayout
                                                                                                                .createParallelGroup(
                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                                .addComponent(panelAlerts,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                Short.MAX_VALUE)
                                                                                                                .addComponent(panelAcR,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                Short.MAX_VALUE)))
                                                                                .addComponent(panelTitulo,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(panelSt,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE))
                                                                .addGap(22, 22, 22)));
                panelMainLayout.setVerticalGroup(
                                panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelMainLayout.createSequentialGroup()
                                                                .addGap(18, 18, 18)
                                                                .addComponent(panelTitulo,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(panelSt,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addGroup(panelMainLayout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                false)
                                                                                .addGroup(panelMainLayout
                                                                                                .createSequentialGroup()
                                                                                                .addComponent(panelAcR,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                Short.MAX_VALUE)
                                                                                                .addGap(18, 18, 18)
                                                                                                .addComponent(panelAlerts,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                Short.MAX_VALUE))
                                                                                .addGroup(panelMainLayout
                                                                                                .createSequentialGroup()
                                                                                                .addComponent(panelFilt,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                Short.MAX_VALUE)
                                                                                                .addPreferredGap(
                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                                                .addComponent(panelDevo,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                Short.MAX_VALUE)))
                                                                .addContainerGap(20, Short.MAX_VALUE)));

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
                this.setLayout(layout);
                layout.setHorizontalGroup(
                                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(panelMain, javax.swing.GroupLayout.Alignment.TRAILING,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                Short.MAX_VALUE));
                layout.setVerticalGroup(
                                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(panelMain, javax.swing.GroupLayout.Alignment.TRAILING,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                Short.MAX_VALUE));
        }// </editor-fold>//GEN-END:initComponents

        private void btnNueva1ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnNueva1ActionPerformed
                abrirNuevaDevolucion();
        }// GEN-LAST:event_btnNueva1ActionPerformed

        private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton3ActionPerformed
                abrirNuevaDevolucion();
        }// GEN-LAST:event_jButton3ActionPerformed

        /**
         * Abre el formulario de nueva devolución
         */
        private void abrirNuevaDevolucion() {
                try {
                        nuevaDevolucion123 formularioNuevo = new nuevaDevolucion123();
                        panelMain.setLayout(new BorderLayout());
                        panelMain.removeAll();
                        panelMain.add(formularioNuevo, BorderLayout.CENTER);
                        panelMain.revalidate();
                        panelMain.repaint();
                        System.out.println("SUCCESS  Formulario de nueva devolución abierto");
                } catch (Exception e) {
                        System.err.println("ERROR  Error abriendo nueva devolución: " + e.getMessage());
                        mostrarError("Error abriendo formulario de nueva devolución: " + e.getMessage());
                }
        }

        private void btnFiltrarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnFiltrarActionPerformed
                // TODO add your handling code here:
        }// GEN-LAST:event_btnFiltrarActionPerformed

        private void btnAutorizarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnAutorizarActionPerformed
                autorizarDevolucionSeleccionada();
        }// GEN-LAST:event_btnAutorizarActionPerformed

        // Declara un método que devuelve una lista de objetos ModelProduct

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton btnAutorizar;
        private javax.swing.JButton btnFiltrar;
        private javax.swing.JButton btnNueva1;
        private javax.swing.JComboBox<String> cbxEstado;
        private javax.swing.JComboBox<String> cbxRango;
        private javax.swing.JButton jButton3;
        private javax.swing.JButton jButton4;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel10;
        private javax.swing.JLabel jLabel11;
        private javax.swing.JLabel jLabel12;
        private javax.swing.JLabel jLabel13;
        private javax.swing.JLabel jLabel14;
        private javax.swing.JLabel jLabel15;
        private javax.swing.JLabel jLabel16;
        private javax.swing.JLabel jLabel17;
        private javax.swing.JLabel jLabel18;
        private javax.swing.JLabel jLabel19;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel20;
        private javax.swing.JLabel jLabel22;
        private javax.swing.JLabel jLabel25;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JLabel jLabel6;
        private javax.swing.JLabel jLabel7;
        private javax.swing.JLabel jLabel8;
        private javax.swing.JLabel jLabel9;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JPanel paneNuevo1;
        private javax.swing.JPanel panelAcR;
        private javax.swing.JPanel panelAlerts;
        private javax.swing.JPanel panelAnotsFv;
        private javax.swing.JPanel panelApendi;
        private javax.swing.JPanel panelDevo;
        private javax.swing.JPanel panelFilt;
        private javax.swing.JPanel panelGenReport;
        private javax.swing.JPanel panelMain;
        private javax.swing.JPanel panelSt;
        private javax.swing.JPanel panelSt1;
        private javax.swing.JPanel panelSt2;
        private javax.swing.JPanel panelSt3;
        private javax.swing.JPanel panelSt4;
        private javax.swing.JPanel panelStAl;
        private javax.swing.JPanel panelTitulo;
        private javax.swing.JPanel panelTitulodevo;
        private javax.swing.JTable tablaDevoluciones;
        private javax.swing.JLabel txtAutoPendientes;
        private javax.swing.JLabel txtFechaNotas;
        private javax.swing.JTextField txtNumero;
        private javax.swing.JLabel txtStockAltoDevolucion;
        // End of variables declaration//GEN-END:variables
}
