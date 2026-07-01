package io.github.afgprojects.framework.governance.server.dto.config;

import io.github.afgprojects.framework.governance.server.entity.config.ConfigItem;
import lombok.Data;

/**
 * 配置项响应 DTO
 * 包含配置项元数据和当前生效值
 */
@Data
@SuppressWarnings("PMD.TooManyFields")
public class ConfigItemResponse {

    private String id;
    private String groupId;
    private String code;
    private String name;
    private String description;
    private String type;
    private String defaultValue;
    private String currentValue;  // 当前生效值
    private String options;
    private String validation;
    private String placeholder;
    private Boolean isSecret;
    private Boolean isRequired;
    private Boolean isDynamic;
    private Boolean isDeprecated;
    private Integer sort;
    private Integer status;

    /**
     * 从实体创建响应
     */
    public static ConfigItemResponse fromEntity(ConfigItem item, String currentValue) {
        ConfigItemResponse response = new ConfigItemResponse();
        response.setId(item.getId());
        response.setGroupId(item.getGroupId());
        response.setCode(item.getCode());
        response.setName(item.getName());
        response.setDescription(item.getDescription());
        response.setType(item.getType());
        response.setDefaultValue(item.getDefaultValue());
        response.setCurrentValue(currentValue);
        response.setOptions(item.getOptions());
        response.setValidation(item.getValidation());
        response.setPlaceholder(item.getPlaceholder());
        response.setIsSecret(item.getIsSecret());
        response.setIsRequired(item.getIsRequired());
        response.setIsDynamic(item.getIsDynamic());
        response.setIsDeprecated(item.getIsDeprecated());
        response.setSort(item.getSort());
        response.setStatus(item.getStatus());
        return response;
    }
}
