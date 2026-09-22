package com.creatoros.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Selects the active {@link AiProvider} from {@code creatoros.ai.provider},
 * falling back to {@link NoOpAiProvider} whenever the chosen provider isn't
 * actually usable - no api-key set, or (defensively) the underlying Spring AI
 * autoconfigured bean failed to build for any reason. This method is the
 * single place responsible for the "AI is entirely optional" guarantee: a
 * misconfigured or absent AI provider must never prevent the application
 * itself from starting (section 5).
 */
@Configuration
public class AiProviderConfig {

    private static final Logger log = LoggerFactory.getLogger(AiProviderConfig.class);

    @Bean
    public AiProvider aiProvider(@Value("${creatoros.ai.provider:none}") String selectedProvider,
                                  @Value("${spring.ai.openai.api-key:}") String openAiApiKey,
                                  @Value("${spring.ai.anthropic.api-key:}") String anthropicApiKey,
                                  ObjectProvider<OpenAiChatModel> openAiChatModel,
                                  ObjectProvider<AnthropicChatModel> anthropicChatModel) {
        if ("openai".equalsIgnoreCase(selectedProvider)) {
            AiProvider provider = tryBuild("openai", StringUtils.hasText(openAiApiKey),
                    () -> new SpringAiChatProvider(openAiChatModel.getObject(), "openai"));
            if (provider != null) {
                return provider;
            }
        } else if ("anthropic".equalsIgnoreCase(selectedProvider)) {
            AiProvider provider = tryBuild("anthropic", StringUtils.hasText(anthropicApiKey),
                    () -> new SpringAiChatProvider(anthropicChatModel.getObject(), "anthropic"));
            if (provider != null) {
                return provider;
            }
        } else if (!"none".equalsIgnoreCase(selectedProvider)) {
            log.warn("Unknown creatoros.ai.provider '{}'; falling back to no AI provider", selectedProvider);
        }

        log.info("AI provider not configured (creatoros.ai.provider={}). AI features will report as unavailable; "
                + "normal analytics are unaffected.", selectedProvider);
        return new NoOpAiProvider();
    }

    private AiProvider tryBuild(String name, boolean hasApiKey, java.util.function.Supplier<AiProvider> factory) {
        if (!hasApiKey) {
            log.warn("creatoros.ai.provider=={} but no api-key is set; falling back to no AI provider", name);
            return null;
        }
        try {
            return factory.get();
        } catch (Exception e) {
            log.warn("Failed to initialize {} AI provider ({}); falling back to no AI provider", name, e.getMessage());
            return null;
        }
    }
}
