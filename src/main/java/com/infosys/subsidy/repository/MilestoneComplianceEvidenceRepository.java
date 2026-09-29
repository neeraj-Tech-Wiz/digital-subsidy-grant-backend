package com.infosys.subsidy.repository;

import com.infosys.subsidy.entity.MilestoneComplianceEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MilestoneComplianceEvidenceRepository extends JpaRepository<MilestoneComplianceEvidence, Long> {
    List<MilestoneComplianceEvidence> findByMilestoneId(Long milestoneId);
    List<MilestoneComplianceEvidence> findByApplicationId(Long applicationId);
    long countByMilestoneIdAndStatus(Long milestoneId, String status);
}
