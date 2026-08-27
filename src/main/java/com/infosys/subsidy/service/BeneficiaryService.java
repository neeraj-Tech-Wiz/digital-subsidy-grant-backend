package com.infosys.subsidy.service;

import com.infosys.subsidy.dto.BeneficiaryRequest;
import com.infosys.subsidy.entity.Beneficiary;
import com.infosys.subsidy.repository.BeneficiaryRepository;
import org.springframework.stereotype.Service;

@Service
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;

    public BeneficiaryService(BeneficiaryRepository beneficiaryRepository) {
        this.beneficiaryRepository = beneficiaryRepository;
    }

    public Beneficiary registerBeneficiary(BeneficiaryRequest request) {

        if (beneficiaryRepository.existsByAadhaarNumber(request.getAadhaarNumber())) {
            throw new RuntimeException("Aadhaar number is already registered");
        }

        if (beneficiaryRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new RuntimeException("Mobile number is already registered");
        }

        Beneficiary beneficiary = new Beneficiary();

        beneficiary.setName(request.getName());
        beneficiary.setAge(request.getAge());
        beneficiary.setAadhaarNumber(request.getAadhaarNumber());
        beneficiary.setMobileNumber(request.getMobileNumber());
        beneficiary.setEmail(request.getEmail());
        beneficiary.setGovernmentScheme(request.getGovernmentScheme());

        return beneficiaryRepository.save(beneficiary);
    }
}