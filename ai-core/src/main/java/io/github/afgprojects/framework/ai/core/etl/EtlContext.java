package io.github.afgprojects.framework.ai.core.etl;

import io.github.afgprojects.framework.ai.core.rag.Document;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ETL 执行上下文。
 *
 * <p>用于跟踪执行过程中的状态、失败记录和重试计数。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class EtlContext {

    private final List<DocumentFailure> failures = new ArrayList<>();
    private final Map<String, Integer> retryCounts = new HashMap<>();
    private final Map<String, Object> metadata = new HashMap<>();

    /**
     * 记录失败。
     */
    public void recordFailure(@Nullable Document document, @NonNull String stage, @NonNull Exception error) {
        failures.add(new DocumentFailure(document, stage, error.getMessage(), error));
    }

    /**
     * 记录失败（仅消息）。
     */
    public void recordFailure(@Nullable Document document, @NonNull String stage, @NonNull String message) {
        failures.add(new DocumentFailure(document, stage, message, null));
    }

    /**
     * 获取文档的重试次数。
     */
    public int getRetryCount(@NonNull Document document) {
        return retryCounts.getOrDefault(document.id(), 0);
    }

    /**
     * 增加重试计数。
     */
    public void incrementRetryCount(@NonNull Document document) {
        retryCounts.merge(document.id(), 1, Integer::sum);
    }

    /**
     * 获取所有失败记录。
     */
    @NonNull
    public List<DocumentFailure> getFailures() {
        return Collections.unmodifiableList(failures);
    }

    /**
     * 设置元数据。
     */
    public void setMetadata(@NonNull String key, @Nullable Object value) {
        metadata.put(key, value);
    }

    /**
     * 获取元数据。
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getMetadata(@NonNull String key) {
        return (T) metadata.get(key);
    }

    /**
     * 获取元数据（带默认值）。
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public <T> T getMetadata(@NonNull String key, @NonNull T defaultValue) {
        Object value = metadata.get(key);
        return value != null ? (T) value : defaultValue;
    }
}