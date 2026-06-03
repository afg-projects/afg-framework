package io.github.afgprojects.framework.ai.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.core.agent.DefaultAgent;
import io.github.afgprojects.framework.ai.core.api.agent.Agent;
import io.github.afgprojects.framework.ai.core.api.agent.AgentExecutor;
import io.github.afgprojects.framework.ai.core.api.agent.AgentRequest;
import io.github.afgprojects.framework.ai.core.api.agent.AgentResponse;
import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.chat.ChatClientRegistry;
import io.github.afgprojects.framework.ai.core.api.tool.ToolRegistry;
import io.github.afgprojects.framework.ai.core.entity.agent.AgentDefinitionEntity;
import io.github.afgprojects.framework.ai.core.entity.agent.AgentSessionEntity;
import io.github.afgprojects.framework.data.core.DataManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

/**
 * Agent 执行服务
 * <p>
 * 负责加载 Agent 定义、构建 Agent 实例、执行 Agent、更新会话状态。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final DataManager dataManager;
    private final ChatClientRegistry chatClientRegistry;
    private final ToolRegistry toolRegistry;
    private final AgentExecutor agentExecutor;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    /**
     * 执行 Agent
     * <p>
     * 不使用 @Transactional：AI 执行可能耗时数分钟，不应持有数据库事务。
     * 会话状态的更新在独立短事务中通过 TransactionTemplate 完成。
     *
     * @param sessionId  会话 ID
     * @param userInput  用户输入
     * @return Agent 响应
     */
    public AgentResponse execute(Long sessionId, String userInput) {
        // 1. 加载会话（读操作，无需事务）
        AgentSessionEntity session = dataManager.findById(AgentSessionEntity.class, sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Agent session not found: " + sessionId));

        // 2. 加载 Agent 定义（读操作，无需事务）
        AgentDefinitionEntity definition = dataManager.findById(AgentDefinitionEntity.class, session.getAgentDefinitionId())
            .orElseThrow(() -> new IllegalArgumentException("Agent definition not found: " + session.getAgentDefinitionId()));

        // 3. 更新会话状态为运行中（短事务）
        updateSessionStatusInTransaction(sessionId, "RUNNING", null);

        // 4. 构建 Agent
        Agent agent = buildAgent(definition);

        // 5. 构建请求
        AgentRequest request = new AgentRequest(
            String.valueOf(sessionId),
            userInput,
            parseJsonToMap(definition.getConfig()),
            java.util.List.of()
        );

        // 6. 执行 Agent（AI 调用，可能耗时数分钟，不在事务中）
        AgentResponse response;
        try {
            response = agentExecutor.execute(agent, request);
        } catch (Exception e) {
            log.error("Agent execution failed for session {}: {}", sessionId, e.getMessage(), e);
            response = AgentResponse.error("Agent execution failed: " + e.getMessage(), e);
        }

        // 7. 更新会话状态（短事务）
        String outputJson = response.output() != null ? toJsonOrNull(Map.of("lastOutput", response.output())) : null;
        updateSessionStatusInTransaction(sessionId, response.status().name(), outputJson);

        log.info("Agent execution completed for session {}: status={}", sessionId, response.status());
        return response;
    }

    /**
     * 在独立短事务中更新会话状态
     * <p>
     * 使用 TransactionTemplate 而非 @Transactional，因为此方法从同一 bean 内部调用，
     * Spring AOP 代理不会拦截自调用。
     */
    private void updateSessionStatusInTransaction(Long sessionId, String status, String metadataJson) {
        new TransactionTemplate(transactionManager).executeWithoutResult(txStatus -> {
            AgentSessionEntity session = dataManager.findById(AgentSessionEntity.class, sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Agent session not found: " + sessionId));
            session.setStatus(status);
            if (metadataJson != null) {
                Map<String, Object> metadata = parseJsonToMap(session.getMetadata());
                metadata.putAll(parseJsonToMap(metadataJson));
                session.setMetadata(toJsonOrNull(metadata));
            }
            dataManager.save(AgentSessionEntity.class, session);
        });
    }

    /**
     * 从 Agent 定义构建 Agent 实例
     */
    private Agent buildAgent(AgentDefinitionEntity definition) {
        // 获取 ChatClient
        AfgChatClient chatClient;
        if (definition.getChatClientName() != null && !definition.getChatClientName().isBlank()) {
            chatClient = chatClientRegistry.get(definition.getChatClientName())
                .orElseThrow(() -> new IllegalArgumentException(
                    "ChatClient not found: " + definition.getChatClientName()));
        } else {
            chatClient = chatClientRegistry.getDefault();
        }

        // 切换模型和系统提示词
        if (definition.getModelName() != null && !definition.getModelName().isBlank()) {
            chatClient = chatClient.withModel(definition.getModelName());
        }
        if (definition.getSystemPrompt() != null && !definition.getSystemPrompt().isBlank()) {
            chatClient = chatClient.withSystemPrompt(definition.getSystemPrompt());
        }

        // 构建 DefaultAgent
        DefaultAgent.Builder builder = DefaultAgent.builder()
            .name(definition.getName())
            .description(definition.getDescription() != null ? definition.getDescription() : "")
            .chatClient(chatClient)
            .toolRegistry(toolRegistry);

        if (definition.getMaxIterations() != null && definition.getMaxIterations() > 0) {
            builder.maxIterations(definition.getMaxIterations());
        }

        return builder.build();
    }

    private Map<String, Object> parseJsonToMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse JSON to Map: {}", e.getMessage());
            return Map.of();
        }
    }

    private String toJsonOrNull(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize value to JSON: {}", e.getMessage());
            return null;
        }
    }
}