package com.incode.integration.security;

import com.incode.integration.exception.InvalidDocumentException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

/**
 * Validates multipart identity document uploads against allowed extensions, MIME types,
 * and binary magic-byte signatures (JPEG and PNG).
 */
@Component
public class DocumentFileValidator {

    private static final Set<String> ALLOWED_JPEG_EXTENSIONS = Set.of("jpg", "jpeg");
    private static final Set<String> ALLOWED_PNG_EXTENSIONS = Set.of("png");
    private static final String MIME_JPEG = "image/jpeg";
    private static final String MIME_PNG = "image/png";
    private static final byte[] PNG_MAGIC = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    /**
     * Validates the uploaded document file and returns normalized metadata.
     *
     * @param file uploaded {@link MultipartFile}
     * @return validated file metadata
     * @throws InvalidDocumentException if the file is missing, empty, or fails format validation
     */
    public ValidatedDocumentMetadata validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidDocumentException("Uploaded image file must not be null or empty.");
        }

        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String extension = extractExtension(originalFilename);
        String contentType = normalizeContentType(file.getContentType());

        validateExtensionAndMime(extension, contentType);
        byte[] headerBytes = readMagicBytes(file);
        validateMagicBytes(headerBytes, contentType);

        return new ValidatedDocumentMetadata(originalFilename, contentType, file.getSize());
    }

    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new InvalidDocumentException("Uploaded image filename must not be blank.");
        }
        String cleanName = originalFilename.replace('\\', '/');
        int lastSlash = cleanName.lastIndexOf('/');
        String baseName = lastSlash >= 0 ? cleanName.substring(lastSlash + 1) : cleanName;
        if (baseName.isBlank()) {
            throw new InvalidDocumentException("Uploaded image filename is invalid.");
        }
        return baseName;
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == filename.length() - 1) {
            throw new InvalidDocumentException(
                    "Uploaded image must include a valid file extension (.jpg, .jpeg, or .png).");
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new InvalidDocumentException("Uploaded image Content-Type header is required (image/jpeg or image/png).");
        }
        int semicolon = contentType.indexOf(';');
        String baseMime = semicolon >= 0 ? contentType.substring(0, semicolon) : contentType;
        return baseMime.trim().toLowerCase(Locale.ROOT);
    }

    private void validateExtensionAndMime(String extension, String contentType) {
        boolean isJpeg = ALLOWED_JPEG_EXTENSIONS.contains(extension) && MIME_JPEG.equals(contentType);
        boolean isPng = ALLOWED_PNG_EXTENSIONS.contains(extension) && MIME_PNG.equals(contentType);
        if (!isJpeg && !isPng) {
            throw new InvalidDocumentException(
                    "Unsupported document format. Accepted formats are JPEG (.jpg/.jpeg, image/jpeg) and PNG (.png, image/png).");
        }
    }

    private byte[] readMagicBytes(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = new byte[8];
            int bytesRead = inputStream.readNBytes(header, 0, header.length);
            if (bytesRead < 3) {
                throw new InvalidDocumentException("Uploaded image file is too small or corrupted.");
            }
            return header;
        } catch (IOException ex) {
            throw new InvalidDocumentException("Failed to read uploaded image content for validation.", ex);
        }
    }

    private void validateMagicBytes(byte[] header, String contentType) {
        if (MIME_JPEG.equals(contentType)) {
            boolean validJpeg = (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8
                    && (header[2] & 0xFF) == 0xFF;
            if (!validJpeg) {
                throw new InvalidDocumentException("File content does not match JPEG image signature.");
            }
            return;
        }

        if (MIME_PNG.equals(contentType)) {
            for (int i = 0; i < PNG_MAGIC.length; i++) {
                if (header[i] != PNG_MAGIC[i]) {
                    throw new InvalidDocumentException("File content does not match PNG image signature.");
                }
            }
        }
    }

    /**
     * Normalized metadata produced after validating an uploaded document.
     */
    public record ValidatedDocumentMetadata(String filename, String contentType, long sizeBytes) {
    }
}

