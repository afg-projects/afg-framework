package io.github.afgprojects.framework.ai.core.agent;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Agent 执行器接口
 *
 * <p>AgentExecutor 负责管理 Agent 的执行过程，提供：
 * <ul>
 *   <li>同步执行 - 阻塞等待结果</li>
 *   <li>异步执行 - 非阻塞返回 Future</li>
 *   <li>重试机制 - 失败时自动重试</li>
 *   <li>执行控制 - 超时、取消等</li>
 * </ul>
 *
 * <p>实现示例：
 * <pre>{@code
 * public class DefaultAgentExecutor implements AgentExecutor {
 *     @Override
 *     public AgentResponse execute(Agent agent, AgentRequest request) {
 *         return agent.execute(request);
 *     }
 *
 *     @Override
 *     public AgentResponse executeWithRetry(Agent agent, AgentRequest request, int maxRetries) {
 *         Exception lastException = null;
 *         for (int i = 0; i <= maxRetries; i++) {
 *             try {
 *                 return agent.execute(request);
 *             } catch (Exception e) {
 *                 lastException = e;
 *             }
 *         }
 *         return AgentResponse.error("Failed after " + maxRetries + " retries", lastException);
 *     }
 * }
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface AgentExecutor {

    /**
     * 执行 Agent
     *
     * <p>同步执行 Agent 并返回结果。
     *
     * @param agent   要执行的 Agent
     * @param request 请求对象
     * @return 响应对象
     */
    @NonNull
    AgentResponse execute(@NonNull Agent agent, @NonNull AgentRequest request);

    /**
     * 带重试的执行 Agent
     *
     * <p>当执行失败时自动重试，直到成功或达到最大重试次数。
     *
     * @param agent      要执行的 Agent
     * @param request    请求对象
     * @param maxRetries 最大重试次数
     * @return 响应对象
     */
    @NonNull
    AgentResponse executeWithRetry(
        @NonNull Agent agent,
        @NonNull AgentRequest request,
        int maxRetries
    );

    /**
     * 异步执行 Agent
     *
     * <p>非阻塞执行 Agent，返回 CompletableFuture。
     *
     * @param agent   要执行的 Agent
     * @param request 请求对象
     * @return 异步响应
     */
    @NonNull
    default CompletableFuture<AgentResponse> executeAsync(
        @NonNull Agent agent,
        @NonNull AgentRequest request
    ) {
        return CompletableFuture.supplyAsync(() -> execute(agent, request));
    }

    /**
     * 带超时的执行 Agent
     *
     * <p>如果执行超时，返回错误响应。
     *
     * @param agent        要执行的 Agent
     * @param request      请求对象
     * @param timeoutMillis 超时时间（毫秒）
     * @return 响应对象
     */
    @NonNull
    default AgentResponse executeWithTimeout(
        @NonNull Agent agent,
        @NonNull AgentRequest request,
        long timeoutMillis
    ) {
        try {
            return executeAsync(agent, request)
                .get(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            return AgentResponse.error("Execution timed out after " + timeoutMillis + "ms");
        } catch (Exception e) {
            return AgentResponse.error("Execution failed", e);
        }
    }

    /**
     * 带重试和超时的执行 Agent
     *
     * @param agent         要执行的 Agent
     * @param request       请求对象
     * @param maxRetries    最大重试次数
     * @param timeoutMillis 超时时间（毫秒）
     * @return 响应对象
     */
    @NonNull
    default AgentResponse executeWithRetryAndTimeout(
        @NonNull Agent agent,
        @NonNull AgentRequest request,
        int maxRetries,
        long timeoutMillis
    ) {
        Exception lastException = null;
        for (int i = 0; i <= maxRetries; i++) {
            try {
                AgentResponse response = executeWithTimeout(agent, request, timeoutMillis);
                if (!response.isError()) {
                    return response;
                }
            } catch (Exception e) {
                lastException = e;
            }
        }
        return AgentResponse.error(
            "Failed after " + maxRetries + " retries",
            lastException != null ? lastException : new RuntimeException("Unknown error")
        );
    }
}
