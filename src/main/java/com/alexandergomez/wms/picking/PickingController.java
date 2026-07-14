package com.alexandergomez.wms.picking;

import java.util.Optional;

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
 * HHT picking workflow endpoints (API.md). All require a bearer token; the
 * global security chain enforces authentication, so every method here can
 * assume {@code caller} is present.
 */
@RestController
@RequestMapping("/api/v1/hht/tasks")
public class PickingController {

    private final PickingService pickingService;

    public PickingController(PickingService pickingService) {
        this.pickingService = pickingService;
    }

    @GetMapping("/next")
    public ResponseEntity<NextTaskResponse> nextTask(@AuthenticationPrincipal AuthenticatedUser caller) {
        Optional<NextTaskResponse> task = pickingService.nextTask(caller);
        return task.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{taskId}/scan-location")
    public ScanLocationResponse scanLocation(@AuthenticationPrincipal AuthenticatedUser caller,
            @PathVariable Long taskId, @Valid @RequestBody ScanRequest request) {
        return pickingService.scanLocation(caller, taskId, request.qrValue());
    }

    @PostMapping("/{taskId}/scan-article")
    public ScanArticleResponse scanArticle(@AuthenticationPrincipal AuthenticatedUser caller,
            @PathVariable Long taskId, @Valid @RequestBody ScanRequest request) {
        return pickingService.scanArticle(caller, taskId, request.qrValue());
    }

    @PostMapping("/{taskId}/confirm")
    public ConfirmResponse confirm(@AuthenticationPrincipal AuthenticatedUser caller,
            @PathVariable Long taskId, @Valid @RequestBody ConfirmRequest request) {
        return pickingService.confirm(caller, taskId, request.confirmationId(), request.quantity());
    }
}
