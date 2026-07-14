package com.alexandergomez.wms.picking;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PickingTaskRepository extends JpaRepository<PickingTask, Long> {

    Optional<PickingTask> findByTaskNumber(String taskNumber);

    List<PickingTask> findByOrderLineIdOrderByTaskSequence(Long orderLineId);

    long countByStatus(TaskStatus status);

    boolean existsByAssignedDeviceIdAndStatusInAndAssignedUserIdNot(
            Long assignedDeviceId, Collection<TaskStatus> statuses, Long assignedUserId);
}
