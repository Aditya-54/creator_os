CREATE TABLE content (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    social_account_id   UUID NOT NULL REFERENCES social_accounts (id) ON DELETE CASCADE,
    platform            VARCHAR(32) NOT NULL,
    platform_content_id VARCHAR(255) NOT NULL,
    title               VARCHAR(500),
    description         TEXT,
    content_type        VARCHAR(32) NOT NULL,
    published_at        TIMESTAMPTZ,
    duration_seconds     INTEGER,
    language            VARCHAR(16),
    country              VARCHAR(8),
    category             VARCHAR(64),
    thumbnail_url        TEXT,
    is_demo             BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    version             BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_content_platform_content_id
    ON content (platform, platform_content_id);

CREATE INDEX ix_content_social_account ON content (social_account_id);
CREATE INDEX ix_content_published_at ON content (published_at);
CREATE INDEX ix_content_platform_country ON content (platform, country);
