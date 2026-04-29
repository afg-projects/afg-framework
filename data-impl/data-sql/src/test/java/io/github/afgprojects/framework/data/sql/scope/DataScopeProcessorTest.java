package io.github.afgprojects.framework.data.sql.scope;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataScopeProcessor 测试
 */
@DisplayName("DataScopeProcessor Tests")
class DataScopeProcessorTest {

    @Nested
    @DisplayName("resolvePlaceholders Tests")
    class ResolvePlaceholdersTests {

        @Test
        @DisplayName("解析 #{currentUserId} 占位符")
        void testResolveCurrentUserId() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                    .userId(123L)
                    .build();

            DataScopeContextProvider provider = () -> context;
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            assertThat(processor.isPlaceholder("#{currentUserId}")).isTrue();
            assertThat(processor.resolvePlaceholders("user_id = #{currentUserId}"))
                    .isEqualTo("user_id = 123");
        }

        @Test
        @DisplayName("解析 #{currentDeptId} 占位符")
        void testResolveCurrentDeptId() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                    .deptId(456L)
                    .build();

            DataScopeContextProvider provider = () -> context;
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            assertThat(processor.resolvePlaceholders("dept_id = #{currentDeptId}"))
                    .isEqualTo("dept_id = 456");
        }

        @Test
        @DisplayName("解析 #{currentUserDeptAndChildIds} 占位符")
        void testResolveDeptIds() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                    .accessibleDeptIds(Set.of(1L, 2L, 3L))
                    .build();

            DataScopeContextProvider provider = () -> context;
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            String result = processor.resolvePlaceholders("dept_id IN (#{currentUserDeptAndChildIds})");
            assertThat(result).contains("1").contains("2").contains("3");
        }

        @Test
        @DisplayName("解析 #{currentUserDeptIds} 占位符")
        void testResolveCurrentUserDeptIds() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                    .accessibleDeptIds(Set.of(10L, 20L))
                    .build();

            DataScopeContextProvider provider = () -> context;
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            String result = processor.resolvePlaceholders("dept_id IN (#{currentUserDeptIds})");
            assertThat(result).contains("10").contains("20");
        }

        @Test
        @DisplayName("解析 #{currentTenantId} 占位符")
        void testResolveTenantId() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                    .tenantId(789L)
                    .build();

            DataScopeContextProvider provider = () -> context;
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            assertThat(processor.resolvePlaceholders("tenant_id = #{currentTenantId}"))
                    .isEqualTo("tenant_id = 789");
        }

        @Test
        @DisplayName("解析多个占位符")
        void testResolveMultiplePlaceholders() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                    .userId(123L)
                    .deptId(456L)
                    .tenantId(789L)
                    .build();

            DataScopeContextProvider provider = () -> context;
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            String sql = "user_id = #{currentUserId} AND dept_id = #{currentDeptId} AND tenant_id = #{currentTenantId}";
            assertThat(processor.resolvePlaceholders(sql))
                    .isEqualTo("user_id = 123 AND dept_id = 456 AND tenant_id = 789");
        }

        @Test
        @DisplayName("处理空上下文")
        void testEmptyContext() {
            DataScopeContextProvider provider = () -> DataScopeUserContext.empty();
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            // 当上下文为空时，占位符保持原样（因为无法获取值）
            String result = processor.resolvePlaceholders("user_id = #{currentUserId}");
            assertThat(result).isEqualTo("user_id = #{currentUserId}");
        }

        @Test
        @DisplayName("处理 null 上下文")
        void testNullContext() {
            DataScopeContextProvider provider = () -> null;
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            String result = processor.resolvePlaceholders("user_id = #{currentUserId}");
            assertThat(result).isEqualTo("user_id = #{currentUserId}");
        }

        @Test
        @DisplayName("currentUserDeptIds with empty accessibleDeptIds and null deptId returns null replacement")
        void testResolvePlaceholdersDeptIdsNullDeptId() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                    .accessibleDeptIds(Set.of())
                    .build();

            DataScopeContextProvider provider = () -> context;
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            // 当 accessibleDeptIds 为空且 deptId 为 null 时，resolvePlaceholderValue 返回 null
            // 占位符保持原样
            String result = processor.resolvePlaceholders("dept_id IN (#{currentUserDeptIds})");
            assertThat(result).isEqualTo("dept_id IN (#{currentUserDeptIds})");
        }

        @Test
        @DisplayName("currentUserDeptAndChildIds with empty accessibleDeptIds and null deptId returns null replacement")
        void testResolvePlaceholdersDeptAndChildIdsNullDeptId() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                    .accessibleDeptIds(Set.of())
                    .build();

            DataScopeContextProvider provider = () -> context;
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            String result = processor.resolvePlaceholders("dept_id IN (#{currentUserDeptAndChildIds})");
            assertThat(result).isEqualTo("dept_id IN (#{currentUserDeptAndChildIds})");
        }

        @Test
        @DisplayName("不解析未知占位符")
        void testUnknownPlaceholder() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                    .userId(123L)
                    .build();

            DataScopeContextProvider provider = () -> context;
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            String result = processor.resolvePlaceholders("column = #{unknownPlaceholder}");
            assertThat(result).isEqualTo("column = #{unknownPlaceholder}");
        }
    }

    @Nested
    @DisplayName("resolvePlaceholder Expression Tests")
    class ResolvePlaceholderExpressionTests {

        @Test
        @DisplayName("resolvePlaceholder 返回 LongValue for currentUserId")
        void testResolvePlaceholderUserId() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                    .userId(123L)
                    .build();

            DataScopeContextProvider provider = () -> context;
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            Expression result = processor.resolvePlaceholder("#{currentUserId}");
            assertThat(result).isInstanceOf(LongValue.class);
            assertThat(((LongValue) result).getValue()).isEqualTo(123L);
        }

        @Test
        @DisplayName("resolvePlaceholder 返回 LongValue for currentDeptId")
        void testResolvePlaceholderDeptId() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                    .deptId(456L)
                    .build();

            DataScopeContextProvider provider = () -> context;
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            Expression result = processor.resolvePlaceholder("#{currentDeptId}");
            assertThat(result).isInstanceOf(LongValue.class);
            assertThat(((LongValue) result).getValue()).isEqualTo(456L);
        }

        @Test
        @DisplayName("resolvePlaceholder 返回 ParenthesedExpressionList for deptIds")
        void testResolvePlaceholderDeptIds() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                    .accessibleDeptIds(Set.of(1L, 2L, 3L))
                    .build();

            DataScopeContextProvider provider = () -> context;
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            Expression result = processor.resolvePlaceholder("#{currentUserDeptAndChildIds}");
            assertThat(result).isInstanceOf(ParenthesedExpressionList.class);
        }

        @Test
        @DisplayName("resolvePlaceholder 返回 LongValue for tenantId")
        void testResolvePlaceholderTenantId() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                    .tenantId(789L)
                    .build();

            DataScopeContextProvider provider = () -> context;
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            Expression result = processor.resolvePlaceholder("#{currentTenantId}");
            assertThat(result).isInstanceOf(LongValue.class);
            assertThat(((LongValue) result).getValue()).isEqualTo(789L);
        }

        @Test
        @DisplayName("resolvePlaceholder 返回 LongValue.MIN_VALUE for null userId")
        void testResolvePlaceholderNullUserId() {
            DataScopeContextProvider provider = () -> DataScopeUserContext.empty();
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            Expression result = processor.resolvePlaceholder("#{currentUserId}");
            assertThat(result).isInstanceOf(LongValue.class);
            assertThat(((LongValue) result).getValue()).isEqualTo(Long.MIN_VALUE);
        }

        @Test
        @DisplayName("resolvePlaceholder 返回 StringValue for non-placeholder")
        void testResolvePlaceholderNonPlaceholder() {
            DataScopeUserContext context = DataScopeUserContext.builder().build();
            DataScopeContextProvider provider = () -> context;
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            Expression result = processor.resolvePlaceholder("not_a_placeholder");
            assertThat(result).isInstanceOf(StringValue.class);
        }

        @Test
        @DisplayName("resolvePlaceholder 返回 StringValue for unknown placeholder")
        void testResolvePlaceholderUnknown() {
            DataScopeUserContext context = DataScopeUserContext.builder().build();
            DataScopeContextProvider provider = () -> context;
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            Expression result = processor.resolvePlaceholder("#{unknownPlaceholder}");
            assertThat(result).isInstanceOf(StringValue.class);
        }

        @Test
        @DisplayName("resolveDeptIds with empty accessibleDeptIds uses deptId")
        void testResolveDeptIdsEmptyUsesDeptId() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                    .deptId(100L)
                    .accessibleDeptIds(Set.of())
                    .build();

            DataScopeContextProvider provider = () -> context;
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            Expression result = processor.resolvePlaceholder("#{currentUserDeptAndChildIds}");
            assertThat(result).isInstanceOf(LongValue.class);
            assertThat(((LongValue) result).getValue()).isEqualTo(100L);
        }

        @Test
        @DisplayName("resolveDeptIds with null accessibleDeptIds uses deptId")
        void testResolveDeptIdsNullUsesDeptId() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                    .deptId(100L)
                    .build();

            DataScopeContextProvider provider = () -> context;
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            Expression result = processor.resolvePlaceholder("#{currentUserDeptAndChildIds}");
            assertThat(result).isInstanceOf(LongValue.class);
            assertThat(((LongValue) result).getValue()).isEqualTo(100L);
        }

        @Test
        @DisplayName("resolveDeptIds with both null accessibleDeptIds and null deptId returns MIN_VALUE")
        void testResolveDeptIdsBothNullReturnsMinValue() {
            DataScopeUserContext context = DataScopeUserContext.builder().build();

            DataScopeContextProvider provider = () -> context;
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            Expression result = processor.resolvePlaceholder("#{currentUserDeptAndChildIds}");
            assertThat(result).isInstanceOf(LongValue.class);
            assertThat(((LongValue) result).getValue()).isEqualTo(Long.MIN_VALUE);
        }

        @Test
        @DisplayName("resolveDeptIds with empty accessibleDeptIds and null deptId returns MIN_VALUE")
        void testResolveDeptIdsEmptyAndNullDeptIdReturnsMinValue() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                    .accessibleDeptIds(Set.of())
                    .build();

            DataScopeContextProvider provider = () -> context;
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            Expression result = processor.resolvePlaceholder("#{currentUserDeptAndChildIds}");
            assertThat(result).isInstanceOf(LongValue.class);
            assertThat(((LongValue) result).getValue()).isEqualTo(Long.MIN_VALUE);
        }

        @Test
        @DisplayName("resolveDeptId with null deptId returns MIN_VALUE")
        void testResolveDeptIdNullReturnsMinValue() {
            DataScopeUserContext context = DataScopeUserContext.builder().build();

            DataScopeContextProvider provider = () -> context;
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            Expression result = processor.resolvePlaceholder("#{currentDeptId}");
            assertThat(result).isInstanceOf(LongValue.class);
            assertThat(((LongValue) result).getValue()).isEqualTo(Long.MIN_VALUE);
        }

        @Test
        @DisplayName("resolveTenantId with null tenantId returns MIN_VALUE")
        void testResolveTenantIdNullReturnsMinValue() {
            DataScopeUserContext context = DataScopeUserContext.builder().build();

            DataScopeContextProvider provider = () -> context;
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            Expression result = processor.resolvePlaceholder("#{currentTenantId}");
            assertThat(result).isInstanceOf(LongValue.class);
            assertThat(((LongValue) result).getValue()).isEqualTo(Long.MIN_VALUE);
        }

        @Test
        @DisplayName("resolvePlaceholder for currentUserDeptIds (includeChilds=false)")
        void testResolvePlaceholderCurrentUserDeptIds() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                    .accessibleDeptIds(Set.of(1L, 2L, 3L))
                    .build();

            DataScopeContextProvider provider = () -> context;
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            Expression result = processor.resolvePlaceholder("#{currentUserDeptIds}");
            assertThat(result).isInstanceOf(ParenthesedExpressionList.class);
        }

        @Test
        @DisplayName("resolvePlaceholder for currentUserDeptIds with empty list and valid deptId")
        void testResolvePlaceholderCurrentUserDeptIdsEmptyWithDeptId() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                    .deptId(999L)
                    .accessibleDeptIds(Set.of())
                    .build();

            DataScopeContextProvider provider = () -> context;
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            Expression result = processor.resolvePlaceholder("#{currentUserDeptIds}");
            assertThat(result).isInstanceOf(LongValue.class);
            assertThat(((LongValue) result).getValue()).isEqualTo(999L);
        }
    }

    @Nested
    @DisplayName("hasPlaceholder Tests")
    class HasPlaceholderTests {

        @Test
        @DisplayName("hasPlaceholder with StringValue containing placeholder")
        void testHasPlaceholderWithStringValue() {
            DataScopeContextProvider provider = DataScopeContextProvider.empty();
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            StringValue stringValue = new StringValue("#{currentUserId}");
            assertThat(processor.hasPlaceholder(stringValue)).isTrue();
        }

        @Test
        @DisplayName("hasPlaceholder with StringValue not containing placeholder")
        void testHasPlaceholderWithNonPlaceholderStringValue() {
            DataScopeContextProvider provider = DataScopeContextProvider.empty();
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            StringValue stringValue = new StringValue("not a placeholder");
            assertThat(processor.hasPlaceholder(stringValue)).isFalse();
        }

        @Test
        @DisplayName("hasPlaceholder with null expression")
        void testHasPlaceholderWithNull() {
            DataScopeContextProvider provider = DataScopeContextProvider.empty();
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            assertThat(processor.hasPlaceholder(null)).isFalse();
        }

        @Test
        @DisplayName("hasPlaceholder with non-StringValue expression")
        void testHasPlaceholderWithNonStringValue() {
            DataScopeContextProvider provider = DataScopeContextProvider.empty();
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            LongValue longValue = new LongValue(123L);
            assertThat(processor.hasPlaceholder(longValue)).isFalse();
        }
    }

    @Nested
    @DisplayName("isPlaceholder Tests")
    class IsPlaceholderTests {

        @Test
        @DisplayName("检查是否是占位符")
        void testIsPlaceholder() {
            DataScopeContextProvider provider = DataScopeContextProvider.empty();
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            assertThat(processor.isPlaceholder("#{currentUserId}")).isTrue();
            assertThat(processor.isPlaceholder("#{currentDeptId}")).isTrue();
            assertThat(processor.isPlaceholder("123")).isFalse();
            assertThat(processor.isPlaceholder(null)).isFalse();
            assertThat(processor.isPlaceholder("")).isFalse();
            assertThat(processor.isPlaceholder("#{invalid")).isFalse();
        }
    }

    @Nested
    @DisplayName("provideUserContext Tests")
    class ProvideUserContextTests {

        @Test
        @DisplayName("provideUserContext 返回正确的上下文")
        void testProvideUserContext() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                    .userId(123L)
                    .deptId(456L)
                    .allDataPermission(true)
                    .build();

            DataScopeContextProvider provider = () -> context;
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            DataScopeUserContext result = processor.provideUserContext();
            assertThat(result.getUserId()).isEqualTo(123L);
            assertThat(result.getDeptId()).isEqualTo(456L);
            assertThat(result.isAllDataPermission()).isTrue();
        }

        @Test
        @DisplayName("provideUserContext 处理 null 返回空上下文")
        void testProvideUserContextReturnsEmpty() {
            DataScopeContextProvider provider = () -> null;
            DataScopeProcessor processor = new DataScopeProcessor(provider);

            DataScopeUserContext result = processor.provideUserContext();
            assertThat(result.getUserId()).isNull();
            assertThat(result.getDeptId()).isNull();
        }
    }
}