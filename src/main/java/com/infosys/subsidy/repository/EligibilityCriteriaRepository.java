package com.infosys.subsidy.repository;

import com.infosys.subsidy.entity.EligibilityCriteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EligibilityCriteriaRepository extends JpaRepository<EligibilityCriteria, Long> {

    List<EligibilityCriteria> findBySchemeId(Long schemeId);

    List<EligibilityCriteria> findBySchemeIdAndActiveTrue(Long schemeId);

    @Modifying
    @Query("DELETE FROM EligibilityCriteria c WHERE c.scheme.id = :schemeId")
    void deleteBySchemeId(@Param("schemeId") Long schemeId);
}
