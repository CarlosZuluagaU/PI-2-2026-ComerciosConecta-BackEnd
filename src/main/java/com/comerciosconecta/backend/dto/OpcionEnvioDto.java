package com.comerciosconecta.backend.dto;

public class OpcionEnvioDto {
    private String tipoEnvio;          // RECOGIDA | LOCAL_PROPIO | LOCAL_TRANSPORTADORA | NACIONAL_TRANSPORTADORA
    private String descripcion;        // "Entrega a domicilio con mensajero propio"
    private Long costoEnCentavos;      // costo calculado
    private Long costoFinalEnCentavos; // 0 si es gratis
    private boolean envioGratis;
    private int diasEstimados;
    private double distanciaKm;
    private String transportadora;     // nombre si aplica

    public OpcionEnvioDto() {}

    public String getTipoEnvio() { return tipoEnvio; }
    public void setTipoEnvio(String tipoEnvio) { this.tipoEnvio = tipoEnvio; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Long getCostoEnCentavos() { return costoEnCentavos; }
    public void setCostoEnCentavos(Long costoEnCentavos) { this.costoEnCentavos = costoEnCentavos; }

    public Long getCostoFinalEnCentavos() { return costoFinalEnCentavos; }
    public void setCostoFinalEnCentavos(Long costoFinalEnCentavos) { this.costoFinalEnCentavos = costoFinalEnCentavos; }

    public boolean isEnvioGratis() { return envioGratis; }
    public void setEnvioGratis(boolean envioGratis) { this.envioGratis = envioGratis; }

    public int getDiasEstimados() { return diasEstimados; }
    public void setDiasEstimados(int diasEstimados) { this.diasEstimados = diasEstimados; }

    public double getDistanciaKm() { return distanciaKm; }
    public void setDistanciaKm(double distanciaKm) { this.distanciaKm = distanciaKm; }

    public String getTransportadora() { return transportadora; }
    public void setTransportadora(String transportadora) { this.transportadora = transportadora; }
}
