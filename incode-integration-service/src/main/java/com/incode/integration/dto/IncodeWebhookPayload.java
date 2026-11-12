package com.incode.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Inbound webhook event payload delivered asynchronously by Incode.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IncodeWebhookPayload {

    private String eventId;

    @NotBlank(message = "eventType is required in webhook payload")
    private String eventType;

    private String interviewId;
    private String sessionId;

    @NotBlank(message = "status is required in webhook payload")
    private String status;

    private String externalCustomerId;
    private Instant timestamp;
    private Map<String, Object> details = new LinkedHashMap<>();

    public IncodeWebhookPayload() {
    }

    public IncodeWebhookPayload(
            String eventId,
            String eventType,
            String interviewId,
            String sessionId,
            String status,
            String externalCustomerId,
            Instant timestamp,
            Map<String, Object> details) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.interviewId = interviewId;
        this.sessionId = sessionId;
        this.status = status;
        this.externalCustomerId = externalCustomerId;
        this.timestamp = timestamp;
        setDetails(details);
    }

    /**
     * Resolves the canonical session identifier from either {@code interviewId} (Incode native)
     * or {@code sessionId}.
     *
     * @return resolved session identifier or {@code null} if neither is present
     */
    public String resolveSessionId() {
        if (interviewId != null && !interviewId.isBlank()) {
            return interviewId;
        }
        if (sessionId != null && !sessionId.isBlank()) {
            return sessionId;
        }
        return null;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(String interviewId) {
        this.interviewId = interviewId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExternalCustomerId() {
        return externalCustomerId;
    }

    public void setExternalCustomerId(String externalCustomerId) {
        this.externalCustomerId = externalCustomerId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getDetails() {
        return Collections.unmodifiableMap(details);
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details == null ? new LinkedHashMap<>() : new LinkedHashMap<>(details);
    }
}

