package com.comerciosconecta.backend.repository;

import com.comerciosconecta.backend.entity.SeguimientoEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SeguimientoEnvioRepository extends JpaRepository<SeguimientoEnvio, Long> {
    List<SeguimientoEnvio> findByEnvioIdOrderByFechaAsc(Long envioId);
}
