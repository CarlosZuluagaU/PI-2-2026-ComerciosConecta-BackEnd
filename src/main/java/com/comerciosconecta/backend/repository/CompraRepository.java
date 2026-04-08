package com.comerciosconecta.backend.repository;

import com.comerciosconecta.backend.entity.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CompraRepository extends JpaRepository<Compra, Long> {
    List<Compra> findByComercioId(Integer comercioId);
}
