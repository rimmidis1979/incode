package com.incode.integration.dto;

import java.io.InputStream;

/**
 * Internal command object passed into the Camel multipart document upload route.
 */
public record DocumentUploadCommand(
        String sessionId,
        DocumentType documentType,
        String filename,
        String contentType,
        long fileSizeBytes,
        InputStream contentStream,
        String correlationId) {
}

