package com.incode.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point for the Incode Integration Microservice.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class IncodeIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(IncodeIntegrationApplication.class, args);
    }
}

