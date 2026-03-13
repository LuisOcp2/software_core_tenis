/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package raven.utils;

import java.util.*;

/**
 * NUEVA CLASE: ChangeTracker.java
 *Rastrea cambios en variantes durante la edición
 * @author CrisDEV
 */

public class ChangeTracker {
    
    // Estado original de las variantes antes de editar
    private final Map<String, VariantSnapshot> originalState = new HashMap<>();
    
    // Cambios detectados
    private final Set<String> modifiedVariants = new HashSet<>();
    private final Set<String> newVariants = new HashSet<>();
    private final Set<String> deletedVariants = new HashSet<>();
    
    public static class VariantSnapshot {
        public int idVariante;
        public int idBodega;
        public String talla;
        public String color;
        public int cantidad;
        public String tipo;
        public byte[] imagenHash; // Hash MD5 de la imagen
        public double precioVenta;
        public double precioCompra;
        
        public String getKey() {
            return idVariante + "_" + idBodega;
        }
        
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof VariantSnapshot)) return false;
            VariantSnapshot other = (VariantSnapshot) o;
            
            boolean imagenIgual;
            if (this.imagenHash == null) {
                imagenIgual = true; // Si no se proporcionó imagen actual, no considerar cambio
            } else {
                imagenIgual = Arrays.equals(this.imagenHash, other.imagenHash);
            }

            return this.talla.equals(other.talla) &&
                   this.color.equals(other.color) &&
                   this.cantidad == other.cantidad &&
                   this.tipo.equals(other.tipo) &&
                   imagenIgual &&
                   Math.abs(this.precioVenta - other.precioVenta) < 0.01 &&
                   Math.abs(this.precioCompra - other.precioCompra) < 0.01;
        }
    }
    
    /**
     * Captura el estado inicial de las variantes
     */
    public void captureInitialState(javax.swing.table.DefaultTableModel model, 
                                   Map<Integer, Integer> variantIdPorFila,
                                   Map<Integer, java.io.File> archivosPorFila,
                                   Map<Integer, Integer> bodegaIdPorFila) {
        originalState.clear();
        
        for (int i = 0; i < model.getRowCount(); i++) {
            VariantSnapshot snapshot = new VariantSnapshot();
            
            Integer idVar = variantIdPorFila.get(i);
            if (idVar != null && idVar > 0) {
                snapshot.idVariante = idVar;
            }
            Integer idBod = bodegaIdPorFila != null ? bodegaIdPorFila.get(i) : null;
            snapshot.idBodega = (idBod != null ? idBod : 0);
            
            snapshot.talla = (String) model.getValueAt(i, 0);
            snapshot.color = (String) model.getValueAt(i, 1);
            
            // Indice 3 es Stock (Cantidad)
            Object valCantidad = model.getValueAt(i, 3);
            if (valCantidad instanceof Integer) {
                snapshot.cantidad = (Integer) valCantidad;
            } else if (valCantidad instanceof String) {
                try {
                    String sVal = ((String) valCantidad).trim();
                    snapshot.cantidad = sVal.isEmpty() ? 0 : Integer.parseInt(sVal);
                } catch (NumberFormatException e) {
                    System.err.println("Warning: Valor no numérico en columna cantidad fila " + i + ": " + valCantidad);
                    snapshot.cantidad = 0;
                }
            } else if (valCantidad instanceof Number) {
                snapshot.cantidad = ((Number) valCantidad).intValue();
            } else {
                snapshot.cantidad = 0;
            }
            
            // Indice 4 es Tipo
            snapshot.tipo = String.valueOf(model.getValueAt(i, 4));
            
            // Capturar hash de imagen si existe
            java.io.File imgFile = archivosPorFila.get(i);
            if (imgFile != null && imgFile.exists()) {
                snapshot.imagenHash = calculateFileHash(imgFile);
            }
            
            originalState.put(snapshot.getKey(), snapshot);
        }
        
        System.out.println("SUCCESS  Estado inicial capturado: " + originalState.size() + " variantes");
    }
    
    /**
     * Detecta cambios comparando estado actual vs original
     */

    public void detectChanges(javax.swing.table.DefaultTableModel model,
                             Map<Integer, Integer> variantIdPorFila,
                             Map<Integer, java.io.File> archivosPorFila,
                             Map<Integer, Integer> bodegaIdPorFila) {

       modifiedVariants.clear();
       newVariants.clear();
       deletedVariants.clear();

       Set<String> currentKeys = new HashSet<>();

       for (int i = 0; i < model.getRowCount(); i++) {
           VariantSnapshot current = new VariantSnapshot();

           // Obtener idVariante si existe
           Integer idVar = variantIdPorFila.get(i);
           if (idVar != null && idVar > 0) {
               current.idVariante = idVar;
           } else {
               current.idVariante = 0; // Nueva variante
           }

           Integer idBod = bodegaIdPorFila != null ? bodegaIdPorFila.get(i) : null;
           current.idBodega = (idBod != null ? idBod : 0);

           current.talla = (String) model.getValueAt(i, 0);
           current.color = (String) model.getValueAt(i, 1);
           
           Object valCant = model.getValueAt(i, 3);
           if (valCant instanceof Number) current.cantidad = ((Number)valCant).intValue();
           else try { current.cantidad = Integer.parseInt(String.valueOf(valCant)); } catch(Exception e) { current.cantidad = 0; }
           
           current.tipo = String.valueOf(model.getValueAt(i, 4));

           // Hash de imagen
           java.io.File imgFile = archivosPorFila.get(i);
           if (imgFile != null && imgFile.exists()) {
               current.imagenHash = calculateFileHash(imgFile);
           }

           String key;
           if (current.idVariante > 0) {
               key = current.getKey(); // "idVariante_idBodega"
           } else {
               // Para variantes nuevas, generar key temporal basado en contenido
               key = (current.talla + current.color + current.tipo).hashCode() + "_0";
           }

           currentKeys.add(key);

           // Comparar con estado original
           if (originalState.containsKey(key)) {
               VariantSnapshot original = originalState.get(key);
               if (!current.equals(original)) {
                   modifiedVariants.add(key);
               }
           } else {
               newVariants.add(key);
           }
       }

       // Detectar variantes eliminadas
       for (String originalKey : originalState.keySet()) {
           if (!currentKeys.contains(originalKey)) {
               deletedVariants.add(originalKey);
           }
       }

       printChangeReport();
   }

   
    
    /**
     * Calcula hash MD5 de un archivo
     */
    private byte[] calculateFileHash(java.io.File file) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    md.update(buffer, 0, read);
                }
            }
            return md.digest();
        } catch (Exception e) {
            return null;
        }
    }
    
    private void printChangeReport() {
        System.out.println("\nResumen REPORTE DE CAMBIOS:");
        System.out.println("=".repeat(50));
        System.out.println(" Nuevas variantes: " + newVariants.size());
        System.out.println(" Variantes modificadas: " + modifiedVariants.size());
        System.out.println("Eliminar Variantes eliminadas: " + deletedVariants.size());
        System.out.println("=".repeat(50) + "\n");
    }
    
    // Getters
    public boolean hasChanges() {
        return !modifiedVariants.isEmpty() || !newVariants.isEmpty() || !deletedVariants.isEmpty();
    }
    
    public Set<String> getModifiedVariants() { return modifiedVariants; }
    public Set<String> getNewVariants() { return newVariants; }
    public Set<String> getDeletedVariants() { return deletedVariants; }
    public Map<String, VariantSnapshot> getOriginalState() { return originalState; }
}

