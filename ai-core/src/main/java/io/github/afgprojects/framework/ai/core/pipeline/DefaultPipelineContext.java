package io.github.afgprojects.framework.ai.core.pipeline;

import io.github.afgprojects.framework.ai.core.api.pipeline.ApplicationConfig;
import io.github.afgprojects.framework.ai.core.api.pipeline.PipelineContext;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class DefaultPipelineContext implements PipelineContext {
    private final String applicationId;
    private final ApplicationConfig config;
    private final String conversationId;
    private final String userId;
    private final String chatUserId;
    private final String userMessage;
    private final Map<String, Object> variables = new ConcurrentHashMap<>();

    private DefaultPipelineContext(Builder builder) {
        this.applicationId = builder.applicationId;
        this.config = builder.config;
        this.conversationId = builder.conversationId;
        this.userId = builder.userId;
        this.chatUserId = builder.chatUserId;
        this.userMessage = builder.userMessage;
    }

    @Override
    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key, Class<T> type) {
        Object value = variables.get(key);
        return value != null ? (T) value : null;
    }

    @Override
    public <T> T getVariable(String key, Class<T> type, T defaultValue) {
        T value = getVariable(key, type);
        return value != null ? value : defaultValue;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String applicationId;
        private ApplicationConfig config;
        private String conversationId;
        private String userId;
        private String chatUserId;
        private String userMessage;

        public Builder applicationId(String applicationId) { this.applicationId = applicationId; return this; }
        public Builder config(ApplicationConfig config) { this.config = config; return this; }
        public Builder conversationId(String conversationId) { this.conversationId = conversationId; return this; }
        public Builder userId(String userId) { this.userId = userId; return this; }
        public Builder chatUserId(String chatUserId) { this.chatUserId = chatUserId; return this; }
        public Builder userMessage(String userMessage) { this.userMessage = userMessage; return this; }
        public DefaultPipelineContext build() { return new DefaultPipelineContext(this); }
    }
}
