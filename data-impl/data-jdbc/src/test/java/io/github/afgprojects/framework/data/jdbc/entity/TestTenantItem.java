package io.github.afgprojects.framework.data.jdbc.entity;

import io.github.afgprojects.framework.data.core.entity.LifecycleCallbacks;
import io.github.afgprojects.framework.data.core.entity.TenantEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 测试用多租户实体
 */
@Getter
@Setter
public class TestTenantItem extends TenantEntity implements LifecycleCallbacks {

    private String name;
    private Integer quantity;

    public static TestTenantItem create(String name, int quantity, String tenantId) {
        TestTenantItem item = new TestTenantItem();
        item.setName(name);
        item.setQuantity(quantity);
        item.setTenantId(tenantId);
        return item;
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
