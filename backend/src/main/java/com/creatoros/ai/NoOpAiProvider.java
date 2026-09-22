package com.creatoros.ai;

import java.util.List;

/** Active when no AI provider is configured. Callers must check {@link #isConfigured()} first. */
public class NoOpAiProvider implements AiProvider {

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public String name() {
        return "none";
    }

    @Override
    public String answer(String systemPrompt, String userQuestion, List<Object> toolObjects) {
        throw new IllegalStateException("No AI provider is configured");
    }
}
