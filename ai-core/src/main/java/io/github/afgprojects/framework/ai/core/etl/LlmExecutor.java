package io.github.afgprojects.framework.ai.core.etl;

import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * LLM 执行器接口。
 *
 * <p>抽象 LLM 调用，支持模板渲染和直接执行。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public interface LlmExecutor {

    /**
     * 使用模板执行 Prompt。
     *
     * @param template  Prompt 模板
     * @param variables 变量映射
     * @return LLM 响应
     */
    @NonNull
    String execute(@NonNull PromptTemplate template, @NonNull Map<String, Object> variables);

    /**
     * 直接执行 Prompt。
     *
     * @param prompt Prompt 字符串
     * @return LLM 响应
     */
    @NonNull
    String execute(@NonNull String prompt);

    /**
     * 异步执行 Prompt。
     *
     * @param prompt Prompt 字符串
     * @return 异步响应
     */
    default @NonNull CompletableFuture<String> executeAsync(@NonNull String prompt) {
        return CompletableFuture.supplyAsync(() -> execute(prompt));
    }
}