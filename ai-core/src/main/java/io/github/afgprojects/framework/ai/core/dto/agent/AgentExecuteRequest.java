package io.github.afgprojects.framework.ai.core.dto.agent;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Agent 执行请求
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Data
public class AgentExecuteRequest {

    @NotBlank(message = "用户输入不能为空")
    private String userInput;

    private boolean stream = false;
}
