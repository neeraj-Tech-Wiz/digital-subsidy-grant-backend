package com.infosys.subsidy.service;

import com.infosys.subsidy.dto.BeneficiaryRequest;
import com.infosys.subsidy.entity.Beneficiary;
import com.infosys.subsidy.exception.DuplicateAadhaarException;
import com.infosys.subsidy.exception.DuplicateMobileException;
import com.infosys.subsidy.repository.BeneficiaryRepository;

import org.springframework.stereotype.Service;

@Service
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;

    public BeneficiaryService(
            BeneficiaryRepository beneficiaryRepository) {

        this.beneficiaryRepository = beneficiaryRepository;
    }


    public Beneficiary registerBeneficiary(
            BeneficiaryRequest request) {

        // Check duplicate Aadhaar number
        if (beneficiaryRepository.existsByAadhaarNumber(
                request.getAadhaarNumber())) {

            throw new DuplicateAadhaarException(
                    "Aadhaar number is already registered"
            );
        }


        // Check duplicate mobile number
        if (beneficiaryRepository.existsByMobileNumber(
                request.getMobileNumber())) {

            throw new DuplicateMobileException(
                    "Mobile number is already registered"
            );
        }


        // Create Beneficiary entity
        Beneficiary beneficiary = new Beneficiary();

        beneficiary.setName(request.getName());
        beneficiary.setFatherName(request.getFatherName());
        beneficiary.setGender(request.getGender());
        beneficiary.setAge(request.getAge());
        beneficiary.setEmail(request.getEmail());
        beneficiary.setMobileNumber(request.getMobileNumber());
        beneficiary.setAadhaarNumber(request.getAadhaarNumber());
        beneficiary.setAddress(request.getAddress());


        // Save to PostgreSQL
        return beneficiaryRepository.save(beneficiary);
    }
}