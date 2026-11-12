package com.incode.integration.dto;

import java.time.Instant;

/**
 * API response DTO returned after streaming an identity document to Incode for verification.
 */
public class DocumentVerifyResponse {

    private String sessionId;
    private DocumentType documentType;
    private String fileName;
    private String contentType;
    private long fileSizeBytes;
    private String status;
    private String verificationReference;
    private Instant processedAt;

    public DocumentVerifyResponse() {
    }

    public DocumentVerifyResponse(
            String sessionId,
            DocumentType documentType,
            String fileName,
            String contentType,
            long fileSizeBytes,
            String status,
            String verificationReference,
            Instant processedAt) {
        this.sessionId = sessionId;
        this.documentType = documentType;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.status = status;
        this.verificationReference = verificationReference;
        this.processedAt = processedAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVerificationReference() {
        return verificationReference;
    }

    public void setVerificationReference(String verificationReference) {
        this.verificationReference = verificationReference;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}

