package com.alexandergomez.wms.admin;

public record StockAdjustmentResponse(
        Long movementId, String articleSku, String locationCode, Integer quantityDelta, Integer resultingQuantity) {
}
