package com.incode.integration.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Strongly typed externalized configuration properties for the Incode Omni API
 * and webhook security integration, bound to {@code incode.integration}.
 */
@Validated
@ConfigurationProperties(prefix = "incode.integration")
public class IncodeIntegrationProperties {

    @NotBlank(message = "incode.integration.base-url must not be blank")
    private String baseUrl;

    @NotBlank(message = "incode.integration.api-key must not be blank")
    private String apiKey;

    @NotBlank(message = "incode.integration.api-version must not be blank")
    private String apiVersion = "1.0";

    @NotNull(message = "incode.integration.connect-timeout must not be null")
    @Min(value = 100, message = "incode.integration.connect-timeout must be at least 100 ms")
    private Integer connectTimeout = 5000;

    @NotNull(message = "incode.integration.read-timeout must not be null")
    @Min(value = 100, message = "incode.integration.read-timeout must be at least 100 ms")
    private Integer readTimeout = 15000;

    @Valid
    @NotNull
    private WebhookProperties webhook = new WebhookProperties();

    @Valid
    @NotNull
    private TlsProperties tls = new TlsProperties();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Integer getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
    }

    public WebhookProperties getWebhook() {
        return webhook;
    }

    public void setWebhook(WebhookProperties webhook) {
        this.webhook = webhook;
    }

    public TlsProperties getTls() {
        return tls;
    }

    public void setTls(TlsProperties tls) {
        this.tls = tls;
    }

    /**
     * Webhook signature validation properties bound to {@code incode.integration.webhook}.
     */
    public static class WebhookProperties {

        @NotBlank(message = "incode.integration.webhook.secret must not be blank")
        private String secret;

        @NotBlank(message = "incode.integration.webhook.signature-header must not be blank")
        private String signatureHeader = "X-Incode-Signature";

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getSignatureHeader() {
            return signatureHeader;
        }

        public void setSignatureHeader(String signatureHeader) {
            this.signatureHeader = signatureHeader;
        }
    }

    /**
     * Outbound TLS verification settings bound to {@code incode.integration.tls}.
     */
    public static class TlsProperties {

        private boolean verifyHostname = true;

        @NotBlank(message = "incode.integration.tls.protocol must not be blank")
        private String protocol = "TLSv1.3";

        public boolean isVerifyHostname() {
            return verifyHostname;
        }

        public void setVerifyHostname(boolean verifyHostname) {
            this.verifyHostname = verifyHostname;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }
    }
}

