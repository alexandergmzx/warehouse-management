package com.alexandergomez.wms.orders;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {

    List<OrderLine> findByOrderIdOrderByLineNumber(Long orderId);
}
