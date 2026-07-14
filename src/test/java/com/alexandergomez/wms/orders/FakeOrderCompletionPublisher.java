package com.alexandergomez.wms.orders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test fake for the Phase 10 MFC extension seam (ADR 0007): records every
 * published event for assertion instead of doing anything with it. Used in
 * place of {@code NoopOrderCompletionPublisher} to prove the seam actually
 * fires, not just that it compiles.
 */
public class FakeOrderCompletionPublisher implements OrderCompletionPublisher {

    private final List<OrderCompletionEvent> published = new ArrayList<>();

    @Override
    public void publish(OrderCompletionEvent event) {
        published.add(event);
    }

    public List<OrderCompletionEvent> published() {
        return Collections.unmodifiableList(published);
    }
}
