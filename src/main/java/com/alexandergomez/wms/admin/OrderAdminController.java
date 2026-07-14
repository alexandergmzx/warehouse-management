package com.alexandergomez.wms.admin;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alexandergomez.wms.identity.AuthenticatedUser;

import jakarta.validation.Valid;

/**
 * Administration order endpoints (API.md). {@code /api/v1/admin/**} is
 * restricted to the {@code ADMIN} role by the security filter chain.
 */
@RestController
@RequestMapping("/api/v1/admin/orders")
public class OrderAdminController {

    private final OrderAdminService orderAdminService;

    public OrderAdminController(OrderAdminService orderAdminService) {
        this.orderAdminService = orderAdminService;
    }

    @PostMapping
    public ResponseEntity<CreateOrderResponse> create(
            @AuthenticationPrincipal AuthenticatedUser admin, @Valid @RequestBody CreateOrderRequest request) {
        CreateOrderResponse response = orderAdminService.createOrder(admin.userId(), request);
        return ResponseEntity.created(URI.create("/api/v1/admin/orders/" + response.orderNumber())).body(response);
    }

    @GetMapping("/{orderNumber}")
    public OrderDetailResponse get(@PathVariable String orderNumber) {
        return orderAdminService.getOrder(orderNumber);
    }
}
