package com.alexandergomez.wms.mfc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.alexandergomez.wms.orders.OrderCompletionEvent;
import com.alexandergomez.wms.orders.OrderCompletionPublisher;

/**
 * The only {@link OrderCompletionPublisher} adapter this PoC implements
 * (ADR 0007, Phase 10). Selected by {@code wms.mfc.adapter=noop} (the
 * default): does nothing beyond one structured log line. A future
 * {@code tcp} adapter would own telegram serialization, connection timeouts,
 * delivery outcomes, retries, and transport-level observability — none of
 * which exist here; see {@code docs/architecture.md}'s MFC extension seam
 * section for those documented (not implemented) boundaries.
 */
@Component
@ConditionalOnProperty(prefix = "wms.mfc", name = "adapter", havingValue = "noop", matchIfMissing = true)
public class NoopOrderCompletionPublisher implements OrderCompletionPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoopOrderCompletionPublisher.class);

    @Override
    public void publish(OrderCompletionEvent event) {
        log.atInfo()
                .addKeyValue("eventId", event.eventId())
                .addKeyValue("orderNumber", event.orderNumber())
                .log("order completion published (no-op adapter)");
    }
}
