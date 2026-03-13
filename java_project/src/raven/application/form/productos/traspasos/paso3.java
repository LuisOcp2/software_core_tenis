/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package raven.application.form.productos.traspasos;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.*;
import java.awt.*;
import raven.clases.productos.TraspasoService;
import raven.controlador.productos.TraspasoController;
import raven.modal.listener.ModalController;

public class paso3 extends javax.swing.JPanel {

    private TraspasoController controller;
    private ModalController modalController; // Para cerrar el modal después de confirmar
    private static final String PANEL = "background:$Login.background";

    public paso3() {
        initComponents();
        configurarComponentes();
    }

    public paso3(TraspasoController controller) {
        this.controller = controller;
        initComponents();
        configurarComponentes();
        cargarResumen();
        resumenArea.putClientProperty(FlatClientProperties.STYLE, PANEL);
    }

    public void confirmarTraspaso() {
        if (controller == null) {
            mostrarErrorSinController();
            return;
        }
        System.out.println("INFO Iniciando confirmación del traspaso...");

        // Validación final antes de confirmar
        if (!controller.validarTraspasoCompleto()) {
            mostrarError("El traspaso tiene datos incompletos o incorrectos");
            return;
        }
        // CORRECCIÓN: Detectar si estamos en modo edición
        boolean modoEdicion = detectarModoEdicion();
        String accion = modoEdicion ? "actualizar" : "crear";
        String textoBoton = modoEdicion ? "Guardar Cambios" : "Confirmar Traspaso";
        // Mostrar diálogo de confirmación con resumen
        String mensaje = "¿Está seguro de " + accion + " este traspaso?\n\n"
                + "Número: " + controller.getTraspasoActual().getNumeroTraspaso() + "\n"
                + "Origen: " + controller.getTraspasoActual().getNombreBodegaOrigen() + "\n"
                + "Destino: " + controller.getTraspasoActual().getNombreBodegaDestino() + "\n"
                + "Productos: " + controller.getCantidadTotalProductos() + "\n\n"
                + (modoEdicion ? "Los cambios se aplicarán inmediatamente." : "Esta acción no se puede deshacer.");
        int opcion = JOptionPane.showConfirmDialog(
                this,
                mensaje,
                textoBoton,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (opcion == JOptionPane.YES_OPTION) {

            javax.swing.SwingWorker<Boolean, String> worker = new javax.swing.SwingWorker<Boolean, String>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    try {
                        TraspasoService service = new TraspasoService();
                        if (controller != null && controller.getTraspasoActual() != null) {
                            String numero = controller.getTraspasoActual().getNumeroTraspaso();
                            boolean necesitaNuevo = (numero == null || numero.trim().isEmpty());
                            if (!necesitaNuevo) {
                                try {
                                    necesitaNuevo = service.existeNumeroTraspaso(numero);
                                } catch (Exception ignore) {
                                    necesitaNuevo = false;
                                }
                            }
                            if (necesitaNuevo) {
                                String nuevo = null;
                                try {
                                    nuevo = service.generarNumeroTraspaso();
                                } catch (Exception ignore) {
                                }
                                if (nuevo != null && !nuevo.trim().isEmpty()) {
                                    controller.getTraspasoActual().setNumeroTraspaso(nuevo);
                                    publicarEstado("Creando traspaso...");
                                }
                            } else {
                                publicarEstado("Creando traspaso...");
                            }
                        } else {
                            publicarEstado("Creando traspaso...");
                        }
                        boolean exito = modoEdicion ? ejecutarActualizacion() : ejecutarCreacion();
                        return exito;
                    } catch (Exception e) {
                        return false;
                    }
                }

                private void publicarEstado(String s) {
                    publish(s);
                }

                @Override
                protected void process(java.util.List<String> chunks) {
                }

                @Override
                protected void done() {
                    boolean exito = false;
                    try {
                        exito = get();
                    } catch (Exception ignore) {
                    }
                    if (exito) {
                        System.out.println("SUCCESS Traspaso " + accion + " exitosamente");
                        mostrarExito("Traspaso " + accion + " exitosamente");
                        deshabilitarInterfaz();
                        SwingUtilities.invokeLater(() -> {
                            firePropertyChange("traspasoConfirmado", false, true);
                            System.out.println("INFO Evento traspasoConfirmado disparado");
                        });
                        System.out.println("INFO Iniciando timer de cierre automático (2 segundos)");
                        javax.swing.Timer timer = new javax.swing.Timer(2000, e -> {
                            System.out.println("INFO Timer activado - cerrando modal...");
                            SwingUtilities.invokeLater(() -> {
                                cerrarModal();
                            });
                        });
                        timer.setRepeats(false);
                        timer.start();
                    } else {
                        mostrarError("Error al " + accion + " el traspaso. Intente nuevamente.");
                    }
                }
            };
            worker.execute();
        }
    }

    private boolean detectarModoEdicion() {
        // MÉTODO 1: Verificar controller directamente
        if (controller != null && controller.esModoEdicion()) {
            System.out.println("INFO Modo edición detectado por controller");
            return true;
        }

        // MÉTODO 3: Verificar por el título
        if (jLabel1 != null && jLabel1.getText().contains("Cambios")) {
            System.out.println("INFO Modo edición detectado por título");
            return true;
        }

        // MÉTODO 4: Verificar por el contenido del resumen
        if (resumenArea != null && resumenArea.getText().contains("EDITANDO TRASPASO")) {
            System.out.println("INFO Modo edición detectado por contenido del resumen");
            return true;
        }

        System.out.println("INFO Modo creación detectado");
        return false;
    }

    private boolean ejecutarActualizacion() {
        try {
            System.out.println("SAVE Ejecutando actualización de traspaso...");

            // Obtener número original del traspaso (desde el título o resumen)
            String numeroOriginal = extraerNumeroOriginal();

            if (numeroOriginal == null) {
                System.err.println("ERROR No se pudo determinar el número original del traspaso");
                return false;
            }

            TraspasoService traspasoService = new TraspasoService();
            boolean resultado = traspasoService.actualizarTraspaso(
                    controller.getTraspasoActual(),
                    numeroOriginal);

            if (resultado) {
                System.out.println("SUCCESS Traspaso actualizado exitosamente: " + numeroOriginal);
            }

            return resultado;

        } catch (Exception e) {
            System.err.println("ERROR Error en actualización: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * MÉTODO NUEVO - Ejecutar creación de traspaso nuevo
     */
    private boolean ejecutarCreacion() {
        try {
            TraspasoService service = new TraspasoService();
            if (controller != null && controller.getTraspasoActual() != null) {
                String numero = controller.getTraspasoActual().getNumeroTraspaso();
                boolean necesitaNuevo = (numero == null || numero.trim().isEmpty());
                if (!necesitaNuevo) {
                    try {
                        necesitaNuevo = service.existeNumeroTraspaso(numero);
                    } catch (Exception ignore) {
                        necesitaNuevo = false;
                    }
                }
                if (necesitaNuevo) {
                    String nuevo = null;
                    try {
                        nuevo = service.generarNumeroTraspaso();
                    } catch (Exception ignore) {
                    }
                    if (nuevo != null && !nuevo.trim().isEmpty()) {
                        controller.getTraspasoActual().setNumeroTraspaso(nuevo);
                        if (jLabel1 != null) {
                            String titulo = jLabel1.getText();
                            if (titulo != null && titulo.contains("Confirmación")) {
                                jLabel1.setText("Confirmación del Traspaso - " + nuevo
                                        + (controller.getCantidadTotalProductos() > 0
                                                ? " (" + controller.getCantidadTotalProductos() + " productos)"
                                                : ""));
                            }
                        }
                    }
                }
            }
        } catch (Exception ignore) {
        }
        return controller.confirmarYGuardarTraspaso();
    }

    /**
     * MÉTODO AUXILIAR - Extraer número original del traspaso
     */
    private String extraerNumeroOriginal() {
        // MÉTODO 1: Obtener desde el controller (más confiable)
        if (controller != null && controller.getNumeroTraspasoOriginal() != null) {
            String numero = controller.getNumeroTraspasoOriginal();
            System.out.println("INFO Número original obtenido del controller: " + numero);
            return numero;
        }

        // MÉTODO 2: Obtener desde el traspaso actual
        if (controller != null && controller.getTraspasoActual() != null
                && controller.getTraspasoActual().getNumeroTraspaso() != null) {
            String numero = controller.getTraspasoActual().getNumeroTraspaso();
            System.out.println("INFO Número original obtenido del traspaso actual: " + numero);
            return numero;
        }

        // MÉTODO 3: Intentar extraer desde el título
        if (jLabel1 != null && jLabel1.getText().contains("Traspaso:")) {
            String titulo = jLabel1.getText();
            int inicio = titulo.indexOf("Traspaso:") + 9;
            if (inicio < titulo.length()) {
                String numero = titulo.substring(inicio).trim();
                if (numero.startsWith("TR")) {
                    System.out.println("INFO Número original extraído del título: " + numero);
                    return numero;
                }
            }
        }

        // MÉTODO 4: Intentar extraer desde el resumen
        if (resumenArea != null) {
            String resumen = resumenArea.getText();
            if (resumen.contains("Traspaso Original:")) {
                int inicio = resumen.indexOf("Traspaso Original:") + 18;
                int fin = resumen.indexOf("\n", inicio);
                if (inicio < resumen.length() && fin > inicio) {
                    String numero = resumen.substring(inicio, fin).trim();
                    System.out.println("INFO Número original extraído del resumen: " + numero);
                    return numero;
                }
            }

            // También buscar en la sección de número
            if (resumen.contains("Número:")) {
                int inicio = resumen.indexOf("Número:") + 7;
                int fin = resumen.indexOf("\n", inicio);
                if (inicio < resumen.length() && fin > inicio) {
                    String numero = resumen.substring(inicio, fin).trim();
                    System.out.println("INFO Número original extraído de número en resumen: " + numero);
                    return numero;
                }
            }
        }

        System.err.println("ERROR No se pudo extraer número original del traspaso");
        return null;
    }

    // NUEVO MÉTODO - Deshabilitar interfaz después de confirmar
    private void deshabilitarInterfaz() {
        if (resumenArea != null) {

        }

        // Agregar mensaje de confirmación al resumen
        if (resumenArea != null && controller != null) {
            String resumenActual = resumenArea.getText();
            String fechaConfirmacion = java.time.LocalDateTime.now().toString();
            resumenActual += "\n\nTRASPASO CONFIRMADO\n";
            resumenActual += "Fecha de confirmación: " + fechaConfirmacion + "\n";
            resumenActual += "Usuario: " + controller.getIdUsuarioActual();
            resumenArea.setText(resumenActual);
        }

        // CORRECCIÓN: Notificar que se completó exitosamente para que el botón
        // Finalizar funcione
        if (controller != null && controller.getTraspasoActual() != null) {
            controller.getTraspasoActual().setConfirmado(true);
        }
    }

    // MÉTODO CORREGIDO - cancelarTraspaso
    private void cancelarTraspaso() {
        int opcion = JOptionPane.showConfirmDialog(
                this,
                "¿Está seguro de cancelar este traspaso?\nSe perderán todos los datos ingresados.",
                "Cancelar Traspaso",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (opcion == JOptionPane.YES_OPTION) {
            if (controller != null) {
                controller.reiniciarTraspaso();
                System.out.println("INFO Traspaso cancelado y reiniciado");
            }

            // Notificar al contenedor padre que se canceló
            firePropertyChange("traspasoCancelado", false, true);
        }
    }

    public void configurarParaModoEdicion(boolean esEdicion, String numeroOriginal) {
        if (esEdicion) {
            System.out.println("INFO Configurando paso3 para modo edición: " + numeroOriginal);

            // Actualizar el título del panel
            if (jLabel1 != null) {
                jLabel1.setText("Confirmar Cambios - Traspaso: " + numeroOriginal);
            }

            // Agregar información específica de edición al resumen
            if (controller != null) {
                String resumenOriginal = controller.generarResumenTraspaso();
                if (resumenArea != null && resumenOriginal != null) {
                    String resumenEdicion = "═══════════════════════════════════════\n"
                            + "           EDITANDO TRASPASO\n"
                            + "═══════════════════════════════════════\n"
                            + "Traspaso Original: " + numeroOriginal + "\n"
                            + "Modo: EDICIÓN - Los cambios se aplicarán inmediatamente\n\n"
                            + resumenOriginal;

                    resumenArea.setText(resumenEdicion);
                    resumenArea.setCaretPosition(0);
                }
            }
        }
    }

    // MÉTODO CORREGIDO - validarDatos
    public boolean validarDatos() {
        if (controller == null) {
            System.out.println("Validación fallida: Controller no disponible");
            return false;
        }

        boolean valido = controller.validarTraspasoCompleto();
        System.out.println("Validación de datos: " + (valido ? "VÁLIDO" : "INVÁLIDO"));

        return valido;
    }

    // NUEVOS MÉTODOS - Mostrar mensajes al usuario
    private void mostrarErrorSinController() {
        String mensaje = "Error: Sistema no inicializado correctamente";
        mostrarError(mensaje);
    }

    private void mostrarError(String mensaje) {
        if (resumenArea != null) {
            resumenArea.setText("ERROR\n\n" + mensaje + "\n\nContacte al administrador del sistema.");
        }

        if (jLabel1 != null) {
            jLabel1.setText("Error - Confirmación del Traspaso");
        }

        System.out.println(mensaje);
    }

    private void mostrarAdvertencia(String mensaje) {
        if (resumenArea != null) {
            String contenido = "ADVERTENCIA\n\n" + mensaje + "\n\n"
                    + "Complete los pasos anteriores antes de continuar.\n\n"
                    + "PASOS REQUERIDOS:\n"
                    + "1. Seleccionar bodegas origen y destino\n"
                    + "2. Especificar tipo y motivo del traspaso\n"
                    + "3. Agregar al menos un producto\n\n"
                    + "Una vez completados estos pasos, regrese aquí para confirmar.";
            resumenArea.setText(contenido);
        }
    }

    private void mostrarExito(String mensaje) {
        System.out.println(mensaje);
    }

    // NUEVO MÉTODO - Refrescar vista

    // NUEVO MÉTODO - Obtener información de estado
    public String obtenerEstadoActual() {
        if (controller == null) {
            return "Controller no disponible";
        }

        StringBuilder estado = new StringBuilder();
        estado.append("ESTADO ACTUAL DEL PASO 3:\n");
        estado.append("Controller: ").append(controller != null ? "OK" : "FAIL").append("\n");

        if (controller != null && controller.getTraspasoActual() != null) {
            estado.append("Datos básicos: ").append(controller.tieneDatosBasicos() ? "OK" : "FAIL").append("\n");
            estado.append("Productos: ").append(controller.tieneProductos() ? "OK" : "FAIL").append("\n");
            estado.append("Válido: ").append(controller.validarTraspasoCompleto() ? "OK" : "FAIL").append("\n");
            estado.append("Cantidad productos: ").append(controller.getCantidadTotalProductos()).append("\n");
        }

        return estado.toString();
    }

    // DEBUG
    public void debug() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("DEBUG - PASO3");
        System.out.println("=".repeat(50));
        System.out.println(obtenerEstadoActual());

        if (controller != null) {
            controller.debug();
        }

        System.out.println("=".repeat(50) + "\n");
    }

    private void configurarComponentes() {
        // Configurar área de resumen
        if (resumenArea != null) {
            resumenArea.setEditable(false);
            resumenArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        }

        System.out.println("INFO Componentes del paso3 configurados");
    }

    public void setController(TraspasoController controller) {
        this.controller = controller;
        System.out.println("INFO Controller establecido en paso3");

        if (controller != null) {
            cargarResumen();
            validarEstadoTraspaso();
        } else {
            mostrarErrorSinController();
        }
    }

    // NUEVO MÉTODO - Establecer modal controller para poder cerrar el modal
    public void setModalController(ModalController modalController) {
        this.modalController = modalController;
        System.out.println("INFO ModalController establecido en paso3");
    }

    // NUEVO MÉTODO - Cerrar el modal
    private boolean modalYaCerrado = false;

    private void cerrarModal() {
        // Prevenir múltiples intentos de cierre
        if (modalYaCerrado) {
            System.out.println("INFO Modal ya cerrado previamente - ignorando intento duplicado");
            return;
        }

        System.out.println("INFO ========== INICIANDO CIERRE DE MODAL ==========");

        if (modalController != null) {
            System.out.println("INFO Usando ModalController para cerrar");
            cerrarConController(modalController);
        } else {
            System.out.println("WARNING ModalController es null, buscando en padres...");

            // INTENTO 1: Buscar en la jerarquía de componentes el panel creartraspaso
            java.awt.Component parent = this.getParent();
            while (parent != null) {
                if (parent instanceof creartraspaso) {
                    System.out.println("INFO Padre 'creartraspaso' encontrado");
                    creartraspaso cp = (creartraspaso) parent;
                    ModalController parentMc = cp.getModalController();

                    if (parentMc != null) {
                        System.out.println("SUCCESS ModalController recuperado del padre creartraspaso");
                        cerrarConController(parentMc);
                        return;
                    } else {
                        System.out.println("WARNING El padre creartraspaso también tiene modalController null");
                    }
                    break; // Encontramos el padre pero no el controller, paramos de buscar este tipo
                }
                parent = parent.getParent();
            }

            // INTENTO 2: Fallback tradicional buscando JDialog
            System.out.println("INFO Buscando modal dialog padre (Fallback)");
            java.awt.Window window = javax.swing.SwingUtilities.getWindowAncestor(this);
            System.out.println("INFO Window encontrado: " + (window != null ? window.getClass().getName() : "null"));

            if (window != null && window instanceof javax.swing.JDialog) {
                System.out.println("INFO Cerrando JDialog modal encontrado");
                try {
                    window.dispose();
                    modalYaCerrado = true;
                    System.out.println("SUCCESS JDialog cerrado exitosamente");
                } catch (Exception ex) {
                    System.err.println("ERROR Error al cerrar JDialog: " + ex.getMessage());
                    ex.printStackTrace();
                }
            } else {
                System.err.println("ERROR No se pudo cerrar el modal");
                System.err.println("  - ModalController: null (local y padre)");
                System.err.println("  - Window tipo: " + (window != null ? window.getClass().getName() : "null"));
            }
        }
    }

    private void cerrarConController(ModalController mc) {
        System.out.println("INFO Cerrando con ModalController: " + mc.getClass().getName());
        try {
            mc.close();
            modalYaCerrado = true;
            System.out.println("SUCCESS Modal cerrado exitosamente via ModalController");
        } catch (Exception ex) {
            System.err.println("ERROR Error al cerrar via ModalController: " + ex.getMessage());
            ex.printStackTrace();
        }

        System.out.println("INFO ========== FIN CIERRE DE MODAL ==========");
    }

    private void cargarResumen() {
        if (controller == null) {
            mostrarErrorSinController();
            return;
        }

        if (controller.getTraspasoActual() == null) {
            mostrarError("Error: Traspaso no inicializado");
            return;
        }

        System.out.println("INFO Cargando resumen del traspaso...");

        // Verificar que hay datos básicos
        if (!controller.tieneDatosBasicos()) {
            mostrarAdvertencia("Complete los datos del Paso 1 antes de continuar");
            return;
        }

        // Generar y mostrar resumen
        String resumen = controller.generarResumenTraspaso();

        if (resumen != null && !resumen.trim().isEmpty()) {
            if (resumenArea != null) {
                resumenArea.setText(resumen);
                resumenArea.setCaretPosition(0); // Mover al inicio
            }

            // También mostrar en consola para debug
            System.out.println("INFO Resumen generado:");
            System.out.println(resumen);

            // Actualizar título con información del traspaso
            actualizarTitulo();

        } else {
            mostrarError("No se pudo generar el resumen del traspaso");
        }
    }

    // NUEVO MÉTODO - Actualizar título con información básica
    private void actualizarTitulo() {
        if (controller != null && controller.getTraspasoActual() != null) {
            String numeroTraspaso = controller.getTraspasoActual().getNumeroTraspaso();
            int cantidadProductos = controller.getCantidadTotalProductos();

            String titulo = "Confirmación del Traspaso";
            if (numeroTraspaso != null) {
                titulo += " - " + numeroTraspaso;
            }
            if (cantidadProductos > 0) {
                titulo += " (" + cantidadProductos + " productos)";
            }

            if (jLabel1 != null) {
                jLabel1.setText(titulo);
            }
        }
    }

    // NUEVO MÉTODO - Validar estado del traspaso
    private void validarEstadoTraspaso() {
        if (controller == null || controller.getTraspasoActual() == null) {
            return;
        }

        boolean datosBasicos = controller.tieneDatosBasicos();
        boolean tieneProductos = controller.tieneProductos();
        boolean esValido = controller.validarTraspasoCompleto();

        System.out.println("INFO Estado del traspaso:");
        System.out.println("   - Datos básicos: " + (datosBasicos ? "OK" : "FAIL"));
        System.out.println("   - Productos: " + (tieneProductos ? "OK" : "FAIL"));
        System.out.println("   - Válido: " + (esValido ? "OK" : "FAIL"));

        // Mostrar advertencias específicas
        if (!datosBasicos) {
            mostrarAdvertencia("Complete los datos del Paso 1");
        } else if (!tieneProductos) {
            mostrarAdvertencia("Agregue productos en el Paso 2");
        }
    }

    // NUEVO MÉTODO - Refrescar vista
    public void refrescarVista() {
        cargarResumen();
        validarEstadoTraspaso();
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

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        resumenArea = new javax.swing.JTextArea();

        jLabel1.setText("Confirmación del Traspaso");

        resumenArea.setEditable(false);
        resumenArea.setColumns(20);
        resumenArea.setRows(5);
        jScrollPane1.setViewportView(resumenArea);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addGroup(jPanel1Layout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, 679, Short.MAX_VALUE)
                                        .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING,
                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap(23, Short.MAX_VALUE)));
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 45,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 489,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE)));
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Método para verificar si hay datos pendientes antes de cerrar
     */
    public boolean hayDatosPendientes() {
        if (controller == null || controller.getTraspasoActual() == null) {
            return false;
        }

        // Verificar si el traspaso ha sido confirmado o no
        // Si ya está confirmado, no se considera como pendiente
        // Si no ha sido confirmado pero hay datos, sí es pendiente
        boolean traspasoConfirmado = controller.getTraspasoActual().isConfirmado();
        System.out.println("INFO Estado de confirmación del traspaso: " + traspasoConfirmado);
        return !traspasoConfirmado;
    }

    /**
     * Método para mostrar confirmación de cierre si hay datos pendientes
     */
    public boolean confirmarCierre() {
        if (hayDatosPendientes()) {
            int opcion = JOptionPane.showConfirmDialog(
                    this,
                    "¿Está seguro de que desea salir?\n" +
                            "El traspaso no ha sido confirmado y se perderán los datos.",
                    "Confirmar salida",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            return opcion == JOptionPane.YES_OPTION;
        }
        return true; // No hay datos pendientes, se puede cerrar directamente
    }

    // Variables declaration - do modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea resumenArea;

    // End of variables declaration//GEN-END:variables
    private static class CircularSpinnerIcon implements javax.swing.Icon, java.awt.event.ActionListener {
        private final int size;
        private final java.awt.Color color;
        private float angle;
        private javax.swing.Timer timer;
        private javax.swing.JComponent target;

        CircularSpinnerIcon(int size, java.awt.Color color) {
            this.size = size;
            this.color = color;
        }

        void start(javax.swing.JComponent target) {
            this.target = target;
            if (timer != null && timer.isRunning())
                return;
            timer = new javax.swing.Timer(16, this);
            timer.start();
        }

        void stop() {
            if (timer != null) {
                timer.stop();
            }
            timer = null;
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            angle += 6f;
            if (angle >= 360f)
                angle = 0f;
            if (target != null)
                target.repaint();
        }

        @Override
        public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            java.awt.geom.Arc2D arc = new java.awt.geom.Arc2D.Float(x, y, size, size, angle, 270,
                    java.awt.geom.Arc2D.OPEN);
            g2.setStroke(new java.awt.BasicStroke(2));
            g2.draw(arc);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
