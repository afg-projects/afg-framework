package io.github.afgprojects.framework.ai.core.skill;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.core.api.skill.SkillDefinition;
import io.github.afgprojects.framework.ai.core.api.skill.SkillRegistry;
import io.github.afgprojects.framework.ai.core.entity.skill.UserSkillEntity;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 用户技能加载器
 *
 * <p>在 Spring 上下文刷新完成后（晚于 {@link SkillRegistrar}），从 DB 加载所有
 * enabled 的 {@link UserSkillEntity}，转成 {@link SkillDefinition} 注册进
 * {@link SkillRegistry}，与 {@code @Skill} 注解 skill 并存。</p>
 *
 * <p>name 冲突时 UserSkill 覆盖 @Skill 注解的（用户可定制内置 skill），日志 warn。</p>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
public class UserSkillLoader {

    private final DataManager dataManager;
    private final SkillRegistry skillRegistry;
    private final ObjectMapper objectMapper;

    @EventListener
    public void onContextRefreshed(ContextRefreshedEvent event) {
        try {
            List<UserSkillEntity> skills = dataManager.entity(UserSkillEntity.class)
                .query()
                .where(Conditions.builder(UserSkillEntity.class)
                    .eq(UserSkillEntity::getEnabled, true)
                    .build())
                .list();
            int loaded = 0;
            for (UserSkillEntity entity : skills) {
                try {
                    skillRegistry.register(toDefinition(entity));
                    loaded++;
                } catch (Exception e) {
                    log.warn("UserSkillLoader: failed to register skill {}: {}", entity.getName(), e.getMessage());
                }
            }
            log.info("UserSkillLoader: loaded {} user skill(s) into SkillRegistry", loaded);
        } catch (Exception e) {
            log.warn("UserSkillLoader: failed to load user skills: {}", e.getMessage());
        }
    }

    /** UserSkillEntity → SkillDefinition（JSON 字段反序列化，失败降级为 null） */
    public SkillDefinition toDefinition(UserSkillEntity entity) {
        return new SkillDefinition(
            entity.getName(),
            entity.getDescription() != null ? entity.getDescription() : "",
            entity.getPrompt() != null ? entity.getPrompt() : "",
            parseJson(entity.getInputs(), new TypeReference<List<SkillDefinition.InputParameter>>() {}),
            parseJson(entity.getTools(), new TypeReference<List<String>>() {}),
            parseJson(entity.getDependsOn(), new TypeReference<List<String>>() {}),
            parseJson(entity.getMetadata(), new TypeReference<Map<String, Object>>() {})
        );
    }

    private <T> T parseJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            log.debug("parse JSON failed ({}): {}", typeRef.getType(), e.getMessage());
            return null;
        }
    }
}
