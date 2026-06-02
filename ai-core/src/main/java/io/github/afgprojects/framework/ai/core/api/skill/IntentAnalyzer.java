package io.github.afgprojects.framework.ai.core.api.skill;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 意图分析器
 *
 * <p>分析用户输入的意图，匹配对应的 Skill。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface IntentAnalyzer {

    /**
     * 分析用户意图
     *
     * @param input   用户输入
     * @param context 执行上下文
     * @return 意图分析结果
     */
    @NonNull
    IntentResult analyze(@NonNull String input, @NonNull SkillContext context);

    /**
     * 分析用户意图（简化版）
     *
     * @param input 用户输入
     * @return 意图分析结果
     */
    @NonNull
    IntentResult analyze(@NonNull String input);
}