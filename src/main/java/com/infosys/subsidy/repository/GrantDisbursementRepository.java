package com.infosys.subsidy.repository;

import com.infosys.subsidy.entity.GrantDisbursement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GrantDisbursementRepository extends JpaRepository<GrantDisbursement, Long> {

    boolean existsByApplicationId(Long applicationId);

    Optional<GrantDisbursement> findByApplicationId(Long applicationId);

    List<GrantDisbursement> findBySchemeIdOrderByDisbursedAtDesc(Long schemeId);

    List<GrantDisbursement> findAllByOrderByDisbursedAtDesc();
}
