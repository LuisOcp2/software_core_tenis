/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package raven.componentes.tablareportes;

import java.util.Map;
import raven.controlador.productos.ModelProduct;

/**
 *
 * @author LUIS
 */
public class mapToModelProduct {

    public ModelProduct mapToModelProduct(Map<String, Object> data) {
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

        // Si necesitas más campos, puedes agregarlos aquí
        return product;
    }

}
