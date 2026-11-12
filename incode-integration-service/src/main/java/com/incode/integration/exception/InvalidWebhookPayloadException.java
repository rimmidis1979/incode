package com.incode.integration.exception;

/**
 * Thrown when an incoming webhook JSON body is malformed or fails semantic validation.
 */
public class InvalidWebhookPayloadException extends RuntimeException {

    public InvalidWebhookPayloadException(String message) {
        super(message);
    }

    public InvalidWebhookPayloadException(String message, Throwable cause) {
        super(message, cause);
    }
}

