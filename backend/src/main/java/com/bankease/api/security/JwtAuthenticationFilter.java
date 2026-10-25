package com.bankease.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that intercepts every HTTP request and performs JWT-based authentication.
 *
 * <p>For each request the filter:</p>
 * <ol>
 *   <li>Extracts the {@code Authorization} header and parses the Bearer token.</li>
 *   <li>Validates the token using {@link JwtTokenProvider}.</li>
 *   <li>Loads the corresponding {@link UserDetails} from the database.</li>
 *   <li>Creates a {@link UsernamePasswordAuthenticationToken} and stores it in
 *       the {@link SecurityContextHolder} so downstream security checks pass.</li>
 * </ol>
 *
 * <p>Any exception during token parsing is swallowed and logged; the request
 * continues unauthenticated and Spring Security's access-control layer will
 * reject it if the endpoint requires authentication.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX         = "Bearer ";

    private final JwtTokenProvider    jwtTokenProvider;
    private final UserDetailsService  userDetailsService;

    /**
     * Core filter logic: extract → validate → authenticate.
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response (not modified by this filter)
     * @param filterChain the remaining filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         filterChain)
            throws ServletException, IOException {

        try {
            String token = extractBearerToken(request);

            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                String email = jwtTokenProvider.getEmailFromToken(token);
                log.debug("Valid JWT found for user '{}', setting security context", email);

                // Only set authentication if not already set for this request
                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities());

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ex) {
            // Log the error and allow the filter chain to continue.
            // Spring Security's ExceptionTranslationFilter will return a 401 if
            // the downstream handler requires authentication.
            log.error("Could not set user authentication in security context: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the raw JWT string from the {@code Authorization: Bearer <token>} header.
     *
     * @param request the HTTP request
     * @return the token string, or {@code null} if the header is absent / malformed
     */
    private String extractBearerToken(HttpServletRequest request) {
        String headerValue = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(headerValue) && headerValue.startsWith(BEARER_PREFIX)) {
            return headerValue.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
