package io.github.afgprojects.framework.ai.agent.skill.intent;

import io.github.afgprojects.framework.ai.agent.skill.SkillContext;
import io.github.afgprojects.framework.ai.agent.skill.SkillDefinition;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 意图分析器接口
 *
 * <p>负责分析用户输入，识别意图并匹配合适的 Skill。
 *
 * <p>工作流程：
 * <ol>
 *   <li>接收用户输入和可用的 Skills 列表</li>
 *   <li>使用 LLM 分析用户意图</li>
 *   <li>返回匹配的 Skills 及置信度</li>
 *   <li>提取用户输入中的参数</li>
 * </ol>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface IntentAnalyzer {

    /**
     * 分析用户输入，识别意图
     *
     * @param userInput       用户输入
     * @param availableSkills 可用的 Skills
     * @param context         执行上下文
     * @return 意图分析结果
     */
    @NonNull
    IntentResult analyze(
        @NonNull String userInput,
        @NonNull List<SkillDefinition> availableSkills,
        @NonNull SkillContext context
    );

    /**
     * 分析用户输入，返回匹配的 Skills 及置信度
     *
     * @param userInput       用户输入
     * @param availableSkills 可用的 Skills
     * @return 匹配结果列表
     */
    @NonNull
    List<SkillMatch> matchSkills(
        @NonNull String userInput,
        @NonNull List<SkillDefinition> availableSkills
    );
}
