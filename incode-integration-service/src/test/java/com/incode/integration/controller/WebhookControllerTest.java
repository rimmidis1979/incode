package com.incode.integration.controller;

import com.incode.integration.config.IncodeIntegrationProperties;
import com.incode.integration.dto.IncodeWebhookPayload;
import com.incode.integration.dto.WebhookAckResponse;
import com.incode.integration.exception.GlobalExceptionHandler;
import com.incode.integration.security.HmacWebhookSignatureValidator;
import com.incode.integration.service.WebhookProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebhookController.class)
@Import({GlobalExceptionHandler.class, HmacWebhookSignatureValidator.class})
@EnableConfigurationProperties(IncodeIntegrationProperties.class)
@ActiveProfiles("test")
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HmacWebhookSignatureValidator signatureValidator;

    @Autowired
    private IncodeIntegrationProperties properties;

    @MockitoBean
    private WebhookProcessingService webhookProcessingService;

    @Test
    void callbackWithValidSignatureShouldReturnImmediate200Ok() throws Exception {
        String json = """
                {
                  "eventId": "evt-100",
                  "eventType": "ONBOARDING_FINISHED",
                  "interviewId": "sess-900",
                  "status": "success",
                  "externalCustomerId": "cust-900"
                }
                """;
        String signature = signatureValidator.computeHmacSha256Hex(
                json.getBytes(StandardCharsets.UTF_8),
                properties.getWebhook().getSecret());

        WebhookAckResponse ack = new WebhookAckResponse(
                true,
                "evt-100",
                "sess-900",
                "SUCCESS",
                Instant.parse("2026-11-11T12:10:00Z"));
        given(webhookProcessingService.acceptAndDispatch(any(IncodeWebhookPayload.class)))
                .willReturn(ack);

        mockMvc.perform(post("/api/v1/compliance/webhook/callback")
                        .header("X-Incode-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acknowledged").value(true))
                .andExpect(jsonPath("$.eventId").value("evt-100"))
                .andExpect(jsonPath("$.sessionId").value("sess-900"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void callbackWithInvalidSignatureShouldReturn401ProblemDetail() throws Exception {
        String json = "{\"eventType\":\"ONBOARDING_FINISHED\",\"interviewId\":\"sess-901\",\"status\":\"fail\"}";

        mockMvc.perform(post("/api/v1/compliance/webhook/callback")
                        .header("X-Incode-Signature", "invalid-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Webhook Signature Verification Failed"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void callbackWithMalformedJsonAndValidSignatureShouldReturn400ProblemDetail() throws Exception {
        String malformedJson = "{broken-json";
        String signature = signatureValidator.computeHmacSha256Hex(
                malformedJson.getBytes(StandardCharsets.UTF_8),
                properties.getWebhook().getSecret());

        mockMvc.perform(post("/api/v1/compliance/webhook/callback")
                        .header("X-Incode-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid Request"))
                .andExpect(jsonPath("$.detail").value("Malformed JSON in Incode webhook payload."));
    }
}

