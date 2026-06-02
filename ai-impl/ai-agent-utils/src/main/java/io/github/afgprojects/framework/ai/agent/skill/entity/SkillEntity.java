package io.github.afgprojects.framework.ai.agent.skill.entity;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * 技能实体
 * <p>
 * 存储已注册技能的元数据信息，支持持久化技能注册和管理。
 * 配合 {@link io.github.afgprojects.framework.ai.agent.skill.PersistentSkillRegistry} 使用，
 * 实现基于数据库的 Skill 注册表。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Getter
@Setter
@AfEntity
@Table(name = "ai_skill")
public class SkillEntity {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    /**
     * 技能名称（唯一）
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 技能描述
     */
    @Column(name = "description", length = 500)
    private @Nullable String description;

    /**
     * 技能类型/分类
     */
    @Column(name = "type", length = 50)
    private @Nullable String type;

    /**
     * 提示词模板
     */
    @Column(name = "prompt_template", columnDefinition = "TEXT")
    private @Nullable String promptTemplate;

    /**
     * 输入参数定义（JSON Schema 格式）
     */
    @Column(name = "input_parameters", columnDefinition = "TEXT")
    private @Nullable String inputParameters;

    /**
     * 输出格式定义（JSON Schema 格式）
     */
    @Column(name = "output_format", columnDefinition = "TEXT")
    private @Nullable String outputFormat;

    /**
     * 技能状态：ENABLED, DISABLED
     */
    @Column(name = "status", length = 20)
    private String status = "ENABLED";

    /**
     * 可用的工具列表（JSON 数组格式）
     */
    @Column(name = "tools", columnDefinition = "TEXT")
    private @Nullable String tools;

    /**
     * 依赖的其他 skills（JSON 数组格式）
     */
    @Column(name = "depends_on", columnDefinition = "TEXT")
    private @Nullable String dependsOn;

    /**
     * 扩展元数据（JSON 格式）
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private @Nullable String metadata;

    /**
     * 创建人
     */
    @Column(name = "created_by", length = 100)
    private @Nullable String createdBy;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private @Nullable LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private @Nullable LocalDateTime updatedAt;

    /**
     * 删除标记
     */
    @Column(name = "deleted")
    private Boolean deleted = false;

    /**
     * 判断是否为新建实体（未持久化）
     *
     * @return true 表示新建实体
     */
    public boolean isNew() {
        return id == null;
    }
}
