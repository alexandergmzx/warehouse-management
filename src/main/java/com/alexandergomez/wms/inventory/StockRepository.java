package com.alexandergomez.wms.inventory;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, StockId> {

    List<Stock> findByArticleIdOrderByLocationId(Long articleId);
}
