/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package raven.application.form.productos.traspasos;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import raven.clases.productos.Bodega;
import raven.clases.productos.TraspasoDatos;
import raven.clases.productos.TraspasoConfig;
import raven.controlador.productos.TraspasoController;
import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import java.util.Map;
import java.util.HashMap;
import raven.controlador.admin.SessionManager;
import raven.controlador.principal.conexion;
import java.sql.*;

public class paso1 extends javax.swing.JPanel {

    private TraspasoController controller;
    List<Bodega> bodegas = new ArrayList<Bodega>();
    Map<String, Object> datos = new HashMap<String, Object>();
    private static final String PANEL = "arc:35;background:$Login.background";
    private static final String Camposdetexto = "arc:15;background:lighten($Menu.background,25%)";

    public paso1() {
        initComponents();
        configurarComponentes();
        panelPasos.putClientProperty(FlatClientProperties.STYLE, PANEL);

    }

    public paso1(TraspasoController controller) {
        this.controller = controller;
        initComponents();
        configurarComponentes();
        panelPasos.putClientProperty(FlatClientProperties.STYLE, PANEL);
        // Cargar datos automáticamente al crear el paso
        SwingUtilities.invokeLater(() -> {
            cargarDatos();
        });
    }

    // MÉTODO MEJORADO - setController con inicialización automática
    public void setController(TraspasoController controller) {
        this.controller = controller;
        System.out.println("INFO Controller establecido en paso1");

        if (controller != null) {
            // Cargar datos automáticamente cuando se establece el controller
            SwingUtilities.invokeLater(() -> {
                cargarDatos();
            });
        }
    }

    private void configurarComponentes() {
        // Configurar fecha actual
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        txtFecha.setText(sdf.format(new Date()));
        txtFecha.setEditable(false);

        // Hacer el número de traspaso no editable
        txtNumTrasp.setEditable(false);

        // Configurar tipos de traspaso
        cbxTipoTras.removeAllItems();
        cbxTipoTras.addItem("Seleccionar");
        cbxTipoTras.addItem("Envio a venta");//
        cbxTipoTras.addItem("Reposición de stock");
        cbxTipoTras.addItem("Reorganización");
        cbxTipoTras.addItem("Urgente");
        cbxTipoTras.addItem("Temporada");
        cbxTipoTras.addItem("Liquidación");

        // Configurar áreas de texto
        txtAMotivo.setLineWrap(true);
        txtAMotivo.setWrapStyleWord(true);
        txtObservaciones.setLineWrap(true);
        txtObservaciones.setWrapStyleWord(true);

        // NUEVO: Configurar listeners para guardado automático
        configurarListenersGuardadoAutomatico();

        cargarUsuariosSolicitantes();

        System.out.println("SUCCESS Componentes configurados correctamente");
    }
    // LISTENERS PARA GUARDADO AUTOMÁTICO EN ÁREAS DE TEXTO
    // Agregar estos listeners en configurarComponentes()

    private javax.swing.Timer debounceTimer;

    private void configurarListenersGuardadoAutomatico() {
        // Inicializar timer de debounce (500ms de retraso)
        debounceTimer = new javax.swing.Timer(500, e -> {
            SwingUtilities.invokeLater(() -> intentarGuardadoAutomatico());
        });
        debounceTimer.setRepeats(false);

        javax.swing.event.DocumentListener docListener = new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                restartTimer();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                restartTimer();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                restartTimer();
            }
        };

        // Listener para el motivo
        txtAMotivo.getDocument().addDocumentListener(docListener);

        // Listener para observaciones
        txtObservaciones.getDocument().addDocumentListener(docListener);
    }

    private void restartTimer() {
        if (debounceTimer.isRunning()) {
            debounceTimer.restart();
        } else {
            debounceTimer.start();
        }
    }

    private void cargarDatos() {
        if (controller != null) {
            try {
                System.out.println("INFO Cargando datos iniciales del Paso 1...");

                // Generar número de traspaso automáticamente
                String numeroTraspaso = controller.generarNumeroTraspaso();
                txtNumTrasp.setText(numeroTraspaso);
                txtNumTrasp.setEditable(false);

                System.out.println("SUCCESS Número de traspaso generado: " + numeroTraspaso);

                // Cargar bodegas
                cargarBodegas();

                // Cargar datos previos si existen
                cargarDatosPrevios();

                aplicarConfiguracionPersistida();

                System.out.println("SUCCESS Datos iniciales cargados correctamente");

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Error al cargar datos iniciales: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            System.out.println("ERROR Controller no inicializado");
            JOptionPane.showMessageDialog(this,
                    "Error: Controller no inicializado",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void aplicarConfiguracionPersistida() {
        TraspasoConfig cfg = TraspasoConfig.load();
        if (cfg.getTipo() != null) {
            for (int i = 0; i < cbxTipoTras.getItemCount(); i++) {
                String it = (String) cbxTipoTras.getItemAt(i);
                if (it.equals(cfg.getTipo())) {
                    cbxTipoTras.setSelectedIndex(i);
                    break;
                }
            }
        }
        if (cfg.getMotivo() != null) {
            txtAMotivo.setText(cfg.getMotivo());
        }
        if (cfg.getIdOrigen() != null) {
            for (int i = 0; i < cbxOrigen.getItemCount(); i++) {
                Object it = cbxOrigen.getItemAt(i);
                if (it instanceof Bodega) {
                    Bodega b = (Bodega) it;
                    if (b.getIdBodega() != null && b.getIdBodega().intValue() == cfg.getIdOrigen().intValue()) {
                        cbxOrigen.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }
        if (cfg.getIdDestino() != null) {
            for (int i = 0; i < cbxDestino.getItemCount(); i++) {
                Object it = cbxDestino.getItemAt(i);
                if (it instanceof Bodega) {
                    Bodega b = (Bodega) it;
                    if (b.getIdBodega() != null && b.getIdBodega().intValue() == cfg.getIdDestino().intValue()) {
                        cbxDestino.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }
        if (cfg.getIdUsuarioSolicita() != null) {
            for (int i = 0; i < cbxUsuario_solicita.getItemCount(); i++) {
                Object it = cbxUsuario_solicita.getItemAt(i);
                if (it instanceof UsuarioItem) {
                    UsuarioItem u = (UsuarioItem) it;
                    if (u.id == cfg.getIdUsuarioSolicita().intValue()) {
                        cbxUsuario_solicita.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }
        if (cfg.isAutoApply()) {
            if (datosCompletos()) {
                guardarDatos();
            }
        }
    }

    private void cargarBodegas() {
        System.out.println("INFO Cargando bodegas (Async)...");
        new SwingWorker<List<Bodega>, Void>() {
            @Override
            protected List<Bodega> doInBackground() throws Exception {
                return controller.cargarBodegas();
            }

            @Override
            protected void done() {
                try {
                    List<Bodega> bodegas = get();
                    if (bodegas != null && !bodegas.isEmpty()) {
                        @SuppressWarnings({ "unchecked", "rawtypes" })
                        JComboBox comboBodegaOrigen = cbxOrigen;

                        @SuppressWarnings({ "unchecked", "rawtypes" })
                        JComboBox comboBodegaDestino = cbxDestino;

                        // Limpiar combos
                        comboBodegaOrigen.removeAllItems();
                        comboBodegaDestino.removeAllItems();

                        // Agregar opción por defecto
                        comboBodegaOrigen.addItem(null);
                        comboBodegaDestino.addItem(null);

                        // Agregar bodegas a ambos combos
                        for (Bodega bodega : bodegas) {
                            comboBodegaOrigen.addItem(bodega);
                            comboBodegaDestino.addItem(bodega);
                            System.out.println("   + " + bodega.getNombre() + " (ID: " + bodega.getIdBodega() + ")");
                        }

                        // Configurar renderer
                        configurarRendererBodegas();

                        System.out.println("SUCCESS " + bodegas.size() + " bodegas cargadas correctamente");

                        // Cargar datos previos y aplicar config persistida
                        cargarDatosPrevios();

                        // Solo aplicar configuración persistida si NO hay datos previos cargados
                        if (controller == null || controller.getTraspasoActual() == null ||
                                controller.getTraspasoActual().getIdBodegaOrigen() == null) {
                            aplicarConfiguracionPersistida();
                        } else {
                            System.out.println(
                                    "INFO Omitiendo configuración persistida porque se cargaron datos existentes del traspaso");
                        }

                    } else {
                        System.out.println("ERROR No se pudieron cargar las bodegas");
                        JOptionPane.showMessageDialog(paso1.this,
                                "Error al cargar las bodegas",
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("ERROR Error cargando bodegas: " + e.getMessage());
                    JOptionPane.showMessageDialog(paso1.this,
                            "Error al cargar bodegas: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void configurarRendererBodegas() {
        // Renderer usando raw types
        @SuppressWarnings({ "unchecked", "rawtypes" })
        ListCellRenderer renderer = new ListCellRenderer() {
            private final DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

            @Override
            public Component getListCellRendererComponent(JList list,
                    Object value, int index, boolean isSelected, boolean cellHasFocus) {

                String text;
                if (value == null) {
                    // Determinar si es origen o destino basándose en el componente
                    if (list.getParent().getParent() == cbxOrigen) {
                        text = "Seleccionar origen";
                    } else {
                        text = "Seleccionar destino";
                    }
                } else if (value instanceof Bodega) {
                    text = value.toString(); // Usa el toString() de Bodega
                } else {
                    text = value.toString();
                }

                return defaultRenderer.getListCellRendererComponent(
                        list, text, index, isSelected, cellHasFocus);
            }
        };
        // Aplicar renderer usando raw types
        @SuppressWarnings({ "unchecked", "rawtypes" })
        JComboBox comboBodegaOrigen = cbxOrigen;
        @SuppressWarnings({ "unchecked", "rawtypes" })
        JComboBox comboBodegaDestino = cbxDestino;

        comboBodegaOrigen.setRenderer(renderer);
        comboBodegaDestino.setRenderer(renderer);

    }

    private void cargarDatosPrevios() {
        if (controller != null && controller.getTraspasoActual() != null) {
            TraspasoDatos traspaso = controller.getTraspasoActual();

            System.out.println("INFO Cargando datos previos del traspaso en paso1...");

            // Cargar número de traspaso si ya existe
            if (traspaso.getNumeroTraspaso() != null) {
                txtNumTrasp.setText(traspaso.getNumeroTraspaso());
                System.out.println("   SUCCESS Número cargado: " + traspaso.getNumeroTraspaso());
            }

            // CORRECCIÓN CRÍTICA: Cargar bodega origen
            if (traspaso.getIdBodegaOrigen() != null) {
                System.out.println("   SEARCH Buscando bodega origen ID: " + traspaso.getIdBodegaOrigen());

                boolean encontradaOrigen = false;
                for (int i = 1; i < cbxOrigen.getItemCount(); i++) { // Empezar en 1 para saltar "Todas las bodegas"
                    Object item = cbxOrigen.getItemAt(i);
                    if (item instanceof Bodega) {
                        Bodega bodega = (Bodega) item;
                        if (bodega.getIdBodega().equals(traspaso.getIdBodegaOrigen())) {
                            cbxOrigen.setSelectedIndex(i);
                            encontradaOrigen = true;
                            System.out.println("   SUCCESS Bodega origen cargada: " + bodega.getNombre());
                            break;
                        }
                    }
                }

                if (!encontradaOrigen) {
                    System.out
                            .println("   WARNING No se encontró bodega origen con ID: " + traspaso.getIdBodegaOrigen());
                }
            }

            // CORRECCIÓN CRÍTICA: Cargar bodega destino
            if (traspaso.getIdBodegaDestino() != null) {
                System.out.println("   SEARCH Buscando bodega destino ID: " + traspaso.getIdBodegaDestino());

                boolean encontradaDestino = false;
                for (int i = 1; i < cbxDestino.getItemCount(); i++) { // Empezar en 1 para saltar "Todas las bodegas"
                    Object item = cbxDestino.getItemAt(i);
                    if (item instanceof Bodega) {
                        Bodega bodega = (Bodega) item;
                        if (bodega.getIdBodega().equals(traspaso.getIdBodegaDestino())) {
                            cbxDestino.setSelectedIndex(i);
                            encontradaDestino = true;
                            System.out.println("   SUCCESS Bodega destino cargada: " + bodega.getNombre());
                            break;
                        }
                    }
                }

                if (!encontradaDestino) {
                    System.out.println(
                            "   WARNING No se encontró bodega destino con ID: " + traspaso.getIdBodegaDestino());
                }
            }

            // Cargar tipo de traspaso
            if (traspaso.getTipoTraspaso() != null) {
                // Buscar el tipo en el combo
                boolean tipoEncontrado = false;
                for (int i = 0; i < cbxTipoTras.getItemCount(); i++) {
                    String item = (String) cbxTipoTras.getItemAt(i);
                    if (item.equals(traspaso.getTipoTraspaso())) {
                        cbxTipoTras.setSelectedIndex(i);
                        tipoEncontrado = true;
                        System.out.println("   SUCCESS Tipo cargado: " + traspaso.getTipoTraspaso());
                        break;
                    }
                }

                if (!tipoEncontrado) {
                    System.out.println(
                            "   WARNING Tipo no encontrado: " + traspaso.getTipoTraspaso() + ", usando por defecto");
                    cbxTipoTras.setSelectedItem("Reposición de stock");
                }
            }

            // Cargar motivo y observaciones
            if (traspaso.getMotivoTraspaso() != null) {
                txtAMotivo.setText(traspaso.getMotivoTraspaso());
                System.out.println("   SUCCESS Motivo cargado: "
                        + (traspaso.getMotivoTraspaso().length() > 50
                                ? traspaso.getMotivoTraspaso().substring(0, 50) + "..."
                                : traspaso.getMotivoTraspaso()));
            }

            if (traspaso.getObservaciones() != null && !traspaso.getObservaciones().trim().isEmpty()) {
                txtObservaciones.setText(traspaso.getObservaciones());
                System.out.println("   SUCCESS Observaciones cargadas");
            }

            // NUEVO: Actualizar fecha a la actual para mostrar "última modificación"
            actualizarFecha();

            System.out.println("SUCCESS Datos previos cargados correctamente en paso1");

            // Forzar repintado de los componentes
            SwingUtilities.invokeLater(() -> {
                cbxOrigen.repaint();
                cbxDestino.repaint();
                cbxTipoTras.repaint();
                this.revalidate();
                this.repaint();
            });
        } else {
            System.out.println("WARNING No hay datos previos para cargar en paso1");
        }
    }

    // MÉTODO NUEVO - Validar datos cargados
    public boolean validarDatosCargados() {
        if (controller == null || controller.getTraspasoActual() == null) {
            System.out.println("ERROR No hay controller o traspaso actual");
            return false;
        }

        TraspasoDatos traspaso = controller.getTraspasoActual();

        boolean valido = true;
        StringBuilder problemas = new StringBuilder();

        // Verificar campos críticos
        if (traspaso.getNumeroTraspaso() == null || traspaso.getNumeroTraspaso().trim().isEmpty()) {
            problemas.append("- Número de traspaso faltante\n");
            valido = false;
        }

        if (traspaso.getIdBodegaOrigen() == null) {
            problemas.append("- Bodega origen faltante\n");
            valido = false;
        }

        if (traspaso.getIdBodegaDestino() == null) {
            problemas.append("- Bodega destino faltante\n");
            valido = false;
        }

        if (traspaso.getTipoTraspaso() == null || traspaso.getTipoTraspaso().trim().isEmpty()) {
            problemas.append("- Tipo de traspaso faltante\n");
            valido = false;
        }

        // Motivo ahora es opcional
        // if (traspaso.getMotivoTraspaso() == null ||
        // traspaso.getMotivoTraspaso().trim().length() < 10) {
        // problemas.append("- Motivo insuficiente\n");
        // valido = false;
        // }

        if (!valido) {
            System.out.println("ERROR Problemas con datos cargados:\n" + problemas.toString());
        } else {
            System.out.println("SUCCESS Datos cargados válidos");
        }

        return valido;
    }

    public boolean validarDatos() {
        System.out.println("INFO Validando datos del Paso 1...");

        String numeroTraspaso = txtNumTrasp.getText().trim();
        Bodega bodegaOrigen = null;
        Bodega bodegaDestino = null;

        // Obtener bodega origen seleccionada
        if (cbxOrigen.getSelectedItem() != null) {
            Object item = cbxOrigen.getSelectedItem();
            if (item instanceof Bodega) {
                bodegaOrigen = (Bodega) item;
                System.out.println("   INFO Bodega origen: " + bodegaOrigen.getNombre() + " (ID: "
                        + bodegaOrigen.getIdBodega() + ")");
            } else if (cbxOrigen.getSelectedIndex() > 0) {
                // Fallback para cuando no es instancia directa (raro pero posible)
                System.out.println("   WARNING Item origen no es instancia Bodega: " + item.getClass().getName());
            }
        } else {
            System.out.println("   WARNING Bodega origen no seleccionada (item null)");
        }

        // Obtener bodega destino seleccionada
        if (cbxDestino.getSelectedItem() != null) {
            Object item = cbxDestino.getSelectedItem();
            if (item instanceof Bodega) {
                bodegaDestino = (Bodega) item;
                System.out.println("   INFO Bodega destino: " + bodegaDestino.getNombre() + " (ID: "
                        + bodegaDestino.getIdBodega() + ")");
            } else if (cbxDestino.getSelectedIndex() > 0) {
                System.out.println("   WARNING Item destino no es instancia Bodega: " + item.getClass().getName());
            }
        } else {
            System.out.println("   WARNING Bodega destino no seleccionada (item null)");
        }

        String tipoTraspaso = (String) cbxTipoTras.getSelectedItem();
        String motivo = txtAMotivo.getText().trim();

        System.out.println("   - Número: " + numeroTraspaso);
        System.out.println("   - Tipo: " + tipoTraspaso);
        System.out.println("   - Motivo: " + (motivo.length() > 50 ? motivo.substring(0, 50) + "..." : motivo));

        boolean valido = controller.validarDatosPaso1(numeroTraspaso, bodegaOrigen,
                bodegaDestino, tipoTraspaso, motivo);

        System.out.println("   Resultado validación: " + (valido ? "VÁLIDO" : "INVÁLIDO"));

        return valido;
    }

    public void guardarDatos() {
        System.out.println("SAVE Guardando datos del Paso 1...");

        if (controller != null) {
            String numeroTraspaso = txtNumTrasp.getText().trim();
            Bodega bodegaOrigen = null;
            Bodega bodegaDestino = null;

            // Obtener bodega origen seleccionada
            if (cbxOrigen.getSelectedIndex() > 0) {
                Object item = cbxOrigen.getSelectedItem();
                if (item instanceof Bodega) {
                    bodegaOrigen = (Bodega) item;
                }
            }

            // Obtener bodega destino seleccionada
            if (cbxDestino.getSelectedIndex() > 0) {
                Object item = cbxDestino.getSelectedItem();
                if (item instanceof Bodega) {
                    bodegaDestino = (Bodega) item;
                }
            }

            String tipoTraspaso = (String) cbxTipoTras.getSelectedItem();
            String motivo = txtAMotivo.getText().trim();
            String observaciones = txtObservaciones.getText().trim();

            // CORRECCIÓN PRINCIPAL: Verificar que los datos estén completos antes de
            // guardar
            if (bodegaOrigen != null && bodegaDestino != null
                    && !numeroTraspaso.isEmpty() && !"Seleccionar".equals(tipoTraspaso)) {
                // && !motivo.isEmpty()) { // Motivo opcional

                controller.guardarDatosPaso1(numeroTraspaso, bodegaOrigen, bodegaDestino,
                        tipoTraspaso, motivo, observaciones);

                System.out.println("SUCCESS Datos guardados en el controller:");
                System.out.println(
                        "   - Bodega origen guardada: " + controller.getTraspasoActual().getNombreBodegaOrigen());
                System.out.println(
                        "   - Bodega destino guardada: " + controller.getTraspasoActual().getNombreBodegaDestino());
                System.out.println("   - Número: " + controller.getTraspasoActual().getNumeroTraspaso());
                System.out.println("   - Tipo: " + controller.getTraspasoActual().getTipoTraspaso());

                // Notificar que los datos están listos
                firePropertyChange("datosCompletados", false, true);

            } else {
                System.out.println("ERROR Datos incompletos, no se pueden guardar");
            }
        } else {
            System.out.println("ERROR Controller no disponible");
            JOptionPane.showMessageDialog(this,
                    "Error: Controller no inicializado",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void actualizarFecha() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        txtFecha.setText(sdf.format(new Date()));
    }

    // MÉTODO MEJORADO - hayCambiosPendientes con mejor detección
    public boolean hayCambiosPendientes() {
        if (controller == null || controller.getTraspasoActual() == null) {
            // Si no hay controller, verificar si hay datos en los campos
            boolean hayCambios = !txtNumTrasp.getText().trim().isEmpty()
                    || cbxOrigen.getSelectedIndex() > 0
                    || cbxDestino.getSelectedIndex() > 0
                    || !cbxTipoTras.getSelectedItem().equals("Seleccionar")
                    || !txtAMotivo.getText().trim().isEmpty()
                    || !txtObservaciones.getText().trim().isEmpty();

            System.out.println("INFO Verificando cambios pendientes (sin controller): " + hayCambios);
            return hayCambios;
        }

        TraspasoDatos traspaso = controller.getTraspasoActual();

        boolean numeroChanged = !txtNumTrasp.getText().trim().equals(
                traspaso.getNumeroTraspaso() != null ? traspaso.getNumeroTraspaso() : "");
        boolean motivoChanged = !txtAMotivo.getText().trim().equals(
                traspaso.getMotivoTraspaso() != null ? traspaso.getMotivoTraspaso() : "");
        boolean observacionesChanged = !txtObservaciones.getText().trim().equals(
                traspaso.getObservaciones() != null ? traspaso.getObservaciones() : "");

        // Verificar cambios en bodegas
        boolean origenChanged = false;
        boolean destinoChanged = false;

        if (cbxOrigen.getSelectedIndex() > 0) {
            Object item = cbxOrigen.getSelectedItem();
            if (item instanceof Bodega) {
                Bodega bodegaSeleccionada = (Bodega) item;
                origenChanged = !bodegaSeleccionada.getIdBodega().equals(traspaso.getIdBodegaOrigen());
            }
        } else {
            origenChanged = traspaso.getIdBodegaOrigen() != null;
        }

        if (cbxDestino.getSelectedIndex() > 0) {
            Object item = cbxDestino.getSelectedItem();
            if (item instanceof Bodega) {
                Bodega bodegaSeleccionada = (Bodega) item;
                destinoChanged = !bodegaSeleccionada.getIdBodega().equals(traspaso.getIdBodegaDestino());
            }
        } else {
            destinoChanged = traspaso.getIdBodegaDestino() != null;
        }

        boolean tipoChanged = false;
        if (cbxTipoTras.getSelectedIndex() > 0 && !"Seleccionar".equals(cbxTipoTras.getSelectedItem())) {
            String tipoSeleccionado = (String) cbxTipoTras.getSelectedItem();
            tipoChanged = !tipoSeleccionado.equals(traspaso.getTipoTraspaso());
        } else {
            tipoChanged = traspaso.getTipoTraspaso() != null;
        }

        boolean hayCambios = numeroChanged || motivoChanged || observacionesChanged
                || origenChanged || destinoChanged || tipoChanged;

        System.out.println("INFO Verificando cambios pendientes: " + hayCambios);
        if (hayCambios) {
            System.out.println("   - Número: " + numeroChanged);
            System.out.println("   - Motivo: " + motivoChanged);
            System.out.println("   - Observaciones: " + observacionesChanged);
            System.out.println("   - Origen: " + origenChanged);
            System.out.println("   - Destino: " + destinoChanged);
            System.out.println("   - Tipo: " + tipoChanged);
        }

        return hayCambios;
    }

    public boolean datosCompletos() {
        boolean completos = cbxOrigen.getSelectedIndex() > 0
                && cbxDestino.getSelectedIndex() > 0
                && cbxTipoTras.getSelectedIndex() > 0
                && !"Seleccionar".equals(cbxTipoTras.getSelectedItem())
                && !txtNumTrasp.getText().trim().isEmpty()
                && !txtNumTrasp.getText().trim().isEmpty();
        // && txtAMotivo.getText().trim().length() >= 10; // Motivo opcional

        System.out.println("INFO Datos completos: " + completos);
        return completos;
    }

    // NUEVO MÉTODO - Obtener resumen de los datos actuales
    public String obtenerResumenDatos() {
        StringBuilder resumen = new StringBuilder();
        resumen.append("DATOS DEL PASO 1:\n");
        resumen.append("Número: ").append(txtNumTrasp.getText()).append("\n");

        if (cbxOrigen.getSelectedIndex() > 0) {
            Object origen = cbxOrigen.getSelectedItem();
            if (origen instanceof Bodega) {
                resumen.append("Origen: ").append(((Bodega) origen).getNombre()).append("\n");
            }
        }

        if (cbxDestino.getSelectedIndex() > 0) {
            Object destino = cbxDestino.getSelectedItem();
            if (destino instanceof Bodega) {
                resumen.append("Destino: ").append(((Bodega) destino).getNombre()).append("\n");
            }
        }

        resumen.append("Tipo: ").append(cbxTipoTras.getSelectedItem()).append("\n");
        resumen.append("Motivo: ").append(txtAMotivo.getText().substring(0,
                Math.min(50, txtAMotivo.getText().length()))).append("...\n");

        return resumen.toString();
    }

    private void intentarGuardadoAutomatico() {
        if (controller == null) {
            return;
        }

        // Verificar si hay datos mínimos para guardar
        String numeroTraspaso = txtNumTrasp.getText().trim();
        String motivo = txtAMotivo.getText().trim();
        boolean tieneOrigen = cbxOrigen.getSelectedIndex() > 0;
        boolean tieneDestino = cbxDestino.getSelectedIndex() > 0;
        boolean tieneTipo = cbxTipoTras.getSelectedIndex() > 0
                && !"Seleccionar".equals(cbxTipoTras.getSelectedItem());

        if (!numeroTraspaso.isEmpty() && tieneOrigen && tieneDestino && tieneTipo) {
            System.out.println("SAVE Datos suficientes detectados, guardando automáticamente...");
            guardarDatos();
        } else {
            System.out.println("INFO Datos insuficientes para guardado automático:");
            System.out.println("   - Número: " + (!numeroTraspaso.isEmpty()));
            System.out.println("   - Origen: " + tieneOrigen);
            System.out.println("   - Destino: " + tieneDestino);
            System.out.println("   - Tipo: " + tieneTipo);
            System.out.println("   - Motivo: " + (motivo.length() >= 10));
        }
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

        panelPasos = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        txtNumTrasp = new javax.swing.JTextField();
        txtFecha = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        cbxOrigen = new javax.swing.JComboBox<>();
        jLabel8 = new javax.swing.JLabel();
        cbxDestino = new javax.swing.JComboBox<>();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        cbxTipoTras = new javax.swing.JComboBox<>();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtAMotivo = new javax.swing.JTextArea();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        txtObservaciones = new javax.swing.JTextArea();
        jLabel13 = new javax.swing.JLabel();
        cbxUsuario_solicita = new javax.swing.JComboBox<>();

        jLabel4.setText("Información general del traspaso");

        jLabel5.setText("numero de traspaso");

        jLabel6.setText("Fecha de solicitud");

        cbxOrigen.setModel(
                new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cbxOrigen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbxOrigenActionPerformed(evt);
            }
        });

        jLabel8.setText("Origen");

        cbxDestino.setModel(
                new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cbxDestino.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbxDestinoActionPerformed(evt);
            }
        });

        jLabel9.setText("Destino");

        jLabel10.setText("Tipo de traspaso");

        cbxTipoTras.setModel(
                new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cbxTipoTras.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbxTipoTrasActionPerformed(evt);
            }
        });

        txtAMotivo.setColumns(20);
        txtAMotivo.setRows(5);
        jScrollPane2.setViewportView(txtAMotivo);

        jLabel11.setText("Motivo del traspaso");

        jLabel12.setText("Observaciones adicionales");

        txtObservaciones.setColumns(20);
        txtObservaciones.setRows(5);
        jScrollPane3.setViewportView(txtObservaciones);

        jLabel13.setText("Solicitado por");

        cbxUsuario_solicita.setModel(
                new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cbxUsuario_solicita.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbxUsuario_solicitaActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelPasosLayout = new javax.swing.GroupLayout(panelPasos);
        panelPasos.setLayout(panelPasosLayout);
        panelPasosLayout.setHorizontalGroup(
                panelPasosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelPasosLayout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addGroup(panelPasosLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelPasosLayout.createSequentialGroup()
                                                .addGroup(panelPasosLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addComponent(jLabel8,
                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 122,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING,
                                                                panelPasosLayout.createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.TRAILING,
                                                                        false)
                                                                        .addComponent(txtNumTrasp)
                                                                        .addComponent(cbxOrigen, 0, 180,
                                                                                Short.MAX_VALUE))
                                                        .addComponent(jLabel5,
                                                                javax.swing.GroupLayout.Alignment.LEADING,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 122,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addGap(65, 65, 65)
                                                .addGroup(panelPasosLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                122, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addGroup(panelPasosLayout
                                                                .createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.TRAILING,
                                                                        false)
                                                                .addComponent(cbxDestino,
                                                                        javax.swing.GroupLayout.Alignment.LEADING, 0,
                                                                        200, Short.MAX_VALUE)
                                                                .addComponent(txtFecha,
                                                                        javax.swing.GroupLayout.Alignment.LEADING)
                                                                .addComponent(jLabel6,
                                                                        javax.swing.GroupLayout.Alignment.LEADING,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 122,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                .addGap(18, 18, 18)
                                                .addGroup(panelPasosLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                122, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(cbxUsuario_solicita, 0, 225, Short.MAX_VALUE)))
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelPasosLayout
                                                .createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addGroup(panelPasosLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                122, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(cbxTipoTras,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE, 163,
                                                                javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addComponent(jScrollPane2)
                                        .addComponent(jScrollPane3)
                                        .addGroup(panelPasosLayout.createSequentialGroup()
                                                .addGroup(panelPasosLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                203, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                122, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(jLabel12))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addGap(20, 20, 20)));
        panelPasosLayout.setVerticalGroup(
                panelPasosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelPasosLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 38,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(19, 19, 19)
                                .addGroup(panelPasosLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(panelPasosLayout.createSequentialGroup()
                                                .addGroup(panelPasosLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addGroup(panelPasosLayout.createSequentialGroup()
                                                                .addGroup(panelPasosLayout.createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.BASELINE)
                                                                        .addComponent(jLabel6)
                                                                        .addComponent(jLabel10))
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                .addGroup(panelPasosLayout.createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.LEADING,
                                                                        false)
                                                                        .addComponent(cbxTipoTras,
                                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                                57, Short.MAX_VALUE)
                                                                        .addComponent(txtFecha)))
                                                        .addGroup(panelPasosLayout.createSequentialGroup()
                                                                .addComponent(jLabel5)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(txtNumTrasp,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 56,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                .addGap(20, 20, 20)
                                                .addGroup(panelPasosLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(panelPasosLayout.createSequentialGroup()
                                                                .addComponent(jLabel8)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(cbxOrigen,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 57,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addGroup(panelPasosLayout.createSequentialGroup()
                                                                .addGroup(panelPasosLayout.createParallelGroup(
                                                                        javax.swing.GroupLayout.Alignment.BASELINE)
                                                                        .addComponent(jLabel9)
                                                                        .addComponent(jLabel13))
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(cbxDestino,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 57,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE))))
                                        .addComponent(cbxUsuario_solicita, javax.swing.GroupLayout.PREFERRED_SIZE, 57,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addComponent(jLabel11)
                                .addGap(10, 10, 10)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 66,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel12)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(80, 80, 80)));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(panelPasos, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(panelPasos, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
    }// </editor-fold>//GEN-END:initComponents

    private void cbxTipoTrasActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cbxTipoTrasActionPerformed
        if (cbxTipoTras.getSelectedIndex() > 0) {
            String tipoSeleccionado = (String) cbxTipoTras.getSelectedItem();
            System.out.println("INFO Tipo de traspaso seleccionado: " + tipoSeleccionado);

            // Intentar guardar si hay datos suficientes
            intentarGuardadoAutomatico();
        }
    }// GEN-LAST:event_cbxTipoTrasActionPerformed

    private void cbxDestinoActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cbxDestinoActionPerformed
        // Evento cuando cambia la selección de bodega destino
        // Opcional: validar que origen y destino no sean iguales
        if (cbxOrigen.getSelectedIndex() > 0 && cbxDestino.getSelectedIndex() > 0) {
            Object origen = cbxOrigen.getSelectedItem();
            Object destino = cbxDestino.getSelectedItem();

            if (origen instanceof Bodega && destino instanceof Bodega) {
                Bodega bodegaOrigen = (Bodega) origen;
                Bodega bodegaDestino = (Bodega) destino;

                if (bodegaOrigen.getIdBodega().equals(bodegaDestino.getIdBodega())) {
                    JOptionPane.showMessageDialog(this,
                            "La bodega de origen y destino no pueden ser la misma",
                            "Advertencia", JOptionPane.WARNING_MESSAGE);
                    cbxDestino.setSelectedIndex(0);
                }
            }
        }
    }// GEN-LAST:event_cbxDestinoActionPerformed

    private void cbxOrigenActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cbxOrigenActionPerformed
        // Validar que origen y destino no sean iguales
        if (cbxOrigen.getSelectedIndex() > 0 && cbxDestino.getSelectedIndex() > 0) {
            Object origen = cbxOrigen.getSelectedItem();
            Object destino = cbxDestino.getSelectedItem();

            if (origen instanceof Bodega && destino instanceof Bodega) {
                Bodega bodegaOrigen = (Bodega) origen;
                Bodega bodegaDestino = (Bodega) destino;

                if (bodegaOrigen.getIdBodega().equals(bodegaDestino.getIdBodega())) {
                    JOptionPane.showMessageDialog(this,
                            "La bodega de origen y destino no pueden ser la misma",
                            "Advertencia", JOptionPane.WARNING_MESSAGE);
                    cbxDestino.setSelectedIndex(0);
                    return;
                }
            }
        }

        // NUEVA FUNCIONALIDAD: Guardar automáticamente cuando se cambia bodega destino
        if (cbxDestino.getSelectedIndex() > 0) {
            Object destino = cbxDestino.getSelectedItem();
            if (destino instanceof Bodega) {
                Bodega bodegaDestino = (Bodega) destino;
                System.out.println("INFO Bodega destino seleccionada: " + bodegaDestino.getNombre());

                // Intentar guardar si hay datos suficientes
                intentarGuardadoAutomatico();
            }
        }
    }// GEN-LAST:event_cbxOrigenActionPerformed

    private void cbxUsuario_solicitaActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cbxUsuario_solicitaActionPerformed
        if (controller == null || controller.getTraspasoActual() == null)
            return;
        Object sel = cbxUsuario_solicita.getSelectedItem();
        if (sel instanceof UsuarioItem) {
            UsuarioItem u = (UsuarioItem) sel;
            controller.getTraspasoActual().setIdUsuarioSolicita(u.id);
            System.out.println("INFO Solicitante seleccionado: " + u.id + " - " + u.nombre);
        }
    }// GEN-LAST:event_cbxUsuario_solicitaActionPerformed

    /**
     * Método para mostrar confirmación de cierre si hay cambios pendientes
     */
    public boolean confirmarCierre() {
        if (hayCambiosPendientes()) {
            int opcion = JOptionPane.showConfirmDialog(
                    this,
                    "¿Está seguro de que desea salir?\n" +
                            "Se perderán los cambios no guardados.",
                    "Confirmar salida",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            return opcion == JOptionPane.YES_OPTION;
        }
        return true; // No hay cambios, se puede cerrar directamente
    }

    private static class UsuarioItem {
        int id;
        String nombre;
        String username;

        public UsuarioItem(int id, String nombre, String username) {
            this.id = id;
            this.nombre = nombre;
            this.username = username;
        }

        @Override
        public String toString() {
            return nombre + (username != null ? " (" + username + ")" : "");
        }
    }

    private void cargarUsuariosSolicitantes() {
        new SwingWorker<List<UsuarioItem>, Void>() {
            @Override
            protected List<UsuarioItem> doInBackground() throws Exception {
                List<UsuarioItem> users = new ArrayList<>();
                Integer currentId = SessionManager.getInstance().getCurrentUserId();
                Connection con = null;
                PreparedStatement ps = null;
                ResultSet rs = null;
                try {
                    con = conexion.getInstance().createConnection();
                    ps = con.prepareStatement("SELECT id_usuario, nombre, username FROM usuarios WHERE activo = 1 " +
                            (currentId != null && currentId > 0 ? "AND id_usuario <> ? " : "") + "ORDER BY nombre");
                    int idx = 1;
                    if (currentId != null && currentId > 0) {
                        ps.setInt(idx++, currentId);
                    }
                    rs = ps.executeQuery();
                    while (rs.next()) {
                        users.add(new UsuarioItem(rs.getInt("id_usuario"), rs.getString("nombre"),
                                rs.getString("username")));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (rs != null)
                        try {
                            rs.close();
                        } catch (Exception ignore) {
                        }
                    if (ps != null)
                        try {
                            ps.close();
                        } catch (Exception ignore) {
                        }
                    if (con != null)
                        try {
                            con.close();
                        } catch (Exception ignore) {
                        }
                }
                return users;
            }

            @Override
            protected void done() {
                try {
                    List<UsuarioItem> users = get();
                    DefaultComboBoxModel<Object> model = new DefaultComboBoxModel<>();
                    model.addElement(null);
                    for (UsuarioItem u : users) {
                        model.addElement(u);
                    }

                    cbxUsuario_solicita.setModel(model);
                    cbxUsuario_solicita.setRenderer(new DefaultListCellRenderer() {
                        @Override
                        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                boolean isSelected, boolean cellHasFocus) {
                            String text;
                            if (value == null)
                                text = "Seleccionar solicitante";
                            else
                                text = value.toString();
                            return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
                        }
                    });
                    System.out.println("SUCCESS Usuarios solicitantes cargados: " + users.size());
                } catch (Exception e) {
                    System.err.println("Error cargando usuarios solicitantes: " + e.getMessage());
                }
            }
        }.execute();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> cbxDestino;
    public javax.swing.JComboBox<String> cbxOrigen;
    private javax.swing.JComboBox<String> cbxTipoTras;
    private javax.swing.JComboBox<Object> cbxUsuario_solicita;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    public javax.swing.JPanel panelPasos;
    private javax.swing.JTextArea txtAMotivo;
    private javax.swing.JTextField txtFecha;
    private javax.swing.JTextField txtNumTrasp;
    private javax.swing.JTextArea txtObservaciones;
    // End of variables declaration//GEN-END:variables
}
