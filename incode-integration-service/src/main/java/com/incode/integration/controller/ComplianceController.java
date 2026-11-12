package com.incode.integration.controller;

import com.incode.integration.dto.DocumentVerifyResponse;
import com.incode.integration.dto.SessionCreateRequest;
import com.incode.integration.dto.SessionCreateResponse;
import com.incode.integration.service.IncodeComplianceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller exposing compliance session initialization and multipart document verification endpoints.
 */
@Validated
@RestController
@RequestMapping("/api/v1/compliance")
public class ComplianceController {

    private final IncodeComplianceService complianceService;

    public ComplianceController(IncodeComplianceService complianceService) {
        this.complianceService = complianceService;
    }

    /**
     * Initializes an Incode Omni identity verification session/workflow.
     *
     * @param request       customer and workflow metadata
     * @param correlationId optional tracing header
     * @return created session details
     */
    @PostMapping(
            value = "/sessions",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SessionCreateResponse> createSession(
            @Valid @RequestBody SessionCreateRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        SessionCreateResponse response = complianceService.createSession(request, correlationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Accepts a multipart identity document upload (.jpg, .png), validates format and MIME type,
     * and streams it securely to Incode.
     *
     * @param sessionId     active Incode session identifier
     * @param image         binary identity document file
     * @param documentType  optional document type (e.g. ID, PASSPORT, DRIVER_LICENSE)
     * @param correlationId optional tracing header
     * @return verification upload status response
     */
    @PostMapping(
            value = "/verify",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocumentVerifyResponse> verifyDocument(
            @RequestParam("sessionId") @NotBlank(message = "sessionId must not be blank") String sessionId,
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "documentType", required = false) String documentType,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        DocumentVerifyResponse response = complianceService.verifyDocument(
                sessionId,
                image,
                documentType,
                correlationId);
        return ResponseEntity.ok(response);
    }
}

