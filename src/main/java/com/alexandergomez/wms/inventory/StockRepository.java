package com.alexandergomez.wms.inventory;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface StockRepository extends JpaRepository<Stock, StockId> {

    List<Stock> findByArticleIdOrderByLocationId(Long articleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stock s WHERE s.articleId = :articleId AND s.locationId = :locationId")
    Optional<Stock> findByIdForUpdate(@Param("articleId") Long articleId, @Param("locationId") Long locationId);
}
