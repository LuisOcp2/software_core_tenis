/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package raven.controlador.productos.controler;

import raven.application.form.productos.creates.CreateDescuento;
import javax.swing.JTextField;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 *
 * @author CrisDEV
 * Controlador de la vista CreateDescuentos, Model y DAO 
 */
public class CtrlCreateDescuento {
    
    private CreateDescuento vista;
    
    /**
     * Constructor del controlador
     * @param vista La vista CreateDescuento a controlar
     */
    public CtrlCreateDescuento(CreateDescuento vista) {
        this.vista = vista;
    }
    
    /**
     * Obtiene la fecha de inicio desde el campo de texto
     * @return La fecha de inicio como String
     */
    public String obtenerFechaInicio() {
        return vista.txtFechaIn.getText().trim();
    }
    
    /**
     * Obtiene la fecha de fin desde el campo de texto
     * @return La fecha de fin como String
     */
    public String obtenerFechaFin() {
        return vista.txtFechafin.getText().trim();
    }
    
    /**
     * Establece la fecha de inicio en el campo de texto
     * @param fecha La fecha a establecer
     */
    public void establecerFechaInicio(String fecha) {
        vista.txtFechaIn.setText(fecha != null ? fecha : "");
    }
    
    /**
     * Establece la fecha de fin en el campo de texto
     * @param fecha La fecha a establecer
     */
    public void establecerFechaFin(String fecha) {
        vista.txtFechafin.setText(fecha != null ? fecha : "");
    }
    
    /**
     * Valida que las fechas sean válidas y estén en el formato correcto
     * @return true si las fechas son válidas, false en caso contrario
     */
    public boolean validarFechas() {
        String fechaInicio = obtenerFechaInicio();
        String fechaFin = obtenerFechaFin();
        
        if (fechaInicio.isEmpty() || fechaFin.isEmpty()) {
            mostrarError("Las fechas de inicio y fin son obligatorias");
            return false;
        }
        
        try {
            LocalDateTime inicio = parsearFecha(fechaInicio);
            LocalDateTime fin = parsearFecha(fechaFin);
            
            if (inicio.isAfter(fin)) {
                mostrarError("La fecha de inicio no puede ser posterior a la fecha de fin");
                return false;
            }
            
            return true;
        } catch (DateTimeParseException e) {
            mostrarError("Formato de fecha inválido. Use el formato: dd/MM/yyyy HH:mm");
            return false;
        }
    }
    
    /**
     * Valida todos los campos de fechas
     * @return true si todos los campos son válidos, false en caso contrario
     */
    public boolean validarTodosLosCampos() {
        return validarFechas();
    }
    
    /**
     * Limpia todos los campos de fechas
     */
    public void limpiarTodosLosCampos() {
        vista.txtFechaIn.setText("");
        vista.txtFechafin.setText("");
    }
    
    /**
     * Parsea una fecha desde texto usando múltiples formatos
     * @param fechaTexto El texto de la fecha a parsear
     * @return LocalDateTime parseado
     * @throws DateTimeParseException si no se puede parsear la fecha
     */
    private LocalDateTime parsearFecha(String fechaTexto) throws DateTimeParseException {
        // Formatos de fecha soportados
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
        };
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                if (formatter.toString().contains("HH:mm")) {
                    return LocalDateTime.parse(fechaTexto, formatter);
                } else {
                    // Si no tiene hora, agregar 00:00
                    return LocalDateTime.parse(fechaTexto + " 00:00", 
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                }
            } catch (DateTimeParseException e) {
                // Continuar con el siguiente formato
            }
        }
        
        throw new DateTimeParseException("No se pudo parsear la fecha: " + fechaTexto, fechaTexto, 0);
    }
    
    /**
     * Muestra un mensaje de error al usuario
     * @param mensaje El mensaje de error a mostrar
     */
    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(vista, mensaje, "Error de Validación", JOptionPane.ERROR_MESSAGE);
    }
}
 