package com.alexandergomez.wms.mfc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.alexandergomez.wms.catalog.LocationRepository;
import com.alexandergomez.wms.configuration.MfcProperties;
import com.alexandergomez.wms.orders.OrderCompletionEvent;
import com.alexandergomez.wms.orders.OrderCompletionPublisher;

/**
 * The real {@link OrderCompletionPublisher} adapter (ADR 0011): writes one
 * {@code PENDING} {@link MfcMission} row in the same transaction that
 * completes the order — {@code publish()} is called synchronously,
 * pre-commit, from {@code picking.PickingService} (ADR 0007). The {@link
 * MissionDispatcher} owns everything after that: serialization, delivery,
 * retries, and state transitions. Selected by {@code wms.mfc.adapter=
 * telegram}; refuses to start (constructor throws) if the required transport
 * location configuration is missing.
 */
@Component
@ConditionalOnProperty(prefix = "wms.mfc", name = "adapter", havingValue = "telegram")
public final class TelegramOrderCompletionPublisher implements OrderCompletionPublisher {

    private static final Logger log = LoggerFactory.getLogger(TelegramOrderCompletionPublisher.class);

    private final MfcMissionRepository missions;
    private final LocationRepository locations;
    private final String sourceLocationCode;
    private final String destinationLocationCode;

    public TelegramOrderCompletionPublisher(MfcMissionRepository missions, LocationRepository locations,
            MfcProperties properties) {
        this.missions = missions;
        this.locations = locations;
        this.sourceLocationCode = properties.transport().sourceLocation();
        this.destinationLocationCode = properties.transport().destinationLocation();
        if (isBlank(sourceLocationCode) || isBlank(destinationLocationCode)) {
            throw new IllegalStateException(
                    "wms.mfc.transport.source-location and wms.mfc.transport.destination-location "
                            + "are required when wms.mfc.adapter=telegram");
        }
    }

    @Override
    public void publish(OrderCompletionEvent event) {
        Long sourceLocationId = resolveLocationId(sourceLocationCode);
        Long destinationLocationId = resolveLocationId(destinationLocationCode);
        OffsetDateTime now = OffsetDateTime.ofInstant(event.completedAt(), ZoneOffset.UTC);
        MfcMission mission = MfcMission.createTransport(event.eventId(), event.orderId(), event.orderNumber(),
                sourceLocationId, destinationLocationId, now);
        missions.save(mission);
        log.atInfo()
                .addKeyValue("eventId", event.eventId())
                .addKeyValue("orderNumber", event.orderNumber())
                .log("MFC TRANSPORT mission queued for dispatch");
    }

    private Long resolveLocationId(String code) {
        return locations.findByCode(code)
                .orElseThrow(() -> new IllegalStateException(
                        "Configured MFC transport location '" + code + "' does not exist"))
                .getId();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
