CREATE TABLE topics (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(128) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_topics_name ON topics (lower(name));

CREATE TABLE content_topics (
    content_id  UUID NOT NULL REFERENCES content (id) ON DELETE CASCADE,
    topic_id    UUID NOT NULL REFERENCES topics (id) ON DELETE CASCADE,
    source      VARCHAR(16) NOT NULL DEFAULT 'RULE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (content_id, topic_id)
);

CREATE INDEX ix_content_topics_topic ON content_topics (topic_id);
