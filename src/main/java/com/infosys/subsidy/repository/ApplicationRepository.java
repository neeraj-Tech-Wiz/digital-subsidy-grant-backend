package com.infosys.subsidy.repository;

import com.infosys.subsidy.entity.Application;
import com.infosys.subsidy.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ApplicationRepository
        extends JpaRepository<Application, Long> {

    List<Application> findByBeneficiaryId(Long beneficiaryId);

    List<Application> findBySchemeId(Long schemeId);

    List<Application> findByStatusAndVerificationDueDateBefore(
            ApplicationStatus status,
            LocalDateTime dateTime
    );

    List<Application> findByCurrentVerificationLevelAndStatus(
            com.infosys.subsidy.enums.VerificationLevel level,
            ApplicationStatus status
    );

    List<Application> findByStatus(ApplicationStatus status);

    List<Application> findByStatusIn(List<ApplicationStatus> statuses);

    java.util.Optional<Application> findFirstByBeneficiaryIdAndSchemeIdAndStatusOrderByRejectedAtDesc(Long beneficiaryId, Long schemeId, ApplicationStatus status);

    // ---- Aggregate helpers used by the dashboard stats endpoint ----

    /** Count applications whose status is in the provided list — one DB round-trip. */
    @Query("SELECT COUNT(a) FROM Application a WHERE a.status IN :statuses")
    long countByStatusIn(@Param("statuses") List<ApplicationStatus> statuses);

    /** Count applications with exactly one status — one DB round-trip. */
    @Query("SELECT COUNT(a) FROM Application a WHERE a.status = :status")
    long countByStatus(@Param("status") ApplicationStatus status);

    /**
     * Returns rows of [schemeId (Long), count (Long)] grouped by scheme.
     * Replaces the per-application N+1 schemeRepository.findById() loop.
     */
    @Query("SELECT a.schemeId, COUNT(a) FROM Application a GROUP BY a.schemeId")
    List<Object[]> countGroupedBySchemeId();

    /**
     * Returns rows of [status (ApplicationStatus), count (Long)] grouped by status.
     * Replaces applicationRepository.findAll() for status distribution analytics.
     */
    @Query("SELECT a.status, COUNT(a) FROM Application a GROUP BY a.status")
    List<Object[]> countGroupedByStatus();
}