package com.comerciosconecta.backend.controller;

import java.util.List;

public class CreateOrderRequest {

    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String customerAddress;
    private String customerCity;
    private Integer comercioId;
    private Long totalInCents;
    private List<Item> items;

    // Envío
    private String tipoEnvio;               // RECOGIDA | LOCAL_PROPIO | LOCAL_TRANSPORTADORA | NACIONAL_TRANSPORTADORA
    private Long shippingCostInCents = 0L;
    private String direccionDestino;
    private String ciudadDestino;
    private String departamentoDestino;
    private Double latDestino;
    private Double lngDestino;

    // Constructor vacío
    public CreateOrderRequest() {
    }

    // Constructor con parámetros
    public CreateOrderRequest(String customerName, String customerEmail, String customerPhone,
                              Long totalInCents, List<Item> items) {
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerPhone = customerPhone;
        this.totalInCents = totalInCents;
        this.items = items;
    }

    // Getters y Setters

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public Long getTotalInCents() {
        return totalInCents;
    }

    public void setTotalInCents(Long totalInCents) {
        this.totalInCents = totalInCents;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public String getCustomerAddress() { return customerAddress; }
    public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }

    public String getCustomerCity() { return customerCity; }
    public void setCustomerCity(String customerCity) { this.customerCity = customerCity; }

    public Integer getComercioId() { return comercioId; }
    public void setComercioId(Integer comercioId) { this.comercioId = comercioId; }

    public String getTipoEnvio() { return tipoEnvio; }
    public void setTipoEnvio(String tipoEnvio) { this.tipoEnvio = tipoEnvio; }

    public Long getShippingCostInCents() { return shippingCostInCents; }
    public void setShippingCostInCents(Long shippingCostInCents) { this.shippingCostInCents = shippingCostInCents; }

    public String getDireccionDestino() { return direccionDestino; }
    public void setDireccionDestino(String direccionDestino) { this.direccionDestino = direccionDestino; }

    public String getCiudadDestino() { return ciudadDestino; }
    public void setCiudadDestino(String ciudadDestino) { this.ciudadDestino = ciudadDestino; }

    public String getDepartamentoDestino() { return departamentoDestino; }
    public void setDepartamentoDestino(String departamentoDestino) { this.departamentoDestino = departamentoDestino; }

    public Double getLatDestino() { return latDestino; }
    public void setLatDestino(Double latDestino) { this.latDestino = latDestino; }

    public Double getLngDestino() { return lngDestino; }
    public void setLngDestino(Double lngDestino) { this.lngDestino = lngDestino; }

    // Clase interna Item
    public static class Item {

        private Long productoId;
        private String nombre;
        private Integer cantidad;
        private Long priceInCents;
        private Integer ivaPercentage;
        private Long subtotalInCents;

        // Constructor vacío
        public Item() {
        }

        // Constructor con parámetros
        public Item(Long productoId, String nombre, Integer cantidad,
                    Long priceInCents, Integer ivaPercentage, Long subtotalInCents) {
            this.productoId = productoId;
            this.nombre = nombre;
            this.cantidad = cantidad;
            this.priceInCents = priceInCents;
            this.ivaPercentage = ivaPercentage;
            this.subtotalInCents = subtotalInCents;
        }

        // Getters y Setters

        public Long getProductoId() {
            return productoId;
        }

        public void setProductoId(Long productoId) {
            this.productoId = productoId;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public Integer getCantidad() {
            return cantidad;
        }

        public void setCantidad(Integer cantidad) {
            this.cantidad = cantidad;
        }

        public Long getPriceInCents() {
            return priceInCents;
        }

        public void setPriceInCents(Long priceInCents) {
            this.priceInCents = priceInCents;
        }

        public Integer getIvaPercentage() {
            return ivaPercentage;
        }

        public void setIvaPercentage(Integer ivaPercentage) {
            this.ivaPercentage = ivaPercentage;
        }

        public Long getSubtotalInCents() {
            return subtotalInCents;
        }

        public void setSubtotalInCents(Long subtotalInCents) {
            this.subtotalInCents = subtotalInCents;
        }
    }
}
