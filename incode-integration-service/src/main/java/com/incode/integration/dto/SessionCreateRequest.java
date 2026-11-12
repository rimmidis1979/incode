package com.incode.integration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Inbound API request DTO for initializing an Incode identity verification session.
 */
public class SessionCreateRequest {

    @NotBlank(message = "externalCustomerId is required")
    @Size(min = 2, max = 128, message = "externalCustomerId must be between 2 and 128 characters")
    private String externalCustomerId;

    @NotBlank(message = "workflowConfigurationId is required")
    @Size(min = 2, max = 128, message = "workflowConfigurationId must be between 2 and 128 characters")
    private String workflowConfigurationId;

    @NotBlank(message = "region is required")
    @Pattern(regexp = "^[A-Za-z0-9_-]{2,32}$", message = "region must be 2 to 32 alphanumeric, hyphen, or underscore characters")
    private String region;

    @Size(min = 2, max = 3, message = "countryCode must be a 2- or 3-letter ISO country code")
    private String countryCode;

    private Map<String, String> metadata = new LinkedHashMap<>();

    public SessionCreateRequest() {
    }

    public SessionCreateRequest(
            String externalCustomerId,
            String workflowConfigurationId,
            String region,
            String countryCode,
            Map<String, String> metadata) {
        this.externalCustomerId = externalCustomerId;
        this.workflowConfigurationId = workflowConfigurationId;
        this.region = region;
        this.countryCode = countryCode;
        setMetadata(metadata);
    }

    public String getExternalCustomerId() {
        return externalCustomerId;
    }

    public void setExternalCustomerId(String externalCustomerId) {
        this.externalCustomerId = externalCustomerId;
    }

    public String getWorkflowConfigurationId() {
        return workflowConfigurationId;
    }

    public void setWorkflowConfigurationId(String workflowConfigurationId) {
        this.workflowConfigurationId = workflowConfigurationId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public Map<String, String> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }
}

