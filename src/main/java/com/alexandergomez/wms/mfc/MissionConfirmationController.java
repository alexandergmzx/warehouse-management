package com.alexandergomez.wms.mfc;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * WCS-facing mission confirmation endpoint (TELEGRAMS.md). Requires a bearer
 * token with the {@code WCS} role; the global security chain enforces this
 * (see {@code security.SecurityConfiguration}).
 */
@RestController
@RequestMapping("/api/v1/mfc/missions")
public class MissionConfirmationController {

    private final MissionConfirmationService confirmationService;

    public MissionConfirmationController(MissionConfirmationService confirmationService) {
        this.confirmationService = confirmationService;
    }

    @PostMapping("/{missionId}/confirmations")
    public MissionConfirmationResponse confirm(
            @PathVariable Long missionId, @Valid @RequestBody MissionConfirmationRequest request) {
        return confirmationService.confirm(missionId, request);
    }
}
