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

## Current Modules

### 1. Beneficiary Registration
- Terminal-based beneficiary registration
- Name, Age, Aadhaar, Mobile, Email validation
- Duplicate Aadhaar & Mobile checking
- PostgreSQL persistence and retrieval verification

### 2. Scheme Master Data Module (`feature/scheme-master`)
- **Scheme Entity & Lifecycle**: Full master data management (Draft, Active, Inactive, Closed) with budget tracking and remaining balance calculations.
- **Configurable Eligibility Criteria**: 1-to-N relationship between Scheme and Eligibility Criteria supporting numeric, boolean, text, and enum rule operators (`==`, `<`, `<=`, `>`, `>=`, `IN`).
- **Spring Data JPA Repositories**: Complete persistence and custom query filtering for schemes and criteria.
- **Service Layer & Business Logic**: Strict validations, duplicate name/code prevention, cascade management, and budget disbursement protection.
- **Transparent Eligibility Scoring & Routing Engine**: Automatic rule evaluation, criterion-wise score breakdown, and routing into decision buckets (High Priority Direct Approval, Manual Review, Additional Info, Ineligible).
- **RESTful API Endpoints**: Full CRUD endpoints at `/api/schemes` for schemes, criteria, status management, disbursement, and simulation.
- **Sample Data Preloader**: Automatic initialization of default Indian Government schemes (PM Kisan, PMEGP, PM Surya Ghar, PMMVY).

## REST API Documentation (Scheme Master)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/schemes` | Create new scheme |
| `GET` | `/api/schemes` | List schemes (supports `keyword`, `category`, `region`, `status` filters) |
| `GET` | `/api/schemes/active` | List all active schemes |
| `GET` | `/api/schemes/{id}` | Get scheme by ID |
| `GET` | `/api/schemes/code/{code}` | Get scheme by scheme code |
| `PUT` | `/api/schemes/{id}` | Update scheme details |
| `PATCH` | `/api/schemes/{id}/status` | Change scheme status |
| `DELETE` | `/api/schemes/{id}` | Delete scheme and associated criteria |
| `GET` | `/api/schemes/{id}/criteria` | Get all criteria for a scheme |
| `POST` | `/api/schemes/{id}/criteria` | Add eligibility criterion |
| `PUT` | `/api/schemes/{id}/criteria/{critId}` | Update criterion |
| `DELETE` | `/api/schemes/{id}/criteria/{critId}` | Remove criterion |
| `PATCH` | `/api/schemes/{id}/criteria/{critId}/toggle` | Toggle active status |
| `POST` | `/api/schemes/{id}/disburse` | Record fund disbursement |
| `POST` | `/api/schemes/{id}/evaluate` | Simulate applicant eligibility evaluation |

## Database

Database name: `subsidy_grant_db`

## Local Environment Variables

The following environment variables are required:

```properties
DB_URL=jdbc:postgresql://localhost:5432/subsidy_grant_db
DB_USERNAME=postgres
DB_PASSWORD=<your-local-password>
```

Do not commit database passwords or other secrets.

## Running the Project

Run `DigitalSubsidyGrantPlatformApplication` or use Maven:

```bash
mvn clean spring-boot:run
```

