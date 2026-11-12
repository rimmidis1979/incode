package com.incode.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Upstream response DTO returned by Incode document upload endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IncodeDocumentUploadResponse {

    private boolean success;
    private String interviewId;
    private String documentId;
    private String status;
    private String message;

    public IncodeDocumentUploadResponse() {
    }

    public IncodeDocumentUploadResponse(
            boolean success,
            String interviewId,
            String documentId,
            String status,
            String message) {
        this.success = success;
        this.interviewId = interviewId;
        this.documentId = documentId;
        this.status = status;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(String interviewId) {
        this.interviewId = interviewId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

