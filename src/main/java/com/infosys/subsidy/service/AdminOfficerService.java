package com.infosys.subsidy.service;

import com.infosys.subsidy.dto.CreateOfficerRequest;
import com.infosys.subsidy.dto.OfficerResponse;
import com.infosys.subsidy.entity.User;
import com.infosys.subsidy.enums.UserRole;
import com.infosys.subsidy.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminOfficerService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminOfficerService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public OfficerResponse createOfficer(CreateOfficerRequest request) {
        if (request.getRole() == UserRole.ADMIN || request.getRole() == UserRole.BENEFICIARY) {
            throw new RuntimeException("Can only create valid Officer roles, not Admin or Beneficiary");
        }

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("An account with this email already exists");
        }

        User officer = new User();
        officer.setName(request.getName().trim());
        officer.setEmail(email);
        officer.setPassword(passwordEncoder.encode(request.getPassword()));
        officer.setRole(request.getRole());
        officer.setActive(true);

        return new OfficerResponse(userRepository.save(officer));
    }

    public List<OfficerResponse> getAllOfficers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() != UserRole.ADMIN && u.getRole() != UserRole.BENEFICIARY)
                .map(OfficerResponse::new)
                .collect(Collectors.toList());
    }

    public OfficerResponse getOfficerById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Officer not found with ID: " + id));
        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.BENEFICIARY) {
            throw new RuntimeException("User found is not an Officer");
        }
        return new OfficerResponse(user);
    }

    @Transactional
    public OfficerResponse activateOfficer(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Officer not found with ID: " + id));
        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.BENEFICIARY) {
            throw new RuntimeException("User found is not an Officer");
        }
        user.setActive(true);
        return new OfficerResponse(userRepository.save(user));
    }

    @Transactional
    public OfficerResponse deactivateOfficer(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Officer not found with ID: " + id));
        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.BENEFICIARY) {
            throw new RuntimeException("User found is not an Officer");
        }
        user.setActive(false);
        return new OfficerResponse(userRepository.save(user));
    }
}
