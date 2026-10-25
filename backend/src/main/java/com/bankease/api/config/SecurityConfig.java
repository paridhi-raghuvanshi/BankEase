package com.bankease.api.config;

import com.bankease.api.security.JwtAuthenticationFilter;
import com.bankease.api.security.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Central Spring Security 6.x configuration for the BankEase application.
 *
 * <p>Key decisions:</p>
 * <ul>
 *   <li>CSRF protection is <strong>disabled</strong> because the API is stateless (JWT-based).</li>
 *   <li>Session management is set to {@link SessionCreationPolicy#STATELESS}; no HTTP session
 *       is created or consulted.</li>
 *   <li>CORS is delegated to the {@link CorsConfigurationSource} bean defined in
 *       {@link CorsConfig}.</li>
 *   <li>{@link JwtAuthenticationFilter} runs before the standard
 *       {@link UsernamePasswordAuthenticationFilter}.</li>
 *   <li>{@link RateLimitingFilter} is prepended before the JWT filter so over-limit requests
 *       are rejected before any token parsing occurs.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** Public endpoints that require no authentication. */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/v1/auth/**",
            "/api/v1/auth/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/actuator/**",
            "/h2-console/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter       rateLimitingFilter;
    private final UserDetailsService       userDetailsService;
    private final CorsConfigurationSource  corsConfigurationSource;

    // -------------------------------------------------------------------------
    // SecurityFilterChain
    // -------------------------------------------------------------------------

    /**
     * Defines the primary HTTP security filter chain.
     *
     * @param http the {@link HttpSecurity} builder
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if the configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Disable CSRF – JWT tokens are the CSRF mitigation strategy
                .csrf(AbstractHttpConfigurer::disable)

                // CORS – use the CorsConfigurationSource bean from CorsConfig
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // Stateless sessions – never create or use an HTTP session
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())

                // Wire the custom authentication provider
                .authenticationProvider(authenticationProvider())

                // Inject the rate-limiter first, then the JWT filter
                .addFilterBefore(rateLimitingFilter,       UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    // -------------------------------------------------------------------------
    // Authentication Beans
    // -------------------------------------------------------------------------

    /**
     * Configures the {@link DaoAuthenticationProvider} that delegates user lookup
     * to our {@link UserDetailsService} and uses BCrypt for password verification.
     *
     * @return configured {@link AuthenticationProvider}
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes the {@link AuthenticationManager} from Spring Security's
     * {@link AuthenticationConfiguration}, required by the auth controller.
     *
     * @param config Spring's authentication configuration
     * @return the application-wide {@link AuthenticationManager}
     * @throws Exception if retrieval fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * BCrypt password encoder bean used both for encoding during registration
     * and for verification during login.
     *
     * @return a {@link BCryptPasswordEncoder} with default strength (10)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
