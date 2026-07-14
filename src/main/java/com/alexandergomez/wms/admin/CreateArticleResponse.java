package com.alexandergomez.wms.admin;

public record CreateArticleResponse(Long id, String sku, String description, String qrValue, boolean active) {
}
