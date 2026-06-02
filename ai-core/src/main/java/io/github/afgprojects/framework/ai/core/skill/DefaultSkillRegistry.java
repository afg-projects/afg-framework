package io.github.afgprojects.framework.ai.core.skill;

import io.github.afgprojects.framework.ai.core.api.skill.SkillDefinition;
import io.github.afgprojects.framework.ai.core.api.skill.SkillRegistry;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认 SkillRegistry 实现
 *
 * <p>基于内存的 Skill 注册表，支持动态注册和注销。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class DefaultSkillRegistry implements SkillRegistry {

    private final Map<String, SkillDefinition> skills = new ConcurrentHashMap<>();

    /**
     * 创建空注册表
     */
    public DefaultSkillRegistry() {
    }

    /**
     * 创建带初始 Skill 的注册表
     *
     * @param skills 初始 Skill 列表
     */
    public DefaultSkillRegistry(@NonNull Collection<SkillDefinition> skills) {
        for (SkillDefinition skill : skills) {
            this.skills.put(skill.name(), skill);
        }
    }

    @Override
    public void register(@NonNull SkillDefinition definition) {
        skills.put(definition.name(), definition);
        log.info("Registered skill: {}", definition.name());
    }

    @Override
    public boolean unregister(@NonNull String name) {
        boolean removed = skills.remove(name) != null;
        if (removed) {
            log.info("Unregistered skill: {}", name);
        }
        return removed;
    }

    @Override
    @NonNull
    public Optional<SkillDefinition> get(@NonNull String name) {
        return Optional.ofNullable(skills.get(name));
    }

    @Override
    public boolean exists(@NonNull String name) {
        return skills.containsKey(name);
    }

    @Override
    @NonNull
    public List<SkillDefinition> getAll() {
        return List.copyOf(skills.values());
    }

    @Override
    public int size() {
        return skills.size();
    }

    @Override
    public void clear() {
        skills.clear();
        log.info("Cleared all skills");
    }
}