package raven.application.form.comercial.devolucion.components;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.*;
import javax.swing.*;

/**
 * Stepper horizontal moderno basado en diseño HTML sketch.
 * Muestra progreso visual con círculos conectados por líneas.
 * Estados: ACTIVE (azul), COMPLETED (verde con ✓), PENDING (gris)
 */
public class StepperPanel extends JPanel {

    // Colores del diseño
    private static final Color ACTIVE_COLOR = new Color(59, 130, 246); // #3b82f6
    private static final Color COMPLETED_COLOR = new Color(16, 185, 129); // #10b981
    private static final Color PENDING_COLOR = new Color(74, 85, 104); // #4a5568
    private static final Color LINE_COLOR = new Color(74, 85, 104); // #4a5568
    private static final Color TEXT_ACTIVE = new Color(59, 130, 246); // #3b82f6
    private static final Color TEXT_NORMAL = new Color(160, 174, 192); // #a0aec0

    private final Step[] steps;
    private int currentStep = 1;

    public StepperPanel(String... stepLabels) {
        this.steps = new Step[stepLabels.length];
        for (int i = 0; i < stepLabels.length; i++) {
            steps[i] = new Step(i + 1, stepLabels[i]);
        }

        initComponents();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(12, 20, 12, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        for (int i = 0; i < steps.length; i++) {
            // Step circle panel
            gbc.gridx = i * 2;
            gbc.weightx = 0;
            add(steps[i], gbc);

            // Connector line (except after last step)
            if (i < steps.length - 1) {
                gbc.gridx = i * 2 + 1;
                gbc.weightx = 1;
                add(createConnectorLine(), gbc);
            }
        }
    }

    private JPanel createConnectorLine() {
        JPanel line = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(LINE_COLOR);
                int y = getHeight() / 2;
                g2.fillRect(0, y - 1, getWidth(), 2);
                g2.dispose();
            }
        };
        line.setOpaque(false);
        line.setPreferredSize(new Dimension(100, 40));
        return line;
    }

    public void setCurrentStep(int step) {
        if (step < 1 || step > steps.length)
            return;
        this.currentStep = step;
        updateSteps();
    }

    private void updateSteps() {
        for (int i = 0; i < steps.length; i++) {
            int stepNumber = i + 1;
            if (stepNumber < currentStep) {
                steps[i].setState(StepState.COMPLETED);
            } else if (stepNumber == currentStep) {
                steps[i].setState(StepState.ACTIVE);
            } else {
                steps[i].setState(StepState.PENDING);
            }
        }
        revalidate();
        repaint();
    }

    // ========== INNER CLASSES ==========

    private enum StepState {
        ACTIVE, COMPLETED, PENDING
    }

    private class Step extends JPanel {
        private final int number;
        private final String label;
        private StepState state = StepState.PENDING;

        private JLabel circle;
        private JLabel textLabel;

        public Step(int number, String label) {
            this.number = number;
            this.label = label;
            initComponents();
        }

        private void initComponents() {
            setLayout(new BorderLayout(0, 8));
            setOpaque(false);

            // Circle
            circle = new JLabel(String.valueOf(number), SwingConstants.CENTER);
            circle.setPreferredSize(new Dimension(40, 40));
            circle.setFont(new Font("Segoe UI", Font.BOLD, 16));
            circle.setOpaque(true);
            circle.putClientProperty(FlatClientProperties.STYLE,
                    "arc:999;" +
                            "background:" + colorToHex(PENDING_COLOR) + ";" +
                            "foreground:#ffffff");

            // Label
            textLabel = new JLabel(label, SwingConstants.CENTER);
            textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            textLabel.setForeground(TEXT_NORMAL);

            add(circle, BorderLayout.NORTH);
            add(textLabel, BorderLayout.CENTER);

            setState(StepState.PENDING);
        }

        public void setState(StepState newState) {
            this.state = newState;

            switch (state) {
                case ACTIVE:
                    circle.setText(String.valueOf(number));
                    circle.putClientProperty(FlatClientProperties.STYLE,
                            "arc:999;" +
                                    "background:" + colorToHex(ACTIVE_COLOR) + ";" +
                                    "foreground:#ffffff");
                    textLabel.setForeground(TEXT_ACTIVE);
                    textLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    break;

                case COMPLETED:
                    circle.setText("✓");
                    circle.putClientProperty(FlatClientProperties.STYLE,
                            "arc:999;" +
                                    "background:" + colorToHex(COMPLETED_COLOR) + ";" +
                                    "foreground:#ffffff");
                    textLabel.setForeground(TEXT_NORMAL);
                    textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    break;

                case PENDING:
                    circle.setText(String.valueOf(number));
                    circle.putClientProperty(FlatClientProperties.STYLE,
                            "arc:999;" +
                                    "background:" + colorToHex(PENDING_COLOR) + ";" +
                                    "foreground:#a0aec0");
                    textLabel.setForeground(TEXT_NORMAL);
                    textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    break;
            }
        }
    }

    private static String colorToHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
