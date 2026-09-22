package com.creatoros.topic;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Lightweight rule-based topic extraction from title/description keywords.
 * Deliberately not LLM-based (section 12 of the spec): topic assignment must
 * work with zero AI configuration. An AI-assisted classifier could later
 * populate {@link TopicSource#AI} alongside this without changing callers.
 */
@Service
public class TopicExtractionService {

    private static final Map<String, String[]> KEYWORDS_BY_TOPIC = new LinkedHashMap<>();

    static {
        KEYWORDS_BY_TOPIC.put("football", new String[]{"football", "soccer", "derby", "goal", "match", "matchday"});
        KEYWORDS_BY_TOPIC.put("gaming", new String[]{"gaming", "game", "grandmaster", "clutch", "ranked", "ace", "esports"});
        KEYWORDS_BY_TOPIC.put("anime", new String[]{"anime", "manga", "opening", "plot twist"});
        KEYWORDS_BY_TOPIC.put("ai", new String[]{"ai", "gpt", "claude", "agent", "llm", "artificial intelligence"});
        KEYWORDS_BY_TOPIC.put("technology", new String[]{"tech", "phone", "app", "productivity", "flagship", "gadget"});
        KEYWORDS_BY_TOPIC.put("fashion", new String[]{"fashion", "style", "outfit", "blazer", "haul", "streetwear"});
        KEYWORDS_BY_TOPIC.put("finance", new String[]{"finance", "trading", "index fund", "stocks", "invest", "day trading"});
    }

    /**
     * Returns the set of topic names whose keywords appear in the given text
     * (case-insensitive, whole-word match). Word boundaries matter: a naive
     * substring check on a short keyword like "ai" would false-positive on
     * "training" or "explain".
     */
    public Set<String> extract(String... texts) {
        String haystack = String.join(" ", texts).toLowerCase();
        Set<String> matches = new LinkedHashSet<>();
        for (var entry : KEYWORDS_BY_TOPIC.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (containsWholeWord(haystack, keyword)) {
                    matches.add(entry.getKey());
                    break;
                }
            }
        }
        return matches;
    }

    private boolean containsWholeWord(String haystack, String keyword) {
        return Pattern.compile("\\b" + Pattern.quote(keyword) + "\\b").matcher(haystack).find();
    }
}
