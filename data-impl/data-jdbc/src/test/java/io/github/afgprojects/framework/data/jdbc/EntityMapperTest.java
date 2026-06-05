package io.github.afgprojects.framework.data.jdbc;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import io.github.afgprojects.framework.data.jdbc.test.BaseDataTest;
import io.github.afgprojects.framework.data.jdbc.entity.TestUser;

/**
 * EntityMapper 集成测试
 * <p>
 * 使用 PostgreSQL Testcontainers + Liquibase 迁移。
 * 测试 ResultSet 到实体的映射，包括类型转换、snake_case 到 camelCase 转换等。
 * </p>
 */
class EntityMapperTest extends BaseDataTest {

    @Nested
    @DisplayName("基本映射")
    class BasicMapping {

        @Test
        @DisplayName("should map all fields when save and retrieve entity")
        void shouldMapAllFields_whenSaveAndRetrieveEntity() {
            TestUser user = new TestUser();
            user.setUsername("mapper-test");
            user.setEmail("mapper@test.com");
            user.setStatus(1);

            TestUser saved = dataManager.save(TestUser.class, user);
            TestUser found = dataManager.findById(TestUser.class, saved.getId()).orElseThrow();

            assertThat(found.getUsername()).isEqualTo("mapper-test");
            assertThat(found.getEmail()).isEqualTo("mapper@test.com");
            assertThat(found.getStatus()).isEqualTo(1);
            assertThat(found.getCreatedAt()).isNotNull();
            assertThat(found.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should handle null fields when entity has null values")
        void shouldHandleNullFields_whenEntityHasNullValues() {
            TestUser user = new TestUser();
            user.setUsername("null-fields");
            // email 和 status 使用默认值

            TestUser saved = dataManager.save(TestUser.class, user);
            TestUser found = dataManager.findById(TestUser.class, saved.getId()).orElseThrow();

            assertThat(found.getUsername()).isEqualTo("null-fields");
        }
    }
}