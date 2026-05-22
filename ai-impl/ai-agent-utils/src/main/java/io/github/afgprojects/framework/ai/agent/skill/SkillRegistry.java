package io.github.afgprojects.framework.ai.agent.skill;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Skill 注册表
 *
 * <p>管理所有已注册的 Skills，支持按名称查找、加载等操作。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface SkillRegistry {

    /**
     * 注册 Skill
     *
     * @param definition Skill 定义
     */
    void register(@NonNull SkillDefinition definition);

    /**
     * 注销 Skill
     *
     * @param name Skill 名称
     * @return 是否成功注销
     */
    boolean unregister(@NonNull String name);

    /**
     * 获取 Skill 定义
     *
     * @param name Skill 名称
     * @return Skill 定义
     */
    @NonNull
    Optional<SkillDefinition> get(@NonNull String name);

    /**
     * 检查 Skill 是否存在
     *
     * @param name Skill 名称
     * @return 是否存在
     */
    boolean exists(@NonNull String name);

    /**
     * 获取所有 Skills
     *
     * @return Skill 列表
     */
    @NonNull
    List<SkillDefinition> getAll();

    /**
     * 获取 Skill 数量
     *
     * @return 数量
     */
    int size();

    /**
     * 清空所有 Skills
     */
    void clear();
}
