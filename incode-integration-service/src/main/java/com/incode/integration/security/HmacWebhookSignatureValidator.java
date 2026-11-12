package com.incode.integration.security;

import com.incode.integration.config.IncodeIntegrationProperties;
import com.incode.integration.exception.WebhookSignatureException;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Validates incoming Incode webhook payloads using HMAC-SHA256 signatures
 * and constant-time digest comparison to prevent spoofing and timing attacks.
 */
@Component
public class HmacWebhookSignatureValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SHA256_PREFIX = "sha256=";

    private final IncodeIntegrationProperties properties;

    public HmacWebhookSignatureValidator(IncodeIntegrationProperties properties) {
        this.properties = properties;
    }

    /**
     * Verifies the signature header against the raw webhook request body bytes.
     *
     * @param rawPayload      raw HTTP request body bytes prior to JSON deserialization
     * @param signatureHeader value of the configured signature header (e.g. {@code X-Incode-Signature})
     * @throws WebhookSignatureException if the signature is missing, malformed, or does not match
     */
    public void verifyOrThrow(byte[] rawPayload, String signatureHeader) {
        if (rawPayload == null || rawPayload.length == 0) {
            throw new WebhookSignatureException("Webhook request body must not be empty.");
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new WebhookSignatureException(
                    "Missing required webhook signature header '" + properties.getWebhook().getSignatureHeader() + "'.");
        }

        String normalizedProvided = normalizeSignature(signatureHeader);
        String expectedHex = computeHmacSha256Hex(rawPayload, properties.getWebhook().getSecret());

        byte[] providedBytes = normalizedProvided.getBytes(StandardCharsets.UTF_8);
        byte[] expectedBytes = expectedHex.getBytes(StandardCharsets.UTF_8);

        if (!MessageDigest.isEqual(providedBytes, expectedBytes)) {
            throw new WebhookSignatureException("Invalid webhook HMAC-SHA256 signature.");
        }
    }

    /**
     * Computes a lowercase hexadecimal HMAC-SHA256 signature for the given payload and secret.
     *
     * @param payload raw payload bytes
     * @param secret  shared HMAC secret
     * @return lowercase hex-encoded HMAC-SHA256 digest
     */
    public String computeHmacSha256Hex(byte[] payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);
            byte[] digest = mac.doFinal(payload);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256 webhook signature.", ex);
        }
    }

    private String normalizeSignature(String rawSignature) {
        String trimmed = rawSignature.trim().toLowerCase(Locale.ROOT);
        if (trimmed.startsWith(SHA256_PREFIX)) {
            return trimmed.substring(SHA256_PREFIX.length());
        }
        return trimmed;
    }
}

