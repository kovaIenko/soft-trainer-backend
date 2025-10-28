package com.backend.softtrainer.filters;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Security filter to block malicious requests and prevent them from reaching the application
 * This helps prevent the HTTP parsing errors and potential security vulnerabilities
 */
@Component
@Order(1)
@Slf4j
public class SecurityRequestFilter implements Filter {

    // Common malicious patterns to block
    private static final List<Pattern> MALICIOUS_PATTERNS = Arrays.asList(
        // PHP exploitation attempts
        Pattern.compile(".*\\\\think\\\\.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\.php.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*index\\.php.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*call_user_func_array.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*invokefunction.*", Pattern.CASE_INSENSITIVE),
        
        // SQL injection attempts
        Pattern.compile(".*union.*select.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*drop.*table.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*insert.*into.*", Pattern.CASE_INSENSITIVE),
        
        // XSS attempts
        Pattern.compile(".*<script.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*javascript:.*", Pattern.CASE_INSENSITIVE),
        
        // Directory traversal
        Pattern.compile(".*\\.\\./.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\\\\\\\.*", Pattern.CASE_INSENSITIVE),
        
        // Command injection
        Pattern.compile(".*\\|.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*;.*", Pattern.CASE_INSENSITIVE)
    );

    // Suspicious user agents that are commonly used by bots/scanners
    private static final List<Pattern> SUSPICIOUS_USER_AGENTS = Arrays.asList(
        Pattern.compile(".*nikto.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*sqlmap.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*nmap.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*scan.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*bot.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*crawler.*", Pattern.CASE_INSENSITIVE)
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();
        String userAgent = httpRequest.getHeader("User-Agent");
        String referer = httpRequest.getHeader("Referer");
        String host = httpRequest.getHeader("Host");

        // Check for malicious patterns in URI
        if (containsMaliciousPattern(requestURI)) {
            log.warn("Blocked malicious request to URI: {} from IP: {}", 
                    requestURI, getClientIpAddress(httpRequest));
            sendSecurityResponse(httpResponse);
            return;
        }

        // Check for suspicious user agents
        if (userAgent != null && containsSuspiciousUserAgent(userAgent)) {
            log.warn("Blocked request with suspicious user agent: {} from IP: {}", 
                    userAgent, getClientIpAddress(httpRequest));
            sendSecurityResponse(httpResponse);
            return;
        }

        // Check for invalid host headers
        if (host != null && isInvalidHost(host)) {
            log.warn("Blocked request with invalid host header: {} from IP: {}", 
                    host, getClientIpAddress(httpRequest));
            sendSecurityResponse(httpResponse);
            return;
        }

        // Check for suspicious referers
        if (referer != null && containsMaliciousPattern(referer)) {
            log.warn("Blocked request with suspicious referer: {} from IP: {}", 
                    referer, getClientIpAddress(httpRequest));
            sendSecurityResponse(httpResponse);
            return;
        }

        // Request passed all security checks, continue with the chain
        chain.doFilter(request, response);
    }

    private boolean containsMaliciousPattern(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        return MALICIOUS_PATTERNS.stream()
                .anyMatch(pattern -> pattern.matcher(input).matches());
    }

    private boolean containsSuspiciousUserAgent(String userAgent) {
        return SUSPICIOUS_USER_AGENTS.stream()
                .anyMatch(pattern -> pattern.matcher(userAgent).matches());
    }

    private boolean isInvalidHost(String host) {
        // Check if host contains protocol (which is invalid for Host header)
        return host.startsWith("http://") || host.startsWith("https://") || 
               host.contains("://") || host.contains("\\");
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private void sendSecurityResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Request blocked for security reasons\"}");
        response.getWriter().flush();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("Security request filter initialized");
    }

    @Override
    public void destroy() {
        log.info("Security request filter destroyed");
    }
}
