package com.incode.integration.security;

import com.incode.integration.config.IncodeIntegrationProperties;
import com.incode.integration.exception.WebhookSignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HmacWebhookSignatureValidatorTest {

    private HmacWebhookSignatureValidator validator;
    private IncodeIntegrationProperties properties;

    @BeforeEach
    void setUp() {
        properties = new IncodeIntegrationProperties();
        properties.setBaseUrl("https://demo-api.incodesmile.com");
        properties.setApiKey("test-key");
        properties.getWebhook().setSecret("super-secret-hmac-key");
        properties.getWebhook().setSignatureHeader("X-Incode-Signature");
        validator = new HmacWebhookSignatureValidator(properties);
    }

    @Test
    void shouldAcceptValidHexSignature() {
        byte[] payload = "{\"eventType\":\"ONBOARDING_FINISHED\",\"interviewId\":\"sess-1\",\"status\":\"success\"}"
                .getBytes(StandardCharsets.UTF_8);
        String signature = validator.computeHmacSha256Hex(payload, "super-secret-hmac-key");

        assertThatCode(() -> validator.verifyOrThrow(payload, signature))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptValidPrefixedAndUppercaseSignature() {
        byte[] payload = "{\"interviewId\":\"sess-2\",\"status\":\"review_needed\"}".getBytes(StandardCharsets.UTF_8);
        String hex = validator.computeHmacSha256Hex(payload, "super-secret-hmac-key");

        assertThatCode(() -> validator.verifyOrThrow(payload, "sha256=" + hex.toUpperCase()))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectMissingSignatureHeader() {
        byte[] payload = "{\"interviewId\":\"sess-3\"}".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> validator.verifyOrThrow(payload, "  "))
                .isInstanceOf(WebhookSignatureException.class)
                .hasMessageContaining("Missing required webhook signature header");
    }

    @Test
    void shouldRejectInvalidSignature() {
        byte[] payload = "{\"interviewId\":\"sess-4\",\"status\":\"fail\"}".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> validator.verifyOrThrow(payload, "deadbeef"))
                .isInstanceOf(WebhookSignatureException.class)
                .hasMessageContaining("Invalid webhook HMAC-SHA256 signature");
    }

    @Test
    void shouldRejectEmptyPayload() {
        assertThatThrownBy(() -> validator.verifyOrThrow(new byte[0], "abc"))
                .isInstanceOf(WebhookSignatureException.class)
                .hasMessageContaining("must not be empty");
    }
}

