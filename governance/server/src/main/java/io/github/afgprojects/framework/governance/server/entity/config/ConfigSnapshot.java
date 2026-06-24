package io.github.afgprojects.framework.governance.server.entity.config;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AfEntity
@Entity
@Table(name = "gov_config_snapshot", indexes = {
    @Index(name = "idx_config_snapshot_tag", columnList = "tag"),
    @Index(name = "idx_config_snapshot_group", columnList = "group_id")
})
public class ConfigSnapshot extends TenantEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "tag", length = 50)
    private String tag;

    @Column(name = "data", columnDefinition = "LONGTEXT")
    private String data;

    @Column(name = "group_id")
    private String groupId;

    @Column(name = "creator_id")
    private String creatorId;

    @Column(name = "creator_name", length = 50)
    private String creatorName;
}
