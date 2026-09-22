package com.creatoros.ai;

import com.creatoros.ai.dto.AiQueryRequest;
import com.creatoros.ai.dto.AiQueryResponse;
import com.creatoros.ai.tools.AnalyticsTools;
import com.creatoros.analytics.AnalyticsService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Implements section 20's flow: question -> (LLM decides which analytics
 * tools to call) -> structured data -> evidence-grounded answer. The LLM
 * never touches the database; it can only call the methods on
 * {@link AnalyticsTools}, each of which is itself backed by a tested
 * analytics service. When no provider is configured, falls back to the
 * user's overview rather than erroring (section 5).
 */
@Service
public class AiAnalystService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalystService.class);

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are the CreatorOS AI Analyst, embedded in a cross-platform content analytics product.
            The current user's id is: %s
            Whenever a tool requires a userId argument, use exactly this value. Never invent or guess a userId.

            Answer the user's question by calling the available analytics tools to gather real evidence -
            do not fabricate metrics. Structure your answer with:
            1) a direct conclusion,
            2) the specific evidence/numbers that support it (cite actual figures from tool results),
            3) relevant comparisons if useful,
            4) explicit limitations or uncertainty when the data doesn't fully support a strong claim.

            Be precise about the difference between an observation ("views increased 5x"), a correlation
            ("this coincided with a rise in shares"), and an interpretation ("consistent with wider distribution").
            Never claim you have proven what caused a change in performance - the data can show correlation,
            not causation. If the tools return no relevant data, say so plainly instead of speculating.
            Keep the answer concise and skip generic motivational advice.
            """;

    private final AiProvider aiProvider;
    private final AnalyticsTools analyticsTools;
    private final AnalyticsService analyticsService;

    public AiAnalystService(AiProvider aiProvider, AnalyticsTools analyticsTools, AnalyticsService analyticsService) {
        this.aiProvider = aiProvider;
        this.analyticsTools = analyticsTools;
        this.analyticsService = analyticsService;
    }

    public AiQueryResponse query(UUID userId, AiQueryRequest request) {
        if (!aiProvider.isConfigured()) {
            return AiQueryResponse.notConfigured(request.question(), analyticsService.overview(userId));
        }
        try {
            String systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted(userId);
            String answer = aiProvider.answer(systemPrompt, request.question(), List.of(analyticsTools));
            return AiQueryResponse.aiAnswer(request.question(), aiProvider.name(), answer);
        } catch (Exception e) {
            log.warn("AI provider '{}' failed to answer query, falling back to overview: {}", aiProvider.name(), e.getMessage());
            return AiQueryResponse.providerError(request.question(), aiProvider.name(), analyticsService.overview(userId));
        }
    }
}
