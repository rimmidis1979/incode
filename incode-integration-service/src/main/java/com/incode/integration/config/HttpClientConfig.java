package com.incode.integration.config;

import org.apache.camel.component.http.HttpClientConfigurer;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

/**
 * Configures outbound HTTP client behavior, timeouts, connection pooling,
 * and TLS verification for Apache Camel HTTP endpoints.
 */
@Configuration
public class HttpClientConfig {

    /**
     * Builds the Camel {@link HttpClientConfigurer} bean enforcing connect/read timeouts
     * and TLS hostname verification against the configured Incode properties.
     *
     * @param properties externalized Incode integration properties
     * @return configured {@link HttpClientConfigurer}
     */
    @Bean("incodeHttpClientConfigurer")
    public HttpClientConfigurer incodeHttpClientConfigurer(IncodeIntegrationProperties properties) {
        return clientBuilder -> {
            Timeout connectTimeout = Timeout.ofMilliseconds(properties.getConnectTimeout());
            Timeout readTimeout = Timeout.ofMilliseconds(properties.getReadTimeout());

            ConnectionConfig connectionConfig = ConnectionConfig.custom()
                    .setConnectTimeout(connectTimeout)
                    .setSocketTimeout(readTimeout)
                    .build();

            SSLContext sslContext = SSLContexts.createDefault();
            HostnameVerifier hostnameVerifier = properties.getTls().isVerifyHostname()
                    ? new DefaultHostnameVerifier()
                    : NoopHostnameVerifier.INSTANCE;

            DefaultClientTlsStrategy tlsStrategy = new DefaultClientTlsStrategy(sslContext, hostnameVerifier);

            PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                    .setDefaultConnectionConfig(connectionConfig)
                    .setTlsSocketStrategy(tlsStrategy)
                    .setMaxConnTotal(200)
                    .setMaxConnPerRoute(50)
                    .build();

            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(connectTimeout)
                    .setResponseTimeout(readTimeout)
                    .build();

            clientBuilder
                    .setConnectionManager(connectionManager)
                    .setDefaultRequestConfig(requestConfig);
        };
    }
}

