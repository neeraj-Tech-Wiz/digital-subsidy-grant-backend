package com.infosys.subsidy.repository;

import com.infosys.subsidy.entity.OtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, String> {
    Optional<OtpChallenge> findById(String id);
}
