CREATE TABLE brands (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(128) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_brands_name ON brands (lower(name));

CREATE TABLE content_brand_mentions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_id      UUID NOT NULL REFERENCES content (id) ON DELETE CASCADE,
    brand_id        UUID NOT NULL REFERENCES brands (id) ON DELETE CASCADE,
    mention_count   INTEGER NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_content_brand_mentions ON content_brand_mentions (content_id, brand_id);
CREATE INDEX ix_content_brand_mentions_brand ON content_brand_mentions (brand_id);
