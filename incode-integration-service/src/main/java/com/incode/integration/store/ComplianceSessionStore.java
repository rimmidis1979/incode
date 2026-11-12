package com.incode.integration.store;

import com.incode.integration.dto.ComplianceSessionRecord;
import com.incode.integration.dto.VerificationStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe in-memory state store tracking compliance verification sessions
 * across outbound session creation, document uploads, and asynchronous webhook updates.
 */
@Component
public class ComplianceSessionStore {

    private final ConcurrentMap<String, ComplianceSessionRecord> sessions = new ConcurrentHashMap<>();

    public void saveSession(
            String sessionId,
            String workflowId,
            String externalCustomerId,
            String region,
            VerificationStatus status) {
        ComplianceSessionRecord record = new ComplianceSessionRecord(
                sessionId,
                workflowId,
                externalCustomerId,
                region,
                status,
                "SESSION_CREATED",
                Instant.now());
        sessions.put(sessionId, record);
    }

    public void markDocumentUploaded(String sessionId) {
        sessions.compute(sessionId, (id, existing) -> {
            if (existing == null) {
                return new ComplianceSessionRecord(
                        id,
                        null,
                        null,
                        null,
                        VerificationStatus.DOCUMENT_UPLOADED,
                        "DOCUMENT_UPLOADED",
                        Instant.now());
            }
            existing.setStatus(VerificationStatus.DOCUMENT_UPLOADED);
            existing.setLastEventType("DOCUMENT_UPLOADED");
            existing.setUpdatedAt(Instant.now());
            return existing;
        });
    }

    public ComplianceSessionRecord updateFromWebhook(
            String sessionId,
            String externalCustomerId,
            VerificationStatus status,
            String eventType) {
        return sessions.compute(sessionId, (id, existing) -> {
            if (existing == null) {
                return new ComplianceSessionRecord(
                        id,
                        null,
                        externalCustomerId,
                        null,
                        status,
                        eventType,
                        Instant.now());
            }
            if (externalCustomerId != null && !externalCustomerId.isBlank()) {
                existing.setExternalCustomerId(externalCustomerId);
            }
            existing.setStatus(status);
            existing.setLastEventType(eventType);
            existing.setUpdatedAt(Instant.now());
            return existing;
        });
    }

    public Optional<ComplianceSessionRecord> findBySessionId(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }
}

