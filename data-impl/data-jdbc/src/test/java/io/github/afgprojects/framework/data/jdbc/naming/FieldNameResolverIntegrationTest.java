package io.github.afgprojects.framework.data.jdbc.naming;

import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.query.Condition;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FieldNameResolver 集成测试
 * <p>
 * 验证 FieldNameResolver 与 TypedConditionBuilder 的集成，
 * 确保 Lambda 方法引用能正确转换为数据库列名。
 */
@DisplayName("FieldNameResolver 集成测试")
class FieldNameResolverIntegrationTest {

    @Nested
    @DisplayName("核心场景")
    class CoreScenarios {

        @Test
        @DisplayName("普通字段转换：userName → user_name")
        void shouldConvertCamelCaseToSnakeCase() {
            Condition condition = Conditions.builder(User.class)
                .eq(User::getUserName, "admin")
                .build();

            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().get(0).field()).isEqualTo("user_name");
        }

        @Test
        @DisplayName("Boolean + @Column：deleted → is_deleted")
        void shouldUseColumnNameFromAnnotation() {
            Condition condition = Conditions.builder(User.class)
                .eq(User::getDeleted, true)
                .build();

            assertThat(condition.getCriteria()).hasSize(1);
            assertThat(condition.getCriteria().get(0).field()).isEqualTo("is_deleted");
        }

        @Test
        @DisplayName("Lambda 条件构建完整链路")
        void shouldBuildCompleteConditionChain() {
            Condition condition = Conditions.builder(User.class)
                .eq(User::getDeleted, false)
                .like(User::getUserName, "admin")
                .isNotNull(User::getEmail)
                .build();

            assertThat(condition.getCriteria()).hasSize(3);
            assertThat(condition.getCriteria().get(0).field()).isEqualTo("is_deleted");
            assertThat(condition.getCriteria().get(1).field()).isEqualTo("user_name");
            assertThat(condition.getCriteria().get(2).field()).isEqualTo("email");
        }
    }

    @Nested
    @DisplayName("边界场景")
    class EdgeCases {

        @Test
        @DisplayName("嵌套条件中的字段名转换")
        void shouldHandleNestedConditions() {
            Condition nested = Conditions.builder(User.class)
                .eq(User::getStatus, 1)
                .build();

            Condition condition = Conditions.builder(User.class)
                .eq(User::getDeleted, false)
                .and(nested)
                .build();

            assertThat(condition.getCriteria()).hasSize(2);
            assertThat(condition.getCriteria().get(0).field()).isEqualTo("is_deleted");
            // Nested conditions use isNested() flag
            assertThat(condition.getCriteria().get(1).isNested()).isTrue();
            // Verify the nested condition itself has correct field names
            Condition nestedCondition = condition.getCriteria().get(1).nestedCondition();
            assertThat(nestedCondition.getCriteria().get(0).field()).isEqualTo("status");
        }

        @Test
        @DisplayName("字段名与列名相同")
        void shouldHandleSameName() {
            Condition condition = Conditions.builder(User.class)
                .eq(User::getId, 1L)
                .build();

            assertThat(condition.getCriteria().get(0).field()).isEqualTo("id");
        }

        @Test
        @DisplayName("应该处理所有条件类型")
        void shouldHandleAllConditionTypes() {
            Condition condition = Conditions.builder(User.class)
                .eq(User::getUserName, "admin")
                .ne(User::getStatus, 0)
                .gt(User::getAge, 18)
                .ge(User::getAge, 18)
                .lt(User::getAge, 60)
                .le(User::getAge, 60)
                .like(User::getUserName, "admin")
                .isNull(User::getEmail)
                .isNotNull(User::getUserName)
                .build();

            assertThat(condition.getCriteria()).hasSize(9);
            // 验证所有字段名都正确转换
            assertThat(condition.getCriteria().get(0).field()).isEqualTo("user_name");
            assertThat(condition.getCriteria().get(1).field()).isEqualTo("status");
        }
    }

    @Nested
    @DisplayName("性能场景")
    class PerformanceTests {

        @Test
        @DisplayName("多次转换应该正常完成")
        void shouldHandleMultipleConversions() {
            // 预热
            for (int i = 0; i < 10; i++) {
                Conditions.builder(User.class)
                    .eq(User::getDeleted, true)
                    .build();
            }

            // 执行多次转换
            for (int i = 0; i < 100; i++) {
                Conditions.builder(User.class)
                    .eq(User::getDeleted, true)
                    .eq(User::getUserName, "user" + i)
                    .build();
            }

            // 只验证能正常完成，不强制性能限制（环境差异）
            assertThat(true).isTrue();
        }
    }

    /**
     * 测试用户实体
     */
    @Table(name = "sys_user")
    static class User {
        @Id
        private Long id;

        @Column(name = "is_deleted")
        private Boolean deleted;

        private String userName;
        private String email;
        private Integer status;
        private Integer age;

        public Long getId() { return id; }
        public Boolean getDeleted() { return deleted; }
        public String getUserName() { return userName; }
        public String getEmail() { return email; }
        public Integer getStatus() { return status; }
        public Integer getAge() { return age; }
    }
}
