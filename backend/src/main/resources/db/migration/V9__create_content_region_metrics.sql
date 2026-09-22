-- Latest known regional performance breakdown per content item, refreshed on each
-- sync. Not a historical time series (platform APIs typically expose regional
-- breakdown as a lifetime aggregate, not per-timestamp), unlike content_snapshots.
CREATE TABLE content_region_metrics (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_id  UUID NOT NULL REFERENCES content (id) ON DELETE CASCADE,
    country     VARCHAR(8) NOT NULL,
    views       BIGINT NOT NULL DEFAULT 0,
    engagement  BIGINT NOT NULL DEFAULT 0,
    captured_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_content_region_metrics ON content_region_metrics (content_id, country);
CREATE INDEX ix_content_region_metrics_country ON content_region_metrics (country);
