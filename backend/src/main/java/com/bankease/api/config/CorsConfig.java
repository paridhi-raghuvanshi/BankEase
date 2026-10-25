package com.bankease.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS (Cross-Origin Resource Sharing) configuration for the BankEase API.
 *
 * <p>Allowed origins are loaded from the {@code app.cors.allowed-origins} application property
 * (comma-separated). All HTTP methods and headers are permitted, and credentials (cookies,
 * Authorization headers) are allowed so that the browser-based frontend can authenticate.</p>
 *
 * <p>Example application.yml snippet:</p>
 * <pre>
 * app:
 *   cors:
 *     allowed-origins: "http://localhost:3000,https://bankease.example.com"
 * </pre>
 */
@Configuration
public class CorsConfig {

    /**
     * Comma-separated list of allowed origins, injected from {@code app.cors.allowed-origins}.
     * Defaults to {@code http://localhost:3000} for local development.
     */
    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOriginsRaw;

    /**
     * Builds and registers a {@link CorsConfigurationSource} that applies to all URL patterns.
     *
     * <p>Configuration details:</p>
     * <ul>
     *   <li><strong>Origins:</strong> parsed from {@code app.cors.allowed-origins}</li>
     *   <li><strong>Methods:</strong> GET, POST, PUT, PATCH, DELETE, OPTIONS</li>
     *   <li><strong>Headers:</strong> all headers allowed</li>
     *   <li><strong>Exposed headers:</strong> Authorization, X-Total-Count (for pagination)</li>
     *   <li><strong>Credentials:</strong> allowed (required for Bearer token support)</li>
     *   <li><strong>Max age:</strong> 3600 seconds (browser pre-flight cache duration)</li>
     * </ul>
     *
     * @return a {@link CorsConfigurationSource} covering all endpoints ({@code /**})
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Parse and apply origins
        List<String> origins = Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        config.setAllowedOrigins(origins);

        // Allowed HTTP methods
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Allow all request headers (the browser will handle preflight negotiation)
        config.setAllowedHeaders(List.of("*"));

        // Expose additional headers that the frontend needs to read
        config.setExposedHeaders(List.of("Authorization", "X-Total-Count"));

        // Allow credentials so the Authorization header works in browsers
        config.setAllowCredentials(true);

        // Cache the pre-flight response for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
