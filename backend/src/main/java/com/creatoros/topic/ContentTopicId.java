package com.creatoros.topic;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ContentTopicId implements Serializable {

    private UUID content;
    private UUID topic;

    public ContentTopicId() {
    }

    public ContentTopicId(UUID content, UUID topic) {
        this.content = content;
        this.topic = topic;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContentTopicId that)) return false;
        return Objects.equals(content, that.content) && Objects.equals(topic, that.topic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, topic);
    }
}
