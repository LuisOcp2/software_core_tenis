package raven.cell;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

public class PanelActionDevolucion extends JPanel {

    private JButton cmdView;
    private JButton cmdAuthorize;
    private JButton cmdReject;

    private JButton cmdAnulate; // Nuevo botón

    // ESTILOS
    private static final String STYLE_BTN_VIEW = "arc:999;background:#2196F3"; // Azul
    private static final String STYLE_BTN_AUTH = "arc:999;background:#4CAF50"; // Verde
    private static final String STYLE_BTN_REJECT = "arc:999;background:#F44336"; // Rojo
    private static final String STYLE_BTN_ANULATE = "arc:999;background:#FF9800"; // Naranja

    public PanelActionDevolucion() {
        init();
    }

    private void init() {
        // Usar MigLayout para mejor alineación y centrado
        setLayout(new MigLayout("insets 0, gap 5, alignx center", "[center]"));
        setOpaque(true);

        cmdView = new JButton();
        cmdAuthorize = new JButton();
        cmdReject = new JButton();
        cmdAnulate = new JButton(); // Inicializar

        configurarBoton(cmdView, FontAwesomeSolid.EYE, STYLE_BTN_VIEW, "Ver detalle");
        configurarBoton(cmdAuthorize, FontAwesomeSolid.CHECK, STYLE_BTN_AUTH, "Autorizar devolución");
        configurarBoton(cmdReject, FontAwesomeSolid.TIMES, STYLE_BTN_REJECT, "Rechazar devolución");
        configurarBoton(cmdAnulate, FontAwesomeSolid.BAN, STYLE_BTN_ANULATE, "Anular devolución"); // Configurar

        // Aumentar tamaño de botones a 36x36 para que se vean bien en filas de 55px
        add(cmdView, "w 36!, h 36!");
        add(cmdAuthorize, "w 36!, h 36!");
        add(cmdReject, "w 36!, h 36!");
        add(cmdAnulate, "w 36!, h 36!"); // Añadir al layout
    }

    private void configurarBoton(JButton btn, Ikon icon, String style, String tooltip) {
        // Aumentar tamaño de icono a 18
        FontIcon fontIcon = FontIcon.of(icon, 18, Color.WHITE);
        btn.setIcon(fontIcon);
        btn.putClientProperty(FlatClientProperties.STYLE, style);
        // Aumentar tamaño preferido
        btn.setPreferredSize(new Dimension(36, 36));
        btn.setToolTipText(tooltip);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(true);
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }

    public void initEvent(TableActionDevolucionEvent event, int row) {
        for (ActionListener al : cmdView.getActionListeners())
            cmdView.removeActionListener(al);
        for (ActionListener al : cmdAuthorize.getActionListeners())
            cmdAuthorize.removeActionListener(al);
        for (ActionListener al : cmdReject.getActionListeners())
            cmdReject.removeActionListener(al);
        for (ActionListener al : cmdAnulate.getActionListeners()) // Limpiar listeners
            cmdAnulate.removeActionListener(al);

        cmdView.addActionListener(ae -> event.onView(row));
        cmdAuthorize.addActionListener(ae -> event.onAuthorize(row));
        cmdReject.addActionListener(ae -> event.onReject(row));
        cmdAnulate.addActionListener(ae -> event.onAnulate(row)); // Conectar evento
    }

    public void mostrarBotonesAutorizacion(boolean mostrar) {
        // Logica existente...
        cmdAuthorize.setVisible(mostrar);
        cmdReject.setVisible(mostrar);
    }

    // Método para mostrar/ocultar botón de anular según estado
    public void mostrarBotonAnular(boolean mostrar) {
        cmdAnulate.setVisible(mostrar);
    }
}
