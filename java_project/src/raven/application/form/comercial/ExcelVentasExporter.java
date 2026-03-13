package raven.application.form.comercial;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

public class ExcelVentasExporter {

    public boolean exportar(List<VentaReporteDTO> ventas, File archivo, String filtros) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Reporte de Ventas");

            // Estilos
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle detailHeaderStyle = createDetailHeaderStyle(workbook);
            CellStyle detailStyle = createDetailStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            // Título
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("REPORTE DETALLADO DE VENTAS");
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            // Filtros
            Row filterRow = sheet.createRow(1);
            Cell filterCell = filterRow.createCell(0);
            filterCell.setCellValue("Filtros: " + filtros);

            // Encabezados
            String[] headers = { "ID Venta", "Fecha", "Cliente", "Vendedor", "Estado", "Tipo Pago", "Total" };
            Row headerRow = sheet.createRow(3);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 4;
            double grandTotal = 0;

            for (VentaReporteDTO venta : ventas) {
                Row row = sheet.createRow(rowNum++);
                createCell(row, 0, venta.getIdVenta(), dataStyle);
                createCell(row, 1, venta.getFecha(), dateStyle); // Asumiendo que fecha viene formateada o string
                createCell(row, 2, venta.getCliente(), dataStyle);
                createCell(row, 3, venta.getVendedor(), dataStyle);
                createCell(row, 4, venta.getEstado(), dataStyle);
                createCell(row, 5, venta.getTipoPago(), dataStyle);
                createCell(row, 6, venta.getTotal(), currencyStyle);

                grandTotal += venta.getTotal();

                // Detalles
                if (venta.getDetalles() != null && !venta.getDetalles().isEmpty()) {
                    Row detHeaderRow = sheet.createRow(rowNum++);
                    createCell(detHeaderRow, 1, "Producto", detailHeaderStyle);
                    createCell(detHeaderRow, 2, "Color", detailHeaderStyle);
                    createCell(detHeaderRow, 3, "Talla", detailHeaderStyle);
                    createCell(detHeaderRow, 4, "Cantidad", detailHeaderStyle);
                    createCell(detHeaderRow, 5, "Precio Unit.", detailHeaderStyle);
                    createCell(detHeaderRow, 6, "Subtotal", detailHeaderStyle);

                    for (Map<String, Object> det : venta.getDetalles()) {
                        Row detRow = sheet.createRow(rowNum++);

                        String producto = (String) det.get("producto");
                        String color = (String) det.get("color");
                        String talla = (String) det.get("talla");
                        int cantidad = (int) det.get("cantidad");
                        double precio = ((Number) det.get("precio_unitario")).doubleValue();
                        double subtotal = ((Number) det.get("subtotal")).doubleValue();

                        createCell(detRow, 1, producto, detailStyle);
                        createCell(detRow, 2, color != null ? color : "-", detailStyle);
                        createCell(detRow, 3, talla != null ? talla : "-", detailStyle);
                        createCell(detRow, 4, cantidad, detailStyle);
                        createCell(detRow, 5, precio, currencyStyle); // Reutilizando currencyStyle para precio
                        createCell(detRow, 6, subtotal, currencyStyle);
                    }
                    rowNum++; // Espacio después de detalles
                }
            }

            // Total General
            Row totalRow = sheet.createRow(rowNum++);
            createCell(totalRow, 5, "TOTAL GENERAL:", headerStyle);
            createCell(totalRow, 6, grandTotal, currencyStyle);

            // Autoajustar columnas (solo las principales)
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream out = new FileOutputStream(archivo)) {
                workbook.write(out);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void createCell(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else {
            cell.setCellValue(value != null ? value.toString() : "");
        }
        cell.setCellStyle(style);
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDateStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        // Si se necesita formato de fecha real, se puede agregar aquí
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("$#,##0.00"));
        return style;
    }

    private CellStyle createDetailHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 9);
        style.setFont(font);
        return style;
    }

    private CellStyle createDetailStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 9);
        style.setFont(font);
        return style;
    }
}
