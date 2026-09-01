package com.infosys.subsidy.repository;

import com.infosys.subsidy.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository
        extends JpaRepository<Application, Long> {
}