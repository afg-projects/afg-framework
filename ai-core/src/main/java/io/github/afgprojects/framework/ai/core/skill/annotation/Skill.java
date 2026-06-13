package io.github.afgprojects.framework.ai.core.skill.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式 Skill 注解。
 * <p>
 * 标注在方法上，自动注册为 Skill 到 {@link io.github.afgprojects.framework.ai.core.api.skill.SkillRegistry}。
 * 方法将在应用启动时被扫描并注册，支持意图关键词匹配。
 *
 * <pre>
 * {@code
 * @Skill(name = "refund", description = "退款处理", intentKeywords = {"退款", "退钱", "退费"})
 * public SkillResult handleRefund(SkillContext context) {
 *     // 业务逻辑
 * }
 * }
 * </pre>
 *
 * @author afg-projects
 * @since 1.0.0
 * @see io.github.afgprojects.framework.ai.core.api.skill.SkillRegistry
 * @see io.github.afgprojects.framework.ai.core.skill.SkillRegistrar
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Skill {

    /**
     * Skill 名称，在注册表中唯一标识。
     *
     * @return Skill 名称
     */
    String name();

    /**
     * Skill 描述，用于意图分析时 LLM 匹配。
     *
     * @return Skill 描述
     */
    String description() default "";

    /**
     * 意图关键词列表，用于基于关键词的快速匹配。
     * <p>
     * 当用户输入包含任一关键词时，该 Skill 的匹配置信度会提高。
     *
     * @return 意图关键词数组
     */
    String[] intentKeywords() default {};

    /**
     * Skill 分类，用于分组管理。
     *
     * @return 分类名称
     */
    String category() default "";
}
