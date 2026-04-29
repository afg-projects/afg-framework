package io.github.afgprojects.framework.core.web.security.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import lombok.Setter;

/**
 * 安全响应头过滤器
 */
@Setter
public class SecurityHeaderFilter extends OncePerRequestFilter {

    private String contentSecurityPolicy;
    private String strictTransportSecurity;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");

        if (contentSecurityPolicy != null) {
            response.setHeader("Content-Security-Policy", contentSecurityPolicy);
        }
        if (strictTransportSecurity != null) {
            response.setHeader("Strict-Transport-Security", strictTransportSecurity);
        }

        filterChain.doFilter(request, response);
    }
}
