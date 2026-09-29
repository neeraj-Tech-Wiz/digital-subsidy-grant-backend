package com.infosys.subsidy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.annotation.PostConstruct;

@EnableScheduling
@SpringBootApplication
public class DigitalSubsidyGrantPlatformApplication {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void fixConstraints() {
        try {
            jdbcTemplate.execute("ALTER TABLE disbursement_milestones DROP CONSTRAINT IF EXISTS disbursement_milestones_compliance_type_check CASCADE");
        } catch (Exception e) {}
        try {
            jdbcTemplate.execute("ALTER TABLE disbursement_milestones DROP CONSTRAINT disbursement_milestones_compliance_type_check");
        } catch (Exception e) {}
    }

	public static void main(String[] args) {
		SpringApplication.run(DigitalSubsidyGrantPlatformApplication.class, args);
	}

}
