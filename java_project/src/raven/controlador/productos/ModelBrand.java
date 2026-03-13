/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package raven.controlador.productos;

public class ModelBrand {

    private int brandId;
    private String name;
    private String description;
    private boolean active;

    public ModelBrand(int brandId, String name, String description, boolean active) {
        this.brandId = brandId;
        this.name = name;
        this.description = description;
        this.active = active;
    }

    public ModelBrand() {
    }

    // Getters y Setters
    public int getBrandId() {
        return brandId;
    }

    public void setBrandId(int brandId) {
        this.brandId = brandId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return name;
    }

    public Object[] toTableRow(int rowNum) {

        return new Object[]{
            false,
            rowNum,
            this,
            brandId,
            description,
       };

    }
}
