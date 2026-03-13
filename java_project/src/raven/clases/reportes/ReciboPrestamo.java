package raven.clases.reportes;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.FileOutputStream;
import java.sql.*;
import java.text.SimpleDateFormat;
import raven.controlador.principal.AppConfig;
import raven.controlador.principal.conexion;

/**
 * Clase para generar recibos de préstamo en formato PDF optimizado para impresoras
 * térmicas de tirilla. Tamaño estándar: 80mm de ancho (226 puntos)
 */
public class ReciboPrestamo {

    // Ancho estándar para impresora térmica de 80mm
    private static final float ANCHO_TIRILLA = 226f; // 80mm en puntos

    // Fuentes personalizadas
    private Font fuenteTitulo;
    private Font fuenteNormal;
    private Font fuenteNegrita;
    private Font fuentePequeña;

    public ReciboPrestamo() {
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
     * Genera el recibo de préstamo en formato PDF
     *
     * @param idPrestamo ID del préstamo a imprimir
     * @return true si se generó correctamente
     */
    public boolean generarRecibo(int idPrestamo) {
        Connection con = null;
        Document documento = null;

        try {
            // Crear documento con tamaño personalizado para tirilla
            documento = new Document(new Rectangle(ANCHO_TIRILLA, 842f)); // Alto variable
            String rutaArchivo = "recibo_prestamo_" + idPrestamo + ".pdf";
            PdfWriter writer = PdfWriter.getInstance(documento, new FileOutputStream(rutaArchivo));

            documento.open();

            // Obtener datos del préstamo
            con = conexion.getInstance().createConnection();
            DatosPrestamo datosPrestamo = obtenerDatosPrestamo(con, idPrestamo);

            if (datosPrestamo == null) {
                System.err.println("No se encontraron datos para el préstamo #" + idPrestamo);
                return false;
            }

            // Construir el recibo
            agregarEncabezado(documento, datosPrestamo);
            agregarLineaSeparadora(documento);
            agregarDatosPrestatario(documento, datosPrestamo);
            agregarLineaSeparadora(documento);
            agregarDetalleProducto(documento, datosPrestamo);
            agregarLineaSeparadora(documento);
            agregarInformacionAdicional(documento, datosPrestamo);
            agregarLineaSeparadora(documento);
            agregarPieDePagina(documento, datosPrestamo);

            documento.close();

            // Abrir el PDF automáticamente
            abrirPDF(rutaArchivo);

            System.out.println("Recibo de préstamo generado exitosamente: " + rutaArchivo);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error al generar recibo de préstamo: " + e.getMessage());
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
    private void agregarEncabezado(Document doc, DatosPrestamo datos) throws DocumentException {
        Paragraph titulo = new Paragraph(AppConfig.name, fuenteTitulo);//nombre de la empresa
        titulo.setAlignment(Element.ALIGN_CENTER);
        doc.add(titulo);

        Paragraph nit = new Paragraph("NIT: ESPACIO BLANCO", fuenteNormal);
        nit.setAlignment(Element.ALIGN_CENTER);
        doc.add(nit);

        Paragraph direccion = new Paragraph("QUINTA MALL\nTel: (123) 456-7890", fuentePequeña);
        direccion.setAlignment(Element.ALIGN_CENTER);
        doc.add(direccion);

        doc.add(new Paragraph(" ")); // Espacio

        Paragraph factura = new Paragraph("COMPROBANTE DE PRÉSTAMO", fuenteNegrita);
        factura.setAlignment(Element.ALIGN_CENTER);
        doc.add(factura);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        Paragraph fechaPrestamo = new Paragraph("Fecha préstamo: " + sdf.format(datos.fechaPrestamo), fuenteNormal);
        fechaPrestamo.setAlignment(Element.ALIGN_CENTER);
        doc.add(fechaPrestamo);

        Paragraph numPrestamo = new Paragraph("No. " + String.format("%06d", datos.idPrestamo), fuenteNormal);
        numPrestamo.setAlignment(Element.ALIGN_CENTER);
        doc.add(numPrestamo);

        // Usuario que realizó el préstamo
        if (datos.nombreUsuario != null && !datos.nombreUsuario.trim().isEmpty()) {
            Paragraph usuario = new Paragraph("Usuario: " + datos.nombreUsuario, fuentePequeña);
            usuario.setAlignment(Element.ALIGN_CENTER);
            doc.add(usuario);
        }

        // Bodega
        if (datos.nombreBodega != null && !datos.nombreBodega.trim().isEmpty()) {
            Paragraph bodega = new Paragraph("Bodega: " + datos.nombreBodega, fuentePequeña);
            bodega.setAlignment(Element.ALIGN_CENTER);
            doc.add(bodega);
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
     * Agrega datos del prestatario
     */
    private void agregarDatosPrestatario(Document doc, DatosPrestamo datos) throws DocumentException {
        Paragraph tituloPrestatario = new Paragraph("DATOS DEL PRESTATARIO", fuenteNegrita);
        tituloPrestatario.setAlignment(Element.ALIGN_CENTER);
        doc.add(tituloPrestatario);

        doc.add(new Paragraph(" ")); // Espacio

        Paragraph nombre = new Paragraph("Nombre: " + 
            (datos.nombrePrestatario != null ? datos.nombrePrestatario : "N/A"), fuenteNormal);
        doc.add(nombre);

        if (datos.celularPrestatario != null && !datos.celularPrestatario.trim().isEmpty()) {
            Paragraph celular = new Paragraph("Celular: " + datos.celularPrestatario, fuenteNormal);
            doc.add(celular);
        }

        if (datos.direccionPrestatario != null && !datos.direccionPrestatario.trim().isEmpty()) {
            Paragraph direccion = new Paragraph("Dirección: " + datos.direccionPrestatario, fuenteNormal);
            doc.add(direccion);
        }
    }

    /**
     * Agrega el detalle del producto prestado
     */
    private void agregarDetalleProducto(Document doc, DatosPrestamo datos) throws DocumentException {
        Paragraph tituloProducto = new Paragraph("PRODUCTO PRESTADO", fuenteNegrita);
        tituloProducto.setAlignment(Element.ALIGN_CENTER);
        doc.add(tituloProducto);

        doc.add(new Paragraph(" ")); // Espacio

        // Nombre del producto
        String nombreProducto = datos.nombreProducto != null ? datos.nombreProducto : "N/A";
        Paragraph producto = new Paragraph("Producto: " + nombreProducto, fuenteNormal);
        doc.add(producto);

        // Color (si existe)
        if (datos.color != null && !datos.color.trim().isEmpty()) {
            Paragraph color = new Paragraph("Color: " + datos.color, fuenteNormal);
            doc.add(color);
        }

        // Talla
        String talla = datos.talla != null ? datos.talla : "N/A";
        Paragraph tallaP = new Paragraph("Talla: " + talla, fuenteNormal);
        doc.add(tallaP);

        // Pie prestado
        String pie = datos.pie != null ? datos.pie : "N/A";
        Paragraph pieP = new Paragraph("Pie prestado: " + pie.toUpperCase(), fuenteNegrita);
        doc.add(pieP);
    }

    /**
     * Agrega información adicional del préstamo
     */
    private void agregarInformacionAdicional(Document doc, DatosPrestamo datos) throws DocumentException {
        // Estado
        String estado = datos.estado != null ? datos.estado : "N/A";
        Paragraph estadoP = new Paragraph("Estado: " + estado.toUpperCase(), fuenteNegrita);
        estadoP.setAlignment(Element.ALIGN_CENTER);
        doc.add(estadoP);

        // Fecha de devolución (si existe)
        if (datos.fechaDevolucion != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            Paragraph devolucion = new Paragraph("Fecha devolución: " + sdf.format(datos.fechaDevolucion), fuenteNormal);
            devolucion.setAlignment(Element.ALIGN_CENTER);
            doc.add(devolucion);
        } else {
            Paragraph devolucion = new Paragraph("Fecha devolución: PENDIENTE", fuenteNormal);
            devolucion.setAlignment(Element.ALIGN_CENTER);
            doc.add(devolucion);
        }

        // Observaciones (si existen)
        if (datos.observaciones != null && !datos.observaciones.trim().isEmpty()) {
            doc.add(new Paragraph(" ")); // Espacio
            Paragraph obs = new Paragraph("Observaciones:", fuenteNegrita);
            doc.add(obs);
            Paragraph obsTexto = new Paragraph(datos.observaciones, fuentePequeña);
            doc.add(obsTexto);
        }
    }

    /**
     * Agrega pie de página
     */
    private void agregarPieDePagina(Document doc, DatosPrestamo datos) throws DocumentException {
        doc.add(new Paragraph(" ")); // Espacio

        Paragraph aviso = new Paragraph("IMPORTANTE", fuenteNegrita);
        aviso.setAlignment(Element.ALIGN_CENTER);
        doc.add(aviso);

        Paragraph condiciones = new Paragraph(
            "El prestatario se compromete a devolver el producto en las mismas condiciones en que fue entregado.",
            fuentePequeña
        );
        condiciones.setAlignment(Element.ALIGN_CENTER);
        doc.add(condiciones);

        doc.add(new Paragraph(" ")); // Espacio

        Paragraph firma = new Paragraph("_________________________\nFirma del prestatario", fuentePequeña);
        firma.setAlignment(Element.ALIGN_CENTER);
        doc.add(firma);

        doc.add(new Paragraph(" ")); // Espacio
        doc.add(new Paragraph(" ")); // Espacio

        Paragraph agradecimiento = new Paragraph("¡Gracias por su confianza!", fuenteNegrita);
        agradecimiento.setAlignment(Element.ALIGN_CENTER);
        doc.add(agradecimiento);
    }

    /**
     * Obtiene los datos del préstamo desde la base de datos
     */
    private DatosPrestamo obtenerDatosPrestamo(Connection con, int idPrestamo) throws SQLException {
        String sql = "SELECT pz.*, "
                + "p.nombre as producto_nombre, "
                + "CONCAT(t.numero, ' ', t.sistema) as talla, "
                + "c.nombre as color, "
                + "b.nombre as bodega_nombre, "
                + "u.nombre as usuario_nombre "
                + "FROM prestamos_zapatos pz "
                + "LEFT JOIN producto_variantes pv ON pz.id_variante = pv.id_variante "
                + "LEFT JOIN productos p ON pv.id_producto = p.id_producto "
                + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                + "LEFT JOIN colores c ON pv.id_color = c.id_color "
                + "LEFT JOIN bodegas b ON pz.id_bodega = b.id_bodega "
                + "LEFT JOIN usuarios u ON pz.id_usuario = u.id_usuario "
                + "WHERE pz.id_prestamo = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPrestamo);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DatosPrestamo datos = new DatosPrestamo();
                    datos.idPrestamo = idPrestamo;
                    datos.fechaPrestamo = rs.getTimestamp("fecha_prestamo");
                    datos.fechaDevolucion = rs.getTimestamp("fecha_devolucion");
                    datos.nombrePrestatario = rs.getString("nombre_prestatario");
                    datos.celularPrestatario = rs.getString("celular_prestatario");
                    datos.direccionPrestatario = rs.getString("direccion_prestatario");
                    datos.nombreProducto = rs.getString("producto_nombre");
                    datos.talla = rs.getString("talla");
                    datos.color = rs.getString("color");
                    datos.pie = rs.getString("pie");
                    datos.estado = rs.getString("estado");
                    datos.observaciones = rs.getString("observaciones");
                    datos.nombreBodega = rs.getString("bodega_nombre");
                    datos.nombreUsuario = rs.getString("usuario_nombre");
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
     * Clase interna para almacenar datos del préstamo
     */
    private static class DatosPrestamo {
        int idPrestamo;
        Timestamp fechaPrestamo;
        Timestamp fechaDevolucion;
        String nombrePrestatario;
        String celularPrestatario;
        String direccionPrestatario;
        String nombreProducto;
        String talla;
        String color;
        String pie;
        String estado;
        String observaciones;
        String nombreBodega;
        String nombreUsuario;
    }
}