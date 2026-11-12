# Incode Omni Integration Microservice

Production-ready Java 21 / Spring Boot 3.4 / Apache Camel 4.10 microservice integrating with the **Incode Omni Identity Verification API** and asynchronous **Incode Webhook / Callback listener**.

## Architecture & Features

- **Runtime & Stack**: Java 21 LTS, Spring Boot 3.4.1, Apache Camel 4.10.0, Apache HttpClient 5.
- **Session Initialization**: `POST /api/v1/compliance/sessions` creates Incode Omni verification flows via Camel routes.
- **Multipart Document Verification**: `POST /api/v1/compliance/verify` accepts binary ID uploads (`image/jpeg`, `image/png`) with magic-byte validation and streams them to Incode.
- **Secure Webhook Callback Listener**: `POST /api/v1/compliance/webhook/callback` verifies incoming HMAC-SHA256 signatures in constant time (`X-Incode-Signature`), returns immediate `200 OK`, and processes events asynchronously via Camel SEDA queues.
- **Enterprise Error Handling**: RFC 7807 Problem Details (`application/problem+json`) with dead-letter channels and exponential backoff retry policies.
- **Testing**: 34 JUnit 5, Mockito, and `@CamelSpringBootTest` test cases (100% pass rate).
- **Postman Ready**: Pre-request HMAC signing scripts and automated assertion tests in `incode-integration-service/postman/`.

## Quick Start

```bash
cd incode-integration-service
mvn test
mvn spring-boot:run
```
