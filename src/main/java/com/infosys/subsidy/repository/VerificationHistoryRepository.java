package com.infosys.subsidy.repository;

import com.infosys.subsidy.entity.VerificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VerificationHistoryRepository extends JpaRepository<VerificationHistory, Long> {
    List<VerificationHistory> findByApplicationIdOrderByActionTimestampDesc(Long applicationId);
    List<VerificationHistory> findByApplicationIdOrderByActionTimestampAsc(Long applicationId);
}
