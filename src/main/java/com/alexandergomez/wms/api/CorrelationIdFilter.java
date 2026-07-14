package com.alexandergomez.wms.api;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Assigns a correlation identifier to every request, echoes it on the response,
 * and publishes it to SLF4J MDC so operational logs correlate without exposing
 * credentials or tokens. A client-supplied {@code X-Correlation-Id} is honoured
 * only when it is a valid UUID; otherwise a fresh one is generated.
 *
 * <p>Ordered ahead of the Spring Security filter chain so the identifier is
 * available to the authentication entry point and access-denied handler.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String REQUEST_ATTRIBUTE = "wms.correlationId";
    private static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String correlationId = resolve(request.getHeader(HEADER));
        request.setAttribute(REQUEST_ATTRIBUTE, correlationId);
        response.setHeader(HEADER, correlationId);
        MDC.put(MDC_KEY, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    /**
     * Returns the correlation identifier bound to the current request, or a new
     * one when the filter has not run (defensive fallback).
     */
    public static String currentCorrelationId(HttpServletRequest request) {
        Object value = request.getAttribute(REQUEST_ATTRIBUTE);
        return value instanceof String correlationId ? correlationId : UUID.randomUUID().toString();
    }

    /**
     * Returns the correlation identifier for the request executing on the
     * current thread, read from MDC. For use in service-layer code that has no
     * direct access to the {@link HttpServletRequest} (for example, when
     * stamping an audit or movement row with the request that caused it).
     */
    public static String currentCorrelationId() {
        String value = MDC.get(MDC_KEY);
        return value != null ? value : UUID.randomUUID().toString();
    }

    private static String resolve(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return UUID.randomUUID().toString();
        }
        try {
            return UUID.fromString(candidate.trim()).toString();
        } catch (IllegalArgumentException ignored) {
            return UUID.randomUUID().toString();
        }
    }
}
