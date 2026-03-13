/*
 * TraspasoController - VERSIÓN CORREGIDA
 * Corrige errores críticos en navegación de pasos y gestión de productos
 */
package raven.controlador.productos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Frame;
// import raven.controlador.admin.SessionManager; (Removed legacy import)
import raven.application.form.productos.traspasos.traspasos;
import raven.clases.admin.UserSession;
import raven.clases.productos.TraspasoService.StockInfo;
import raven.clases.productos.TraspasoService;
import raven.clases.productos.TraspasoDatos;
import raven.clases.productos.ProductoTraspasoItem;
import raven.clases.productos.Bodega;
import raven.controlador.principal.conexion;
import raven.componentes.notificacion.Notification;
import raven.utils.tono.CorporateTone;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

public class TraspasoController {

    private final TraspasoService traspasoService;
    private TraspasoDatos traspasoActual;
    private int idUsuarioActual;
    private final UserSession session;
    private boolean modoEdicion = false;
    private String numeroTraspasoOriginal = null;

    public TraspasoController(UserSession session) {
        this.session = session;
        this.traspasoService = new TraspasoService();
        this.traspasoActual = new TraspasoDatos();
        
        // Inicializar con la bodega del usuario si existe
        if (session != null && session.getIdBodegaUsuario() != null) {
            this.traspasoActual.setIdBodegaOrigen(session.getIdBodegaUsuario());
            System.out.println("DEBUG Bodega origen inicializada por session: " + session.getIdBodegaUsuario());
        }

        this.idUsuarioActual = (session != null && session.getCurrentUser() != null) 
                                ? session.getCurrentUser().getIdUsuario() 
                                : -1;
        String nombreUsuario = (session != null && session.getCurrentUser() != null) ? session.getCurrentUser().getNombre() : "Desconocido";
        System.out.println("SUCCESS  TraspasoController inicializado para usuario: " + nombreUsuario);
    }

    // ================================
    // GETTERS Y MÉTODOS BÁSICOS
    // ================================
    public TraspasoDatos getTraspasoActual() {
        return traspasoActual;
    }

    public void configurarModoEdicion(String numeroOriginal) {
        this.modoEdicion = true;
        this.numeroTraspasoOriginal = numeroOriginal;
        System.out.println("Controller configurado para edicion: " + numeroOriginal);
    }

    public boolean esModoEdicion() {
        return modoEdicion;
    }

    public String getNumeroTraspasoOriginal() {
        return numeroTraspasoOriginal;
    }

    public void reiniciarTraspaso() {
        this.traspasoActual = new TraspasoDatos();
        
        // Re-inicializar con la bodega del usuario
        if (session != null && session.getIdBodegaUsuario() != null) {
            this.traspasoActual.setIdBodegaOrigen(session.getIdBodegaUsuario());
        }
        
        System.out.println("Traspaso reiniciado");
    }

    public int getIdUsuarioActual() {
        return idUsuarioActual;
    }

    public void setIdUsuarioActual(int idUsuario) {
        this.idUsuarioActual = idUsuario;
    }

    /**
     * Obtiene el ID de la bodega asociada al usuario actual.
     * MANTENER COMPATIBILIDAD CON LA UI (traspasos.java)
     */
    public Integer getBodegaUsuario() {
        return (session != null) ? session.getIdBodegaUsuario() : null;
    }

    // ================================
    // MÉTODOS DEL PASO 1 - DATOS BÁSICOS (CORREGIDO)
    // ================================
    public List<Bodega> cargarBodegas() {
        try {
            List<Bodega> bodegas = traspasoService.obtenerBodegasActivas();
            System.out.println("SUCCESS  Bodegas cargadas via Service: " + bodegas.size());
            return bodegas;
        } catch (SQLException e) {
            System.err.println("ERROR  Error al cargar bodegas: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public String generarNumeroTraspaso() {
        try {
            // Si estamos en modo edición, usar el número original
            if (modoEdicion && numeroTraspasoOriginal != null) {
                System.out.println("Modo edicion: usando numero original " + numeroTraspasoOriginal);
                return numeroTraspasoOriginal;
            }

            // Si es creación nueva, generar nuevo número
            String numero = traspasoService.generarNumeroTraspaso();
            System.out.println("Nuevo numero generado: " + numero);
            return numero;

        } catch (SQLException e) {
            System.err.println("ERROR  Error generando número de traspaso: " + e.getMessage());
            e.printStackTrace();

            // Fallback con timestamp para evitar duplicados
            long timestamp = System.currentTimeMillis() % 1000000;
            String fallback = "TR" + String.format("%06d", timestamp);
            System.out.println("Usando numero fallback: " + fallback);
            return fallback;
        }
    }

    public boolean validarDatosPaso1(String numeroTraspaso, Bodega bodegaOrigen,
            Bodega bodegaDestino, String tipoTraspaso, String motivo) {
        StringBuilder errores = new StringBuilder();

        if (numeroTraspaso == null || numeroTraspaso.trim().isEmpty()) {
            errores.append("- El número de traspaso es obligatorio\n");
        }

        if (bodegaOrigen == null) {
            errores.append("- Debe seleccionar una bodega de origen\n");
        }

        if (bodegaDestino == null) {
            errores.append("- Debe seleccionar una bodega de destino\n");
        }

        if (bodegaOrigen != null && bodegaDestino != null) {
            if (bodegaOrigen.getIdBodega().equals(bodegaDestino.getIdBodega())) {
                errores.append("- La bodega de origen y destino deben ser diferentes\n");
            }
        }

        if (tipoTraspaso == null || "Seleccionar".equals(tipoTraspaso)) {
            errores.append("- Debe seleccionar un tipo de traspaso\n");
        }

        // Motivo ahora es opcional
        // if (motivo == null || motivo.trim().isEmpty()) {
        // errores.append("- El motivo del traspaso es obligatorio\n");
        // } else if (motivo.trim().length() < 10) {
        // errores.append("- El motivo debe tener al menos 10 caracteres\n");
        // }

        if (errores.length() > 0) {
            JOptionPane.showMessageDialog(null,
                    "Por favor corrija los siguientes errores:\n\n" + errores.toString(),
                    "Errores de validación",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    // MÉTODO CORREGIDO - Ahora guarda correctamente los datos
    public void guardarDatosPaso1(String numeroTraspaso, Bodega bodegaOrigen,
            Bodega bodegaDestino, String tipoTraspaso,
            String motivo, String observaciones) {

        System.out.println("Guardando datos del paso 1...");

        traspasoActual.setNumeroTraspaso(numeroTraspaso);
        traspasoActual.setIdBodegaOrigen(bodegaOrigen.getIdBodega());
        traspasoActual.setNombreBodegaOrigen(bodegaOrigen.getNombre());
        traspasoActual.setIdBodegaDestino(bodegaDestino.getIdBodega());
        traspasoActual.setNombreBodegaDestino(bodegaDestino.getNombre());
        traspasoActual.setTipoTraspaso(tipoTraspaso);
        traspasoActual.setMotivoTraspaso(motivo);
        traspasoActual.setObservaciones(observaciones);
        if (traspasoActual.getIdUsuarioSolicita() == null || traspasoActual.getIdUsuarioSolicita() <= 0) {
            traspasoActual.setIdUsuarioSolicita(idUsuarioActual);
        }

        System.out.println("Datos del paso 1 guardados:");
        System.out.println("   - Número: " + numeroTraspaso);
        System.out.println("   - Origen: " + bodegaOrigen.getNombre() + " (ID: " + bodegaOrigen.getIdBodega() + ")");
        System.out.println("   - Destino: " + bodegaDestino.getNombre() + " (ID: " + bodegaDestino.getIdBodega() + ")");
        System.out.println("   - Tipo: " + tipoTraspaso);
        System.out.println("   - Motivo: " + motivo);
    }

    // ================================
    // MÉTODOS DEL PASO 2 - PRODUCTOS (CORREGIDO)
    // ================================
    // MÉTODO CORREGIDO - Ahora valida stock por bodega específica considerando
    // campo id_bodega
    public boolean validarStockEnBodegaOrigen(Integer idProducto, Integer idVariante,
            String tipo, int cantidad) {
        StockVerificationResult result = verificarStockDisponible(idProducto, idVariante, tipo, cantidad);
        if (!result.isExito()) {
            JOptionPane.showMessageDialog(null, result.getMensaje(), "Stock Insuficiente", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        System.out.println("SUCCESS  Stock validado correctamente");
        return true;
    }

    public static class StockVerificationResult {
        private boolean exito;
        private String mensaje;
        private int stockDisponible;

        public StockVerificationResult(boolean exito, String mensaje, int stockDisponible) {
            this.exito = exito;
            this.mensaje = mensaje;
            this.stockDisponible = stockDisponible;
        }

        public boolean isExito() {
            return exito;
        }

        public String getMensaje() {
            return mensaje;
        }

        public int getStockDisponible() {
            return stockDisponible;
        }
    }

    public StockVerificationResult verificarStockDisponible(Integer idProducto, Integer idVariante, String tipo,
            int cantidad) {
        if (traspasoActual.getIdBodegaOrigen() == null) {
            return new StockVerificationResult(false, "Debe completar el paso 1 antes de agregar productos", 0);
        }

        try {
            boolean stockSuficiente = traspasoService.validarStockBodega(
                    traspasoActual.getIdBodegaOrigen(),
                    idProducto,
                    idVariante,
                    tipo,
                    cantidad);

            if (!stockSuficiente) {
                StockInfo stockInfo = traspasoService.obtenerStockBodega(
                        traspasoActual.getIdBodegaOrigen(),
                        idProducto,
                        idVariante);

                int stockActual = stockInfo.getStock(tipo);

                String msg = "Stock insuficiente en la bodega origen.\n"
                        + "Stock disponible: " + stockActual + " " + tipo + "\n"
                        + "Cantidad solicitada: " + cantidad + " " + tipo;
                return new StockVerificationResult(false, msg, stockActual);
            }
            return new StockVerificationResult(true, "OK", cantidad);
        } catch (SQLException e) {
            e.printStackTrace();
            return new StockVerificationResult(false, "Error validando stock: " + e.getMessage(), 0);
        }
    }

    // MÉTODO CORREGIDO - Considera correctamente el campo id_bodega en
    // producto_variantes

    // MÉTODO CORREGIDO - Ahora agrega productos correctamente y valida duplicados
    public boolean agregarProducto(ProductoTraspasoItem producto) {
        // Wrapper legacy que mantiene el comportamiento UI original pero usa la lógica
        // desacoplada
        if (producto == null)
            return false;

        // 1. Validar Stock
        if (!validarStockEnBodegaOrigen(producto.getIdProducto(), producto.getIdVariante(), producto.getTipo(),
                producto.getCantidadSolicitada())) {
            return false;
        }

        // 2. Verificar duplicados
        ProductoTraspasoItem existente = buscarProductoExistente(producto);
        if (existente != null) {
            int opcion = JOptionPane.showConfirmDialog(null,
                    "El producto ya existe en el traspaso.\n¿Desea actualizar la cantidad?",
                    "Producto Existente",
                    JOptionPane.YES_NO_OPTION);

            if (opcion == JOptionPane.YES_OPTION) {
                int nuevaCantidad = existente.getCantidadSolicitada() + producto.getCantidadSolicitada();
                // Re-validar stock total
                if (!validarStockEnBodegaOrigen(producto.getIdProducto(), producto.getIdVariante(), producto.getTipo(),
                        nuevaCantidad)) {
                    return false;
                }
                existente.setCantidadSolicitada(nuevaCantidad);
                System.out.println("SUCCESS  Cantidad actualizada a: " + nuevaCantidad);
                return true;
            }
            return false;
        }

        // 3. Agregar nuevo
        traspasoActual.agregarProducto(producto);
        System.out.println("SUCCESS  Producto agregado: " + producto.getNombreCompleto());
        return true;
    }

    public ProductoTraspasoItem buscarProductoExistente(ProductoTraspasoItem producto) {
        for (ProductoTraspasoItem existente : traspasoActual.getProductos()) {
            if (sonElMismoProducto(existente, producto)) {
                return existente;
            }
        }
        return null;
    }

    // Nuevo método UI-agnostic para usar desde SwingWorker
    public void agregarProductoDirecto(ProductoTraspasoItem producto) {
        traspasoActual.agregarProducto(producto);
    }

    public void actualizarCantidadProducto(ProductoTraspasoItem existente, int nuevaCantidad) {
        existente.setCantidadSolicitada(nuevaCantidad);
    }

    private boolean sonElMismoProducto(ProductoTraspasoItem p1, ProductoTraspasoItem p2) {
        return p1.getIdProducto().equals(p2.getIdProducto())
                && Objects.equals(p1.getIdVariante(), p2.getIdVariante())
                && p1.getTipo().equals(p2.getTipo());
    }

    public boolean removerProducto(int index) {
        if (index < 0 || index >= traspasoActual.getProductos().size()) {
            System.err.println("ERROR  Índice inválido para remover producto: " + index);
            return false;
        }

        ProductoTraspasoItem producto = traspasoActual.getProductos().get(index);
        traspasoActual.removerProducto(index);
        System.out.println("Eliminar Producto removido: " + producto.getNombreCompleto());
        return true;
    }

    public int getCantidadTotalProductos() {
        return traspasoActual.getProductos().size();
    }

    public List<ProductoTraspasoItem> getProductosTraspaso() {
        return traspasoActual.getProductos();
    }

    // ================================
    // MÉTODOS DEL PASO 3 - CONFIRMACIÓN (CORREGIDO)
    // ================================
    public boolean validarTraspasoCompleto() {
        System.out.println("Buscar Validando traspaso completo...");

        if (!traspasoActual.isValid()) {
            System.out.println("ERROR  Datos básicos incompletos");
            return false;
        }

        if (traspasoActual.getProductos().isEmpty()) {
            System.out.println("ERROR  Sin productos agregados");
            return false;
        }

        for (ProductoTraspasoItem producto : traspasoActual.getProductos()) {
            if (producto.getCantidadSolicitada() <= 0) {
                System.out.println("ERROR  Producto con cantidad inválida: " + producto.getNombreCompleto());
                return false;
            }
        }

        System.out.println("SUCCESS  Traspaso válido para confirmación");
        return true;
    }

    // MÉTODO CORREGIDO - Ahora genera resumen con datos reales y formato mejorado
    public String generarResumenTraspaso() {
        StringBuilder resumen = new StringBuilder();

        resumen.append("═══════════════════════════════════════\n");
        resumen.append("           RESUMEN DEL TRASPASO\n");
        resumen.append("═══════════════════════════════════════\n\n");

        // Datos básicos
        resumen.append(" INFORMACIÓN GENERAL\n");
        resumen.append("────────────────────────────────────────\n");
        resumen.append("Número: ").append(traspasoActual.getNumeroTraspaso() != null
                ? traspasoActual.getNumeroTraspaso()
                : "No asignado").append("\n");
        resumen.append("Fecha: ").append(java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
        resumen.append("Tipo: ").append(traspasoActual.getTipoTraspaso() != null
                ? traspasoActual.getTipoTraspaso()
                : "No especificado").append("\n");
        resumen.append("Estado: Pendiente\n\n");

        // Bodegas
        resumen.append(" BODEGAS\n");
        resumen.append("────────────────────────────────────────\n");
        resumen.append("Origen: ").append(traspasoActual.getNombreBodegaOrigen() != null
                ? traspasoActual.getNombreBodegaOrigen()
                : "No seleccionada").append("\n");
        resumen.append("Destino: ").append(traspasoActual.getNombreBodegaDestino() != null
                ? traspasoActual.getNombreBodegaDestino()
                : "No seleccionada").append("\n\n");

        // Motivo
        resumen.append("Nota MOTIVO\n");
        resumen.append("────────────────────────────────────────\n");
        resumen.append(traspasoActual.getMotivoTraspaso() != null
                ? traspasoActual.getMotivoTraspaso()
                : "No especificado").append("\n\n");

        // Productos
        resumen.append("Caja PRODUCTOS (").append(traspasoActual.getProductos().size()).append(")\n");
        resumen.append("────────────────────────────────────────\n");

        int totalCajas = 0;
        int totalPares = 0;

        for (int i = 0; i < traspasoActual.getProductos().size(); i++) {
            ProductoTraspasoItem producto = traspasoActual.getProductos().get(i);
            resumen.append(String.format("%2d. %s\n", i + 1, producto.getNombreCompleto()));
            resumen.append(String.format("    %s: %d %s\n",
                    producto.getTipo().toUpperCase(),
                    producto.getCantidadSolicitada(),
                    producto.getTipo()));

            if ("caja".equals(producto.getTipo())) {
                totalCajas += producto.getCantidadSolicitada();
            } else {
                totalPares += producto.getCantidadSolicitada();
            }

            if (i < traspasoActual.getProductos().size() - 1) {
                resumen.append("\n");
            }
        }

        // Totales
        resumen.append("\nResumen TOTALES\n");
        resumen.append("────────────────────────────────────────\n");
        resumen.append("Total productos: ").append(traspasoActual.getProductos().size()).append("\n");
        if (totalCajas > 0) {
            resumen.append("Total cajas: ").append(totalCajas).append("\n");
        }
        if (totalPares > 0) {
            resumen.append("Total pares: ").append(totalPares).append("\n");
        }

        // Observaciones
        if (traspasoActual.getObservaciones() != null
                && !traspasoActual.getObservaciones().trim().isEmpty()) {
            resumen.append("\n OBSERVACIONES\n");
            resumen.append("────────────────────────────────────────\n");
            resumen.append(traspasoActual.getObservaciones()).append("\n");
        }

        resumen.append("\n═══════════════════════════════════════\n");

        return resumen.toString();
    }

    public boolean confirmarYGuardarTraspaso() {
        try {
            System.out.println(" Confirmando y guardando traspaso...");
            System.out.println("   Modo: " + (modoEdicion ? "EDICIÓN" : "CREACIÓN"));

            // Asegurar que el usuario solicitante sea el de sesión activa
            int sid = (session != null && session.getCurrentUser() != null) 
                    ? session.getCurrentUser().getIdUsuario() 
                    : -1;
            if (sid > 0) {
                this.idUsuarioActual = sid;
                if (traspasoActual != null) {
                    traspasoActual.setIdUsuarioSolicita(sid);
                }
                System.out.println("   Usuario solicitante (sesión): " + sid);
            } else {
                System.out.println("   WARNING  Sesión no activa; usando idUsuarioActual=" + this.idUsuarioActual);
            }

            if (!validarTraspasoCompleto()) {
                return false;
            }

            if (modoEdicion) {
                // MODO EDICIÓN: Actualizar traspaso existente
                return ejecutarActualizacion();
            } else {
                // MODO CREACIÓN: Crear nuevo traspaso
                return ejecutarCreacion();
            }

        } catch (Exception e) {
            System.err.println("ERROR  Error inesperado guardando traspaso: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Error inesperado al guardar traspaso:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private boolean ejecutarCreacion() {
        try {
            System.out.println(" Ejecutando creación de nuevo traspaso...");

            int idTraspaso = traspasoService.guardarTraspaso(traspasoActual, idUsuarioActual);

            if (idTraspaso > 0) {
                traspasoActual.setConfirmado(true);

                System.out.println("SUCCESS  Traspaso creado exitosamente");
                System.out.println("   - ID: " + idTraspaso);
                System.out.println("   - Número: " + traspasoActual.getNumeroTraspaso());
                System.out.println("   - Productos: " + traspasoActual.getProductos().size());

                // Notificación push visible a bodega ORIGEN (la que debe enviar)
                try {
                    Frame[] frames = Frame.getFrames();
                    Frame frame = frames.length > 0 ? frames[0] : null;

                    if (frame != null) {
                        String bodegaOrigen = traspasoActual.getNombreBodegaOrigen();
                        String bodegaDestino = traspasoActual.getNombreBodegaDestino();
                        int cantidadProductos = traspasoActual.getProductos().size();

                        CorporateTone.playAlert();
                        new Notification(
                                frame,
                                Notification.Type.WARNING,
                                Notification.Location.TOP_RIGHT,
                                " Nueva Solicitud de Traspaso",
                                String.format("La bodega '%s' solicita %d producto(s) a '%s'\n" +
                                        "Traspaso: %s",
                                        bodegaDestino,
                                        cantidadProductos,
                                        bodegaOrigen,
                                        traspasoActual.getNumeroTraspaso()),
                                FontAwesomeSolid.EXCHANGE_ALT,
                                3000).showNotification();
                    }
                } catch (Exception ex) {
                    System.err.println("Error mostrando notificación visual de solicitud: " + ex.getMessage());
                }

                // CORRECCIÓN: Comentado para permitir cierre automático del modal
                // El usuario ya ve la notificación de tipo Toast y el mensaje en paso3
                // Este JOptionPane bloqueaba el cierre automático del modal
                /*
                 * JOptionPane.showMessageDialog(null,
                 * "¡Traspaso creado exitosamente!\n\n"
                 * + "Número: " + traspasoActual.getNumeroTraspaso() + "\n"
                 * + "ID: " + idTraspaso + "\n"
                 * + "Productos: " + traspasoActual.getProductos().size(),
                 * "Éxito", JOptionPane.INFORMATION_MESSAGE);
                 */

                return true;
            } else {
                System.err.println("ERROR  Error: No se obtuvo ID del traspaso guardado");
                JOptionPane.showMessageDialog(null,
                        "Error al guardar el traspaso en la base de datos",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }

        } catch (SQLException e) {
            System.err.println("ERROR  Error SQL creando traspaso: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Error de base de datos al crear traspaso:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private boolean ejecutarActualizacion() {
        try {
            System.out.println("Actualizando Ejecutando actualización de traspaso existente...");

            if (numeroTraspasoOriginal == null) {
                System.err.println("ERROR  Error: No se encontró número original para actualización");
                return false;
            }

            boolean resultado = traspasoService.actualizarTraspaso(traspasoActual, numeroTraspasoOriginal);

            if (resultado) {
                traspasoActual.setConfirmado(true);

                System.out.println("SUCCESS  Traspaso actualizado exitosamente");
                System.out.println("   - Número: " + numeroTraspasoOriginal);
                System.out.println("   - Productos: " + traspasoActual.getProductos().size());

                // CORRECCIÓN: Comentado para permitir cierre automático del modal
                // El usuario ya ve el mensaje de confirmación en paso3
                /*
                 * JOptionPane.showMessageDialog(null,
                 * "¡Traspaso actualizado exitosamente!\n\n"
                 * + "Número: " + numeroTraspasoOriginal + "\n"
                 * + "Productos: " + traspasoActual.getProductos().size(),
                 * "Éxito", JOptionPane.INFORMATION_MESSAGE);
                 */

                return true;
            } else {
                System.err.println("ERROR  Error: No se pudo actualizar el traspaso");
                JOptionPane.showMessageDialog(null,
                        "Error al actualizar el traspaso en la base de datos",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }

        } catch (SQLException e) {
            System.err.println("ERROR  Error SQL actualizando traspaso: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Error de base de datos al actualizar traspaso:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    // ================================
    // MÉTODOS DE VALIDACIÓN DE ESTADO (CORREGIDOS)
    // ================================
    public boolean tieneDatosBasicos() {
        boolean tiene = traspasoActual.getNumeroTraspaso() != null
                && !traspasoActual.getNumeroTraspaso().trim().isEmpty()
                && traspasoActual.getIdBodegaOrigen() != null
                && traspasoActual.getIdBodegaDestino() != null
                && traspasoActual.getTipoTraspaso() != null
                && !traspasoActual.getTipoTraspaso().trim().isEmpty()
                && !"Seleccionar".equals(traspasoActual.getTipoTraspaso());
        // && traspasoActual.getMotivoTraspaso() != null
        // && !traspasoActual.getMotivoTraspaso().trim().isEmpty()
        // && traspasoActual.getMotivoTraspaso().length() >= 10;

        System.out
                .println("Buscar Verificación datos básicos: " + (tiene ? "SUCCESS  COMPLETOS" : "ERROR  INCOMPLETOS"));
        return tiene;
    }

    public boolean tieneProductos() {
        boolean tiene = traspasoActual.getProductos() != null && !traspasoActual.getProductos().isEmpty();
        System.out.println("Buscar Verificación productos: "
                + (tiene ? "SUCCESS  TIENE (" + traspasoActual.getProductos().size() + ")" : "ERROR  SIN PRODUCTOS"));
        return tiene;
    }

    // ================================
    // MÉTODOS ADICIONALES
    // ================================
    public boolean cambiarEstadoTraspaso(String numeroTraspaso, String nuevoEstado) {
        try {
            boolean resultado = traspasoService.cambiarEstadoTraspaso(
                    numeroTraspaso, nuevoEstado, idUsuarioActual);

            if (resultado) {
                JOptionPane.showMessageDialog(null,
                        "Estado del traspaso " + numeroTraspaso + " cambiado a: " + nuevoEstado,
                        "Éxito", JOptionPane.INFORMATION_MESSAGE);
            }

            return resultado;
        } catch (SQLException e) {
            System.err.println("ERROR  Error cambiando estado: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<Object[]> obtenerTraspasos(String estado, Bodega bodegaOrigen,
            Bodega bodegaDestino, String fechaDesde,
            String fechaHasta) {
        try {
            Integer idBodegaOrigen = (bodegaOrigen != null) ? bodegaOrigen.getIdBodega() : null;
            Integer idBodegaDestino = (bodegaDestino != null) ? bodegaDestino.getIdBodega() : null;

            return traspasoService.obtenerTraspasos(
                    estado, idBodegaOrigen, idBodegaDestino, fechaDesde, fechaHasta);
        } catch (SQLException e) {
            System.err.println("ERROR  Error obteniendo traspasos: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // DEBUG
    public void debug() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println(" DEBUG - ESTADO DEL TRASPASO");
        System.out.println("=".repeat(60));
        System.out.println("Número: " + traspasoActual.getNumeroTraspaso());
        System.out.println("Bodega origen: " + traspasoActual.getNombreBodegaOrigen()
                + " (ID: " + traspasoActual.getIdBodegaOrigen() + ")");
        System.out.println("Bodega destino: " + traspasoActual.getNombreBodegaDestino()
                + " (ID: " + traspasoActual.getIdBodegaDestino() + ")");
        System.out.println("Tipo: " + traspasoActual.getTipoTraspaso());
        System.out.println("Motivo: " + traspasoActual.getMotivoTraspaso());
        System.out.println("Productos: " + traspasoActual.getProductos().size());

        System.out.println("\nCaja PRODUCTOS:");
        for (int i = 0; i < traspasoActual.getProductos().size(); i++) {
            ProductoTraspasoItem p = traspasoActual.getProductos().get(i);
            System.out.println("  " + (i + 1) + ". " + p.getNombreCompleto()
                    + " (ID: " + p.getIdProducto() + ", Variante: " + p.getIdVariante()
                    + ", Cantidad: " + p.getCantidadSolicitada() + " " + p.getTipo() + ")");
        }
        System.out.println("=".repeat(60) + "\n");
    }

    public void setTraspasoActual(TraspasoDatos traspaso) {
        if (traspaso != null) {
            this.traspasoActual = traspaso;
            System.out.println("SUCCESS  TraspasoActual establecido:");
            System.out.println("   - Número: " + traspaso.getNumeroTraspaso());
            System.out.println("   - Origen: " + traspaso.getNombreBodegaOrigen());
            System.out.println("   - Destino: " + traspaso.getNombreBodegaDestino());
            System.out.println("   - Productos: " + traspaso.getProductos().size());
        } else {
            System.err.println("WARNING  Intento de establecer traspaso nulo");
            this.traspasoActual = new TraspasoDatos();
        }
    }

    public boolean cargarTraspasoExistente(String numeroTraspaso) {
        try {
            System.out.println("Actualizando Cargando traspaso existente: " + numeroTraspaso);

            // CONFIGURAR MODO EDICIÓN INMEDIATAMENTE
            configurarModoEdicion(numeroTraspaso);

            // Obtener información básica del traspaso
            Map<String, Object> traspasoInfo = obtenerInformacionTraspaso(numeroTraspaso);
            if (traspasoInfo == null) {
                System.err.println("ERROR  No se encontró el traspaso: " + numeroTraspaso);
                return false;
            }

            // Obtener detalles de productos
            List<Map<String, Object>> detalles = obtenerDetallesTraspaso(numeroTraspaso);

            // Crear TraspasoDatos desde la información obtenida
            TraspasoDatos traspaso = crearTraspasoDatosDesdeInfo(traspasoInfo, detalles);

            if (traspaso != null) {
                this.traspasoActual = traspaso;
                System.out.println("SUCCESS  Traspaso cargado exitosamente en controller (MODO EDICIÓN)");
                return true;
            }

            return false;

        } catch (Exception e) {
            System.err.println("ERROR  Error cargando traspaso existente: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private Map<String, Object> obtenerInformacionTraspaso(String numeroTraspaso) throws SQLException {
        String sql = "SELECT t.*, "
                + "bo.nombre as bodega_origen, bd.nombre as bodega_destino, "
                + "us.nombre as usuario_solicita, ua.nombre as usuario_autoriza, "
                + "ur.nombre as usuario_recibe "
                + "FROM traspasos t "
                + "INNER JOIN bodegas bo ON t.id_bodega_origen = bo.id_bodega "
                + "INNER JOIN bodegas bd ON t.id_bodega_destino = bd.id_bodega "
                + "INNER JOIN usuarios us ON t.id_usuario_solicita = us.id_usuario "
                + "LEFT JOIN usuarios ua ON t.id_usuario_autoriza = ua.id_usuario "
                + "LEFT JOIN usuarios ur ON t.id_usuario_recibe = ur.id_usuario "
                + "WHERE t.numero_traspaso = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, numeroTraspaso);
            rs = stmt.executeQuery();

            if (rs.next()) {
                Map<String, Object> traspasoInfo = new HashMap<>();
                traspasoInfo.put("id_traspaso", rs.getInt("id_traspaso"));
                traspasoInfo.put("numero_traspaso", rs.getString("numero_traspaso"));
                traspasoInfo.put("id_bodega_origen", rs.getInt("id_bodega_origen"));
                traspasoInfo.put("id_bodega_destino", rs.getInt("id_bodega_destino"));
                traspasoInfo.put("id_usuario_solicita", rs.getInt("id_usuario_solicita"));
                traspasoInfo.put("fecha_solicitud", rs.getTimestamp("fecha_solicitud"));
                traspasoInfo.put("fecha_autorizacion", rs.getTimestamp("fecha_autorizacion"));
                traspasoInfo.put("fecha_envio", rs.getTimestamp("fecha_envio"));
                traspasoInfo.put("fecha_recepcion", rs.getTimestamp("fecha_recepcion"));
                traspasoInfo.put("estado", rs.getString("estado"));
                traspasoInfo.put("motivo", rs.getString("motivo"));
                traspasoInfo.put("observaciones", rs.getString("observaciones"));
                traspasoInfo.put("total_productos", rs.getInt("total_productos"));
                traspasoInfo.put("bodega_origen", rs.getString("bodega_origen"));
                traspasoInfo.put("bodega_destino", rs.getString("bodega_destino"));
                traspasoInfo.put("usuario_solicita", rs.getString("usuario_solicita"));
                traspasoInfo.put("usuario_autoriza", rs.getString("usuario_autoriza"));
                traspasoInfo.put("usuario_recibe", rs.getString("usuario_recibe"));
                return traspasoInfo;
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }

        return null;
    }

    /**
     * MÉTODO NUEVO - Obtener detalles de productos del traspaso
     */
    private List<Map<String, Object>> obtenerDetallesTraspaso(String numeroTraspaso) throws SQLException {
        String sql = "SELECT td.*, p.nombre as producto_nombre, p.codigo_modelo, "
                + "c.nombre as color_nombre, t.numero as talla_numero, "
                + "pv.sku, pv.ean "
                + "FROM traspaso_detalles td "
                + "INNER JOIN traspasos tr ON td.id_traspaso = tr.id_traspaso "
                + "INNER JOIN productos p ON td.id_producto = p.id_producto "
                + "LEFT JOIN producto_variantes pv ON td.id_variante = pv.id_variante "
                + "LEFT JOIN colores c ON pv.id_color = c.id_color "
                + "LEFT JOIN tallas t ON pv.id_talla = t.id_talla "
                + "WHERE tr.numero_traspaso = ? "
                + "ORDER BY p.nombre, c.nombre, t.numero";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Map<String, Object>> detalles = new ArrayList<>();

        try {
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, numeroTraspaso);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> detalle = new HashMap<>();
                detalle.put("id_detalle_traspaso", rs.getInt("id_detalle_traspaso"));
                detalle.put("id_producto", rs.getInt("id_producto"));
                detalle.put("id_variante", rs.getObject("id_variante")); // Puede ser null
                detalle.put("producto_nombre", rs.getString("producto_nombre"));
                detalle.put("codigo_modelo", rs.getString("codigo_modelo"));
                detalle.put("sku", rs.getString("sku"));
                detalle.put("ean", rs.getString("ean"));
                detalle.put("color_nombre", rs.getString("color_nombre"));
                detalle.put("talla_numero", rs.getString("talla_numero"));
                detalle.put("cantidad_solicitada", rs.getInt("cantidad_solicitada"));
                detalle.put("cantidad_enviada", rs.getInt("cantidad_enviada"));
                detalle.put("cantidad_recibida", rs.getInt("cantidad_recibida"));
                detalle.put("estado_detalle", rs.getString("estado_detalle"));
                detalle.put("observaciones", rs.getString("observaciones"));
                // CORRECCIÓN: Leer el campo Tipo
                detalle.put("tipo", rs.getString("Tipo"));

                detalles.add(detalle);
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }

        return detalles;
    }

    /**
     * MÉTODO NUEVO - Crear TraspasoDatos desde información de BD
     */
    private TraspasoDatos crearTraspasoDatosDesdeInfo(Map<String, Object> traspasoInfo,
            List<Map<String, Object>> detalles) {
        try {
            TraspasoDatos traspaso = new TraspasoDatos();

            // Datos básicos del traspaso
            traspaso.setNumeroTraspaso(traspasoInfo.get("numero_traspaso").toString());
            traspaso.setIdBodegaOrigen((Integer) traspasoInfo.get("id_bodega_origen"));
            traspaso.setNombreBodegaOrigen(traspasoInfo.get("bodega_origen").toString());
            traspaso.setIdBodegaDestino((Integer) traspasoInfo.get("id_bodega_destino"));
            traspaso.setNombreBodegaDestino(traspasoInfo.get("bodega_destino").toString());
            traspaso.setEstado(traspasoInfo.get("estado").toString());
            traspaso.setIdUsuarioSolicita((Integer) traspasoInfo.get("id_usuario_solicita"));

            // Motivo y observaciones
            if (traspasoInfo.get("motivo") != null) {
                traspaso.setMotivoTraspaso(traspasoInfo.get("motivo").toString());
            }

            if (traspasoInfo.get("observaciones") != null) {
                traspaso.setObservaciones(traspasoInfo.get("observaciones").toString());
            }

            // Tipo de traspaso (derivar desde motivo o usar por defecto)
            traspaso.setTipoTraspaso(determinarTipoTraspaso(traspaso.getMotivoTraspaso()));

            // Convertir detalles a ProductoTraspasoItem
            for (Map<String, Object> detalle : detalles) {
                ProductoTraspasoItem producto = crearProductoDesdeDetalle(detalle);
                if (producto != null) {
                    traspaso.agregarProducto(producto);
                }
            }

            System.out.println("SUCCESS  TraspasoDatos creado desde BD:");
            System.out.println("   - Número: " + traspaso.getNumeroTraspaso());
            System.out.println("   - Productos: " + traspaso.getProductos().size());

            return traspaso;

        } catch (Exception e) {
            System.err.println("ERROR  Error creando TraspasoDatos: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * MÉTODO AUXILIAR - Crear ProductoTraspasoItem desde detalle de BD
     */
    private ProductoTraspasoItem crearProductoDesdeDetalle(Map<String, Object> detalle) {
        try {
            ProductoTraspasoItem producto = new ProductoTraspasoItem();

            producto.setIdProducto((Integer) detalle.get("id_producto"));

            Object idVariante = detalle.get("id_variante");
            if (idVariante != null) {
                producto.setIdVariante((Integer) idVariante);
            }

            producto.setNombreProducto(detalle.get("producto_nombre").toString());
            producto.setCodigoProducto(detalle.get("codigo_modelo").toString());

            Object colorNombre = detalle.get("color_nombre");
            if (colorNombre != null) {
                producto.setColor(colorNombre.toString());
            }

            Object tallaNombre = detalle.get("talla_numero");
            if (tallaNombre != null) {
                producto.setTalla(tallaNombre.toString());
            }

            producto.setCantidadSolicitada((Integer) detalle.get("cantidad_solicitada"));

            // Determinar tipo basándose en la cantidad o usar "par" por defecto
            // CORRECCIÓN: Usar el tipo guardado en BD
            Object tipoObj = detalle.get("tipo");
            if (tipoObj != null && !tipoObj.toString().isEmpty()) {
                producto.setTipo(tipoObj.toString());
            } else {
                producto.setTipo("par");
            }

            // Obtener precio del producto (consulta adicional si es necesario)
            java.math.BigDecimal precio = obtenerPrecioProducto(producto.getIdProducto(), producto.getIdVariante());
            producto.setPrecioUnitario(precio);

            Object observacionesProducto = detalle.get("observaciones");
            if (observacionesProducto != null) {
                producto.setObservaciones(observacionesProducto.toString());
            }

            return producto;

        } catch (Exception e) {
            System.err.println("ERROR  Error creando producto desde detalle: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * MÉTODO AUXILIAR - Determinar tipo de traspaso desde motivo
     */
    private String determinarTipoTraspaso(String motivo) {
        if (motivo == null) {
            return "Reposición de stock";
        }

        String motivoLower = motivo.toLowerCase();

        if (motivoLower.contains("urgente")) {
            return "Urgente";
        }
        if (motivoLower.contains("reorganiz")) {
            return "Reorganización";
        }
        if (motivoLower.contains("temporada")) {
            return "Temporada";
        }
        if (motivoLower.contains("liquidac")) {
            return "Liquidación";
        }

        return "Reposición de stock"; // Por defecto
    }

    /**
     * MÉTODO AUXILIAR - Obtener precio del producto
     */
    private java.math.BigDecimal obtenerPrecioProducto(Integer idProducto, Integer idVariante) {
        String sql = "SELECT precio_venta FROM productos WHERE id_producto = ?";

        if (idVariante != null) {
            sql = "SELECT COALESCE(pv.precio_venta, p.precio_venta) as precio_venta "
                    + "FROM productos p "
                    + "LEFT JOIN producto_variantes pv ON p.id_producto = pv.id_producto "
                    + "WHERE p.id_producto = ? AND pv.id_variante = ?";
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = conexion.getInstance().createConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, idProducto);

            if (idVariante != null) {
                stmt.setInt(2, idVariante);
            }

            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("precio_venta");
            }

        } catch (SQLException e) {
            System.err.println("Error obteniendo precio: " + e.getMessage());
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return java.math.BigDecimal.ZERO;
    }
}
