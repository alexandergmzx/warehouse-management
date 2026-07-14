package com.alexandergomez.wms.admin;

public record CreateLocationResponse(Long id, String code, String qrValue, Integer pickSequence, boolean active) {
}
