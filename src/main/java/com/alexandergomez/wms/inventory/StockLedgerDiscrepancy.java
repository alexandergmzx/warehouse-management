package com.alexandergomez.wms.inventory;

/**
 * One stock bin whose on-hand quantity disagrees with the sum of its movement
 * ledger deltas. A clean fixture reconciles to an empty result (FT-13).
 */
public record StockLedgerDiscrepancy(
        long articleId,
        long locationId,
        int stockQuantity,
        long ledgerQuantity) {
}
