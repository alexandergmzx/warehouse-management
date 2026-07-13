package com.alexandergomez.wms.catalog;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Storage location (bin). Multi-bin picking slices ascend by location
 * {@code code}; {@code pick_sequence} orders walking of the warehouse.
 */
@Entity
@Table(name = "location")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "qr_value", nullable = false, length = 100)
    private String qrValue;

    @Column(name = "pick_sequence", nullable = false)
    private Integer pickSequence;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Location() {
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getQrValue() {
        return qrValue;
    }

    public Integer getPickSequence() {
        return pickSequence;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
