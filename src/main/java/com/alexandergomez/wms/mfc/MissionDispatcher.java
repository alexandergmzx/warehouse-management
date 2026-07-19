package com.alexandergomez.wms.mfc;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.alexandergomez.wms.catalog.Location;
import com.alexandergomez.wms.catalog.LocationRepository;
import com.alexandergomez.wms.configuration.MfcProperties;

import jakarta.annotation.PostConstruct;

/**
 * Polls {@link MfcMission} rows in {@code PENDING} state and POSTs each as
 * one telegram to the WCS (ADR 0011, TELEGRAMS.md). Claiming reuses the same
 * {@code FOR UPDATE SKIP LOCKED} pattern the picking module uses for FIFO
 * task claims ({@link MfcMissionJdbcRepository}); each claimed mission is
 * dispatched in its own transaction so one failure never blocks the rest of
 * the queue. Selected by {@code wms.mfc.adapter=telegram}; refuses to start
 * if {@code wms.mfc.telegram.base-url} is missing — validated in {@link
 * PostConstruct}, not the constructor, since {@link #dispatchNextOnce()}'s
 * {@code @Transactional} requires Spring to CGLIB-proxy this class (so it
 * cannot be {@code final}, and a constructor that leaves the object
 * partially initialized on failure is worth avoiding regardless).
 */
@Component
@ConditionalOnProperty(prefix = "wms.mfc", name = "adapter", havingValue = "telegram")
public class MissionDispatcher {

    private static final Logger log = LoggerFactory.getLogger(MissionDispatcher.class);

    private final MfcMissionJdbcRepository claimQueue;
    private final MfcMissionRepository missions;
    private final MfcMissionTransitionRepository transitions;
    private final LocationRepository locations;
    private final MfcProperties properties;
    private RestClient restClient;
    private Duration retryInterval;
    private int maxAttempts;

    public MissionDispatcher(MfcMissionJdbcRepository claimQueue, MfcMissionRepository missions,
            MfcMissionTransitionRepository transitions, LocationRepository locations, MfcProperties properties) {
        this.claimQueue = claimQueue;
        this.missions = missions;
        this.transitions = transitions;
        this.locations = locations;
        this.properties = properties;
    }

    @PostConstruct
    void initialize() {
        String baseUrl = properties.telegram().baseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException(
                    "wms.mfc.telegram.base-url is required when wms.mfc.adapter=telegram");
        }
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.retryInterval = properties.telegram().retryInterval();
        this.maxAttempts = properties.telegram().maxAttempts();
    }

    @Scheduled(fixedDelayString = "${wms.mfc.telegram.retry-interval:PT30S}")
    public void dispatchPending() {
        while (dispatchNextOnce()) {
            // drain every currently-claimable PENDING mission each tick
        }
    }

    /**
     * Claims and dispatches at most one mission, in its own transaction.
     * Returns {@code true} if a mission was claimed (whether or not delivery
     * succeeded), so the caller can keep draining the queue.
     */
    @Transactional
    public boolean dispatchNextOnce() {
        Optional<Long> claimedId = claimQueue.claimNextDispatchableMissionId();
        if (claimedId.isEmpty()) {
            return false;
        }
        MfcMission mission = missions.findByIdForUpdate(claimedId.get())
                .orElseThrow(() -> new IllegalStateException("Claimed mission vanished: " + claimedId.get()));
        attemptDispatch(mission);
        return true;
    }

    private void attemptDispatch(MfcMission mission) {
        mission.recordDispatchAttempt();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        try {
            TelegramPayload payload = toPayload(mission, now);
            ResponseEntity<Void> response = restClient.post()
                    .uri("/missions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("WCS responded " + response.getStatusCode().value());
            }
            MissionState previous = mission.getState();
            mission.markDispatched();
            missions.save(mission);
            transitions.save(MfcMissionTransition.record(
                    mission.getId(), previous, MissionState.DISPATCHED, null, UUID.randomUUID(), now));
            log.atInfo()
                    .addKeyValue("missionId", mission.getId())
                    .addKeyValue("eventId", mission.getEventId())
                    .addKeyValue("attempts", mission.getAttempts())
                    .log("MFC mission dispatched");
        } catch (RestClientException | IllegalStateException ex) {
            handleFailure(mission, ex.getMessage(), now);
        }
    }

    private void handleFailure(MfcMission mission, String error, OffsetDateTime now) {
        if (mission.getAttempts() >= maxAttempts) {
            MissionState previous = mission.getState();
            mission.markDispatchExhausted(error);
            missions.save(mission);
            transitions.save(MfcMissionTransition.record(
                    mission.getId(), previous, MissionState.FAILED, error, UUID.randomUUID(), now));
            log.atWarn()
                    .addKeyValue("missionId", mission.getId())
                    .addKeyValue("eventId", mission.getEventId())
                    .addKeyValue("attempts", mission.getAttempts())
                    .addKeyValue("error", error)
                    .log("MFC mission dispatch exhausted; marked FAILED");
        } else {
            mission.scheduleRetry(error, now.plus(retryInterval));
            missions.save(mission);
            log.atInfo()
                    .addKeyValue("missionId", mission.getId())
                    .addKeyValue("attempts", mission.getAttempts())
                    .addKeyValue("error", error)
                    .log("MFC mission dispatch failed; retry scheduled");
        }
    }

    private TelegramPayload toPayload(MfcMission mission, OffsetDateTime now) {
        Location source = locations.findById(mission.getSourceLocationId())
                .orElseThrow(() -> new IllegalStateException("Mission source location vanished"));
        Location destination = locations.findById(mission.getDestinationLocationId())
                .orElseThrow(() -> new IllegalStateException("Mission destination location vanished"));
        return new TelegramPayload(mission.getId(), mission.getEventId(), mission.getMissionType().name(),
                mission.getOrderId(), mission.getOrderNumber(), source.getCode(), destination.getCode(),
                MissionState.DISPATCHED.name(), now);
    }
}
