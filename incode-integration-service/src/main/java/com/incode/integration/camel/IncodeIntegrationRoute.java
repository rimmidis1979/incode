package com.incode.integration.camel;

import com.incode.integration.config.IncodeIntegrationProperties;
import com.incode.integration.dto.DocumentUploadCommand;
import com.incode.integration.dto.IncodeDocumentUploadResponse;
import com.incode.integration.dto.IncodeStartSessionPayload;
import com.incode.integration.dto.IncodeStartSessionResponse;
import com.incode.integration.dto.IncodeWebhookPayload;
import com.incode.integration.dto.SessionCreateRequest;
import com.incode.integration.exception.IncodeApiException;
import com.incode.integration.exception.IncodeTimeoutException;
import com.incode.integration.service.WebhookAuditProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.HttpHostConnectException;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.UUID;

/**
 * Apache Camel {@link RouteBuilder} defining enterprise integration flows for:
 * <ul>
 *     <li>Incode Omni session/workflow initialization</li>
 *     <li>Streaming multipart identity document uploads</li>
 *     <li>Asynchronous webhook event ingestion and audit processing</li>
 *     <li>Dead-letter channel logging and upstream exception translation</li>
 * </ul>
 */
@Component
public class IncodeIntegrationRoute extends RouteBuilder {

    private static final Logger log = LoggerFactory.getLogger(IncodeIntegrationRoute.class);

    public static final String ROUTE_CREATE_SESSION = "direct:createIncodeSession";
    public static final String ROUTE_UPLOAD_DOCUMENT = "direct:uploadIncodeDocument";
    public static final String ROUTE_INGEST_WEBHOOK = "direct:ingestWebhookEvent";
    public static final String ROUTE_WEBHOOK_ASYNC = "seda:webhookAsyncProcessor?concurrentConsumers=4";
    public static final String ROUTE_DEAD_LETTER = "direct:deadLetterChannel";

    private static final String HEADER_API_KEY = "x-api-key";
    private static final String HEADER_API_VERSION = "api-version";
    private static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    private static final String HEADER_SESSION_ID = "X-Incode-Session-Id";
    private static final String HEADER_HARDWARE_ID = "X-Incode-Hardware-Id";
    private static final String HEADER_DOCUMENT_TYPE = "X-Document-Type";

    private final IncodeIntegrationProperties properties;
    private final WebhookAuditProcessor webhookAuditProcessor;

    public IncodeIntegrationRoute(
            IncodeIntegrationProperties properties,
            WebhookAuditProcessor webhookAuditProcessor) {
        this.properties = properties;
        this.webhookAuditProcessor = webhookAuditProcessor;
    }

    @Override
    public void configure() {
        errorHandler(deadLetterChannel(ROUTE_DEAD_LETTER)
                .maximumRedeliveries(2)
                .redeliveryDelay(250)
                .backOffMultiplier(2.0)
                .useExponentialBackOff()
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .logRetryAttempted(true));

        onException(HttpOperationFailedException.class)
                .maximumRedeliveries(0)
                .handled(true)
                .to(ROUTE_DEAD_LETTER)
                .process(exchange -> {
                    HttpOperationFailedException cause = exchange.getProperty(
                            Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);
                    int statusCode = cause != null ? cause.getStatusCode() : 502;
                    String statusText = cause != null ? cause.getStatusText() : "Unknown upstream error";
                    String responseBody = cause != null ? cause.getResponseBody() : null;
                    throw new IncodeApiException(
                            "Incode Omni API returned HTTP " + statusCode + " (" + statusText + ")",
                            statusCode,
                            responseBody,
                            cause);
                });

        onException(SocketTimeoutException.class, ConnectTimeoutException.class, HttpHostConnectException.class, IOException.class)
                .maximumRedeliveries(2)
                .redeliveryDelay(250)
                .backOffMultiplier(2.0)
                .useExponentialBackOff()
                .handled(true)
                .to(ROUTE_DEAD_LETTER)
                .process(exchange -> {
                    Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    String detail = cause != null ? cause.getMessage() : "Upstream network error";
                    throw new IncodeTimeoutException(
                            "Timeout or connectivity failure communicating with Incode Omni API: " + detail,
                            cause);
                });

        from(ROUTE_DEAD_LETTER)
                .routeId("incode-dead-letter-route")
                .process(exchange -> {
                    Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                    String routeId = exchange.getProperty(Exchange.FAILURE_ROUTE_ID, String.class);
                    String correlationId = exchange.getIn().getHeader(HEADER_CORRELATION_ID, String.class);
                    String endpoint = exchange.getProperty(Exchange.FAILURE_ENDPOINT, String.class);
                    log.error(
                            "DEAD_LETTER_CHANNEL routeId={} correlationId={} failedEndpoint={} exceptionType={} message={}",
                            routeId,
                            correlationId,
                            endpoint,
                            cause != null ? cause.getClass().getSimpleName() : "Unknown",
                            cause != null ? cause.getMessage() : "No exception details");
                });

        from(ROUTE_CREATE_SESSION)
                .routeId("incode-create-session-route")
                .process(exchange -> {
                    SessionCreateRequest request = exchange.getIn().getBody(SessionCreateRequest.class);
                    String correlationId = ensureCorrelationId(exchange);
                    IncodeStartSessionPayload outbound = new IncodeStartSessionPayload(
                            request.getWorkflowConfigurationId(),
                            request.getExternalCustomerId(),
                            request.getCountryCode(),
                            request.getRegion(),
                            request.getMetadata());
                    exchange.getIn().setBody(outbound);
                    exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
                    exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
                    exchange.getIn().setHeader(HEADER_API_KEY, properties.getApiKey());
                    exchange.getIn().setHeader(HEADER_API_VERSION, properties.getApiVersion());
                    exchange.getIn().setHeader(HEADER_CORRELATION_ID, correlationId);
                })
                .marshal().json(JsonLibrary.Jackson)
                .log(LoggingLevel.INFO, log, "Dispatching Incode session creation request correlationId=${header.X-Correlation-Id}")
                .to(buildIncodeEndpointUri("/omni/start"))
                .unmarshal().json(JsonLibrary.Jackson, IncodeStartSessionResponse.class);

        from(ROUTE_UPLOAD_DOCUMENT)
                .routeId("incode-upload-document-route")
                .process(exchange -> {
                    DocumentUploadCommand command = exchange.getIn().getBody(DocumentUploadCommand.class);
                    String correlationId = command.correlationId() != null && !command.correlationId().isBlank()
                            ? command.correlationId()
                            : ensureCorrelationId(exchange);

                    HttpEntity multipartEntity = MultipartEntityBuilder.create()
                            .addBinaryBody(
                                    "image",
                                    command.contentStream(),
                                    ContentType.parse(command.contentType()),
                                    command.filename())
                            .addTextBody("sessionId", command.sessionId(), ContentType.TEXT_PLAIN)
                            .addTextBody("documentType", command.documentType().name(), ContentType.TEXT_PLAIN)
                            .build();

                    exchange.getIn().setBody(multipartEntity);
                    exchange.getIn().removeHeader(Exchange.CONTENT_TYPE);
                    exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
                    exchange.getIn().setHeader(HEADER_API_KEY, properties.getApiKey());
                    exchange.getIn().setHeader(HEADER_API_VERSION, properties.getApiVersion());
                    exchange.getIn().setHeader(HEADER_SESSION_ID, command.sessionId());
                    exchange.getIn().setHeader(HEADER_HARDWARE_ID, "incode-integration-service");
                    exchange.getIn().setHeader(HEADER_DOCUMENT_TYPE, command.documentType().name());
                    exchange.getIn().setHeader(HEADER_CORRELATION_ID, correlationId);
                })
                .log(LoggingLevel.INFO, log, "Streaming identity document to Incode sessionId=${header.X-Incode-Session-Id} correlationId=${header.X-Correlation-Id}")
                .to(buildIncodeEndpointUri("/omni/add/document"))
                .unmarshal().json(JsonLibrary.Jackson, IncodeDocumentUploadResponse.class);

        from(ROUTE_INGEST_WEBHOOK)
                .routeId("incode-webhook-ingest-route")
                .log(LoggingLevel.INFO, log, "Enqueuing verified Incode webhook eventId=${header.X-Event-Id}")
                .to("seda:webhookAsyncProcessor?waitForTaskToComplete=Never");

        from(ROUTE_WEBHOOK_ASYNC)
                .routeId("incode-webhook-async-route")
                .process(exchange -> {
                    IncodeWebhookPayload payload = exchange.getIn().getBody(IncodeWebhookPayload.class);
                    webhookAuditProcessor.processAsync(payload);
                });
    }

    private String buildIncodeEndpointUri(String path) {
        String baseUrl = properties.getBaseUrl().replaceAll("/+$", "");
        return baseUrl + path + "?httpClientConfigurer=#incodeHttpClientConfigurer&throwExceptionOnFailure=true";
    }

    private String ensureCorrelationId(Exchange exchange) {
        String existing = exchange.getIn().getHeader(HEADER_CORRELATION_ID, String.class);
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        return UUID.randomUUID().toString();
    }
}

