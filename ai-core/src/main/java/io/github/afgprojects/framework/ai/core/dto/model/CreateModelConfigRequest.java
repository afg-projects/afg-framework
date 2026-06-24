package io.github.afgprojects.framework.ai.core.dto.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateModelConfigRequest {

    @NotNull(message = "供应商ID不能为空")
    private String providerId;

    @NotBlank(message = "模型名称不能为空")
    private String modelName;

    private String displayName;

    @NotBlank(message = "模型类型不能为空")
    private String modelType;

    private String capabilities;

    private String config;

    private Boolean enabled = true;
}
