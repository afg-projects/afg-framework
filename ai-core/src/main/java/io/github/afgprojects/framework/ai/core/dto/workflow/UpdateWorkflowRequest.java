package io.github.afgprojects.framework.ai.core.dto.workflow;

import lombok.Data;

/**
 * 更新工作流定义请求
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Data
public class UpdateWorkflowRequest {

    private String name;

    private String description;

    private String dslContent;

    private String version;

    private String status;

    private Long applicationId;
}
