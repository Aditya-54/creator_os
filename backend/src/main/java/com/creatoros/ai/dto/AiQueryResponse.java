package com.creatoros.ai.dto;

import com.creatoros.analytics.dto.OverviewResponse;

public record AiQueryResponse(
        String question,
        boolean aiGenerated,
        String provider,
        String answer,
        OverviewResponse fallbackOverview,
        String limitations
) {
    public static AiQueryResponse aiAnswer(String question, String provider, String answer) {
        return new AiQueryResponse(question, true, provider, answer, null,
                "This answer is grounded in CreatorOS's stored analytics via tool calls, but cannot account for "
                        + "external factors the platform doesn't track (algorithm changes, paid promotion, press, etc.).");
    }

    public static AiQueryResponse notConfigured(String question, OverviewResponse overview) {
        return new AiQueryResponse(question, false, "none",
                "No AI provider is configured, so CreatorOS can't answer this in natural language yet. "
                        + "Set OPENAI_API_KEY or ANTHROPIC_API_KEY (and creatoros.ai.provider) to enable the AI analyst. "
                        + "Showing your current content overview instead.",
                overview, null);
    }

    public static AiQueryResponse providerError(String question, String provider, OverviewResponse overview) {
        return new AiQueryResponse(question, false, provider,
                "The " + provider + " AI provider is configured but the request to it failed (network issue, "
                        + "invalid/expired key, or rate limiting). Showing your current content overview instead.",
                overview, null);
    }
}
