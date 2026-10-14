package com.infosys.subsidy.repository;

import com.infosys.subsidy.entity.DisbursementMilestone;
import com.infosys.subsidy.enums.MilestoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DisbursementMilestoneRepository extends JpaRepository<DisbursementMilestone, Long> {
    List<DisbursementMilestone> findByPlanIdOrderByMilestoneNumberAsc(Long planId);
    List<DisbursementMilestone> findByStatusAndDueDateBefore(MilestoneStatus status, LocalDateTime date);

    /** Count milestones with a given status — one lightweight aggregate query. */
    @Query("SELECT COUNT(m) FROM DisbursementMilestone m WHERE m.status = :status")
    long countByStatus(@Param("status") MilestoneStatus status);
}
