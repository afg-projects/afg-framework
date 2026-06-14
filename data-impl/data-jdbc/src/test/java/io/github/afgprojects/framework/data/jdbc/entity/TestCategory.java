package io.github.afgprojects.framework.data.jdbc.entity;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.LifecycleCallbacks;
import io.github.afgprojects.framework.data.core.entity.TreeEntity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 测试用树形结构实体（分类/部门等层级结构）
 */
@Getter
@Setter
@AfEntity
@Table(name = "test_category")
public class TestCategory extends TreeEntity<TestCategory> implements LifecycleCallbacks {

    private String name;
    private String description;

    public static TestCategory create(String name, String description) {
        TestCategory category = new TestCategory();
        category.setName(name);
        category.setDescription(description);
        return category;
    }

    public static TestCategory create(String name, String description, Long parentId) {
        TestCategory category = create(name, description);
        category.setParentId(parentId);
        return category;
    }

    @Override
    public void beforeCreate() {
        setCreatedAt(Instant.now());
        setUpdatedAt(Instant.now());
    }

    @Override
    public void beforeUpdate() {
        setUpdatedAt(Instant.now());
    }
}
