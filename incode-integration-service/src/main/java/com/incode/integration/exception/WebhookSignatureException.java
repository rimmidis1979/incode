package com.incode.integration.exception;

/**
 * Thrown when an incoming Incode webhook request fails HMAC-SHA256 signature verification.
 */
public class WebhookSignatureException extends RuntimeException {

    public WebhookSignatureException(String message) {
        super(message);
    }
}

