package raven.clases.productos;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.output.PrinterOutputStream;
import javax.print.PrintService;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import raven.controlador.principal.AppConfig;
import raven.controlador.principal.conexion;

/**
 * Clase para imprimir comprobantes de traspasos en impresora 80mm
 */
public class ImpresionTraspasoPOST {

    private String nombreImpresora;

    // Estilos reusable
    private Style estiloTitulo;
    private Style estiloNegrita;
    private Style estiloNormal;
    private Style estiloCentrado;
    private Style estiloDerecha;
    private Style estiloPequeno;

    public ImpresionTraspasoPOST(String nombreImpresora) {
        this.nombreImpresora = nombreImpresora;
        inicializarEstilos();
    }

    public ImpresionTraspasoPOST() {
        this.nombreImpresora = null;
        inicializarEstilos();
    }

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

    public boolean imprimirTraspaso(int idTraspaso) {
        Connection con = null;
        EscPos escpos = null;

        try {
            con = conexion.getInstance().createConnection();
            DatosTraspaso datos = obtenerDatosTraspaso(con, idTraspaso);
            if (datos == null) {
                System.err.println("No se encontraron datos para el traspaso #" + idTraspaso);
                return false;
            }

            PrintService printService = buscarImpresora();
            if (printService == null) {
                System.err.println("No se encontró la impresora.");
                listarImpresorasDisponibles();
                return false;
            }

            PrinterOutputStream printerOutputStream = new PrinterOutputStream(printService);
            escpos = new EscPos(printerOutputStream);
            escpos.initializePrinter();

            imprimirEncabezado(escpos, datos);
            imprimirLineaSeparadora(escpos);
            imprimirInfoBodegas(escpos, datos);
            imprimirLineaSeparadora(escpos);
            imprimirDetalles(escpos, con, idTraspaso);
            imprimirLineaSeparadora(escpos);
            imprimirPie(escpos, datos);

            escpos.feed(4);
            escpos.cut(EscPos.CutMode.PART);
            escpos.close();

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al imprimir traspaso: " + e.getMessage());
            return false;
        } finally {
            if (escpos != null) {
                try {
                    escpos.close();
                } catch (Exception ignore) {
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    private DatosTraspaso obtenerDatosTraspaso(Connection con, int idTraspaso) throws SQLException {
        String sql = "SELECT t.*, "
                + "bo.nombre as bodega_origen, bd.nombre as bodega_destino, "
                + "u.nombre as usuario_nombre "
                + "FROM traspasos t "
                + "LEFT JOIN bodegas bo ON t.id_bodega_origen = bo.id_bodega "
                + "LEFT JOIN bodegas bd ON t.id_bodega_destino = bd.id_bodega "
                + "LEFT JOIN usuarios u ON t.id_usuario_solicita = u.id_usuario "
                + "WHERE t.id_traspaso = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idTraspaso);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DatosTraspaso d = new DatosTraspaso();
                    d.idTraspaso = idTraspaso;
                    d.fechaSolicitud = rs.getTimestamp("fecha_solicitud");
                    d.fechaEnvio = rs.getTimestamp("fecha_envio");
                    d.fechaRecepcion = rs.getTimestamp("fecha_recepcion");
                    d.estado = rs.getString("estado");
                    d.origen = rs.getString("bodega_origen");
                    d.destino = rs.getString("bodega_destino");
                    d.usuario = rs.getString("usuario_nombre");
                    d.observaciones = rs.getString("observaciones");
                    d.montoTotal = rs.getBigDecimal("monto_total");
                    d.montoRecibido = rs.getBigDecimal("monto_recibido");
                    return d;
                }
            }
        }
        return null;
    }

    private void imprimirEncabezado(EscPos escpos, DatosTraspaso datos) throws Exception {
        escpos.writeLF(estiloTitulo, AppConfig.name); // O nombre genérico
        escpos.feed(1);
        escpos.writeLF(estiloNegrita, "COMPROBANTE DE TRASPASO");
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        if (datos.fechaSolicitud != null) {
            escpos.writeLF(estiloCentrado, "Fecha: " + sdf.format(datos.fechaSolicitud));
        }
        escpos.writeLF(estiloCentrado, "No. " + String.format("%06d", datos.idTraspaso));
        if (datos.usuario != null) {
            escpos.writeLF(estiloCentrado, "Solicitó: " + datos.usuario);
        }
    }

    private void imprimirInfoBodegas(EscPos escpos, DatosTraspaso datos) throws Exception {
        escpos.writeLF(estiloNormal, "Origen:  " + (datos.origen != null ? datos.origen : "N/A"));
        escpos.writeLF(estiloNormal, "Destino: " + (datos.destino != null ? datos.destino : "N/A"));
        escpos.writeLF(estiloNormal, "Estado:  " + (datos.estado != null ? datos.estado.toUpperCase() : "N/A"));
    }

    private void imprimirDetalles(EscPos escpos, Connection con, int idTraspaso) throws Exception {
        String sql = "SELECT td.cantidad_solicitada, td.cantidad_enviada, td.cantidad_recibida, "
                + "td.precio_unitario, "
                + "p.nombre, t.numero, t.sistema, c.nombre as color "
                + "FROM traspaso_detalles td "
                + "INNER JOIN productos p ON td.id_producto = p.id_producto "
                + "LEFT JOIN producto_variantes pv ON td.id_variante = pv.id_variante "
                + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                + "LEFT JOIN colores c ON pv.id_color = c.id_color "
                + "WHERE td.id_traspaso = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idTraspaso);
            try (ResultSet rs = ps.executeQuery()) {
                escpos.writeLF(estiloNegrita, "PRODUCTOS");
                while (rs.next()) {
                    String nombre = rs.getString("nombre");
                    String color = rs.getString("color");
                    String tallaNum = rs.getString("numero");
                    // String tallaSis = rs.getString("sistema");

                    int sol = rs.getInt("cantidad_solicitada");
                    int env = rs.getInt("cantidad_enviada");
                    int rec = rs.getInt("cantidad_recibida");

                    String nombreMostrar = nombre.length() > 38 ? nombre.substring(0, 35) + "..." : nombre;
                    escpos.writeLF(estiloNormal, nombreMostrar);

                    String variante = "";
                    if (color != null)
                        variante += color + " ";
                    if (tallaNum != null)
                        variante += "- Talla " + tallaNum;
                    if (!variante.isEmpty())
                        escpos.writeLF(estiloNormal, "  (" + variante + ")");

                    // Mostrar cantidades y precio
                    String cantStr = String.format("  Sol:%d  Env:%d  Rec:%d", sol, env, rec);
                    escpos.writeLF(estiloNormal, cantStr);

                    java.math.BigDecimal precio = rs.getBigDecimal("precio_unitario");
                    if (precio != null && precio.doubleValue() > 0) {
                        double subtotal = precio.doubleValue() * env;
                        escpos.writeLF(estiloNormal, String.format("  $ %,.0f  x  %d  =  $ %,.0f",
                                precio.doubleValue(), env, subtotal));
                    }
                    escpos.feed(1);
                }
            }
        }
    }

    private void imprimirPie(EscPos escpos, DatosTraspaso datos) throws Exception {
        if (datos.montoTotal != null && datos.montoTotal.doubleValue() > 0) {
            escpos.writeLF(estiloDerecha, String.format("Total Enviado: $ %,.0f", datos.montoTotal.doubleValue()));
        }
        if (datos.montoRecibido != null && datos.montoRecibido.doubleValue() > 0) {
            escpos.writeLF(estiloDerecha, String.format("Total Recibido: $ %,.0f", datos.montoRecibido.doubleValue()));
        }
        escpos.feed(1);

        if (datos.observaciones != null && !datos.observaciones.isEmpty()) {
            escpos.writeLF(estiloNormal, "Obs: " + datos.observaciones);
            escpos.feed(1);
        }
        escpos.writeLF(estiloCentrado, "--- FIN DEL DOCUMENTO ---");
    }

    private void imprimirLineaSeparadora(EscPos escpos) throws Exception {
        escpos.writeLF(estiloCentrado, "----------------------------------------");
    }

    private PrintService buscarImpresora() {
        PrintService[] printServices = javax.print.PrintServiceLookup.lookupPrintServices(null, null);
        if (nombreImpresora == null || nombreImpresora.trim().isEmpty()) {
            if (printServices.length > 0)
                return printServices[0];
            return null;
        }
        for (PrintService ps : printServices) {
            if (ps.getName().toLowerCase().contains(nombreImpresora.toLowerCase())) {
                return ps;
            }
        }
        return null;
    }

    public void listarImpresorasDisponibles() {
        PrintService[] printServices = javax.print.PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService ps : printServices) {
            System.out.println("Printer: " + ps.getName());
        }
    }

    private static class DatosTraspaso {
        int idTraspaso;
        Timestamp fechaSolicitud;
        Timestamp fechaEnvio;
        Timestamp fechaRecepcion;
        String estado;
        String origen;
        String destino;
        String usuario;
        String observaciones;
        java.math.BigDecimal montoTotal;
        java.math.BigDecimal montoRecibido;
    }
}
