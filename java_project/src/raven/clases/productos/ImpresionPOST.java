package raven.clases.productos;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.output.PrinterOutputStream;
import javax.print.PrintService;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import raven.controlador.principal.AppConfig;
import raven.controlador.principal.conexion;

/**
 * Clase para imprimir recibos directamente en impresora POS térmica de 80mm
 * usando comandos ESC/POS con la librería escpos-coffee
 *
 * Dependencia Maven:
 * <dependency>
 * <groupId>com.github.anastaciocintra</groupId>
 * <artifactId>escpos-coffee</artifactId>
 * <version>4.1.0</version>
 * </dependency>
 *
 * @author Generado para raven.clases.reportes
 */
public class ImpresionPOST {

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
     *
     * @param nombreImpresora Nombre o parte del nombre de la impresora
     * instalada
     */
    public ImpresionPOST(String nombreImpresora) {
        this.nombreImpresora = nombreImpresora;
        inicializarEstilos();
    }

    /**
     * Constructor por defecto - usa la impresora predeterminada del sistema
     */
    public ImpresionPOST() {
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
     * Imprime el recibo directamente en la impresora térmica
     *
     * @param idVenta ID de la venta a imprimir
     * @return true si se imprimió correctamente
     */
    public boolean imprimirRecibo(int idVenta) {
        return imprimirRecibo(idVenta, null);
    }

    /**
     * Imprime el recibo directamente en la impresora térmica con fecha personalizada
     *
     * @param idVenta ID de la venta a imprimir
     * @param fechaPersonalizada Fecha y hora exacta de la venta (opcional)
     * @return true si se imprimió correctamente
     */
    public boolean imprimirRecibo(int idVenta, java.time.LocalDateTime fechaPersonalizada) {
        Connection con = null;
        EscPos escpos = null;

        try {
            con = conexion.getInstance().createConnection();
            DatosVenta datosVenta = obtenerDatosVenta(con, idVenta);
            if (datosVenta == null) {
                System.err.println("No se encontraron datos para la venta #" + idVenta);
                return false;
            }

            // Sobrescribir fecha si se proporciona
            if (fechaPersonalizada != null) {
                datosVenta.fechaVenta = java.sql.Timestamp.valueOf(fechaPersonalizada);
            }

            PrintService printService = buscarImpresora();
            if (printService == null) {
                System.err.println("No se encontró la impresora: "
                        + (nombreImpresora != null ? nombreImpresora : "predeterminada"));
                listarImpresorasDisponibles();

                // Fallback: generar TXT en vez de fallar silencioso
                String nombreArchivo = "recibo_" + idVenta + ".txt";
                System.out.println("Generando recibo TXT de respaldo: " + nombreArchivo);
                return generarPreviewTXT(idVenta, nombreArchivo, fechaPersonalizada);
            }

            System.out.println("Imprimiendo en: " + printService.getName());

            // Crear salida a la impresora
            PrinterOutputStream printerOutputStream = new PrinterOutputStream(printService);
            escpos = new EscPos(printerOutputStream);

            // Inicializar impresora
            escpos.initializePrinter();

            // Construir el recibo
            imprimirEncabezado(escpos, datosVenta, con);
            imprimirLineaSeparadora(escpos);
            imprimirDatosCliente(escpos, datosVenta);
            imprimirLineaSeparadora(escpos);
            imprimirDetallesProductos(escpos, con, idVenta);
            imprimirLineaSeparadora(escpos);
            imprimirTotales(escpos, datosVenta);
            imprimirLineaSeparadora(escpos);
            imprimirPieDePagina(escpos, datosVenta);

            // Alimentar papel y cortar
            escpos.feed(4);
            escpos.cut(EscPos.CutMode.PART); // Corte parcial (deja pestaña)
            // Usar EscPos.CutMode.FULL para corte completo

            // Cerrar
            escpos.close();

            System.out.println("OK Recibo #" + idVenta + " impreso exitosamente");
            return true;

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al imprimir recibo para venta " + idVenta);
            e.printStackTrace();   // ya lo tienes
            return false;
        } finally {
            // Cerrar escpos si quedó abierto
            if (escpos != null) {
                try {
                    escpos.close();
                } catch (Exception ignore) {
                }
            }
            // Cerrar conexión
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Genera un preview del recibo en formato TXT (para pruebas sin gastar
     * papel)
     *
     * @param idVenta ID de la venta a previsualizar
     * @param nombreArchivo Nombre del archivo TXT (ej: "preview_recibo.txt")
     * @return true si se generó correctamente
     */
    public boolean generarPreviewTXT(int idVenta, String rutaCompleta) {
        return generarPreviewTXT(idVenta, rutaCompleta, null);
    }

    public boolean generarPreviewTXT(int idVenta, String rutaCompleta, java.time.LocalDateTime fechaPersonalizada) {
    Connection con = null;

    try {
        // Obtener datos de la venta
        con = conexion.getInstance().createConnection();
        DatosVenta datosVenta = obtenerDatosVenta(con, idVenta);
        
        // Sobrescribir fecha si se proporciona
        if (fechaPersonalizada != null && datosVenta != null) {
            datosVenta.fechaVenta = java.sql.Timestamp.valueOf(fechaPersonalizada);
        }

        if (datosVenta == null) {
            System.err.println("No se encontraron datos para la venta #" + idVenta);
            return false;
        }

        StringBuilder recibo = new StringBuilder();
        int anchoTirilla = 40;

        // ENCABEZADO
        recibo.append(centrarTexto("COMPROBANTE DE VENTA", anchoTirilla)).append("\n");

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        recibo.append(centrarTexto("Fecha: " + sdf.format(datosVenta.fechaVenta), anchoTirilla)).append("\n");
        recibo.append(centrarTexto("No. " + String.format("%06d", datosVenta.idVenta), anchoTirilla)).append("\n");

        if (datosVenta.nombreUsuario != null && !datosVenta.nombreUsuario.trim().isEmpty()) {
            recibo.append(centrarTexto("Vendedor: " + datosVenta.nombreUsuario, anchoTirilla)).append("\n");
        }
        recibo.append("\n");

        // DATOS BODEGA
        DatosBodega b = null;
        if (datosVenta.idBodegaUsuario != null && datosVenta.idBodegaUsuario > 0) {
            try {
                b = obtenerDatosBodega(con, datosVenta.idBodegaUsuario);
            } catch (Exception ignore) {}
        }

        String nombreBodega = (b != null && b.nombre != null && !b.nombre.trim().isEmpty())
                ? b.nombre : AppConfig.name;

        recibo.append(centrarTexto(nombreBodega, anchoTirilla)).append("\n");

        if (b != null) {
            if (b.direccion != null && !b.direccion.trim().isEmpty()) {
                recibo.append(centrarTexto(b.direccion, anchoTirilla)).append("\n");
            }
            if (b.telefono != null && !b.telefono.trim().isEmpty()) {
                recibo.append(centrarTexto("Tel: " + b.telefono, anchoTirilla)).append("\n");
            }
        }

        // SEPARADOR
        recibo.append(repetirCaracter('-', anchoTirilla)).append("\n");

        // DATOS CLIENTE
        if (datosVenta.nombreCliente != null && !datosVenta.nombreCliente.trim().isEmpty()) {
            recibo.append("Cliente: ").append(datosVenta.nombreCliente).append("\n");
            if (datosVenta.dniCliente != null && !datosVenta.dniCliente.trim().isEmpty()) {
                recibo.append("CC/NIT:  ").append(datosVenta.dniCliente).append("\n");
            }
        } else {
            recibo.append("Cliente: PUBLICO GENERAL\n");
        }

        // SEPARADOR
        recibo.append(repetirCaracter('-', anchoTirilla)).append("\n");

        // PRODUCTOS
        String sql = "SELECT vd.cantidad, vd.precio_unitario, vd.subtotal, vd.tipo_venta, "
                + "p.nombre, pv.ean, c.nombre as color, "
                + "CONCAT(t.numero, ' ', t.sistema) as talla "
                + "FROM venta_detalles vd "
                + "INNER JOIN productos p ON vd.id_producto = p.id_producto "
                + "LEFT JOIN producto_variantes pv ON vd.id_variante = pv.id_variante "
                + "LEFT JOIN colores c ON pv.id_color = c.id_color "
                + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                + "WHERE vd.id_venta = ? AND vd.activo = 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idVenta);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nombre = rs.getString("nombre");
                    String color = rs.getString("color");
                    String talla = rs.getString("talla");
                    int cantidad = rs.getInt("cantidad");
                    BigDecimal precio = rs.getBigDecimal("precio_unitario");
                    BigDecimal subtotal = rs.getBigDecimal("subtotal");
                    String tipoVenta = rs.getString("tipo_venta");

                    String nombreMostrar = nombre.length() > 38 ? nombre.substring(0, 35) + "..." : nombre;
                    recibo.append(nombreMostrar).append("\n");

                    if (color != null && !color.trim().isEmpty() && talla != null && !talla.trim().isEmpty()) {
                        recibo.append("  (").append(color).append(" - ").append(talla.trim()).append(")\n");
                    } else if (color != null && !color.trim().isEmpty()) {
                        recibo.append("  (").append(color).append(")\n");
                    } else if (talla != null && !talla.trim().isEmpty()) {
                        recibo.append("  (Talla: ").append(talla.trim()).append(")\n");
                    }

                    String unidad = tipoVenta != null && tipoVenta.toLowerCase().contains("caja") ? "caja(s)" : "par(es)";

                    String detalle = String.format("  %d %s x $%,.0f = $%,.0f",
                            cantidad, unidad, precio, subtotal);
                    recibo.append(detalle).append("\n\n");
                }
            }
        }

        // SEPARADOR
        recibo.append(repetirCaracter('-', anchoTirilla)).append("\n");

        // TOTALES
        recibo.append(alinearDerecha("Subtotal:        $" + String.format("%,10.0f", datosVenta.subtotal), anchoTirilla)).append("\n");

        if (datosVenta.descuento > 0) {
            recibo.append(alinearDerecha("Descuento:       $" + String.format("%,10.0f", datosVenta.descuento), anchoTirilla)).append("\n");
        }

        recibo.append(repetirCaracter('=', anchoTirilla)).append("\n");
        recibo.append(alinearDerecha("TOTAL: $" + String.format("%,.0f", datosVenta.total), anchoTirilla)).append("\n");
        recibo.append(repetirCaracter('=', anchoTirilla)).append("\n");

        // SEPARADOR
        recibo.append(repetirCaracter('-', anchoTirilla)).append("\n");

        // PIE DE PÁGINA
        recibo.append("\n");
        String metodoPago = datosVenta.tipoPago != null ? datosVenta.tipoPago.toUpperCase() : "EFECTIVO";
        recibo.append(centrarTexto("Metodo de pago: " + metodoPago, anchoTirilla)).append("\n");

        String estado = datosVenta.estado != null ? datosVenta.estado.toUpperCase() : "COMPLETADA";
        recibo.append(centrarTexto("Estado: " + estado, anchoTirilla)).append("\n\n");

        recibo.append(centrarTexto("Gracias por su compra!", anchoTirilla)).append("\n\n");
        recibo.append(centrarTexto("Garantía 30 días (Costura/Fabricación)", anchoTirilla)).append("\n");
        recibo.append(centrarTexto("Requiere recibo original", anchoTirilla)).append("\n");
        recibo.append(centrarTexto("No cubre mal uso o desgaste normal", anchoTirilla)).append("\n");
        // Guardar en archivo en la ruta indicada
        try (java.io.FileWriter writer = new java.io.FileWriter(rutaCompleta)) {
            writer.write(recibo.toString());
        }

        System.out.println("OK Preview generado: " + rutaCompleta);
        return true;

    } catch (Exception e) {
        e.printStackTrace();
        System.err.println("Error al generar preview: " + e.getMessage());
        return false;
    } finally {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}


    /**
     * Centra un texto en el ancho especificado
     */
    private String centrarTexto(String texto, int ancho) {
        if (texto.length() >= ancho) {
            return texto.substring(0, ancho);
        }
        int espacios = (ancho - texto.length()) / 2;
        return repetirCaracter(' ', espacios) + texto;
    }

    /**
     * Alinea texto a la derecha
     */
    private String alinearDerecha(String texto, int ancho) {
        if (texto.length() >= ancho) {
            return texto.substring(0, ancho);
        }
        int espacios = ancho - texto.length();
        return repetirCaracter(' ', espacios) + texto;
    }

    /**
     * Repite un caracter n veces
     */
    private String repetirCaracter(char c, int veces) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < veces; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Imprime el encabezado del recibo
     */
    private void imprimirEncabezado(EscPos escpos, DatosVenta datos, Connection con) throws Exception {
        // Obtener datos de bodega
        DatosBodega b = null;
        if (datos.idBodegaUsuario != null && datos.idBodegaUsuario > 0) {
            try {
                b = obtenerDatosBodega(con, datos.idBodegaUsuario);
            } catch (Exception ignore) {
            }
        }

        // Nombre del negocio
        String nombreBodega = (b != null && b.nombre != null && !b.nombre.trim().isEmpty())
                ? b.nombre : AppConfig.name;
        escpos.writeLF(estiloTitulo, nombreBodega);

        // Dirección y teléfono
        if (b != null) {
            if (b.direccion != null && !b.direccion.trim().isEmpty()) {
                escpos.writeLF(estiloPequeno, b.direccion);
            }
            if (b.telefono != null && !b.telefono.trim().isEmpty()) {
                escpos.writeLF(estiloPequeno, "Tel: " + b.telefono);
            }
        }

        escpos.feed(1);

        // Título factura
        escpos.writeLF(estiloNegrita, "COMPROBANTE DE VENTA");

        // Fecha y hora
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        escpos.writeLF(estiloCentrado, "Fecha: " + sdf.format(datos.fechaVenta));

        // Número de factura
        escpos.writeLF(estiloCentrado, "No. " + String.format("%06d", datos.idVenta));

        // Vendedor
        if (datos.nombreUsuario != null && !datos.nombreUsuario.trim().isEmpty()) {
            escpos.writeLF(estiloCentrado, "Vendedor: " + datos.nombreUsuario);
        }
    }

    /**
     * Imprime línea separadora
     */
    private void imprimirLineaSeparadora(EscPos escpos) throws Exception {
        escpos.writeLF(estiloCentrado, "----------------------------------------");
    }

    /**
     * Imprime línea separadora doble
     */
    private void imprimirLineaSeparadoraDoble(EscPos escpos) throws Exception {
        escpos.writeLF(estiloCentrado, "========================================");
    }

    /**
     * Imprime datos del cliente
     */
    private void imprimirDatosCliente(EscPos escpos, DatosVenta datos) throws Exception {
        if (datos.nombreCliente != null && !datos.nombreCliente.trim().isEmpty()) {
            escpos.writeLF(estiloNormal, "Cliente: " + datos.nombreCliente);
            if (datos.dniCliente != null && !datos.dniCliente.trim().isEmpty()) {
                escpos.writeLF(estiloNormal, "CC/NIT:  " + datos.dniCliente);
            }
        } else {
            escpos.writeLF(estiloNormal, "Cliente: PUBLICO GENERAL");
        }
    }

    /**
     * Imprime los detalles de productos
     */
    private void imprimirDetallesProductos(EscPos escpos, Connection con, int idVenta) throws Exception {
        String sql = "SELECT vd.cantidad, vd.precio_unitario, vd.subtotal, vd.tipo_venta, "
                + "p.nombre, pv.ean, c.nombre as color, "
                + "CONCAT(t.numero, ' ', t.sistema) as talla "
                + "FROM venta_detalles vd "
                + "INNER JOIN productos p ON vd.id_producto = p.id_producto "
                + "LEFT JOIN producto_variantes pv ON vd.id_variante = pv.id_variante "
                + "LEFT JOIN colores c ON pv.id_color = c.id_color "
                + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                + "WHERE vd.id_venta = ? AND vd.activo = 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idVenta);

            try (ResultSet rs = ps.executeQuery()) {
                int itemNum = 0;
                while (rs.next()) {
                    itemNum++;
                    String nombre = rs.getString("nombre");
                    String color = rs.getString("color");
                    String talla = rs.getString("talla");
                    int cantidad = rs.getInt("cantidad");
                    BigDecimal precio = rs.getBigDecimal("precio_unitario");
                    BigDecimal subtotal = rs.getBigDecimal("subtotal");
                    String tipoVenta = rs.getString("tipo_venta");

                    // Nombre del producto (truncar si es muy largo)
                    String nombreMostrar = nombre.length() > 38 ? nombre.substring(0, 35) + "..." : nombre;
                    escpos.writeLF(estiloNormal, nombreMostrar);

                    // Variantes (color y talla)
                    if (color != null && !color.trim().isEmpty() && talla != null && !talla.trim().isEmpty()) {
                        escpos.writeLF(estiloNormal, "  (" + color + " - " + talla.trim() + ")");
                    } else if (color != null && !color.trim().isEmpty()) {
                        escpos.writeLF(estiloNormal, "  (" + color + ")");
                    } else if (talla != null && !talla.trim().isEmpty()) {
                        escpos.writeLF(estiloNormal, "  (Talla: " + talla.trim() + ")");
                    }

                    // Determinar unidad
                    String unidad = tipoVenta != null && tipoVenta.toLowerCase().contains("caja") ? "caja(s)" : "par(es)";

                    // Cantidad, precio y subtotal en una línea
                    String detalle = String.format("  %d %s x $%,.0f = $%,.0f",
                            cantidad, unidad, precio, subtotal);
                    escpos.writeLF(estiloNormal, detalle);

                    escpos.feed(1); // Espacio entre productos
                }

                if (itemNum == 0) {
                    escpos.writeLF(estiloNormal, "(Sin productos)");
                }
            }
        }
    }

    /**
     * Imprime los totales
     */
    private void imprimirTotales(EscPos escpos, DatosVenta datos) throws Exception {
        // Subtotal
        String subtotalStr = String.format("Subtotal:        $%,10.0f", datos.subtotal);
        escpos.writeLF(estiloDerecha, subtotalStr);

        // Descuento si aplica
        if (datos.descuento > 0) {
            String descuentoStr = String.format("Descuento:       $%,10.0f", datos.descuento);
            escpos.writeLF(estiloDerecha, descuentoStr);
        }

        // Total con estilo destacado
        imprimirLineaSeparadoraDoble(escpos);
        Style estiloTotal = new Style()
                .setJustification(EscPosConst.Justification.Right)
                .setBold(true)
                .setFontSize(Style.FontSize._1, Style.FontSize._2);
        String totalStr = String.format("TOTAL: $%,.0f", datos.total);
        escpos.writeLF(estiloTotal, totalStr);
        imprimirLineaSeparadoraDoble(escpos);
    }

    /**
     * Imprime pie de página
     */
    private void imprimirPieDePagina(EscPos escpos, DatosVenta datos) throws Exception {
        escpos.feed(1);

        // Método de pago
        String metodoPago = datos.tipoPago != null ? datos.tipoPago.toUpperCase() : "EFECTIVO";
        escpos.writeLF(estiloCentrado, "Metodo de pago: " + metodoPago);

        // Estado
        String estado = datos.estado != null ? datos.estado.toUpperCase() : "COMPLETADA";
        escpos.writeLF(estiloCentrado, "Estado: " + estado);

        escpos.feed(1);

        // Mensaje de agradecimiento
        escpos.writeLF(estiloNegrita, "Gracias por su compra!");

        escpos.feed(1);

        // Garantía (dividida en líneas para que quepa)
        escpos.writeLF(estiloPequeno, "Garantía 30 días (Costura/Fabricación)");
        escpos.writeLF(estiloPequeno, "Requiere recibo original");
        escpos.writeLF(estiloPequeno, "No cubre mal uso o desgaste normal");
    }

    /**
     * Busca la impresora por nombre
     *
     * @return PrintService encontrado o null
     */
    private PrintService buscarImpresora() {
        PrintService[] printServices = javax.print.PrintServiceLookup.lookupPrintServices(null, null);

        // Si no se especificó nombre, usar la primera disponible
        if (nombreImpresora == null || nombreImpresora.trim().isEmpty()) {
            if (printServices.length > 0) {
                return printServices[0];
            }
            return null;
        }

        // Buscar por nombre (coincidencia parcial, sin distinguir mayúsculas)
        for (PrintService printService : printServices) {
            if (printService.getName().toLowerCase().contains(nombreImpresora.toLowerCase())) {
                return printService;
            }
        }

        return null;
    }

    /**
     * Lista todas las impresoras disponibles en el sistema
     */
    public void listarImpresorasDisponibles() {
        PrintService[] printServices = javax.print.PrintServiceLookup.lookupPrintServices(null, null);
        System.out.println("\n=== Impresoras disponibles ===");
        if (printServices.length == 0) {
            System.out.println("  (No se encontraron impresoras)");
        } else {
            for (int i = 0; i < printServices.length; i++) {
                System.out.println("  [" + i + "] " + printServices[i].getName());
            }
        }
        System.out.println("==============================\n");
    }

    /**
     * Obtiene lista de nombres de impresoras disponibles
     *
     * @return Lista de nombres de impresoras
     */
    public static List<String> obtenerImpresorasDisponibles() {
        PrintService[] printServices = javax.print.PrintServiceLookup.lookupPrintServices(null, null);
        List<String> nombres = new ArrayList<>();
        for (PrintService ps : printServices) {
            nombres.add(ps.getName());
        }
        return nombres;
    }

    /**
     * Prueba de impresión - imprime un ticket de prueba
     *
     * @return true si se imprimió correctamente
     */
    public boolean imprimirPrueba() {
        EscPos escpos = null;
        try {
            PrintService printService = buscarImpresora();
            if (printService == null) {
                System.err.println("No se encontró la impresora");
                listarImpresorasDisponibles();
                return false;
            }

            PrinterOutputStream printerOutputStream = new PrinterOutputStream(printService);
            escpos = new EscPos(printerOutputStream);

            escpos.initializePrinter();
            escpos.writeLF(estiloTitulo, "PRUEBA DE IMPRESION");
            escpos.writeLF(estiloCentrado, "----------------------------------------");
            escpos.writeLF(estiloCentrado, "Impresora: " + printService.getName());
            escpos.writeLF(estiloCentrado, "Fecha: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date()));
            escpos.writeLF(estiloCentrado, "----------------------------------------");
            escpos.writeLF(estiloNegrita, "Si puede leer esto,");
            escpos.writeLF(estiloNegrita, "la impresora funciona correctamente!");
            escpos.feed(4);
            escpos.cut(EscPos.CutMode.PART);
            escpos.close();

            System.out.println("Prueba de impresión enviada correctamente");
            return true;

        } catch (Exception e) {
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

    // ==================== MÉTODOS DE ACCESO A DATOS ====================
    /**
     * Obtiene los datos de la venta desde la base de datos
     */
    private DatosVenta obtenerDatosVenta(Connection con, int idVenta) throws SQLException {
        String sql = "SELECT v.*, "
                + "c.nombre as cliente_nombre, c.dni, "
                + "u.nombre as usuario_nombre, u.rol as usuario_cargo, u.id_usuario as usuario_id, u.id_bodega as usuario_bodega "
                + "FROM ventas v "
                + "LEFT JOIN clientes c ON v.id_cliente = c.id_cliente "
                + "LEFT JOIN usuarios u ON v.id_usuario = u.id_usuario "
                + "WHERE v.id_venta = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idVenta);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DatosVenta datos = new DatosVenta();
                    datos.idVenta = idVenta;
                    datos.fechaVenta = rs.getTimestamp("fecha_venta");
                    datos.nombreCliente = rs.getString("cliente_nombre");
                    datos.dniCliente = rs.getString("dni");
                    datos.subtotal = rs.getDouble("subtotal");
                    datos.descuento = rs.getDouble("descuento");
                    datos.total = rs.getDouble("total");
                    datos.tipoPago = rs.getString("tipo_pago");
                    datos.estado = rs.getString("estado");
                    datos.nombreUsuario = rs.getString("usuario_nombre");
                    datos.cargoUsuario = rs.getString("usuario_cargo");
                    try {
                        datos.idUsuario = rs.getInt("usuario_id");
                    } catch (SQLException ignore) {
                    }
                    try {
                        datos.idBodegaUsuario = (Integer) rs.getObject("usuario_bodega");
                    } catch (SQLException ignore) {
                    }
                    return datos;
                }
            }
        }
        return null;
    }

    /**
     * Obtiene los datos de la bodega
     */
    private DatosBodega obtenerDatosBodega(Connection con, int idBodega) throws SQLException {
        String sql = "SELECT nombre, direccion, telefono FROM bodegas WHERE id_bodega = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idBodega);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DatosBodega b = new DatosBodega();
                    b.nombre = rs.getString("nombre");
                    try {
                        b.direccion = rs.getString("direccion");
                    } catch (SQLException ignore) {
                    }
                    try {
                        b.telefono = rs.getString("telefono");
                    } catch (SQLException ignore) {
                    }
                    return b;
                }
            }
        }
        return null;
    }

    // ==================== CLASES INTERNAS ====================
    /**
     * Clase interna para almacenar datos de la venta
     */
    private static class DatosVenta {

        int idVenta;
        Timestamp fechaVenta;
        String nombreCliente;
        String dniCliente;
        double subtotal;
        double descuento;
        double total;
        String tipoPago;
        String estado;
        String nombreUsuario;
        String cargoUsuario;
        int idUsuario;
        Integer idBodegaUsuario;
    }

    /**
     * Clase interna para almacenar datos de la bodega
     */
    private static class DatosBodega {

        String nombre;
        String direccion;
        String telefono;
    }

    // ==================== GETTERS Y SETTERS ====================
    public String getNombreImpresora() {
        return nombreImpresora;
    }

    public void setNombreImpresora(String nombreImpresora) {
        this.nombreImpresora = nombreImpresora;
    }
}

