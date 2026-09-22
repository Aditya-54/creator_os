CREATE TABLE sync_jobs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    social_account_id   UUID NOT NULL REFERENCES social_accounts (id) ON DELETE CASCADE,
    status              VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    started_at          TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    attempt_count       INTEGER NOT NULL DEFAULT 0,
    error_message       TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    version             BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX ix_sync_jobs_account_created ON sync_jobs (social_account_id, created_at DESC);
CREATE INDEX ix_sync_jobs_status ON sync_jobs (status);
