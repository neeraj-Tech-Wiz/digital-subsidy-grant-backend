package com.infosys.subsidy.service;

import com.infosys.subsidy.dto.BeneficiaryProfileResponse;
import com.infosys.subsidy.dto.BeneficiaryRequest;
import com.infosys.subsidy.entity.Beneficiary;
import com.infosys.subsidy.entity.User;
import com.infosys.subsidy.enums.UserRole;
import com.infosys.subsidy.exception.DuplicateAadhaarException;
import com.infosys.subsidy.exception.DuplicateMobileException;
import com.infosys.subsidy.repository.BeneficiaryRepository;
import com.infosys.subsidy.repository.UserRepository;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;

    public BeneficiaryService(
            BeneficiaryRepository beneficiaryRepository,
            UserRepository userRepository) {

        this.beneficiaryRepository = beneficiaryRepository;
        this.userRepository = userRepository;
    }


    // ============================================================
    // LEGACY METHOD — kept for backward compatibility with
    // ConsoleApplicationRunner and old terminal-based workflow.
    // Creates a beneficiary record WITHOUT a User link.
    // ============================================================

    public Beneficiary registerBeneficiary(BeneficiaryRequest request) {

        if (beneficiaryRepository.existsByAadhaarNumber(request.getAadhaarNumber())) {
            throw new DuplicateAadhaarException("Aadhaar number is already registered");
        }

        if (beneficiaryRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new DuplicateMobileException("Mobile number is already registered");
        }

        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setName(request.getName());
        beneficiary.setFatherName(request.getFatherName());
        beneficiary.setGender(request.getGender());
        beneficiary.setAge(request.getAge());
        beneficiary.setEmail(request.getEmail());
        beneficiary.setMobileNumber(request.getMobileNumber());
        beneficiary.setAadhaarNumber(request.getAadhaarNumber());
        beneficiary.setAddress(request.getAddress());

        return beneficiaryRepository.save(beneficiary);
    }


    // ============================================================
    // WEB FLOW — Create authenticated beneficiary profile.
    // Linked to the authenticated User via user_id.
    // The frontend must NEVER send userId — it is derived from JWT.
    // ============================================================

    public BeneficiaryProfileResponse createProfile(
            BeneficiaryRequest request,
            String authenticatedEmail) {

        // 1. Find the authenticated user
        User user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        // 2. Verify role
        if (user.getRole() != UserRole.BENEFICIARY) {
            throw new RuntimeException("Only beneficiary users can create a beneficiary profile");
        }

        // 3. Check if profile already exists
        if (beneficiaryRepository.existsByUserId(user.getId())) {
            throw new RuntimeException("Beneficiary profile already exists for this user");
        }

        // 4. Check duplicate Aadhaar
        if (beneficiaryRepository.existsByAadhaarNumber(request.getAadhaarNumber())) {
            throw new DuplicateAadhaarException("Aadhaar number is already registered");
        }

        // 5. Check duplicate mobile
        if (beneficiaryRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new DuplicateMobileException("Mobile number is already registered");
        }

        // 6. Create the beneficiary profile
        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setName(request.getName());
        beneficiary.setFatherName(request.getFatherName());
        beneficiary.setGender(request.getGender());
        beneficiary.setAge(request.getAge());

        // Email is always taken from the authenticated user's account — never from the request.
        beneficiary.setEmail(user.getEmail());

        beneficiary.setMobileNumber(request.getMobileNumber());
        beneficiary.setAadhaarNumber(request.getAadhaarNumber());
        beneficiary.setAddress(request.getAddress());

        // 7. Link to authenticated user — this is the critical step.
        beneficiary.setUser(user);

        // 8. Save
        Beneficiary saved = beneficiaryRepository.save(beneficiary);

        return toProfileResponse(saved);
    }


    // ============================================================
    // WEB FLOW — Get the logged-in beneficiary's profile.
    // Returns Optional.empty() if no profile has been created yet.
    // ============================================================

    public Optional<BeneficiaryProfileResponse> getMyProfile(String authenticatedEmail) {

        User user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        return beneficiaryRepository
                .findByUserId(user.getId())
                .map(this::toProfileResponse);
    }


    // ============================================================
    // HELPER — Map Beneficiary entity to clean DTO.
    // Masks Aadhaar number for security.
    // ============================================================

    private BeneficiaryProfileResponse toProfileResponse(Beneficiary beneficiary) {

        BeneficiaryProfileResponse response = new BeneficiaryProfileResponse();
        response.setId(beneficiary.getId());
        response.setName(beneficiary.getName());
        response.setFatherName(beneficiary.getFatherName());
        response.setGender(beneficiary.getGender());
        response.setAge(beneficiary.getAge());
        response.setEmail(beneficiary.getEmail());
        response.setMobileNumber(beneficiary.getMobileNumber());
        response.setAddress(beneficiary.getAddress());
        response.setProfileExists(true);

        // Set userId from linked user
        if (beneficiary.getUser() != null) {
            response.setUserId(beneficiary.getUser().getId());
        }

        // Mask Aadhaar: only show last 4 digits
        String aadhaar = beneficiary.getAadhaarNumber();
        if (aadhaar != null && aadhaar.length() == 12) {
            response.setAadhaarNumberMasked("XXXX-XXXX-" + aadhaar.substring(8));
        }

        return response;
    }
}