package com.alexandergomez.wms.security;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.alexandergomez.wms.api.CorrelationIdFilter;
import com.alexandergomez.wms.api.ProblemCode;
import com.alexandergomez.wms.api.ProblemDetailFactory;
import com.alexandergomez.wms.api.ProblemResponseWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Renders unauthenticated access (missing, invalid, expired, or revoked token)
 * as an RFC 9457 problem. The specific code is taken from the request attribute
 * set by {@link BearerTokenAuthenticationFilter}; a missing token defaults to
 * {@code INVALID_TOKEN}.
 */
@Component
public class WmsAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ProblemDetailFactory problems;
    private final ProblemResponseWriter writer;

    public WmsAuthenticationEntryPoint(ProblemDetailFactory problems, ProblemResponseWriter writer) {
        this.problems = problems;
        this.writer = writer;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        Object attribute = request.getAttribute(BearerTokenAuthenticationFilter.PROBLEM_ATTRIBUTE);
        ProblemCode code = attribute instanceof ProblemCode problemCode
                ? problemCode : ProblemCode.INVALID_TOKEN;
        ProblemDetail problem = problems.create(
                code, null, CorrelationIdFilter.currentCorrelationId(request), Map.of());
        writer.write(response, problem);
    }
}
