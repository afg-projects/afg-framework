package io.github.afgprojects.framework.ai.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Skill 配置属性
 *
 * @author afg-projects
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "afg.ai.skill")
public class SkillProperties {

    /**
     * Skills 文件目录路径（支持 classpath: 前缀）
     */
    private String skillsPath;

    /**
     * 是否启用 Skill 自动加载
     */
    private boolean enabled = true;

    public String getSkillsPath() {
        return skillsPath;
    }

    public void setSkillsPath(String skillsPath) {
        this.skillsPath = skillsPath;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
