package com.alexandergomez.wms.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alexandergomez.wms.mfc.MissionDetailResponse;
import com.alexandergomez.wms.mfc.MissionQueryService;

/** Admin diagnostic read of MFC missions (ADR 0011); covered by the existing {@code /api/v1/admin/**} security rule. */
@RestController
@RequestMapping("/api/v1/admin/mfc/missions")
public class MfcAdminController {

    private final MissionQueryService missionQueryService;

    public MfcAdminController(MissionQueryService missionQueryService) {
        this.missionQueryService = missionQueryService;
    }

    @GetMapping("/{missionId}")
    public MissionDetailResponse findById(@PathVariable Long missionId) {
        return missionQueryService.findById(missionId);
    }
}
