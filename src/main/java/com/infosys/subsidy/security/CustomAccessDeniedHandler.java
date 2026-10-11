package com.infosys.subsidy.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infosys.subsidy.service.AuditService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public CustomAccessDeniedHandler(AuditService auditService) {
        this.auditService = auditService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = "Anonymous";
        String role = "UNAUTHENTICATED";
        
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            email = auth.getName();
            if (!auth.getAuthorities().isEmpty()) {
                role = auth.getAuthorities().iterator().next().getAuthority();
            }
        }

        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        String uri = request.getRequestURI();
        String method = request.getMethod();

        auditService.logAccessDenied(email, role, ip, userAgent, method, uri);

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");

        Map<String, Object> error = new HashMap<>();
        error.put("error", "Access Denied");
        error.put("message", "You do not have permission to access this resource.");
        error.put("timestamp", LocalDateTime.now().toString());

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
