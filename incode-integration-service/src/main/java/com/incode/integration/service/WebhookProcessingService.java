package com.incode.integration.service;

import com.incode.integration.dto.IncodeWebhookPayload;
import com.incode.integration.dto.VerificationStatus;
import com.incode.integration.dto.WebhookAckResponse;
import com.incode.integration.exception.InvalidWebhookPayloadException;
import org.apache.camel.ProducerTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Service coordinating validation, immediate acknowledgement, and asynchronous
 * Apache Camel dispatch of Incode webhook status updates.
 */
@Service
public class WebhookProcessingService {

    public static final String ROUTE_INGEST_WEBHOOK = "direct:ingestWebhookEvent";

    private final ProducerTemplate producerTemplate;

    public WebhookProcessingService(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }

    /**
     * Validates semantic webhook fields, dispatches the event asynchronously onto the
     * Apache Camel processing pipeline, and returns an immediate acknowledgement.
     *
     * @param payload parsed webhook event payload
     * @return immediate acknowledgement response
     */
    public WebhookAckResponse acceptAndDispatch(IncodeWebhookPayload payload) {
        if (payload == null) {
            throw new InvalidWebhookPayloadException("Webhook payload must not be null.");
        }
        String sessionId = payload.resolveSessionId();
        if (sessionId == null) {
            throw new InvalidWebhookPayloadException(
                    "Webhook payload must include either 'interviewId' or 'sessionId'.");
        }
        if (payload.getEventType() == null || payload.getEventType().isBlank()) {
            throw new InvalidWebhookPayloadException("Webhook payload must include 'eventType'.");
        }
        if (payload.getStatus() == null || payload.getStatus().isBlank()) {
            throw new InvalidWebhookPayloadException("Webhook payload must include 'status'.");
        }

        String eventId = (payload.getEventId() != null && !payload.getEventId().isBlank())
                ? payload.getEventId()
                : UUID.randomUUID().toString();
        payload.setEventId(eventId);

        producerTemplate.sendBodyAndHeader(ROUTE_INGEST_WEBHOOK, payload, "X-Event-Id", eventId);

        return new WebhookAckResponse(
                true,
                eventId,
                sessionId,
                VerificationStatus.fromWireValue(payload.getStatus()).name(),
                Instant.now());
    }
}
