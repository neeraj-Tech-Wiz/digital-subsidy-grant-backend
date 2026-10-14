package com.infosys.subsidy.repository;

import com.infosys.subsidy.entity.GrantDisbursement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface GrantDisbursementRepository extends JpaRepository<GrantDisbursement, Long> {

    boolean existsByApplicationId(Long applicationId);

    Optional<GrantDisbursement> findByApplicationId(Long applicationId);

    List<GrantDisbursement> findBySchemeIdOrderByDisbursedAtDesc(Long schemeId);

    List<GrantDisbursement> findAllByOrderByDisbursedAtDesc();

    /** Sum all disbursed grant amounts — one aggregate query, zero heap allocation. */
    @Query("SELECT COALESCE(SUM(g.grantAmount), 0) FROM GrantDisbursement g")
    BigDecimal sumGrantAmount();
}
