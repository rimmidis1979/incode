package com.incode.integration.dto;

import java.time.Instant;

/**
 * Internal state snapshot tracking a compliance verification session across API calls and webhooks.
 */
public class ComplianceSessionRecord {

    private String sessionId;
    private String workflowId;
    private String externalCustomerId;
    private String region;
    private VerificationStatus status;
    private String lastEventType;
    private Instant updatedAt;

    public ComplianceSessionRecord() {
    }

    public ComplianceSessionRecord(
            String sessionId,
            String workflowId,
            String externalCustomerId,
            String region,
            VerificationStatus status,
            String lastEventType,
            Instant updatedAt) {
        this.sessionId = sessionId;
        this.workflowId = workflowId;
        this.externalCustomerId = externalCustomerId;
        this.region = region;
        this.status = status;
        this.lastEventType = lastEventType;
        this.updatedAt = updatedAt;
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

    public VerificationStatus getStatus() {
        return status;
    }

    public void setStatus(VerificationStatus status) {
        this.status = status;
    }

    public String getLastEventType() {
        return lastEventType;
    }

    public void setLastEventType(String lastEventType) {
        this.lastEventType = lastEventType;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

