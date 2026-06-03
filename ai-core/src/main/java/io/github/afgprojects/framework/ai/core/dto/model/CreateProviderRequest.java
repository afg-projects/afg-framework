package io.github.afgprojects.framework.ai.core.dto.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateProviderRequest {

    @NotBlank(message = "供应商名称不能为空")
    private String providerName;

    @NotBlank(message = "供应商类型不能为空")
    private String providerType;

    private String baseUrl;

    private String apiKey;

    private Boolean enabled = true;

    private String config;
}
