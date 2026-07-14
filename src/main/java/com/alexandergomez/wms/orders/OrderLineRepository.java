package com.alexandergomez.wms.orders;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {

    List<OrderLine> findByOrderIdOrderByLineNumber(Long orderId);

    long countByOrderIdAndStatusNot(Long orderId, OrderLineStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ol FROM OrderLine ol WHERE ol.id = :id")
    Optional<OrderLine> findByIdForUpdate(@Param("id") Long id);
}
