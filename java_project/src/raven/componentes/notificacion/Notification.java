package raven.componentes.notificacion;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;
import javax.swing.JDialog;
import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.TimingTarget;
import org.jdesktop.animation.timing.TimingTargetAdapter;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;
import javax.swing.UIManager;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

public class Notification extends javax.swing.JComponent {
    // Manejo de stacking en pantalla
    private static final java.util.List<Notification> ACTIVE = new java.util.ArrayList<>();
    private static final int STACK_GAP = 8;
    private static final int BASE_MARGIN = 20;

    private JDialog dialog;
    private Animator animator;
    private final Frame fram;
    private boolean showing;
    private Thread thread;
    private int animate = 10;
    private BufferedImage imageShadow;
    private int shadowSize = 6;
    private Type type;
    private Location location;
    private final FontIcon iconSuccess;
    private int displayDurationMs = 5000;
    private String customTitle;
    private String customMessage;
    private FontIcon customIcon;
    private boolean expanded = false;
    private Animator expandAnimator;
    private boolean read = false;
    private java.util.Date timestamp = new java.util.Date();
    private int priority = 0;
    private java.util.function.Consumer<Notification> onApprove;
    private java.util.function.Consumer<Notification> onReject;
    private Color stripeColor = new Color(28, 139, 206);
    private int baseX;
    private int baseY;
    private boolean fromBottom;  // true si apilamos hacia arriba (bottom-right)

    public Notification(Frame fram, Type type, Location location, String message) {

        if (fram == null) {
            fram = (Frame) KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        }
        if (fram == null) {
            fram = new Frame(); // Frame temporal como respaldo
            fram.setVisible(false); // No mostrar el frame temporal
        }
        Logger.getLogger(Notification.class.getName())
                .warning("Usando Frame temporal para la notificación");

        this.fram = fram;

        this.type = type;

        this.location = location;

        initComponents();

        init(message);

        initAnimator();
        Color tabTextColor = new Color(255, 255, 255);
        iconSuccess = createColoredIcon(FontAwesomeSolid.USER, tabTextColor);
        init(message);
        initAnimator();

    }

    public Notification(Frame fram, Type type, Location location, String title, String message, Ikon icon, int durationMs) {
        if (fram == null) {
            fram = (Frame) KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        }
        if (fram == null) {
            fram = new Frame();
            fram.setVisible(false);
        }
        this.fram = fram;
        this.type = type;
        this.location = location;
        Color tabTextColor = new Color(255, 255, 255);
        iconSuccess = createColoredIcon(FontAwesomeSolid.USER, tabTextColor);
        this.customTitle = title;
        this.customMessage = message;
        this.customIcon = icon != null ? createColoredIcon(icon, tabTextColor) : null;
        this.displayDurationMs = Math.max(5000, durationMs);
        initComponents();
        init(message);
        initAnimator();
    }

    private void init(String message) {
       

        // Crear diálogo con el frame válido
        dialog = new JDialog(fram);
        dialog.setUndecorated(true);
        dialog.setFocusableWindowState(false);
        dialog.setBackground(new Color(0, 0, 0, 0));
        dialog.add(this);

        FontIcon iconToUse = customIcon != null ? customIcon : iconSuccess;
        lbIcon.setIcon(iconToUse);
        String title = customTitle != null ? customTitle : (type == Type.SUCCESS ? "Success" : type == Type.INFO ? "Info" : "Warning");
        lbMessage.setText(title);
        lbMessageText.setText(customMessage != null ? customMessage : message);
        lbDate.setText(new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(timestamp));
        applyTypeStyle();
        header.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                toggleDetails();
            }
        });
        // Forzar actualización de layout
        dialog.pack();
        dialog.setSize(getPreferredSize());
        dialog.validate();
        lbIcon.repaint();
    }
    
    private FontIcon createColoredIcon(Ikon icon, Color color) {
        FontIcon fontIcon = FontIcon.of(icon);
        fontIcon.setIconSize(18);
        fontIcon.setIconColor(color);
        return fontIcon;
    }

    private void initAnimator() {
        TimingTarget target;
        target = new TimingTargetAdapter() {
            private int x;
            private int top;
            private boolean top_to_bot;

            @Override
            public void timingEvent(float fraction) {
                if (showing) {
                    float alpha = 1f - fraction;
                    int y = (int) ((1f - fraction) * animate);
                    if (top_to_bot) {
                        dialog.setLocation(x, top + y);
                    } else {
                        dialog.setLocation(x, top - y);
                    }
                    dialog.setOpacity(alpha);
                } else {
                    float alpha = fraction;
                    int y = (int) (fraction * animate);
                    if (top_to_bot) {
                        dialog.setLocation(x, top + y);
                    } else {
                        dialog.setLocation(x, top - y);
                    }
                    dialog.setOpacity(alpha);
                }
            }

            @Override
            public void begin() {
                if (!showing) {
                    dialog.setOpacity(0f);
                    int margin = BASE_MARGIN;
                    int x = 0;
                    int y = 0;
                    boolean top_to_bot = true;

                    if (fram == null || location == Location.BOTTOM_RIGHT) {
                        // Obtener área útil de la pantalla (excluyendo la barra de tareas)
                        GraphicsConfiguration gc = GraphicsEnvironment
                                .getLocalGraphicsEnvironment()
                                .getDefaultScreenDevice()
                                .getDefaultConfiguration();

                        Rectangle usableBounds = gc.getBounds();
                        Insets screenInsets = Toolkit.getDefaultToolkit()
                                .getScreenInsets(gc);

                        usableBounds.x += screenInsets.left;
                        usableBounds.y += screenInsets.top;
                        usableBounds.width -= (screenInsets.left + screenInsets.right);
                        usableBounds.height -= (screenInsets.top + screenInsets.bottom);

                        // Calcular posición inferior derecha
                        x = usableBounds.x + usableBounds.width - dialog.getWidth() - margin;
                        y = usableBounds.y + usableBounds.height - dialog.getHeight() - margin;
                        top_to_bot = false;
                    } else {
                        // Lógica original para posicionamiento relativo al Frame padre
                        if (location == null) {
                            x = fram.getX() + ((fram.getWidth() - dialog.getWidth()) / 2);
                            y = fram.getY() + ((fram.getHeight() - dialog.getHeight()) / 2);
                            top_to_bot = true;
                        } else {
                            switch (location) {
                                case BOTTOM_RIGHT:
                                    x = fram.getX() + fram.getWidth() - dialog.getWidth() - margin;
                                    y = fram.getY() + fram.getHeight() - dialog.getHeight() - margin;
                                    top_to_bot = false;
                                    break;
                                // ... otros casos ...
                            }
                        }
                    }

                    baseX = x;
                    baseY = y;
                    fromBottom = !top_to_bot; // en bottom_right se apila hacia arriba

                    // Stacking: calcular offset seg·n cantidad de notificaciones activas
                    int idx;
                    synchronized (ACTIVE) {
                        ACTIVE.add(Notification.this);
                        idx = ACTIVE.indexOf(Notification.this);
                    }
                    int offsetY = (dialog.getHeight() + STACK_GAP) * idx;
                    int finalY = fromBottom ? (y - offsetY) : (y + offsetY);

                    top = finalY;
                    dialog.setLocation(x, finalY);
                    dialog.setVisible(true);
                }
            }

            @Override
            public void end() {
                showing = !showing;
                if (showing) {
                    thread = new Thread(() -> {
                        sleep();
                        closeNotification();
                    });
                    thread.start();
                } else {
                    dialog.dispose();
                    // Al cerrar, quitar de la pila y reposicionar restantes
                    synchronized (ACTIVE) {
                        ACTIVE.remove(Notification.this);
                        repositionActive();
                    }
                }
            }
        };
        animator = new Animator(500, target);
        animator.setResolution(5);
        expandAnimator = new Animator(200, new TimingTargetAdapter() {
            @Override
            public void timingEvent(float fraction) {
                int h = (int) (fraction * 120);
                detailsPanel.setPreferredSize(expanded ? new java.awt.Dimension(detailsPanel.getWidth(), h)
                        : new java.awt.Dimension(detailsPanel.getWidth(), 120 - h));
                detailsPanel.revalidate();
            }
            @Override
            public void end() {
                detailsPanel.setVisible(expanded);
            }
        });
    }

    public void showNotification() {
        animator.start();
    }

    private void closeNotification() {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
        if (animator.isRunning()) {
            if (!showing) {
                animator.stop();
                showing = true;
                animator.start();
            }
        } else {
            showing = true;
            animator.start();
        }
    }

    private void sleep() {
        try {
            Thread.sleep(displayDurationMs);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public void paint(Graphics grphcs) {
        Graphics2D g2 = (Graphics2D) grphcs.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.drawImage(imageShadow, 0, 0, null);
        int x = shadowSize;
        int y = shadowSize;
        int width = getWidth() - shadowSize * 2;
        int height = getHeight() - shadowSize * 2;
        g2.fillRect(x, y, width, height);
        g2.setColor(stripeColor);
        g2.fillRect(6, 5, 5, getHeight() - shadowSize * 2 + 1);
        g2.dispose();
        super.paint(grphcs);
    }

    @Override
    public void setBounds(int i, int i1, int i2, int i3) {
        super.setBounds(i, i1, i2, i3);
        createImageShadow();
    }

    private void createImageShadow() {
        imageShadow = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = imageShadow.createGraphics();
        g2.drawImage(createShadow(), 0, 0, null);
        g2.dispose();
    }

    private static void repositionActive() {
        synchronized (ACTIVE) {
            int idx = 0;
            for (Notification n : ACTIVE) {
                int offsetY = (n.dialog.getHeight() + STACK_GAP) * idx;
                int newY = n.fromBottom ? (n.baseY - offsetY) : (n.baseY + offsetY);
                n.dialog.setLocation(n.baseX, newY);
                idx++;
            }
        }
    }

    private BufferedImage createShadow() {
        BufferedImage img = new BufferedImage(getWidth() - shadowSize * 2, getHeight() - shadowSize * 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.fillRect(0, 0, img.getWidth(), img.getHeight());
        g2.dispose();
        return new ShadowRenderer(shadowSize, 0.3f, new Color(100, 100, 100)).createShadow(img);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lbIcon = new javax.swing.JLabel();
        panel = new javax.swing.JPanel();
        lbMessage = new javax.swing.JLabel();
        lbMessageText = new javax.swing.JLabel();
        cmdClose = new javax.swing.JButton();
        lbDate = new javax.swing.JLabel();
        header = new javax.swing.JPanel();
        detailsPanel = new javax.swing.JPanel();
        btnApprove = new javax.swing.JButton();
        btnReject = new javax.swing.JButton();

        lbIcon.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        panel.setOpaque(false);

        lbMessage.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        lbMessage.setForeground(UIManager.getColor("Label.foreground"));
        lbMessage.setText("Message");

        lbDate.setForeground(UIManager.getColor("Label.disabledForeground"));
        lbDate.setText("Fecha");

        header.setOpaque(false);
        javax.swing.GroupLayout headerLayout = new javax.swing.GroupLayout(header);
        header.setLayout(headerLayout);
        headerLayout.setHorizontalGroup(
            headerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(headerLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(headerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbMessage)
                    .addComponent(lbDate))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        headerLayout.setVerticalGroup(
            headerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(headerLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbMessage)
                .addGap(3, 3, 3)
                .addComponent(lbDate)
                .addContainerGap())
        );

        detailsPanel.setOpaque(false);
        detailsPanel.setVisible(false);
        lbMessageText.setForeground(UIManager.getColor("Label.foreground"));
        lbMessageText.setText("Message Text");
        btnApprove.setText("Aprobar");
        btnApprove.addActionListener(e -> { if (onApprove != null) onApprove.accept(this); markAsRead(true); });
        btnReject.setText("Rechazar");
        btnReject.addActionListener(e -> { if (onReject != null) onReject.accept(this); markAsRead(true); });
        javax.swing.GroupLayout detailsLayout = new javax.swing.GroupLayout(detailsPanel);
        detailsPanel.setLayout(detailsLayout);
        detailsLayout.setHorizontalGroup(
            detailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(detailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lbMessageText, javax.swing.GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE)
                    .addGroup(detailsLayout.createSequentialGroup()
                        .addComponent(btnApprove)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnReject)))
                .addContainerGap())
        );
        detailsLayout.setVerticalGroup(
            detailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lbMessageText)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(detailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnApprove)
                    .addComponent(btnReject))
                .addContainerGap())
        );

        cmdClose.setBorder(null);
        cmdClose.setContentAreaFilled(false);
        cmdClose.setFocusable(false);
        cmdClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmdCloseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(lbIcon)
                .addGap(10, 10, 10)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(header, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(detailsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cmdClose)
                .addGap(15, 15, 15))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cmdClose, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(header, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, 0)
                        .addComponent(detailsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(lbIcon, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(10, 10, 10))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void cmdCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmdCloseActionPerformed
        closeNotification();
    }//GEN-LAST:event_cmdCloseActionPerformed

    public static enum Type {
        SUCCESS, INFO, WARNING, URGENT, REMINDER
    }

    public static enum Location {
        TOP_CENTER, TOP_RIGHT, TOP_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT, BOTTOM_LEFT, CENTER
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    public int getPriority() { return priority; }
    public boolean isRead() { return read; }
    public void markAsRead(boolean value) {
        this.read = value;
        lbMessage.setFont(new java.awt.Font("sansserif", value ? java.awt.Font.PLAIN : java.awt.Font.BOLD, 14));
        repaint();
    }
    public void setOnApprove(java.util.function.Consumer<Notification> cb) { this.onApprove = cb; }
    public void setOnReject(java.util.function.Consumer<Notification> cb) { this.onReject = cb; }
    public Type getType() { return type; }
    public java.util.Date getTimestamp() { return timestamp; }
    private void toggleDetails() {
        expanded = !expanded;
        detailsPanel.setVisible(true);
        expandAnimator.start();
    }
    private void applyTypeStyle() {
        Color stripe;
        FontIcon iconToUse;
        Color iconColor = FlatLaf.isLafDark() ? Color.WHITE : UIManager.getColor("Label.foreground");
        switch (type) {
            case URGENT:
                stripe = new Color(198, 40, 40);
                iconToUse = createColoredIcon(FontAwesomeSolid.EXCLAMATION_TRIANGLE, iconColor);
                break;
            case REMINDER:
                stripe = new Color(255, 149, 0);
                iconToUse = createColoredIcon(FontAwesomeSolid.CLOCK, iconColor);
                break;
            case INFO:
                stripe = new Color(10, 132, 255);
                iconToUse = createColoredIcon(FontAwesomeSolid.INFO_CIRCLE, iconColor);
                break;
            case SUCCESS:
                stripe = new Color(46, 125, 50);
                iconToUse = createColoredIcon(FontAwesomeSolid.CHECK_CIRCLE, iconColor);
                break;
            default:
                stripe = new Color(241, 196, 15);
                iconToUse = createColoredIcon(FontAwesomeSolid.EXCLAMATION_CIRCLE, iconColor);
                break;
        }
        lbIcon.setIcon(iconToUse);
        this.setBackground(FlatLaf.isLafDark() ? new Color(36, 46, 59, 230) : new Color(255, 255, 255, 230));
        this.setOpaque(false);
        this.stripeColor = stripe;
    }

    public static void runVisualTest() {
        if (FlatLaf.isLafDark()) {
            FlatMacLightLaf.setup();
            FlatLaf.updateUI();
        }
        Notification n1 = new Notification(null, Type.INFO, Location.TOP_RIGHT, "Título", "Mensaje informativo", FontAwesomeSolid.INFO_CIRCLE, 3000);
        n1.showNotification();
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        FlatMacDarkLaf.setup();
        FlatLaf.updateUI();
        Notification n2 = new Notification(null, Type.URGENT, Location.TOP_RIGHT, "Urgente", "Mensaje urgente", FontAwesomeSolid.EXCLAMATION_TRIANGLE, 3000);
        n2.showNotification();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cmdClose;
    private javax.swing.JButton btnApprove;
    private javax.swing.JButton btnReject;
    private javax.swing.JPanel detailsPanel;
    private javax.swing.JPanel header;
    private javax.swing.JLabel lbIcon;
    private javax.swing.JLabel lbMessage;
    private javax.swing.JLabel lbMessageText;
    private javax.swing.JLabel lbDate;
    private javax.swing.JPanel panel;
    // End of variables declaration//GEN-END:variables
}
