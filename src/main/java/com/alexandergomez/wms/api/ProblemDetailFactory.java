package com.alexandergomez.wms.api;

import java.util.Map;

import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

/**
 * Builds RFC 9457 {@link ProblemDetail} bodies with the required stable members
 * {@code type}, {@code title}, {@code status}, {@code code}, and
 * {@code correlationId}, plus optional safe extensions.
 */
@Component
public class ProblemDetailFactory {

    public ProblemDetail create(ProblemCode code, String detail, String correlationId,
            Map<String, Object> extensions) {
        ProblemDetail problem = ProblemDetail.forStatus(code.status());
        problem.setType(code.type());
        problem.setTitle(code.title());
        problem.setProperty("code", code.code());
        problem.setProperty("correlationId", correlationId);
        if (detail != null && !detail.isBlank()) {
            problem.setDetail(detail);
        }
        extensions.forEach(problem::setProperty);
        return problem;
    }
}
