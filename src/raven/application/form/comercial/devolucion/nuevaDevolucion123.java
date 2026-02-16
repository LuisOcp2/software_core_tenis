/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package raven.application.form.comercial.devolucion;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.UIManager;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import raven.application.form.comercial.devolucion.components.StepperPanel;
import raven.application.form.comercial.devolucion.components.DesignConstants;

/**
 *
 * @author lmog2
 */
public class nuevaDevolucion123 extends javax.swing.JPanel {

        private static final String Camposdetexto = "arc:15;background:lighten($Menu.background,25%)";
        private static final String PANEL = "arc:35;background:$Login.background";
        private static final String PANELTitle = "arc:35;background:lighten($Menu.background,10%)";
        private static final String FONT_HEADER_STYLE = "font:$h1.font";
        private static final String FONT_SUBHEADER_STYLE = "font:$h2.font";
        private static final String btnRounded = "arc:100;background:darken($Login.background,10%);";

        private boolean inicializadoMVC = false;
        private buscarVenta buscarForm;
        private StepperPanel stepperPanel;

        public nuevaDevolucion123() {
                initComponents();
                cargarUI();
                // AGREGAR MVC
                inicializarMVC();
                // Diseño de Botones
                actualizarBarraPasos(1);
        }

        /**
         * Inicializa el MVC para este formulario
         */
        private void inicializarMVC() {
                if (inicializadoMVC) {
                        return;
                }

                try {
                        // Configurar el panel cambiante con layout correcto
                        configurarPanelCambiante();

                        // Inicializar formulario de búsqueda con MVC
                        cargarBuscarVentaConMVC();

                        inicializadoMVC = true;
                        System.out.println("SUCCESS  MVC inicializado en nuevaDevolucion123");

                } catch (Exception e) {
                        System.err.println("ERROR  Error inicializando MVC: " + e.getMessage());
                }
        }

        /**
         * Configura el panel que cambia de contenido
         */
        private void configurarPanelCambiante() {
                if (panelCambiante != null) {
                        panelCambiante.setLayout(new BorderLayout());
                }
        }

        private void cargarBuscarVentaConMVC() {
                try {
                        // Crear instancia de búsqueda con MVC
                        buscarForm = new buscarVenta();
                        buscarForm.setController(this); // Inject controller

                        // Agregar al panel cambiante
                        if (panelCambiante != null) {
                                panelCambiante.removeAll();
                                panelCambiante.add(buscarForm, BorderLayout.CENTER);
                                panelCambiante.revalidate();
                                panelCambiante.repaint();
                        }

                } catch (Exception e) {
                        System.err.println("ERROR  Error cargando buscarVenta con MVC: " + e.getMessage());
                }
        }

        public void cargarUI() {
                // Configuraciones de estilo existentes
                panelFondo.putClientProperty(FlatClientProperties.STYLE,
                                "arc:35;background:darken($Login.background,10%);");
                panelPasos.putClientProperty(FlatClientProperties.STYLE, PANEL);
                jPanel5.putClientProperty(FlatClientProperties.STYLE, PANEL);
                jPanel7.putClientProperty(FlatClientProperties.STYLE, PANEL);
                jPanel8.putClientProperty(FlatClientProperties.STYLE, PANEL);
                btnBuscarVenta.putClientProperty(FlatClientProperties.STYLE, btnRounded);
                btnSelect.putClientProperty(FlatClientProperties.STYLE, btnRounded);
                btnConfirmar.putClientProperty(FlatClientProperties.STYLE, btnRounded);

                // CONFIGURAR CORRECTAMENTE EL LAYOUT DEL PANEL CAMBIANTE
                panelCambiante.setLayout(new BorderLayout());
                // El contenido inicial se carga en inicializarMVC

                // Inicializar Stepper
                stepperPanel = new StepperPanel("Buscar Venta", "Seleccionar", "Confirmar");
                panelPasos.removeAll();
                panelPasos.setLayout(new BorderLayout());
                panelPasos.add(stepperPanel, BorderLayout.CENTER);
                panelPasos.revalidate();
                panelPasos.repaint();

                // Aplicar tema oscuro
                panelFondo.setOpaque(true);
                panelFondo.setBackground(DesignConstants.BG_DARK);

                // FIX: Make all panels responsive
                configurarLayoutResponsivo();
        }

        // ====================================================================
        // NAVEGACIÓN Y CONTROL DE PASOS
        // ====================================================================

        public void irAPaso1() {
                actualizarBarraPasos(1);
                cargarBuscarVentaConMVC();
        }

        public void irAPaso2(raven.controlador.principal.ModelVenta venta,
                        java.util.List<raven.controlador.principal.ModelDetalleVenta> productos) {
                actualizarBarraPasos(2);
                try {
                        SeleccionarProductos p2 = new SeleccionarProductos(venta, productos);
                        p2.setController(this); // Controlador necesario para volver atrás

                        panelCambiante.removeAll();
                        panelCambiante.add(p2, BorderLayout.CENTER);
                        panelCambiante.revalidate();
                        panelCambiante.repaint();
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }

        public void irAPaso3() {
                actualizarBarraPasos(3);
                // Mostrar panel de éxito
                javax.swing.JPanel panelExito = new javax.swing.JPanel(new BorderLayout());
                panelExito.putClientProperty(FlatClientProperties.STYLE, "background:$Login.background");

                javax.swing.JLabel lblExito = new javax.swing.JLabel("Devolución Completada Exitosamente");
                lblExito.putClientProperty(FlatClientProperties.STYLE,
                                "font:$h1.font;foreground:$Component.accentColor");
                lblExito.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

                javax.swing.JLabel lblSub = new javax.swing.JLabel("Se ha generado la nota de crédito correctamente.");
                lblSub.putClientProperty(FlatClientProperties.STYLE, "font:$h3.font");
                lblSub.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

                javax.swing.JButton btnInicio = new javax.swing.JButton("Volver al Inicio");
                btnInicio.putClientProperty(FlatClientProperties.STYLE, btnRounded + "foreground:#ffffff;font:bold +2");
                btnInicio.addActionListener(e -> irAPaso1());

                javax.swing.JPanel centerPanel = new javax.swing.JPanel(new java.awt.GridLayout(3, 1, 10, 10));
                centerPanel.setOpaque(false);
                centerPanel.add(lblExito);
                centerPanel.add(lblSub);

                javax.swing.JPanel btnPanel = new javax.swing.JPanel();
                btnPanel.setOpaque(false);
                btnPanel.add(btnInicio);
                centerPanel.add(btnPanel);

                panelExito.add(centerPanel, BorderLayout.CENTER);

                panelCambiante.removeAll();
                panelCambiante.add(panelExito, BorderLayout.CENTER);
                panelCambiante.revalidate();
                panelCambiante.repaint();
        }

        public void actualizarBarraPasos(int paso) {
                if (stepperPanel != null) {
                        stepperPanel.setCurrentStep(paso);
                }
        }

        /**
         * Configura los layouts para ser responsivos eliminando tamaños fijos
         */
        private void configurarLayoutResponsivo() {
                // Remover tamaños fijos de paneles principales y agregar padding
                panelFondo.setPreferredSize(null);
                panelFondo.setMaximumSize(null);
                panelFondo.setMinimumSize(new java.awt.Dimension(0, 0));
                panelFondo.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));

                panelPasos.setPreferredSize(null);
                panelPasos.setMaximumSize(null);
                panelPasos.setMinimumSize(new java.awt.Dimension(0, 0));
                panelPasos.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 20, 12, 20));

                panelCambiante.setPreferredSize(null);
                panelCambiante.setMaximumSize(null);
                panelCambiante.setMinimumSize(new java.awt.Dimension(0, 0));
                panelCambiante.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));

                // Configurar líneas conectoras para que sean flexibles
                jPanel4.setPreferredSize(null);
                jPanel4.setMaximumSize(new java.awt.Dimension(Short.MAX_VALUE, 11));
                jPanel4.setMinimumSize(new java.awt.Dimension(50, 11));

                jPanel6.setPreferredSize(null);
                jPanel6.setMaximumSize(new java.awt.Dimension(Short.MAX_VALUE, 11));
                jPanel6.setMinimumSize(new java.awt.Dimension(50, 11));

                System.out.println("✓ Layout responsivo y UI/UX mejorado para nuevaDevolucion123");
        }

        @SuppressWarnings("unchecked")
        // <editor-fold defaultstate="collapsed" desc="Generated
        // Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                jScrollPane3 = new javax.swing.JScrollPane();
                sPanePrincip = new javax.swing.JPanel();
                jScrollPane4 = new javax.swing.JScrollPane();
                panelPrinci = new javax.swing.JPanel();
                jLabel15 = new javax.swing.JLabel();
                jLabel24 = new javax.swing.JLabel();
                panelPoliticas1 = new javax.swing.JPanel();
                jLabel26 = new javax.swing.JLabel();
                jLabel27 = new javax.swing.JLabel();
                jLabel30 = new javax.swing.JLabel();
                jLabel31 = new javax.swing.JLabel();
                jLabel33 = new javax.swing.JLabel();
                jPanel1 = new javax.swing.JPanel();
                jLabel34 = new javax.swing.JLabel();
                txtNVenta1 = new javax.swing.JTextField();
                jLabel35 = new javax.swing.JLabel();
                btnBuscarVenta1 = new javax.swing.JButton();
                panelPoliticas2 = new javax.swing.JPanel();
                jLabel28 = new javax.swing.JLabel();
                jLabel29 = new javax.swing.JLabel();
                jLabel32 = new javax.swing.JLabel();
                jLabel41 = new javax.swing.JLabel();
                jLabel42 = new javax.swing.JLabel();
                panelVentaDescripcion1 = new javax.swing.JPanel();
                jLabel36 = new javax.swing.JLabel();
                txtFechaVenta1 = new javax.swing.JLabel();
                jLabel37 = new javax.swing.JLabel();
                txtCliente1 = new javax.swing.JLabel();
                txtTotal1 = new javax.swing.JLabel();
                jLabel38 = new javax.swing.JLabel();
                jLabel39 = new javax.swing.JLabel();
                txtNProd1 = new javax.swing.JLabel();
                txtEstado1 = new javax.swing.JLabel();
                jLabel40 = new javax.swing.JLabel();
                panelFondo = new javax.swing.JPanel();
                jLabel16 = new javax.swing.JLabel();
                panelPasos = new javax.swing.JPanel();
                jPanel4 = new javax.swing.JPanel();
                jPanel5 = new javax.swing.JPanel();
                btnSelect = new javax.swing.JButton();
                jLabel2 = new javax.swing.JLabel();
                jPanel6 = new javax.swing.JPanel();
                jPanel8 = new javax.swing.JPanel();
                btnConfirmar = new javax.swing.JButton();
                jLabel4 = new javax.swing.JLabel();
                jPanel7 = new javax.swing.JPanel();
                btnBuscarVenta = new javax.swing.JButton();
                jLabel3 = new javax.swing.JLabel();
                panelCambiante = new javax.swing.JPanel();

                jScrollPane3.setBackground(new java.awt.Color(204, 255, 204));
                jScrollPane3.setBorder(null);

                panelPrinci.setBackground(new java.awt.Color(255, 153, 153));

                jLabel15.setFont(new java.awt.Font("Arial", 1, 30)); // NOI18N
                jLabel15.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel15.setText("Nueva Devolución");

                jLabel24.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
                jLabel24.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel24.setText("Complete los datos para iniciar el proceso ");

                jLabel26.setText("Políticas de Devolución ");

                jLabel27.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
                jLabel27.setText("Tiempo límite: 30 días desde la compra ");

                jLabel30.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
                jLabel30.setText("Devoluciones > $500 requieren autorización gerencial ");

                jLabel31.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
                jLabel31.setText("Productos deben conservar empaque y etiquetas originales ");

                jLabel33.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
                jLabel33.setText("Se genera nota de crédito válida por 30 dias");

                javax.swing.GroupLayout panelPoliticas1Layout = new javax.swing.GroupLayout(panelPoliticas1);
                panelPoliticas1.setLayout(panelPoliticas1Layout);
                panelPoliticas1Layout.setHorizontalGroup(
                                panelPoliticas1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelPoliticas1Layout.createSequentialGroup()
                                                                .addGap(18, 18, 18)
                                                                .addGroup(panelPoliticas1Layout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(jLabel33,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(jLabel31,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(jLabel30,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(jLabel26,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(jLabel27,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE))
                                                                .addContainerGap()));
                panelPoliticas1Layout.setVerticalGroup(
                                panelPoliticas1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelPoliticas1Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(jLabel26,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                32, Short.MAX_VALUE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel27,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                36, Short.MAX_VALUE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel30,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                36, Short.MAX_VALUE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel31,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                36, Short.MAX_VALUE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addComponent(jLabel33,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                36, Short.MAX_VALUE)));

                jLabel34.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
                jLabel34.setText("1. Identificación de la Venta ");

                txtNVenta1.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                txtNVenta1ActionPerformed(evt);
                        }
                });

                jLabel35.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
                jLabel35.setText("Número de Venta * ");

                btnBuscarVenta1.setText("Buscar");
                btnBuscarVenta1.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                btnBuscarVenta1ActionPerformed(evt);
                        }
                });

                jLabel28.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel28.setText("Políticas de Devolución ");

                jLabel29.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
                jLabel29.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel29.setText("Tiempo límite: 30 días desde la compra ");

                jLabel32.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
                jLabel32.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel32.setText("Devoluciones > $500 requieren autorización gerencial ");

                jLabel41.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
                jLabel41.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel41.setText("Productos deben conservar empaque y etiquetas originales ");

                jLabel42.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
                jLabel42.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel42.setText("Se genera nota de crédito válida por 30 dias");

                javax.swing.GroupLayout panelPoliticas2Layout = new javax.swing.GroupLayout(panelPoliticas2);
                panelPoliticas2.setLayout(panelPoliticas2Layout);
                panelPoliticas2Layout.setHorizontalGroup(
                                panelPoliticas2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelPoliticas2Layout.createSequentialGroup()
                                                                .addGap(18, 18, 18)
                                                                .addGroup(panelPoliticas2Layout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(jLabel42,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(jLabel41,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(jLabel32,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(jLabel28,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(jLabel29,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE))
                                                                .addContainerGap()));
                panelPoliticas2Layout.setVerticalGroup(
                                panelPoliticas2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelPoliticas2Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(jLabel28,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                32, Short.MAX_VALUE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel29,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                36, Short.MAX_VALUE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel32,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                36, Short.MAX_VALUE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel41,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                36, Short.MAX_VALUE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addComponent(jLabel42,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                36, Short.MAX_VALUE)));

                javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
                jPanel1.setLayout(jPanel1Layout);
                jPanel1Layout.setHorizontalGroup(
                                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel1Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addGroup(jPanel1Layout.createParallelGroup(
                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(jLabel34,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(jLabel35,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(txtNVenta1)
                                                                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                                                                                jPanel1Layout.createSequentialGroup()
                                                                                                                .addGap(0, 0, Short.MAX_VALUE)
                                                                                                                .addComponent(btnBuscarVenta1,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                84,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                                .addContainerGap())
                                                .addGroup(jPanel1Layout.createParallelGroup(
                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                .addGroup(jPanel1Layout.createSequentialGroup()
                                                                                .addContainerGap()
                                                                                .addComponent(panelPoliticas2,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addContainerGap())));
                jPanel1Layout.setVerticalGroup(
                                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel1Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(jLabel34,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                40,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel35)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(txtNVenta1,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                50,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(btnBuscarVenta1)
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE))
                                                .addGroup(jPanel1Layout.createParallelGroup(
                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                                                                jPanel1Layout.createSequentialGroup()
                                                                                                .addContainerGap(
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                Short.MAX_VALUE)
                                                                                                .addComponent(panelPoliticas2,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addContainerGap())));

                jLabel36.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
                jLabel36.setText("Venta Encontrada ");

                txtFechaVenta1.setText("14 de noviembre de 2025 ");

                jLabel37.setText("Cliente: ");

                txtCliente1.setText("María González Rodríguez ");

                txtTotal1.setText("$ 1.250,00 ");

                jLabel38.setText("Total:");

                jLabel39.setText("Productos: ");

                txtNProd1.setText("2 producto(s) ");

                txtEstado1.setText("Completada ");

                jLabel40.setText("Esrado");

                javax.swing.GroupLayout panelVentaDescripcion1Layout = new javax.swing.GroupLayout(
                                panelVentaDescripcion1);
                panelVentaDescripcion1.setLayout(panelVentaDescripcion1Layout);
                panelVentaDescripcion1Layout.setHorizontalGroup(
                                panelVentaDescripcion1Layout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelVentaDescripcion1Layout.createSequentialGroup()
                                                                .addGap(26, 26, 26)
                                                                .addGroup(panelVentaDescripcion1Layout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addGroup(panelVentaDescripcion1Layout
                                                                                                .createSequentialGroup()
                                                                                                .addComponent(jLabel36)
                                                                                                .addPreferredGap(
                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                .addComponent(txtFechaVenta1)
                                                                                                .addContainerGap(
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                Short.MAX_VALUE))
                                                                                .addGroup(panelVentaDescripcion1Layout
                                                                                                .createSequentialGroup()
                                                                                                .addGroup(panelVentaDescripcion1Layout
                                                                                                                .createParallelGroup(
                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                                .addComponent(txtCliente1)
                                                                                                                .addComponent(jLabel37))
                                                                                                .addPreferredGap(
                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                Short.MAX_VALUE)
                                                                                                .addGroup(panelVentaDescripcion1Layout
                                                                                                                .createParallelGroup(
                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                                .addComponent(txtTotal1)
                                                                                                                .addComponent(jLabel38))
                                                                                                .addGap(44, 44, 44)
                                                                                                .addGroup(panelVentaDescripcion1Layout
                                                                                                                .createParallelGroup(
                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                                .addComponent(jLabel39)
                                                                                                                .addComponent(txtNProd1))
                                                                                                .addGap(115, 115, 115))
                                                                                .addGroup(panelVentaDescripcion1Layout
                                                                                                .createSequentialGroup()
                                                                                                .addGroup(panelVentaDescripcion1Layout
                                                                                                                .createParallelGroup(
                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                                .addComponent(txtEstado1)
                                                                                                                .addComponent(jLabel40))
                                                                                                .addGap(0, 0, Short.MAX_VALUE)))));
                panelVentaDescripcion1Layout.setVerticalGroup(
                                panelVentaDescripcion1Layout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelVentaDescripcion1Layout.createSequentialGroup()
                                                                .addContainerGap(15, Short.MAX_VALUE)
                                                                .addGroup(panelVentaDescripcion1Layout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.BASELINE)
                                                                                .addComponent(jLabel36,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                40,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addComponent(txtFechaVenta1,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                40,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addGroup(panelVentaDescripcion1Layout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.TRAILING,
                                                                                                false)
                                                                                .addGroup(panelVentaDescripcion1Layout
                                                                                                .createSequentialGroup()
                                                                                                .addComponent(jLabel39,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                28,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addPreferredGap(
                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                .addComponent(txtNProd1,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                Short.MAX_VALUE))
                                                                                .addGroup(panelVentaDescripcion1Layout
                                                                                                .createSequentialGroup()
                                                                                                .addComponent(jLabel38,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                28,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addPreferredGap(
                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                .addComponent(txtTotal1,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                Short.MAX_VALUE))
                                                                                .addGroup(panelVentaDescripcion1Layout
                                                                                                .createSequentialGroup()
                                                                                                .addComponent(jLabel37,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                28,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addPreferredGap(
                                                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                                                .addComponent(txtCliente1)))
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addComponent(jLabel40,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                16,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(txtEstado1)
                                                                .addGap(36, 36, 36)));

                javax.swing.GroupLayout panelPrinciLayout = new javax.swing.GroupLayout(panelPrinci);
                panelPrinci.setLayout(panelPrinciLayout);
                panelPrinciLayout.setHorizontalGroup(
                                panelPrinciLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jLabel24, javax.swing.GroupLayout.Alignment.TRAILING,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                Short.MAX_VALUE)
                                                .addComponent(jLabel15, javax.swing.GroupLayout.Alignment.TRAILING,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                Short.MAX_VALUE)
                                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelPrinciLayout
                                                                .createSequentialGroup()
                                                                .addContainerGap(181, Short.MAX_VALUE)
                                                                .addGroup(panelPrinciLayout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                false)
                                                                                .addComponent(panelPoliticas1,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(jPanel1,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE)
                                                                                .addComponent(panelVentaDescripcion1,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                0,
                                                                                                Short.MAX_VALUE))
                                                                .addGap(151, 151, 151)));
                panelPrinciLayout.setVerticalGroup(
                                panelPrinciLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelPrinciLayout.createSequentialGroup()
                                                                .addComponent(jLabel15,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                46,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel24)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(panelPoliticas1,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(jPanel1,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(panelVentaDescripcion1,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(0, 349, Short.MAX_VALUE)));

                jScrollPane4.setViewportView(panelPrinci);

                javax.swing.GroupLayout sPanePrincipLayout = new javax.swing.GroupLayout(sPanePrincip);
                sPanePrincip.setLayout(sPanePrincipLayout);
                sPanePrincipLayout.setHorizontalGroup(
                                sPanePrincipLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(sPanePrincipLayout.createSequentialGroup()
                                                                .addGap(350, 350, 350)
                                                                .addComponent(jScrollPane4,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                720,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(464, 464, 464)));
                sPanePrincipLayout.setVerticalGroup(
                                sPanePrincipLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(sPanePrincipLayout.createSequentialGroup()
                                                                .addComponent(jScrollPane4)
                                                                .addContainerGap()));

                jScrollPane3.setViewportView(sPanePrincip);

                panelFondo.setBackground(new java.awt.Color(255, 204, 204));

                jLabel16.setFont(new java.awt.Font("Arial", 1, 30)); // NOI18N
                jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel16.setText("Nueva Devolución");

                panelPasos.setBackground(new java.awt.Color(204, 255, 204));

                javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
                jPanel4.setLayout(jPanel4Layout);
                jPanel4Layout.setHorizontalGroup(
                                jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGap(0, 0, Short.MAX_VALUE));
                jPanel4Layout.setVerticalGroup(
                                jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGap(0, 11, Short.MAX_VALUE));

                btnSelect.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                btnSelectActionPerformed(evt);
                        }
                });

                jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel2.setText("<html><div align='center'>Seleccionar<br>Productos</div></html>");

                javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
                jPanel5.setLayout(jPanel5Layout);
                jPanel5Layout.setHorizontalGroup(
                                jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel5Layout.createSequentialGroup()
                                                                .addGap(26, 26, 26)
                                                                .addGroup(jPanel5Layout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                false)
                                                                                .addComponent(btnSelect,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                68,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addComponent(jLabel2))
                                                                .addGap(26, 26, 26)));
                jPanel5Layout.setVerticalGroup(
                                jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel5Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(btnSelect,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                68,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel2,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));

                javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
                jPanel6.setLayout(jPanel6Layout);
                jPanel6Layout.setHorizontalGroup(
                                jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGap(0, 0, Short.MAX_VALUE));
                jPanel6Layout.setVerticalGroup(
                                jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGap(0, 11, Short.MAX_VALUE));

                btnConfirmar.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                btnConfirmarActionPerformed(evt);
                        }
                });

                jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel4.setText("Confirmar");

                javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
                jPanel8.setLayout(jPanel8Layout);
                jPanel8Layout.setHorizontalGroup(
                                jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel8Layout.createSequentialGroup()
                                                                .addGap(26, 26, 26)
                                                                .addGroup(jPanel8Layout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                false)
                                                                                .addComponent(btnConfirmar,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                68,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addComponent(jLabel4,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                Short.MAX_VALUE))
                                                                .addGap(26, 26, 26)));
                jPanel8Layout.setVerticalGroup(
                                jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel8Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(btnConfirmar,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                68,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel4)
                                                                .addGap(22, 22, 22)));

                btnBuscarVenta.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                btnBuscarVentaActionPerformed(evt);
                        }
                });

                jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel3.setText("<html><div align='center'>Buscar<br>Venta</div></html>");

                javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
                jPanel7.setLayout(jPanel7Layout);
                jPanel7Layout.setHorizontalGroup(
                                jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel7Layout.createSequentialGroup()
                                                                .addGap(26, 26, 26)
                                                                .addGroup(jPanel7Layout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                false)
                                                                                .addComponent(btnBuscarVenta,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                68,
                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addComponent(jLabel3))
                                                                .addGap(26, 26, 26)));
                jPanel7Layout.setVerticalGroup(
                                jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel7Layout.createSequentialGroup()
                                                                .addContainerGap()
                                                                .addComponent(btnBuscarVenta,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                68,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(jLabel3,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)));

                javax.swing.GroupLayout panelPasosLayout = new javax.swing.GroupLayout(panelPasos);
                panelPasos.setLayout(panelPasosLayout);
                panelPasosLayout.setHorizontalGroup(
                                panelPasosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelPasosLayout.createSequentialGroup()
                                                                .addGap(0, 0, Short.MAX_VALUE)
                                                                .addComponent(jPanel7,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(jPanel4,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(jPanel5,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                                javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addComponent(jPanel6,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(18, 18, 18)
                                                                .addComponent(jPanel8,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(0, 0, Short.MAX_VALUE)));
                panelPasosLayout.setVerticalGroup(
                                panelPasosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelPasosLayout.createSequentialGroup()
                                                                .addGap(10, 10, 10)
                                                                .addGroup(panelPasosLayout
                                                                                .createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addGroup(panelPasosLayout
                                                                                                .createSequentialGroup()
                                                                                                .addComponent(jPanel7,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                                .addGap(0, 0, Short.MAX_VALUE))
                                                                                .addGroup(panelPasosLayout
                                                                                                .createSequentialGroup()
                                                                                                .addGroup(panelPasosLayout
                                                                                                                .createParallelGroup(
                                                                                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                                                                                false)
                                                                                                                .addComponent(jPanel5,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                Short.MAX_VALUE)
                                                                                                                .addComponent(jPanel8,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                Short.MAX_VALUE))
                                                                                                .addGap(10, 10, 10))))
                                                .addGroup(panelPasosLayout.createSequentialGroup()
                                                                .addGroup(
                                                                                panelPasosLayout.createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                .addGroup(panelPasosLayout
                                                                                                                .createSequentialGroup()
                                                                                                                .addGap(60, 60, 60)
                                                                                                                .addComponent(jPanel4,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                                                .addGroup(panelPasosLayout
                                                                                                                .createSequentialGroup()
                                                                                                                .addGap(60, 60, 60)
                                                                                                                .addComponent(jPanel6,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                                .addGap(67, 67, 67)));

                javax.swing.GroupLayout panelCambianteLayout = new javax.swing.GroupLayout(panelCambiante);
                panelCambiante.setLayout(panelCambianteLayout);
                panelCambianteLayout.setHorizontalGroup(
                                panelCambianteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGap(0, 0, Short.MAX_VALUE));
                panelCambianteLayout.setVerticalGroup(
                                panelCambianteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGap(0, 0, Short.MAX_VALUE));

                javax.swing.GroupLayout panelFondoLayout = new javax.swing.GroupLayout(panelFondo);
                panelFondo.setLayout(panelFondoLayout);
                panelFondoLayout.setHorizontalGroup(
                                panelFondoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelFondoLayout.createSequentialGroup()
                                                                .addGap(18, 18, 18)
                                                                .addComponent(jLabel16,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                281,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE))
                                                .addGroup(panelFondoLayout.createSequentialGroup()
                                                                .addGap(10, 10, 10)
                                                                .addGroup(
                                                                                panelFondoLayout.createParallelGroup(
                                                                                                javax.swing.GroupLayout.Alignment.LEADING)
                                                                                                .addComponent(panelCambiante,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                Short.MAX_VALUE)
                                                                                                .addComponent(panelPasos,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addGap(10, 10, 10)));
                panelFondoLayout.setVerticalGroup(
                                panelFondoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(panelFondoLayout.createSequentialGroup()
                                                                .addComponent(jLabel16,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                                46,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(10, 10, 10)
                                                                .addComponent(panelPasos,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(10, 10, 10)
                                                                .addComponent(panelCambiante,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                Short.MAX_VALUE)
                                                                .addContainerGap()));

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
                this.setLayout(layout);
                layout.setHorizontalGroup(
                                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(panelFondo, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
                layout.setVerticalGroup(
                                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(panelFondo, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        }// </editor-fold>//GEN-END:initComponents

        private void txtNVenta1ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_txtNVenta1ActionPerformed
                // TODO add your handling code here:
        }// GEN-LAST:event_txtNVenta1ActionPerformed

        private void btnBuscarVenta1ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnBuscarVenta1ActionPerformed
                // TODO add your handling code here:
        }// GEN-LAST:event_btnBuscarVenta1ActionPerformed

        private void btnSelectActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnSelectActionPerformed
                // TODO add your handling code here:
        }// GEN-LAST:event_btnSelectActionPerformed

        private void btnConfirmarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnConfirmarActionPerformed
                // TODO add your handling code here:
        }// GEN-LAST:event_btnConfirmarActionPerformed

        private void btnBuscarVentaActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnBuscarVentaActionPerformed
                // TODO add your handling code here:
        }// GEN-LAST:event_btnBuscarVentaActionPerformed

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton btnBuscarVenta;
        private javax.swing.JButton btnBuscarVenta1;
        private javax.swing.JButton btnConfirmar;
        private javax.swing.JButton btnSelect;
        private javax.swing.JLabel jLabel15;
        private javax.swing.JLabel jLabel16;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel24;
        private javax.swing.JLabel jLabel26;
        private javax.swing.JLabel jLabel27;
        private javax.swing.JLabel jLabel28;
        private javax.swing.JLabel jLabel29;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel30;
        private javax.swing.JLabel jLabel31;
        private javax.swing.JLabel jLabel32;
        private javax.swing.JLabel jLabel33;
        private javax.swing.JLabel jLabel34;
        private javax.swing.JLabel jLabel35;
        private javax.swing.JLabel jLabel36;
        private javax.swing.JLabel jLabel37;
        private javax.swing.JLabel jLabel38;
        private javax.swing.JLabel jLabel39;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JLabel jLabel40;
        private javax.swing.JLabel jLabel41;
        private javax.swing.JLabel jLabel42;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JPanel jPanel4;
        private javax.swing.JPanel jPanel5;
        private javax.swing.JPanel jPanel6;
        private javax.swing.JPanel jPanel7;
        private javax.swing.JPanel jPanel8;
        private javax.swing.JScrollPane jScrollPane3;
        private javax.swing.JScrollPane jScrollPane4;
        public static javax.swing.JPanel panelCambiante;
        private javax.swing.JPanel panelFondo;
        private javax.swing.JPanel panelPasos;
        private javax.swing.JPanel panelPoliticas1;
        private javax.swing.JPanel panelPoliticas2;
        private javax.swing.JPanel panelPrinci;
        private javax.swing.JPanel panelVentaDescripcion1;
        private javax.swing.JPanel sPanePrincip;
        private javax.swing.JLabel txtCliente1;
        private javax.swing.JLabel txtEstado1;
        private javax.swing.JLabel txtFechaVenta1;
        private javax.swing.JLabel txtNProd1;
        private javax.swing.JTextField txtNVenta1;
        private javax.swing.JLabel txtTotal1;
        // End of variables declaration//GEN-END:variables
}
