package com.alexandergomez.wms.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Application exception carrying a stable {@link ProblemCode} and optional safe
 * extension members. The global handler renders it as RFC 9457
 * {@code application/problem+json}. Extensions must never contain credentials,
 * tokens, hashes, or internal exception details.
 */
public class ProblemException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ProblemCode problemCode;
    private final LinkedHashMap<String, Object> extensions;

    public ProblemException(ProblemCode problemCode) {
        this(problemCode, problemCode.title(), Map.of());
    }

    public ProblemException(ProblemCode problemCode, String detail) {
        this(problemCode, detail, Map.of());
    }

    public ProblemException(ProblemCode problemCode, String detail, Map<String, Object> extensions) {
        super(detail);
        this.problemCode = problemCode;
        this.extensions = new LinkedHashMap<>(extensions);
    }

    public ProblemCode problemCode() {
        return problemCode;
    }

    public Map<String, Object> extensions() {
        return Collections.unmodifiableMap(extensions);
    }
}
