package com.comerciosconecta.backend.repository;

import com.comerciosconecta.backend.entity.ConfiguracionEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConfiguracionEnvioRepository extends JpaRepository<ConfiguracionEnvio, Long> {
    Optional<ConfiguracionEnvio> findByComercioId(Integer comercioId);
}
