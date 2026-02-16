/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package raven.controlador.comercial;

public class ModelSupplier {

    private int supplierId;
    private String name;
    private String ruc;
    private String address;
    private String phone;
    private String email;
    private boolean active;

    public ModelSupplier(int supplierId, String name, String ruc, String address,
            String phone, String email, boolean active) {
        this.supplierId = supplierId;
        this.name = name;
        this.ruc = ruc;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.active = active;
    }

    public ModelSupplier() {
    }

    // Getters y Setters
    public int getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(int supplierId) {
        this.supplierId = supplierId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
            ruc,
            address,
            phone,
            email,};
    }

}
