package io.github.afgprojects.framework.ai.core.api.skill;

import org.jspecify.annotations.NonNull;

/**
 * Skill 调度器
 *
 * <p>根据用户意图分析结果，将请求分发到对应的 Skill。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface SkillDispatcher {

    /**
     * 调度 Skill
     *
     * @param input 用户输入
     * @return 路由结果
     */
    @NonNull
    SkillRoutingResult dispatch(@NonNull String input);

    /**
     * 获取意图分析器
     *
     * @return 意图分析器
     */
    @NonNull
    IntentAnalyzer getIntentAnalyzer();

    /**
     * 获取 Skill 注册表
     *
     * @return 注册表
     */
    @NonNull
    SkillRegistry getRegistry();
}