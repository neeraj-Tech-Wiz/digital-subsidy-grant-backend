package com.infosys.subsidy.controller;

import com.infosys.subsidy.service.AdminAuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/audit")
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    public AdminAuditController(AdminAuditService adminAuditService) {
        this.adminAuditService = adminAuditService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String applicationId,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        if (size > 100) {
            size = 100;
        }

        Map<String, Object> result = adminAuditService.getUnifiedAuditLogs(
                eventType, actor, role, applicationId, success, from, to, page, size
        );

        return ResponseEntity.ok(result);
    }
}
