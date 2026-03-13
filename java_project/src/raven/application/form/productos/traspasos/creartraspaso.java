package raven.application.form.productos.traspasos;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import javax.swing.JOptionPane;
import raven.modal.listener.ModalController;
import raven.modal.Toast;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import raven.clases.productos.ProductoTraspasoItem;
import raven.clases.productos.TraspasoDatos;
import raven.clases.productos.TraspasoService;
import raven.controlador.principal.conexion;
import raven.controlador.productos.TraspasoController;
import raven.controlador.admin.SessionManager;
import raven.clases.admin.UserSession;

public class creartraspaso extends javax.swing.JPanel {

    public int paso = 1;
    private ModalController modalController;
    public TraspasoController controller;
    public paso1 panelPaso1;
    private paso2 panelPaso2;
    private paso3 panelPaso3;
    private int idUsuarioActual = 1;
    traspasos ts;
    private static final String PANEL = "arc:35;background:$Login.background";
    private static final String Camposdetexto = "arc:15;background:lighten($Menu.background,25%)";
    private boolean modoEdicion = false;
    private boolean cambiosGuardados = false;
    private boolean omitirConfirmacion = false; // Flag para controlar confirmación externa
    private String numeroTraspasoOriginal = null;
    private traspasos formularioTraspasos = null; // Referencia al formulario padre

    public void setOmitirConfirmacion(boolean omitir) {
        this.omitirConfirmacion = omitir;
    }

    private boolean manejandoDatosCompletados = false;

    public creartraspaso() {
        initComponents();
        initInterfaz();
        inicializarComponentesMVC();
        configurarApariencia();
        btnAtras.setVisible(false);
    }

    private void initInterfaz() {
        // Estiliza el panel principal con bordes redondeados y color de fondo
        panelPasos.putClientProperty(FlatClientProperties.STYLE, PANEL);
        panelPasosStatus.putClientProperty(FlatClientProperties.STYLE, PANEL);
        panelBotonera.putClientProperty(FlatClientProperties.STYLE, PANEL);

        // Forzar actualización del renderizado para evitar problemas visuales
        SwingUtilities.invokeLater(() -> {
            // Asegurar que todos los paneles tengan un tamaño preferido
            if (panelPasos != null) {
                panelPasos.setMinimumSize(new Dimension(700, 500));
                panelPasos.setPreferredSize(new Dimension(728, 600));
            }

            // Actualizar el layout para asegurar renderizado correcto
            revalidate();
            repaint();
        });
    }

    public void setModoEdicion(boolean modoEdicion) {
        this.modoEdicion = modoEdicion;
        System.out.println(" Formulario configurado en modo: " + (modoEdicion ? "EDICIÓN" : "CREACIÓN"));
    }

    public boolean isModoEdicion() {
        return modoEdicion;
    }

    public boolean isCambiosGuardados() {
        return cambiosGuardados;
    }

    public void setModalController(ModalController mc) {
        this.modalController = mc;
    }

    public ModalController getModalController() {
        return this.modalController;
    }

    public void settraspasos(traspasos formularioTraspasos) {
        this.formularioTraspasos = formularioTraspasos;
        this.ts = formularioTraspasos; // CORRECCIÓN: Asignar también a ts para que el listener funcione
    }

    public traspasos getFormularioTraspasos() {
        return this.formularioTraspasos;
    }

    public boolean hayCambiosPendientes() {
        if (!modoEdicion) {
            return false; // En modo creación no hay cambios pendientes
        }

        if (controller == null || controller.getTraspasoActual() == null) {
            return false;
        }

        // En modo edición, siempre considerar que puede haber cambios
        // ya que los datos se cargan y el usuario puede modificarlos
        return !cambiosGuardados;
    }

    public boolean confirmarCierreEdicion() {
        if (!modoEdicion) {
            return true;
        }

        if (cambiosGuardados) {
            return true;
        }

        int opcion = JOptionPane.showConfirmDialog(this,
                "¿Está seguro de cerrar sin guardar los cambios?\n"
                        + "Se perderán todas las modificaciones realizadas.",
                "Confirmar Cierre",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        return opcion == JOptionPane.YES_OPTION;
    }

    private void mostrarResumenCambios() {
        if (controller == null) {
            Toast.show(this, Toast.Type.ERROR, "Error: No hay datos para mostrar");
            return;
        }

        // Crear paso3 en modo edición
        paso3 p3 = new paso3(controller);

        // CORRECCIÓN: Pasar modalController también en modo edición
        if (modalController != null) {
            p3.setModalController(modalController);
        } else {
            System.err.println("WARNING modalController es null en mostrarResumenCambios");
        }

        conectarListenerPaso3(p3); // Conectar listener al nuevo panel
        p3.setSize(728, 640);
        p3.setLocation(0, 0);

        // Limpiar panel y agregar paso3
        panelPasos.removeAll();
        panelPasos.add(p3, BorderLayout.CENTER);
        panelPasos.revalidate();
        panelPasos.repaint();

        // Actualizar paso actual
        paso = 3;
        panelPaso3 = p3;

        // Actualizar botones para mostrar "Guardar Cambios"
        actualizarBotonesParaEdicion();
    }

    private void actualizarBotonesParaEdicion() {
        if (btnSiguientePaso != null) {
            btnSiguientePaso.setText("Guardar Cambios");
            if (paso == 3) {
                btnSiguientePaso.setVisible(true);
            }
        }
    }

    public void onModalClosing() {
        // Verificar si hay cambios pendientes en el paso actual y confirmar cierre
        if (!confirmarCierreActual()) {
            // Prevenir cierre del modal si el usuario cancela
            return;
        }

        if (modoEdicion && !confirmarCierreEdicion()) {
            // Prevenir cierre del modal
            return;
        }

        // Limpiar recursos y forzar actualización del renderizado para evitar problemas
        // visuales posteriores
        SwingUtilities.invokeLater(() -> {
            // Limpiar componentes para liberar memoria
            if (panelPasos != null) {
                panelPasos.removeAll();
                panelPasos.invalidate();
                panelPasos.repaint();
            }

            // Forzar actualización del contenedor principal
            invalidate();
            validate();
            repaint();
        });
    }

    /**
     * Método para confirmar cierre según el paso actual
     */
    public boolean confirmarCierreActual() {
        // Si se solicitó omitir confirmación (ej. botón X o Cancelar interno), permitir
        // cierre directo
        if (omitirConfirmacion) {
            return false; // False = NO mostrar confirmación externa (el cierre procede o ya fue
                          // confirmado)
        }
        // De lo contrario (clic fuera), pedir confirmación
        return true;

        /*
         * Lógica anterior desactivada para forzar confirmación
         * switch (paso) {
         * case 1:
         * if (panelPaso1 != null) {
         * return panelPaso1.confirmarCierre();
         * }
         * break;
         * case 2:
         * if (panelPaso2 != null) {
         * return panelPaso2.confirmarCierre();
         * }
         * break;
         * case 3:
         * if (panelPaso3 != null) {
         * return panelPaso3.confirmarCierre();
         * }
         * break;
         * }
         * return true;
         */
    }

    public boolean cargarTraspasoExistente(String numeroTraspaso,
            Map<String, Object> traspasoInfo,
            List<Map<String, Object>> detalles) {
        try {
            System.out.println(" Cargando traspaso existente en formulario: " + numeroTraspaso);

            // Guardar número original para comparación
            this.numeroTraspasoOriginal = numeroTraspaso;

            // Inicializar controller si no existe
            if (controller == null) {
                UserSession userSession = UserSession.getInstance();
                controller = new TraspasoController(userSession);
            }

            // CORRECCIÓN CRÍTICA: Configurar modo edición ANTES de cargar
            controller.configurarModoEdicion(numeroTraspaso);

            // Usar el nuevo método del controller para cargar datos
            boolean cargado = controller.cargarTraspasoExistente(numeroTraspaso);

            if (cargado) {
                System.out.println("SUCCESS  Traspaso cargado exitosamente en controller:");
                TraspasoDatos traspaso = controller.getTraspasoActual();
                System.out.println("   - Número: " + traspaso.getNumeroTraspaso());
                System.out.println("   - Origen: " + traspaso.getNombreBodegaOrigen());
                System.out.println("   - Destino: " + traspaso.getNombreBodegaDestino());
                System.out.println("   - Productos: " + traspaso.getProductos().size());
                System.out.println("   - Tipo: " + traspaso.getTipoTraspaso());
                System.out.println("   - Modo Controller: " + (controller.esModoEdicion() ? "EDICIÓN" : "CREACIÓN"));

                // Actualizar la interfaz con los datos cargados
                mostrarPaso1();

                return true;
            } else {
                System.err.println("ERROR  No se pudo cargar el traspaso en el controller");
                return false;
            }

        } catch (Exception e) {
            System.err.println("ERROR  Error cargando traspaso existente: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

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

            String tipoTraspaso = traspasoInfo.get("motivo") != null
                    ? traspasoInfo.get("motivo").toString()
                    : "Reposición de stock";
            traspaso.setTipoTraspaso(tipoTraspaso);

            traspaso.setMotivoTraspaso(traspasoInfo.get("motivo").toString());

            Object observaciones = traspasoInfo.get("observaciones");
            traspaso.setObservaciones(observaciones != null ? observaciones.toString() : "");

            traspaso.setIdUsuarioSolicita((Integer) traspasoInfo.get("id_usuario_solicita"));

            // Convertir detalles a ProductoTraspasoItem
            for (Map<String, Object> detalle : detalles) {
                ProductoTraspasoItem producto = crearProductoDesdeDetalle(detalle);
                if (producto != null) {
                    traspaso.agregarProducto(producto);
                }
            }

            System.out.println("SUCCESS  TraspasoDatos creado desde info existente");
            return traspaso;

        } catch (Exception e) {
            System.err.println("ERROR  Error creando TraspasoDatos: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

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
            producto.setTipo("par"); // Por defecto, se puede cambiar según lógica de negocio

            // Obtener precio del producto (puede requerir consulta adicional)
            java.math.BigDecimal precio = obtenerPrecioProducto(producto.getIdProducto(), producto.getIdVariante());
            producto.setPrecioUnitario(precio);

            Object observacionesProducto = detalle.get("observaciones");
            if (observacionesProducto != null) {
                producto.setObservaciones(observacionesProducto.toString());
            }

            System.out.println("   SUCCESS  Producto cargado: " + producto.getNombreCompleto());
            return producto;

        } catch (Exception e) {
            System.err.println("ERROR  Error creando producto desde detalle: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

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
                stmt.setInt(2, idProducto);
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

    protected void navegarAPaso(int numeroPaso) {
        System.out.println("Actualizando Navegando al paso: " + numeroPaso);

        switch (numeroPaso) {
            case 1:
                mostrarPaso1();
                break;
            case 2:
                mostrarPaso2();
                break;
            case 3:
                if (modoEdicion) {
                    mostrarResumenCambios();
                } else {
                    mostrarPaso3Normal();
                }
                break;
            default:
                System.err.println("ERROR  Paso no válido: " + numeroPaso);
        }
    }

    // Métodos auxiliares para navegación:
    private void mostrarPaso1() {
        if (panelPaso1 == null && controller != null) {
            panelPaso1 = new paso1(controller);
        }
        cambiarAPaso(panelPaso1, 1);
    }

    private void mostrarPaso2() {
        if (panelPaso2 == null && controller != null) {
            panelPaso2 = new paso2(controller);
        }
        cambiarAPaso(panelPaso2, 2);
    }

    private void mostrarPaso3Normal() {
        // CORRECCIÓN CRÍTICA: Siempre verificar y configurar panelPaso3 correctamente
        if (panelPaso3 == null && controller != null) {
            System.out.println("INFO Creando panelPaso3 por primera vez");
            panelPaso3 = new paso3(controller);

            // IMPORTANTE: Conectar listener inmediatamente después de crear el panel
            conectarListenerPaso3(panelPaso3);
        }

        // CORRECCIÓN: Pasar el modalController SIEMPRE, no solo en la creación
        // Esto asegura que el modalController esté disponible incluso si el panel fue
        // reutilizado
        if (panelPaso3 != null) {
            if (modalController != null) {
                System.out.println("INFO Pasando modalController a paso3: " + modalController.getClass().getName());
                panelPaso3.setModalController(modalController);
            } else {
                System.err.println("ERROR modalController es null en creartraspaso al mostrar paso3");
                System.err.println("ERROR panelPaso3 NO PODRÁ CERRAR EL MODAL AUTOMÁTICAMENTE");
            }
        }

        cambiarAPaso(panelPaso3, 3);
    }

    private void cambiarAPaso(JPanel panel, int numeroPaso) {
        if (panel != null && panelPasos != null) {
            panel.setSize(728, 640);
            panel.setLocation(0, 0);

            // Mejor manejo del cambio de paso para evitar problemas de renderizado
            panelPasos.removeAll();
            panelPasos.add(panel, BorderLayout.CENTER);

            // Forzar actualización del layout de forma ordenada
            panelPasos.invalidate();
            panelPasos.validate();
            panelPasos.repaint();

            // Forzar actualización también en el contenedor padre
            Container parent = panelPasos.getParent();
            if (parent != null) {
                parent.invalidate();
                parent.validate();
                parent.repaint();
            }

            paso = numeroPaso;

            // Actualizar visibilidad de botones
            if (btnSiguientePaso != null) {
                if (numeroPaso == 3) {
                    btnSiguientePaso.setText(modoEdicion ? "Guardar Cambios" : "Confirmar");
                } else {
                    btnSiguientePaso.setText("Siguiente paso");
                }
                btnSiguientePaso.setVisible(numeroPaso < 3 || (numeroPaso == 3)); // Ensure visible in step 3
            }
            if (btnAtras != null) {
                btnAtras.setVisible(numeroPaso > 1);
            }

            System.out.println("SUCCESS  Cambiado al paso " + numeroPaso);
        }
    }

    private void actualizarTituloEdicion() {
        if (numeroTraspasoOriginal != null) {
            // Si tienes algún label de título, actualizarlo aquí
            System.out.println("Nota Modo edición activo para: " + numeroTraspasoOriginal);
        }
    }

    public void finalizar() {
        System.out.println("Actualizando Finalizando traspaso - Modo: " + (modoEdicion ? "EDICIÓN" : "CREACIÓN"));

        if (controller == null) {
            Toast.show(this, Toast.Type.ERROR, "Error: Sistema no inicializado");
            return;
        }

        if (!controller.validarTraspasoCompleto()) {
            Toast.show(this, Toast.Type.ERROR, "Complete todos los datos antes de finalizar");
            return;
        }

        if (modoEdicion) {
            finalizarEdicion();
        } else {
            finalizarCreacion();
        }
    }

    private void finalizarEdicion() {
        String mensaje = "¿Está seguro de guardar los cambios en el traspaso " + numeroTraspasoOriginal + "?\n\n"
                + "Origen: " + controller.getTraspasoActual().getNombreBodegaOrigen() + "\n"
                + "Destino: " + controller.getTraspasoActual().getNombreBodegaDestino() + "\n"
                + "Productos: " + controller.getCantidadTotalProductos() + "\n\n"
                + "Los cambios se aplicarán inmediatamente.";

        int opcion = JOptionPane.showConfirmDialog(this,
                mensaje,
                "Confirmar Cambios",
                JOptionPane.YES_NO_OPTION);

        if (opcion == JOptionPane.YES_OPTION) {
            try {
                TraspasoService traspasoService = new TraspasoService();
                boolean exitoso = traspasoService.actualizarTraspaso(
                        controller.getTraspasoActual(),
                        numeroTraspasoOriginal);

                if (exitoso) {
                    cambiosGuardados = true;
                    Toast.show(this, Toast.Type.SUCCESS, "Traspaso actualizado exitosamente");

                    // Actualizar la tabla en el formulario padre
                    if (formularioTraspasos != null) {
                        SwingUtilities.invokeLater(() -> {
                            formularioTraspasos.forzarActualizacionTraspasos();
                        });
                    }

                    // Cerrar modal después de mostrar éxito
                    Timer timer = new Timer(1500, e -> {
                        if (modalController != null) {
                            modalController.close();
                        }
                    });
                    timer.setRepeats(false);
                    timer.start();

                } else {
                    Toast.show(this, Toast.Type.ERROR, "Error al actualizar el traspaso");
                }

            } catch (Exception e) {
                System.err.println("Error actualizando traspaso: " + e.getMessage());
                e.printStackTrace();
                Toast.show(this, Toast.Type.ERROR, "Error inesperado: " + e.getMessage());
            }
        }
    }

    private void finalizarCreacion() {
        String mensaje = "¿Está seguro de crear este traspaso?\n\n"
                + "Número: " + controller.getTraspasoActual().getNumeroTraspaso() + "\n"
                + "Origen: " + controller.getTraspasoActual().getNombreBodegaOrigen() + "\n"
                + "Destino: " + controller.getTraspasoActual().getNombreBodegaDestino() + "\n"
                + "Productos: " + controller.getCantidadTotalProductos();

        int opcion = JOptionPane.showConfirmDialog(this,
                mensaje,
                "Confirmar Creación",
                JOptionPane.YES_NO_OPTION);

        if (opcion == JOptionPane.YES_OPTION) {
            try {
                boolean exitoso = controller.confirmarYGuardarTraspaso();

                if (exitoso) {
                    cambiosGuardados = true;
                    Toast.show(this, Toast.Type.SUCCESS, "Traspaso creado exitosamente");

                    if (formularioTraspasos != null) {
                        String numero = controller.getTraspasoActual() != null
                                ? controller.getTraspasoActual().getNumeroTraspaso()
                                : null;
                        SwingUtilities.invokeLater(() -> {
                            formularioTraspasos.forzarActualizacionTraspasos();
                            if (numero != null && !numero.isEmpty()) {
                                formularioTraspasos.seleccionarTraspasoEnTabla(numero);
                            }
                        });
                    }

                    // Cerrar modal después de mostrar éxito
                    Timer timer = new Timer(1500, e -> {
                        if (modalController != null) {
                            modalController.close();
                        }
                    });
                    timer.setRepeats(false);
                    timer.start();

                } else {
                    Toast.show(this, Toast.Type.ERROR, "Error al crear el traspaso");
                }

            } catch (Exception e) {
                System.err.println("Error creando traspaso: " + e.getMessage());
                e.printStackTrace();
                Toast.show(this, Toast.Type.ERROR, "Error inesperado: " + e.getMessage());
            }
        }
    }

    /**
     * Método para validar si el traspaso puede ser editado
     */
    public boolean puedeSerEditado() {
        if (controller == null || controller.getTraspasoActual() == null) {
            return false;
        }

        String estado = controller.getTraspasoActual().getEstado();
        return "pendiente".equals(estado) || "autorizado".equals(estado);
    }

    /**
     * Método para obtener información de cambios
     */
    public String obtenerResumenCambios() {
        if (!modoEdicion || controller == null) {
            return "Nuevo traspaso";
        }

        StringBuilder resumen = new StringBuilder();
        resumen.append("CAMBIOS EN TRASPASO ").append(numeroTraspasoOriginal).append(":\n\n");

        TraspasoDatos actual = controller.getTraspasoActual();
        resumen.append("• Origen: ").append(actual.getNombreBodegaOrigen()).append("\n");
        resumen.append("• Destino: ").append(actual.getNombreBodegaDestino()).append("\n");
        resumen.append("• Tipo: ").append(actual.getTipoTraspaso()).append("\n");
        resumen.append("• Products: ").append(actual.getProductos().size()).append(" items\n");

        return resumen.toString();
    }

    private boolean actualizarTraspasoExistente() {
        try {
            // Aquí implementar la lógica de actualización
            // Por ahora usar el servicio existente o crear uno nuevo para actualización

            TraspasoService traspasoService = new TraspasoService();
            TraspasoDatos traspasoActual = controller.getTraspasoActual();

            // TODO: Implementar método de actualización en TraspasoService
            boolean resultado = traspasoService.actualizarTraspaso(traspasoActual, numeroTraspasoOriginal);

            if (resultado) {
                System.out.println("SUCCESS  Traspaso " + numeroTraspasoOriginal + " actualizado exitosamente");
            }

            return resultado;

        } catch (Exception e) {
            System.err.println("ERROR  Error actualizando traspaso: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Método para agregar al TraspasoController
     */

    private void inicializarComponentesMVC() {
        // Establecer ID de usuario desde sesión si está disponible
        int sid = SessionManager.getInstance().getCurrentUserId();
        if (sid > 0) {
            idUsuarioActual = sid;
        } else {
            System.out.println("WARNING  No hay usuario en sesión; utilizando idUsuarioActual=" + idUsuarioActual);
        }
        // Crear controlador con sesión de usuario actual
        UserSession userSession = UserSession.getInstance();
        this.controller = new TraspasoController(userSession);

        // Crear instancias de los paneles con el controlador
        this.panelPaso1 = new paso1(controller);
        this.panelPaso2 = new paso2(controller);
        // CORRECCIÓN CRÍTICA: NO crear panelPaso3 aquí porque modalController aún es
        // null.
        // Se creará en mostrarPaso3Normal() cuando modalController esté disponible.
        // this.panelPaso3 = new paso3(controller);

        this.panelPaso1.addPropertyChangeListener("datosCompletados", new java.beans.PropertyChangeListener() {
            @Override
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                if (Boolean.TRUE.equals(evt.getNewValue())) {
                    if (manejandoDatosCompletados)
                        return;
                    manejandoDatosCompletados = true;
                    if (!panelPaso1.validarDatos()) {
                        manejandoDatosCompletados = false;
                        return;
                    }
                    mostrarPaso2();
                    paso = 2;
                    btnAtras.setVisible(true);
                    actualizarIndicadorPaso(2);
                    manejandoDatosCompletados = false;
                }
            }
        });

        // CORRECCIÓN: Mover configuración de eventos de paso3 a mostrarPaso3Normal()
        // donde el panel será creado con modalController disponible.
        // conectarListenerPaso3(panelPaso3);

        // Ocultar botón atrás inicialmente
        btnAtras.setVisible(false);

        // Configurar indicadores visuales de pasos
        configurarIndicadoresPasos();

        System.out.println(" Componentes MVC inicializados correctamente");
    }

    private void configurarEventosPaso3() {
        panelPaso3.addPropertyChangeListener("traspasoConfirmado", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ((Boolean) evt.getNewValue()) {
                    System.out.println(" Traspaso confirmado - Iniciando actualización...");

                    Toast.show(creartraspaso.this, Toast.Type.SUCCESS,
                            "Traspaso creado exitosamente");

                    // ACTUALIZAR LA LISTA DE TRASPASOS EN EL FORMULARIO PRINCIPAL
                    // ACTUALIZAR LA LISTA DE TRASPASOS EN EL FORMULARIO PRINCIPAL
                    if (ts != null) {
                        // Actualizar inmediatamente
                        SwingUtilities.invokeLater(() -> {
                            try {
                                System.out.println("Actualizando Actualizando lista de traspasos...");
                                ts.forzarActualizacionTraspasos(); // Usar el método que garantiza la actualización
                                System.out.println("SUCCESS  Lista de traspasos actualizada exitosamente");

                                // Mostrar toast en el formulario principal
                                SwingUtilities.invokeLater(() -> {
                                    Toast.show(ts, Toast.Type.SUCCESS,
                                            "Nuevo traspaso agregado: "
                                                    + (controller != null && controller.getTraspasoActual() != null
                                                            ? controller.getTraspasoActual().getNumeroTraspaso()
                                                            : ""));
                                });

                            } catch (Exception e) {
                                System.err.println("ERROR  Error actualizando lista: " + e.getMessage());
                                e.printStackTrace();

                                // Mostrar error si la actualización falla
                                SwingUtilities.invokeLater(() -> {
                                    Toast.show(ts, Toast.Type.ERROR,
                                            "Error actualizando lista de traspasos");
                                });
                            }
                        });
                    }

                    // Cerrar modal después de actualizar
                    Timer timer = new Timer(2000, e -> { // 2 segundos para ver el mensaje
                        if (modalController != null) {
                            modalController.close();
                        } else {
                            // Fallback si modalController es null - intentar cerrar padre
                            java.awt.Window window = javax.swing.SwingUtilities.getWindowAncestor(creartraspaso.this);
                            if (window != null) {
                                window.dispose();
                            }
                        }
                    });
                    timer.setRepeats(false);
                    timer.start();
                } else if (Boolean.TRUE.equals(evt.getNewValue()) == false) {
                    // Caso de cancelación
                    // No hacer nada o manejar si es necesario
                }
            }
        });
    }

    private void conectarListenerPaso3(paso3 panel) {
        if (panel == null)
            return;

        // Remover listeners anteriores para evitar duplicados si se reutiliza
        for (java.beans.PropertyChangeListener pl : panel.getPropertyChangeListeners("traspasoConfirmado")) {
            panel.removePropertyChangeListener("traspasoConfirmado", pl);
        }

        panel.addPropertyChangeListener("traspasoConfirmado", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ((Boolean) evt.getNewValue()) {
                    System.out.println(" Traspaso confirmado (Listener Dinámico) - Cerrando modal...");

                    Toast.show(creartraspaso.this, Toast.Type.SUCCESS, "Traspaso completado exitosamente");

                    // ACTUALIZAR LA LISTA DE TRASPASOS EN EL FORMULARIO PRINCIPAL
                    if (ts != null) {
                        SwingUtilities.invokeLater(() -> {
                            ts.forzarActualizacionTraspasos();
                        });
                    }

                    // CORRECCIÓN: NO cerrar el modal aquí - paso3 maneja el cierre automático
                    // con su timer de 2 segundos. Esto previene intentos duplicados de cierre.
                    // Si necesitas cerrar manualmente, usa el botón Finalizar.
                    System.out.println("INFO Listener confirmado - paso3 manejará el cierre automático");
                }
            }
        });

        panelPaso3.addPropertyChangeListener("traspasoCancelado", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ((Boolean) evt.getNewValue()) {
                    if (modalController != null) {
                        modalController.close();
                    }
                }
            }

        });
    }

    public boolean iniciarConAutoConfig() {
        try {
            raven.clases.productos.TraspasoConfig cfg = raven.clases.productos.TraspasoConfig.load();
            if (!raven.clases.productos.TraspasoConfig.getAutoApplyPref()) {
                return false;
            }

            if (cfg.getIdOrigen() == null || cfg.getIdDestino() == null || cfg.getTipo() == null
                    || cfg.getMotivo() == null) {
                return false;
            }
            if (cfg.getIdOrigen().intValue() == cfg.getIdDestino().intValue()) {
                Toast.show(this, Toast.Type.WARNING, "Origen y destino deben ser diferentes");
                return false;
            }
            if (cfg.getMotivo().trim().length() < 10) {
                Toast.show(this, Toast.Type.WARNING, "Motivo demasiado corto");
                return false;
            }

            java.util.List<raven.clases.productos.Bodega> bodegas = controller.cargarBodegas();
            raven.clases.productos.Bodega bOrigen = null, bDestino = null;
            for (raven.clases.productos.Bodega b : bodegas) {
                if (b.getIdBodega() != null && b.getIdBodega().intValue() == cfg.getIdOrigen().intValue())
                    bOrigen = b;
                if (b.getIdBodega() != null && b.getIdBodega().intValue() == cfg.getIdDestino().intValue())
                    bDestino = b;
            }
            if (bOrigen == null || bDestino == null) {
                Toast.show(this, Toast.Type.ERROR, "No se encontraron bodegas configuradas");
                return false;
            }

            String numero = controller.generarNumeroTraspaso();
            String observaciones = "";
            controller.guardarDatosPaso1(numero, bOrigen, bDestino, cfg.getTipo(), cfg.getMotivo(), observaciones);
            if (cfg.getIdUsuarioSolicita() != null && cfg.getIdUsuarioSolicita() > 0) {
                controller.getTraspasoActual().setIdUsuarioSolicita(cfg.getIdUsuarioSolicita());
            }

            mostrarPaso2();
            paso = 2;
            btnAtras.setVisible(true);
            actualizarIndicadorPaso(2);

            // Forzar actualización del renderizado para evitar problemas visuales
            SwingUtilities.invokeLater(() -> {
                // Actualizar el layout del contenedor principal
                invalidate();
                validate();
                repaint();

                // Actualizar el panel de pasos específico
                if (panelPasos != null) {
                    panelPasos.invalidate();
                    panelPasos.validate();
                    panelPasos.repaint();
                }
            });

            return true;

        } catch (Exception e) {
            System.err.println("Error iniciando con auto-config: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void configurarApariencia() {
        // Configurar colores iniciales de los paneles indicadores
        actualizarIndicadorPaso(1);

        try {
            if (jButton2 != null) {
                jButton2.putClientProperty(FlatClientProperties.STYLE,
                        "arc:18; borderWidth:1; borderColor:darken(#e74c3c,10%); background:#e74c3c; foreground:#ffffff;");
                jButton2.setIcon(new FlatSVGIcon("raven/icon/svg/action_cancel_3d.svg", 0.9f));
                jButton2.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
                jButton2.setIconTextGap(8);
                jButton2.setPreferredSize(new Dimension(142, 40));
            }
            if (btnAtras != null) {
                btnAtras.putClientProperty(FlatClientProperties.STYLE,
                        "arc:18; borderWidth:1; borderColor:darken(#8892a6,10%); background:#8892a6; foreground:#ffffff;");
                btnAtras.setIcon(new FlatSVGIcon("raven/icon/svg/action_back_3d.svg", 0.9f));
                btnAtras.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
                btnAtras.setIconTextGap(8);
            }
            if (btnSiguientePaso != null) {
                btnSiguientePaso.putClientProperty(FlatClientProperties.STYLE,
                        "arc:18; borderWidth:1; borderColor:darken(#0d6efd,10%); background:#0d6efd; foreground:#ffffff;");
                btnSiguientePaso.setIcon(new FlatSVGIcon("raven/icon/svg/action_next_3d.svg", 0.9f));
                btnSiguientePaso.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
                btnSiguientePaso.setIconTextGap(8);
                btnSiguientePaso.setPreferredSize(new Dimension(160, 40));
            }
        } catch (Exception ignore) {
        }
    }

    public void configurarParaEdicion() {
        if (!modoEdicion) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                // Actualizar título del formulario
                if (numeroTraspasoOriginal != null) {
                    // Si tienes algún componente de título, actualizarlo aquí
                    System.out.println("Nota Configurando formulario para editar: " + numeroTraspasoOriginal);
                }

                // Configurar botones específicos para edición
                configurarBotonesEdicion();

                // Validar que los datos estén correctamente cargados
                if (controller != null && controller.getTraspasoActual() != null) {
                    TraspasoDatos traspaso = controller.getTraspasoActual();

                    // Verificar datos críticos
                    if (traspaso.getNumeroTraspaso() == null ||
                            traspaso.getIdBodegaOrigen() == null ||
                            traspaso.getIdBodegaDestino() == null) {

                        System.err.println("WARNING  Datos críticos faltantes en modo edición");
                        // mostrarError("Error: Datos del traspaso incompletos para edición");
                        return;
                    }

                    System.out.println("SUCCESS  Formulario configurado correctamente para edición");
                }

            } catch (Exception e) {
                System.err.println("ERROR  Error configurando formulario para edición: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void configurarBotonesEdicion() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Buscar botones por sus nombres reales en tu formulario
                // Necesitas verificar en tu archivo .form o .java cuáles son los nombres reales

                // OPCIÓN A: Si los botones están declarados en Variables declaration
                buscarYConfigurarBotones();

                // OPCIÓN B: Configuración manual (una vez conozcas los nombres)

                // Cambiar título si existe
                actualizarTituloEdicion();

            } catch (Exception e) {
                System.err.println("WARNING  Error configurando botones de edición: " + e.getMessage());
                // No es crítico, continuar sin cambiar botones
            }
        });
    }

    private void buscarYConfigurarBotones() {
        // Buscar todos los botones en el formulario
        Component[] components = this.getComponents();
        buscarBotonesEnContainer(this);
    }

    private void buscarBotonesEnContainer(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton) {
                JButton boton = (JButton) component;
                String texto = boton.getText() != null ? boton.getText().toLowerCase() : "";

                // Identificar botones por su texto
                if (texto.contains("siguiente") || texto.contains("continuar") || texto.contains("next")) {
                    boton.setText("Actualizar");
                    System.out.println("SUCCESS  Botón 'Siguiente' actualizado a 'Actualizar'");
                } else if (texto.contains("finalizar") || texto.contains("confirmar") || texto.contains("guardar")) {
                    boton.setText("Guardar Cambios");
                    System.out.println("SUCCESS  Botón 'Finalizar' actualizado a 'Guardar Cambios'");
                }
            } else if (component instanceof Container) {
                buscarBotonesEnContainer((Container) component);
            }
        }
    }

    private void configurarIndicadoresPasos() {
        // Los paneles jPanel5, jPanel8, jPanel9 actúan como indicadores
        // jPanel5 = Paso 1, jPanel8 = Paso 2, jPanel9 = Paso 3

        // Color inactivo: gris claro
        Color colorInactivo = new Color(200, 200, 200);
        // Color activo: azul
        Color colorActivo = new Color(33, 150, 243);
        // Color completado: verde
        Color colorCompletado = new Color(76, 175, 80);

        // Establecer colores iniciales
        jPanel5.setBackground(colorActivo); // Paso 1 activo
        jPanel8.setBackground(colorInactivo); // Paso 2 inactivo
        jPanel9.setBackground(colorInactivo); // Paso 3 inactivo
    }

    private void actualizarIndicadorPaso(int pasoActual) {
        Color colorInactivo = new Color(200, 200, 200);
        Color colorActivo = new Color(33, 150, 243);
        Color colorCompletado = new Color(76, 175, 80);

        // Resetear todos los colores
        jPanel5.setBackground(colorInactivo);
        jPanel8.setBackground(colorInactivo);
        jPanel9.setBackground(colorInactivo);

        // Marcar pasos completados
        if (pasoActual > 1) {
            jPanel5.setBackground(colorCompletado);
        }
        if (pasoActual > 2) {
            jPanel8.setBackground(colorCompletado);
        }

        // Marcar paso actual
        switch (pasoActual) {
            case 1:
                jPanel5.setBackground(colorActivo);
                break;
            case 2:
                jPanel8.setBackground(colorActivo);
                break;
            case 3:
                jPanel9.setBackground(colorActivo);
                break;
        }

        // Repintar componentes
        jPanel5.repaint();
        jPanel8.repaint();
        jPanel9.repaint();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        datePicker = new raven.datetime.component.date.DatePicker();
        jTextField1 = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel1 = new javax.swing.JPanel();
        panelPasos = new javax.swing.JPanel();
        panelPasosStatus = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel8 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jPanel9 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
        panelBotonera = new javax.swing.JPanel();
        btnSiguientePaso = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        btnAtras = new javax.swing.JButton();

        jTextField1.setText("jTextField1");

        jPanel1.setMaximumSize(new java.awt.Dimension(727, 765));
        jPanel1.setPreferredSize(new java.awt.Dimension(727, 765));

        panelPasos.setBackground(new java.awt.Color(153, 255, 153));
        panelPasos.setPreferredSize(new java.awt.Dimension(669, 650));

        javax.swing.GroupLayout panelPasosLayout = new javax.swing.GroupLayout(panelPasos);
        panelPasos.setLayout(panelPasosLayout);
        panelPasosLayout.setHorizontalGroup(
                panelPasosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE));
        panelPasosLayout.setVerticalGroup(
                panelPasosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 705, Short.MAX_VALUE));

        panelPasosStatus.setBackground(new java.awt.Color(204, 255, 102));
        panelPasosStatus.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        jPanel5.setBackground(new java.awt.Color(102, 102, 255));
        jPanel5.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Datos basicos");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
                jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 150,
                                javax.swing.GroupLayout.PREFERRED_SIZE));
        jPanel5Layout.setVerticalGroup(
                jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING,
                                javax.swing.GroupLayout.DEFAULT_SIZE, 48, Short.MAX_VALUE));

        jPanel8.setBackground(new java.awt.Color(102, 102, 255));
        jPanel8.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Productos");

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
                jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE));
        jPanel8Layout.setVerticalGroup(
                jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING,
                                javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE));

        jPanel9.setBackground(new java.awt.Color(102, 102, 255));
        jPanel9.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Confirmar");

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
                jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel9Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 150,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)));
        jPanel9Layout.setVerticalGroup(
                jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING,
                                javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                                Short.MAX_VALUE));

        jSeparator1.setForeground(new java.awt.Color(0, 0, 0));
        jSeparator1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N

        jSeparator2.setForeground(new java.awt.Color(0, 0, 0));
        jSeparator2.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N

        javax.swing.GroupLayout panelPasosStatusLayout = new javax.swing.GroupLayout(panelPasosStatus);
        panelPasosStatus.setLayout(panelPasosStatusLayout);
        panelPasosStatusLayout.setHorizontalGroup(
                panelPasosStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelPasosStatusLayout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 88,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(5, 5, 5)
                                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 100,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(5, 5, 5)
                                .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap()));
        panelPasosStatusLayout.setVerticalGroup(
                panelPasosStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelPasosStatusLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelPasosStatusLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jPanel8, javax.swing.GroupLayout.Alignment.TRAILING,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jPanel9, javax.swing.GroupLayout.Alignment.TRAILING,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelPasosStatusLayout
                                                .createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(14, 14, 14)))
                                .addContainerGap())
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                panelPasosStatusLayout.createSequentialGroup()
                                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(19, 19, 19)));

        panelBotonera.setBackground(new java.awt.Color(255, 204, 204));

        btnSiguientePaso.setText("Siguiente paso");
        btnSiguientePaso.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSiguientePasoActionPerformed(evt);
            }
        });

        jButton2.setText("Cancelar");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        btnAtras.setText("Atras");
        btnAtras.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAtrasActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelBotoneraLayout = new javax.swing.GroupLayout(panelBotonera);
        panelBotonera.setLayout(panelBotoneraLayout);
        panelBotoneraLayout.setHorizontalGroup(
                panelBotoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                panelBotoneraLayout.createSequentialGroup()
                                        .addContainerGap(302, Short.MAX_VALUE)
                                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 142,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(btnAtras, javax.swing.GroupLayout.PREFERRED_SIZE, 100,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btnSiguientePaso, javax.swing.GroupLayout.PREFERRED_SIZE, 142,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(24, 24, 24)));
        panelBotoneraLayout.setVerticalGroup(
                panelBotoneraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelBotoneraLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelBotoneraLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(btnAtras, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(btnSiguientePaso, javax.swing.GroupLayout.PREFERRED_SIZE, 40,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jButton2, javax.swing.GroupLayout.Alignment.LEADING,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(10, 10, 10)));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addGroup(jPanel1Layout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(panelPasos, javax.swing.GroupLayout.Alignment.LEADING,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, 728, Short.MAX_VALUE)
                                        .addComponent(panelBotonera, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(panelPasosStatus, javax.swing.GroupLayout.Alignment.LEADING,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(20, 20, 20)));
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(panelPasosStatus, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(10, 10, 10)
                                .addComponent(panelPasos, javax.swing.GroupLayout.DEFAULT_SIZE, 705, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(panelBotonera, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(10, 10, 10)));

        jScrollPane1.setViewportView(jPanel1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 775, Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 855, Short.MAX_VALUE));
    }// </editor-fold>//GEN-END:initComponents

    private void btnSiguientePasoActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnSiguientePasoActionPerformed
        if (paso == 1) {
            // CORRECCIÓN: Validar y guardar datos del paso 1 antes de continuar
            if (!panelPaso1.validarDatos()) {
                Toast.show(this, Toast.Type.ERROR, "Complete correctamente los datos del Paso 1");
                return;
            }

            // Guardar datos del paso 1
            panelPaso1.guardarDatos();

            // Verificar que los datos se guardaron correctamente
            if (!controller.tieneDatosBasicos()) {
                Toast.show(this, Toast.Type.ERROR, "Error al guardar los datos del Paso 1");
                return;
            }

            // CORRECCIÓN: Crear nueva instancia de paso2 con el controller actualizado
            panelPaso2 = new paso2(controller);
            panelPaso2.setSize(728, 640);
            panelPaso2.setLocation(0, 0);

            panelPasos.removeAll();
            panelPasos.add(panelPaso2, BorderLayout.CENTER);
            panelPasos.revalidate();
            panelPasos.repaint();

            paso = 2;
            btnAtras.setVisible(true);
            actualizarIndicadorPaso(2);

            System.out.println("SUCCESS  Navegación a Paso 2 exitosa");
            return;

        } else if (paso == 2) {
            // CORRECCIÓN: Verificar que hay productos agregados antes de continuar
            if (!controller.tieneProductos()) {
                Toast.show(this, Toast.Type.WARNING,
                        "Debe agregar al menos un producto al traspaso");
                return;
            }

            // CORRECCIÓN: Crear nueva instancia de paso3 con el controller actualizado
            panelPaso3 = new paso3(controller);
            panelPaso3.setSize(728, 640);
            panelPaso3.setLocation(0, 0);

            panelPasos.removeAll();
            panelPasos.add(panelPaso3, BorderLayout.CENTER);
            panelPasos.revalidate();
            panelPasos.repaint();

            paso = 3;
            btnSiguientePaso.setText(modoEdicion ? "Guardar Cambios" : "Confirmar");
            actualizarIndicadorPaso(3);

            // Configurar eventos del nuevo paso3
            configurarEventosPaso3();

            System.out.println("SUCCESS  Navegación a Paso 3 exitosa");

        } else if (paso == 3) {
            // CORRECCIÓN: Permitir finalizar desde el botón principal invocando la
            // confirmación de paso3
            if (panelPaso3 != null) {
                panelPaso3.confirmarTraspaso();
            } else {
                Toast.show(this, Toast.Type.ERROR, "Error interno: paso3 no inicializado");
            }
        }
    }// GEN-LAST:event_btnSiguientePasoActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton2ActionPerformed
        int respuesta = JOptionPane.showConfirmDialog(
                creartraspaso.this,
                "¿Está seguro de salir del proceso de creación de traspaso?\n"
                        + "Se perderán todos los datos ingresados.",
                "Confirmar salir",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (respuesta == JOptionPane.YES_OPTION) {
            if (controller != null) {
                controller.reiniciarTraspaso();
            }
            if (modalController != null) {
                setOmitirConfirmacion(true); // Evitar doble confirmación
                modalController.close();
            }
        }

    }// GEN-LAST:event_jButton2ActionPerformed

    private void btnAtrasActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnAtrasActionPerformed
        if (paso == 3) {
            // CORRECCIÓN: Volver al paso 2 manteniendo el mismo controller
            panelPaso2.setSize(669, 640);
            panelPaso2.setLocation(0, 0);

            panelPasos.removeAll();
            panelPasos.add(panelPaso2, BorderLayout.CENTER);
            panelPasos.revalidate();
            panelPasos.repaint();

            paso = 2;
            btnSiguientePaso.setText("Siguiente paso");
            actualizarIndicadorPaso(2);

            System.out.println(" Regreso a Paso 2");
            return;

        } else if (paso == 2) {
            // CORRECCIÓN: Volver al paso 1 manteniendo el mismo controller
            panelPaso1.setSize(669, 640);
            panelPaso1.setLocation(0, 0);
            panelPaso1.panelPasos.putClientProperty(FlatClientProperties.STYLE, PANEL);
            panelPasos.removeAll();
            panelPasos.add(panelPaso1, BorderLayout.CENTER);
            panelPasos.revalidate();
            panelPasos.repaint();

            paso = 1;
            btnAtras.setVisible(false);
            btnSiguientePaso.setText("Siguiente paso");
            actualizarIndicadorPaso(1);

            System.out.println(" Regreso a Paso 1");
        }

    }// GEN-LAST:event_btnAtrasActionPerformed

    public void init() {
        // CORRECCIÓN: Inicializar datos del paso 1 cuando se abre el modal
        if (controller != null && panelPaso1 != null) {
            // Generar número de traspaso automáticamente
            String numeroTraspaso = controller.generarNumeroTraspaso();
            if (numeroTraspaso != null) {
                System.out.println(" Número de traspaso inicializado: " + numeroTraspaso);
            }
        }

        // Forzar actualización del renderizado para evitar problemas visuales
        SwingUtilities.invokeLater(() -> {
            // Actualizar el layout del contenedor principal
            this.invalidate();
            this.validate();
            this.repaint();

            // Actualizar todos los componentes hijos para asegurar renderizado correcto
            for (Component comp : this.getComponents()) {
                if (comp instanceof Container) {
                    ((Container) comp).invalidate();
                    ((Container) comp).validate();
                }
                comp.invalidate();
                comp.repaint();
            }

            // Forzar actualización del panel de pasos específico
            if (panelPasos != null) {
                panelPasos.invalidate();
                panelPasos.validate();
                panelPasos.repaint();
            }
        });

        System.out.println(" Modal de traspaso inicializado");
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAtras;
    private javax.swing.JButton btnSiguientePaso;
    private raven.datetime.component.date.DatePicker datePicker;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JPanel panelBotonera;
    public javax.swing.JPanel panelPasos;
    private javax.swing.JPanel panelPasosStatus;
    // End of variables declaration//GEN-END:variables

}
