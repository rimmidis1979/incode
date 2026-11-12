package com.incode.integration.camel;

import com.incode.integration.dto.ComplianceSessionRecord;
import com.incode.integration.dto.DocumentType;
import com.incode.integration.dto.DocumentVerifyResponse;
import com.incode.integration.dto.IncodeWebhookPayload;
import com.incode.integration.dto.SessionCreateRequest;
import com.incode.integration.dto.SessionCreateResponse;
import com.incode.integration.dto.VerificationStatus;
import com.incode.integration.exception.IncodeApiException;
import com.incode.integration.exception.IncodeTimeoutException;
import com.incode.integration.service.IncodeComplianceService;
import com.incode.integration.service.WebhookProcessingService;
import com.incode.integration.store.ComplianceSessionStore;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.apache.hc.core5.http.HttpEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@CamelSpringBootTest
@SpringBootTest
@UseAdviceWith
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class IncodeIntegrationRouteTest {

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private IncodeComplianceService complianceService;

    @Autowired
    private WebhookProcessingService webhookProcessingService;

    @Autowired
    private ComplianceSessionStore sessionStore;

    @Test
    void createSessionRouteShouldMarshalRequestPropagateHeadersAndUnmarshalResponse() throws Exception {
        AdviceWith.adviceWith(camelContext, "incode-create-session-route", route ->
                route.weaveByToUri("https://sandbox-test.incodesmile.com/omni/start*")
                        .replace()
                        .to("mock:incodeStart"));
        camelContext.start();

        MockEndpoint mockStart = camelContext.getEndpoint("mock:incodeStart", MockEndpoint.class);
        mockStart.expectedMessageCount(1);
        mockStart.expectedHeaderReceived("x-api-key", "test-api-key-12345");
        mockStart.expectedHeaderReceived("api-version", "1.0");
        mockStart.whenAnyExchangeReceived(exchange -> {
            String requestJson = exchange.getIn().getBody(String.class);
            assertThat(requestJson).contains("\"configurationId\":\"wf-kyc-01\"");
            assertThat(requestJson).contains("\"externalCustomerId\":\"cust-42\"");
            exchange.getIn().setBody("{\"interviewId\":\"incode-sess-42\",\"status\":\"INITIATED\"}");
        });

        SessionCreateResponse response = complianceService.createSession(
                new SessionCreateRequest("cust-42", "wf-kyc-01", "US", "USA", Map.of("tier", "gold")),
                "corr-test-1");

        mockStart.assertIsSatisfied();
        assertThat(response.getSessionId()).isEqualTo("incode-sess-42");
        assertThat(response.getWorkflowId()).isEqualTo("wf-kyc-01");
        assertThat(response.getStatus()).isEqualTo("INITIATED");
        assertThat(sessionStore.findBySessionId("incode-sess-42"))
                .isPresent()
                .get()
                .extracting(ComplianceSessionRecord::getStatus)
                .isEqualTo(VerificationStatus.INITIATED);
    }

    @Test
    void uploadDocumentRouteShouldStreamMultipartEntityAndPropagateHeaders() throws Exception {
        AdviceWith.adviceWith(camelContext, "incode-upload-document-route", route ->
                route.weaveByToUri("https://sandbox-test.incodesmile.com/omni/add/document*")
                        .replace()
                        .to("mock:incodeUpload"));
        camelContext.start();

        MockEndpoint mockUpload = camelContext.getEndpoint("mock:incodeUpload", MockEndpoint.class);
        mockUpload.expectedMessageCount(1);
        mockUpload.expectedHeaderReceived("x-api-key", "test-api-key-12345");
        mockUpload.expectedHeaderReceived("X-Incode-Session-Id", "incode-sess-55");
        mockUpload.expectedHeaderReceived("X-Document-Type", "DRIVER_LICENSE");
        mockUpload.whenAnyExchangeReceived(exchange -> {
            HttpEntity entity = exchange.getIn().getBody(HttpEntity.class);
            assertThat(entity).isNotNull();
            assertThat(entity.getContentType()).contains("multipart/form-data");
            exchange.getIn().setBody(
                    "{\"success\":true,\"interviewId\":\"incode-sess-55\",\"documentId\":\"doc-55\",\"status\":\"UPLOADED\"}");
        });

        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
        MockMultipartFile image = new MockMultipartFile("image", "dl.jpg", "image/jpeg", jpegBytes);

        DocumentVerifyResponse response = complianceService.verifyDocument(
                "incode-sess-55",
                image,
                "DRIVER_LICENSE",
                "corr-upload-1");

        mockUpload.assertIsSatisfied();
        assertThat(response.getSessionId()).isEqualTo("incode-sess-55");
        assertThat(response.getDocumentType()).isEqualTo(DocumentType.DRIVER_LICENSE);
        assertThat(response.getVerificationReference()).isEqualTo("doc-55");
    }

    @Test
    void upstreamHttpErrorShouldRouteToDeadLetterAndThrowIncodeApiException() throws Exception {
        AdviceWith.adviceWith(camelContext, "incode-create-session-route", route ->
                route.weaveByToUri("https://sandbox-test.incodesmile.com/omni/start*")
                        .replace()
                        .to("mock:incodeError"));
        AdviceWith.adviceWith(camelContext, "incode-dead-letter-route", route ->
                route.weaveAddLast().to("mock:deadLetter"));
        camelContext.start();

        MockEndpoint mockError = camelContext.getEndpoint("mock:incodeError", MockEndpoint.class);
        MockEndpoint mockDeadLetter = camelContext.getEndpoint("mock:deadLetter", MockEndpoint.class);
        mockDeadLetter.expectedMessageCount(1);

        mockError.whenAnyExchangeReceived(exchange -> {
            throw new HttpOperationFailedException(
                    "https://sandbox-test.incodesmile.com/omni/start",
                    503,
                    "Service Unavailable",
                    null,
                    Map.of(),
                    "{\"error\":\"upstream_unavailable\"}");
        });

        SessionCreateRequest request = new SessionCreateRequest("cust-err", "wf-1", "US", "USA", Map.of());

        assertThatThrownBy(() -> complianceService.createSession(request, "corr-err-1"))
                .isInstanceOf(IncodeApiException.class)
                .satisfies(ex -> {
                    IncodeApiException apiEx = (IncodeApiException) ex;
                    assertThat(apiEx.getUpstreamStatusCode()).isEqualTo(503);
                    assertThat(apiEx.getMessage()).contains("503");
                });

        mockDeadLetter.assertIsSatisfied();
    }

    @Test
    void upstreamTimeoutShouldRetryRouteToDeadLetterAndThrowIncodeTimeoutException() throws Exception {
        AdviceWith.adviceWith(camelContext, "incode-create-session-route", route ->
                route.weaveByToUri("https://sandbox-test.incodesmile.com/omni/start*")
                        .replace()
                        .to("mock:incodeTimeout"));
        AdviceWith.adviceWith(camelContext, "incode-dead-letter-route", route ->
                route.weaveAddLast().to("mock:deadLetterTimeout"));
        camelContext.start();

        MockEndpoint mockTimeout = camelContext.getEndpoint("mock:incodeTimeout", MockEndpoint.class);
        MockEndpoint mockDeadLetter = camelContext.getEndpoint("mock:deadLetterTimeout", MockEndpoint.class);
        // 1 initial attempt + 2 redeliveries = 3 calls
        mockTimeout.expectedMessageCount(3);
        mockDeadLetter.expectedMessageCount(1);

        mockTimeout.whenAnyExchangeReceived(exchange -> {
            throw new SocketTimeoutException("Connect timed out");
        });

        SessionCreateRequest request = new SessionCreateRequest("cust-to", "wf-1", "US", "USA", Map.of());

        assertThatThrownBy(() -> complianceService.createSession(request, "corr-to-1"))
                .isInstanceOf(IncodeTimeoutException.class)
                .hasMessageContaining("Connect timed out");

        mockTimeout.assertIsSatisfied();
        mockDeadLetter.assertIsSatisfied();
    }

    @Test
    void webhookIngestRouteShouldProcessStatusAsynchronouslyViaSeda() throws Exception {
        camelContext.start();
        sessionStore.saveSession("sess-async-1", "wf-1", "cust-async", "US", VerificationStatus.INITIATED);

        IncodeWebhookPayload payload = new IncodeWebhookPayload(
                "evt-async-1",
                "MANUAL_REVIEW_TRIGGERED",
                "sess-async-1",
                null,
                "review_needed",
                "cust-async",
                Instant.now(),
                Map.of("reason", "glare_detected"));

        webhookProcessingService.acceptAndDispatch(payload);

        long deadlineNanos = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        ComplianceSessionRecord record = null;
        while (System.nanoTime() < deadlineNanos) {
            record = sessionStore.findBySessionId("sess-async-1").orElse(null);
            if (record != null && record.getStatus() == VerificationStatus.REVIEW_NEEDED) {
                break;
            }
            Thread.sleep(25);
        }

        assertThat(record).isNotNull();
        assertThat(record.getStatus()).isEqualTo(VerificationStatus.REVIEW_NEEDED);
        assertThat(record.getLastEventType()).isEqualTo("MANUAL_REVIEW_TRIGGERED");
    }
}

