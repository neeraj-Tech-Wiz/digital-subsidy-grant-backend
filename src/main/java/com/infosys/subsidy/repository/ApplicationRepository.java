package com.infosys.subsidy.repository;

import com.infosys.subsidy.entity.Application;
import com.infosys.subsidy.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

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
}