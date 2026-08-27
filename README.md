# Digital Subsidy & Grant Administration Platform

Backend system for managing government subsidy and grant applications,
eligibility verification, approvals, disbursement and fund utilization.

## Technology Stack

- Java 25
- Spring Boot
- Spring Data JPA
- Hibernate
- PostgreSQL 18
- Maven
- Git/GitHub

## Current Module

### Beneficiary Registration

Currently implemented:

- Terminal-based beneficiary registration
- Name validation
- Age validation
- Aadhaar validation
- Mobile validation
- Email validation
- Government scheme input
- Duplicate Aadhaar checking
- Duplicate mobile checking
- PostgreSQL persistence
- Database retrieval verification

## Database

Database name:

subsidy_grant_db

## Local Environment Variables

The following environment variables are required:

DB_URL=jdbc:postgresql://localhost:5432/subsidy_grant_db
DB_USERNAME=postgres
DB_PASSWORD=<your-local-password>

Do not commit database passwords or other secrets.

## Running the Project

Run:

DigitalSubsidyGrantPlatformApplication

The current version performs beneficiary registration through
the terminal.
