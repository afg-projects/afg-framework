package io.github.afgprojects.framework.ai.core.entity.workflow;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.SoftDeletable;
import io.github.afgprojects.framework.data.core.entity.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI 工作流定义实体
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AfEntity
@Table(name = "ai_workflow_definition")
public class WorkflowDefinitionEntity extends TenantEntity implements SoftDeletable {

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "dsl_content", columnDefinition = "JSON")
    private String dslContent;

    @Column(name = "version", length = 50)
    private String version;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "application_id")
    private String applicationId;

    @Column(name = "user_id", length = 64)
    private String userId;
}
