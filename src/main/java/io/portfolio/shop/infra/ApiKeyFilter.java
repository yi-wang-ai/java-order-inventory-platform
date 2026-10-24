package io.portfolio.shop.infra;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Local demo authorization. Replace with identity-based RBAC before production use. */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {
    private final byte[] expected;
    public ApiKeyFilter(@Value("${app.api-key}") String apiKey) {
        if (apiKey == null || apiKey.length() < 16) throw new IllegalArgumentException("APP_API_KEY must have at least 16 characters");
        expected = apiKey.getBytes(StandardCharsets.UTF_8);
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String candidate = request.getHeader("X-API-Key");
        if (candidate == null || !MessageDigest.isEqual(expected, candidate.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Valid X-API-Key required");
            return;
        }
        chain.doFilter(request, response);
    }
}
