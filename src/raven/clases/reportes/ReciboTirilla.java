package raven.clases.reportes;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.FileOutputStream;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.math.BigDecimal;
import raven.controlador.principal.AppConfig;
import raven.controlador.principal.conexion;

/**
 * Clase para generar recibos de venta en formato PDF optimizado para impresoras
 * térmicas de tirilla Tamaño estándar: 80mm de ancho (226 puntos)
 */
public class ReciboTirilla {

    // Ancho estándar para impresora térmica de 80mm
    private static final float ANCHO_TIRILLA = 226f; // 80mm en puntos

    // Fuentes personalizadas
    private Font fuenteTitulo;
    private Font fuenteNormal;
    private Font fuenteNegrita;
    private Font fuentePequeña;

    public ReciboTirilla() {
        inicializarFuentes();
    }

    /**
     * Inicializa las fuentes para el recibo
     */
    private void inicializarFuentes() {
        try {
            fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.BOLD);
            fuenteNegrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD);
            fuenteNormal = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL);
            fuentePequeña = FontFactory.getFont(FontFactory.HELVETICA, 7, Font.NORMAL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Genera el recibo de venta en formato PDF
     *
     * @param idVenta ID de la venta a imprimir
     * @return true si se generó correctamente
     */
    public boolean generarRecibo(int idVenta) {
        return generarRecibo(idVenta, null);
    }

    /**
     * Genera el recibo de venta en formato PDF con fecha personalizada
     *
     * @param idVenta ID de la venta a imprimir
     * @param fechaPersonalizada Fecha y hora exacta de la venta (opcional)
     * @return true si se generó correctamente
     */
    public boolean generarRecibo(int idVenta, java.time.LocalDateTime fechaPersonalizada) {
        Connection con = null;
        Document documento = null;

        try {
            // Crear documento con tamaño personalizado para tirilla
            documento = new Document(new Rectangle(ANCHO_TIRILLA, 842f)); // Alto variable
            String rutaArchivo = "recibo_venta_" + idVenta + ".pdf";
            PdfWriter writer = PdfWriter.getInstance(documento, new FileOutputStream(rutaArchivo));

            documento.open();

            // Obtener datos de la venta
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

            // Construir el recibo
            agregarEncabezado(documento, datosVenta);
            agregarLineaSeparadora(documento);
            agregarDatosCliente(documento, datosVenta);
            agregarLineaSeparadora(documento);
            agregarDetallesProductos(documento, con, idVenta);
            agregarLineaSeparadora(documento);
            agregarTotales(documento, datosVenta);
            agregarLineaSeparadora(documento);
            agregarPieDePagina(documento, datosVenta);

            documento.close();

            // Abrir el PDF automáticamente
            abrirPDF(rutaArchivo);

            System.out.println("Recibo generado exitosamente: " + rutaArchivo);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error al generar recibo: " + e.getMessage());
            return false;
        } finally {
            if (documento != null && documento.isOpen()) {
                documento.close();
            }
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
     * Agrega el encabezado del recibo
     */
    private void agregarEncabezado(Document doc, DatosVenta datos) throws DocumentException {
        DatosBodega b = null;
        if (datos.idBodegaUsuario != null && datos.idBodegaUsuario > 0) {
            try (Connection con = conexion.getInstance().createConnection()) {
                b = obtenerDatosBodega(con, datos.idBodegaUsuario);
            } catch (Exception ignore) {}
        }
        String nombreBodega = (b != null && b.nombre != null && !b.nombre.trim().isEmpty()) ? b.nombre : AppConfig.name;
        Paragraph titulo = new Paragraph(nombreBodega, fuenteTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        doc.add(titulo);

        String dirTexto = null;
        if (b != null) {
            String linea1 = (b.direccion != null && !b.direccion.trim().isEmpty()) ? b.direccion : "";
            String tel = (b.telefono != null && !b.telefono.trim().isEmpty()) ? ("\nTel: " + b.telefono) : "";
            dirTexto = (linea1 + tel).trim();
        }
        Paragraph direccion = new Paragraph(dirTexto != null && !dirTexto.isEmpty() ? dirTexto : " ", fuentePequeña);
        direccion.setAlignment(Element.ALIGN_CENTER);
        doc.add(direccion);

        doc.add(new Paragraph(" ")); // Espacio

        Paragraph factura = new Paragraph("COMPROBANTE DE VENTA", fuenteNegrita);
        factura.setAlignment(Element.ALIGN_CENTER);
        doc.add(factura);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        Paragraph fecha = new Paragraph("Fecha: " + sdf.format(datos.fechaVenta), fuenteNormal);
        fecha.setAlignment(Element.ALIGN_CENTER);
        doc.add(fecha);

        Paragraph numFactura = new Paragraph("No. " + String.format("%06d", datos.idVenta), fuenteNormal);
        numFactura.setAlignment(Element.ALIGN_CENTER);
        doc.add(numFactura);

        // NUEVO: Vendedor en el encabezado
        if (datos.nombreUsuario != null && !datos.nombreUsuario.trim().isEmpty()) {
            Paragraph vendedor = new Paragraph("Vendedor: " + datos.nombreUsuario, fuentePequeña);
            vendedor.setAlignment(Element.ALIGN_CENTER);
            doc.add(vendedor);
        }
    }

    /**
     * Agrega línea separadora
     */
    private void agregarLineaSeparadora(Document doc) throws DocumentException {
        Paragraph linea = new Paragraph("----------------------------------------", fuenteNormal);
        linea.setAlignment(Element.ALIGN_CENTER);
        doc.add(linea);
    }

    /**
     * Agrega datos del cliente
     */
    private void agregarDatosCliente(Document doc, DatosVenta datos) throws DocumentException {
        if (datos.nombreCliente != null && !datos.nombreCliente.trim().isEmpty()) {
            Paragraph cliente = new Paragraph("Cliente: " + datos.nombreCliente, fuenteNormal);
            doc.add(cliente);

            if (datos.dniCliente != null && !datos.dniCliente.trim().isEmpty()) {
                Paragraph dni = new Paragraph("CC: " + datos.dniCliente, fuenteNormal);
                doc.add(dni);
            }
        } else {
            Paragraph cliente = new Paragraph("Cliente: PUBLICO GENERAL", fuenteNormal);
            doc.add(cliente);
        }
    }

    /**
     * Agrega los detalles de productos
     */
    private void agregarDetallesProductos(Document doc, Connection con, int idVenta) throws DocumentException, SQLException {
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

                    // Nombre del producto con variantes
                    String nombreCompleto = nombre;
                    if (color != null && !color.trim().isEmpty()
                            && talla != null && !talla.trim().isEmpty()) {
                        nombreCompleto += "\n  (" + color + " - " + talla.trim() + ")";
                    }

                    Paragraph producto = new Paragraph(nombreCompleto, fuentePequeña);
                    doc.add(producto);

                    // Determinar unidad
                    String unidad = tipoVenta != null && tipoVenta.toLowerCase().contains("caja") ? "caja(s)" : "par(es)";

                    // Cantidad, precio y subtotal
                    String detalle = String.format("  %d %s x $%,.0f = $%,.0f",
                            cantidad, unidad, precio, subtotal);
                    Paragraph detalleP = new Paragraph(detalle, fuenteNormal);
                    doc.add(detalleP);

                    doc.add(new Paragraph(" ")); // Espacio entre productos
                }
            }
        }
    }

    /**
     * Agrega los totales
     */
    private void agregarTotales(Document doc, DatosVenta datos) throws DocumentException {
        // Subtotal
        Paragraph subtotal = new Paragraph(
                String.format("Subtotal:        $%,10.0f", datos.subtotal),
                fuenteNormal
        );
        subtotal.setAlignment(Element.ALIGN_RIGHT);
        doc.add(subtotal);

        // Descuento si aplica
        if (datos.descuento > 0) {
            Paragraph descuento = new Paragraph(
                    String.format("Descuento:       $%,10.0f", datos.descuento),
                    fuenteNormal
            );
            descuento.setAlignment(Element.ALIGN_RIGHT);
            doc.add(descuento);
        }

        // Total
        Paragraph total = new Paragraph(
                String.format("TOTAL:           $%,10.0f", datos.total),
                fuenteNegrita
        );
        total.setAlignment(Element.ALIGN_RIGHT);
        doc.add(total);
    }

    /**
     * Agrega pie de página
     */
    private void agregarPieDePagina(Document doc, DatosVenta datos) throws DocumentException {
        doc.add(new Paragraph(" ")); // Espacio

        Paragraph metodoPago = new Paragraph("Método de pago: "
                + (datos.tipoPago != null ? datos.tipoPago.toUpperCase() : "EFECTIVO"),
                fuenteNormal
        );
        metodoPago.setAlignment(Element.ALIGN_CENTER);
        doc.add(metodoPago);

        Paragraph estado = new Paragraph("Estado: "
                + (datos.estado != null ? datos.estado.toUpperCase() : "COMPLETADA"),
                fuenteNormal
        );
        estado.setAlignment(Element.ALIGN_CENTER);
        doc.add(estado);

        doc.add(new Paragraph(" ")); // Espacio

        Paragraph agradecimiento = new Paragraph("¡Gracias por su compra!", fuenteNegrita);
        agradecimiento.setAlignment(Element.ALIGN_CENTER);
        doc.add(agradecimiento);

        Paragraph despedida = new Paragraph("Garantía 30 días (Costura/Fabricación). Requiere recibo original. No cubre mal uso o desgaste normal.", fuentePequeña);
        despedida.setAlignment(Element.ALIGN_CENTER);
        doc.add(despedida);
    }

    /**
     * Obtiene los datos de la venta
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
                    datos.nombreUsuario = rs.getString("usuario_nombre");  // NUEVO
                    datos.cargoUsuario = rs.getString("usuario_cargo");    // NUEVO
                    try { datos.idUsuario = rs.getInt("usuario_id"); } catch (SQLException ignore) {}
                    try { datos.idBodegaUsuario = (Integer) rs.getObject("usuario_bodega"); } catch (SQLException ignore) {}
                    return datos;
                }
            }
        }

        return null;
    }

    /**
     * Abre el PDF generado con el visor predeterminado
     */
    private void abrirPDF(String rutaArchivo) {
        try {
            java.awt.Desktop.getDesktop().open(new java.io.File(rutaArchivo));
        } catch (Exception e) {
            System.err.println("No se pudo abrir el PDF automáticamente: " + e.getMessage());
        }
    }

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
        String nombreUsuario;  // NUEVO
        String cargoUsuario;
        int idUsuario;
        Integer idBodegaUsuario;
    }

    private static class DatosBodega {
        String nombre;
        String direccion;
        String telefono;
    }

    private DatosBodega obtenerDatosBodega(Connection con, int idBodega) throws SQLException {
        String sql = "SELECT nombre, direccion, telefono FROM bodegas WHERE id_bodega = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idBodega);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DatosBodega b = new DatosBodega();
                    b.nombre = rs.getString("nombre");
                    // Algunas instalaciones pueden no tener direccion/telefono; usar getObject defensivo
                    try { b.direccion = rs.getString("direccion"); } catch (SQLException ignore) {}
                    try { b.telefono = rs.getString("telefono"); } catch (SQLException ignore) {}
                    return b;
                }
            }
        }
        return null;
    }
}
