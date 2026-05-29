package com.comerciosconecta.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "seguimiento_envio")
public class SeguimientoEnvio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "envio_id", nullable = false)
    private Envio envio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoEnvio estado;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "ubicacion_actual")
    private String ubicacionActual;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    public SeguimientoEnvio() {}

    public SeguimientoEnvio(Envio envio, EstadoEnvio estado, String descripcion, String ubicacionActual) {
        this.envio = envio;
        this.estado = estado;
        this.descripcion = descripcion;
        this.ubicacionActual = ubicacionActual;
        this.fecha = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @JsonIgnore
    public Envio getEnvio() { return envio; }
    public void setEnvio(Envio envio) { this.envio = envio; }

    public EstadoEnvio getEstado() { return estado; }
    public void setEstado(EstadoEnvio estado) { this.estado = estado; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getUbicacionActual() { return ubicacionActual; }
    public void setUbicacionActual(String ubicacionActual) { this.ubicacionActual = ubicacionActual; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
