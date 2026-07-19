package com.alexandergomez.wms.mfc;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MfcMissionTransitionRepository extends JpaRepository<MfcMissionTransition, Long> {

    List<MfcMissionTransition> findByMfcMissionIdOrderByOccurredAtAscIdAsc(Long mfcMissionId);
}
