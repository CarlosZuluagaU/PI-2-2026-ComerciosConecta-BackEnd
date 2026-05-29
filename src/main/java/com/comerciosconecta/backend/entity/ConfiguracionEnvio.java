package com.comerciosconecta.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "configuracion_envio")
public class ConfiguracionEnvio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comercio_id", unique = true, nullable = false)
    private Integer comercioId;

    // Coordenadas del comercio para calcular distancia
    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;

    // Dirección del comercio detectada por geocodificación inversa
    @Column(name = "direccion_comercio")
    private String direccionComercio;

    @Column(name = "ciudad_comercio")
    private String ciudadComercio;

    @Column(name = "departamento_comercio")
    private String departamentoComercio;

    // Radio en km que se considera entrega local (más allá = nacional)
    @Column(name = "radio_local_km")
    private Double radioLocalKm = 25.0;

    // --- Modalidades habilitadas ---
    @Column(name = "permitir_recogida")
    private Boolean permitirRecogida = true;

    @Column(name = "permitir_entrega_local")
    private Boolean permitirEntregaLocal = true;

    @Column(name = "permitir_entrega_nacional")
    private Boolean permitirEntregaNacional = false;

    // PROPIO | TRANSPORTADORA | AMBOS
    @Column(name = "tipo_entrega_local")
    private String tipoEntregaLocal = "PROPIO";

    // --- Tarifas entrega propia local ---
    // Si se configura precio fijo, se usa ese; si no, se calcula por km
    @Column(name = "tarifa_local_propio_fija")
    private Long tarifaLocalPropioFija;           // centavos, ej: 500000 = $5.000

    @Column(name = "tarifa_local_propio_por_km")
    private Long tarifaLocalPropioPorKm;           // centavos por km

    // --- Tarifas transportadora local ---
    @Column(name = "tarifa_local_transport_fija")
    private Long tarifaLocalTransportFija;

    @Column(name = "tarifa_local_transport_por_km")
    private Long tarifaLocalTransportPorKm;

    // --- Tarifas nacional ---
    @Column(name = "tarifa_nacional_fija")
    private Long tarifaNacionalFija;

    @Column(name = "tarifa_nacional_por_km")
    private Long tarifaNacionalPorKm;

    // --- Envío gratis ---
    // Si el monto de la orden supera este valor, el envío es gratis (null = no aplica)
    @Column(name = "monto_minimo_envio_gratis")
    private Long montoMinimoEnvioGratis;           // centavos

    // --- Tiempos estimados de entrega (en días) ---
    @Column(name = "dias_entrega_local_propio")
    private Integer diasEntregaLocalPropio = 1;

    @Column(name = "dias_entrega_local_transport")
    private Integer diasEntregaLocalTransport = 2;

    @Column(name = "dias_entrega_nacional")
    private Integer diasEntregaNacional = 5;

    // Nombre de la transportadora que usa el comercio (ej: "Coordinadora")
    @Column(name = "nombre_transportadora")
    private String nombreTransportadora;

    // ── Nuevas tarifas por zona geográfica (reemplazan el sistema anterior) ──
    @Column(name = "precio_ciudad_centavos")
    private Long precioCiudadCentavos;

    @Column(name = "precio_departamento_centavos")
    private Long precioDepartamentoCentavos;

    @Column(name = "precio_nacional_centavos")
    private Long precioNacionalCentavos;

    @Column(name = "dias_entrega_ciudad")
    private Integer diasEntregaCiudad = 1;

    @Column(name = "dias_entrega_departamento")
    private Integer diasEntregaDepartamento = 3;

    public ConfiguracionEnvio() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getComercioId() { return comercioId; }
    public void setComercioId(Integer comercioId) { this.comercioId = comercioId; }

    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }

    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }

    public String getDireccionComercio() { return direccionComercio; }
    public void setDireccionComercio(String direccionComercio) { this.direccionComercio = direccionComercio; }

    public String getCiudadComercio() { return ciudadComercio; }
    public void setCiudadComercio(String ciudadComercio) { this.ciudadComercio = ciudadComercio; }

    public String getDepartamentoComercio() { return departamentoComercio; }
    public void setDepartamentoComercio(String departamentoComercio) { this.departamentoComercio = departamentoComercio; }

    public Double getRadioLocalKm() { return radioLocalKm; }
    public void setRadioLocalKm(Double radioLocalKm) { this.radioLocalKm = radioLocalKm; }

    public Boolean getPermitirRecogida() { return permitirRecogida; }
    public void setPermitirRecogida(Boolean permitirRecogida) { this.permitirRecogida = permitirRecogida; }

    public Boolean getPermitirEntregaLocal() { return permitirEntregaLocal; }
    public void setPermitirEntregaLocal(Boolean permitirEntregaLocal) { this.permitirEntregaLocal = permitirEntregaLocal; }

    public Boolean getPermitirEntregaNacional() { return permitirEntregaNacional; }
    public void setPermitirEntregaNacional(Boolean permitirEntregaNacional) { this.permitirEntregaNacional = permitirEntregaNacional; }

    public String getTipoEntregaLocal() { return tipoEntregaLocal; }
    public void setTipoEntregaLocal(String tipoEntregaLocal) { this.tipoEntregaLocal = tipoEntregaLocal; }

    public Long getTarifaLocalPropioFija() { return tarifaLocalPropioFija; }
    public void setTarifaLocalPropioFija(Long tarifaLocalPropioFija) { this.tarifaLocalPropioFija = tarifaLocalPropioFija; }

    public Long getTarifaLocalPropioPorKm() { return tarifaLocalPropioPorKm; }
    public void setTarifaLocalPropioPorKm(Long tarifaLocalPropioPorKm) { this.tarifaLocalPropioPorKm = tarifaLocalPropioPorKm; }

    public Long getTarifaLocalTransportFija() { return tarifaLocalTransportFija; }
    public void setTarifaLocalTransportFija(Long tarifaLocalTransportFija) { this.tarifaLocalTransportFija = tarifaLocalTransportFija; }

    public Long getTarifaLocalTransportPorKm() { return tarifaLocalTransportPorKm; }
    public void setTarifaLocalTransportPorKm(Long tarifaLocalTransportPorKm) { this.tarifaLocalTransportPorKm = tarifaLocalTransportPorKm; }

    public Long getTarifaNacionalFija() { return tarifaNacionalFija; }
    public void setTarifaNacionalFija(Long tarifaNacionalFija) { this.tarifaNacionalFija = tarifaNacionalFija; }

    public Long getTarifaNacionalPorKm() { return tarifaNacionalPorKm; }
    public void setTarifaNacionalPorKm(Long tarifaNacionalPorKm) { this.tarifaNacionalPorKm = tarifaNacionalPorKm; }

    public Long getMontoMinimoEnvioGratis() { return montoMinimoEnvioGratis; }
    public void setMontoMinimoEnvioGratis(Long montoMinimoEnvioGratis) { this.montoMinimoEnvioGratis = montoMinimoEnvioGratis; }

    public Integer getDiasEntregaLocalPropio() { return diasEntregaLocalPropio; }
    public void setDiasEntregaLocalPropio(Integer diasEntregaLocalPropio) { this.diasEntregaLocalPropio = diasEntregaLocalPropio; }

    public Integer getDiasEntregaLocalTransport() { return diasEntregaLocalTransport; }
    public void setDiasEntregaLocalTransport(Integer diasEntregaLocalTransport) { this.diasEntregaLocalTransport = diasEntregaLocalTransport; }

    public Integer getDiasEntregaNacional() { return diasEntregaNacional; }
    public void setDiasEntregaNacional(Integer diasEntregaNacional) { this.diasEntregaNacional = diasEntregaNacional; }

    public String getNombreTransportadora() { return nombreTransportadora; }
    public void setNombreTransportadora(String nombreTransportadora) { this.nombreTransportadora = nombreTransportadora; }

    public Long getPrecioCiudadCentavos() { return precioCiudadCentavos; }
    public void setPrecioCiudadCentavos(Long precioCiudadCentavos) { this.precioCiudadCentavos = precioCiudadCentavos; }

    public Long getPrecioDepartamentoCentavos() { return precioDepartamentoCentavos; }
    public void setPrecioDepartamentoCentavos(Long precioDepartamentoCentavos) { this.precioDepartamentoCentavos = precioDepartamentoCentavos; }

    public Long getPrecioNacionalCentavos() { return precioNacionalCentavos; }
    public void setPrecioNacionalCentavos(Long precioNacionalCentavos) { this.precioNacionalCentavos = precioNacionalCentavos; }

    public Integer getDiasEntregaCiudad() { return diasEntregaCiudad; }
    public void setDiasEntregaCiudad(Integer diasEntregaCiudad) { this.diasEntregaCiudad = diasEntregaCiudad; }

    public Integer getDiasEntregaDepartamento() { return diasEntregaDepartamento; }
    public void setDiasEntregaDepartamento(Integer diasEntregaDepartamento) { this.diasEntregaDepartamento = diasEntregaDepartamento; }
}
