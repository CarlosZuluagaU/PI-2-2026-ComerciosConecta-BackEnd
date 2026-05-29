package com.comerciosconecta.backend.repository;

import com.comerciosconecta.backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByUuid(String uuid);

    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findByComercioIdOrderByCreatedAtDesc(Integer comercioId);
    long countByComercioId(Integer comercioId);

    @Query("SELECT o FROM Order o WHERE LOWER(o.customerEmail) = LOWER(:email) AND o.comercioOrderNumber = :num")
    Optional<Order> findByEmailAndComercioOrderNumber(@Param("email") String email, @Param("num") Integer num);

    @Query("SELECT o FROM Order o WHERE LOWER(o.customerEmail) = LOWER(:email) AND o.uuid = :uuid")
    Optional<Order> findByEmailAndUuid(@Param("email") String email, @Param("uuid") String uuid);

    List<Order> findByCustomerEmailIgnoreCaseAndComercioIdOrderByCreatedAtDesc(String email, Integer comercioId);

    List<Order> findByCustomerDocumentAndComercioIdOrderByCreatedAtDesc(String document, Integer comercioId);
}
