package com.incode.integration.exception;

/**
 * Thrown when an outbound HTTP call to the Incode Omni API times out or fails at the transport layer.
 */
public class IncodeTimeoutException extends RuntimeException {

    public IncodeTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}

