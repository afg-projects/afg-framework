package io.github.afgprojects.framework.ai.agent.skill;

import io.github.afgprojects.framework.ai.core.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.chat.AiChatResponse;
import io.github.afgprojects.framework.ai.core.chat.AiMessage;
import io.github.afgprojects.framework.ai.core.tool.Tool;
import io.github.afgprojects.framework.ai.core.tool.ToolRegistry;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 技能执行器默认实现
 *
 * <p>使用 {@link AfgChatClient} 解析自然语言并执行对应技能。
 * 支持两种模式：
 * <ul>
 *   <li>直调模式：已知技能 ID 时直接调用</li>
 *   <li>匹配模式：通过自然语言描述选择并调用技能</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultSkillExecutor implements SkillExecutor {

    private static final Logger log = LoggerFactory.getLogger(DefaultSkillExecutor.class);

    private static final String SKILL_SYSTEM_PROMPT = """
            You are an assistant that helps identify and execute skills based on user requests.

            Given a user request, determine which skill to use and extract the parameters.

            Respond in the following format:
            Skill: [skill_id]
            Parameters: [JSON object with parameters]

            If no skill matches, respond:
            Skill: none
            Parameters: {}

            Available skills:
            %s
            """;

    private final SkillRegistry skillRegistry;
    private final AfgChatClient chatClient;
    private final @Nullable ToolRegistry toolRegistry;

    public DefaultSkillExecutor(
            @NonNull SkillRegistry skillRegistry,
            @NonNull AfgChatClient chatClient,
            @Nullable ToolRegistry toolRegistry
    ) {
        this.skillRegistry = skillRegistry;
        this.chatClient = chatClient;
        this.toolRegistry = toolRegistry;
    }

    @Override
    public @NonNull SkillResult execute(@NonNull String name, @NonNull Map<String, Object> inputs) {
        log.info("Executing skill by name: {}", name);

        var definitionOpt = skillRegistry.get(name);
        if (definitionOpt.isEmpty()) {
            return SkillResult.failure("Skill not found: " + name);
        }

        SkillDefinition definition = definitionOpt.get();
        return executeDefinition(definition, inputs);
    }

    @Override
    public @NonNull SkillResult execute(@NonNull SkillContext context) {
        log.info("Executing skill from context: {}", context.getSkillName());

        var definitionOpt = skillRegistry.get(context.getSkillName());
        if (definitionOpt.isEmpty()) {
            return SkillResult.failure("Skill not found: " + context.getSkillName());
        }

        SkillDefinition definition = definitionOpt.get();
        return executeDefinition(definition, context.getInputs());
    }

    @Override
    public @NonNull Flux<SkillResult> executeStream(@NonNull SkillContext context) {
        return Flux.create(sink -> {
            try {
                SkillResult result = execute(context);
                sink.next(result);
                sink.complete();
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    @Override
    public @NonNull AiChatResponse executeRaw(@NonNull SkillContext context) {
        log.info("Executing skill raw from context: {}", context.getSkillName());

        var definitionOpt = skillRegistry.get(context.getSkillName());
        if (definitionOpt.isEmpty()) {
            return AiChatResponse.of("Skill not found: " + context.getSkillName());
        }

        SkillDefinition definition = definitionOpt.get();
        String prompt = renderPrompt(definition.prompt(), context.getInputs());

        return chatClient.chat(AiMessage.user(prompt));
    }

    @Override
    public @NonNull String renderPrompt(@NonNull String prompt, @NonNull SkillContext context) {
        return renderPrompt(prompt, context.getInputs());
    }

    private @NonNull String renderPrompt(@NonNull String prompt, @NonNull Map<String, Object> inputs) {
        String rendered = prompt;
        for (Map.Entry<String, Object> entry : inputs.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}",
                    entry.getValue() != null ? entry.getValue().toString() : "");
        }
        return rendered;
    }

    @Override
    public @NonNull SkillRegistry getRegistry() {
        return skillRegistry;
    }

    @Override
    public @NonNull List<String> getToolNames(@NonNull SkillDefinition definition) {
        if (definition.tools() == null || definition.tools().isEmpty()) {
            return List.of();
        }
        return definition.tools();
    }

    // ── 内部方法 ────────────────────────────────────────────────────────────────

    private @NonNull SkillResult executeDefinition(
            @NonNull SkillDefinition definition,
            @NonNull Map<String, Object> inputs
    ) {
        log.info("Executing skill: {} ({})", definition.name(), definition.description());

        try {
            // 构建提示词
            String prompt = renderPrompt(definition.prompt(), inputs);

            // 调用 LLM
            AiChatResponse response = chatClient.chat(AiMessage.user(prompt));

            String content = response.content() != null ? response.content() : "";

            // 如果技能关联了工具，执行工具
            if (toolRegistry != null && definition.tools() != null) {
                for (String toolName : definition.tools()) {
                    var toolOpt = toolRegistry.getTool(toolName);
                    if (toolOpt.isPresent()) {
                        content = executeToolAndAppend(toolOpt.get(), inputs, content);
                    }
                }
            }

            return SkillResult.success(content);

        } catch (Exception e) {
            log.error("Skill execution failed: {} - {}", definition.name(), e.getMessage());
            return SkillResult.failure("Skill execution failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private @NonNull String executeToolAndAppend(
            @NonNull Tool<?, ?> tool,
            @NonNull Map<String, Object> inputs,
            @NonNull String existingContent
    ) {
        try {
            Tool<Map<String, Object>, Object> typedTool = (Tool<Map<String, Object>, Object>) tool;
            Object result = typedTool.execute(inputs);
            if (result != null) {
                return existingContent + "\n\nTool result (" + tool.name() + "): " + result;
            }
        } catch (Exception e) {
            log.warn("Tool {} execution failed: {}", tool.name(), e.getMessage());
        }
        return existingContent;
    }
}