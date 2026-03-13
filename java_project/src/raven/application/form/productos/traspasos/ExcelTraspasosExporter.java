package raven.application.form.productos.traspasos;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.awt.Desktop;

/**
 * Clase para exportar el reporte general de traspasos a Excel
 */
public class ExcelTraspasosExporter {

    public boolean exportar(List<TraspasoReporteDTO> listaTraspasos, File archivo, String filtrosAplicados) {
        // Crear libro de trabajo
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Traspasos");

            // Estilos
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
            dateStyle.setBorderBottom(BorderStyle.THIN);
            dateStyle.setBorderLeft(BorderStyle.THIN);
            dateStyle.setBorderRight(BorderStyle.THIN);

            CellStyle borderStyle = workbook.createCellStyle();
            borderStyle.setBorderBottom(BorderStyle.THIN);
            borderStyle.setBorderLeft(BorderStyle.THIN);
            borderStyle.setBorderRight(BorderStyle.THIN);
            borderStyle.setBorderTop(BorderStyle.THIN);

            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setAlignment(HorizontalAlignment.CENTER);
            numberStyle.setBorderBottom(BorderStyle.THIN);
            numberStyle.setBorderLeft(BorderStyle.THIN);
            numberStyle.setBorderRight(BorderStyle.THIN);
            numberStyle.setBorderTop(BorderStyle.THIN);

            CellStyle detailHeaderStyle = workbook.createCellStyle();
            Font detailFont = workbook.createFont();
            detailFont.setBold(true);
            detailFont.setColor(IndexedColors.BLACK.getIndex());
            detailHeaderStyle.setFont(detailFont);
            detailHeaderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            detailHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            detailHeaderStyle.setBorderBottom(BorderStyle.THIN);
            detailHeaderStyle.setBorderLeft(BorderStyle.THIN);
            detailHeaderStyle.setBorderRight(BorderStyle.THIN);

            CellStyle detailStyle = workbook.createCellStyle();
            detailStyle.setBorderBottom(BorderStyle.THIN);
            detailStyle.setBorderLeft(BorderStyle.THIN);
            detailStyle.setBorderRight(BorderStyle.THIN);
            detailStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());

            int rowNum = 0;

            // Título y filtros
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("REPORTE GENERAL DE TRASPASOS");

            Row filterRow = sheet.createRow(rowNum++);
            Cell filterCell = filterRow.createCell(0);
            filterCell.setCellValue("Filtros: " + filtrosAplicados);

            rowNum++; // Espacio

            // Encabezados de tabla
            // Encabezados de tabla
            String[] headers = { "No. Traspaso", "Bodega Origen", "Bodega Destino", "Fecha Solicitud", "Estado",
                    "Total Productos", "Total Env.", "Total Rec." };
            Row headerRow = sheet.createRow(rowNum++);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Datos
            for (TraspasoReporteDTO dto : listaTraspasos) {
                Row row = sheet.createRow(rowNum++);

                // No. Traspaso
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(dto.getNumero());
                cell0.setCellStyle(borderStyle);

                // Origen
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(dto.getOrigen());
                cell1.setCellStyle(borderStyle);

                // Destino
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(dto.getDestino());
                cell2.setCellStyle(borderStyle);

                // Fecha
                Cell cell3 = row.createCell(3);
                cell3.setCellValue(dto.getFecha());
                cell3.setCellStyle(borderStyle);

                // Estado
                Cell cell4 = row.createCell(4);
                cell4.setCellValue(dto.getEstado());
                cell4.setCellStyle(numberStyle);

                // Total Productos
                Cell cell5 = row.createCell(5);
                String prodVal = dto.getTotalProductos();
                try {
                    cell5.setCellValue(Integer.parseInt(prodVal));
                } catch (NumberFormatException e) {
                    cell5.setCellValue(prodVal);
                }
                cell5.setCellStyle(numberStyle);

                // Total Env
                Cell cell6 = row.createCell(6);
                if (dto.getMontoTotal() != null) {
                    cell6.setCellValue(dto.getMontoTotal().doubleValue());
                } else {
                    cell6.setCellValue(0);
                }
                cell6.setCellStyle(numberStyle);

                // Total Rec
                Cell cell7 = row.createCell(7);
                if (dto.getMontoRecibido() != null) {
                    cell7.setCellValue(dto.getMontoRecibido().doubleValue());
                } else {
                    cell7.setCellValue(0);
                }
                cell7.setCellStyle(numberStyle);

                // AGREGAR DETALLES SI EXISTEN
                if (dto.getDetalles() != null && !dto.getDetalles().isEmpty()) {
                    // Encabezados de detalle
                    Row detHeaderRow = sheet.createRow(rowNum++);

                    // Indentar detalles
                    Cell cblank = detHeaderRow.createCell(0);
                    cblank.setCellStyle(detailStyle);

                    Cell cDH1 = detHeaderRow.createCell(1);
                    cDH1.setCellValue("Producto");
                    cDH1.setCellStyle(detailHeaderStyle);
                    Cell cDH2 = detHeaderRow.createCell(2);
                    cDH2.setCellValue("Color - Talla");
                    cDH2.setCellStyle(detailHeaderStyle);
                    Cell cDH3 = detHeaderRow.createCell(3);
                    cDH3.setCellValue("Cant. Solic.");
                    cDH3.setCellStyle(detailHeaderStyle);
                    Cell cDH4 = detHeaderRow.createCell(4);
                    cDH4.setCellValue("Cant. Env.");
                    cDH4.setCellStyle(detailHeaderStyle);
                    Cell cDH5 = detHeaderRow.createCell(5);
                    cDH5.setCellValue("Cant. Rec.");
                    cDH5.setCellStyle(detailHeaderStyle);
                    Cell cDH6 = detHeaderRow.createCell(6);
                    cDH6.setCellValue("Precio U.");
                    cDH6.setCellStyle(detailHeaderStyle);
                    Cell cDH7 = detHeaderRow.createCell(7);
                    cDH7.setCellValue("Subtotal");
                    cDH7.setCellStyle(detailHeaderStyle);

                    for (java.util.Map<String, Object> det : dto.getDetalles()) {
                        Row detRow = sheet.createRow(rowNum++);
                        Cell cD0 = detRow.createCell(0);
                        cD0.setCellStyle(detailStyle); // Blank indent

                        String prodName = (String) det.get("producto_nombre");
                        String color = (String) det.get("color_nombre");
                        String talla = (String) det.get("talla_numero");
                        String variantInfo = (color != null ? color : "") + (talla != null ? " - " + talla : "");

                        Integer sol = (Integer) det.get("cantidad_solicitada");
                        Integer env = (Integer) det.get("cantidad_enviada");
                        Integer rec = (Integer) det.get("cantidad_recibida");

                        Cell cD1 = detRow.createCell(1);
                        cD1.setCellValue(prodName);
                        cD1.setCellStyle(detailStyle);
                        Cell cD2 = detRow.createCell(2);
                        cD2.setCellValue(variantInfo);
                        cD2.setCellStyle(detailStyle);
                        Cell cD3 = detRow.createCell(3);
                        cD3.setCellValue(sol != null ? sol : 0);
                        cD3.setCellStyle(detailStyle);
                        Cell cD4 = detRow.createCell(4);
                        cD4.setCellValue(env != null ? env : 0);
                        cD4.setCellStyle(detailStyle);
                        Cell cD5 = detRow.createCell(5);
                        cD5.setCellValue(rec != null ? rec : 0);
                        cD5.setCellStyle(detailStyle);

                        // Precio Unitario
                        Cell cD6 = detRow.createCell(6);
                        if (det.get("precio_unitario") != null) {
                            java.math.BigDecimal val = (java.math.BigDecimal) det.get("precio_unitario");
                            cD6.setCellValue(val.doubleValue());
                        } else {
                            cD6.setCellValue(0);
                        }
                        cD6.setCellStyle(detailStyle);

                        // Subtotal
                        Cell cD7 = detRow.createCell(7);
                        if (det.get("subtotal") != null) {
                            java.math.BigDecimal val = (java.math.BigDecimal) det.get("subtotal");
                            cD7.setCellValue(val.doubleValue());
                        } else {
                            cD7.setCellValue(0);
                        }
                        cD7.setCellStyle(detailStyle);
                    }
                    // Fila vacia separadora
                    rowNum++;
                }
            }

            // Auto-size columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Guardar archivo
            try (FileOutputStream out = new FileOutputStream(archivo)) {
                workbook.write(out);
            }

            // Abrir automáticamente
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(archivo);
                }
            } catch (Exception ignore) {
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
