package com.infosys.subsidy.repository;

import com.infosys.subsidy.entity.FundRelease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FundReleaseRepository extends JpaRepository<FundRelease, Long> {
    List<FundRelease> findByApplicationIdOrderByReleasedAtAsc(Long applicationId);
    boolean existsByMilestoneId(Long milestoneId);
}
