package io.github.afgprojects.framework.governance.server.entity.config;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.FullEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AfEntity
@Entity
@Table(name = "gov_config_item", indexes = {
    @Index(name = "idx_config_item_code", columnList = "code")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_config_item_group_code", columnNames = {"group_id", "code"})
})
public class ConfigItem extends FullEntity {

    @Column(name = "group_id", nullable = false)
    private String groupId;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "type", nullable = false, length = 20)
    private String type = "STRING";

    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;

    @Column(name = "options", columnDefinition = "TEXT")
    private String options;

    @Column(name = "validation", columnDefinition = "TEXT")
    private String validation;

    @Column(name = "placeholder", length = 200)
    private String placeholder;

    @Column(name = "is_secret")
    private Boolean isSecret = false;

    @Column(name = "is_required")
    private Boolean isRequired = false;

    @Column(name = "is_dynamic")
    private Boolean isDynamic = true;

    @Column(name = "is_deprecated")
    private Boolean isDeprecated = false;

    @Column(name = "sort")
    private Integer sort = 0;

    @Column(name = "status")
    private Integer status = 1;
}
