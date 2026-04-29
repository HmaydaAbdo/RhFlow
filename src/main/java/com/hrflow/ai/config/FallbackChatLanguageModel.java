package com.hrflow.ai.config;

import com.hrflow.ai.exception.AllProvidersExhaustedException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FallbackChatLanguageModel implements ChatModel {

    private static final Logger log = LoggerFactory.getLogger(FallbackChatLanguageModel.class);

    private final List<ChatModel> providers;
    private final List<String> names;

    public FallbackChatLanguageModel(List<ChatModel> providers, List<String> names) {
        if (providers == null || providers.isEmpty()) {
            throw new IllegalArgumentException("At least one ChatModel provider is required");
        }
        this.providers = List.copyOf(providers);
        this.names = List.copyOf(names);
    }

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        Exception lastCause = null;

        for (int i = 0; i < providers.size(); i++) {
            String providerName = names.get(i);
            try {
                log.debug("AI call → provider '{}' (index {})", providerName, i);
                ChatResponse response = providers.get(i).chat(chatRequest);
                if (i > 0) {
                    log.info("AI call succeeded on fallback provider '{}'", providerName);
                }
                return response;

            } catch (Exception ex) {
                lastCause = ex;
                log.warn("Provider '{}' failed (index {}) — trying next. Reason: {}",
                        providerName, i, rootMessage(ex));
            }
        }

        throw new AllProvidersExhaustedException(
                "All " + providers.size() + " AI provider(s) failed. Last error: " + rootMessage(lastCause),
                lastCause);
    }

    private static String rootMessage(Throwable ex) {
        if (ex == null) return "unknown";
        Throwable cause = ex;
        while (cause.getCause() != null) cause = cause.getCause();
        String msg = cause.getMessage();
        return (msg != null && !msg.isBlank()) ? msg : ex.getClass().getSimpleName();
    }
}