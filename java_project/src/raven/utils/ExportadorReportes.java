package raven.utils;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableModel;

/**
 * Utilidad para exportar tablas a Excel, PDF y CSV
 */
public class ExportadorReportes {

    /**
     * Exporta una JTable a archivo Excel (CSV con extensión xls para
     * compatibilidad)
     */
    public static void exportarExcel(JTable tabla, String nombreBase) {
        if (tabla == null || tabla.getRowCount() == 0) {
            JOptionPane.showMessageDialog(null, "No hay datos para exportar", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar como Excel");
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
        String sanitizedBase = nombreBase.replaceAll("[^a-zA-Z0-9-_]", "_");
        fileChooser.setSelectedFile(new File(sanitizedBase + "_" + timestamp + ".xls"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos Excel (*.xls)", "xls"));

        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".xls")) {
                file = new File(file.getAbsolutePath() + ".xls");
            }
            try (FileWriter writer = new FileWriter(file)) {
                TableModel model = tabla.getModel();

                // Escribir cabeceras
                for (int i = 0; i < model.getColumnCount(); i++) {
                    writer.write(model.getColumnName(i) + "\t");
                }
                writer.write("\n");

                // Escribir datos
                for (int row = 0; row < model.getRowCount(); row++) {
                    for (int col = 0; col < model.getColumnCount(); col++) {
                        Object val = model.getValueAt(row, col);
                        writer.write((val != null ? val.toString() : "") + "\t");
                    }
                    writer.write("\n");
                }

                JOptionPane.showMessageDialog(null,
                        "Archivo exportado exitosamente:\n" + file.getAbsolutePath(),
                        "Éxito", JOptionPane.INFORMATION_MESSAGE);

                // Abrir el archivo
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Error al exportar: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Exporta una JTable a archivo CSV
     */
    public static void exportarCSV(JTable tabla, String nombreBase) {
        if (tabla == null || tabla.getRowCount() == 0) {
            JOptionPane.showMessageDialog(null, "No hay datos para exportar", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar como CSV");
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
        String sanitizedBase = nombreBase.replaceAll("[^a-zA-Z0-9-_]", "_");
        fileChooser.setSelectedFile(new File(sanitizedBase + "_" + timestamp + ".csv"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos CSV (*.csv)", "csv"));

        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getAbsolutePath() + ".csv");
            }
            try (FileWriter writer = new FileWriter(file)) {
                TableModel model = tabla.getModel();

                // Cabeceras
                for (int i = 0; i < model.getColumnCount(); i++) {
                    if (i > 0)
                        writer.write(",");
                    writer.write("\"" + model.getColumnName(i) + "\"");
                }
                writer.write("\n");

                // Datos
                for (int row = 0; row < model.getRowCount(); row++) {
                    for (int col = 0; col < model.getColumnCount(); col++) {
                        if (col > 0)
                            writer.write(",");
                        Object val = model.getValueAt(row, col);
                        String str = val != null ? val.toString().replace("\"", "\"\"") : "";
                        writer.write("\"" + str + "\"");
                    }
                    writer.write("\n");
                }

                JOptionPane.showMessageDialog(null,
                        "Archivo CSV exportado:\n" + file.getAbsolutePath(),
                        "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Error al exportar CSV: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Exporta una JTable a un archivo PDF real usando iText.
     */
    public static void exportarPDF(JTable tabla, String titulo) {
        if (tabla == null || tabla.getRowCount() == 0) {
            JOptionPane.showMessageDialog(null, "No hay datos para exportar", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar como PDF");
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
        String nombreBase = titulo.toLowerCase().replaceAll("[^a-zA-Z0-9-_]", "_");
        fileChooser.setSelectedFile(new File(nombreBase + "_" + timestamp + ".pdf"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos PDF (*.pdf)", "pdf"));

        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".pdf")) {
                file = new File(file.getAbsolutePath() + ".pdf");
            }

            TableModel model = tabla.getModel();
            Document document = new Document(PageSize.A4.rotate());

            try {
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
                Paragraph titleParagraph = new Paragraph(titulo, titleFont);
                titleParagraph.setAlignment(Element.ALIGN_CENTER);
                titleParagraph.setSpacingAfter(15);
                document.add(titleParagraph);

                Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
                String fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
                Paragraph info = new Paragraph("Generado: " + fecha + "   |   Total registros: " + model.getRowCount(),
                        infoFont);
                info.setAlignment(Element.ALIGN_RIGHT);
                info.setSpacingAfter(10);
                document.add(info);

                PdfPTable pdfTable = new PdfPTable(model.getColumnCount());
                pdfTable.setWidthPercentage(100);

                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.WHITE);
                for (int i = 0; i < model.getColumnCount(); i++) {
                    String header = model.getColumnName(i);
                    PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                    cell.setBackgroundColor(new BaseColor(33, 150, 243));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setPadding(5);
                    pdfTable.addCell(cell);
                }

                Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
                for (int row = 0; row < model.getRowCount(); row++) {
                    for (int col = 0; col < model.getColumnCount(); col++) {
                        Object val = model.getValueAt(row, col);
                        String str = val != null ? val.toString() : "";
                        PdfPCell cell = new PdfPCell(new Phrase(str, dataFont));
                        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        cell.setPadding(4);
                        pdfTable.addCell(cell);
                    }
                }

                document.add(pdfTable);

                JOptionPane.showMessageDialog(null,
                        "PDF exportado:\n" + file.getAbsolutePath(),
                        "Éxito", JOptionPane.INFORMATION_MESSAGE);

                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                }
            } catch (DocumentException | java.io.IOException e) {
                JOptionPane.showMessageDialog(null,
                        "Error al exportar PDF: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                if (document.isOpen()) {
                    document.close();
                }
            }
        }
    }
}
