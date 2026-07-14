package com.alexandergomez.wms.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alexandergomez.wms.identity.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/stock")
public class StockAdminController {

    private final StockAdminService stockAdminService;

    public StockAdminController(StockAdminService stockAdminService) {
        this.stockAdminService = stockAdminService;
    }

    @PostMapping("/adjustments")
    public ResponseEntity<StockAdjustmentResponse> adjust(
            @AuthenticationPrincipal AuthenticatedUser admin, @Valid @RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.status(201).body(stockAdminService.adjust(admin.userId(), request));
    }
}
