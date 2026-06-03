package io.github.afgprojects.framework.ai.core.dto.workflow;

import lombok.Data;

import java.util.Map;

/**
 * 工作流执行请求
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Data
public class WorkflowExecuteRequest {

    private Map<String, Object> inputs;

    private boolean stream = false;
}
