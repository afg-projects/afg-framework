package io.github.afgprojects.framework.ai.core.properties.model;

import lombok.Data;

/**
 * 模型管理配置。
 */
@Data
public class ModelConfig {

    /**
     * 是否启用模型管理。
     */
    private boolean enabled = true;

    /**
     * 默认模型类型。
     */
    private ModelType defaultType = ModelType.CHAT;
}
