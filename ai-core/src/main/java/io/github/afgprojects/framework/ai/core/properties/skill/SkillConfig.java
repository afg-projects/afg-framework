package io.github.afgprojects.framework.ai.core.properties.skill;

import lombok.Data;

/**
 * 技能系统配置。
 */
@Data
public class SkillConfig {

    /**
     * 是否启用技能系统。
     */
    private boolean enabled = true;

    /**
     * 是否启用持久化技能注册。
     */
    private boolean persistentEnabled = false;
}
