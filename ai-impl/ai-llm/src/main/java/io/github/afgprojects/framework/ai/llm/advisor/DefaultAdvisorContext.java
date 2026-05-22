package io.github.afgprojects.framework.ai.llm.advisor;

import io.github.afgprojects.framework.ai.core.model.LlmRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of {@link LlmAdvisor.AdvisorContext}.
 * <p>
 * Provides a simple context implementation for advisor execution.
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultAdvisorContext implements LlmAdvisor.AdvisorContext {

    private final LlmRequest request;
    private final String sessionId;
    private final Map<String, Object> attributes = new HashMap<>();

    /**
     * Creates a new advisor context.
     *
     * @param request   the LLM request
     * @param sessionId the session ID (optional)
     */
    public DefaultAdvisorContext(@NonNull LlmRequest request, @Nullable String sessionId) {
        this.request = request;
        this.sessionId = sessionId;
    }

    /**
     * Creates a new advisor context without a session ID.
     *
     * @param request the LLM request
     */
    public DefaultAdvisorContext(@NonNull LlmRequest request) {
        this(request, null);
    }

    @Override
    @NonNull
    public LlmRequest getRequest() {
        return request;
    }

    @Override
    @Nullable
    public String getSessionId() {
        return sessionId;
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(@NonNull String key) {
        return (T) attributes.get(key);
    }

    @Override
    public void setAttribute(@NonNull String key, @Nullable Object value) {
        attributes.put(key, value);
    }
}
