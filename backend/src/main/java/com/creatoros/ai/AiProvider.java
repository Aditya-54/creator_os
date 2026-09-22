package com.creatoros.ai;

import java.util.List;

/**
 * Abstraction over "something that can turn a question plus tool access into
 * an answer". Exactly one implementation is active at a time, selected by
 * {@link AiProviderConfig} from {@code creatoros.ai.provider}. When nothing
 * is configured, {@link NoOpAiProvider} is used and the application keeps
 * working - only natural-language AI answers become unavailable (section 5,
 * section 20).
 */
public interface AiProvider {

    boolean isConfigured();

    String name();

    /**
     * @param toolObjects Spring AI tool-annotated objects (e.g. {@code AnalyticsTools})
     *                    the model may call to ground its answer in real data.
     */
    String answer(String systemPrompt, String userQuestion, List<Object> toolObjects);
}
