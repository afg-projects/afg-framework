package io.github.afgprojects.framework.ai.agent.executor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.chat.AiChatResponse;
import io.github.afgprojects.framework.ai.core.api.chat.AiMessage;
import io.github.afgprojects.framework.ai.core.api.planning.ReActExecutor;
import io.github.afgprojects.framework.ai.core.api.planning.ReActResult;
import io.github.afgprojects.framework.ai.core.api.tool.Tool;
import io.github.afgprojects.framework.ai.core.api.tool.ToolRegistry;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ReAct 推理执行器实现
 *
 * <p>实现 ReAct (Reasoning + Acting) 推理模式：
 * <ol>
 *   <li>Thought: LLM 思考下一步行动</li>
 *   <li>Action: LLM 选择执行工具</li>
 *   <li>Observation: 执行工具并获取结果</li>
 *   <li>重复直到得出最终答案</li>
 * </ol>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultReActExecutor implements ReActExecutor {

    private static final Logger log = LoggerFactory.getLogger(DefaultReActExecutor.class);

    private static final String REACT_SYSTEM_PROMPT = """
            You are a reasoning agent that uses the ReAct (Reasoning + Acting) framework.

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

    private final AfgChatClient chatClient;
    private final ToolRegistry toolRegistry;
    private final int maxSteps;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建 ReAct 执行器
     *
     * @param chatClient   对话客户端
     * @param toolRegistry 工具注册表
     * @param maxSteps     最大推理步数
     */
    public DefaultReActExecutor(
            @NonNull AfgChatClient chatClient,
            @NonNull ToolRegistry toolRegistry,
            int maxSteps
    ) {
        this.chatClient = chatClient;
        this.toolRegistry = toolRegistry;
        this.maxSteps = maxSteps;
    }

    @Override
    public @NonNull ReActResult execute(@NonNull String task) {
        return executeWithMaxSteps(task, maxSteps);
    }

    @Override
    public @NonNull ReActResult executeWithMaxSteps(@NonNull String task, int maxSteps) {
        log.info("Starting ReAct execution for task: {}", task);

        List<Object> steps = new ArrayList<>();
        List<AiMessage> messages = new ArrayList<>();
        messages.add(AiMessage.user(task));

        // 构建系统提示
        String systemPrompt = buildSystemPrompt();

        for (int step = 1; step <= maxSteps; step++) {
            log.debug("ReAct step {}", step);

            // 调用 LLM
            AiChatResponse response = chatClient.withSystemPrompt(systemPrompt).chat(messages);
            String content = response.content() != null ? response.content() : "";

            // 解析响应
            ReActParsedResponse parsed = parseResponse(content);

            // 记录步骤
            Map<String, Object> stepRecord = new java.util.LinkedHashMap<>();
            stepRecord.put("step", step);
            stepRecord.put("thought", parsed.thought());
            stepRecord.put("action", parsed.action());
            stepRecord.put("actionInput", parsed.actionInput());
            stepRecord.put("isFinal", parsed.isFinal());

            // 检查是否完成
            if (parsed.isFinal()) {
                log.info("ReAct completed at step {} with final answer", step);
                stepRecord.put("finalAnswer", parsed.finalAnswer());
                steps.add(stepRecord);
                return ReActResult.success(parsed.finalAnswer(), steps);
            }

            // 执行工具
            String observation = null;
            if (parsed.action() != null) {
                observation = executeAction(parsed.action(), parsed.actionInput());
                stepRecord.put("observation", observation);
            }

            steps.add(stepRecord);

            // 更新对话历史
            messages.add(AiMessage.assistant(content));
            if (observation != null) {
                messages.add(AiMessage.user("Observation: " + observation));
            }
        }

        log.warn("ReAct reached max steps ({}) without final answer", maxSteps);
        return ReActResult.failure("Max steps reached without final answer", steps);
    }

    /**
     * 构建系统提示
     */
    private @NonNull String buildSystemPrompt() {
        StringBuilder toolsDescription = new StringBuilder();
        for (Tool<?, ?> tool : toolRegistry.getAllTools()) {
            toolsDescription.append("- ").append(tool.name()).append(": ").append(tool.description()).append("\n");
        }
        return String.format(REACT_SYSTEM_PROMPT, toolsDescription.toString());
    }

    /**
     * 解析 LLM 响应
     */
    private @NonNull ReActParsedResponse parseResponse(@NonNull String content) {
        String thought = null;
        String action = null;
        String actionInput = null;
        String finalAnswer = null;

        // 提取 Thought
        Matcher thoughtMatcher = THOUGHT_PATTERN.matcher(content);
        if (thoughtMatcher.find()) {
            thought = thoughtMatcher.group(1).trim();
        }

        // 检查是否有 Final Answer
        Matcher finalAnswerMatcher = FINAL_ANSWER_PATTERN.matcher(content);
        if (finalAnswerMatcher.find()) {
            finalAnswer = finalAnswerMatcher.group(1).trim();
            return new ReActParsedResponse(thought, null, null, finalAnswer, true);
        }

        // 提取 Action
        Matcher actionMatcher = ACTION_PATTERN.matcher(content);
        if (actionMatcher.find()) {
            action = actionMatcher.group(1).trim();
        }

        // 提取 Action Input
        Matcher actionInputMatcher = ACTION_INPUT_PATTERN.matcher(content);
        if (actionInputMatcher.find()) {
            actionInput = actionInputMatcher.group(1).trim();
        }

        return new ReActParsedResponse(thought, action, actionInput, null, false);
    }

    /**
     * 执行动作（工具调用）
     */
    private @Nullable String executeAction(@NonNull String actionName, @Nullable String actionInput) {
        log.debug("Executing action: {} with input: {}", actionName, actionInput);

        var toolOpt = toolRegistry.getTool(actionName);
        if (toolOpt.isEmpty()) {
            return "Error: Tool not found: " + actionName;
        }

        try {
            // 解析输入参数
            Map<String, Object> args = parseActionInput(actionInput);

            // 执行工具
            @SuppressWarnings("unchecked")
            Tool<Map<String, Object>, Object> tool = (Tool<Map<String, Object>, Object>) toolOpt.get();
            Object result = tool.execute(args);

            return result != null ? result.toString() : "null";

        } catch (Exception e) {
            log.error("Action execution failed: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 解析动作输入
     */
    private java.util.@NonNull Map<String, Object> parseActionInput(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return Map.of();
        }

        // 尝试解析 JSON
        try {
            return objectMapper.readValue(input, new TypeReference<>() {});
        } catch (Exception e) {
            // 如果不是 JSON，作为简单字符串处理
            return Map.of("input", input);
        }
    }

    /**
     * 解析后的响应
     */
    private record ReActParsedResponse(
            @Nullable String thought,
            @Nullable String action,
            @Nullable String actionInput,
            @Nullable String finalAnswer,
            boolean isFinal
    ) {}
}