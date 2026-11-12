package com.incode.integration.security;

import com.incode.integration.exception.InvalidDocumentException;
import com.incode.integration.security.DocumentFileValidator.ValidatedDocumentMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentFileValidatorTest {

    private final DocumentFileValidator validator = new DocumentFileValidator();

    private static final byte[] VALID_JPEG_BYTES = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46
    };

    private static final byte[] VALID_PNG_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00
    };

    @Test
    void shouldValidateGenuineJpegFile() {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "passport-front.jpg",
                "image/jpeg",
                VALID_JPEG_BYTES);

        ValidatedDocumentMetadata metadata = validator.validate(file);

        assertThat(metadata.filename()).isEqualTo("passport-front.jpg");
        assertThat(metadata.contentType()).isEqualTo("image/jpeg");
        assertThat(metadata.sizeBytes()).isEqualTo(VALID_JPEG_BYTES.length);
    }

    @Test
    void shouldValidateGenuinePngFileAndSanitizePath() {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "../../tmp/driver-license.PNG",
                "image/png",
                VALID_PNG_BYTES);

        ValidatedDocumentMetadata metadata = validator.validate(file);

        assertThat(metadata.filename()).isEqualTo("driver-license.PNG");
        assertThat(metadata.contentType()).isEqualTo("image/png");
    }

    @Test
    void shouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "empty.jpg",
                "image/jpeg",
                new byte[0]);

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("must not be null or empty");
    }

    @Test
    void shouldRejectUnsupportedExtensionOrMimeType() {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "document.pdf",
                "application/pdf",
                "%PDF-1.4".getBytes());

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("Unsupported document format");
    }

    @Test
    void shouldRejectSpoofedJpegMagicBytes() {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "fake.jpg",
                "image/jpeg",
                VALID_PNG_BYTES);

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("does not match JPEG image signature");
    }
}

