package com.incode.integration.dto;

import com.incode.integration.exception.InvalidDocumentException;

import java.util.Arrays;
import java.util.Locale;

/**
 * Supported identity document types accepted by the compliance verification endpoint.
 */
public enum DocumentType {
    ID,
    PASSPORT,
    DRIVER_LICENSE,
    RESIDENCE_PERMIT;

    /**
     * Resolves an optional document type string to a canonical {@link DocumentType}.
     * Defaults to {@link #ID} when omitted or blank.
     *
     * @param rawValue raw input value from multipart form parameters
     * @return resolved {@link DocumentType}
     * @throws InvalidDocumentException if a non-blank value is not recognized
     */
    public static DocumentType fromNullable(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return ID;
        }
        String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(type -> type.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new InvalidDocumentException(
                        "Unsupported documentType '" + rawValue + "'. Allowed values: ID, PASSPORT, DRIVER_LICENSE, RESIDENCE_PERMIT"));
    }
}

