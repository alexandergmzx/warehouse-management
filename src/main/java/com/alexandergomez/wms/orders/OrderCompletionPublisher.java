package com.alexandergomez.wms.orders;

/**
 * Application port for the future MFC extension (ADR 0007, Phase 10):
 * order-domain code depends only on this interface and {@link
 * OrderCompletionEvent}, never on a transport (e.g. a future TCP telegram
 * adapter). The current, configuration-selected implementation is a no-op;
 * see {@code com.alexandergomez.wms.mfc}.
 */
public interface OrderCompletionPublisher {

    void publish(OrderCompletionEvent event);
}
