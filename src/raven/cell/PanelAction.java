package raven.cell;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import com.formdev.flatlaf.FlatClientProperties;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

public class PanelAction extends JPanel {

    private JButton cmdEdit;
    private JButton cmdView;
    private JButton cmdCaja;

    // ESTILOS MODERNOS
    private static final String STYLE_BTN_VIEW = "arc:12;background:#2196F3"; // Azul -> Ver
    private static final String STYLE_BTN_EDIT = "arc:12;background:#2ECC71"; // Verde -> Facturar
    private static final String STYLE_BTN_CAJA = "arc:12;background:#F39C12"; // Dorado -> Convertir

    public PanelAction() {
        init();
    }

    private void init() {
        // Usar FlowLayout para mejor distribución horizontal y rendimiento
        setLayout(new FlowLayout(FlowLayout.CENTER, 6, 2));
        setOpaque(true);

        // Inicializar botones
        cmdView = new JButton();
        cmdEdit = new JButton();
        cmdCaja = new JButton();

        // Configurar botones con sus iconos y estilos
        configurarBoton(cmdView, FontAwesomeSolid.EYE, STYLE_BTN_VIEW, "Ver detalle del traspaso");
        configurarBoton(cmdEdit, FontAwesomeSolid.FILE_INVOICE_DOLLAR, STYLE_BTN_EDIT, "Facturar / procesar traspaso");
        configurarBoton(cmdCaja, FontAwesomeSolid.BOX_OPEN, STYLE_BTN_CAJA, "Convertir caja a pares");

        // Agregar componentes directamente al panel
        add(cmdEdit);
        add(cmdView);
        add(cmdCaja);

        // Estado inicial
        cmdEdit.setVisible(false);
        cmdCaja.setVisible(false);
    }

    private void configurarBoton(JButton btn, Ikon icon, String style, String tooltip) {
        // Configurar icono
        FontIcon fontIcon = FontIcon.of(icon, 14, Color.WHITE);
        
        btn.setIcon(fontIcon);
        
        // Configurar estilo FlatLaf
        btn.putClientProperty(FlatClientProperties.STYLE, style);
        
        // Dimensiones y propiedades visuales
        btn.setPreferredSize(new Dimension(30, 28));
        btn.setToolTipText(tooltip);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(true);
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }

    /**
     * Mantenido por compatibilidad con llamadas externas.
     * Ya no es necesario realizar acciones pesadas aquí.
     */
    public void aplicarIconosFijos() {
        // No-op
    }

    public void initEvent(TableActionEvent event, int row) {
        // Eliminar listeners anteriores para evitar acumulación
        // (Es más seguro remover todos los listeners de acción)
        for (ActionListener al : cmdEdit.getActionListeners()) cmdEdit.removeActionListener(al);
        for (ActionListener al : cmdView.getActionListeners()) cmdView.removeActionListener(al);
        for (ActionListener al : cmdCaja.getActionListeners()) cmdCaja.removeActionListener(al);

        // Agregar nuevos listeners
        cmdView.addActionListener(ae -> event.onView(row));
        cmdEdit.addActionListener(ae -> event.onEdit(row));
        cmdCaja.addActionListener(ae -> event.onCaja(row));
    }

    public void setEditVisible(boolean visible) {
        cmdEdit.setVisible(visible);
    }

    public void setCajaVisible(boolean visible) {
        cmdCaja.setVisible(visible);
    }
}
