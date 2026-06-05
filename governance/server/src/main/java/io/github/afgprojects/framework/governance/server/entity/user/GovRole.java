package io.github.afgprojects.framework.governance.server.entity.user;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.FullEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 角色实体
 */
@Getter
@Setter
@AfEntity
@Entity
@Table(name = "gov_role", indexes = {
    @Index(name = "idx_role_code", columnList = "code", unique = true)
})
public class GovRole extends FullEntity {

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "status")
    private Integer status = 1;
}
