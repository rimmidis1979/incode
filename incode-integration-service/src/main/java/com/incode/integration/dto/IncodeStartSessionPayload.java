package com.incode.integration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Outbound request payload sent to Incode Omni {@code /omni/start}.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class IncodeStartSessionPayload {

    private String configurationId;
    private String externalCustomerId;
    private String countryCode;
    private String region;
    private Map<String, String> customFields = new LinkedHashMap<>();

    public IncodeStartSessionPayload() {
    }

    public IncodeStartSessionPayload(
            String configurationId,
            String externalCustomerId,
            String countryCode,
            String region,
            Map<String, String> customFields) {
        this.configurationId = configurationId;
        this.externalCustomerId = externalCustomerId;
        this.countryCode = countryCode;
        this.region = region;
        if (customFields != null) {
            this.customFields = new LinkedHashMap<>(customFields);
        }
    }

    public String getConfigurationId() {
        return configurationId;
    }

    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
    }

    public String getExternalCustomerId() {
        return externalCustomerId;
    }

    public void setExternalCustomerId(String externalCustomerId) {
        this.externalCustomerId = externalCustomerId;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Map<String, String> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, String> customFields) {
        this.customFields = customFields == null ? new LinkedHashMap<>() : new LinkedHashMap<>(customFields);
    }
}

