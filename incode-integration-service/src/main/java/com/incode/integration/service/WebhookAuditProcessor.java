package com.incode.integration.service;

import com.incode.integration.dto.ComplianceSessionRecord;
import com.incode.integration.dto.IncodeWebhookPayload;
import com.incode.integration.dto.VerificationStatus;
import com.incode.integration.store.ComplianceSessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Asynchronous processor invoked by the Apache Camel webhook SEDA route
 * to update compliance session state and emit structured audit records.
 */
@Component
public class WebhookAuditProcessor {

    private static final Logger log = LoggerFactory.getLogger(WebhookAuditProcessor.class);

    private final ComplianceSessionStore sessionStore;

    public WebhookAuditProcessor(ComplianceSessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    /**
     * Updates the internal compliance session record and logs an audit event
     * matching the verification outcome ({@code SUCCESS}, {@code FAIL}, {@code REVIEW_NEEDED}).
     *
     * @param payload validated webhook payload
     */
    public void processAsync(IncodeWebhookPayload payload) {
        String sessionId = payload.resolveSessionId();
        VerificationStatus mappedStatus = VerificationStatus.fromWireValue(payload.getStatus());

        ComplianceSessionRecord updated = sessionStore.updateFromWebhook(
                sessionId,
                payload.getExternalCustomerId(),
                mappedStatus,
                payload.getEventType());

        switch (mappedStatus) {
            case SUCCESS -> log.info(
                    "AUDIT_COMPLIANCE_EVENT outcome=SUCCESS eventId={} sessionId={} externalCustomerId={} eventType={} updatedAt={}",
                    payload.getEventId(),
                    updated.getSessionId(),
                    updated.getExternalCustomerId(),
                    payload.getEventType(),
                    updated.getUpdatedAt());
            case FAIL -> log.warn(
                    "AUDIT_COMPLIANCE_EVENT outcome=FAIL eventId={} sessionId={} externalCustomerId={} eventType={} updatedAt={}",
                    payload.getEventId(),
                    updated.getSessionId(),
                    updated.getExternalCustomerId(),
                    payload.getEventType(),
                    updated.getUpdatedAt());
            case REVIEW_NEEDED -> log.warn(
                    "AUDIT_COMPLIANCE_EVENT outcome=REVIEW_NEEDED eventId={} sessionId={} externalCustomerId={} eventType={} updatedAt={}",
                    payload.getEventId(),
                    updated.getSessionId(),
                    updated.getExternalCustomerId(),
                    payload.getEventType(),
                    updated.getUpdatedAt());
            default -> log.info(
                    "AUDIT_COMPLIANCE_EVENT outcome={} rawStatus={} eventId={} sessionId={} eventType={} updatedAt={}",
                    mappedStatus,
                    payload.getStatus(),
                    payload.getEventId(),
                    updated.getSessionId(),
                    payload.getEventType(),
                    updated.getUpdatedAt());
        }
    }
}

