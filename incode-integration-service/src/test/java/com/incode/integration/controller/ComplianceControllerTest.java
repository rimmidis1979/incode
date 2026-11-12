package com.incode.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incode.integration.dto.DocumentType;
import com.incode.integration.dto.DocumentVerifyResponse;
import com.incode.integration.dto.SessionCreateRequest;
import com.incode.integration.dto.SessionCreateResponse;
import com.incode.integration.exception.GlobalExceptionHandler;
import com.incode.integration.exception.IncodeApiException;
import com.incode.integration.exception.IncodeTimeoutException;
import com.incode.integration.exception.InvalidDocumentException;
import com.incode.integration.service.IncodeComplianceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ComplianceController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class ComplianceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IncodeComplianceService complianceService;

    @Test
    void createSessionShouldReturn201Created() throws Exception {
        SessionCreateRequest request = new SessionCreateRequest(
                "cust-1001",
                "flow-kyc-standard",
                "US",
                "USA",
                Map.of("channel", "web"));

        SessionCreateResponse response = new SessionCreateResponse(
                "interview-999",
                "flow-kyc-standard",
                "cust-1001",
                "US",
                "INITIATED",
                Instant.parse("2026-11-11T12:00:00Z"));

        given(complianceService.createSession(any(SessionCreateRequest.class), eq("corr-123")))
                .willReturn(response);

        mockMvc.perform(post("/api/v1/compliance/sessions")
                        .header("X-Correlation-Id", "corr-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sessionId").value("interview-999"))
                .andExpect(jsonPath("$.workflowId").value("flow-kyc-standard"))
                .andExpect(jsonPath("$.status").value("INITIATED"));
    }

    @Test
    void createSessionWithBlankFieldsShouldReturnRfc7807BadRequest() throws Exception {
        String invalidJson = """
                {
                  "externalCustomerId": "",
                  "workflowConfigurationId": "",
                  "region": "US!"
                }
                """;

        mockMvc.perform(post("/api/v1/compliance/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Request Validation Failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.externalCustomerId").exists())
                .andExpect(jsonPath("$.errors.workflowConfigurationId").exists());
    }

    @Test
    void createSessionShouldTranslateUpstreamTimeoutTo504ProblemDetail() throws Exception {
        SessionCreateRequest request = new SessionCreateRequest(
                "cust-1002",
                "flow-kyc-standard",
                "US",
                "USA",
                Map.of());

        given(complianceService.createSession(any(SessionCreateRequest.class), any()))
                .willThrow(new IncodeTimeoutException("Read timed out", new SocketTimeoutException("Read timed out")));

        mockMvc.perform(post("/api/v1/compliance/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isGatewayTimeout())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Upstream Incode API Timeout"))
                .andExpect(jsonPath("$.status").value(504));
    }

    @Test
    void createSessionShouldTranslateUpstreamApiErrorTo502ProblemDetail() throws Exception {
        SessionCreateRequest request = new SessionCreateRequest(
                "cust-1003",
                "flow-kyc-standard",
                "EU",
                "DEU",
                Map.of());

        given(complianceService.createSession(any(SessionCreateRequest.class), any()))
                .willThrow(new IncodeApiException("Incode API returned HTTP 401", 401, "{\"error\":\"unauthorized\"}"));

        mockMvc.perform(post("/api/v1/compliance/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.upstreamStatusCode").value(401));
    }

    @Test
    void verifyDocumentShouldAcceptMultipartUploadAndReturn200() throws Exception {
        MockMultipartFile imagePart = new MockMultipartFile(
                "image",
                "id-front.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});

        DocumentVerifyResponse response = new DocumentVerifyResponse(
                "sess-500",
                DocumentType.PASSPORT,
                "id-front.jpg",
                "image/jpeg",
                4L,
                "DOCUMENT_UPLOADED",
                "doc-777",
                Instant.parse("2026-11-11T12:05:00Z"));

        given(complianceService.verifyDocument(eq("sess-500"), any(), eq("PASSPORT"), eq("corr-doc-1")))
                .willReturn(response);

        mockMvc.perform(multipart("/api/v1/compliance/verify")
                        .file(imagePart)
                        .param("sessionId", "sess-500")
                        .param("documentType", "PASSPORT")
                        .header("X-Correlation-Id", "corr-doc-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("sess-500"))
                .andExpect(jsonPath("$.documentType").value("PASSPORT"))
                .andExpect(jsonPath("$.verificationReference").value("doc-777"));
    }

    @Test
    void verifyDocumentShouldRejectInvalidFileWith400ProblemDetail() throws Exception {
        MockMultipartFile badFile = new MockMultipartFile(
                "image",
                "script.txt",
                "text/plain",
                "not-an-image".getBytes());

        given(complianceService.verifyDocument(eq("sess-501"), any(), any(), any()))
                .willThrow(new InvalidDocumentException("Unsupported document format."));

        mockMvc.perform(multipart("/api/v1/compliance/verify")
                        .file(badFile)
                        .param("sessionId", "sess-501"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid Request"))
                .andExpect(jsonPath("$.detail").value("Unsupported document format."));
    }

    @Test
    void verifyDocumentWithBlankSessionIdShouldReturn400ConstraintViolation() throws Exception {
        MockMultipartFile imagePart = new MockMultipartFile(
                "image",
                "id-front.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});

        mockMvc.perform(multipart("/api/v1/compliance/verify")
                        .file(imagePart)
                        .param("sessionId", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Constraint Violation"))
                .andExpect(jsonPath("$.errors.sessionId").exists());
    }
}

