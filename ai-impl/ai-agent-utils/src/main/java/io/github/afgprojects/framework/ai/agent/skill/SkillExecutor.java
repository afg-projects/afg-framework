package io.github.afgprojects.framework.ai.agent.skill;

import io.github.afgprojects.framework.ai.core.model.LlmResponse;
import io.github.afgprojects.framework.ai.core.tool.ToolDefinition;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Skill 执行器接口
 *
 * <p>执行 Skill，集成 Spring AI 的 ChatClient 和 Advisor 机制。
 *
 * <p>执行流程：
 * <ol>
 *   <li>渲染提示词模板</li>
 *   <li>构建 ChatClient 请求（含 Advisor）</li>
 *   <li>调用 LLM</li>
 *   <li>处理工具调用（如有）</li>
 *   <li>返回结果</li>
 * </ol>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface SkillExecutor {

    /**
     * 执行 Skill（同步）
     *
     * @param name   Skill 名称
     * @param inputs 输入参数
     * @return 执行结果
     */
    @NonNull
    SkillResult execute(@NonNull String name, @NonNull Map<String, Object> inputs);

    /**
     * 执行 Skill（带上下文）
     *
     * @param context 执行上下文
     * @return 执行结果
     */
    @NonNull
    SkillResult execute(@NonNull SkillContext context);

    /**
     * 执行 Skill（流式）
     *
     * @param context 执行上下文
     * @return 流式结果
     */
    @NonNull
    Flux<SkillResult> executeStream(@NonNull SkillContext context);

    /**
     * 执行 Skill 并返回 LLM 原始响应
     *
     * @param context 执行上下文
     * @return LLM 响应
     */
    @NonNull
    LlmResponse executeRaw(@NonNull SkillContext context);

    /**
     * 渲染提示词模板
     *
     * @param prompt  提示词模板
     * @param context 执行上下文
     * @return 渲染后的提示词
     */
    @NonNull
    String renderPrompt(@NonNull String prompt, @NonNull SkillContext context);

    /**
     * 获取 Skill 注册表
     *
     * @return 注册表
     */
    @NonNull
    SkillRegistry getRegistry();

    /**
     * 获取 Skill 可用的工具列表
     *
     * @param definition Skill 定义
     * @return 工具定义列表
     */
    @NonNull
    default List<ToolDefinition> getTools(@NonNull SkillDefinition definition) {
        if (definition.tools() == null || definition.tools().isEmpty()) {
            return List.of();
        }
        // 从 ToolRegistry 获取工具定义
        return List.of();
    }
}
