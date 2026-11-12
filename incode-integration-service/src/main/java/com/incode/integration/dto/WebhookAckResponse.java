package com.incode.integration.dto;

import java.time.Instant;

/**
 * Immediate HTTP 200 acknowledgement DTO returned to Incode webhook callers.
 */
public class WebhookAckResponse {

    private boolean acknowledged;
    private String eventId;
    private String sessionId;
    private String status;
    private Instant receivedAt;

    public WebhookAckResponse() {
    }

    public WebhookAckResponse(
            boolean acknowledged,
            String eventId,
            String sessionId,
            String status,
            Instant receivedAt) {
        this.acknowledged = acknowledged;
        this.eventId = eventId;
        this.sessionId = sessionId;
        this.status = status;
        this.receivedAt = receivedAt;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
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

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }
}

