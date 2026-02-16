package raven.application.form;

import com.formdev.flatlaf.FlatClientProperties;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author Raven
 */
public class PanelLogin extends JPanel {

    public PanelLogin() {
        setOpaque(false);
        setLayout(new MigLayout("fillx,wrap,insets 30 40 50 40, width 360, hidemode 3", "[grow,fill]", "[]18[]12[]18[]12[]24[]"));
        putClientProperty(FlatClientProperties.STYLE, "background:$Login.background;arc:20;");
    }
}
