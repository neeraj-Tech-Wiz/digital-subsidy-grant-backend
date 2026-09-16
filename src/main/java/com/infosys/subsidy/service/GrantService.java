package com.infosys.subsidy.service;

import com.infosys.subsidy.entity.*;
import com.infosys.subsidy.enums.ApplicationStatus;
import com.infosys.subsidy.enums.UserRole;
import com.infosys.subsidy.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class GrantService {

    private final GrantDisbursementRepository grantDisbursementRepository;
    private final ApplicationRepository applicationRepository;
    private final SchemeRepository schemeRepository;
    private final UserRepository userRepository;
    private final BeneficiaryRepository beneficiaryRepository;

    public GrantService(GrantDisbursementRepository grantDisbursementRepository,
                        ApplicationRepository applicationRepository,
                        SchemeRepository schemeRepository,
                        UserRepository userRepository,
                        BeneficiaryRepository beneficiaryRepository) {
        this.grantDisbursementRepository = grantDisbursementRepository;
        this.applicationRepository = applicationRepository;
        this.schemeRepository = schemeRepository;
        this.userRepository = userRepository;
        this.beneficiaryRepository = beneficiaryRepository;
    }

    @Transactional
    public GrantDisbursement disburseGrant(Long applicationId, String officerEmail) {
        User officer = userRepository.findByEmail(officerEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Officer not found"));

        if (officer.getRole() != UserRole.GRANT_OFFICER && officer.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Grant Officers can disburse funds.");
        }

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        if (application.getStatus() != ApplicationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application must be APPROVED to disburse funds. Current status: " + application.getStatus());
        }

        if (grantDisbursementRepository.existsByApplicationId(applicationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Funds have already been disbursed for this application.");
        }

        Scheme scheme = schemeRepository.findByIdWithLock(application.getSchemeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scheme not found"));

        BigDecimal grantAmount = scheme.getGrantAmount();
        BigDecimal remainingBudget = scheme.getRemainingBudget();

        if (remainingBudget.compareTo(grantAmount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient scheme budget. Available: ₹" + remainingBudget + ", Required: ₹" + grantAmount);
        }

        application.setStatus(ApplicationStatus.GRANT_DISBURSED);
        applicationRepository.save(application);

        scheme.setDisbursedAmount(scheme.getDisbursedAmount().add(grantAmount));
        schemeRepository.save(scheme);

        GrantDisbursement disbursement = new GrantDisbursement();
        disbursement.setApplicationId(applicationId);
        disbursement.setSchemeId(scheme.getId());
        disbursement.setBeneficiaryId(application.getBeneficiaryId());
        disbursement.setGrantAmount(grantAmount);
        disbursement.setDisbursedBy(officer.getId());
        disbursement.setDisbursedAt(LocalDateTime.now());
        disbursement.setTransactionReference("TRX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        return grantDisbursementRepository.save(disbursement);
    }
    
    @Transactional(readOnly = true)
    public java.util.List<com.infosys.subsidy.dto.OfficerQueueItemDTO> getPendingGrants() {
        java.util.List<Application> pending = applicationRepository.findByStatus(ApplicationStatus.APPROVED);
        return pending.stream().map(app -> {
            String beneficiaryName = beneficiaryRepository.findById(app.getBeneficiaryId())
                    .map(Beneficiary::getName).orElse("Unknown Beneficiary");
            String schemeName = schemeRepository.findById(app.getSchemeId())
                    .map(Scheme::getSchemeName).orElse("Unknown Scheme");
            
            return new com.infosys.subsidy.dto.OfficerQueueItemDTO(
                    app.getId(), app.getApplicationDate(), app.getStatus(), 
                    app.getCurrentVerificationLevel(), app.getVerificationRoute(), 
                    app.getEligibilityScore(), beneficiaryName, schemeName, 
                    app.getVerificationAssignedAt(), app.getVerificationDueDate(), app.getRemarks());
        }).collect(java.util.stream.Collectors.toList());
    }

    @Transactional(readOnly = true)
    public com.infosys.subsidy.dto.ApplicationDetailDTO getGrantApplicationDetails(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        
        Beneficiary beneficiary = beneficiaryRepository.findById(application.getBeneficiaryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Beneficiary not found"));
                
        Scheme scheme = schemeRepository.findById(application.getSchemeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scheme not found"));

        com.infosys.subsidy.dto.ApplicationDetailDTO dto = new com.infosys.subsidy.dto.ApplicationDetailDTO();
        dto.setApplicationId(application.getId());
        dto.setStatus(application.getStatus());
        dto.setCurrentVerificationLevel(application.getCurrentVerificationLevel());
        dto.setVerificationRoute(application.getVerificationRoute());
        dto.setEligibilityScore(application.getEligibilityScore());
        dto.setApplicationDate(application.getApplicationDate());
        
        dto.setBeneficiaryName(beneficiary.getName());
        dto.setMobileNumber(beneficiary.getMobileNumber());
        dto.setEmail(beneficiary.getEmail());
        String maskedAadhaar = "XXXX-XXXX-" + beneficiary.getAadhaarNumber().substring(Math.max(0, beneficiary.getAadhaarNumber().length() - 4));
        dto.setMaskedAadhaar(maskedAadhaar);
        
        dto.setSchemeName(scheme.getSchemeName());
        dto.setSchemeCode(scheme.getSchemeCode());
        
        return dto;
    }
}
