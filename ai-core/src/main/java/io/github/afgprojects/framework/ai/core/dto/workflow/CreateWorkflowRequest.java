package io.github.afgprojects.framework.ai.core.dto.workflow;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建工作流定义请求
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Data
public class CreateWorkflowRequest {

    @NotBlank(message = "工作流名称不能为空")
    private String name;

    private String description;

    @NotBlank(message = "DSL 内容不能为空")
    private String dslContent;

    private String version;

    private String status = "DRAFT";

    private String applicationId;
}
