package com.comerciosconecta.backend.repository;

import com.comerciosconecta.backend.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    Optional<Venta> findByUuid(String uuid);
    Optional<Venta> findByReferencia(String referencia);
    java.util.List<Venta> findByComercioIdOrderByCreatedAtDesc(Integer comercioId);
}
