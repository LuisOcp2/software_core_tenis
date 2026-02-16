package raven.application.form.reportes;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import javax.swing.*;
import raven.application.Application;
import raven.clases.admin.UserSession;
import raven.controlador.admin.ModelUser;

/**
 * Panel principal de reportes con tiles para cada categoría
 */
public class ReportesMainForm extends JPanel {

    private static final String PANEL_STYLE = "arc:20;background:$Panel.background";
    private static final Color CARD_BG_1 = new Color(59, 130, 246); // Azul
    private static final Color CARD_BG_2 = new Color(16, 185, 129); // Verde
    private static final Color CARD_BG_3 = new Color(249, 115, 22); // Naranja
    private static final Color CARD_BG_4 = new Color(139, 92, 246); // Púrpura
    private static final Color CARD_BG_5 = new Color(236, 72, 153); // Rosa
    private static final Color CARD_BG_6 = new Color(14, 165, 233); // Cyan
    private static final Color CARD_BG_7 = new Color(234, 179, 8); // Amarillo
    private static final Color CARD_BG_8 = new Color(239, 68, 68); // Rojo

    public ReportesMainForm() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        putClientProperty(FlatClientProperties.STYLE, "border:10,10,10,10");

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.putClientProperty(FlatClientProperties.STYLE, "background:$Panel.background");
        JLabel lblTitle = new JLabel("Centro de Reportes");
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font:bold +10");
        JLabel lblSubtitle = new JLabel("Seleccione un tipo de reporte para generar");
        lblSubtitle.putClientProperty(FlatClientProperties.STYLE, "foreground:$Label.disabledForeground");

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        titlePanel.add(lblTitle);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(lblSubtitle);
        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        add(headerPanel, BorderLayout.NORTH);

        // Grid de tarjetas
        JPanel gridPanel = new JPanel(new GridLayout(2, 4, 15, 15));
        gridPanel.setOpaque(false);

        gridPanel.add(createReportCard("Inventario", "Caja", "Stock, rotación, valorización", CARD_BG_1,
                this::abrirReporteInventario));
        gridPanel.add(
                createReportCard("Ventas", "Efectivo", "Ventas por período, vendedor", CARD_BG_2, this::abrirReporteVentas));
        gridPanel.add(createReportCard("Compras", "Carrito", "Compras por proveedor", CARD_BG_3, this::abrirReporteCompras));
        gridPanel.add(createReportCard("Gastos", "Gastos", "Gastos operativos", CARD_BG_4, this::abrirReporteGastos));
        gridPanel.add(createReportCard("Devoluciones", "Actualizando", "Devoluciones y notas crédito", CARD_BG_5,
                this::abrirReporteDevoluciones));
        gridPanel.add(createReportCard("Traspasos", "", "Movimientos entre bodegas", CARD_BG_6,
                this::abrirReporteTraspasos));
        gridPanel.add(createReportCard("Clientes", "", "Top clientes, fidelización", CARD_BG_7,
                this::abrirReporteClientes));

        // Auditoría solo para admin/gerente
        ModelUser user = UserSession.getInstance().getCurrentUser();
        if (user != null && (user.getRol().equalsIgnoreCase("admin") || user.getRol().equalsIgnoreCase("gerente"))) {
            gridPanel.add(createReportCard("Auditoría", "Buscar", "Trazabilidad, sesiones", CARD_BG_8,
                    this::abrirReporteAuditoria));
        } else {
            JPanel placeholder = new JPanel();
            placeholder.setOpaque(false);
            gridPanel.add(placeholder);
        }

        add(gridPanel, BorderLayout.CENTER);
    }

    private JPanel createReportCard(String titulo, String icono, String descripcion, Color bgColor, Runnable onClick) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(bgColor);
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        card.putClientProperty(FlatClientProperties.STYLE, "arc:15");
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Icono grande
        JLabel lblIcono = new JLabel(icono, SwingConstants.CENTER);
        lblIcono.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        lblIcono.setForeground(Color.WHITE);

        // Texto
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitulo.setForeground(Color.WHITE);
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblDesc = new JLabel("<html><body style='width:120px'>" + descripcion + "</body></html>");
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDesc.setForeground(new Color(255, 255, 255, 200));
        lblDesc.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(lblTitulo);
        textPanel.add(Box.createVerticalStrut(5));
        textPanel.add(lblDesc);

        card.add(lblIcono, BorderLayout.NORTH);
        card.add(textPanel, BorderLayout.CENTER);

        // Efecto hover
        card.addMouseListener(new MouseAdapter() {
            Color originalBg = bgColor;

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(originalBg.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(originalBg);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                onClick.run();
            }
        });

        return card;
    }

    private void abrirReporteInventario() {
        Application.showForm(new ReporteInventarioForm());
    }

    private void abrirReporteVentas() {
        try {
            Application.showForm(new raven.application.form.comercial.reporteVentas());
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error al abrir reporte de ventas: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void abrirReporteCompras() {
        Application.showForm(new ReporteComprasForm());
    }

    private void abrirReporteGastos() {
        Application.showForm(new ReporteGastosForm());
    }

    private void abrirReporteDevoluciones() {
        Application.showForm(new ReporteDevolucionesForm());
    }

    private void abrirReporteTraspasos() {
        Application.showForm(new ReporteTraspasoForm());
    }

    private void abrirReporteClientes() {
        Application.showForm(new ReporteClientesForm());
    }

    private void abrirReporteAuditoria() {
        Application.showForm(new ReporteAuditoriaForm());
    }
}

