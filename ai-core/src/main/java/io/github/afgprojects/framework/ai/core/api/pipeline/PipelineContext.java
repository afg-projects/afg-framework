package io.github.afgprojects.framework.ai.core.api.pipeline;

import java.util.List;
import java.util.Map;

public interface PipelineContext {
    String getApplicationId();
    ApplicationConfig getConfig();
    String getConversationId();
    String getUserId();
    String getChatUserId();
    String getUserMessage();
    Map<String, Object> getVariables();
    void setVariable(String key, Object value);
    <T> T getVariable(String key, Class<T> type);
    <T> T getVariable(String key, Class<T> type, T defaultValue);

    default String getOptimizedQuestion() {
        return getVariable("optimizedQuestion", String.class, getUserMessage());
    }

    @SuppressWarnings("unchecked")
    default List<SearchResult> getSearchResults() {
        return getVariable("searchResults", List.class, List.of());
    }

    default String getReferenceContent() {
        return getVariable("referenceContent", String.class, "");
    }
}
