package io.github.afgprojects.framework.ai.core.dto.model;

import lombok.Data;

@Data
public class UpdateProviderRequest {

    private String providerName;

    private String providerType;

    private String baseUrl;

    private String apiKey;

    private Boolean enabled;

    private String config;
}
