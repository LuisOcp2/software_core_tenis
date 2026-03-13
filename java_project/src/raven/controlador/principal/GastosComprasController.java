package raven.controlador.principal;

import raven.clases.admin.UserSession;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import raven.clases.admin.ServiceCajaMovimiento;
import raven.clases.principal.ServiceCompraExterna;
import raven.clases.principal.ServiceGastoOperativo;

/**
 * Controlador para el formulario de Gastos y Compras Externas.
 *
 * Implementa patrón MVC/MVP: - Recibe eventos de la vista - Coordina con los
 * servicios - Actualiza la vista mediante callbacks
 *
 * Principios aplicados: - Single Responsibility: coordina flujo entre vista y
 * servicios - Dependency Injection: recibe servicios por constructor - Observer
 * Pattern: notifica cambios mediante callbacks
 *
 * @author CrisDEV
 * @version 1.0
 */
public class GastosComprasController {

    // ═══════════════════════════════════════════════════════════════════════════
    // DEPENDENCIAS
    // ═══════════════════════════════════════════════════════════════════════════
    private final ServiceGastoOperativo serviceGasto;
    private final ServiceCompraExterna serviceCompra;
    private final ServiceCajaMovimiento serviceCaja;

    // ═══════════════════════════════════════════════════════════════════════════
    // ESTADO DEL CONTROLADOR
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * Lista de gastos del movimiento actual
     */
    private List<ModelGastoOperativo> gastosActuales;

    /**
     * Lista temporal de detalles de compra externa
     */
    private List<ModelCompraExternaDetalle> detallesCompraActual;
    private Consumer<Integer> onCompraRegistrada;
    /**
     * Catálogos cargados - AHORA CON CACHÉ ESTÁTICA para mejor rendimiento
     */
    private List<ModelTipoGasto> tiposGasto;
    private List<ModelTalla> tallas;
    private List<ModelColor> colores;

    // ═══════════════════════════════════════════════════════════════════════════
    // CACHÉ ESTÁTICA DE CATÁLOGOS - Mejora de rendimiento
    // ═══════════════════════════════════════════════════════════════════════════
    private static List<ModelTipoGasto> cacheTiposGasto = null;
    private static List<ModelTalla> cacheTallas = null;
    private static List<ModelColor> cacheColores = null;
    private static long cacheTimestamp = 0;
    private static final long CACHE_DURATION_MS = 5 * 60 * 1000; // 5 minutos de caché

    /**
     * Datos de sesión
     */
    private Integer idMovimientoCaja;
    private Integer idBodega;
    private Integer idUsuario;
    private Integer idCaja;

    // ═══════════════════════════════════════════════════════════════════════════
    // CALLBACKS PARA LA VISTA
    // ═══════════════════════════════════════════════════════════════════════════
    private Consumer<String> onError;
    private Consumer<String> onSuccess;
    private Consumer<List<ModelGastoOperativo>> onGastosUpdated;
    private Consumer<BigDecimal> onTotalGastosUpdated;
    private Consumer<List<ModelCompraExternaDetalle>> onDetallesCompraUpdated;
    private Consumer<BigDecimal> onTotalCompraUpdated;
    private Runnable onFormCleared;

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════
    public GastosComprasController() {
        this.serviceGasto = new ServiceGastoOperativo();
        this.serviceCompra = new ServiceCompraExterna();
        this.serviceCaja = new ServiceCajaMovimiento();
        this.gastosActuales = new ArrayList<>();
        this.detallesCompraActual = new ArrayList<>();
    }

    /**
     * Constructor con inyección de dependencias.
     */
    public GastosComprasController(ServiceGastoOperativo serviceGasto,
            ServiceCompraExterna serviceCompra) {
        this.serviceGasto = serviceGasto;
        this.serviceCompra = serviceCompra;
        this.serviceCaja = new ServiceCajaMovimiento();
        this.gastosActuales = new ArrayList<>();
        this.detallesCompraActual = new ArrayList<>();
    }

    public void setOnCompraRegistrada(Consumer<Integer> cb) {
        this.onCompraRegistrada = cb;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INICIALIZACIÓN
    // ═══════════════════════════════════════════════════════════════════════════
    public void inicializar() {
        inicializar(null);
    }

    public void inicializar(Integer idMovimientoOverride) {
        try {
            if (!inicializarSesionYCaja()) {
                return;
            }

            if (idMovimientoOverride != null) {
                this.idMovimientoCaja = idMovimientoOverride;
            }

            cargarCatalogos();
            cargarGastosMovimiento();

            System.out.println("SUCCESS  Controller inicializado");
            System.out.println("   Usuario: " + idUsuario);
            System.out.println("   Bodega: " + idBodega);
            System.out.println("   Caja: " + idCaja);
            System.out.println("   Movimiento: " + idMovimientoCaja);

        } catch (SQLException e) {
            notificarError("Error al inicializar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean inicializarSesionYCaja() throws SQLException {
        raven.clases.admin.UserSession session = raven.clases.admin.UserSession.getInstance();
        if (session.isLoggedIn()) {
            this.idUsuario = session.getCurrentUser() != null ? session.getCurrentUser().getIdUsuario() : null;
            this.idBodega = session.getIdBodegaUsuario();
            this.idCaja = session.getIdCajaActiva();
            this.idMovimientoCaja = session.getIdMovimientoActual();

            if (this.idCaja == null || this.idUsuario == null) {
                notificarError("Sesión inválida: usuario o caja no definidos");
                return false;
            }

            if (this.idMovimientoCaja == null) {
                raven.clases.admin.ServiceCajaMovimiento svcMov = new raven.clases.admin.ServiceCajaMovimiento();
                raven.controlador.admin.ModelCajaMovimiento mov = svcMov.obtenerMovimientoAbierto(this.idCaja,
                        this.idUsuario);
                if (mov != null) {
                    this.idMovimientoCaja = mov.getIdMovimiento();
                } else {
                    raven.clases.admin.ServiceCajaMovimiento svc = new raven.clases.admin.ServiceCajaMovimiento();
                    java.math.BigDecimal montoInicial = java.math.BigDecimal.ZERO;
                    raven.controlador.admin.ModelCajaMovimiento nuevo = svc.abrirCaja(this.idCaja, this.idUsuario,
                            montoInicial, "Apertura automática");
                    this.idMovimientoCaja = nuevo.getIdMovimiento();
                }
            }
            return true;
        }

        raven.controlador.admin.SessionManager sm = raven.controlador.admin.SessionManager.getInstance();
        if (!sm.isSessionActive()) {
            int userId = obtenerUsuarioActivoPorDefecto();
            if (userId <= 0) {
                notificarError("No hay usuarios activos disponibles");
                return false;
            }
            sm.loginForDevelopment(userId);
        }

        raven.controlador.admin.ModelUser u = sm.getCurrentUser();
        if (u == null) {
            notificarError("No se pudo inicializar usuario de desarrollo");
            return false;
        }

        this.idUsuario = u.getIdUsuario();
        this.idBodega = u.getIdBodega();

        raven.clases.admin.ServiceCaja serviceCaja = new raven.clases.admin.ServiceCaja();
        java.util.List<raven.controlador.admin.ModelCaja> cajas = serviceCaja.getAll();
        if (cajas == null || cajas.isEmpty()) {
            notificarError("No hay cajas activas disponibles");
            return false;
        }
        this.idCaja = cajas.get(0).getIdCaja();

        raven.clases.admin.ServiceCajaMovimiento svcMov = new raven.clases.admin.ServiceCajaMovimiento();
        raven.controlador.admin.ModelCajaMovimiento mov = svcMov.obtenerMovimientoAbierto(this.idCaja, this.idUsuario);
        if (mov != null) {
            this.idMovimientoCaja = mov.getIdMovimiento();
        } else {
            raven.controlador.admin.ModelCajaMovimiento nuevo = svcMov.abrirCaja(this.idCaja, this.idUsuario,
                    java.math.BigDecimal.ZERO, "Apertura automática");
            this.idMovimientoCaja = nuevo.getIdMovimiento();
        }

        return true;
    }

    private int obtenerUsuarioActivoPorDefecto() {
        java.sql.Connection con = null;
        java.sql.PreparedStatement stmt = null;
        java.sql.ResultSet rs = null;
        try {
            con = raven.controlador.principal.conexion.getInstance().createConnection();
            stmt = con.prepareStatement("SELECT id_usuario FROM usuarios WHERE activo = 1 ORDER BY id_usuario LIMIT 1");
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (java.sql.SQLException e) {
            return 0;
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (Exception ignore) {
            }
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Exception ignore) {
            }
            try {
                if (con != null)
                    con.close();
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * Carga los catálogos necesarios.
     * SUCCESS  OPTIMIZADO: Usa caché estática para evitar consultas repetidas a la BD.
     * Los catálogos se cargan solo la primera vez o cuando expira la caché.
     */
    private void cargarCatalogos() throws SQLException {
        long ahora = System.currentTimeMillis();
        boolean cacheValida = (ahora - cacheTimestamp) < CACHE_DURATION_MS;

        if (cacheTiposGasto != null && cacheTallas != null && cacheColores != null && cacheValida) {
            // Usar caché existente
            this.tiposGasto = cacheTiposGasto;
            this.tallas = cacheTallas;
            this.colores = cacheColores;
            System.out.println(" Catálogos cargados desde CACHÉ ( rápido)");
        } else {
            // Cargar de BD y actualizar caché
            long startTime = System.currentTimeMillis();

            cacheTiposGasto = serviceGasto.listarTiposGasto();
            cacheTallas = serviceCompra.listarTallas();
            cacheColores = serviceCompra.listarColores();
            cacheTimestamp = ahora;

            this.tiposGasto = cacheTiposGasto;
            this.tallas = cacheTallas;
            this.colores = cacheColores;

            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println(" Catálogos cargados desde BD en " + elapsed + "ms:");
            System.out.println("   - Tipos de gasto: " + tiposGasto.size());
            System.out.println("   - Tallas: " + tallas.size());
            System.out.println("   - Colores: " + colores.size());
        }
    }

    /**
     * Invalida la caché de catálogos para forzar recarga en la próxima apertura.
     * Útil cuando se agregan nuevos tipos de gasto, tallas o colores.
     */
    public static void invalidarCacheCatalogos() {
        cacheTiposGasto = null;
        cacheTallas = null;
        cacheColores = null;
        cacheTimestamp = 0;
        System.out.println("Actualizando Caché de catálogos invalidada");
    }

    /**
     * Carga los gastos existentes del movimiento de caja actual.
     */
    private void cargarGastosMovimiento() throws SQLException {
        if (idMovimientoCaja != null) {
            this.gastosActuales = serviceGasto.listarPorMovimiento(idMovimientoCaja);
            notificarGastosActualizados();
            actualizarTotalGastos();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // OPERACIONES DE GASTOS OPERATIVOS
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * Registra un nuevo gasto operativo.
     *
     * @param idTipoGasto   ID del tipo de gasto seleccionado
     * @param concepto      Descripción del gasto
     * @param monto         Monto del gasto
     * @param proveedor     Nombre del proveedor/tercero (opcional)
     * @param numeroRecibo  Número de recibo (opcional)
     * @param observaciones Observaciones adicionales (opcional)
     */
    public void registrarGasto(Integer idTipoGasto, String concepto, BigDecimal monto,
            String proveedor, String numeroRecibo, String observaciones) {
        try {
            // Validaciones previas
            if (idMovimientoCaja == null) {
                notificarError("No hay movimiento de caja activo");
                return;
            }

            if (idTipoGasto == null) {
                notificarError("Seleccione un tipo de gasto");
                return;
            }

            if (concepto == null || concepto.trim().isEmpty()) {
                notificarError("El concepto es requerido");
                return;
            }

            if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
                notificarError("El monto debe ser mayor a cero");
                return;
            }

            BigDecimal saldoDisponible = serviceCaja.obtenerSaldoTotalDisponible(idMovimientoCaja);
            if (monto.compareTo(saldoDisponible) > 0) {
                notificarError(String.format(
                        "WARNING  Fondos insuficientes.\n\n" +
                                "Saldo disponible: $%,.0f\n" +
                                "Monto del gasto: $%,.0f\n\n" +
                                "No se puede registrar este gasto porque supera el saldo disponible del movimiento.",
                        saldoDisponible, monto));
                return;
            }

            // Verificar si requiere autorización
            Optional<ModelTipoGasto> tipoOpt = serviceGasto.obtenerTipoGasto(idTipoGasto);
            if (tipoOpt.isPresent() && tipoOpt.get().requiereAutorizacionPara(monto)) {
                notificarError("Este gasto requiere autorización. Monto máximo sin autorización: $"
                        + tipoOpt.get().getMontoMaximoSinAutorizacion());
                return;
            }

            // Construir modelo
            ModelGastoOperativo gasto = ModelGastoOperativo.builder()
                    .tipoGasto(idTipoGasto)
                    .concepto(concepto.trim())
                    .monto(monto)
                    .proveedor(proveedor)
                    .numeroRecibo(numeroRecibo)
                    .observaciones(observaciones)
                    .bodega(idBodega)
                    .usuario(idUsuario)
                    .build();

            // Registrar
            int idGasto = serviceGasto.registrarGasto(gasto, idMovimientoCaja);

            // Recargar lista
            cargarGastosMovimiento();

            // Notificar éxito
            notificarExito("Gasto registrado correctamente. ID: " + idGasto);

            // Limpiar formulario
            if (onFormCleared != null) {
                onFormCleared.run();
            }

        } catch (SQLException e) {
            notificarError("Error al registrar gasto: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalStateException | IllegalArgumentException e) {
            notificarError(e.getMessage());
        }
    }

    /**
     * Anula un gasto existente.
     */
    public void anularGasto(int idGasto) {
        try {
            // Confirmar acción
            int respuesta = JOptionPane.showConfirmDialog(
                    null,
                    "¿Está seguro de anular este gasto?\nEsta acción no se puede deshacer.",
                    "Confirmar anulación",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (respuesta != JOptionPane.YES_OPTION) {
                return;
            }

            boolean anulado = serviceGasto.anularGasto(idGasto, idUsuario);

            if (anulado) {
                cargarGastosMovimiento();
                notificarExito("Gasto anulado correctamente");
            }

        } catch (SQLException e) {
            notificarError("Error al anular gasto: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Actualiza el total de gastos.
     */
    private void actualizarTotalGastos() throws SQLException {
        BigDecimal total = serviceGasto.obtenerTotalGastos(idMovimientoCaja);
        if (onTotalGastosUpdated != null) {
            onTotalGastosUpdated.accept(total);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // OPERACIONES DE COMPRAS EXTERNAS
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * Agrega un item a la compra externa actual.
     *
     * @param idTalla      ID de la talla seleccionada
     * @param idColor      ID del color seleccionado
     * @param descripcion  Descripción del producto
     * @param cantidad     Cantidad en pares
     * @param precioCompra Precio de compra unitario
     * @param precioVenta  Precio de venta sugerido
     */
    public void agregarItemCompra(Integer idTalla, Integer idColor, String descripcion,
            int cantidad, BigDecimal precioCompra, BigDecimal precioVenta) {
        try {
            // Validaciones
            if (idTalla == null) {
                notificarError("Seleccione una talla");
                return;
            }

            if (idColor == null) {
                notificarError("Seleccione un color");
                return;
            }

            if (cantidad <= 0) {
                notificarError("La cantidad debe ser mayor a cero");
                return;
            }

            if (precioCompra == null || precioCompra.compareTo(BigDecimal.ZERO) <= 0) {
                notificarError("El precio de compra debe ser mayor a cero");
                return;
            }

            // Obtener nombres para display
            String nombreTalla = obtenerNombreTalla(idTalla);
            String nombreColor = obtenerNombreColor(idColor);

            // Crear detalle (sin ID de variante aún, se crea al guardar)
            ModelCompraExternaDetalle detalle = ModelCompraExternaDetalle.builder()
                    .producto(1) // ID producto genérico temporal
                    .variante(0) // Se creará al guardar
                    .descripcion(descripcion != null ? descripcion : "Producto Genérico")
                    .talla(nombreTalla)
                    .color(nombreColor)
                    .cantidad(cantidad)
                    .precioUnitario(precioCompra)
                    .precioVenta(precioVenta)
                    .build();
            // Guardar IDs para la creación de variante
            detalle.setIdProducto(225); // ID producto genérico

            // Agregar a lista temporal
            detallesCompraActual.add(detalle);

            // Notificar actualización
            notificarDetallesCompraActualizados();

            System.out.println("SUCCESS  Item agregado: " + detalle.getDescripcionCompleta());

        } catch (IllegalArgumentException e) {
            notificarError(e.getMessage());
        }
    }

    /**
     * Elimina un item de la compra actual.
     */
    public void eliminarItemCompra(int index) {
        if (index >= 0 && index < detallesCompraActual.size()) {
            ModelCompraExternaDetalle eliminado = detallesCompraActual.remove(index);
            notificarDetallesCompraActualizados();
            System.out.println("Eliminar Item eliminado: " + eliminado.getDescripcionCompleta());
        }
    }

    /**
     * Registra la compra externa completa.
     *
     * @param tiendaProveedor Nombre de la tienda/proveedor
     * @param numeroFactura   Número de factura/recibo
     * @param observaciones   Observaciones adicionales
     */
    public void registrarCompraExterna(String tiendaProveedor, String numeroFactura,
            String observaciones) {
        try {
            // ═══════════════════════════════════════════════════════════════
            // 1. VALIDACIONES
            // ═══════════════════════════════════════════════════════════════
            if (idMovimientoCaja == null) {
                notificarError("No hay movimiento de caja activo");
                return;
            }

            if (idBodega == null || idBodega <= 0) {
                notificarError("Error de Sesión: No tiene una bodega asignada. Por favor, inicie sesión nuevamente.");
                return;
            }

            if (tiendaProveedor == null || tiendaProveedor.trim().isEmpty()) {
                notificarError("El proveedor/tienda es requerido");
                return;
            }

            if (detallesCompraActual.isEmpty()) {
                notificarError("Agregue al menos un producto a la compra");
                return;
            }

            // ═══════════════════════════════════════════════════════════════
            // VALIDACIÓN DE EFECTIVO DISPONIBLE PARA COMPRA
            // ═══════════════════════════════════════════════════════════════
            BigDecimal totalCompra = calcularTotalCompraActual();
            BigDecimal efectivoDisponible = serviceCaja.obtenerEfectivoDisponible(idMovimientoCaja);
            if (totalCompra.compareTo(efectivoDisponible) > 0) {
                notificarError(String.format(
                        "WARNING  Fondos insuficientes para esta compra.\n\n" +
                                "Efectivo disponible: $%,.0f\n" +
                                "Total de la compra: $%,.0f\n\n" +
                                "No se puede registrar esta compra porque supera el efectivo en caja.",
                        efectivoDisponible, totalCompra));
                return;
            }

            // ═══════════════════════════════════════════════════════════════
            // 2. CREAR VARIANTES Y REGISTRAR COMPRA (TRANSACCIÓN ÚNICA)
            // ═══════════════════════════════════════════════════════════════
            java.sql.Connection conn = raven.controlador.principal.conexion.getConnectionStatic();
            try {
                conn.setAutoCommit(false);

                // 2.1 Crear variantes para cada detalle
                for (ModelCompraExternaDetalle detalle : detallesCompraActual) {
                    int idTalla = obtenerIdTallaPorNombre(detalle.getNombreTalla());
                    int idColor = obtenerIdColorPorNombre(detalle.getNombreColor());

                    int idVariante = serviceCompra.obtenerOCrearVariante(
                            conn, idTalla, idColor,
                            detalle.getPrecioUnitario(),
                            detalle.getPrecioVenta(),
                            tiendaProveedor,
                            idUsuario,
                            detalle.getDescripcionProducto() // Descripción
                    );

                    detalle.setIdVariante(idVariante);
                }

                // 2.2 Construir modelo de compra
                ModelCompraExterna compra = new ModelCompraExterna();
                compra.setTiendaProveedor(tiendaProveedor);
                compra.setNumeroFacturaRecibo(numeroFactura);
                compra.setObservaciones(observaciones);
                compra.setIdBodega(idBodega);
                compra.setIdUsuario(idUsuario);
                compra.setDetalles(new ArrayList<>(detallesCompraActual));

                // 2.3 Registrar compra usando la MISMA conexión
                int idMovimientoCajaActual = idMovimientoCaja;
                int idCompraRegistrada = serviceCompra.registrarCompra(conn, compra, idMovimientoCajaActual);

                compra.setIdCompraExterna(idCompraRegistrada);

                conn.commit(); // <--- ÚNICO COMMIT PARA TODO EL PROCESO

                System.out.println("SUCCESS  Compra registrada con ID: " + idCompraRegistrada);

                // 2.4 Disparar callback
                if (onCompraRegistrada != null) {
                    onCompraRegistrada.accept(idCompraRegistrada);
                }

                // 2.5 Limpiar datos
                detallesCompraActual.clear();
                notificarDetallesCompraActualizados();

                if (onFormCleared != null) {
                    onFormCleared.run();
                }

                notificarExito("Compra externa registrada exitosamente");

            } catch (SQLException e) {
                if (conn != null) conn.rollback();
                throw e;
            } finally {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            }

        } catch (SQLException e) {
            notificarError("Error al registrar compra: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalStateException | IllegalArgumentException e) {
            notificarError(e.getMessage());
            e.printStackTrace();
        }
    }

    // En GastosComprasController
    public ServiceCompraExterna getService() {
        return serviceCompra;
    }

    /**
     * Limpia la compra actual.
     */
    public void limpiarCompraActual() {
        detallesCompraActual.clear();
        notificarDetallesCompraActualizados();
    }

    /**
     * Calcula el total de la compra actual.
     */
    public BigDecimal calcularTotalCompraActual() {
        return detallesCompraActual.stream()
                .map(ModelCompraExternaDetalle::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GETTERS PARA LA VISTA
    // ═══════════════════════════════════════════════════════════════════════════
    public List<ModelTipoGasto> getTiposGasto() {
        return tiposGasto != null ? tiposGasto : new ArrayList<>();
    }

    public List<ModelTalla> getTallas() {
        return tallas != null ? tallas : new ArrayList<>();
    }

    public List<ModelColor> getColores() {
        return colores != null ? colores : new ArrayList<>();
    }

    public List<ModelGastoOperativo> getGastosActuales() {
        return gastosActuales;
    }

    public List<ModelCompraExternaDetalle> getDetallesCompraActual() {
        return detallesCompraActual;
    }

    /**
     * Obtiene información del movimiento de caja actual.
     */
    public String getInfoMovimientoCaja() {
        if (idCaja == null || idMovimientoCaja == null) {
            return "Sin caja activa";
        }

        UserSession session = UserSession.getInstance();
        String usuario = session.getCurrentUser() != null
                ? session.getCurrentUser().getUsername()
                : "Usuario";

        return String.format("Caja #%d - Movimiento del día | Usuario: %s",
                idCaja, usuario);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTODOS DE TABLA
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * Crea el modelo de tabla para gastos.
     */
    public DefaultTableModel crearModeloTablaGastos() {
        String[] columnas = { "ID", "Tipo", "Concepto", "Monto", "Proveedor", "Estado" };
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (ModelGastoOperativo gasto : gastosActuales) {
            modelo.addRow(new Object[] {
                    gasto.getIdGasto(),
                    gasto.getNombreTipoGasto(),
                    gasto.getConcepto(),
                    formatearMonto(gasto.getMonto()),
                    gasto.getProveedorPersona(),
                    gasto.getEstado().getValor()
            });
        }

        return modelo;
    }

    /**
     * Crea el modelo de tabla para detalles de compra.
     */
    public DefaultTableModel crearModeloTablaCompra() {
        String[] columnas = { "Descripción", "Talla", "Color", "Cant.", "P.Compra", "P.Venta", "Subtotal" };
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (ModelCompraExternaDetalle detalle : detallesCompraActual) {
            modelo.addRow(new Object[] {
                    detalle.getDescripcionProducto(),
                    detalle.getNombreTalla(),
                    detalle.getNombreColor(),
                    detalle.getCantidad(),
                    formatearMonto(detalle.getPrecioUnitario()),
                    formatearMonto(detalle.getPrecioVenta()),
                    formatearMonto(detalle.getSubtotal())
            });
        }

        return modelo;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTODOS DE UTILIDAD PRIVADOS
    // ═══════════════════════════════════════════════════════════════════════════
    private String obtenerNombreTalla(Integer idTalla) {
        return tallas.stream()
                .filter(t -> t.getIdTalla().equals(idTalla))
                .map(ModelTalla::getNumero)
                .findFirst()
                .orElse("N/A");
    }

    private String obtenerNombreColor(Integer idColor) {
        return colores.stream()
                .filter(c -> c.getIdColor().equals(idColor))
                .map(ModelColor::getNombre)
                .findFirst()
                .orElse("N/A");
    }

    private int obtenerIdTallaPorNombre(String nombre) {
        return tallas.stream()
                .filter(t -> t.getNumero().equals(nombre))
                .map(ModelTalla::getIdTalla)
                .findFirst()
                .orElse(0);
    }

    private int obtenerIdColorPorNombre(String nombre) {
        return colores.stream()
                .filter(c -> c.getNombre().equals(nombre))
                .map(ModelColor::getIdColor)
                .findFirst()
                .orElse(0);
    }

    private String formatearMonto(BigDecimal monto) {
        if (monto == null) {
            return "$0";
        }
        return String.format("$%,.0f", monto);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NOTIFICACIONES A LA VISTA
    // ═══════════════════════════════════════════════════════════════════════════
    private void notificarError(String mensaje) {
        if (onError != null) {
            onError.accept(mensaje);
        } else {
            JOptionPane.showMessageDialog(null, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void notificarExito(String mensaje) {
        if (onSuccess != null) {
            onSuccess.accept(mensaje);
        } else {
            JOptionPane.showMessageDialog(null, mensaje, "Éxito", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void notificarGastosActualizados() {
        if (onGastosUpdated != null) {
            onGastosUpdated.accept(gastosActuales);
        }
    }

    private void notificarDetallesCompraActualizados() {
        if (onDetallesCompraUpdated != null) {
            onDetallesCompraUpdated.accept(detallesCompraActual);
        }
        if (onTotalCompraUpdated != null) {
            onTotalCompraUpdated.accept(calcularTotalCompraActual());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SETTERS DE CALLBACKS
    // ═══════════════════════════════════════════════════════════════════════════
    public void setOnError(Consumer<String> onError) {
        this.onError = onError;
    }

    public void setOnSuccess(Consumer<String> onSuccess) {
        this.onSuccess = onSuccess;
    }

    public void setOnGastosUpdated(Consumer<List<ModelGastoOperativo>> callback) {
        this.onGastosUpdated = callback;
    }

    public void setOnTotalGastosUpdated(Consumer<BigDecimal> callback) {
        this.onTotalGastosUpdated = callback;
    }

    public void setOnDetallesCompraUpdated(Consumer<List<ModelCompraExternaDetalle>> callback) {
        this.onDetallesCompraUpdated = callback;
    }

    public void setOnTotalCompraUpdated(Consumer<BigDecimal> callback) {
        this.onTotalCompraUpdated = callback;
    }

    public void setOnFormCleared(Runnable callback) {
        this.onFormCleared = callback;
    }
}
