package com.infosys.subsidy.repository;

import com.infosys.subsidy.entity.DisbursementMilestone;
import com.infosys.subsidy.enums.MilestoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DisbursementMilestoneRepository extends JpaRepository<DisbursementMilestone, Long> {
    List<DisbursementMilestone> findByPlanIdOrderByMilestoneNumberAsc(Long planId);
    List<DisbursementMilestone> findByStatusAndDueDateBefore(MilestoneStatus status, LocalDateTime date);
}
