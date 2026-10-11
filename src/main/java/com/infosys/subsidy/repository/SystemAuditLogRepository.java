package com.infosys.subsidy.repository;

import com.infosys.subsidy.entity.SystemAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemAuditLogRepository extends JpaRepository<SystemAuditLog, Long>, JpaSpecificationExecutor<SystemAuditLog> {
}
