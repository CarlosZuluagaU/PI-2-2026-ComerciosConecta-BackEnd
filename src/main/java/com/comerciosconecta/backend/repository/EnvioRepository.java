package com.comerciosconecta.backend.repository;

import com.comerciosconecta.backend.entity.Envio;
import com.comerciosconecta.backend.entity.EstadoEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EnvioRepository extends JpaRepository<Envio, Long> {
    Optional<Envio> findByOrderId(Long orderId);
    Optional<Envio> findByNumeroGuia(String numeroGuia);
    List<Envio> findByComercioIdOrderByFechaCreacionDesc(Integer comercioId);
    List<Envio> findByComercioIdAndEstadoEnvioOrderByFechaCreacionDesc(Integer comercioId, EstadoEnvio estadoEnvio);
}
