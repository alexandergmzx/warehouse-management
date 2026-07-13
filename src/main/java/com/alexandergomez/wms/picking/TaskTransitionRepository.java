package com.alexandergomez.wms.picking;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskTransitionRepository extends JpaRepository<TaskTransition, Long> {

    List<TaskTransition> findByPickingTaskIdOrderByOccurredAtAscIdAsc(Long pickingTaskId);
}
