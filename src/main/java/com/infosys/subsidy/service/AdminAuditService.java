package com.infosys.subsidy.service;

import com.infosys.subsidy.dto.AuditResponseDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminAuditService {

    private final EntityManager entityManager;

    public AdminAuditService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Map<String, Object> getUnifiedAuditLogs(
            String eventType, 
            String actorEmail, 
            String actorRole,
            String applicationId,
            Boolean success,
            String fromDate,
            String toDate,
            int page, 
            int size
    ) {
        String baseSystemSql = "SELECT " +
                " id as original_id, " +
                " 'SYSTEM' as source_table, " +
                " timestamp, " +
                " actor_email, " +
                " actor_role, " +
                " event_type, " +
                " resource_type, " +
                " resource_id, " +
                " application_id, " +
                " success, " +
                " remarks, " +
                " failure_reason, " +
                " action, " +
                " CAST(NULL as DECIMAL) as amount " +
                " FROM system_audit_log ";
                
        String baseHistorySql = "SELECT " +
                " id as original_id, " +
                " 'BUSINESS' as source_table, " +
                " action_timestamp as timestamp, " +
                " officer_name as actor_email, " +
                " officer_role as actor_role, " +
                " action as event_type, " +
                " 'APPLICATION' as resource_type, " +
                " CAST(application_id as VARCHAR) as resource_id, " +
                " CAST(application_id as VARCHAR) as application_id, " +
                " true as success, " +
                " remarks, " +
                " CAST(NULL as VARCHAR) as failure_reason, " +
                " CONCAT(previous_status, ' -> ', new_status) as action, " +
                " amount " +
                " FROM verification_history ";
                
        String baseLegacySql = "SELECT " +
                " id as original_id, " +
                " 'LEGACY_DISBURS' as source_table, " +
                " disbursed_at as timestamp, " +
                " (SELECT email FROM users WHERE id = disbursed_by) as actor_email, " +
                " 'GRANT_OFFICER' as actor_role, " +
                " 'LEGACY_GRANT_DISBURSED' as event_type, " +
                " 'APPLICATION' as resource_type, " +
                " CAST(application_id as VARCHAR) as resource_id, " +
                " CAST(application_id as VARCHAR) as application_id, " +
                " true as success, " +
                " CONCAT('Legacy disbursement: TRX ', transaction_reference) as remarks, " +
                " CAST(NULL as VARCHAR) as failure_reason, " +
                " 'LEGACY_GRANT_DISBURSED' as action, " +
                " grant_amount as amount " +
                " FROM grant_disbursements ";

        StringBuilder systemWhere = new StringBuilder(" WHERE 1=1 ");
        
        // Exclude Beneficiary logs for Authentication events globally from this view
        systemWhere.append(" AND NOT (actor_role = 'BENEFICIARY' AND event_type IN ('LOGIN_SUCCESS', 'LOGIN_FAILED', 'LOGOUT')) ");

        StringBuilder historyWhere = new StringBuilder(" WHERE 1=1 ");
        StringBuilder legacyWhere = new StringBuilder(" WHERE 1=1 ");
        Map<String, Object> params = new HashMap<>();

        if (eventType != null && !eventType.trim().isEmpty()) {
            String category = eventType.trim();
            java.util.List<String> systemEvents = new java.util.ArrayList<>();
            java.util.List<String> historyActions = new java.util.ArrayList<>();

            switch (category) {
                case "AUTHENTICATION":
                    systemEvents.addAll(java.util.Arrays.asList("LOGIN_SUCCESS", "LOGIN_FAILED", "LOGOUT"));
                    break;
                case "APPLICATION":
                    historyActions.addAll(java.util.Arrays.asList("APPLICATION_SUBMITTED", "APPLICATION_REVIEWED", "RETURNED_TO_APPLICANT", "RESUBMITTED", "APPROVED", "REJECTED", "FINAL_APPROVED", "ESCALATION_REVIEWED", "FORWARDED"));
                    break;
                case "VERIFICATION":
                    historyActions.addAll(java.util.Arrays.asList("ELIGIBILITY_SCORED", "DOCUMENT_SUBMITTED", "DOCUMENT_UPLOADED", "DOCUMENT_VERIFIED", "DOCUMENT_REJECTED", "ELIGIBILITY_VERIFIED", "FORWARDED"));
                    break;
                case "COMPLIANCE":
                    historyActions.addAll(java.util.Arrays.asList("BANKING_DOCUMENT_SUBMITTED", "BANKING_DOCUMENT_VERIFIED", "BANKING_DOCUMENT_REJECTED", "UTILIZATION_PROOF_SUBMITTED", "UTILIZATION_PROOF_NON_COMPLIANT", "UTILIZATION_PROOF_RESUBMITTED", "UTILIZATION_PROOF_VERIFIED"));
                    break;
                case "FINANCIAL":
                    historyActions.addAll(java.util.Arrays.asList("DISBURSEMENT_PLAN_CREATED", "MILESTONE_RELEASED", "FUND_RELEASE_CREATED"));
                    break;
                case "ADMINISTRATION":
                    systemEvents.addAll(java.util.Arrays.asList("SCHEME_CREATED", "SCHEME_UPDATED", "SCHEME_STATUS_CHANGED", "SCHEME_DELETED", "OFFICER_CREATED", "OFFICER_UPDATED", "OFFICER_ACTIVATED", "OFFICER_DEACTIVATED"));
                    break;
                case "SECURITY":
                    systemEvents.addAll(java.util.Arrays.asList("ACCESS_DENIED", "LOGIN_FAILED"));
                    break;
                default:
                    systemEvents.add(category);
                    historyActions.add(category);
                    break;
            }

            if (!systemEvents.isEmpty()) {
                systemWhere.append(" AND event_type IN :systemEvents ");
                params.put("systemEvents", systemEvents);
            } else {
                systemWhere.append(" AND 1=0 ");
            }

            if (!historyActions.isEmpty()) {
                historyWhere.append(" AND action IN :historyActions ");
                params.put("historyActions", historyActions);
            } else {
                historyWhere.append(" AND 1=0 ");
            }
            
            if ("FINANCIAL".equals(category)) {
                legacyWhere.append(" AND 1=1 ");
            } else {
                legacyWhere.append(" AND 1=0 ");
            }
        }
        
        if (actorEmail != null && !actorEmail.trim().isEmpty()) {
            systemWhere.append(" AND actor_email LIKE :actorEmail ");
            historyWhere.append(" AND officer_name LIKE :actorEmail ");
            legacyWhere.append(" AND (SELECT email FROM users WHERE id = disbursed_by) LIKE :actorEmail ");
            params.put("actorEmail", "%" + actorEmail.trim() + "%");
        }

        if (actorRole != null && !actorRole.trim().isEmpty()) {
            String roleVal = actorRole.trim();
            systemWhere.append(" AND (actor_role = :actorRole OR actor_role = :rolePrefixed) ");
            historyWhere.append(" AND (officer_role = :actorRole OR officer_role = :rolePrefixed) ");
            legacyWhere.append(" AND ('GRANT_OFFICER' = :actorRole OR 'ROLE_GRANT_OFFICER' = :rolePrefixed) ");
            params.put("actorRole", roleVal);
            params.put("rolePrefixed", roleVal.startsWith("ROLE_") ? roleVal : "ROLE_" + roleVal);
        }
        
        if (applicationId != null && !applicationId.trim().isEmpty()) {
            systemWhere.append(" AND application_id = :appId ");
            historyWhere.append(" AND CAST(application_id as VARCHAR) = :appId ");
            legacyWhere.append(" AND CAST(application_id as VARCHAR) = :appId ");
            params.put("appId", applicationId.trim());
        }

        if (success != null) {
            systemWhere.append(" AND success = :success ");
            // VerificationHistory events are practically always 'success' true businesses processes completed.
            // If the frontend filters by success=false, history returns 0 records.
            if (!success) {
                historyWhere.append(" AND 1=0 "); 
                legacyWhere.append(" AND 1=0 "); 
            }
            params.put("success", success);
        }

        if (fromDate != null && !fromDate.trim().isEmpty()) {
            systemWhere.append(" AND timestamp >= :fromDate ");
            historyWhere.append(" AND action_timestamp >= :fromDate ");
            params.put("fromDate", java.sql.Timestamp.valueOf(fromDate.trim() + " 00:00:00"));
        }

        if (toDate != null && !toDate.trim().isEmpty()) {
            systemWhere.append(" AND timestamp <= :toDate ");
            historyWhere.append(" AND action_timestamp <= :toDate ");
            legacyWhere.append(" AND disbursed_at <= :toDate ");
            params.put("toDate", java.sql.Timestamp.valueOf(toDate.trim() + " 23:59:59"));
        }

        String unionSql = baseSystemSql + systemWhere.toString() + 
                 " UNION ALL " + 
                 baseHistorySql + historyWhere.toString() +
                 " UNION ALL " +
                 baseLegacySql + legacyWhere.toString();
                 

        String countSql = "SELECT COUNT(*) FROM (" + unionSql + ") as combined";
        String paginatedSql = unionSql + " ORDER BY timestamp DESC LIMIT :limit OFFSET :offset";

        Query countQuery = entityManager.createNativeQuery(countSql);
        Query dataQuery = entityManager.createNativeQuery(paginatedSql);

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            countQuery.setParameter(entry.getKey(), entry.getValue());
            dataQuery.setParameter(entry.getKey(), entry.getValue());
        }
        
        dataQuery.setParameter("limit", size);
        dataQuery.setParameter("offset", page * size);

        long totalElements = ((Number) countQuery.getSingleResult()).longValue();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        @SuppressWarnings("unchecked")
        List<Object[]> rawResults = dataQuery.getResultList();
        
        List<AuditResponseDTO> dtoList = new ArrayList<>();

        for (Object[] row : rawResults) {
            AuditResponseDTO dto = new AuditResponseDTO();
            // id, source_table, timestamp, actor_email, actor_role, event_type, 
            // resource_type, resource_id, application_id, success, remarks, failure_reason, action
            
            dto.setId(((Number) row[0]).longValue());
            // row[1] is source_table (SYSTEM or BUSINESS) not strictly needed in dto, but nice
            
            Object tsObj = row[2];
            if (tsObj instanceof Timestamp) {
                dto.setTimestamp(((Timestamp) tsObj).toLocalDateTime());
            } else if (tsObj instanceof LocalDateTime) {
                dto.setTimestamp((LocalDateTime) tsObj);
            }
            
            dto.setActorEmail((String) row[3]);
            dto.setActorRole((String) row[4]);
            
            String eventTypeStr = (String) row[5];
            try {
                dto.setEventType(com.infosys.subsidy.enums.AuditEventType.valueOf(eventTypeStr));
            } catch (Exception e) {
                // If it's a verification history action not in the enum, just map it as string if we changed the DTO
                // Wait, DTO uses enum AuditEventType! I need to ensure VerificationHistory actions match or I need to change DTO eventType to String in UI.
                // Let's assume AuditResponseDTO eventType should be String for unified list.
            }
            
            dto.setResourceType((String) row[6]);
            dto.setResourceId((String) row[7]);
            dto.setApplicationId((String) row[8]);
            
            Object succObj = row[9];
            if (succObj instanceof Boolean) {
                dto.setSuccess((Boolean) succObj);
            } else if (succObj instanceof Number) {
                dto.setSuccess(((Number) succObj).intValue() > 0);
            }

            dto.setRemarks((String) row[10]);
            dto.setFailureReason((String) row[11]);
            dto.setAction((String) row[12]);
            
            if (row[13] != null) {
                if (row[13] instanceof java.math.BigDecimal) {
                    dto.setAmount((java.math.BigDecimal) row[13]);
                } else if (row[13] instanceof Number) {
                    dto.setAmount(new java.math.BigDecimal(((Number) row[13]).doubleValue()));
                }
            }
            
            // To handle eventType being a String instead of enum in DTO, let's keep it in action or remarks for now 
            // Or I will modify AuditResponseDTO slightly to allow String eventType.
            dto.setEventTypeStr(eventTypeStr);
            
            dto.setSourceTable((String) row[1]); // Let's add this to DTO

            dtoList.add(dto);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", dtoList);
        response.put("page", page);
        response.put("size", size);
        response.put("totalElements", totalElements);
        response.put("totalPages", totalPages);

        return response;
    }
}
