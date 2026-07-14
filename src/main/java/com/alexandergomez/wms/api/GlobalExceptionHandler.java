package com.alexandergomez.wms.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Renders every controller-layer failure as an RFC 9457
 * {@code application/problem+json} body with a stable {@link ProblemCode} and
 * the request correlation identifier. Security-layer 401/403 responses are
 * rendered separately by the authentication entry point and access-denied
 * handler using the same {@link ProblemDetailFactory}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ProblemDetailFactory problems;

    public GlobalExceptionHandler(ProblemDetailFactory problems) {
        this.problems = problems;
    }

    @ExceptionHandler(ProblemException.class)
    public ResponseEntity<ProblemDetail> handleProblem(ProblemException ex, WebRequest request) {
        ProblemDetail problem = problems.create(
                ex.problemCode(), ex.getMessage(), correlationId(request), ex.extensions());
        return ResponseEntity.status(ex.problemCode().status()).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = problems.create(
                ProblemCode.INTERNAL_ERROR, null, correlationId(request), Map.of());
        return ResponseEntity.status(ProblemCode.INTERNAL_ERROR.status()).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, Object> fields = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ProblemDetail problem = problems.create(ProblemCode.VALIDATION_FAILED,
                "One or more fields are invalid.", correlationId(request), Map.of("fields", fields));
        return ResponseEntity.status(ProblemCode.VALIDATION_FAILED.status()).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = problems.create(ProblemCode.MALFORMED_REQUEST,
                "Request body could not be read as valid JSON.", correlationId(request), Map.of());
        return ResponseEntity.status(ProblemCode.MALFORMED_REQUEST.status()).body(problem);
    }

    private static String correlationId(WebRequest request) {
        Object value = request.getAttribute(
                CorrelationIdFilter.REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        return value instanceof String correlationId ? correlationId : "unknown";
    }
}
