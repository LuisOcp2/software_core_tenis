package raven.clases.inventario;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.output.PrinterOutputStream;
import raven.controlador.principal.AppConfig;
import raven.controlador.principal.conexion;
import raven.modelos.DetalleConteoInventario;

import javax.print.PrintService;
import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Clase para imprimir conteos de inventario en impresora POS térmica (80mm)
 * Adaptado de ImpresionPOST
 */
public class ImpresionPOSInventario {

    private String nombreImpresora;

    // Estilos reutilizables
    private Style estiloTitulo;
    private Style estiloNegrita;
    private Style estiloNormal;
    private Style estiloCentrado;
    private Style estiloDerecha;
    private Style estiloPequeno;

    public ImpresionPOSInventario(String nombreImpresora) {
        this.nombreImpresora = nombreImpresora;
        inicializarEstilos();
    }

    public ImpresionPOSInventario() {
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

    /**
     * Imprime el conteo de inventario
     */
    public boolean imprimirConteo(int idConteo) {
        Connection con = null;
        EscPos escpos = null;

        try {
            con = conexion.getInstance().createConnection();
            DatosConteo datosConteo = obtenerDatosConteo(con, idConteo);

            if (datosConteo == null) {
                JOptionPane.showMessageDialog(null, "No se encontraron datos para el conteo #" + idConteo);
                return false;
            }

            // Obtener detalles (productos)
            ServiceConteoInventario service = new ServiceConteoInventario();
            List<DetalleConteoInventario> detalles = service.obtenerDetallesConteo(idConteo);

            PrintService printService = buscarImpresora();
            if (printService == null) {
                // Fallback: Preview TXT
                JOptionPane.showMessageDialog(null, "No se encontró impresora POS. Generando vista previa.");
                // Here we could implement generating a TXT preview similar to sales if needed
                // For now just error
                return false;
            }

            // Crear salida a la impresora
            PrinterOutputStream printerOutputStream = new PrinterOutputStream(printService);
            escpos = new EscPos(printerOutputStream);

            escpos.initializePrinter();

            // 1. Encabezado
            imprimirEncabezado(escpos, datosConteo);
            imprimirLineaSeparadora(escpos);

            // 2. Detalles
            imprimirDetalles(escpos, detalles);
            imprimirLineaSeparadora(escpos);

            // 3. Resumen/Pie
            imprimirResumen(escpos, datosConteo, detalles);

            // Cortar
            escpos.feed(4);
            escpos.cut(EscPos.CutMode.PART);
            escpos.close();

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al imprimir: " + e.getMessage());
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

    private void imprimirEncabezado(EscPos escpos, DatosConteo datos) throws Exception {
        escpos.writeLF(estiloTitulo, AppConfig.name != null ? AppConfig.name : "INVENTARIO");
        escpos.feed(1);
        escpos.writeLF(estiloNegrita, "REPORTE DE CONTEO");

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        escpos.writeLF(estiloCentrado, "Fecha: " + sdf.format(new Date())); // Fecha impresion
        escpos.writeLF(estiloCentrado, "Conteo No. " + datos.idConteo);
        escpos.writeLF(estiloCentrado, "Bodega: " + datos.nombreBodega);
        escpos.writeLF(estiloCentrado, "Responsable: " + datos.nombreResponsable);

        if (datos.tipoConteo != null) {
            escpos.writeLF(estiloCentrado, "Tipo: " + datos.tipoConteo.toUpperCase());
        }

        escpos.feed(1);
    }

    private void imprimirDetalles(EscPos escpos, List<DetalleConteoInventario> detalles) throws Exception {
        // Headers
        // Item | Sist | Cont | Dif
        // Small font maybe? Or normal

        escpos.writeLF(estiloNormal, "PRODUCTO");
        escpos.writeLF(estiloNormal, "   Talla/Color      Sist  Cont  Dif");
        imprimirLineaSeparadora(escpos);

        for (DetalleConteoInventario d : detalles) {
            String prodName = d.getProducto().getName();
            if (prodName.length() > 38)
                prodName = prodName.substring(0, 38);

            escpos.writeLF(estiloNormal, prodName);

            String talla = d.getProducto().getSize();
            String color = d.getProducto().getColor();
            String metaInfo = (talla != null ? talla : "-") + "/" + (color != null ? color : "-");
            if (metaInfo.length() > 18)
                metaInfo = metaInfo.substring(0, 18);

            // Format numbers columns
            // Sist (4 chars) Cont (4 chars) Dif (4 chars)
            String nums = String.format("%4d  %4d  %4d", d.getStockSistema(), d.getStockContado(), d.getDiferencia());

            // Pad metaInfo to align
            String line2 = String.format("   %-16s %s", metaInfo, nums);
            escpos.writeLF(estiloNormal, line2);
        }
    }

    private void imprimirResumen(EscPos escpos, DatosConteo datos, List<DetalleConteoInventario> detalles)
            throws Exception {
        int totalItems = detalles.size();
        int totalUnidades = 0;
        int totalDiferencia = 0;

        for (DetalleConteoInventario d : detalles) {
            totalUnidades += d.getStockContado();
            totalDiferencia += d.getDiferencia();
        }

        escpos.writeLF(estiloDerecha, "Total Items: " + totalItems);
        escpos.writeLF(estiloDerecha, "Unidades Contadas: " + totalUnidades);
        escpos.writeLF(estiloDerecha, "Diferencia Total: " + totalDiferencia);

        escpos.feed(1);
        escpos.writeLF(estiloCentrado, "--- Fin del Reporte ---");

        if (datos.estado != null) {
            escpos.writeLF(estiloCentrado, "Estado: " + datos.estado.toUpperCase());
        }

        // Espacio firma
        escpos.feed(3);
        escpos.writeLF(estiloCentrado, "__________________________");
        escpos.writeLF(estiloCentrado, "Firma Responsable");
    }

    private void imprimirLineaSeparadora(EscPos escpos) throws Exception {
        escpos.writeLF(estiloCentrado, "--------------------------------");
    }

    private PrintService buscarImpresora() {
        PrintService[] printServices = javax.print.PrintServiceLookup.lookupPrintServices(null, null);
        if (nombreImpresora == null || nombreImpresora.trim().isEmpty()) {
            return printServices.length > 0 ? printServices[0] : null;
        }
        for (PrintService ps : printServices) {
            if (ps.getName().toLowerCase().contains(nombreImpresora.toLowerCase())) {
                return ps;
            }
        }
        // Try to find default or "POS" or "Printer" if specific not found?
        // For now return null or first
        return printServices.length > 0 ? printServices[0] : null;
    }

    // --- Data Helper --- (Inner Class or method)

    private DatosConteo obtenerDatosConteo(Connection con, int idConteo) throws SQLException {
        String sql = "SELECT c.id_conteo, c.fecha_programada, c.tipo_conteo, c.estado, " +
                "b.nombre as nombre_bodega, u.nombre as nombre_responsable " +
                "FROM conteos_inventario c " +
                "LEFT JOIN bodegas b ON c.id_bodega = b.id_bodega " +
                "LEFT JOIN usuarios u ON c.id_usuario_responsable = u.id_usuario " +
                "WHERE c.id_conteo = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idConteo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DatosConteo d = new DatosConteo();
                    d.idConteo = rs.getInt("id_conteo");
                    d.fecha = rs.getTimestamp("fecha_programada");
                    d.tipoConteo = rs.getString("tipo_conteo");
                    d.estado = rs.getString("estado");
                    d.nombreBodega = rs.getString("nombre_bodega");
                    d.nombreResponsable = rs.getString("nombre_responsable");
                    return d;
                }
            }
        }
        return null;
    }

    private static class DatosConteo {
        int idConteo;
        Date fecha;
        String tipoConteo;
        String estado;
        String nombreBodega;
        String nombreResponsable;
    }
}
