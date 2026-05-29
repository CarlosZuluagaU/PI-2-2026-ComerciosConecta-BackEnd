package com.comerciosconecta.backend.dto;

public class CalcularEnvioRequest {
    private Integer comercioId;
    private String direccionDestino;
    private String ciudadDestino;
    private String departamentoDestino;
    private Double latDestino;
    private Double lngDestino;
    private Long montoOrdenEnCentavos; // para calcular si aplica envío gratis

    public Integer getComercioId() { return comercioId; }
    public void setComercioId(Integer comercioId) { this.comercioId = comercioId; }

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

    public Long getMontoOrdenEnCentavos() { return montoOrdenEnCentavos; }
    public void setMontoOrdenEnCentavos(Long montoOrdenEnCentavos) { this.montoOrdenEnCentavos = montoOrdenEnCentavos; }
}
