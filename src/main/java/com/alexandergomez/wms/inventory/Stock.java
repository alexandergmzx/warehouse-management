package com.alexandergomez.wms.inventory;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Stock on hand for one article at one location. The {@code version} column is
 * mapped as a plain value (not a JPA {@code @Version}); concurrency control is
 * pessimistic row locking under {@code READ COMMITTED} per ADR 0003.
 */
@Entity
@Table(name = "stock")
@IdClass(StockId.class)
public class Stock {

    @Id
    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Id
    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Stock() {
    }

    public Long getArticleId() {
        return articleId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Long getVersion() {
        return version;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
