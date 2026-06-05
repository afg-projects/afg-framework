package io.github.afgprojects.framework.governance.server.entity.management;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.FullEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 环境实体类
 *
 * @author afg-projects
 */
@Getter
@Setter
@AfEntity
@Entity
@Table(name = "gov_environment", indexes = {
        @Index(name = "idx_gov_environment_code", columnList = "code")
})
public class Environment extends FullEntity {

    /**
     * 环境编码
     */
    @Column(name = "code", nullable = false, length = 50, unique = true)
    private String code;

    /**
     * 环境名称
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 环境描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 排序
     */
    @Column(name = "sort")
    private Integer sort = 0;

    /**
     * 状态（0-禁用，1-启用）
     */
    @Column(name = "status")
    private Integer status = 1;
}
