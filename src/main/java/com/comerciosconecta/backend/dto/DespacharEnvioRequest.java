package com.comerciosconecta.backend.dto;

import java.time.LocalDate;

public class DespacharEnvioRequest {
    private String tipoEnvioFinal;       // confirmar cuál modalidad usó el comercio
    private String nombreEntregador;
    private String telefonoEntregador;
    private String numeroGuia;           // si lo tiene (transportadora externa)
    private String urlSeguimiento;       // URL tracking de la transportadora
    private String nombreTransportadora;
    private LocalDate fechaEntregaEstimada;
    private String notasComercio;

    public String getTipoEnvioFinal() { return tipoEnvioFinal; }
    public void setTipoEnvioFinal(String tipoEnvioFinal) { this.tipoEnvioFinal = tipoEnvioFinal; }

    public String getNombreEntregador() { return nombreEntregador; }
    public void setNombreEntregador(String nombreEntregador) { this.nombreEntregador = nombreEntregador; }

    public String getTelefonoEntregador() { return telefonoEntregador; }
    public void setTelefonoEntregador(String telefonoEntregador) { this.telefonoEntregador = telefonoEntregador; }

    public String getNumeroGuia() { return numeroGuia; }
    public void setNumeroGuia(String numeroGuia) { this.numeroGuia = numeroGuia; }

    public String getUrlSeguimiento() { return urlSeguimiento; }
    public void setUrlSeguimiento(String urlSeguimiento) { this.urlSeguimiento = urlSeguimiento; }

    public String getNombreTransportadora() { return nombreTransportadora; }
    public void setNombreTransportadora(String nombreTransportadora) { this.nombreTransportadora = nombreTransportadora; }

    public LocalDate getFechaEntregaEstimada() { return fechaEntregaEstimada; }
    public void setFechaEntregaEstimada(LocalDate fechaEntregaEstimada) { this.fechaEntregaEstimada = fechaEntregaEstimada; }

    public String getNotasComercio() { return notasComercio; }
    public void setNotasComercio(String notasComercio) { this.notasComercio = notasComercio; }
}
