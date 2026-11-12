package com.incode.integration.exception;

/**
 * Thrown when the upstream Incode Omni API returns an unsuccessful HTTP status or invalid response.
 */
public class IncodeApiException extends RuntimeException {

    private final int upstreamStatusCode;
    private final String upstreamBody;

    public IncodeApiException(String message, int upstreamStatusCode, String upstreamBody) {
        super(message);
        this.upstreamStatusCode = upstreamStatusCode;
        this.upstreamBody = upstreamBody;
    }

    public IncodeApiException(String message, int upstreamStatusCode, String upstreamBody, Throwable cause) {
        super(message, cause);
        this.upstreamStatusCode = upstreamStatusCode;
        this.upstreamBody = upstreamBody;
    }

    public int getUpstreamStatusCode() {
        return upstreamStatusCode;
    }

    public String getUpstreamBody() {
        return upstreamBody;
    }
}

