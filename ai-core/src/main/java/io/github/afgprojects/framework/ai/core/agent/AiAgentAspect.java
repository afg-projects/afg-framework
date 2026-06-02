package io.github.afgprojects.framework.ai.core.agent;

import io.github.afgprojects.framework.ai.core.api.agent.Agent;
import io.github.afgprojects.framework.ai.core.api.agent.AgentExecutor;
import io.github.afgprojects.framework.ai.core.api.agent.AgentRequest;
import io.github.afgprojects.framework.ai.core.api.agent.AgentResponse;
import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.chat.ChatClientRegistry;
import io.github.afgprojects.framework.ai.core.agent.annotation.AiAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * @AiAgent 注解的 AOP 切面，拦截标注了 @AiAgent 的方法，自动执行 Agent。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class AiAgentAspect {

    private final AgentExecutor agentExecutor;
    private final ChatClientRegistry chatClientRegistry;
    private final AgentRegistry agentRegistry;

    @Around("@annotation(aiAgent)")
    public Object aroundAiAgent(ProceedingJoinPoint joinPoint, AiAgent aiAgent) throws Throwable {
        log.debug("AiAgent aspect intercepting: {}", joinPoint.getSignature().getName());

        // 从方法参数提取用户输入
        Object[] args = joinPoint.getArgs();
        String userInput = args.length > 0 ? String.valueOf(args[0]) : "";

        // 生成会话 ID
        String sessionId = UUID.randomUUID().toString();

        // 构建 Agent 请求
        AgentRequest request = new AgentRequest(
                sessionId,
                userInput,
                buildContext(aiAgent),
                List.of()
        );

        // 获取或创建 Agent
        Agent agent = getOrCreateAgent(aiAgent);

        // 执行 Agent
        AgentResponse response;
        if (aiAgent.timeoutMs() > 0) {
            response = agentExecutor.executeWithRetryAndTimeout(
                    agent,
                    request,
                    0, // maxRetries - 可从注解扩展
                    aiAgent.timeoutMs()
            );
        } else {
            response = agentExecutor.execute(agent, request);
        }

        // 返回结果
        if (response.isError()) {
            throw new AgentExecutionException("Agent execution failed: " + response.output());
        }

        return response.output();
    }

    /**
     * 构建上下文信息
     */
    @NonNull
    private Map<String, Object> buildContext(AiAgent aiAgent) {
        Map<String, Object> context = new HashMap<>();
        context.put("chatClient", aiAgent.chatClient());
        context.put("maxIterations", aiAgent.maxIterations());
        return context;
    }

    /**
     * 获取或创建 Agent
     */
    @NonNull
    private Agent getOrCreateAgent(AiAgent aiAgent) {
        String agentName = aiAgent.value();
        if (agentName != null && !agentName.isEmpty()) {
            return agentRegistry.get(agentName)
                    .orElseThrow(() -> new IllegalStateException("Agent not found: " + agentName));
        }

        // 如果没有指定 Agent 名称，创建临时 Agent
        return createTemporaryAgent(aiAgent);
    }

    /**
     * 创建临时 Agent
     */
    @NonNull
    private Agent createTemporaryAgent(AiAgent aiAgent) {
        // 获取 ChatClient
        var chatClient = chatClientRegistry.get(aiAgent.chatClient())
                .orElseGet(chatClientRegistry::getDefault);

        // 创建简单的 Agent 实现
        return new SimpleAgent(chatClient, aiAgent.maxIterations());
    }

    /**
     * 简单 Agent 实现 - 无工具的轻量级 Agent
     */
    private static class SimpleAgent implements Agent {

        private final AfgChatClient chatClient;
        private final int maxIterations;

        SimpleAgent(AfgChatClient chatClient, int maxIterations) {
            this.chatClient = chatClient;
            this.maxIterations = maxIterations;
        }

        @Override
        @NonNull
        public String getName() {
            return "SimpleAgent";
        }

        @Override
        @NonNull
        public String getDescription() {
            return "Simple agent without tools";
        }

        @Override
        @NonNull
        public AgentResponse execute(@NonNull AgentRequest request) {
            try {
                var response = chatClient.chat(request.userInput());
                return AgentResponse.completed(response.content());
            } catch (Exception e) {
                return AgentResponse.error("Agent execution failed", e);
            }
        }

        @Override
        @NonNull
        public List<?> getTools() {
            return List.of();
        }
    }

    /**
     * Agent 注册接口（简化版）
     */
    public interface AgentRegistry {
        @NonNull
        Optional<Agent> get(@NonNull String name);
    }

    /**
     * Agent 执行异常
     */
    public static class AgentExecutionException extends RuntimeException {
        public AgentExecutionException(String message) {
            super(message);
        }
    }
}
