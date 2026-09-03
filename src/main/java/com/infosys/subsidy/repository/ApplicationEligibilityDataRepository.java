package com.infosys.subsidy.repository;

import com.infosys.subsidy.entity.ApplicationEligibilityData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationEligibilityDataRepository
        extends JpaRepository<ApplicationEligibilityData, Long> {

    List<ApplicationEligibilityData> findByApplicationId(
            Long applicationId
    );
}