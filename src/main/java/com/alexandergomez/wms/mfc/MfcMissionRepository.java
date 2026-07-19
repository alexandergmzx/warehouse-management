package com.alexandergomez.wms.mfc;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface MfcMissionRepository extends JpaRepository<MfcMission, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MfcMission m WHERE m.id = :id")
    Optional<MfcMission> findByIdForUpdate(@Param("id") Long id);
}
