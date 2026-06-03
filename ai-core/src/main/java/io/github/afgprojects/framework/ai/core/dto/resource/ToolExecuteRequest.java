package io.github.afgprojects.framework.ai.core.dto.resource;

import lombok.Data;

import java.util.Map;

/**
 * 工具执行请求
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Data
public class ToolExecuteRequest {

    private Map<String, Object> parameters;
}
