package com.alexandergomez.wms.inventory;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByOrderIdOrderByOccurredAtAscIdAsc(Long orderId);

    Optional<StockMovement> findByPickingTaskId(Long pickingTaskId);
}
