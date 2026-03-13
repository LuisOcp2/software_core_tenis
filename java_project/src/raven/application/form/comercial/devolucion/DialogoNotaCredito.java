package raven.application.form.comercial.devolucion;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.*;
import java.awt.print.PrinterException;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import raven.controlador.comercial.ModelNotaCredito;

/**
 * Diálogo para visualizar Nota de Crédito
 * 
 * @author Sistema
 * @version 1.1 - Actualizado con manejo de nulls y mejoras visuales
 */
public class DialogoNotaCredito extends JDialog {

    private final ModelNotaCredito notaCredito;
    private final DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter formatoFechaHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private JButton btnImprimir;
    private JButton btnCerrar;
    private JTextArea txtContenidoImpresion;

    // ANCHOS IMPRESIÓN (Caracteres aproximados para fuente Monospaced 8-10pt)
    private static final int ANCHO_TICKET_58MM = 32;
    private static final int ANCHO_TICKET_80MM = 48;
    private static final int ANCHO_CARTA = 80;

    public DialogoNotaCredito(Frame parent, ModelNotaCredito notaCredito) {
        super(parent, "Nota de Crédito", true);
        this.notaCredito = notaCredito;

        inicializarComponentes();
        setLocationRelativeTo(parent);

        // Auto-copiar número al portapapeles
        if (notaCredito != null && notaCredito.getNumeroNotaCredito() != null) {
            try {
                java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(
                        notaCredito.getNumeroNotaCredito());
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);

                // Mostrar notificación temporal pequeña
                Timer timer = new Timer(1000, e -> {
                    // Solo para efecto visual inicial
                });
                timer.setRepeats(false);
                timer.start();

                setTitle("Nota de Crédito - " + notaCredito.getNumeroNotaCredito() + " (Copiado)");
            } catch (Exception e) {
                System.err.println("Error al copiar al portapapeles: " + e.getMessage());
            }
        }
    }

    private void inicializarComponentes() {
        setSize(600, 750); // SUCCESS Aumentado un poco para mejor visualización
        setLayout(new BorderLayout(10, 10));

        // Panel principal
        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Encabezado
        JPanel panelEncabezado = crearPanelEncabezado();

        // Información
        JPanel panelInfo = crearPanelInformacion();

        // Detalle financiero
        JPanel panelDetalle = crearPanelDetalle();

        // SUCCESS NUEVO: Panel de términos y condiciones
        JPanel panelTerminos = crearPanelTerminos();

        // Panel de botones
        JPanel panelBotones = crearPanelBotones();

        // Agregar componentes
        JPanel panelContenido = new JPanel();
        panelContenido.setLayout(new BoxLayout(panelContenido, BoxLayout.Y_AXIS));
        panelContenido.add(panelEncabezado);
        panelContenido.add(Box.createVerticalStrut(20));
        panelContenido.add(panelInfo);
        panelContenido.add(Box.createVerticalStrut(20));
        panelContenido.add(panelDetalle);
        panelContenido.add(Box.createVerticalStrut(15));
        panelContenido.add(panelTerminos);

        // SUCCESS NUEVO: Agregar scroll al contenido completo
        JScrollPane scrollPrincipal = new JScrollPane(panelContenido);
        scrollPrincipal.setBorder(null);

        panelPrincipal.add(scrollPrincipal, BorderLayout.CENTER);
        panelPrincipal.add(panelBotones, BorderLayout.SOUTH);

        add(panelPrincipal);

        // SUCCESS NUEVO: Preparar contenido para impresión
        prepararContenidoImpresion();
    }

    private JPanel crearPanelEncabezado() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(33, 150, 243));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel lblTitulo = new JLabel("NOTA DE CRÉDITO", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitulo.setForeground(Color.WHITE);

        // SUCCESS MEJORADO: Manejo seguro de null
        String numeroNC = notaCredito.getNumeroNotaCredito() != null
                ? notaCredito.getNumeroNotaCredito()
                : "NC-XXXXXX";

        JLabel lblNumero = new JLabel(numeroNC, SwingConstants.CENTER);
        lblNumero.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblNumero.setForeground(Color.WHITE);

        panel.add(lblTitulo, BorderLayout.NORTH);
        panel.add(lblNumero, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearPanelInformacion() {
        JPanel panel = new JPanel(new GridLayout(9, 2, 10, 10)); // SUCCESS Aumentado a 9 filas
        panel.setBorder(BorderFactory.createTitledBorder("Información General"));

        // Fecha de emisión
        panel.add(crearLabel("Fecha de Emisión:"));
        panel.add(crearLabelValor(notaCredito.getFechaEmision() != null
                ? notaCredito.getFechaEmision().format(formatoFechaHora)
                : "N/A"));

        // Fecha de vencimiento - SUCCESS MEJORADO: Con alerta si está próximo a vencer
        panel.add(crearLabel("Fecha de Vencimiento:"));
        JLabel lblVencimiento = crearLabelValor(notaCredito.getFechaVencimiento() != null
                ? notaCredito.getFechaVencimiento().format(formatoFecha)
                : "N/A");

        // SUCCESS NUEVO: Alerta si está próximo a vencer (menos de 7 días)
        if (notaCredito.getFechaVencimiento() != null) {
            long diasRestantes = java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDateTime.now(),
                    notaCredito.getFechaVencimiento());

            if (diasRestantes <= 7 && diasRestantes > 0) {
                lblVencimiento.setForeground(new Color(255, 152, 0)); // Naranja
                lblVencimiento.setFont(new Font("Segoe UI", Font.BOLD, 12));
            } else if (diasRestantes <= 0) {
                lblVencimiento.setForeground(new Color(244, 67, 54)); // Rojo
                lblVencimiento.setFont(new Font("Segoe UI", Font.BOLD, 12));
                lblVencimiento.setText(lblVencimiento.getText() + " - VENCIDA");
            }
        }
        panel.add(lblVencimiento);

        // Cliente
        panel.add(crearLabel("Cliente:"));
        panel.add(crearLabelValor(notaCredito.getClienteNombre()));

        // DNI
        panel.add(crearLabel("DNI:"));
        panel.add(crearLabelValor(notaCredito.getClienteDni()));

        // Devolución asociada
        panel.add(crearLabel("Devolución:"));
        panel.add(crearLabelValor(notaCredito.getNumeroDevolucion()));

        // Estado - SUCCESS MEJORADO: Color según estado
        panel.add(crearLabel("Estado:"));
        JLabel lblEstado = crearLabelValor(
                notaCredito.getEstado() != null
                        ? notaCredito.getEstado().getDescripcion()
                        : "N/A");

        // Color según estado
        if (notaCredito.getEstado() != null) {
            switch (notaCredito.getEstado()) {
                case EMITIDA:
                    lblEstado.setForeground(new Color(76, 175, 80)); // Verde
                    break;
                case APLICADA:
                    lblEstado.setForeground(new Color(33, 150, 243)); // Azul
                    break;
                case ANULADA:
                    lblEstado.setForeground(new Color(244, 67, 54)); // Rojo
                    break;
                case VENCIDA:
                    lblEstado.setForeground(new Color(158, 158, 158)); // Gris
                    break;
            }
        }
        lblEstado.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.add(lblEstado);

        // Tipo
        panel.add(crearLabel("Tipo:"));
        panel.add(crearLabelValor(
                notaCredito.getTipoNota() != null
                        ? notaCredito.getTipoNota().getDescripcion()
                        : "N/A"));

        // SUCCESS NUEVO: Usuario que generó
        panel.add(crearLabel("Generada por:"));
        panel.add(crearLabelValor("Usuario ID: " + notaCredito.getIdUsuarioGenera()));

        // Observaciones
        panel.add(crearLabel("Observaciones:"));
        JTextArea txtObs = new JTextArea(
                notaCredito.getObservaciones() != null
                        ? notaCredito.getObservaciones()
                        : "Sin observaciones");
        txtObs.setEditable(false);
        txtObs.setLineWrap(true);
        txtObs.setWrapStyleWord(true);
        JScrollPane scrollObs = new JScrollPane(txtObs);
        scrollObs.setPreferredSize(new Dimension(200, 50));
        panel.add(scrollObs);

        return panel;
    }

    private JPanel crearPanelDetalle() {
        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Detalle Financiero"));

        // Subtotal
        panel.add(crearLabel("Subtotal:"));
        panel.add(crearLabelValor(String.format("$%,.2f",
                notaCredito.getSubtotal() != null ? notaCredito.getSubtotal() : 0)));

        // IVA
        panel.add(crearLabel("IVA (19%):"));
        panel.add(crearLabelValor(String.format("$%,.2f",
                notaCredito.getIva() != null ? notaCredito.getIva() : 0)));

        // Total
        panel.add(crearLabel("TOTAL:"));
        JLabel lblTotal = crearLabelValor(String.format("$%,.2f",
                notaCredito.getTotal() != null ? notaCredito.getTotal() : 0));
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTotal.setForeground(new Color(33, 150, 243));
        panel.add(lblTotal);

        // Saldo usado
        panel.add(crearLabel("Saldo Usado:"));
        panel.add(crearLabelValor(String.format("$%,.2f",
                notaCredito.getSaldoUsado() != null ? notaCredito.getSaldoUsado() : 0)));

        // Saldo disponible
        panel.add(crearLabel("SALDO DISPONIBLE:"));
        JLabel lblSaldo = crearLabelValor(String.format("$%,.2f",
                notaCredito.getSaldoDisponible() != null ? notaCredito.getSaldoDisponible() : 0));
        lblSaldo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblSaldo.setForeground(new Color(76, 175, 80));
        panel.add(lblSaldo);

        return panel;
    }

    /**
     * SUCCESS NUEVO: Panel de términos y condiciones
     */
    private JPanel crearPanelTerminos() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Términos y Condiciones"));

        JTextArea txtTerminos = new JTextArea();
        txtTerminos.setEditable(false);
        txtTerminos.setLineWrap(true);
        txtTerminos.setWrapStyleWord(true);
        txtTerminos.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        txtTerminos.setText(
                "• Esta nota de crédito es válida por 30 días desde su emisión.\n" +
                        "• Puede ser utilizada para futuras compras en cualquier sucursal.\n" +
                        "• No es canjeable por dinero en efectivo.\n" +
                        "• Debe presentarse al momento de realizar la compra.\n" +
                        "• En caso de pérdida, no se realizarán reposiciones.");

        JScrollPane scroll = new JScrollPane(txtTerminos);
        scroll.setPreferredSize(new Dimension(0, 100));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearPanelBotones() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        btnImprimir = new JButton("Imprimir Nota de Crédito");
        btnImprimir.putClientProperty(FlatClientProperties.STYLE,
                "arc:10;background:#2196F3;foreground:#FFFFFF;font:bold");
        btnImprimir.addActionListener(e -> imprimirNotaCredito());

        btnCerrar = new JButton("Cerrar");
        btnCerrar.putClientProperty(FlatClientProperties.STYLE, "arc:10");
        btnCerrar.addActionListener(e -> dispose());

        panel.add(btnImprimir);
        panel.add(btnCerrar);

        return panel;
    }

    private JLabel crearLabel(String texto) {
        JLabel label = new JLabel(texto);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return label;
    }

    private JLabel crearLabelValor(String texto) {
        JLabel label = new JLabel(texto != null ? texto : "N/A");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return label;
    }

    /**
     * SUCCESS NUEVO: Prepara contenido para impresión rápida
     */
    /**
     * SUCCESS NUEVO: Prepara contenido para impresión rápida
     */
    private void prepararContenidoImpresion() {
        txtContenidoImpresion = new JTextArea();
        txtContenidoImpresion.setFont(new Font("Courier New", Font.PLAIN, 10));
        // Por defecto formato 80mm
        txtContenidoImpresion.setText(generarContenidoTexto(ANCHO_TICKET_80MM));
    }

    /**
     * SUCCESS NUEVO: Genera contenido dinámico según el ancho disponible
     */
    private String generarContenidoTexto(int ancho) {
        StringBuilder sb = new StringBuilder();
        String linea = "=".repeat(ancho) + "\n";
        String separador = "-".repeat(ancho) + "\n";

        sb.append(linea);
        sb.append(centrarTexto("NOTA DE CRÉDITO", ancho)).append("\n");
        sb.append(linea).append("\n");

        sb.append(formatearPar("Número:",
                notaCredito.getNumeroNotaCredito() != null ? notaCredito.getNumeroNotaCredito() : "N/A", ancho));
        sb.append(formatearPar("Fecha:",
                notaCredito.getFechaEmision() != null ? notaCredito.getFechaEmision().format(formatoFechaHora) : "N/A",
                ancho));
        sb.append(formatearPar("Vence:",
                notaCredito.getFechaVencimiento() != null ? notaCredito.getFechaVencimiento().format(formatoFecha)
                        : "N/A",
                ancho));

        sb.append("\n").append(separador).append("\n");

        sb.append(formatearPar("Cliente:",
                notaCredito.getClienteNombre() != null ? notaCredito.getClienteNombre() : "N/A", ancho));
        sb.append(formatearPar("DNI:",
                notaCredito.getClienteDni() != null ? notaCredito.getClienteDni() : "N/A", ancho));
        sb.append(formatearPar("Ref. Dev:",
                notaCredito.getNumeroDevolucion() != null ? notaCredito.getNumeroDevolucion() : "N/A", ancho));

        sb.append("\n").append(separador);
        sb.append(centrarTexto("DETALLE FINANCIERO", ancho)).append("\n\n");

        sb.append(formatearPar("Subtotal:", String.format("$%,.2f", notaCredito.getSubtotal()), ancho));
        sb.append(formatearPar("IVA:", String.format("$%,.2f", notaCredito.getIva()), ancho));
        sb.append(formatearPar("TOTAL:", String.format("$%,.2f", notaCredito.getTotal()), ancho));

        sb.append("\n").append(separador);
        sb.append(centrarTexto("SALDOS", ancho)).append("\n\n");

        sb.append(formatearPar("Estado:",
                notaCredito.getEstado() != null ? notaCredito.getEstado().getDescripcion() : "N/A", ancho));
        sb.append(formatearPar("Disponible:", String.format("$%,.2f", notaCredito.getSaldoDisponible()), ancho));

        if (notaCredito.getObservaciones() != null && !notaCredito.getObservaciones().trim().isEmpty()) {
            sb.append("\n").append(separador);
            sb.append("OBSERVACIONES:\n");
            sb.append(wrapText(notaCredito.getObservaciones(), ancho)).append("\n");
        }

        sb.append("\n").append(linea).append("\n");
        sb.append(centrarTexto("GRACIAS POR SU PREFERENCIA", ancho)).append("\n");
        sb.append(linea);

        return sb.toString();
    }

    private String centrarTexto(String texto, int ancho) {
        if (texto.length() >= ancho)
            return texto;
        int padding = (ancho - texto.length()) / 2;
        return " ".repeat(padding) + texto;
    }

    private String formatearPar(String label, String valor, int ancho) {
        // Formato: "Label: Valor"
        int espacioLabel = ancho / 2;
        int espacioValor = ancho - espacioLabel;

        // Truncar si es necesario
        if (label.length() > espacioLabel)
            label = label.substring(0, espacioLabel - 1);
        if (valor.length() > espacioValor)
            valor = valor.substring(0, espacioValor);

        return String.format("%-" + espacioLabel + "s%s\n", label, valor.trim()); // Alineado izq el label, el valor
                                                                                  // depende
    }

    private String wrapText(String text, int width) {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        while (index < text.length()) {
            int endIndex = Math.min(index + width, text.length());
            sb.append(text.substring(index, endIndex)).append("\n");
            index += width;
        }
        return sb.toString();
    }

    /**
     * SUCCESS MODIFICADO: Método de impresión con dos opciones
     */
    private void imprimirNotaCredito() {
        // Crear opciones
        String[] opciones = { "Impresión Rápida", "Generar PDF", "Cancelar" };

        int seleccion = JOptionPane.showOptionDialog(this,
                "Seleccione el método de impresión:",
                "Imprimir Nota de Crédito",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]);

        try {
            if (seleccion == 0) {
                // DIALOGO SECUNDARIO PARA FORMATO
                String[] formatos = { "Ticket 80mm", "Ticket 58mm", "Carta/A4" };
                int resp = JOptionPane.showOptionDialog(this,
                        "Seleccione el formato de papel:",
                        "Formato de Impresión",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        formatos,
                        formatos[0]);

                if (resp >= 0) {
                    int ancho = ANCHO_TICKET_80MM;
                    if (resp == 1)
                        ancho = ANCHO_TICKET_58MM;
                    if (resp == 2)
                        ancho = ANCHO_CARTA;

                    // Regenerar contenido con el ancho seleccionado
                    txtContenidoImpresion.setText(generarContenidoTexto(ancho));
                    imprimirTextoPlano();
                }
            } else if (seleccion == 1) {
                // Generar PDF
                generarPDF();
            }
        } catch (Exception e) {
            System.err.println("ERROR  Error en impresión: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error en impresión: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * SUCCESS NUEVO: Impresión rápida en texto plano
     */
    private void imprimirTextoPlano() throws PrinterException {
        boolean impreso = txtContenidoImpresion.print();

        if (impreso) {
            JOptionPane.showMessageDialog(this,
                    "Nota de crédito enviada a impresión correctamente",
                    "Impresión Exitosa", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * SUCCESS MEJORADO: Generación de PDF
     */
    private void generarPDF() {
        try {
            GeneradorPDFNotaCredito generador = new GeneradorPDFNotaCredito();
            generador.generarPDF(notaCredito, this);

            JOptionPane.showMessageDialog(this,
                    "PDF generado exitosamente",
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            System.err.println("ERROR  Error generando PDF: " + e.getMessage());
            throw new RuntimeException("Error generando PDF", e);
        }
    }
}
