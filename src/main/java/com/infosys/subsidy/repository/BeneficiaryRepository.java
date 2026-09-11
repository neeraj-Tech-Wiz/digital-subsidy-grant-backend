package com.infosys.subsidy.repository;

import com.infosys.subsidy.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    boolean existsByAadhaarNumber(String aadhaarNumber);

    boolean existsByMobileNumber(String mobileNumber);

    Optional<Beneficiary> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}