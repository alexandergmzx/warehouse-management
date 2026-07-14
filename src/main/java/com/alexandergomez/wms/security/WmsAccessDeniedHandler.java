package com.alexandergomez.wms.security;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.alexandergomez.wms.api.CorrelationIdFilter;
import com.alexandergomez.wms.api.ProblemCode;
import com.alexandergomez.wms.api.ProblemDetailFactory;
import com.alexandergomez.wms.api.ProblemResponseWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Renders an authenticated caller without the required role as
 * {@code 403 FORBIDDEN} in RFC 9457 form.
 */
@Component
public class WmsAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemDetailFactory problems;
    private final ProblemResponseWriter writer;

    public WmsAccessDeniedHandler(ProblemDetailFactory problems, ProblemResponseWriter writer) {
        this.problems = problems;
        this.writer = writer;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        ProblemDetail problem = problems.create(
                ProblemCode.FORBIDDEN, null, CorrelationIdFilter.currentCorrelationId(request), Map.of());
        writer.write(response, problem);
    }
}
