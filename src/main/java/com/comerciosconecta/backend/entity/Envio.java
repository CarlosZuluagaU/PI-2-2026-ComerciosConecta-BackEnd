package com.comerciosconecta.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "envio")
public class Envio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "comercio_id", nullable = false)
    private Integer comercioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_envio", nullable = false)
    private TipoEnvio tipoEnvio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_envio", nullable = false)
    private EstadoEnvio estadoEnvio = EstadoEnvio.PENDIENTE;

    // --- Destino ---
    @Column(name = "direccion_destino", columnDefinition = "TEXT")
    private String direccionDestino;

    @Column(name = "ciudad_destino")
    private String ciudadDestino;

    @Column(name = "departamento_destino")
    private String departamentoDestino;

    @Column(name = "lat_destino")
    private Double latDestino;

    @Column(name = "lng_destino")
    private Double lngDestino;

    // --- Cálculo ---
    @Column(name = "distancia_km")
    private Double distanciaKm;

    @Column(name = "costo_envio_calculado")
    private Long costoEnvioCalculado = 0L;   // centavos (antes de envío gratis)

    @Column(name = "costo_envio_final")
    private Long costoEnvioFinal = 0L;       // centavos (lo que paga el cliente)

    @Column(name = "envio_gratis")
    private Boolean envioGratis = false;

    // --- Fechas ---
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_despacho")
    private LocalDateTime fechaDespacho;

    @Column(name = "fecha_entrega_estimada")
    private LocalDate fechaEntregaEstimada;

    @Column(name = "fecha_entrega_real")
    private LocalDate fechaEntregaReal;

    // --- Guía ---
    @Column(name = "numero_guia", unique = true)
    private String numeroGuia;

    @Column(name = "nombre_entregador")
    private String nombreEntregador;

    @Column(name = "telefono_entregador")
    private String telefonoEntregador;

    @Column(name = "url_seguimiento", columnDefinition = "TEXT")
    private String urlSeguimiento;

    @Column(name = "nombre_transportadora")
    private String nombreTransportadora;

    // --- Notas ---
    @Column(name = "notas_comercio", columnDefinition = "TEXT")
    private String notasComercio;

    @Column(name = "notas_cliente", columnDefinition = "TEXT")
    private String notasCliente;

    // --- Historial de seguimiento ---
    @JsonIgnore
    @OneToMany(mappedBy = "envio", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fecha ASC")
    private List<SeguimientoEnvio> historial;

    public Envio() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Integer getComercioId() { return comercioId; }
    public void setComercioId(Integer comercioId) { this.comercioId = comercioId; }

    public TipoEnvio getTipoEnvio() { return tipoEnvio; }
    public void setTipoEnvio(TipoEnvio tipoEnvio) { this.tipoEnvio = tipoEnvio; }

    public EstadoEnvio getEstadoEnvio() { return estadoEnvio; }
    public void setEstadoEnvio(EstadoEnvio estadoEnvio) { this.estadoEnvio = estadoEnvio; }

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

    public Double getDistanciaKm() { return distanciaKm; }
    public void setDistanciaKm(Double distanciaKm) { this.distanciaKm = distanciaKm; }

    public Long getCostoEnvioCalculado() { return costoEnvioCalculado; }
    public void setCostoEnvioCalculado(Long costoEnvioCalculado) { this.costoEnvioCalculado = costoEnvioCalculado; }

    public Long getCostoEnvioFinal() { return costoEnvioFinal; }
    public void setCostoEnvioFinal(Long costoEnvioFinal) { this.costoEnvioFinal = costoEnvioFinal; }

    public Boolean getEnvioGratis() { return envioGratis; }
    public void setEnvioGratis(Boolean envioGratis) { this.envioGratis = envioGratis; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaDespacho() { return fechaDespacho; }
    public void setFechaDespacho(LocalDateTime fechaDespacho) { this.fechaDespacho = fechaDespacho; }

    public LocalDate getFechaEntregaEstimada() { return fechaEntregaEstimada; }
    public void setFechaEntregaEstimada(LocalDate fechaEntregaEstimada) { this.fechaEntregaEstimada = fechaEntregaEstimada; }

    public LocalDate getFechaEntregaReal() { return fechaEntregaReal; }
    public void setFechaEntregaReal(LocalDate fechaEntregaReal) { this.fechaEntregaReal = fechaEntregaReal; }

    public String getNumeroGuia() { return numeroGuia; }
    public void setNumeroGuia(String numeroGuia) { this.numeroGuia = numeroGuia; }

    public String getNombreEntregador() { return nombreEntregador; }
    public void setNombreEntregador(String nombreEntregador) { this.nombreEntregador = nombreEntregador; }

    public String getTelefonoEntregador() { return telefonoEntregador; }
    public void setTelefonoEntregador(String telefonoEntregador) { this.telefonoEntregador = telefonoEntregador; }

    public String getUrlSeguimiento() { return urlSeguimiento; }
    public void setUrlSeguimiento(String urlSeguimiento) { this.urlSeguimiento = urlSeguimiento; }

    public String getNombreTransportadora() { return nombreTransportadora; }
    public void setNombreTransportadora(String nombreTransportadora) { this.nombreTransportadora = nombreTransportadora; }

    public String getNotasComercio() { return notasComercio; }
    public void setNotasComercio(String notasComercio) { this.notasComercio = notasComercio; }

    public String getNotasCliente() { return notasCliente; }
    public void setNotasCliente(String notasCliente) { this.notasCliente = notasCliente; }

    public List<SeguimientoEnvio> getHistorial() { return historial; }
    public void setHistorial(List<SeguimientoEnvio> historial) { this.historial = historial; }
}
