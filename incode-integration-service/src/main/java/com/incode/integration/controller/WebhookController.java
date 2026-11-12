package com.incode.integration.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incode.integration.config.IncodeIntegrationProperties;
import com.incode.integration.dto.IncodeWebhookPayload;
import com.incode.integration.dto.WebhookAckResponse;
import com.incode.integration.exception.InvalidWebhookPayloadException;
import com.incode.integration.security.HmacWebhookSignatureValidator;
import com.incode.integration.service.WebhookProcessingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Secure webhook listener endpoint receiving asynchronous verification status updates from Incode.
 */
@RestController
@RequestMapping("/api/v1/compliance/webhook")
public class WebhookController {

    private final IncodeIntegrationProperties properties;
    private final HmacWebhookSignatureValidator signatureValidator;
    private final WebhookProcessingService webhookProcessingService;
    private final ObjectMapper objectMapper;

    public WebhookController(
            IncodeIntegrationProperties properties,
            HmacWebhookSignatureValidator signatureValidator,
            WebhookProcessingService webhookProcessingService,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.signatureValidator = signatureValidator;
        this.webhookProcessingService = webhookProcessingService;
        this.objectMapper = objectMapper;
    }

    /**
     * Receives Incode webhook callbacks, verifies the raw body HMAC-SHA256 signature,
     * dispatches asynchronous processing, and acknowledges immediately with HTTP 200 OK.
     *
     * @param rawPayload raw HTTP request body bytes
     * @param request    servlet request used to read the configured signature header
     * @return immediate {@code 200 OK} acknowledgement
     */
    @PostMapping(
            value = "/callback",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebhookAckResponse> handleCallback(
            @RequestBody(required = false) byte[] rawPayload,
            HttpServletRequest request) {
        String signatureHeaderName = properties.getWebhook().getSignatureHeader();
        String signatureHeader = request.getHeader(signatureHeaderName);

        signatureValidator.verifyOrThrow(rawPayload, signatureHeader);

        IncodeWebhookPayload payload = parsePayload(rawPayload);
        WebhookAckResponse ack = webhookProcessingService.acceptAndDispatch(payload);
        return ResponseEntity.ok(ack);
    }

    private IncodeWebhookPayload parsePayload(byte[] rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, IncodeWebhookPayload.class);
        } catch (JsonProcessingException ex) {
            throw new InvalidWebhookPayloadException("Malformed JSON in Incode webhook payload.", ex);
        } catch (IOException ex) {
            throw new InvalidWebhookPayloadException("Unable to read Incode webhook payload.", ex);
        }
    }
}

