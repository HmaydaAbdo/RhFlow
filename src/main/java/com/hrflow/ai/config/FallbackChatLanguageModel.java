package com.hrflow.ai.config;

import com.hrflow.ai.exception.AllProvidersExhaustedException;
import dev.langchain4j.data.message.ChatMessage;
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
                if (isRetriable(ex)) {
                    log.warn("Provider '{}' retriable error — trying next. Reason: {}",
                            providerName, rootMessage(ex));
                } else {
                    // 400 / 404 / 422 — content or config error, same on every provider.
                    log.error("Provider '{}' non-retriable error (bad request / wrong model) — aborting chain. Reason: {}",
                            providerName, rootMessage(ex));
                    throw ex;
                }
            }
        }

        throw new AllProvidersExhaustedException(
                "All " + providers.size() + " AI provider(s) failed. Last error: " + rootMessage(lastCause),
                lastCause);
    }

    // ── Retriability ──────────────────────────────────────────────────────────

    private static boolean isRetriable(Exception ex) {
        String msg = rootMessage(ex).toLowerCase();
        String simpleName = ex.getClass().getSimpleName();

        // ── Rate limit ────────────────────────────────────────────────────────
        if (msg.contains("429") || msg.contains("rate limit") || msg.contains("rate_limit")) return true;
        if (simpleName.equals("RateLimitException")) return true;

        // ── Auth failures (401 / 403) — provider-specific, next provider may work ──
        // A bad key on Groq doesn't mean Google or Cerebras will reject it.
        if (msg.contains("401") || msg.contains("unauthorized")) return true;
        if (msg.contains("403") || msg.contains("forbidden")) return true;
        if (simpleName.equals("AuthenticationException")) return true;

        // ── Auth failures hidden inside 400 responses ─────────────────────────
        // Google AI Studio returns HTTP 400 INVALID_ARGUMENT for invalid API keys
        // instead of 401. Detect by inspecting the response body content.
        if (msg.contains("api_key_invalid")
                || msg.contains("api key not valid")
                || msg.contains("invalid_api_key")
                || msg.contains("invalid api key")
                || msg.contains("authentication_error")) return true;

        // ── Server-side errors 5xx ────────────────────────────────────────────
        if (containsServerSideStatusCode(msg)) return true;

        // ── Network / timeout ─────────────────────────────────────────────────
        if (msg.contains("timeout") || msg.contains("timed out")
                || msg.contains("connect") || msg.contains("socket")
                || msg.contains("read error") || msg.contains("i/o error")) return true;

        // ── Model not found (404) — provider-specific config issue ────────────
        // A wrong model name on one provider (e.g. Cerebras, Ollama) should not
        // abort the chain: the same prompt may succeed on the next provider.
        if (msg.contains("404") || msg.contains("not found")
                || msg.contains("does not exist")
                || msg.contains("model_not_found")
                || simpleName.equals("ModelNotFoundException")) return true;

        // ── Non-retriable : 400 bad request, 422 unprocessable ────────────────
        // These indicate a malformed prompt or content policy — every provider
        // would fail for the same reason, so there is no value in trying the next.
        return false;
    }

    private static boolean containsServerSideStatusCode(String msg) {
        for (int code = 500; code <= 599; code++) {
            if (msg.contains(String.valueOf(code))) return true;
        }
        return false;
    }

    private static String rootMessage(Throwable ex) {
        if (ex == null) return "unknown";
        Throwable cause = ex;
        while (cause.getCause() != null) cause = cause.getCause();
        String msg = cause.getMessage();
        return (msg != null && !msg.isBlank()) ? msg : ex.getClass().getSimpleName();
    }
}