package raven.application.form.comercial;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.RowFilter;
import javax.swing.text.DateFormatter;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.*;
import java.util.*;
import java.util.regex.Pattern;
import java.awt.image.BufferedImage;

/**
 *
 * @author CrisDEV
 */
public class ComprasForm2 extends javax.swing.JPanel {

    public ComprasForm2() {
        initComponents();
        // ===== Estilos del título según tema =====
        lbtitulo.putClientProperty(FlatClientProperties.STYLE,
                "font:$h1.font;foreground:$Text.foreground");

        configurarEstilosTema();
        inicializarPanelFiltros();
        inicializarLogicaTabla();
        conectarEventosChipsEstado();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        PanelTitle = new javax.swing.JPanel();
        panelimg = new javax.swing.JPanel();
        lbtitulo = new javax.swing.JLabel();
        lbsubtitulo = new javax.swing.JLabel();
        BtnNuevaEntrada = new javax.swing.JButton();
        PanelTotalCompras = new javax.swing.JPanel();
        PanelFacturasPagadas = new javax.swing.JPanel();
        PanelAbonos = new javax.swing.JPanel();
        PanelPendientePago = new javax.swing.JPanel();
        ScrolFiltros = new javax.swing.JScrollPane();
        jPanel5 = new javax.swing.JPanel();
        BtnTodas = new javax.swing.JButton();
        BtnPendientes = new javax.swing.JButton();
        BtnAboanadas = new javax.swing.JButton();
        Pagadas = new javax.swing.JButton();
        panelTabla = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        Tabla = new javax.swing.JTable();
        lbsubtitulo2 = new javax.swing.JLabel();
        Txtbuscar = new javax.swing.JTextField();

        PanelTitle.setBackground(new java.awt.Color(51, 51, 51));
        PanelTitle.setForeground(new java.awt.Color(51, 51, 51));

        javax.swing.GroupLayout panelimgLayout = new javax.swing.GroupLayout(panelimg);
        panelimg.setLayout(panelimgLayout);
        panelimgLayout.setHorizontalGroup(
            panelimgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        panelimgLayout.setVerticalGroup(
            panelimgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 85, Short.MAX_VALUE)
        );

        lbtitulo.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lbtitulo.setText("Gestión de Compras");

        lbsubtitulo.setText("Administra todas tus entradas de mercancía y pagos a proveedores");

        BtnNuevaEntrada.setText("Nueva Entrada");

        javax.swing.GroupLayout PanelTitleLayout = new javax.swing.GroupLayout(PanelTitle);
        PanelTitle.setLayout(PanelTitleLayout);
        PanelTitleLayout.setHorizontalGroup(
            PanelTitleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PanelTitleLayout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addComponent(panelimg, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(PanelTitleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(PanelTitleLayout.createSequentialGroup()
                        .addComponent(lbsubtitulo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(BtnNuevaEntrada, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(PanelTitleLayout.createSequentialGroup()
                        .addComponent(lbtitulo)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        PanelTitleLayout.setVerticalGroup(
            PanelTitleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PanelTitleLayout.createSequentialGroup()
                .addGroup(PanelTitleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(PanelTitleLayout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(lbtitulo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(PanelTitleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lbsubtitulo)
                            .addComponent(BtnNuevaEntrada, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(PanelTitleLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(panelimg, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        PanelTotalCompras.setBackground(new java.awt.Color(0, 0, 255));

        javax.swing.GroupLayout PanelTotalComprasLayout = new javax.swing.GroupLayout(PanelTotalCompras);
        PanelTotalCompras.setLayout(PanelTotalComprasLayout);
        PanelTotalComprasLayout.setHorizontalGroup(
            PanelTotalComprasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 198, Short.MAX_VALUE)
        );
        PanelTotalComprasLayout.setVerticalGroup(
            PanelTotalComprasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        PanelFacturasPagadas.setBackground(new java.awt.Color(0, 255, 102));

        javax.swing.GroupLayout PanelFacturasPagadasLayout = new javax.swing.GroupLayout(PanelFacturasPagadas);
        PanelFacturasPagadas.setLayout(PanelFacturasPagadasLayout);
        PanelFacturasPagadasLayout.setHorizontalGroup(
            PanelFacturasPagadasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        PanelFacturasPagadasLayout.setVerticalGroup(
            PanelFacturasPagadasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        PanelAbonos.setBackground(new java.awt.Color(255, 255, 51));

        javax.swing.GroupLayout PanelAbonosLayout = new javax.swing.GroupLayout(PanelAbonos);
        PanelAbonos.setLayout(PanelAbonosLayout);
        PanelAbonosLayout.setHorizontalGroup(
            PanelAbonosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 198, Short.MAX_VALUE)
        );
        PanelAbonosLayout.setVerticalGroup(
            PanelAbonosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        PanelPendientePago.setBackground(new java.awt.Color(255, 51, 51));

        javax.swing.GroupLayout PanelPendientePagoLayout = new javax.swing.GroupLayout(PanelPendientePago);
        PanelPendientePago.setLayout(PanelPendientePagoLayout);
        PanelPendientePagoLayout.setHorizontalGroup(
            PanelPendientePagoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 186, Short.MAX_VALUE)
        );
        PanelPendientePagoLayout.setVerticalGroup(
            PanelPendientePagoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        BtnTodas.setText("Todas las compras");
        BtnTodas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BtnTodasActionPerformed(evt);
            }
        });

        BtnPendientes.setText("Pendientes");

        BtnAboanadas.setText("Abonadas");

        Pagadas.setText("Pagadas");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addComponent(BtnTodas, javax.swing.GroupLayout.PREFERRED_SIZE, 198, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(BtnPendientes, javax.swing.GroupLayout.DEFAULT_SIZE, 195, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(BtnAboanadas, javax.swing.GroupLayout.PREFERRED_SIZE, 202, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(Pagadas, javax.swing.GroupLayout.PREFERRED_SIZE, 193, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(BtnAboanadas, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(Pagadas, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(BtnPendientes, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(BtnTodas, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        Tabla.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null}
            },
            new String [] {
                "N° Entrada", "Fecha", "Proveedor", "N° Factura", "Total Compra", "Abonado", "Saldo", "Estado", "Acciones"
            }
        ));
        jScrollPane1.setViewportView(Tabla);

        lbsubtitulo2.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lbsubtitulo2.setText("Listado de Compras");

        javax.swing.GroupLayout panelTablaLayout = new javax.swing.GroupLayout(panelTabla);
        panelTabla.setLayout(panelTablaLayout);
        panelTablaLayout.setHorizontalGroup(
            panelTablaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelTablaLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1))
            .addGroup(panelTablaLayout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addComponent(lbsubtitulo2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(Txtbuscar, javax.swing.GroupLayout.PREFERRED_SIZE, 308, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        panelTablaLayout.setVerticalGroup(
            panelTablaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelTablaLayout.createSequentialGroup()
                .addGroup(panelTablaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelTablaLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lbsubtitulo2))
                    .addComponent(Txtbuscar))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 397, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(9, 9, 9))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(panelTabla, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(ScrolFiltros, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(PanelTitle, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(PanelTotalCompras, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(PanelFacturasPagadas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(PanelAbonos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(PanelPendientePago, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(PanelTitle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(PanelFacturasPagadas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(PanelTotalCompras, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(PanelAbonos, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(PanelPendientePago, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ScrolFiltros, javax.swing.GroupLayout.PREFERRED_SIZE, 167, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelTabla, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void BtnTodasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BtnTodasActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_BtnTodasActionPerformed

    // ====== Campos y utilidades para filtros / tema ======
    private JFormattedTextField txtFechaDesde;
    private JFormattedTextField txtFechaHasta;
    private JComboBox<String> cmbProveedor;
    private JComboBox<String> cmbEstado;
    private JFormattedTextField txtMontoMin;
    private JFormattedTextField txtMontoMax;
    private JButton btnAplicarFiltros;
    private JButton btnLimpiarFiltros;
    private JPanel panelFiltros;
    private TableRowSorter<TableModel> sorter;
    private final SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
    private final DecimalFormat nf = new DecimalFormat("#,##0.00");
    private JLabel lbIconCompras;
    private javax.swing.JPanel filtrosHeader;
    private javax.swing.JLabel lbTituloFiltros;
    private javax.swing.JButton btnToggleFiltros;
    private boolean filtrosVisibles = false;

    private void configurarEstilosTema() {
        // Panel de título y KPI cards
        PanelTitle.setBackground(UIManager.getColor("Panel.background"));
        PanelTitle.putClientProperty(FlatClientProperties.STYLE,
                "background:$Panel.background;arc:20;" +
                "border:1,1,1,1,$Component.borderColor,,12");
        BtnNuevaEntrada.putClientProperty(FlatClientProperties.STYLE,
                "arc:16; background:@accentColor; foreground:#FFFFFF;" +
                "borderWidth:0; focusWidth:1");
        BtnNuevaEntrada.setIcon(safeIcon("raven/icon/svg/search.svg", "raven/icon/svg/xtreme.svg", 18, 18));
        FlatSVGIcon iconCompras = safeIcon("raven/icon/svg/compra.svg", "raven/icon/svg/caja.svg", 36, 36);
        lbIconCompras = new JLabel(iconCompras);
        lbIconCompras.setHorizontalAlignment(SwingConstants.CENTER);
        lbIconCompras.putClientProperty(FlatClientProperties.STYLE, "foreground:$Text.foreground");
        panelimg.setLayout(new java.awt.BorderLayout());
        panelimg.add(lbIconCompras, java.awt.BorderLayout.CENTER);
        panelimg.putClientProperty(FlatClientProperties.STYLE,
                "background:lighten($Panel.background,2%);arc:16;" +
                "border:1,1,1,1,$Component.borderColor,,10");

        resetPanelBackground(PanelTotalCompras);
        resetPanelBackground(PanelFacturasPagadas);
        resetPanelBackground(PanelAbonos);
        resetPanelBackground(PanelPendientePago);
        configurarCard(PanelTotalCompras);
        configurarCard(PanelFacturasPagadas);
        configurarCard(PanelAbonos);
        configurarCard(PanelPendientePago);

        // Tabla y búsqueda
        Tabla.putClientProperty(FlatClientProperties.STYLE,
                "gridColor:lighten($Component.borderColor,10%);" +
                "selectionBackground:$Table.selectionBackground;" +
                "selectionForeground:$Table.selectionForeground");
        Txtbuscar.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                "Buscar por N° entrada, factura, proveedor...");
        Txtbuscar.putClientProperty(FlatClientProperties.STYLE,
                "arc:14; background:lighten($Panel.background,6%);");
    }

    private void resetPanelBackground(JPanel p) {
        p.setBackground(UIManager.getColor("Panel.background"));
    }

    private void configurarCard(JPanel card) {
        card.putClientProperty(FlatClientProperties.STYLE,
                "background:lighten($Panel.background,2%);" +
                "arc:18; border:1,1,1,1,$Component.borderColor,,10");
    }

    private void inicializarPanelFiltros() {
        panelFiltros = new JPanel(new GridBagLayout());
        panelFiltros.putClientProperty(FlatClientProperties.STYLE,
                "background:lighten($Panel.background,2%);arc:16;" +
                "border:1,1,1,1,$Component.borderColor,,12");
        ScrolFiltros.setViewportView(panelFiltros);
        ScrolFiltros.putClientProperty(FlatClientProperties.STYLE,
                "background:$Panel.background;border:0,0,0,0");

        filtrosHeader = new JPanel(new BorderLayout());
        filtrosHeader.putClientProperty(FlatClientProperties.STYLE,
                "background:lighten($Panel.background,2%);arc:16;" +
                "border:1,1,1,1,$Component.borderColor,,12");
        lbTituloFiltros = new JLabel("Filtros de Búsqueda");
        lbTituloFiltros.putClientProperty(FlatClientProperties.STYLE, "font:$h3.font;foreground:$Text.foreground");
        btnToggleFiltros = new JButton("Expandir ▸");
        btnToggleFiltros.putClientProperty(FlatClientProperties.STYLE,
                "arc:14;background:lighten($Panel.background,4%);border:1,1,1,1,$Component.borderColor,,10");
        btnToggleFiltros.addActionListener(e -> toggleFiltros());
        filtrosHeader.add(lbTituloFiltros, BorderLayout.WEST);
        filtrosHeader.add(btnToggleFiltros, BorderLayout.EAST);
        ScrolFiltros.setVisible(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8,8,8,8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0; gbc.gridx = 0; gbc.weightx = 0.25;

        txtFechaDesde = crearCampoFecha("dd/mm/aaaa");
        panelFiltros.add(wrapLabeled("Fecha Desde", txtFechaDesde), gbc);
        gbc.gridx++;
        txtFechaHasta = crearCampoFecha("dd/mm/aaaa");
        panelFiltros.add(wrapLabeled("Fecha Hasta", txtFechaHasta), gbc);
        gbc.gridx++;

        cmbProveedor = new JComboBox<>(new String[]{"Todos los proveedores"});
        cmbProveedor.putClientProperty(FlatClientProperties.STYLE,
                "arc:14;background:lighten($Panel.background,4%);");
        panelFiltros.add(wrapLabeled("Proveedor", cmbProveedor), gbc);
        gbc.gridx++;

        cmbEstado = new JComboBox<>(new String[]{"Todos los estados","PAGADA","ABONADA","PENDIENTE"});
        cmbEstado.putClientProperty(FlatClientProperties.STYLE,
                "arc:14;background:lighten($Panel.background,4%);");
        panelFiltros.add(wrapLabeled("Estado", cmbEstado), gbc);
        gbc.gridy++; gbc.gridx = 0;

        txtMontoMin = crearCampoMonto("0.00");
        panelFiltros.add(wrapLabeled("$ Monto Mínimo", txtMontoMin), gbc);
        gbc.gridx++;
        txtMontoMax = crearCampoMonto("0.00");
        panelFiltros.add(wrapLabeled("$ Monto Máximo", txtMontoMax), gbc);
        gbc.gridx++;

        btnAplicarFiltros = new JButton("Aplicar Filtros");
        btnAplicarFiltros.setIcon(safeIcon("raven/icon/svg/search.svg", "raven/icon/svg/xtreme.svg", 16, 16));
        btnAplicarFiltros.putClientProperty(FlatClientProperties.STYLE,
                "arc:16; background:@accentColor; foreground:#FFFFFF;" +
                "borderWidth:0; focusWidth:1");
        btnAplicarFiltros.addActionListener(e -> aplicarFiltros());
        panelFiltros.add(btnAplicarFiltros, gbc);
        gbc.gridx++;

        btnLimpiarFiltros = new JButton("Limpiar");
        btnLimpiarFiltros.putClientProperty(FlatClientProperties.STYLE,
                "arc:16; background:lighten($Panel.background,4%);" +
                "border:1,1,1,1,$Component.borderColor,,10");
        btnLimpiarFiltros.addActionListener(e -> limpiarFiltros());
        panelFiltros.add(btnLimpiarFiltros, gbc);
    }

    private FlatSVGIcon safeIcon(String primary, String fallback, int w, int h) {
        try {
            FlatSVGIcon ic = new FlatSVGIcon(primary, w, h);
            BufferedImage img = new BufferedImage(Math.max(w, 1), Math.max(h, 1), BufferedImage.TYPE_INT_ARGB);
            ic.paintIcon(null, img.getGraphics(), 0, 0);
            return ic;
        } catch (Throwable t) {
            return new FlatSVGIcon(fallback, w, h);
        }
    }

    private JPanel wrapLabeled(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(4,4));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.putClientProperty(FlatClientProperties.STYLE, "foreground:$Text.foreground");
        p.add(l, BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private JFormattedTextField crearCampoFecha(String placeholder) {
        JFormattedTextField f = new JFormattedTextField(new DateFormatter(df));
        f.setColumns(10);
        f.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        f.putClientProperty(FlatClientProperties.STYLE,
                "arc:14; background:lighten($Panel.background,6%);");
        return f;
    }

    private JFormattedTextField crearCampoMonto(String placeholder) {
        NumberFormat format = nf;
        JFormattedTextField f = new JFormattedTextField(new NumberFormatter(format));
        f.setColumns(10);
        f.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        f.putClientProperty(FlatClientProperties.STYLE,
                "arc:14; background:lighten($Panel.background,6%);");
        return f;
    }

    private void inicializarLogicaTabla() {
        sorter = new TableRowSorter<>(Tabla.getModel());
        Tabla.setRowSorter(sorter);

        // Buscar rápido por texto
        Txtbuscar.addActionListener(e -> aplicarFiltros());
    }

    private void conectarEventosChipsEstado() {
        BtnTodas.addActionListener(e -> { cmbEstado.setSelectedIndex(0); aplicarFiltros(); });
        BtnPendientes.addActionListener(e -> { cmbEstado.setSelectedItem("PENDIENTE"); aplicarFiltros(); });
        BtnAboanadas.addActionListener(e -> { cmbEstado.setSelectedItem("ABONADA"); aplicarFiltros(); });
        Pagadas.addActionListener(e -> { cmbEstado.setSelectedItem("PAGADA"); aplicarFiltros(); });

        // Estilos chips
        estilizarChip(BtnTodas, true);
        estilizarChip(BtnPendientes, false);
        estilizarChip(BtnAboanadas, false);
        estilizarChip(Pagadas, false);
    }

    private void estilizarChip(JButton btn, boolean primary) {
        btn.putClientProperty(FlatClientProperties.STYLE,
                primary ? "arc:18;background:lighten(@accentColor,5%);foreground:#FFFFFF;"
                        : "arc:18;background:lighten($Panel.background,4%);border:1,1,1,1,$Component.borderColor,,10");
    }

    private void aplicarFiltros() {
        java.util.List<RowFilter<TableModel,Object>> filters = new ArrayList<>();

        // Mantener posición del scroll
        JScrollBar sb = jScrollPane1.getVerticalScrollBar();
        int pos = sb.getValue();

        // Texto general
        String q = Txtbuscar.getText() != null ? Txtbuscar.getText().trim().toLowerCase() : "";
        if (!q.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(q)));
        }

        // Estado
        Object estadoSel = cmbEstado.getSelectedItem();
        if (estadoSel != null && !"Todos los estados".equals(estadoSel.toString())) {
            filters.add(RowFilter.regexFilter("^" + estadoSel + "$", 7));
        }

        // Proveedor
        Object provSel = cmbProveedor.getSelectedItem();
        if (provSel != null && !"Todos los proveedores".equals(provSel.toString())) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(provSel.toString()), 2));
        }

        // Fechas
        Date dDesde = parseDate(txtFechaDesde.getText());
        Date dHasta = parseDate(txtFechaHasta.getText());
        if (dDesde != null || dHasta != null) {
            filters.add(new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends TableModel, ? extends Object> entry) {
                    try {
                        String fv = Objects.toString(entry.getValue(1), "");
                        Date fd = df.parse(fv);
                        if (dDesde != null && fd.before(dDesde)) return false;
                        if (dHasta != null && fd.after(dHasta)) return false;
                        return true;
                    } catch (Exception ex) {
                        return true;
                    }
                }
            });
        }

        // Montos
        Double min = parseMonto(txtMontoMin.getText());
        Double max = parseMonto(txtMontoMax.getText());
        if (min != null || max != null) {
            filters.add(new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends TableModel, ? extends Object> entry) {
                    try {
                        String totalStr = Objects.toString(entry.getValue(4), "0");
                        Number n = nf.parse(totalStr.replace("$", "").replace(",", ""));
                        double v = n.doubleValue();
                        if (min != null && v < min) return false;
                        if (max != null && v > max) return false;
                        return true;
                    } catch (Exception ex) {
                        return true;
                    }
                }
            });
        }

        sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
        SwingUtilities.invokeLater(() -> sb.setValue(pos));
    }

    private void limpiarFiltros() {
        txtFechaDesde.setValue(null);
        txtFechaHasta.setValue(null);
        cmbProveedor.setSelectedIndex(0);
        cmbEstado.setSelectedIndex(0);
        txtMontoMin.setValue(null);
        txtMontoMax.setValue(null);
        Txtbuscar.setText("");
        aplicarFiltros();
    }

    private void toggleFiltros() {
        filtrosVisibles = !filtrosVisibles;
        ScrolFiltros.setVisible(filtrosVisibles);
        btnToggleFiltros.setText(filtrosVisibles ? "Ocultar ▾" : "Expandir ▸");
        revalidate();
        repaint();
    }

    private Date parseDate(String s) {
        try {
            if (s == null) return null;
            s = s.trim();
            if (s.isEmpty()) return null;
            return df.parse(s);
        } catch (Exception ex) { return null; }
    }
    private Double parseMonto(String s) {
        try {
            if (s == null) return null;
            s = s.trim();
            if (s.isEmpty()) return null;
            return Double.parseDouble(s.replace(",", ""));
        } catch (Exception ex) { return null; }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton BtnAboanadas;
    private javax.swing.JButton BtnNuevaEntrada;
    private javax.swing.JButton BtnPendientes;
    private javax.swing.JButton BtnTodas;
    private javax.swing.JButton Pagadas;
    private javax.swing.JPanel PanelAbonos;
    private javax.swing.JPanel PanelFacturasPagadas;
    private javax.swing.JPanel PanelPendientePago;
    private javax.swing.JPanel PanelTitle;
    private javax.swing.JPanel PanelTotalCompras;
    private javax.swing.JScrollPane ScrolFiltros;
    public javax.swing.JTable Tabla;
    private javax.swing.JTextField Txtbuscar;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lbsubtitulo;
    private javax.swing.JLabel lbsubtitulo2;
    private javax.swing.JLabel lbtitulo;
    private javax.swing.JPanel panelTabla;
    private javax.swing.JPanel panelimg;
    // End of variables declaration//GEN-END:variables
}
