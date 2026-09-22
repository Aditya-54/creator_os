package com.creatoros.topic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TopicExtractionServiceTest {

    private final TopicExtractionService service = new TopicExtractionService();

    @Test
    void matchesWholeWordKeyword() {
        assertThat(service.extract("I built an AI agent that codes itself", "")).contains("ai");
    }

    @Test
    void doesNotFalsePositiveOnSubstringOfKeyword() {
        // "training" contains the substring "ai" - must not be tagged as the AI topic.
        assertThat(service.extract("Training vlog: matchday prep", "Behind the scenes before kickoff."))
                .doesNotContain("ai")
                .contains("football");
    }

    @Test
    void matchesMultipleTopicsFromOneText() {
        assertThat(service.extract("GPT vs Claude for football analysis", ""))
                .contains("ai", "football");
    }

    @Test
    void returnsEmptySetWhenNoKeywordsMatch() {
        assertThat(service.extract("A completely unrelated caption", "")).isEmpty();
    }

    @Test
    void isCaseInsensitive() {
        assertThat(service.extract("GAMING HIGHLIGHTS", "")).contains("gaming");
    }
}
