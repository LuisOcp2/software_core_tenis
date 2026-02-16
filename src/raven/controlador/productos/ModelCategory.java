/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package raven.controlador.productos;

public class ModelCategory {

    private int categoryId;
    private String name;
    private String description;
    private boolean active;

    public ModelCategory(int categoryId, String name, String description, boolean active) {
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.active = active;
    }

    public ModelCategory() {
    }

    // Getters y Setters
    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
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
            categoryId,
            description,};

    }
}
