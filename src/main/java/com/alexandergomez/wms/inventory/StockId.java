package com.alexandergomez.wms.inventory;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite identifier for {@link Stock} ({@code article_id}, {@code location_id}).
 */
public class StockId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long articleId;
    private Long locationId;

    public StockId() {
    }

    public StockId(Long articleId, Long locationId) {
        this.articleId = articleId;
        this.locationId = locationId;
    }

    public Long getArticleId() {
        return articleId;
    }

    public Long getLocationId() {
        return locationId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof StockId that)) {
            return false;
        }
        return Objects.equals(articleId, that.articleId)
                && Objects.equals(locationId, that.locationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(articleId, locationId);
    }
}
