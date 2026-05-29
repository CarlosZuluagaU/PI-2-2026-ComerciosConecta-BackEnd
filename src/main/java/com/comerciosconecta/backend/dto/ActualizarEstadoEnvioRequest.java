package com.comerciosconecta.backend.dto;

public class ActualizarEstadoEnvioRequest {
    private String estadoEnvio;      // EstadoEnvio enum como String
    private String descripcion;      // ej: "Paquete en bodega Medellín"
    private String ubicacionActual;  // ej: "Medellín, Antioquia"

    public String getEstadoEnvio() { return estadoEnvio; }
    public void setEstadoEnvio(String estadoEnvio) { this.estadoEnvio = estadoEnvio; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getUbicacionActual() { return ubicacionActual; }
    public void setUbicacionActual(String ubicacionActual) { this.ubicacionActual = ubicacionActual; }
}
