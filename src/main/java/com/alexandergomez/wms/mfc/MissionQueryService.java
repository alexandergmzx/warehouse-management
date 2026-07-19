package com.alexandergomez.wms.mfc;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alexandergomez.wms.api.ProblemCode;
import com.alexandergomez.wms.api.ProblemException;
import com.alexandergomez.wms.catalog.Location;
import com.alexandergomez.wms.catalog.LocationRepository;

/** Admin-facing read side of {@link MfcMission} (diagnostic use only). */
@Service
public class MissionQueryService {

    private final MfcMissionRepository missions;
    private final MfcMissionTransitionRepository transitions;
    private final LocationRepository locations;

    public MissionQueryService(MfcMissionRepository missions, MfcMissionTransitionRepository transitions,
            LocationRepository locations) {
        this.missions = missions;
        this.transitions = transitions;
        this.locations = locations;
    }

    @Transactional(readOnly = true)
    public MissionDetailResponse findById(Long missionId) {
        MfcMission mission = missions.findById(missionId)
                .orElseThrow(() -> new ProblemException(ProblemCode.MISSION_NOT_FOUND, "Mission not found."));
        String sourceCode = locationCode(mission.getSourceLocationId());
        String destinationCode = locationCode(mission.getDestinationLocationId());
        List<MissionDetailResponse.TransitionSummary> history = transitions
                .findByMfcMissionIdOrderByOccurredAtAscIdAsc(mission.getId())
                .stream()
                .map(t -> new MissionDetailResponse.TransitionSummary(
                        t.getPreviousState() == null ? null : t.getPreviousState().name(),
                        t.getNewState().name(), t.getReason(), t.getOccurredAt()))
                .toList();
        return new MissionDetailResponse(mission.getId(), mission.getMissionType().name(), mission.getState().name(),
                mission.getOrderId(), mission.getOrderNumber(), sourceCode, destinationCode,
                mission.getAttempts(), mission.getLastError(), history);
    }

    private String locationCode(Long locationId) {
        return locations.findById(locationId).map(Location::getCode).orElse(null);
    }
}
