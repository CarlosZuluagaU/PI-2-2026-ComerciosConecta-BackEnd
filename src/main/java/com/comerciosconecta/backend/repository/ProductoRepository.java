package com.comerciosconecta.backend.repository;

import com.comerciosconecta.backend.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;



@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // Buscar producto por referencia (que es única)
    Optional<Producto> findByReferencia(String referencia);

    // Verificar si existe un producto por nombre
    boolean existsByNombre(String nombre);

    // Verificar si existe un producto por referencia
    boolean existsByReferencia(String referencia);

    // Buscar productos por comercio
    List<Producto> findByComercioId(Long comercioId);

    // Productos con stock <= stockMinimo (alerta de reorden)
    @org.springframework.data.jpa.repository.Query("SELECT p FROM Producto p WHERE p.stock <= p.stockMinimo")
    List<Producto> findLowStockProductos();

    // Categorías distintas por comercio
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT p.categoria FROM Producto p WHERE p.comercio.id = :comercioId AND p.categoria IS NOT NULL AND p.categoria <> ''")
    List<String> findDistinctCategoriasByComercioId(@org.springframework.data.repository.query.Param("comercioId") Long comercioId);

    // Marcas distintas por comercio
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT p.marca FROM Producto p WHERE p.comercio.id = :comercioId AND p.marca IS NOT NULL AND p.marca <> ''")
    List<String> findDistinctMarcasByComercioId(@org.springframework.data.repository.query.Param("comercioId") Long comercioId);
}

