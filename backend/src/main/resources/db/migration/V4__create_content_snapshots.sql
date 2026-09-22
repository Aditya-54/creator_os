-- Append-only historical performance observations. Never updated/overwritten;
-- the analytics engine derives growth/acceleration/viral events from the series.
CREATE TABLE content_snapshots (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_id                  UUID NOT NULL REFERENCES content (id) ON DELETE CASCADE,
    captured_at                 TIMESTAMPTZ NOT NULL,
    views                       BIGINT NOT NULL DEFAULT 0,
    likes                       BIGINT NOT NULL DEFAULT 0,
    comments                    BIGINT NOT NULL DEFAULT 0,
    shares                      BIGINT NOT NULL DEFAULT 0,
    followers_attributed        BIGINT,
    watch_time_seconds          BIGINT,
    avg_view_duration_seconds   DOUBLE PRECISION,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_content_snapshots_content_captured
    ON content_snapshots (content_id, captured_at);

CREATE INDEX ix_content_snapshots_content_captured_desc
    ON content_snapshots (content_id, captured_at DESC);
