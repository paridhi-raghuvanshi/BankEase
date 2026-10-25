package com.bankease.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SpringDoc OpenAPI 2.x (Swagger UI) configuration for the BankEase API.
 *
 * <p>Registers a JWT Bearer security scheme named {@code BearerAuth} and applies it as a
 * global security requirement so that every endpoint in the generated documentation shows
 * the padlock icon and accepts an {@code Authorization: Bearer <token>} header.</p>
 *
 * <p>Swagger UI is served at {@code /swagger-ui/index.html} and the raw OpenAPI spec
 * at {@code /v3/api-docs} (both paths are whitelisted in {@link SecurityConfig}).</p>
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    /** Base URL of the server shown in the Servers drop-down of Swagger UI. */
    @Value("${app.server.url:http://localhost:8080}")
    private String serverUrl;

    /**
     * Builds the {@link OpenAPI} definition with metadata, server info, security scheme,
     * and a global security requirement.
     *
     * @return fully configured {@link OpenAPI} instance picked up by SpringDoc
     */
    @Bean
    public OpenAPI bankEaseOpenAPI() {
        return new OpenAPI()
                .info(buildApiInfo())
                .servers(List.of(buildServer()))
                .components(buildComponents())
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }

    // -------------------------------------------------------------------------
    // Private builders
    // -------------------------------------------------------------------------

    /**
     * Constructs the API {@link Info} block with title, version, description, contact,
     * and license.
     */
    private Info buildApiInfo() {
        return new Info()
                .title("BankEase NextGen Net Banking API")
                .version("1.0.0")
                .description("""
                        RESTful API for the BankEase NextGen Internet Banking platform.

                        Provides endpoints for:
                        - **Authentication** – registration, login, token refresh, logout
                        - **Account Management** – open, close, and query bank accounts
                        - **Transactions** – fund transfers (NEFT/RTGS/IMPS), deposits, withdrawals
                        - **Beneficiary Management** – add, update, and remove beneficiaries
                        - **Statements** – paginated transaction history and PDF statement download
                        - **User Profile** – view and update personal information

                        All monetary values are represented as `BigDecimal` with currency code ISO 4217.
                        Authentication uses short-lived JWT access tokens (Bearer) and long-lived refresh tokens.
                        """)
                .contact(new Contact()
                        .name("BankEase Engineering Team")
                        .email("engineering@bankease.example.com")
                        .url("https://bankease.example.com"))
                .license(new License()
                        .name("Proprietary – BankEase Ltd.")
                        .url("https://bankease.example.com/legal/api-license"));
    }

    /**
     * Constructs the {@link Server} entry shown in Swagger UI's server drop-down.
     */
    private Server buildServer() {
        return new Server()
                .url(serverUrl)
                .description("BankEase API Server");
    }

    /**
     * Constructs the {@link Components} block containing the JWT Bearer {@link SecurityScheme}.
     *
     * <p>The scheme is an HTTP Bearer scheme using JWT format. Once registered here and
     * referenced in the global security requirement, every generated endpoint will include
     * the bearer token requirement by default. Individual operations can override this using
     * the {@code @SecurityRequirements} annotation.</p>
     */
    private Components buildComponents() {
        SecurityScheme jwtBearerScheme = new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                        Enter a valid JWT access token obtained from `POST /api/v1/auth/login`.

                        Format: `Bearer <token>` (the "Bearer " prefix is added automatically by Swagger UI).
                        """);

        return new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, jwtBearerScheme);
    }
}
