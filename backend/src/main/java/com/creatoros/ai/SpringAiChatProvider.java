package com.creatoros.ai;

import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

/** Wraps any Spring AI {@link ChatModel} (OpenAI, Anthropic, ...) behind the {@link AiProvider} contract. */
public class SpringAiChatProvider implements AiProvider {

    private final ChatModel chatModel;
    private final String providerName;

    public SpringAiChatProvider(ChatModel chatModel, String providerName) {
        this.chatModel = chatModel;
        this.providerName = providerName;
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public String name() {
        return providerName;
    }

    @Override
    public String answer(String systemPrompt, String userQuestion, List<Object> toolObjects) {
        ChatClient client = ChatClient.builder(chatModel).build();
        return client.prompt()
                .system(systemPrompt)
                .user(userQuestion)
                .tools(toolObjects.toArray())
                .call()
                .content();
    }
}
