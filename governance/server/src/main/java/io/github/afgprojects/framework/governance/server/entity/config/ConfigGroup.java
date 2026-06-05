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
@Table(name = "gov_config_group", indexes = {
    @Index(name = "idx_config_group_code", columnList = "code")
})
public class ConfigGroup extends FullEntity {

    @Column(name = "environment_id")
    private Long environmentId;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "sort")
    private Integer sort = 0;

    @Column(name = "status")
    private Integer status = 1;
}
