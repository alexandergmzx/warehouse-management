package com.alexandergomez.wms.catalog;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    Optional<Article> findBySku(String sku);

    Optional<Article> findByQrValue(String qrValue);
}
