package com.incode.integration.dto;

import java.util.Locale;

/**
 * Normalized compliance verification lifecycle states.
 */
public enum VerificationStatus {
    INITIATED,
    DOCUMENT_UPLOADED,
    SUCCESS,
    FAIL,
    REVIEW_NEEDED,
    UNKNOWN;

    /**
     * Maps Incode wire status strings (e.g. {@code success}, {@code fail}, {@code review_needed})
     * to a normalized {@link VerificationStatus}.
     *
     * @param wireStatus raw status string from Incode
     * @return mapped status enum
     */
    public static VerificationStatus fromWireValue(String wireStatus) {
        if (wireStatus == null || wireStatus.isBlank()) {
            return UNKNOWN;
        }
        String normalized = wireStatus.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return switch (normalized) {
            case "INITIATED", "CREATED", "PENDING" -> INITIATED;
            case "DOCUMENT_UPLOADED", "UPLOADED" -> DOCUMENT_UPLOADED;
            case "SUCCESS", "APPROVED", "OK", "PASSED", "ONBOARDING_FINISHED" -> SUCCESS;
            case "FAIL", "FAILED", "REJECTED", "DENIED" -> FAIL;
            case "REVIEW_NEEDED", "MANUAL_REVIEW", "NEEDS_REVIEW" -> REVIEW_NEEDED;
            default -> UNKNOWN;
        };
    }
}

