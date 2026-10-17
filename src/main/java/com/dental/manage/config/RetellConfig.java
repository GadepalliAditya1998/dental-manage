package com.dental.manage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Retell AI REST client configuration.
 */
@Configuration
public class RetellConfig {

    @Value("${retell.api-key:}")
    private String apiKey;

    @Value("${retell.base-url:https://api.retellai.com}")
    private String baseUrl;

    @Bean
    public RestClient retellRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
