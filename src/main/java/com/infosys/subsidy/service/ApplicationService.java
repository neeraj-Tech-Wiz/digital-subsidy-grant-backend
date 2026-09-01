package com.infosys.subsidy.service;

import com.infosys.subsidy.dto.ApplicationRequest;
import com.infosys.subsidy.entity.Application;
import com.infosys.subsidy.entity.ApplicationStatus;
import com.infosys.subsidy.repository.ApplicationRepository;
import com.infosys.subsidy.repository.BeneficiaryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final BeneficiaryRepository beneficiaryRepository;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            BeneficiaryRepository beneficiaryRepository) {

        this.applicationRepository = applicationRepository;
        this.beneficiaryRepository = beneficiaryRepository;
    }

    public Application createApplication(
            ApplicationRequest request) {

        beneficiaryRepository.findById(request.getBeneficiaryId())
                .orElseThrow(() ->
                        new RuntimeException("Beneficiary not found"));

        Application application = new Application();

        application.setBeneficiaryId(
                request.getBeneficiaryId());

        application.setSchemeId(
                request.getSchemeId());

        application.setApplicationDate(
                LocalDateTime.now());

        application.setStatus(
                ApplicationStatus.SUBMITTED);

        application.setEligibilityScore(null);

        return applicationRepository.save(application);
    }
}
