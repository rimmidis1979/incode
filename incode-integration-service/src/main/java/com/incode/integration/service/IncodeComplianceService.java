package com.incode.integration.service;

import com.incode.integration.camel.IncodeIntegrationRoute;
import com.incode.integration.dto.DocumentType;
import com.incode.integration.dto.DocumentUploadCommand;
import com.incode.integration.dto.DocumentVerifyResponse;
import com.incode.integration.dto.IncodeDocumentUploadResponse;
import com.incode.integration.dto.IncodeStartSessionResponse;
import com.incode.integration.dto.SessionCreateRequest;
import com.incode.integration.dto.SessionCreateResponse;
import com.incode.integration.dto.VerificationStatus;
import com.incode.integration.exception.IncodeApiException;
import com.incode.integration.exception.IncodeTimeoutException;
import com.incode.integration.exception.InvalidDocumentException;
import com.incode.integration.security.DocumentFileValidator;
import com.incode.integration.security.DocumentFileValidator.ValidatedDocumentMetadata;
import com.incode.integration.store.ComplianceSessionStore;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

/**
 * Enterprise service orchestrating Incode Omni session creation and document verification
 * over Apache Camel integration routes.
 */
@Service
public class IncodeComplianceService {

    private static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

    private final ProducerTemplate producerTemplate;
    private final DocumentFileValidator documentFileValidator;
    private final ComplianceSessionStore sessionStore;

    public IncodeComplianceService(
            ProducerTemplate producerTemplate,
            DocumentFileValidator documentFileValidator,
            ComplianceSessionStore sessionStore) {
        this.producerTemplate = producerTemplate;
        this.documentFileValidator = documentFileValidator;
        this.sessionStore = sessionStore;
    }

    /**
     * Initiates a new Incode verification session/workflow and records its initial state.
     *
     * @param request       validated session creation request
     * @param correlationId optional request correlation identifier
     * @return created session summary DTO
     */
    public SessionCreateResponse createSession(SessionCreateRequest request, String correlationId) {
        String effectiveCorrelationId = resolveCorrelationId(correlationId);
        IncodeStartSessionResponse upstream = executeCamelRoute(
                IncodeIntegrationRoute.ROUTE_CREATE_SESSION,
                request,
                effectiveCorrelationId,
                IncodeStartSessionResponse.class);

        if (upstream == null || upstream.getInterviewId() == null || upstream.getInterviewId().isBlank()) {
            throw new IncodeApiException(
                    "Incode Omni API returned an empty session response without an interviewId.",
                    502,
                    null);
        }

        String status = (upstream.getStatus() != null && !upstream.getStatus().isBlank())
                ? upstream.getStatus()
                : VerificationStatus.INITIATED.name();

        sessionStore.saveSession(
                upstream.getInterviewId(),
                request.getWorkflowConfigurationId(),
                request.getExternalCustomerId(),
                request.getRegion(),
                VerificationStatus.INITIATED);

        return new SessionCreateResponse(
                upstream.getInterviewId(),
                request.getWorkflowConfigurationId(),
                request.getExternalCustomerId(),
                request.getRegion(),
                status,
                Instant.now());
    }

    /**
     * Validates an uploaded identity document and streams it securely to Incode via Camel.
     *
     * @param sessionId       active Incode session identifier
     * @param image           multipart identity document image (.jpg, .jpeg, .png)
     * @param rawDocumentType optional document type string
     * @param correlationId   optional request correlation identifier
     * @return document upload verification response DTO
     */
    public DocumentVerifyResponse verifyDocument(
            String sessionId,
            MultipartFile image,
            String rawDocumentType,
            String correlationId) {
        DocumentType documentType = DocumentType.fromNullable(rawDocumentType);
        ValidatedDocumentMetadata metadata = documentFileValidator.validate(image);
        String effectiveCorrelationId = resolveCorrelationId(correlationId);

        IncodeDocumentUploadResponse upstream;
        try (InputStream contentStream = image.getInputStream()) {
            DocumentUploadCommand command = new DocumentUploadCommand(
                    sessionId.trim(),
                    documentType,
                    metadata.filename(),
                    metadata.contentType(),
                    metadata.sizeBytes(),
                    contentStream,
                    effectiveCorrelationId);

            upstream = executeCamelRoute(
                    IncodeIntegrationRoute.ROUTE_UPLOAD_DOCUMENT,
                    command,
                    effectiveCorrelationId,
                    IncodeDocumentUploadResponse.class);
        } catch (IOException ex) {
            throw new InvalidDocumentException("Unable to stream uploaded document bytes.", ex);
        }

        sessionStore.markDocumentUploaded(sessionId.trim());

        String status = (upstream != null && upstream.getStatus() != null && !upstream.getStatus().isBlank())
                ? upstream.getStatus()
                : VerificationStatus.DOCUMENT_UPLOADED.name();
        String verificationReference = (upstream != null && upstream.getDocumentId() != null && !upstream.getDocumentId().isBlank())
                ? upstream.getDocumentId()
                : effectiveCorrelationId;

        return new DocumentVerifyResponse(
                sessionId.trim(),
                documentType,
                metadata.filename(),
                metadata.contentType(),
                metadata.sizeBytes(),
                status,
                verificationReference,
                Instant.now());
    }

    private <T> T executeCamelRoute(
            String endpointUri,
            Object body,
            String correlationId,
            Class<T> responseType) {
        try {
            return producerTemplate.requestBodyAndHeader(
                    endpointUri,
                    body,
                    HEADER_CORRELATION_ID,
                    correlationId,
                    responseType);
        } catch (CamelExecutionException ex) {
            rethrowTranslatedException(ex);
            throw ex;
        }
    }

    private void rethrowTranslatedException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof IncodeApiException apiException) {
                throw apiException;
            }
            if (current instanceof IncodeTimeoutException timeoutException) {
                throw timeoutException;
            }
            current = current.getCause();
        }
    }

    private String resolveCorrelationId(String correlationId) {
        return (correlationId != null && !correlationId.isBlank())
                ? correlationId.trim()
                : UUID.randomUUID().toString();
    }
}

