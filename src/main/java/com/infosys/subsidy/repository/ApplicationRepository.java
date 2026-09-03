package com.infosys.subsidy.repository;

import com.infosys.subsidy.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository
        extends JpaRepository<Application, Long> {

    List<Application> findByBeneficiaryId(Long beneficiaryId);

    List<Application> findBySchemeId(Long schemeId);
}