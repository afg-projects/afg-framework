package io.github.afgprojects.framework.ai.core.agent;

import io.github.afgprojects.framework.ai.core.api.agent.Agent;
import io.github.afgprojects.framework.ai.core.api.agent.AgentRequest;
import io.github.afgprojects.framework.ai.core.api.agent.AgentResponse;
import io.github.afgprojects.framework.ai.core.api.agent.AgentExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

/**
 * 默认 Agent 执行器
 *
 * <p>提供 Agent 的同步执行和带重试的执行能力：
 * <ul>
 *   <li>{@link #execute} - 委托给 Agent 执行，包装异常处理</li>
 *   <li>{@link #executeWithRetry} - 循环重试，指数退避</li>
 * </ul>
 *
 * <p>异步执行、超时控制、重试+超时组合由 {@link AgentExecutor} 接口的 default 方法提供。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class DefaultAgentExecutor implements AgentExecutor {

    private static final long INITIAL_BACKOFF_MILLIS = 500;
    private static final double BACKOFF_MULTIPLIER = 2.0;

    @Override
    @NonNull
    public AgentResponse execute(@NonNull Agent agent, @NonNull AgentRequest request) {
        log.debug("Executing agent '{}' for session '{}'", agent.getName(), request.sessionId());
        try {
            AgentResponse response = agent.execute(request);
            log.debug("Agent '{}' execution completed with status: {}", agent.getName(), response.status());
            return response;
        } catch (Exception e) {
            log.error("Agent '{}' execution failed for session '{}': {}",
                agent.getName(), request.sessionId(), e.getMessage(), e);
            return AgentResponse.error(
                "Agent '" + agent.getName() + "' execution failed: " + e.getMessage(), e);
        }
    }

    @Override
    @NonNull
    public AgentResponse executeWithRetry(
        @NonNull Agent agent,
        @NonNull AgentRequest request,
        int maxRetries
    ) {
        log.debug("Executing agent '{}' with maxRetries={} for session '{}'",
            agent.getName(), maxRetries, request.sessionId());

        Exception lastException = null;
        long backoffMillis = INITIAL_BACKOFF_MILLIS;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                AgentResponse response = agent.execute(request);
                if (!response.isError()) {
                    if (attempt > 0) {
                        log.info("Agent '{}' succeeded on attempt {} of {}",
                            agent.getName(), attempt + 1, maxRetries + 1);
                    }
                    return response;
                }
                log.warn("Agent '{}' returned error on attempt {} of {}: {}",
                    agent.getName(), attempt + 1, maxRetries + 1, response.output());
            } catch (Exception e) {
                lastException = e;
                log.warn("Agent '{}' threw exception on attempt {} of {}: {}",
                    agent.getName(), attempt + 1, maxRetries + 1, e.getMessage());
            }

            if (attempt < maxRetries) {
                try {
                    log.debug("Backing off {}ms before retry for agent '{}'", backoffMillis, agent.getName());
                    Thread.sleep(backoffMillis);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("Retry interrupted for agent '{}'", agent.getName());
                    return AgentResponse.error(
                        "Agent '" + agent.getName() + "' retry interrupted", ie);
                }
                backoffMillis = (long) (backoffMillis * BACKOFF_MULTIPLIER);
            }
        }

        log.error("Agent '{}' failed after {} retries for session '{}'",
            agent.getName(), maxRetries, request.sessionId());
        return AgentResponse.error(
            "Agent '" + agent.getName() + "' failed after " + maxRetries + " retries",
            lastException != null ? lastException : new RuntimeException("All attempts returned error"));
    }
}
