package io.github.afgprojects.framework.ai.core.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.core.api.agent.Agent;
import io.github.afgprojects.framework.ai.core.api.agent.AgentRequest;
import io.github.afgprojects.framework.ai.core.api.agent.AgentResponse;
import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.chat.AiChatResponse;
import io.github.afgprojects.framework.ai.core.api.chat.AiMessage;
import io.github.afgprojects.framework.ai.core.api.tool.Tool;
import io.github.afgprojects.framework.ai.core.api.tool.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 默认 Agent 实现
 *
 * <p>实现基于 ReAct (Reasoning + Acting) 模式的智能体：
 * <ol>
 *   <li>Thought: LLM 思考下一步行动</li>
 *   <li>Action: LLM 选择执行工具</li>
 *   <li>Observation: 执行工具并获取结果</li>
 *   <li>重复直到得出最终答案</li>
 * </ol>
 *
 * <p>使用示例：
 * <pre>{@code
 * DefaultAgent agent = DefaultAgent.builder()
 *     .name("MyAgent")
 *     .description("A helpful assistant")
 *     .chatClient(chatClient)
 *     .toolRegistry(toolRegistry)
 *     .maxIterations(10)
 *     .build();
 *
 * AgentResponse response = agent.execute(new AgentRequest("session-1", "What is the weather?"));
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class DefaultAgent implements Agent {

    private static final String AGENT_SYSTEM_PROMPT = """
            You are an AI agent that uses the ReAct (Reasoning + Acting) framework.

            For each step, you must respond in the following format:

            Thought: [Your reasoning about what to do next]
            Action: [tool_name]
            Action Input: [JSON arguments for the tool]

            OR if you have the final answer:

            Thought: [Your final reasoning]
            Final Answer: [Your answer to the user]

            Available tools:
            %s

            Remember:
            1. Always start with "Thought:" to explain your reasoning
            2. Use "Action:" and "Action Input:" to call tools
            3. Use "Final Answer:" when you have the complete answer
            4. Be concise and focused
            """;

    private static final Pattern THOUGHT_PATTERN = Pattern.compile("Thought:\\s*(.+?)(?=\\n(?:Action|Final)|$)", Pattern.DOTALL);
    private static final Pattern ACTION_PATTERN = Pattern.compile("Action:\\s*(\\w+)");
    private static final Pattern ACTION_INPUT_PATTERN = Pattern.compile("Action Input:\\s*(.+?)(?=\\n|$)", Pattern.DOTALL);
    private static final Pattern FINAL_ANSWER_PATTERN = Pattern.compile("Final Answer:\\s*(.+)$", Pattern.DOTALL);

    private final String name;
    private final String description;
    private final AfgChatClient chatClient;
    private final ToolRegistry toolRegistry;
    private final int maxIterations;
    private final ObjectMapper objectMapper;

    /**
     * 私有构造函数，使用 Builder 创建实例
     */
    private DefaultAgent(@NonNull Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.chatClient = builder.chatClient;
        this.toolRegistry = builder.toolRegistry;
        this.maxIterations = builder.maxIterations;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public @NonNull String getName() {
        return name;
    }

    @Override
    public @NonNull String getDescription() {
        return description;
    }

    @Override
    public @NonNull AgentResponse execute(@NonNull AgentRequest request) {
        log.info("Agent '{}' executing request for session: {}", name, request.sessionId());
        log.debug("User input: {}", request.userInput());

        try {
            return doExecute(request);
        } catch (Exception e) {
            log.error("Agent execution failed: {}", e.getMessage(), e);
            return AgentResponse.error("Agent execution failed: " + e.getMessage(), e);
        }
    }

    private @NonNull AgentResponse doExecute(@NonNull AgentRequest request) {
        List<AiMessage> messages = new ArrayList<>();

        // 添加历史记录
        if (request.history() != null && !request.history().isEmpty()) {
            for (Object historyItem : request.history()) {
                if (historyItem instanceof AiMessage aiMessage) {
                    messages.add(aiMessage);
                }
            }
        }

        // 添加当前用户输入
        messages.add(AiMessage.user(request.userInput()));

        // 构建系统提示
        String systemPrompt = buildSystemPrompt();

        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            log.debug("Agent iteration {}/{}", iteration, maxIterations);

            // 调用 LLM
            AiChatResponse response = chatClient.withSystemPrompt(systemPrompt).chat(messages);
            String content = response.content() != null ? response.content() : "";

            // 解析响应
            ParsedResponse parsed = parseResponse(content);

            // 检查是否完成
            if (parsed.isFinal()) {
                log.info("Agent completed at iteration {} with final answer", iteration);
                return AgentResponse.completed(parsed.finalAnswer());
            }

            // 执行工具
            String observation = null;
            if (parsed.action() != null) {
                observation = executeTool(parsed.action(), parsed.actionInput());
            }

            // 更新对话历史
            messages.add(AiMessage.assistant(content));
            if (observation != null) {
                messages.add(AiMessage.user("Observation: " + observation));
            }
        }

        log.warn("Agent reached max iterations ({}) without final answer", maxIterations);
        return AgentResponse.error("Max iterations reached without final answer");
    }

    /**
     * 构建系统提示，包含工具描述
     */
    private @NonNull String buildSystemPrompt() {
        StringBuilder toolsDescription = new StringBuilder();
        for (Tool<?, ?> tool : toolRegistry.getAllTools()) {
            toolsDescription.append("- ")
                    .append(tool.name())
                    .append(": ")
                    .append(tool.description())
                    .append("\n");
        }
        return String.format(AGENT_SYSTEM_PROMPT, toolsDescription.toString());
    }

    /**
     * 解析 LLM 响应
     */
    private @NonNull ParsedResponse parseResponse(@NonNull String content) {
        String thought = null;
        String action = null;
        String actionInput = null;
        String finalAnswer = null;

        Matcher thoughtMatcher = THOUGHT_PATTERN.matcher(content);
        if (thoughtMatcher.find()) {
            thought = thoughtMatcher.group(1).trim();
        }

        Matcher finalAnswerMatcher = FINAL_ANSWER_PATTERN.matcher(content);
        if (finalAnswerMatcher.find()) {
            finalAnswer = finalAnswerMatcher.group(1).trim();
            return new ParsedResponse(thought, null, null, finalAnswer, true);
        }

        Matcher actionMatcher = ACTION_PATTERN.matcher(content);
        if (actionMatcher.find()) {
            action = actionMatcher.group(1).trim();
        }

        Matcher actionInputMatcher = ACTION_INPUT_PATTERN.matcher(content);
        if (actionInputMatcher.find()) {
            actionInput = actionInputMatcher.group(1).trim();
        }

        return new ParsedResponse(thought, action, actionInput, null, false);
    }

    /**
     * 执行工具
     */
    private @Nullable String executeTool(@NonNull String toolName, @Nullable String actionInput) {
        log.debug("Executing tool: {} with input: {}", toolName, actionInput);

        var toolOpt = toolRegistry.getTool(toolName);
        if (toolOpt.isEmpty()) {
            log.warn("Tool not found: {}", toolName);
            return "Error: Tool not found: " + toolName;
        }

        try {
            Map<String, Object> args = parseToolInput(actionInput);

            @SuppressWarnings("unchecked")
            Tool<Map<String, Object>, Object> tool = (Tool<Map<String, Object>, Object>) toolOpt.get();
            Object result = tool.execute(args);

            String resultStr = result != null ? result.toString() : "null";
            log.debug("Tool {} returned: {}", toolName, resultStr);
            return resultStr;

        } catch (Exception e) {
            log.error("Tool execution failed: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 解析工具输入
     */
    private @NonNull Map<String, Object> parseToolInput(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(input, new TypeReference<>() {});
        } catch (Exception e) {
            log.debug("Failed to parse tool input as JSON, using raw input: {}", input);
            return Map.of("input", input);
        }
    }

    @Override
    public @NonNull List<?> getTools() {
        Collection<Tool<?, ?>> tools = toolRegistry.getAllTools();
        return new ArrayList<>(tools);
    }

    @Override
    public boolean supportsTool(@NonNull String toolName) {
        return toolRegistry.exists(toolName);
    }

    /**
     * 创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder 类
     */
    public static class Builder {

        private String name = "DefaultAgent";
        private String description = "A default AI agent";
        private AfgChatClient chatClient;
        private ToolRegistry toolRegistry;
        private int maxIterations = 10;

        /**
         * 设置 Agent 名称
         */
        public Builder name(@NonNull String name) {
            this.name = name;
            return this;
        }

        /**
         * 设置 Agent 描述
         */
        public Builder description(@NonNull String description) {
            this.description = description;
            return this;
        }

        /**
         * 设置对话客户端
         */
        public Builder chatClient(@NonNull AfgChatClient chatClient) {
            this.chatClient = chatClient;
            return this;
        }

        /**
         * 设置工具注册表
         */
        public Builder toolRegistry(@NonNull ToolRegistry toolRegistry) {
            this.toolRegistry = toolRegistry;
            return this;
        }

        /**
         * 设置最大迭代次数
         */
        public Builder maxIterations(int maxIterations) {
            this.maxIterations = maxIterations;
            return this;
        }

        /**
         * 构建 DefaultAgent 实例
         */
        public DefaultAgent build() {
            if (chatClient == null) {
                throw new IllegalStateException("chatClient is required");
            }
            if (toolRegistry == null) {
                throw new IllegalStateException("toolRegistry is required");
            }
            if (maxIterations <= 0) {
                throw new IllegalArgumentException("maxIterations must be positive");
            }
            return new DefaultAgent(this);
        }
    }

    /**
     * 解析后的响应
     */
    private record ParsedResponse(
            @Nullable String thought,
            @Nullable String action,
            @Nullable String actionInput,
            @Nullable String finalAnswer,
            boolean isFinal
    ) {}
}
