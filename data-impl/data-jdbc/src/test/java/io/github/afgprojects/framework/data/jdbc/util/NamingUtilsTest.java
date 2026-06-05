package io.github.afgprojects.framework.data.jdbc.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NamingUtils 单元测试
 */
@DisplayName("NamingUtils 测试")
class NamingUtilsTest {

    @Nested
    @DisplayName("columnNameToFieldName")
    class ColumnNameToFieldNameTests {

        @Test
        @DisplayName("下划线命名转驼峰")
        void shouldConvertSnakeCaseToCamelCase() {
            assertThat(NamingUtils.columnNameToFieldName("user_name")).isEqualTo("userName");
        }

        @Test
        @DisplayName("单列名直接返回小写")
        void shouldReturnLowercaseForSingleWord() {
            assertThat(NamingUtils.columnNameToFieldName("name")).isEqualTo("name");
        }

        @Test
        @DisplayName("多段下划线命名转驼峰")
        void shouldConvertMultiSegmentSnakeCase() {
            assertThat(NamingUtils.columnNameToFieldName("user_first_name")).isEqualTo("userFirstName");
        }

        @Test
        @DisplayName("全大写列名转小写驼峰")
        void shouldConvertUppercaseToCamelCase() {
            assertThat(NamingUtils.columnNameToFieldName("USER_NAME")).isEqualTo("userName");
        }

        @Test
        @DisplayName("混合大小写下划线命名转驼峰")
        void shouldConvertMixedCaseSnakeCase() {
            assertThat(NamingUtils.columnNameToFieldName("User_Name")).isEqualTo("userName");
        }

        @Test
        @DisplayName("is_xxx 列名遵循阿里巴巴布尔约定转 isXxx")
        void shouldConvertIsPrefixColumnToIsXxx() {
            // 阿里巴巴约定：数据库布尔字段 is_xxx → Java 属性 isXxx（而非 xxx）
            assertThat(NamingUtils.columnNameToFieldName("is_active")).isEqualTo("isActive");
        }

        @Test
        @DisplayName("is_ 前缀多段下划线转驼峰")
        void shouldConvertIsPrefixMultiSegmentToCamelCase() {
            assertThat(NamingUtils.columnNameToFieldName("is_user_active")).isEqualTo("isUserActive");
        }

        @Test
        @DisplayName("id 列名保持不变")
        void shouldKeepIdAsIs() {
            assertThat(NamingUtils.columnNameToFieldName("id")).isEqualTo("id");
        }

        @Test
        @DisplayName("created_at 转为 createdAt")
        void shouldConvertCreatedAt() {
            assertThat(NamingUtils.columnNameToFieldName("created_at")).isEqualTo("createdAt");
        }
    }

    @Nested
    @DisplayName("fieldNameToColumnName")
    class FieldNameToColumnNameTests {

        @Test
        @DisplayName("驼峰转下划线命名")
        void shouldConvertCamelCaseToSnakeCase() {
            assertThat(NamingUtils.fieldNameToColumnName("userName")).isEqualTo("user_name");
        }

        @Test
        @DisplayName("单字段名直接返回小写")
        void shouldReturnLowercaseForSingleWord() {
            assertThat(NamingUtils.fieldNameToColumnName("name")).isEqualTo("name");
        }

        @Test
        @DisplayName("多段驼峰转下划线命名")
        void shouldConvertMultiSegmentCamelCase() {
            assertThat(NamingUtils.fieldNameToColumnName("userFirstName")).isEqualTo("user_first_name");
        }

        @Test
        @DisplayName("布尔字段 isXxx 转 is_xxx")
        void shouldConvertBooleanIsPrefixToSnakeCase() {
            // 阿里巴巴约定：Java 布尔属性 isXxx → 数据库字段 is_xxx
            assertThat(NamingUtils.fieldNameToColumnName("isActive")).isEqualTo("is_active");
        }

        @Test
        @DisplayName("is 前缀多段驼峰转下划线")
        void shouldConvertIsPrefixMultiSegmentToSnakeCase() {
            assertThat(NamingUtils.fieldNameToColumnName("isUserActive")).isEqualTo("is_user_active");
        }

        @Test
        @DisplayName("id 字段名保持不变")
        void shouldKeepIdAsIs() {
            assertThat(NamingUtils.fieldNameToColumnName("id")).isEqualTo("id");
        }

        @Test
        @DisplayName("createdAt 转为 created_at")
        void shouldConvertCreatedAt() {
            assertThat(NamingUtils.fieldNameToColumnName("createdAt")).isEqualTo("created_at");
        }

        @Test
        @DisplayName("连续大写字母处理")
        void shouldHandleConsecutiveUppercase() {
            assertThat(NamingUtils.fieldNameToColumnName("httpUrl")).isEqualTo("http_url");
        }
    }

    @Nested
    @DisplayName("inferTableName")
    class InferTableNameTests {

        @Test
        @DisplayName("简单类名推断表名")
        void shouldInferTableNameFromSimpleClassName() {
            assertThat(NamingUtils.inferTableName(TestUser.class)).isEqualTo("test_user");
        }

        @Test
        @DisplayName("驼峰类名推断下划线表名")
        void shouldInferSnakeCaseTableNameFromCamelCaseClass() {
            assertThat(NamingUtils.inferTableName(TestUserProfile.class)).isEqualTo("test_user_profile");
        }

        @Test
        @DisplayName("多段驼峰类名推断表名")
        void shouldInferTableNameFromMultiSegmentCamelCase() {
            assertThat(NamingUtils.inferTableName(TestSystemUserRole.class)).isEqualTo("test_system_user_role");
        }

        @Test
        @DisplayName("单字类名推断表名")
        void shouldInferTableNameFromSingleWordClass() {
            assertThat(NamingUtils.inferTableName(SimpleEntity.class)).isEqualTo("simple_entity");
        }

        @Test
        @DisplayName("User 类推断表名")
        void shouldInferTableNameFromUserClass() {
            assertThat(NamingUtils.inferTableName(User.class)).isEqualTo("user");
        }

        @Test
        @DisplayName("OrderItem 类推断表名")
        void shouldInferTableNameFromOrderItemClass() {
            assertThat(NamingUtils.inferTableName(OrderItem.class)).isEqualTo("order_item");
        }
    }

    // 测试用的简单类
    static class TestUser {}
    static class TestUserProfile {}
    static class TestSystemUserRole {}
    static class SimpleEntity {}
    static class User {}
    static class OrderItem {}
}
