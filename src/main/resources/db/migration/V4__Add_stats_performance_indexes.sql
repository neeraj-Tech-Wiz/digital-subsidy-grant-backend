-- V4: Add performance indexes for the admin dashboard stats endpoint
--
-- Context: The /api/admin/dashboard/stats endpoint was taking ~23 seconds.
-- Analysis identified two common filter columns with no indexes:
--   1. applications.status  — used in every status aggregate and group-by query
--   2. system_audit_log.timestamp — used in every date-range filter in the audit UNION ALL queries
--
-- Both are VARCHAR/TIMESTAMP columns referenced by frequently-called admin queries.
-- Using IF NOT EXISTS ensures idempotent deployment on Render (no duplicate-index errors).

-- Index 1: application status filters (COUNT grouped by status, pending/approved KPIs)
CREATE INDEX IF NOT EXISTS idx_applications_status ON applications(status);

-- Index 2: application scheme grouping (countGroupedBySchemeId)
CREATE INDEX IF NOT EXISTS idx_applications_scheme_id ON applications(scheme_id);

-- Index 3: audit log timestamp date-range filters (activityToday, failedLogins, recentActivity)
-- DESC ordering matches the ORDER BY timestamp DESC used by the paginated query.
CREATE INDEX IF NOT EXISTS idx_system_audit_log_timestamp ON system_audit_log(timestamp DESC);

-- Index 4: verification history timestamp date-range filters and ORDER BY DESC in UNION ALL
CREATE INDEX IF NOT EXISTS idx_verification_history_action_timestamp ON verification_history(action_timestamp DESC);

-- Index 5: disbursement milestone status filters (countByStatus for NON_COMPLIANT / UNDER_REVIEW)
CREATE INDEX IF NOT EXISTS idx_disbursement_milestones_status ON disbursement_milestones(status);

