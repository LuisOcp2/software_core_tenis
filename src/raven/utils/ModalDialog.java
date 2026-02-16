package raven.utils;

import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Utilidad para mostrar paneles como diálogos modales.
 */
public class ModalDialog {
    
    /**
     * Interfaz funcional para ejecutar código después de cerrar un diálogo modal.
     */
    public interface OnCloseListener {
        void onClose();
    }
    
    /**
     * Muestra un panel como un diálogo modal.
     * @param panel Panel a mostrar
     * @param title Título del diálogo
     * @param modalityType Tipo de modalidad
     * @param onCloseListener Listener a ejecutar cuando se cierre el diálogo
     */
    public static void showModal(JPanel panel, String title, ModalityType modalityType, OnCloseListener onCloseListener) {
        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog();
            dialog.setTitle(title);
            dialog.setModalityType(modalityType);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setContentPane(panel);
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            
            // Establecer tamaño mínimo
            Dimension size = panel.getPreferredSize();
            dialog.setMinimumSize(new Dimension(
                    Math.max(size.width, 600),
                    Math.max(size.height, 500)));
            
            // Agregar listener para ejecutar código al cerrar
            if (onCloseListener != null) {
                dialog.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        onCloseListener.onClose();
                    }
                });
            }
            
            dialog.setVisible(true);
        });
    }
    
    /**
     * Muestra un panel como un diálogo modal sin listener de cierre.
     * @param panel Panel a mostrar
     * @param title Título del diálogo
     * @param modalityType Tipo de modalidad
     */
    public static void showModal(JPanel panel, String title, ModalityType modalityType) {
        showModal(panel, title, modalityType, null);
    }
}