package com.infosys.subsidy.service;

import com.infosys.subsidy.entity.SystemAuditLog;
import com.infosys.subsidy.enums.AuditEventType;
import com.infosys.subsidy.repository.SystemAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private final SystemAuditLogRepository auditLogRepository;

    public AuditService(SystemAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Logs an event within the current transaction boundary.
     * Transactional consistency means if the business transaction rolls back, this audit log also rolls back.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void logEvent(SystemAuditLog auditLog) {
        try {
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            logger.error("Failed to persist audit log for event: " + auditLog.getEventType(), e);
            // We eat the exception here cautiously to avoid failing business ops purely due to logging issues,
            // though JPA might still mark the tx as rollback-only if it's a critical DB constraint error.
        }
    }

    /**
     * Specifically for logging failures that occur during Authentication or similar processes
     * where the main transaction either doesn't exist or is rolling back, but we still need the audit log.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuthEventRequiresNew(SystemAuditLog auditLog) {
        try {
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            logger.error("Failed to persist REQUIRES_NEW audit log", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAccessDenied(String email, String role, String ipAddress, String userAgent, String method, String uri) {
        try {
            SystemAuditLog log = new SystemAuditLog();
            log.setEventType(AuditEventType.ACCESS_DENIED);
            log.setActorEmail(email != null ? email : "Anonymous");
            log.setActorRole(role != null ? role : "UNAUTHENTICATED");
            log.setActorType("USER");
            log.setResourceType("ENDPOINT");
            log.setResourceId(method + " " + uri);
            log.setSuccess(false);
            log.setFailureReason("Access Denied");
            log.setIpAddress(ipAddress);
            log.setUserAgent(userAgent);
            log.setDescription("Denied access to " + uri);
            
            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.error("Failed to log access denied event", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void logAdminAction(AuditEventType eventType, String resourceType, String resourceId, String description) {
        try {
            SystemAuditLog log = new SystemAuditLog();
            log.setEventType(eventType);
            log.setResourceType(resourceType);
            log.setResourceId(resourceId);
            log.setDescription(description);
            log.setSuccess(true);
            log.setActorType("USER");

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                log.setActorEmail(auth.getName());
                if (!auth.getAuthorities().isEmpty()) {
                    log.setActorRole(auth.getAuthorities().iterator().next().getAuthority());
                }
                // Custom user principal could provide ID, but email is enough if we don't have it direct
            } else {
                log.setActorEmail("SYSTEM");
                log.setActorRole("SYSTEM");
                log.setActorType("SYSTEM");
            }

            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                log.setIpAddress(req.getRemoteAddr());
                log.setUserAgent(req.getHeader("User-Agent"));
            }

            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.error("Failed to persist admin audit log", e);
        }
    }
}
