package com.alexandergomez.wms.admin;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alexandergomez.wms.api.CorrelationIdFilter;
import com.alexandergomez.wms.api.ProblemCode;
import com.alexandergomez.wms.api.ProblemException;
import com.alexandergomez.wms.catalog.Article;
import com.alexandergomez.wms.catalog.ArticleRepository;
import com.alexandergomez.wms.catalog.Location;
import com.alexandergomez.wms.catalog.LocationRepository;
import com.alexandergomez.wms.inventory.Stock;
import com.alexandergomez.wms.inventory.StockMovement;
import com.alexandergomez.wms.inventory.StockMovementRepository;
import com.alexandergomez.wms.inventory.StockRepository;

/**
 * Signed manual stock corrections (API.md), appending an {@code ADJUSTMENT}
 * movement in the same transaction as the stock update. Order creation,
 * confirm, and this endpoint all update {@code stock} before inserting the
 * matching movement (architecture.md), so the flush-then-insert ordering here
 * matches the pattern already established by {@code PickingService.confirm}.
 */
@Service
public class StockAdminService {

    private final ArticleRepository articles;
    private final LocationRepository locations;
    private final StockRepository stock;
    private final StockMovementRepository stockMovements;

    public StockAdminService(ArticleRepository articles, LocationRepository locations,
            StockRepository stock, StockMovementRepository stockMovements) {
        this.articles = articles;
        this.locations = locations;
        this.stock = stock;
        this.stockMovements = stockMovements;
    }

    @Transactional
    public StockAdjustmentResponse adjust(Long adminUserId, StockAdjustmentRequest request) {
        if (request.quantityDelta() == 0) {
            throw new ProblemException(ProblemCode.VALIDATION_FAILED, "quantityDelta must not be zero.");
        }
        Article article = articles.findBySku(request.articleSku())
                .orElseThrow(() -> new ProblemException(ProblemCode.ARTICLE_NOT_FOUND, "Article not found."));
        Location location = locations.findByCode(request.locationCode())
                .orElseThrow(() -> new ProblemException(ProblemCode.LOCATION_NOT_FOUND, "Location not found."));

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Stock stockRow = stock.findByIdForUpdate(article.getId(), location.getId())
                .orElseGet(() -> stock.saveAndFlush(Stock.initial(article.getId(), location.getId(), now)));

        int resultingQuantity = stockRow.getQuantity() + request.quantityDelta();
        if (resultingQuantity < 0) {
            throw new ProblemException(ProblemCode.NEGATIVE_RESULTING_STOCK,
                    "Adjustment would result in negative stock.");
        }
        stockRow.applyDelta(request.quantityDelta(), now);
        stock.saveAndFlush(stockRow);

        UUID correlationId = currentCorrelationUuid();
        StockMovement movement = stockMovements.save(StockMovement.adjustment(article.getId(), location.getId(),
                request.quantityDelta(), resultingQuantity, adminUserId, request.reason(), correlationId, now));

        return new StockAdjustmentResponse(movement.getId(), article.getSku(), location.getCode(),
                request.quantityDelta(), resultingQuantity);
    }

    private static UUID currentCorrelationUuid() {
        try {
            return UUID.fromString(CorrelationIdFilter.currentCorrelationId());
        } catch (IllegalArgumentException ex) {
            return UUID.randomUUID();
        }
    }
}
