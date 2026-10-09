package com.buildflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "buildflow.cors")
public record CorsProperties(
        List<String> allowedOrigins
) {
}
