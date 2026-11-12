package com.incode.integration.dto;

import java.time.Instant;

/**
 * Outbound API response DTO returned after initializing an Incode verification session.
 */
public class SessionCreateResponse {

    private String sessionId;
    private String workflowId;
    private String externalCustomerId;
    private String region;
    private String status;
    private Instant createdAt;

    public SessionCreateResponse() {
    }

    public SessionCreateResponse(
            String sessionId,
            String workflowId,
            String externalCustomerId,
            String region,
            String status,
            Instant createdAt) {
        this.sessionId = sessionId;
        this.workflowId = workflowId;
        this.externalCustomerId = externalCustomerId;
        this.region = region;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getExternalCustomerId() {
        return externalCustomerId;
    }

    public void setExternalCustomerId(String externalCustomerId) {
        this.externalCustomerId = externalCustomerId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

