package com.incode.integration.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler translating application, validation, security, and upstream errors
 * into RFC 7807 Problem Details ({@code application/problem+json}).
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail detail = buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                "urn:incode:problem:validation-error",
                "Request Validation Failed",
                "One or more request fields failed validation.",
                request);
        detail.setProperty("errors", fieldErrors);
        return problemResponse(detail, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {
        Map<String, String> violations = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> extractLeafPath(violation.getPropertyPath().toString()),
                        ConstraintViolation::getMessage,
                        (first, second) -> first,
                        LinkedHashMap::new));
        ProblemDetail detail = buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                "urn:incode:problem:constraint-violation",
                "Constraint Violation",
                "Request parameters failed constraint validation.",
                request);
        detail.setProperty("errors", violations);
        return problemResponse(detail, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({
            InvalidDocumentException.class,
            InvalidWebhookPayloadException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            MissingRequestHeaderException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ProblemDetail> handleBadRequestExceptions(
            Exception ex,
            HttpServletRequest request) {
        String message = ex instanceof HttpMessageNotReadableException
                ? "Malformed JSON request payload."
                : ex.getMessage();
        ProblemDetail detail = buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                "urn:incode:problem:bad-request",
                "Invalid Request",
                message,
                request);
        return problemResponse(detail, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request) {
        ProblemDetail detail = buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                "urn:incode:problem:payload-too-large",
                "Uploaded File Exceeds Maximum Size",
                "The uploaded document exceeds the maximum permitted file size.",
                request);
        return problemResponse(detail, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(WebhookSignatureException.class)
    public ResponseEntity<ProblemDetail> handleWebhookSignatureFailure(
            WebhookSignatureException ex,
            HttpServletRequest request) {
        log.warn("Rejected webhook request due to signature verification failure: {}", ex.getMessage());
        ProblemDetail detail = buildProblemDetail(
                HttpStatus.UNAUTHORIZED,
                "urn:incode:problem:invalid-webhook-signature",
                "Webhook Signature Verification Failed",
                ex.getMessage(),
                request);
        return problemResponse(detail, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(IncodeApiException.class)
    public ResponseEntity<ProblemDetail> handleIncodeApiException(
            IncodeApiException ex,
            HttpServletRequest request) {
        log.error("Incode upstream API error (status={}): {}", ex.getUpstreamStatusCode(), ex.getMessage());
        ProblemDetail detail = buildProblemDetail(
                HttpStatus.BAD_GATEWAY,
                "urn:incode:problem:upstream-api-error",
                "Upstream Incode API Failure",
                ex.getMessage(),
                request);
        detail.setProperty("upstreamStatusCode", ex.getUpstreamStatusCode());
        return problemResponse(detail, HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(IncodeTimeoutException.class)
    public ResponseEntity<ProblemDetail> handleIncodeTimeoutException(
            IncodeTimeoutException ex,
            HttpServletRequest request) {
        log.error("Incode upstream API timeout/connectivity failure: {}", ex.getMessage(), ex);
        ProblemDetail detail = buildProblemDetail(
                HttpStatus.GATEWAY_TIMEOUT,
                "urn:incode:problem:upstream-timeout",
                "Upstream Incode API Timeout",
                ex.getMessage(),
                request);
        return problemResponse(detail, HttpStatus.GATEWAY_TIMEOUT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request) {
        log.error("Unhandled internal error processing request [{} {}]", request.getMethod(), request.getRequestURI(), ex);
        ProblemDetail detail = buildProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "urn:incode:problem:internal-error",
                "Internal Server Error",
                "An unexpected error occurred while processing the compliance request.",
                request);
        return problemResponse(detail, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ProblemDetail buildProblemDetail(
            HttpStatus status,
            String typeUri,
            String title,
            String detailMessage,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detailMessage);
        problem.setType(URI.create(typeUri));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    private ResponseEntity<ProblemDetail> problemResponse(ProblemDetail detail, HttpStatus status) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(PROBLEM_JSON);
        return new ResponseEntity<>(detail, headers, status);
    }

    private String extractLeafPath(String propertyPath) {
        int lastDot = propertyPath.lastIndexOf('.');
        return lastDot >= 0 && lastDot < propertyPath.length() - 1
                ? propertyPath.substring(lastDot + 1)
                : propertyPath;
    }
}

