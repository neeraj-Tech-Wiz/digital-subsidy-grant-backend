package com.infosys.subsidy.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("========================================");
        System.out.println("Running Database Migration Runner");
        System.out.println("Updating application_documents CHECK constraint...");
        
        try {
            // Drop old constraint
            jdbcTemplate.execute("ALTER TABLE application_documents DROP CONSTRAINT IF EXISTS application_documents_document_type_check");
            
            // Add new constraint accommodating the new enums
            String newConstraint = "ALTER TABLE application_documents ADD CONSTRAINT application_documents_document_type_check CHECK " +
                    "(document_type::text = ANY (ARRAY[" +
                    "'AADHAAR_CARD'::character varying, " +
                    "'INCOME_CERTIFICATE'::character varying, " +
                    "'MARKSHEET'::character varying, " +
                    "'BONAFIDE_CERTIFICATE'::character varying, " +
                    "'COLLEGE_ID_CARD'::character varying, " +
                    "'CASTE_CERTIFICATE'::character varying, " +
                    "'LAND_CERTIFICATE'::character varying, " +
                    "'PROPERTY_DOCUMENT'::character varying, " +
                    "'LAND_RECORD_7_12'::character varying, " +
                    "'BANK_PASSBOOK'::character varying, " +
                    "'RESIDENCE_CERTIFICATE'::character varying, " +
                    "'OTHER'::character varying]::text[]))";
                    
            jdbcTemplate.execute(newConstraint);
            System.out.println("Successfully updated application_documents_document_type_check.");
        } catch (Exception e) {
            System.err.println("Database migration failed: " + e.getMessage());
        }
        
        System.out.println("========================================");
    }
}
