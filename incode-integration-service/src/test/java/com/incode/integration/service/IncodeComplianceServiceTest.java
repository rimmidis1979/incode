package com.incode.integration.service;

import com.incode.integration.camel.IncodeIntegrationRoute;
import com.incode.integration.dto.DocumentType;
import com.incode.integration.dto.DocumentUploadCommand;
import com.incode.integration.dto.DocumentVerifyResponse;
import com.incode.integration.dto.IncodeDocumentUploadResponse;
import com.incode.integration.dto.IncodeStartSessionResponse;
import com.incode.integration.dto.SessionCreateRequest;
import com.incode.integration.dto.SessionCreateResponse;
import com.incode.integration.dto.VerificationStatus;
import com.incode.integration.exception.IncodeApiException;
import com.incode.integration.exception.IncodeTimeoutException;
import com.incode.integration.security.DocumentFileValidator;
import com.incode.integration.store.ComplianceSessionStore;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.net.SocketTimeoutException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IncodeComplianceServiceTest {

    @Mock
    private ProducerTemplate producerTemplate;

    private ComplianceSessionStore sessionStore;
    private IncodeComplianceService service;

    @BeforeEach
    void setUp() {
        sessionStore = new ComplianceSessionStore();
        service = new IncodeComplianceService(producerTemplate, new DocumentFileValidator(), sessionStore);
    }

    @Test
    void createSessionShouldPersistInitialStateAndReturnDto() {
        SessionCreateRequest request = new SessionCreateRequest(
                "cust-201",
                "wf-kyc-v2",
                "US",
                "USA",
                Map.of("source", "mobile"));
        IncodeStartSessionResponse upstream = new IncodeStartSessionResponse(
                "sess-201",
                "tok-xyz",
                "onboarding",
                "client-1",
                false,
                "INITIATED");

        given(producerTemplate.requestBodyAndHeader(
                eq(IncodeIntegrationRoute.ROUTE_CREATE_SESSION),
                eq(request),
                eq("X-Correlation-Id"),
                eq("corr-201"),
                eq(IncodeStartSessionResponse.class)))
                .willReturn(upstream);

        SessionCreateResponse response = service.createSession(request, "corr-201");

        assertThat(response.getSessionId()).isEqualTo("sess-201");
        assertThat(response.getWorkflowId()).isEqualTo("wf-kyc-v2");
        assertThat(response.getStatus()).isEqualTo("INITIATED");
        assertThat(sessionStore.findBySessionId("sess-201"))
                .isPresent()
                .get()
                .satisfies(record -> {
                    assertThat(record.getExternalCustomerId()).isEqualTo("cust-201");
                    assertThat(record.getStatus()).isEqualTo(VerificationStatus.INITIATED);
                });
    }

    @Test
    void createSessionShouldThrowIncodeApiExceptionWhenInterviewIdMissing() {
        SessionCreateRequest request = new SessionCreateRequest("cust-202", "wf-1", "US", "USA", Map.of());
        given(producerTemplate.requestBodyAndHeader(
                eq(IncodeIntegrationRoute.ROUTE_CREATE_SESSION),
                eq(request),
                eq("X-Correlation-Id"),
                any(),
                eq(IncodeStartSessionResponse.class)))
                .willReturn(new IncodeStartSessionResponse());

        assertThatThrownBy(() -> service.createSession(request, null))
                .isInstanceOf(IncodeApiException.class)
                .hasMessageContaining("without an interviewId");
    }

    @Test
    void createSessionShouldUnwrapCamelExecutionExceptionToIncodeTimeoutException() {
        SessionCreateRequest request = new SessionCreateRequest("cust-203", "wf-1", "US", "USA", Map.of());
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        IncodeTimeoutException timeout = new IncodeTimeoutException("Socket timeout", new SocketTimeoutException("Read timed out"));
        CamelExecutionException wrapped = new CamelExecutionException("Route failed", exchange, timeout);

        given(producerTemplate.requestBodyAndHeader(
                eq(IncodeIntegrationRoute.ROUTE_CREATE_SESSION),
                eq(request),
                eq("X-Correlation-Id"),
                eq("corr-203"),
                eq(IncodeStartSessionResponse.class)))
                .willThrow(wrapped);

        assertThatThrownBy(() -> service.createSession(request, "corr-203"))
                .isSameAs(timeout);
    }

    @Test
    void verifyDocumentShouldValidateImageDispatchCommandAndUpdateStore() {
        byte[] pngBytes = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01, 0x02
        };
        MockMultipartFile image = new MockMultipartFile("image", "id-card.png", "image/png", pngBytes);

        IncodeDocumentUploadResponse upstream = new IncodeDocumentUploadResponse(
                true, "sess-301", "doc-ref-301", "UPLOADED", "ok");

        given(producerTemplate.requestBodyAndHeader(
                eq(IncodeIntegrationRoute.ROUTE_UPLOAD_DOCUMENT),
                any(DocumentUploadCommand.class),
                eq("X-Correlation-Id"),
                eq("corr-301"),
                eq(IncodeDocumentUploadResponse.class)))
                .willReturn(upstream);

        DocumentVerifyResponse response = service.verifyDocument("sess-301", image, "ID", "corr-301");

        ArgumentCaptor<DocumentUploadCommand> captor = ArgumentCaptor.forClass(DocumentUploadCommand.class);
        verify(producerTemplate).requestBodyAndHeader(
                eq(IncodeIntegrationRoute.ROUTE_UPLOAD_DOCUMENT),
                captor.capture(),
                eq("X-Correlation-Id"),
                eq("corr-301"),
                eq(IncodeDocumentUploadResponse.class));

        assertThat(captor.getValue().sessionId()).isEqualTo("sess-301");
        assertThat(captor.getValue().documentType()).isEqualTo(DocumentType.ID);
        assertThat(response.getVerificationReference()).isEqualTo("doc-ref-301");
        assertThat(sessionStore.findBySessionId("sess-301"))
                .isPresent()
                .get()
                .satisfies(record -> assertThat(record.getStatus()).isEqualTo(VerificationStatus.DOCUMENT_UPLOADED));
    }
}

