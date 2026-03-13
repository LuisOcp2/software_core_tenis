package raven.application.form.comercial;

import com.formdev.flatlaf.FlatClientProperties;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import raven.application.Application;
import raven.application.form.principal.generarVentaFor1;
import raven.componentes.tablareportes.ProfileTableRendererReports;
import raven.controlador.productos.ModelProduct;
public class viewReport extends javax.swing.JPanel {

    public viewReport() {
        // Inicializa los componentes de la interfaz gráfica (generados automáticamente por NetBeans u otro IDE)
        initComponents();
        init();
        configurarTabla();

    }

    private void init() {
        panelMain.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:30;" // Radio de esquina de 25px
                + "background:$Login.background;");  // Usa color de fondo de tabla
        panelup.putClientProperty(FlatClientProperties.STYLE, ""
                + "arc:25;" // Radio de esquina de 25px
                + "background:$Table.gridColor;");  // Usa color de fondo de tabla

        tituloinfo.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$h1.font;");  // Usa estilo de fuente h1
        tituloinfo1.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$h3.font;");  // Usa estilo de fuente h1
        tituloinfo2.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$h3.font;");  // Usa estilo de fuente h1
        tituloinfo.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$h3.font;");  // Usa estilo de fuente h1
        // titleVenta.putClientProperty(FlatClientProperties.STYLE, ""
        //    + "font:$h3.font;");  // Usa estilo de fuente h1
        txtCliente.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$h3.font;");  // Usa estilo de fuente h1

        txtnVenta1.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$h3.font;");  // Usa estilo de fuente h1
        txtnFecha.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$h3.font;");  // Usa estilo de fuente h1
        // titleVenta2.putClientProperty(FlatClientProperties.STYLE, ""
        //        + "font:$h3.font;");  // Usa estilo de fuente h1
        txtnFecha.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$h3.font;");  // Usa estilo de fuente h1
        txtCliente.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$h3.font;");  // Usa estilo de fuente h1
        txtDni.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$h3.font;");  // Usa estilo de fuente h1
        txtTel.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$h3.font;");  // Usa estilo de fuente h1
        txtEmail.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$h3.font;");  // Usa estilo de fuente h1
        txtDir.putClientProperty(FlatClientProperties.STYLE, ""
                + "font:$h3.font;");  // Usa estilo de fuente h1

    }

    ;
    private void configurarTabla() {
        // Estilizar la tabla
        tablaProd.putClientProperty(FlatClientProperties.STYLE, ""
                + "showHorizontalLines:true;"
                + "showVerticalLines:false;"
                + "rowHeight:70;" // Aumentado para dar espacio al contenido
                + "intercellSpacing:10,5");

        // Personalizar los encabezados de columna
        tablaProd.getTableHeader().putClientProperty(FlatClientProperties.STYLE, ""
                + "hoverBackground:$Table.background;"
                + "height:40;"
                + "separatorColor:$TableHeader.background;"
                + "font:bold $h4.font");

        // Aplicar el renderer personalizado a la columna de productos (columna 1)
        ProfileTableRendererReports renderer = new ProfileTableRendererReports(tablaProd);
        tablaProd.getColumnModel().getColumn(1).setCellRenderer(renderer);

    }
    // En la clase viewReport:

    public void setVentaInfo(int idVenta, String fecha, String estado, double total, double descuento, String metodoPago) {
        txtnVenta1.setText(String.valueOf(idVenta));
        txtnFecha.setText(fecha);
        String color;

        switch (estado) {
            case "completada":
                color = "#4CAF50"; // Verde
                break;
            case "pendiente":
                color = "#FFC107";  // Amarillo
                break;
            case "cancelada":
                color = "#F44336"; // Rojo
                break;
            default:
                color = "#9E9E9E";        // Gris
        }
        if (estado != null) {

            txtEstado.setText("<html><div style='border-radius: 10px; display: inline-block; "
                    + "font-size: 11px; font-weight: bold; "
                    + "background-color: " + color + "; "
                    + "color: white;'>"
                    + estado + "</div></html>");
            
            // Deshabilitar botón de edición si el estado no permite edición
            if (estado.equalsIgnoreCase("cancelada") || 
                estado.equalsIgnoreCase("cotizacion_rechazada") || 
                estado.equalsIgnoreCase("cotizacion_convertida")) {
                btnAdd.setEnabled(false);
            } else {
                btnAdd.setEnabled(true);
            }
        }

        txtTotal.setText(String.format("$%,.2f", total));
        txtDescuento.setText(String.format("$%,.2f", descuento));
        txtMetodoPago.setText(metodoPago);
    }

    public void setClienteInfo(String nombre, String dni, String telefono, String email, String direccion) {
        txtCliente.setText(nombre);
        txtDni.setText(dni);
        txtTel.setText(telefono);
        txtEmail.setText(email);
        txtDir.setText(direccion);
    }
// Opción 1: Usar directamente el Map en la tabla

    public void setProductos(List<Map<String, Object>> detalles) {
        DefaultTableModel model = (DefaultTableModel) tablaProd.getModel();
        model.setRowCount(0);

        int rowNum = 1;
        int productosDevueltos = 0;
        double totalDevuelto = 0.0;

        for (Map<String, Object> detalle : detalles) {
            Object descuentoDetalle = detalle.get("descuento_detalle");
            if (descuentoDetalle == null) {
                descuentoDetalle = 0.0;
            }

            // Verificar si el producto está activo (1) o no (0 = devuelto)
            Integer activo = detalle.get("activo") != null
                    ? Integer.parseInt(detalle.get("activo").toString()) : 1;
            // Modificar el nombre del producto para indicar si está devuelto
            String nombreProducto = (String) detalle.get("producto");
            if (activo == 0) {
                // Estilo HTML para resaltar "[DEVUELTO]"
                String estiloAdicional = ""; // Puedes agregar estilos adicionales aquí si es necesario
                String color = "#FF0000"; // Rojo

                nombreProducto = "<html>" + nombreProducto
                        + " <div style='"
                        + "padding: 3px 8px; border-radius: 10px; display: inline-block; "
                        + "font-size: 11px; font-weight: bold; "
                        + estiloAdicional
                        + "background-color: " + color + "; "
                        + "color: white;'"
                        + ">[DEVUELTO]</div></html>";

                productosDevueltos++;

                // Sumar al total devuelto
                Object subtotalObj = detalle.get("subtotal");
                if (subtotalObj != null) {
                    try {
                        double subtotal = Double.parseDouble(subtotalObj.toString());
                        totalDevuelto += subtotal;
                    } catch (NumberFormatException e) {
                        // Ignorar errores de conversión
                    }
                }
            }

            // Actualizar el nombre en el mapa
            detalle.put("producto", nombreProducto);
            model.addRow(new Object[]{
                rowNum++,
                detalle, // Pasar el Map completo para que el renderer lo procese
                detalle.get("tipo_salida") != null ? detalle.get("tipo_salida") : "Producto",
                String.format("$%,.2f", detalle.get("precio_unitario")),
                detalle.get("cantidad"),
                String.format("$%,.2f", descuentoDetalle),
                String.format("$%,.2f", detalle.get("subtotal"))
            });
        }

        // Actualizar la tabla
        tablaProd.getColumnModel().getColumn(1).setCellRenderer(
                new ProfileTableRendererReports(tablaProd)
        );

    }

    /**
     * Método para convertir un Map de datos de la base de datos en un
     * ModelProduct
     */
    private ModelProduct mapToModelProduct(Map<String, Object> data) {
        ModelProduct product = new ModelProduct();
        // Establecer los campos básicos del producto
        if (data.containsKey("id_producto")) {
            product.setProductId(Integer.parseInt(data.get("id_producto").toString()));
        }

        if (data.containsKey("producto")) {
            product.setName(data.get("producto").toString());
        }

        if (data.containsKey("barcode")) {
            product.setBarcode(data.get("barcode").toString());
        } else if (data.containsKey("codigo_barras")) {
            product.setBarcode(data.get("codigo_barras").toString());
        }

        if (data.containsKey("color")) {
            product.setColor(data.get("color").toString());
        }

        if (data.containsKey("talla")) {
            product.setSize(data.get("talla").toString());
        } else if (data.containsKey("size")) {
            product.setSize(data.get("size").toString());
        }

        return product;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        datePicker = new raven.datetime.component.date.DatePicker();
        jTextField1 = new javax.swing.JTextField();
        panelMain = new javax.swing.JPanel();
        panelup = new javax.swing.JPanel();
        tituloinfo = new javax.swing.JLabel();
        titleVenta = new javax.swing.JLabel();
        txtCliente = new javax.swing.JLabel();
        txtnFecha = new javax.swing.JLabel();
        titleVenta1 = new javax.swing.JLabel();
        txtEstado = new javax.swing.JLabel();
        titleVenta2 = new javax.swing.JLabel();
        tituloinfo1 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        titleVenta3 = new javax.swing.JLabel();
        txtnVenta1 = new javax.swing.JLabel();
        txtDni = new javax.swing.JLabel();
        titleVenta4 = new javax.swing.JLabel();
        txtTel = new javax.swing.JLabel();
        titleVenta5 = new javax.swing.JLabel();
        titleVenta6 = new javax.swing.JLabel();
        txtEmail = new javax.swing.JLabel();
        titleVenta7 = new javax.swing.JLabel();
        txtDir = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        tituloinfo2 = new javax.swing.JLabel();
        txtMetodoPago = new javax.swing.JLabel();
        titleVenta8 = new javax.swing.JLabel();
        txtDescuento = new javax.swing.JLabel();
        titleVenta10 = new javax.swing.JLabel();
        txtTotal = new javax.swing.JLabel();
        titleVenta11 = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JSeparator();
        jSeparator4 = new javax.swing.JSeparator();
        tituloinfo3 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tablaProd = new javax.swing.JTable();
        btnAdd = new javax.swing.JButton();

        jTextField1.setText("jTextField1");

        panelMain.setBackground(new java.awt.Color(153, 153, 153));

        tituloinfo.setText("Informacion de venta");

        javax.swing.GroupLayout panelupLayout = new javax.swing.GroupLayout(panelup);
        panelup.setLayout(panelupLayout);
        panelupLayout.setHorizontalGroup(
            panelupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelupLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tituloinfo, javax.swing.GroupLayout.PREFERRED_SIZE, 217, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelupLayout.setVerticalGroup(
            panelupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelupLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tituloinfo, javax.swing.GroupLayout.DEFAULT_SIZE, 46, Short.MAX_VALUE)
                .addGap(5, 5, 5))
        );

        titleVenta.setText("Numero de venta");

        txtCliente.setText("Francisco Puertas");

        txtnFecha.setText("24 de abril 2005");

        titleVenta1.setText("Fecha de venta");

        txtEstado.setText("COMPLETADO");

        titleVenta2.setText("Estado");

        tituloinfo1.setText("Lista de productos");

        jSeparator1.setForeground(new java.awt.Color(255, 255, 255));

        titleVenta3.setText("Cliente");

        txtnVenta1.setText("123456789");

        txtDni.setText("3127657063");

        titleVenta4.setText("DNI");

        txtTel.setText("8900593205");

        titleVenta5.setText("TELEFONO");

        titleVenta6.setText("Email");

        txtEmail.setText("direccion@estudiante.anillo.com.co");

        titleVenta7.setText("Dirección");

        txtDir.setText("Calle #8N 25-351");

        tituloinfo2.setText("Informacion de pago");

        txtMetodoPago.setText("Efectivo");

        titleVenta8.setText("Tipo de pago");

        txtDescuento.setText("$2.000.999");

        titleVenta10.setText("Descuento");

        txtTotal.setText("$10.000.000");

        titleVenta11.setText("Total");

        jSeparator3.setForeground(new java.awt.Color(255, 255, 255));
        jSeparator3.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jSeparator3.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N

        jSeparator4.setForeground(new java.awt.Color(255, 255, 255));
        jSeparator4.setOrientation(javax.swing.SwingConstants.VERTICAL);

        tituloinfo3.setText("Informacion del cliente");

        tablaProd.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "N°", "Producto", "tipo salida", "precio par", "cantidad", "descuento", "subtotal"
            }
        ));
        jScrollPane1.setViewportView(tablaProd);
        if (tablaProd.getColumnModel().getColumnCount() > 0) {
            tablaProd.getColumnModel().getColumn(0).setMinWidth(30);
            tablaProd.getColumnModel().getColumn(0).setPreferredWidth(30);
            tablaProd.getColumnModel().getColumn(0).setMaxWidth(30);
            tablaProd.getColumnModel().getColumn(1).setPreferredWidth(300);
            tablaProd.getColumnModel().getColumn(2).setMinWidth(90);
            tablaProd.getColumnModel().getColumn(2).setPreferredWidth(90);
            tablaProd.getColumnModel().getColumn(2).setMaxWidth(90);
            tablaProd.getColumnModel().getColumn(4).setMinWidth(80);
            tablaProd.getColumnModel().getColumn(4).setPreferredWidth(80);
            tablaProd.getColumnModel().getColumn(4).setMaxWidth(80);
        }

        btnAdd.setText("Agregar");
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelMainLayout = new javax.swing.GroupLayout(panelMain);
        panelMain.setLayout(panelMainLayout);
        panelMainLayout.setHorizontalGroup(
            panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelup, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(panelMainLayout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelMainLayout.createSequentialGroup()
                        .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelMainLayout.createSequentialGroup()
                                .addGap(126, 126, 126)
                                .addComponent(titleVenta4)
                                .addGap(106, 106, 106)
                                .addComponent(titleVenta5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(panelMainLayout.createSequentialGroup()
                                .addComponent(jScrollPane1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                        .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jSeparator1)
                    .addComponent(jSeparator2)
                    .addGroup(panelMainLayout.createSequentialGroup()
                        .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelMainLayout.createSequentialGroup()
                                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(titleVenta)
                                    .addComponent(txtnVenta1, javax.swing.GroupLayout.PREFERRED_SIZE, 149, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(txtnFecha, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(titleVenta1))
                                .addGap(18, 18, 18)
                                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(titleVenta2)
                                    .addComponent(txtEstado))
                                .addGap(24, 24, 24)
                                .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(20, 20, 20)
                                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(panelMainLayout.createSequentialGroup()
                                        .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(titleVenta8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(txtMetodoPago, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addGap(20, 20, 20)
                                        .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(titleVenta10)
                                            .addComponent(txtDescuento, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGap(20, 20, 20)
                                        .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(txtTotal, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(titleVenta11, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                    .addComponent(tituloinfo2, javax.swing.GroupLayout.PREFERRED_SIZE, 313, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(panelMainLayout.createSequentialGroup()
                                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(panelMainLayout.createSequentialGroup()
                                        .addComponent(txtCliente, javax.swing.GroupLayout.PREFERRED_SIZE, 122, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(24, 24, 24)
                                        .addComponent(txtTel, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(20, 20, 20)
                                        .addComponent(txtDni, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(titleVenta3)
                                    .addComponent(tituloinfo3, javax.swing.GroupLayout.PREFERRED_SIZE, 217, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(20, 20, 20)
                                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(titleVenta6)
                                    .addComponent(txtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, 204, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(20, 20, 20)
                                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(titleVenta7)
                                    .addComponent(txtDir, javax.swing.GroupLayout.PREFERRED_SIZE, 167, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(panelMainLayout.createSequentialGroup()
                        .addComponent(tituloinfo1, javax.swing.GroupLayout.PREFERRED_SIZE, 411, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnAdd)
                        .addGap(32, 32, 32)))
                .addGap(30, 30, 30))
        );
        panelMainLayout.setVerticalGroup(
            panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMainLayout.createSequentialGroup()
                .addComponent(panelup, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(panelMainLayout.createSequentialGroup()
                        .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelMainLayout.createSequentialGroup()
                                .addComponent(tituloinfo2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelMainLayout.createSequentialGroup()
                                        .addComponent(titleVenta8)
                                        .addGap(4, 4, 4)
                                        .addComponent(txtMetodoPago, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelMainLayout.createSequentialGroup()
                                        .addComponent(titleVenta10)
                                        .addGap(4, 4, 4)
                                        .addComponent(txtDescuento, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelMainLayout.createSequentialGroup()
                                        .addComponent(titleVenta11)
                                        .addGap(4, 4, 4)
                                        .addComponent(txtTotal, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(2, 2, 2))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelMainLayout.createSequentialGroup()
                                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(panelMainLayout.createSequentialGroup()
                                        .addComponent(titleVenta, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(txtnVenta1, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(panelMainLayout.createSequentialGroup()
                                            .addComponent(titleVenta1)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(txtnFecha, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(panelMainLayout.createSequentialGroup()
                                            .addComponent(titleVenta2)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(txtEstado, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                .addGap(9, 9, 9)))
                        .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tituloinfo3, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelMainLayout.createSequentialGroup()
                        .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(titleVenta4)
                            .addComponent(titleVenta5)
                            .addComponent(titleVenta3))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtCliente, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtTel, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtDni, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelMainLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelMainLayout.createSequentialGroup()
                                .addComponent(titleVenta6)
                                .addGap(4, 4, 4)
                                .addComponent(txtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelMainLayout.createSequentialGroup()
                                .addComponent(titleVenta7)
                                .addGap(4, 4, 4)
                                .addComponent(txtDir, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 12, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelMainLayout.createSequentialGroup()
                        .addGap(110, 110, 110)
                        .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelMainLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(tituloinfo1, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnAdd))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 362, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(44, 44, 44))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panelMain, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panelMain, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
        try {
            // Obtener el ID de venta actual
            int idVenta = Integer.parseInt(txtnVenta1.getText());

            // Cerrar el formulario actual (formulario de visualización)
            javax.swing.SwingUtilities.getWindowAncestor(this).dispose();

            // Abrir el formulario de venta en modo edición con el ID específico
            Application.showForm(new generarVentaFor1(idVenta));
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Error al obtener el ID de la venta: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            System.getLogger(viewReport.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }

    }//GEN-LAST:event_btnAddActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAdd;
    private raven.datetime.component.date.DatePicker datePicker;
    protected static javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JPanel panelMain;
    private javax.swing.JPanel panelup;
    protected static javax.swing.JTable tablaProd;
    private javax.swing.JLabel titleVenta;
    private javax.swing.JLabel titleVenta1;
    private javax.swing.JLabel titleVenta10;
    private javax.swing.JLabel titleVenta11;
    private javax.swing.JLabel titleVenta2;
    private javax.swing.JLabel titleVenta3;
    private javax.swing.JLabel titleVenta4;
    private javax.swing.JLabel titleVenta5;
    private javax.swing.JLabel titleVenta6;
    private javax.swing.JLabel titleVenta7;
    private javax.swing.JLabel titleVenta8;
    private javax.swing.JLabel tituloinfo;
    private javax.swing.JLabel tituloinfo1;
    private javax.swing.JLabel tituloinfo2;
    private javax.swing.JLabel tituloinfo3;
    protected static javax.swing.JLabel txtCliente;
    protected static javax.swing.JLabel txtDescuento;
    protected static javax.swing.JLabel txtDir;
    protected static javax.swing.JLabel txtDni;
    protected static javax.swing.JLabel txtEmail;
    protected static javax.swing.JLabel txtEstado;
    protected static javax.swing.JLabel txtMetodoPago;
    protected static javax.swing.JLabel txtTel;
    protected static javax.swing.JLabel txtTotal;
    protected static javax.swing.JLabel txtnFecha;
    protected static javax.swing.JLabel txtnVenta1;
    // End of variables declaration//GEN-END:variables
}
