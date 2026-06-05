package io.github.afgprojects.framework.governance.server.entity.user;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.FullEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 治理中心用户实体
 */
@Getter
@Setter
@AfEntity
@Entity
@Table(name = "sys_user", indexes = {
    @Index(name = "idx_user_username", columnList = "username", unique = true),
    @Index(name = "idx_user_mobile", columnList = "mobile"),
    @Index(name = "idx_user_email", columnList = "email")
})
public class GovUser extends FullEntity {

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    @Column(name = "real_name", length = 50)
    private String realName;

    @Column(name = "mobile", length = 20)
    private String mobile;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "avatar", length = 200)
    private String avatar;

    @Column(name = "user_type", length = 20)
    private String userType = "user";

    @Column(name = "status")
    private Integer status = 1;
}
