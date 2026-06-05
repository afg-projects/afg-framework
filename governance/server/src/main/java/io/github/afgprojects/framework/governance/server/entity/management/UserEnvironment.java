package io.github.afgprojects.framework.governance.server.entity.management;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户环境关联表
 */
@Getter
@Setter
@AfEntity
@Entity
@Table(name = "gov_user_environment", indexes = {
    @Index(name = "idx_user_env_user", columnList = "user_id"),
    @Index(name = "idx_user_env_environment", columnList = "environment_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_environment", columnNames = {"user_id", "environment_id"})
})
public class UserEnvironment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "environment_id", nullable = false)
    private Long environmentId;
}
