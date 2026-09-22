CREATE TABLE social_accounts (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    platform                VARCHAR(32) NOT NULL,
    platform_account_id     VARCHAR(255) NOT NULL,
    username                VARCHAR(255),
    access_token_encrypted  TEXT,
    refresh_token_encrypted TEXT,
    token_expiry            TIMESTAMPTZ,
    connected_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_synced_at          TIMESTAMPTZ,
    status                  VARCHAR(32) NOT NULL DEFAULT 'CONNECTED',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                 BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_social_accounts_platform_account
    ON social_accounts (platform, platform_account_id);

CREATE INDEX ix_social_accounts_user ON social_accounts (user_id);
CREATE INDEX ix_social_accounts_status ON social_accounts (status);
