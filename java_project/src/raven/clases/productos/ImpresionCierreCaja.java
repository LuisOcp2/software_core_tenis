package raven.clases.productos;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.output.PrinterOutputStream;
import javax.print.PrintService;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import raven.controlador.admin.ModelCajaMovimiento;
import raven.controlador.admin.ResumenCierreCaja;
import raven.controlador.principal.AppConfig;
import java.util.List;

/**
 * Clase para imprimir comprobantes de cierre de caja en impresora POS térmica.
 * Utiliza comandos ESC/POS con la librería escpos-coffee.
 * w
 * Sigue el mismo patrón de ImpresionPOST.java
 *
 * @author Sistema
 * @version 1.0
 */
public class ImpresionCierreCaja {

    private String nombreImpresora;

    // Estilos reutilizables
    private Style estiloTitulo;
    private Style estiloNegrita;
    private Style estiloNormal;
    private Style estiloCentrado;
    private Style estiloDerecha;
    private Style estiloPequeno;

    /**
     * Constructor con nombre de impresora
     */
    public ImpresionCierreCaja(String nombreImpresora) {
        this.nombreImpresora = nombreImpresora;
        inicializarEstilos();
    }

    /**
     * Constructor por defecto - usa la impresora predeterminada del sistema
     */
    public ImpresionCierreCaja() {
        this.nombreImpresora = null;
        inicializarEstilos();
    }

    /**
     * Inicializa los estilos de texto para la impresión
     */
    private void inicializarEstilos() {
        estiloTitulo = new Style()
                .setFontSize(Style.FontSize._2, Style.FontSize._2)
                .setJustification(EscPosConst.Justification.Center)
                .setBold(true);

        estiloNegrita = new Style()
                .setBold(true)
                .setJustification(EscPosConst.Justification.Center);

        estiloNormal = new Style()
                .setJustification(EscPosConst.Justification.Left_Default);

        estiloCentrado = new Style()
                .setJustification(EscPosConst.Justification.Center);

        estiloDerecha = new Style()
                .setJustification(EscPosConst.Justification.Right);

        estiloPequeno = new Style()
                .setFontSize(Style.FontSize._1, Style.FontSize._1)
                .setJustification(EscPosConst.Justification.Center);
    }

    /**
     * Imprime el comprobante de cierre de caja en la impresora térmica
     *
     * @param movimiento Datos del movimiento de caja cerrado
     * @param resumen    Resumen detallado del cierre (puede ser null)
     * @return true si se imprimió correctamente
     */
    public boolean imprimirCierreCaja(ModelCajaMovimiento movimiento, ResumenCierreCaja resumen) {
        EscPos escpos = null;

        try {
            PrintService printService = buscarImpresora();
            if (printService == null) {
                System.err.println("No se encontró impresora para cierre de caja. Generando TXT de respaldo.");
                return generarPreviewTXT(movimiento, resumen);
            }

            System.out.println("Imprimiendo cierre de caja en: " + printService.getName());

            // Crear salida a la impresora
            PrinterOutputStream printerOutputStream = new PrinterOutputStream(printService);
            escpos = new EscPos(printerOutputStream);

            // Inicializar impresora
            escpos.initializePrinter();

            // Construir el comprobante
            imprimirEncabezado(escpos, movimiento);
            imprimirLineaSeparadora(escpos);
            imprimirDatosMovimiento(escpos, movimiento);
            imprimirLineaSeparadora(escpos);
            imprimirResumenFinanciero(escpos, movimiento);

            // Desglose por método de pago (si hay resumen)
            if (resumen != null && resumen.getDetallesPorTipo() != null
                    && !resumen.getDetallesPorTipo().isEmpty()) {
                imprimirLineaSeparadora(escpos);
                imprimirDesglosePagos(escpos, resumen);
            }

            // Resumen de productos vendidos (NUEVO)
            if (resumen != null && resumen.getProductosVendidos() != null
                    && !resumen.getProductosVendidos().isEmpty()) {
                imprimirLineaSeparadora(escpos);
                imprimirResumenProductos(escpos, resumen);
            }

            // Egresos (si hay resumen)
            if (resumen != null) {
                imprimirLineaSeparadora(escpos);
                imprimirEgresos(escpos, resumen);
            }

            // Observaciones
            if (movimiento.getObservaciones() != null
                    && !movimiento.getObservaciones().trim().isEmpty()) {
                imprimirLineaSeparadora(escpos);
                imprimirObservaciones(escpos, movimiento);
            }

            imprimirLineaSeparadora(escpos);
            imprimirPieDePagina(escpos);

            // Alimentar papel y cortar
            escpos.feed(4);
            escpos.cut(EscPos.CutMode.PART);
            escpos.close();

            System.out.println("OK Comprobante de cierre #" + movimiento.getIdMovimiento() + " impreso exitosamente");
            return true;

        } catch (Exception e) {
            System.err.println("Error al imprimir cierre de caja: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (escpos != null) {
                try {
                    escpos.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    /**
     * Genera un preview del comprobante en formato TXT
     */
    public boolean generarPreviewTXT(ModelCajaMovimiento movimiento, ResumenCierreCaja resumen) {
        try {
            StringBuilder sb = new StringBuilder();
            int ancho = 40;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            // Encabezado
            sb.append(centrarTexto("CIERRE DE CAJA", ancho)).append("\n");
            sb.append(centrarTexto(AppConfig.name != null ? AppConfig.name : "SISTEMA POS", ancho)).append("\n");
            sb.append(repetirCaracter('=', ancho)).append("\n\n");

            // Datos del movimiento
            sb.append(String.format("Movimiento #%d\n", movimiento.getIdMovimiento()));
            sb.append(String.format("Caja: %s\n", movimiento.getNombreCaja()));
            sb.append(String.format("Usuario: %s\n", movimiento.getNombreUsuario()));
            sb.append(String.format("Apertura: %s\n",
                    movimiento.getFechaApertura() != null ? movimiento.getFechaApertura().format(formatter) : "N/A"));
            sb.append(String.format("Cierre:   %s\n",
                    movimiento.getFechaCierre() != null ? movimiento.getFechaCierre().format(formatter) : "N/A"));
            sb.append(String.format("Duracion: %d minutos\n\n", movimiento.getDuracionEnMinutos()));

            // Resumen financiero
            sb.append(repetirCaracter('-', ancho)).append("\n");
            sb.append(centrarTexto("RESUMEN FINANCIERO", ancho)).append("\n");
            sb.append(repetirCaracter('-', ancho)).append("\n");
            sb.append(String.format("Monto Inicial:  %s\n", formatearMoneda(movimiento.getMontoInicial())));
            sb.append(String.format("Total Ventas:   %s\n", formatearMoneda(movimiento.getTotalVentas())));
            sb.append(String.format("Monto Esperado: %s\n", formatearMoneda(movimiento.getMontoEsperado())));
            sb.append(String.format("Monto Contado:  %s\n", formatearMoneda(movimiento.getMontoFinal())));
            sb.append(repetirCaracter('=', ancho)).append("\n");

            BigDecimal diferencia = movimiento.calcularDiferencia();
            String estado = "CUADRADO";
            if (diferencia.compareTo(BigDecimal.ZERO) > 0) {
                estado = "SOBRANTE";
            } else if (diferencia.compareTo(BigDecimal.ZERO) < 0) {
                estado = "FALTANTE";
            }
            sb.append(String.format("Diferencia:     %s (%s)\n", formatearMoneda(diferencia), estado));
            sb.append(repetirCaracter('=', ancho)).append("\n\n");

            // Desglose por método de pago
            if (resumen != null && resumen.getDetallesPorTipo() != null
                    && !resumen.getDetallesPorTipo().isEmpty()) {
                sb.append(repetirCaracter('-', ancho)).append("\n");
                sb.append(centrarTexto("DESGLOSE POR METODO DE PAGO", ancho)).append("\n");
                sb.append(repetirCaracter('-', ancho)).append("\n");

                for (ResumenCierreCaja.DetallePago detalle : resumen.getDetallesPorTipo().values()) {
                    sb.append(String.format("%s: %d pagos -> %s\n",
                            detalle.getDescripcion(),
                            detalle.getCantidadPagos(),
                            formatearMoneda(detalle.getMontoTotal())));
                }
                sb.append("\n");
            }

            // Egresos
            if (resumen != null) {
                sb.append(repetirCaracter('-', ancho)).append("\n");
                sb.append(centrarTexto("EGRESOS DEL TURNO", ancho)).append("\n");
                sb.append(repetirCaracter('-', ancho)).append("\n");
                sb.append(String.format("Gastos Operativos:  %d -> %s\n",
                        resumen.getTotalGastos(),
                        formatearMoneda(resumen.getMontoTotalGastos())));
                sb.append(String.format("Compras Externas:   %d -> %s\n",
                        resumen.getTotalCompras(),
                        formatearMoneda(resumen.getMontoTotalCompras())));
                BigDecimal totalEgresos = resumen.getMontoTotalGastos().add(resumen.getMontoTotalCompras());
                sb.append(String.format("Total Egresos:      %s\n\n", formatearMoneda(totalEgresos)));
            }

            // Observaciones
            if (movimiento.getObservaciones() != null
                    && !movimiento.getObservaciones().trim().isEmpty()) {
                sb.append(repetirCaracter('-', ancho)).append("\n");
                sb.append("Observaciones:\n");
                sb.append(movimiento.getObservaciones()).append("\n\n");
            }

            // Pie de página
            sb.append(repetirCaracter('=', ancho)).append("\n");
            sb.append(centrarTexto("Documento generado automaticamente", ancho)).append("\n");
            sb.append(centrarTexto("Sistema POS", ancho)).append("\n");

            // Guardar archivo
            String nombreArchivo = String.format("cierre_caja_%d.txt", movimiento.getIdMovimiento());
            String rutaCompleta = System.getProperty("user.home") + "/Documents/" + nombreArchivo;

            try (java.io.FileWriter writer = new java.io.FileWriter(rutaCompleta)) {
                writer.write(sb.toString());
            }

            System.out.println("OK Preview de cierre generado: " + rutaCompleta);
            return true;

        } catch (Exception e) {
            System.err.println("Error al generar preview de cierre: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ==================== MÉTODOS DE IMPRESIÓN ====================

    private void imprimirEncabezado(EscPos escpos, ModelCajaMovimiento movimiento) throws Exception {
        String nombreNegocio = AppConfig.name != null ? AppConfig.name : "SISTEMA POS";
        escpos.writeLF(estiloTitulo, nombreNegocio);
        escpos.feed(1);
        escpos.writeLF(estiloNegrita, "CIERRE DE CAJA");
        escpos.writeLF(estiloCentrado, "Movimiento #" + movimiento.getIdMovimiento());
    }

    private void imprimirLineaSeparadora(EscPos escpos) throws Exception {
        escpos.writeLF(estiloCentrado, "----------------------------------------");
    }

    private void imprimirDatosMovimiento(EscPos escpos, ModelCajaMovimiento movimiento) throws Exception {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        escpos.writeLF(estiloNormal, "Caja: " + movimiento.getNombreCaja());
        escpos.writeLF(estiloNormal, "Usuario: " + movimiento.getNombreUsuario());
        escpos.feed(1);
        escpos.writeLF(estiloNormal, "Apertura: " +
                (movimiento.getFechaApertura() != null ? movimiento.getFechaApertura().format(formatter) : "N/A"));
        escpos.writeLF(estiloNormal, "Cierre:   " +
                (movimiento.getFechaCierre() != null ? movimiento.getFechaCierre().format(formatter) : "N/A"));
        escpos.writeLF(estiloNormal, "Duracion: " + movimiento.getDuracionEnMinutos() + " minutos");
    }

    private void imprimirResumenFinanciero(EscPos escpos, ModelCajaMovimiento movimiento) throws Exception {
        escpos.writeLF(estiloNegrita, "RESUMEN FINANCIERO");
        escpos.feed(1);

        escpos.writeLF(estiloNormal, String.format("Monto Inicial:  %s",
                formatearMoneda(movimiento.getMontoInicial())));
        escpos.writeLF(estiloNormal, String.format("Total Ventas:   %s",
                formatearMoneda(movimiento.getTotalVentas())));
        escpos.writeLF(estiloNormal, String.format("Monto Esperado: %s",
                formatearMoneda(movimiento.getMontoEsperado())));
        escpos.writeLF(estiloNormal, String.format("Monto Contado:  %s",
                formatearMoneda(movimiento.getMontoFinal())));

        escpos.feed(1);

        // Diferencia con estilo destacado
        BigDecimal diferencia = movimiento.calcularDiferencia();
        String estado;
        if (diferencia.compareTo(BigDecimal.ZERO) > 0) {
            estado = "SOBRANTE";
        } else if (diferencia.compareTo(BigDecimal.ZERO) < 0) {
            estado = "FALTANTE";
        } else {
            estado = "CUADRADO";
        }

        Style estiloResultado = new Style()
                .setJustification(EscPosConst.Justification.Center)
                .setBold(true)
                .setFontSize(Style.FontSize._1, Style.FontSize._2);

        escpos.writeLF(estiloResultado,
                String.format("DIFERENCIA: %s", formatearMoneda(diferencia)));
        escpos.writeLF(estiloNegrita, "** " + estado + " **");
    }

    private void imprimirDesglosePagos(EscPos escpos, ResumenCierreCaja resumen) throws Exception {
        escpos.writeLF(estiloNegrita, "DESGLOSE POR METODO DE PAGO");
        escpos.feed(1);

        for (ResumenCierreCaja.DetallePago detalle : resumen.getDetallesPorTipo().values()) {
            escpos.writeLF(estiloNormal, String.format("%s: %d -> %s",
                    detalle.getDescripcion(),
                    detalle.getCantidadPagos(),
                    formatearMoneda(detalle.getMontoTotal())));
        }
    }

    private void imprimirResumenProductos(EscPos escpos, ResumenCierreCaja resumen) throws Exception {
        escpos.writeLF(estiloNegrita, "RESUMEN DE PRODUCTOS");
        escpos.feed(1);

        for (ResumenCierreCaja.DetalleProducto producto : resumen.getProductosVendidos()) {
            escpos.writeLF(estiloNormal, String.format("%d %s %s",
                    producto.getCantidad(),
                    producto.getUnidad(),
                    producto.getNombreProducto()));
            escpos.writeLF(estiloDerecha, "-> " + formatearMoneda(producto.getMontoTotal()));
        }
    }

    private void imprimirEgresos(EscPos escpos, ResumenCierreCaja resumen) throws Exception {
        escpos.writeLF(estiloNegrita, "EGRESOS DEL TURNO");
        escpos.feed(1);

        escpos.writeLF(estiloNormal, String.format("Gastos Operativos:  %d -> %s",
                resumen.getTotalGastos(),
                formatearMoneda(resumen.getMontoTotalGastos())));
        escpos.writeLF(estiloNormal, String.format("Compras Externas:   %d -> %s",
                resumen.getTotalCompras(),
                formatearMoneda(resumen.getMontoTotalCompras())));

        BigDecimal totalEgresos = resumen.getMontoTotalGastos().add(resumen.getMontoTotalCompras());
        escpos.writeLF(estiloNormal, String.format("Total Egresos:      %s",
                formatearMoneda(totalEgresos)));
    }

    private void imprimirObservaciones(EscPos escpos, ModelCajaMovimiento movimiento) throws Exception {
        escpos.writeLF(estiloNegrita, "OBSERVACIONES");
        escpos.feed(1);
        escpos.writeLF(estiloPequeno, movimiento.getObservaciones());
    }

    private void imprimirPieDePagina(EscPos escpos) throws Exception {
        escpos.feed(1);
        escpos.writeLF(estiloPequeno, "Documento generado automaticamente");
        escpos.writeLF(estiloPequeno, "Sistema POS");
    }

    // ==================== UTILIDADES ====================

    private PrintService buscarImpresora() {
        PrintService[] printServices = javax.print.PrintServiceLookup.lookupPrintServices(null, null);

        if (nombreImpresora == null || nombreImpresora.trim().isEmpty()) {
            if (printServices.length > 0) {
                return printServices[0];
            }
            return null;
        }

        for (PrintService printService : printServices) {
            if (printService.getName().toLowerCase().contains(nombreImpresora.toLowerCase())) {
                return printService;
            }
        }

        return null;
    }

    private String formatearMoneda(BigDecimal monto) {
        if (monto == null) {
            return "$0.00";
        }
        return String.format("$%,.2f", monto.doubleValue());
    }

    private String formatearMoneda(double monto) {
        return String.format("$%,.2f", monto);
    }

    private String centrarTexto(String texto, int ancho) {
        if (texto.length() >= ancho) {
            return texto.substring(0, ancho);
        }
        int espacios = (ancho - texto.length()) / 2;
        return repetirCaracter(' ', espacios) + texto;
    }

    private String repetirCaracter(char c, int veces) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < veces; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    // ==================== GETTERS Y SETTERS ====================

    public String getNombreImpresora() {
        return nombreImpresora;
    }

    public void setNombreImpresora(String nombreImpresora) {
        this.nombreImpresora = nombreImpresora;
    }
}
