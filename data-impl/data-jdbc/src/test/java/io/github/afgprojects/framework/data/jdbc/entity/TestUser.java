package io.github.afgprojects.framework.data.jdbc.entity;

import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import io.github.afgprojects.framework.data.core.entity.LifecycleCallbacks;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 测试用基本实体
 */
@Getter
@Setter
public class TestUser extends BaseEntity implements LifecycleCallbacks {

    private String username;
    private String email;
    private Integer status = 1;

    public static TestUser create(String username, String email) {
        TestUser user = new TestUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setStatus(1);
        return user;
    }

    public static TestUser create(String username, String email, int status) {
        TestUser user = create(username, email);
        user.setStatus(status);
        return user;
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
