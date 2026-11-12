package com.incode.integration.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class IncodeIntegrationPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void shouldBindValidIncodeIntegrationProperties() {
        contextRunner
                .withPropertyValues(
                        "incode.integration.base-url=https://api.incodesmile.com",
                        "incode.integration.api-key=live-api-key",
                        "incode.integration.webhook.secret=live-webhook-secret",
                        "incode.integration.connect-timeout=3000",
                        "incode.integration.read-timeout=12000")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    IncodeIntegrationProperties props = context.getBean(IncodeIntegrationProperties.class);
                    assertThat(props.getBaseUrl()).isEqualTo("https://api.incodesmile.com");
                    assertThat(props.getApiKey()).isEqualTo("live-api-key");
                    assertThat(props.getWebhook().getSecret()).isEqualTo("live-webhook-secret");
                    assertThat(props.getConnectTimeout()).isEqualTo(3000);
                    assertThat(props.getReadTimeout()).isEqualTo(12000);
                });
    }

    @Test
    void shouldFailStartupWhenRequiredSecretsOrUrlAreMissing() {
        contextRunner
                .withPropertyValues(
                        "incode.integration.base-url=",
                        "incode.integration.api-key=key",
                        "incode.integration.webhook.secret=secret")
                .run(context -> assertThat(context).hasFailed());
    }

    @EnableConfigurationProperties(IncodeIntegrationProperties.class)
    static class TestConfig {
    }
}

