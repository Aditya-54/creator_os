CREATE TABLE viral_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_id      UUID NOT NULL REFERENCES content (id) ON DELETE CASCADE,
    detected_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    start_time      TIMESTAMPTZ NOT NULL,
    end_time        TIMESTAMPTZ,
    peak_growth     DOUBLE PRECISION NOT NULL,
    baseline_growth DOUBLE PRECISION NOT NULL,
    multiplier      DOUBLE PRECISION NOT NULL,
    confidence      VARCHAR(16) NOT NULL,
    explanation     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_viral_events_content ON viral_events (content_id, start_time DESC);
