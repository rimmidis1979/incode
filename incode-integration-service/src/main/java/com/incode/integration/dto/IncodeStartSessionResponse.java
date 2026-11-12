package com.incode.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Upstream response model returned by Incode Omni {@code /omni/start}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IncodeStartSessionResponse {

    private String interviewId;
    private String token;
    private String flowType;
    private String clientId;
    private Boolean existingSession;
    private String status;

    public IncodeStartSessionResponse() {
    }

    public IncodeStartSessionResponse(
            String interviewId,
            String token,
            String flowType,
            String clientId,
            Boolean existingSession,
            String status) {
        this.interviewId = interviewId;
        this.token = token;
        this.flowType = flowType;
        this.clientId = clientId;
        this.existingSession = existingSession;
        this.status = status;
    }

    public String getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(String interviewId) {
        this.interviewId = interviewId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getFlowType() {
        return flowType;
    }

    public void setFlowType(String flowType) {
        this.flowType = flowType;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Boolean getExistingSession() {
        return existingSession;
    }

    public void setExistingSession(Boolean existingSession) {
        this.existingSession = existingSession;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

