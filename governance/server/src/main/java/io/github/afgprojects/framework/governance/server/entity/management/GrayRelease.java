package io.github.afgprojects.framework.governance.server.entity.management;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.FullEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 灰度发布实体
 */
@Getter
@Setter
@AfEntity
@Entity
@Table(name = "gov_gray_release", indexes = {
    @Index(name = "idx_gray_config_item", columnList = "config_item_id"),
    @Index(name = "idx_gray_environment", columnList = "environment_id")
})
public class GrayRelease extends FullEntity {

    @Column(name = "config_item_id", nullable = false)
    private String configItemId;

    @Column(name = "environment_id", nullable = false)
    private String environmentId;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "gray_instances", columnDefinition = "TEXT")
    private String grayInstances;

    @Column(name = "status")
    private Integer status = 1;

    @Column(name = "created_by", length = 100)
    private String createdBy;
}
