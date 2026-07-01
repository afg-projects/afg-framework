package io.github.afgprojects.framework.ai.core.entity.skill;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.SoftDeletable;
import io.github.afgprojects.framework.data.core.entity.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 用户技能实体
 *
 * <p>持久化的 {@link io.github.afgprojects.framework.ai.core.api.skill.SkillDefinition}，
 * 启动时加载进 SkillRegistry，与 {@code @Skill} 注解 skill 并存。用户可通过 API
 * 创建/编辑/删除/启停技能。</p>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AfEntity
@Table(name = "ai_skill")
public class UserSkillEntity extends TenantEntity implements SoftDeletable {

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    /** 技能名（业务键，唯一） */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    /** 提示词模板（TEXT，支持变量引用） */
    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    /** 输入参数定义（List&lt;InputParameter&gt; 序列化为 JSON） */
    @Column(name = "inputs", columnDefinition = "JSON")
    private String inputs;

    /** 关联工具名列表（List&lt;String&gt; 序列化为 JSON） */
    @Column(name = "tools", columnDefinition = "JSON")
    private String tools;

    /** 依赖技能名列表（List&lt;String&gt; 序列化为 JSON） */
    @Column(name = "depends_on", columnDefinition = "JSON")
    private String dependsOn;

    /** 扩展元数据（Map&lt;String,Object&gt; 序列化为 JSON） */
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    /** 是否启用（禁用则不注册进 SkillRegistry） */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
}
