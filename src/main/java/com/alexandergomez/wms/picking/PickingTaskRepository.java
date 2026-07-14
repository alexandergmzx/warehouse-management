package com.alexandergomez.wms.picking;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PickingTaskRepository extends JpaRepository<PickingTask, Long> {

    Optional<PickingTask> findByTaskNumber(String taskNumber);

    List<PickingTask> findByOrderLineIdOrderByTaskSequence(Long orderLineId);

    long countByStatus(TaskStatus status);

    long countByOrderLineIdAndStatusNot(Long orderLineId, TaskStatus status);

    boolean existsByAssignedDeviceIdAndStatusInAndAssignedUserIdNot(
            Long assignedDeviceId, Collection<TaskStatus> statuses, Long assignedUserId);

    /** The caller's current active task, if any (at most one per user, ADR 0004). */
    Optional<PickingTask> findByAssignedUserIdAndStatusIn(Long assignedUserId, Collection<TaskStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PickingTask t WHERE t.id = :id")
    Optional<PickingTask> findByIdForUpdate(@Param("id") Long id);
}
