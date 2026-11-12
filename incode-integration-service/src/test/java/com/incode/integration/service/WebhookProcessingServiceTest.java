package com.incode.integration.service;

import com.incode.integration.dto.IncodeWebhookPayload;
import com.incode.integration.dto.VerificationStatus;
import com.incode.integration.dto.WebhookAckResponse;
import com.incode.integration.exception.InvalidWebhookPayloadException;
import com.incode.integration.store.ComplianceSessionStore;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebhookProcessingServiceTest {

    @Mock
    private ProducerTemplate producerTemplate;

    private WebhookProcessingService processingService;
    private ComplianceSessionStore sessionStore;
    private WebhookAuditProcessor auditProcessor;

    @BeforeEach
    void setUp() {
        processingService = new WebhookProcessingService(producerTemplate);
        sessionStore = new ComplianceSessionStore();
        auditProcessor = new WebhookAuditProcessor(sessionStore);
    }

    @Test
    void acceptAndDispatchShouldGenerateEventIdWhenAbsentAndForwardToCamel() {
        IncodeWebhookPayload payload = new IncodeWebhookPayload(
                null,
                "ONBOARDING_FINISHED",
                null,
                "sess-fallback-1",
                "success",
                "cust-11",
                Instant.now(),
                Map.of());

        WebhookAckResponse ack = processingService.acceptAndDispatch(payload);

        assertThat(ack.isAcknowledged()).isTrue();
        assertThat(ack.getEventId()).isNotBlank();
        assertThat(ack.getSessionId()).isEqualTo("sess-fallback-1");
        assertThat(ack.getStatus()).isEqualTo("SUCCESS");
        verify(producerTemplate).sendBodyAndHeader(
                eq(WebhookProcessingService.ROUTE_INGEST_WEBHOOK),
                eq(payload),
                eq("X-Event-Id"),
                eq(ack.getEventId()));
    }

    @Test
    void acceptAndDispatchShouldRejectPayloadWithoutSessionOrInterviewId() {
        IncodeWebhookPayload payload = new IncodeWebhookPayload(
                "evt-1",
                "ONBOARDING_FINISHED",
                null,
                null,
                "fail",
                "cust-12",
                Instant.now(),
                Map.of());

        assertThatThrownBy(() -> processingService.acceptAndDispatch(payload))
                .isInstanceOf(InvalidWebhookPayloadException.class)
                .hasMessageContaining("interviewId");
    }

    @Test
    void auditProcessorShouldTransitionSessionStatesCleanly() {
        sessionStore.saveSession("sess-700", "wf-700", "cust-700", "EU", VerificationStatus.INITIATED);

        auditProcessor.processAsync(new IncodeWebhookPayload(
                "evt-701", "MANUAL_REVIEW", "sess-700", null, "review_needed", "cust-700", Instant.now(), Map.of()));
        assertThat(sessionStore.findBySessionId("sess-700"))
                .get()
                .satisfies(r -> assertThat(r.getStatus()).isEqualTo(VerificationStatus.REVIEW_NEEDED));

        auditProcessor.processAsync(new IncodeWebhookPayload(
                "evt-702", "ONBOARDING_FINISHED", "sess-700", null, "fail", "cust-700", Instant.now(), Map.of()));
        assertThat(sessionStore.findBySessionId("sess-700"))
                .get()
                .satisfies(r -> assertThat(r.getStatus()).isEqualTo(VerificationStatus.FAIL));

        auditProcessor.processAsync(new IncodeWebhookPayload(
                "evt-703", "ONBOARDING_FINISHED", "sess-700", null, "success", "cust-700", Instant.now(), Map.of()));
        assertThat(sessionStore.findBySessionId("sess-700"))
                .get()
                .satisfies(r -> assertThat(r.getStatus()).isEqualTo(VerificationStatus.SUCCESS));
    }
}

