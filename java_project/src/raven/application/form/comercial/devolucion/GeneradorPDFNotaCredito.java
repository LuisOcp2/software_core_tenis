package raven.application.form.comercial.devolucion;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.awt.Desktop;
import java.io.File;
import java.time.format.DateTimeFormatter;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import raven.controlador.comercial.ModelNotaCredito;
import java.io.FileOutputStream;

/**
 * Generador de PDF para Notas de Crédito usando iText 5
 * 
 * DEPENDENCIA: Agregar iText 5.5.13.3 al proyecto
 * Maven: com.itextpdf:itextpdf:5.5.13.3
 * 
 * @author Sistema
 * @version 1.0
 */
public class GeneradorPDFNotaCredito {
    
    private final DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter formatoFechaHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    // Colores corporativos
    private static final BaseColor COLOR_PRIMARIO = new BaseColor(33, 150, 243);
    private static final BaseColor COLOR_TEXTO = BaseColor.BLACK;
    private static final BaseColor COLOR_GRIS = new BaseColor(128, 128, 128);
    
    /**
     * Genera el PDF de la nota de crédito
     */
    public void generarPDF(ModelNotaCredito nota, java.awt.Component parent) throws Exception {
        // Diálogo para guardar archivo
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Nota de Crédito");
        fileChooser.setSelectedFile(new File(
                "NotaCredito_" + nota.getNumeroNotaCredito().replace("-", "_") + ".pdf"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Documents", "pdf"));
        
        int result = fileChooser.showSaveDialog(parent);
        
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        
        File archivo = fileChooser.getSelectedFile();
        if (!archivo.getName().toLowerCase().endsWith(".pdf")) {
            archivo = new File(archivo.getAbsolutePath() + ".pdf");
        }
        
        // Crear documento PDF
        Document document = new Document(PageSize.LETTER);
        PdfWriter.getInstance(document, new FileOutputStream(archivo));
        
        document.open();
        
        // Agregar contenido
        agregarEncabezado(document, nota);
        agregarInformacionGeneral(document, nota);
        agregarDetalleFinanciero(document, nota);
        agregarPiePagina(document, nota);
        
        document.close();
        
        // Preguntar si desea abrir el PDF
        int opcion = JOptionPane.showConfirmDialog(parent,
                "PDF generado exitosamente.\n¿Desea abrir el archivo?",
                "PDF Generado", JOptionPane.YES_NO_OPTION);
        
        if (opcion == JOptionPane.YES_OPTION) {
            Desktop.getDesktop().open(archivo);
        }
    }
    
    private void agregarEncabezado(Document document, ModelNotaCredito nota) throws DocumentException {
        // Tabla de encabezado con 2 columnas
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setSpacingAfter(20);
        
        // Logo/Nombre empresa (izquierda)
        PdfPCell celdaEmpresa = new PdfPCell();
        celdaEmpresa.setBorder(Rectangle.NO_BORDER);
        
        Font fontEmpresa = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, COLOR_PRIMARIO);
        Paragraph pEmpresa = new Paragraph("BODEGA DE ZAPATOS", fontEmpresa);
        pEmpresa.setAlignment(Element.ALIGN_LEFT);
        celdaEmpresa.addElement(pEmpresa);
        
        Font fontDireccion = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, COLOR_GRIS);
        Paragraph pDireccion = new Paragraph("Dirección de la empresa\nTeléfono: (000) 000-0000", fontDireccion);
        celdaEmpresa.addElement(pDireccion);
        
        tabla.addCell(celdaEmpresa);
        
        // Número de nota de crédito (derecha)
        PdfPCell celdaNota = new PdfPCell();
        celdaNota.setBorder(Rectangle.BOX);
        celdaNota.setBorderColor(COLOR_PRIMARIO);
        celdaNota.setBorderWidth(2);
        celdaNota.setBackgroundColor(new BaseColor(230, 240, 255));
        celdaNota.setPadding(10);
        
        Font fontTitulo = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, COLOR_PRIMARIO);
        Paragraph pTitulo = new Paragraph("NOTA DE CRÉDITO", fontTitulo);
        pTitulo.setAlignment(Element.ALIGN_CENTER);
        celdaNota.addElement(pTitulo);
        
        Font fontNumero = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
        Paragraph pNumero = new Paragraph(nota.getNumeroNotaCredito(), fontNumero);
        pNumero.setAlignment(Element.ALIGN_CENTER);
        celdaNota.addElement(pNumero);
        
        tabla.addCell(celdaNota);
        
        document.add(tabla);
    }
    
    private void agregarInformacionGeneral(Document document, ModelNotaCredito nota) throws DocumentException {
        Font fontTitulo = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, COLOR_PRIMARIO);
        Paragraph pTitulo = new Paragraph("INFORMACIÓN GENERAL", fontTitulo);
        pTitulo.setSpacingBefore(10);
        pTitulo.setSpacingAfter(10);
        document.add(pTitulo);
        
        // Tabla de información
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new int[]{30, 70});
        tabla.setSpacingAfter(20);
        
        Font fontLabel = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
        Font fontValor = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        
        // Fecha de emisión
        agregarFila(tabla, "Fecha de Emisión:", 
                nota.getFechaEmision() != null ? nota.getFechaEmision().format(formatoFechaHora) : "N/A",
                fontLabel, fontValor);
        
        // Fecha de vencimiento
        agregarFila(tabla, "Fecha de Vencimiento:", 
                nota.getFechaVencimiento() != null ? nota.getFechaVencimiento().format(formatoFecha) : "N/A",
                fontLabel, fontValor);
        
        // Cliente
        agregarFila(tabla, "Cliente:", nota.getClienteNombre(), fontLabel, fontValor);
        
        // DNI
        agregarFila(tabla, "DNI:", nota.getClienteDni(), fontLabel, fontValor);
        
        // Devolución asociada
        agregarFila(tabla, "Devolución:", nota.getNumeroDevolucion(), fontLabel, fontValor);
        
        // Estado
        agregarFila(tabla, "Estado:", nota.getEstado().getDescripcion(), fontLabel, fontValor);
        
        // Tipo
        agregarFila(tabla, "Tipo:", nota.getTipoNota().getDescripcion(), fontLabel, fontValor);
        
        document.add(tabla);
    }
    
   
    
    private void agregarDetalleFinanciero(Document document, ModelNotaCredito nota) throws DocumentException {
        Font fontTitulo = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, COLOR_PRIMARIO);
        Paragraph pTitulo = new Paragraph("DETALLE FINANCIERO", fontTitulo);
        pTitulo.setSpacingBefore(10);
        pTitulo.setSpacingAfter(10);
        document.add(pTitulo);
        
        // Tabla de detalle
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new int[]{70, 30});
        tabla.setSpacingAfter(20);
        
        Font fontLabel = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
        Font fontValor = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
        Font fontTotal = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, COLOR_PRIMARIO);
        
        // Subtotal
        agregarFilaFinanciera(tabla, "Subtotal:", 
                String.format("$%.2f", nota.getSubtotal()), fontLabel, fontValor);
        
        // IVA
        agregarFilaFinanciera(tabla, "IVA (19%):", 
                String.format("$%.2f", nota.getIva()), fontLabel, fontValor);
        
        // Línea separadora
        PdfPCell celdaSeparador = new PdfPCell(new Phrase(" "));
        celdaSeparador.setColspan(2);
        celdaSeparador.setBorder(Rectangle.TOP);
        celdaSeparador.setBorderWidth(2);
        celdaSeparador.setBorderColor(COLOR_PRIMARIO);
        tabla.addCell(celdaSeparador);
        
        // Total
        agregarFilaFinanciera(tabla, "TOTAL:", 
                String.format("$%.2f", nota.getTotal()), fontTotal, fontTotal);
        
        // Espacio
        PdfPCell celdaEspacio = new PdfPCell(new Phrase(" "));
        celdaEspacio.setColspan(2);
        celdaEspacio.setBorder(Rectangle.NO_BORDER);
        celdaEspacio.setFixedHeight(10);
        tabla.addCell(celdaEspacio);
        
        // Saldo usado
        agregarFilaFinanciera(tabla, "Saldo Usado:", 
                String.format("$%.2f", nota.getSaldoUsado()), fontLabel, fontValor);
        
        // Saldo disponible destacado
        PdfPCell celdaLabelSaldo = new PdfPCell(new Phrase("SALDO DISPONIBLE:", fontTotal));
        celdaLabelSaldo.setBorder(Rectangle.NO_BORDER);
        celdaLabelSaldo.setHorizontalAlignment(Element.ALIGN_RIGHT);
        celdaLabelSaldo.setPadding(5);
        celdaLabelSaldo.setBackgroundColor(new BaseColor(230, 255, 230));
        tabla.addCell(celdaLabelSaldo);
        
        Font fontSaldoDisponible = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, 
                new BaseColor(76, 175, 80));
        PdfPCell celdaSaldo = new PdfPCell(new Phrase(
                String.format("$%.2f", nota.getSaldoDisponible()), fontSaldoDisponible));
        celdaSaldo.setBorder(Rectangle.NO_BORDER);
        celdaSaldo.setHorizontalAlignment(Element.ALIGN_RIGHT);
        celdaSaldo.setPadding(5);
        celdaSaldo.setBackgroundColor(new BaseColor(230, 255, 230));
        tabla.addCell(celdaSaldo);
        
        document.add(tabla);
    }
    
    private void agregarPiePagina(Document document, ModelNotaCredito nota) throws DocumentException {
        // Términos y condiciones
        Font fontTitulo = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
        Paragraph pTitulo = new Paragraph("TÉRMINOS Y CONDICIONES", fontTitulo);
        pTitulo.setSpacingBefore(20);
        pTitulo.setSpacingAfter(5);
        document.add(pTitulo);
        
        Font fontTerminos = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, COLOR_GRIS);
        Paragraph pTerminos = new Paragraph(
                "• Esta nota de crédito es válida hasta el " + 
                (nota.getFechaVencimiento() != null ? nota.getFechaVencimiento().format(formatoFecha) : "N/A") + ".\n" +
                "• El saldo disponible puede ser utilizado en compras futuras.\n" +
                "• Esta nota de crédito no es reembolsable en efectivo.\n" +
                "• Conserve este documento para futuras transacciones.\n" +
                "• Para consultas, contacte al servicio de atención al cliente.",
                fontTerminos
        );
        pTerminos.setAlignment(Element.ALIGN_JUSTIFIED);
        document.add(pTerminos);
        
        // Firma
        Paragraph pFirma = new Paragraph("\n\n_____________________________\nAutorizado por", 
                new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, COLOR_GRIS));
        pFirma.setAlignment(Element.ALIGN_CENTER);
        pFirma.setSpacingBefore(30);
        document.add(pFirma);
    }
    
    private void agregarFila(PdfPTable tabla, String label, String valor, 
                            Font fontLabel, Font fontValor) {
        PdfPCell celdaLabel = new PdfPCell(new Phrase(label, fontLabel));
        celdaLabel.setBorder(Rectangle.NO_BORDER);
        celdaLabel.setPadding(5);
        celdaLabel.setBackgroundColor(new BaseColor(245, 245, 245));
        tabla.addCell(celdaLabel);
        
        PdfPCell celdaValor = new PdfPCell(new Phrase(valor != null ? valor : "N/A", fontValor));
        celdaValor.setBorder(Rectangle.NO_BORDER);
        celdaValor.setPadding(5);
        tabla.addCell(celdaValor);
    }
    
    private void agregarFilaFinanciera(PdfPTable tabla, String label, String valor, 
                                      Font fontLabel, Font fontValor) {
        PdfPCell celdaLabel = new PdfPCell(new Phrase(label, fontLabel));
        celdaLabel.setBorder(Rectangle.NO_BORDER);
        celdaLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        celdaLabel.setPadding(5);
        tabla.addCell(celdaLabel);
        
        PdfPCell celdaValor = new PdfPCell(new Phrase(valor, fontValor));
        celdaValor.setBorder(Rectangle.NO_BORDER);
        celdaValor.setHorizontalAlignment(Element.ALIGN_RIGHT);
        celdaValor.setPadding(5);
        tabla.addCell(celdaValor);
    }
}