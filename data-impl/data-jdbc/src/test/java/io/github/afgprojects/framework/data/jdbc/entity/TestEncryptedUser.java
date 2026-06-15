package io.github.afgprojects.framework.data.jdbc.entity;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.apt.entity.EncryptedField;
import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 测试用加密实体
 * <p>
 * phone 字段使用 AES-GCM 加密，phone_hash 作为盲索引列。
 */
@Getter
@Setter
@AfEntity
@Table(name = "test_encrypted_user")
public class TestEncryptedUser extends BaseEntity {

    @Column(name = "username")
    private String username;

    @EncryptedField(blindIndexColumn = "phone_hash")
    @Column(name = "phone")
    private String phone;

    @Column(name = "status")
    private Integer status = 1;

    public static TestEncryptedUser create(String username, String phone) {
        TestEncryptedUser user = new TestEncryptedUser();
        user.setUsername(username);
        user.setPhone(phone);
        user.setStatus(1);
        return user;
    }
}
