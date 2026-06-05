/*
 * Copyright 2024 AFG Projects.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.afgprojects.framework.data.sql.scope;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * DataScopeProcessor 单元测试
 * <p>
 * 测试数据权限占位符解析、文本替换和判断逻辑。
 */
class DataScopeProcessorTest {

    // ==================== currentUserId 占位符 ====================

    @Nested
    @DisplayName("currentUserId 占位符")
    class CurrentUserIdPlaceholder {

        @Test
        @DisplayName("should resolve to LongValue when currentUserId placeholder with valid userId")
        void shouldResolveToLongValue_whenCurrentUserIdPlaceholderWithValidUserId() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .userId(123L)
                .build();
            DataScopeProcessor processor = new DataScopeProcessor(() -> context);

            Expression result = processor.resolvePlaceholder("#{currentUserId}");

            assertThat(result).isInstanceOf(LongValue.class);
            assertThat(((LongValue) result).getValue()).isEqualTo(123L);
        }

        @Test
        @DisplayName("should resolve to Long MIN_VALUE when currentUserId placeholder with null userId")
        void shouldResolveToLongMinValue_whenCurrentUserIdPlaceholderWithNullUserId() {
            DataScopeUserContext context = DataScopeUserContext.builder().build();
            DataScopeProcessor processor = new DataScopeProcessor(() -> context);

            Expression result = processor.resolvePlaceholder("#{currentUserId}");

            assertThat(result).isInstanceOf(LongValue.class);
            assertThat(((LongValue) result).getValue()).isEqualTo(Long.MIN_VALUE);
        }
    }

    // ==================== currentDeptId 占位符 ====================

    @Nested
    @DisplayName("currentDeptId 占位符")
    class CurrentDeptIdPlaceholder {

        @Test
        @DisplayName("should resolve to LongValue when currentDeptId placeholder with valid deptId")
        void shouldResolveToLongValue_whenCurrentDeptIdPlaceholderWithValidDeptId() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .deptId(456L)
                .build();
            DataScopeProcessor processor = new DataScopeProcessor(() -> context);

            Expression result = processor.resolvePlaceholder("#{currentDeptId}");

            assertThat(result).isInstanceOf(LongValue.class);
            assertThat(((LongValue) result).getValue()).isEqualTo(456L);
        }

        @Test
        @DisplayName("should resolve to Long MIN_VALUE when currentDeptId placeholder with null deptId")
        void shouldResolveToLongMinValue_whenCurrentDeptIdPlaceholderWithNullDeptId() {
            DataScopeUserContext context = DataScopeUserContext.builder().build();
            DataScopeProcessor processor = new DataScopeProcessor(() -> context);

            Expression result = processor.resolvePlaceholder("#{currentDeptId}");

            assertThat(result).isInstanceOf(LongValue.class);
            assertThat(((LongValue) result).getValue()).isEqualTo(Long.MIN_VALUE);
        }
    }

    // ==================== currentTenantId 占位符 ====================

    @Nested
    @DisplayName("currentTenantId 占位符")
    class CurrentTenantIdPlaceholder {

        @Test
        @DisplayName("should resolve to LongValue when currentTenantId placeholder with valid tenantId")
        void shouldResolveToLongValue_whenCurrentTenantIdPlaceholderWithValidTenantId() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .tenantId(789L)
                .build();
            DataScopeProcessor processor = new DataScopeProcessor(() -> context);

            Expression result = processor.resolvePlaceholder("#{currentTenantId}");

            assertThat(result).isInstanceOf(LongValue.class);
            assertThat(((LongValue) result).getValue()).isEqualTo(789L);
        }

        @Test
        @DisplayName("should resolve to Long MIN_VALUE when currentTenantId placeholder with null tenantId")
        void shouldResolveToLongMinValue_whenCurrentTenantIdPlaceholderWithNullTenantId() {
            DataScopeUserContext context = DataScopeUserContext.builder().build();
            DataScopeProcessor processor = new DataScopeProcessor(() -> context);

            Expression result = processor.resolvePlaceholder("#{currentTenantId}");

            assertThat(result).isInstanceOf(LongValue.class);
            assertThat(((LongValue) result).getValue()).isEqualTo(Long.MIN_VALUE);
        }
    }

    // ==================== currentUserDeptIds 占位符 ====================

    @Nested
    @DisplayName("currentUserDeptIds 占位符")
    class CurrentUserDeptIdsPlaceholder {

        @Test
        @DisplayName("should resolve to ParenthesedExpressionList when multiple deptIds")
        void shouldResolveToParenthesedExpressionList_whenMultipleDeptIds() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .accessibleDeptIds(Set.of(10L, 20L, 30L))
                .build();
            DataScopeProcessor processor = new DataScopeProcessor(() -> context);

            Expression result = processor.resolvePlaceholder("#{currentUserDeptIds}");

            assertThat(result).isInstanceOf(ParenthesedExpressionList.class);
        }

        @Test
        @DisplayName("should resolve to LongValue when single deptId via deptId field")
        void shouldResolveToLongValue_whenSingleDeptIdViaDeptIdField() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .deptId(10L)
                .build();
            DataScopeProcessor processor = new DataScopeProcessor(() -> context);

            Expression result = processor.resolvePlaceholder("#{currentUserDeptIds}");

            assertThat(result).isInstanceOf(LongValue.class);
            assertThat(((LongValue) result).getValue()).isEqualTo(10L);
        }

        @Test
        @DisplayName("should resolve to Long MIN_VALUE when no deptIds and no deptId")
        void shouldResolveToLongMinValue_whenNoDeptIdsAndNoDeptId() {
            DataScopeUserContext context = DataScopeUserContext.builder().build();
            DataScopeProcessor processor = new DataScopeProcessor(() -> context);

            Expression result = processor.resolvePlaceholder("#{currentUserDeptIds}");

            assertThat(result).isInstanceOf(LongValue.class);
            assertThat(((LongValue) result).getValue()).isEqualTo(Long.MIN_VALUE);
        }
    }

    // ==================== currentUserDeptAndChildIds 占位符 ====================

    @Nested
    @DisplayName("currentUserDeptAndChildIds 占位符")
    class CurrentUserDeptAndChildIdsPlaceholder {

        @Test
        @DisplayName("should resolve to ParenthesedExpressionList when multiple deptIds")
        void shouldResolveToParenthesedExpressionList_whenMultipleDeptIds() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .accessibleDeptIds(Set.of(10L, 20L))
                .build();
            DataScopeProcessor processor = new DataScopeProcessor(() -> context);

            Expression result = processor.resolvePlaceholder("#{currentUserDeptAndChildIds}");

            assertThat(result).isInstanceOf(ParenthesedExpressionList.class);
        }

        @Test
        @DisplayName("should resolve to LongValue when single deptId")
        void shouldResolveToLongValue_whenSingleDeptId() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .deptId(10L)
                .build();
            DataScopeProcessor processor = new DataScopeProcessor(() -> context);

            Expression result = processor.resolvePlaceholder("#{currentUserDeptAndChildIds}");

            assertThat(result).isInstanceOf(LongValue.class);
            assertThat(((LongValue) result).getValue()).isEqualTo(10L);
        }
    }

    // ==================== 非占位符和未知占位符 ====================

    @Nested
    @DisplayName("非占位符和未知占位符")
    class NonPlaceholderAndUnknown {

        @Test
        @DisplayName("should return StringValue when input is not a placeholder")
        void shouldReturnStringValue_whenInputIsNotAPlaceholder() {
            DataScopeUserContext context = DataScopeUserContext.builder().build();
            DataScopeProcessor processor = new DataScopeProcessor(() -> context);

            Expression result = processor.resolvePlaceholder("just_text");

            assertThat(result).isInstanceOf(StringValue.class);
            assertThat(((StringValue) result).getValue()).isEqualTo("just_text");
        }

        @Test
        @DisplayName("should return StringValue when placeholder is unknown")
        void shouldReturnStringValue_whenPlaceholderIsUnknown() {
            DataScopeUserContext context = DataScopeUserContext.builder().build();
            DataScopeProcessor processor = new DataScopeProcessor(() -> context);

            Expression result = processor.resolvePlaceholder("#{unknownPlaceholder}");

            assertThat(result).isInstanceOf(StringValue.class);
            assertThat(((StringValue) result).getValue()).isEqualTo("#{unknownPlaceholder}");
        }
    }

    // ==================== resolvePlaceholders 文本替换 ====================

    @Nested
    @DisplayName("resolvePlaceholders 文本替换")
    class ResolvePlaceholdersText {

        @Test
        @DisplayName("should replace placeholder in text when resolvePlaceholders called")
        void shouldReplacePlaceholderInText_whenResolvePlaceholdersCalled() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .userId(123L)
                .build();
            DataScopeProcessor processor = new DataScopeProcessor(() -> context);

            String result = processor.resolvePlaceholders("user_id = #{currentUserId}");

            assertThat(result).isEqualTo("user_id = 123");
        }

        @Test
        @DisplayName("should replace multiple placeholders in text when resolvePlaceholders called")
        void shouldReplaceMultiplePlaceholdersInText_whenResolvePlaceholdersCalled() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .userId(123L)
                .deptId(456L)
                .build();
            DataScopeProcessor processor = new DataScopeProcessor(() -> context);

            String result = processor.resolvePlaceholders("user_id = #{currentUserId} AND dept_id = #{currentDeptId}");

            assertThat(result).isEqualTo("user_id = 123 AND dept_id = 456");
        }

        @Test
        @DisplayName("should keep unknown placeholder when resolvePlaceholders called")
        void shouldKeepUnknownPlaceholder_whenResolvePlaceholdersCalled() {
            DataScopeUserContext context = DataScopeUserContext.builder().build();
            DataScopeProcessor processor = new DataScopeProcessor(() -> context);

            String result = processor.resolvePlaceholders("status = #{unknownPlaceholder}");

            assertThat(result).isEqualTo("status = #{unknownPlaceholder}");
        }

        @Test
        @DisplayName("should return original text when no placeholders present")
        void shouldReturnOriginalText_whenNoPlaceholdersPresent() {
            DataScopeUserContext context = DataScopeUserContext.builder().build();
            DataScopeProcessor processor = new DataScopeProcessor(() -> context);

            String result = processor.resolvePlaceholders("status = 1");

            assertThat(result).isEqualTo("status = 1");
        }

        @Test
        @DisplayName("should replace tenantId placeholder in text")
        void shouldReplaceTenantIdPlaceholderInText() {
            DataScopeUserContext context = DataScopeUserContext.builder()
                .tenantId(789L)
                .build();
            DataScopeProcessor processor = new DataScopeProcessor(() -> context);

            String result = processor.resolvePlaceholders("tenant_id = #{currentTenantId}");

            assertThat(result).isEqualTo("tenant_id = 789");
        }
    }

    // ==================== hasPlaceholder 和 isPlaceholder ====================

    @Nested
    @DisplayName("hasPlaceholder 和 isPlaceholder")
    class PlaceholderDetection {

        @Test
        @DisplayName("should return true when isPlaceholder called with valid placeholder")
        void shouldReturnTrue_whenIsPlaceholderCalledWithValidPlaceholder() {
            DataScopeProcessor processor = new DataScopeProcessor(DataScopeContextProvider.empty());

            assertThat(processor.isPlaceholder("#{currentUserId}")).isTrue();
            assertThat(processor.isPlaceholder("#{currentDeptId}")).isTrue();
            assertThat(processor.isPlaceholder("#{currentUserDeptIds}")).isTrue();
        }

        @Test
        @DisplayName("should return false when isPlaceholder called with non placeholder")
        void shouldReturnFalse_whenIsPlaceholderCalledWithNonPlaceholder() {
            DataScopeProcessor processor = new DataScopeProcessor(DataScopeContextProvider.empty());

            assertThat(processor.isPlaceholder("just_text")).isFalse();
            assertThat(processor.isPlaceholder("#{incomplete")).isFalse();
            assertThat(processor.isPlaceholder("no_placeholder}")).isFalse();
        }

        @Test
        @DisplayName("should return false when isPlaceholder called with null")
        void shouldReturnFalse_whenIsPlaceholderCalledWithNull() {
            DataScopeProcessor processor = new DataScopeProcessor(DataScopeContextProvider.empty());

            assertThat(processor.isPlaceholder(null)).isFalse();
        }

        @Test
        @DisplayName("should return true when hasPlaceholder called with StringValue containing placeholder")
        void shouldReturnTrue_whenHasPlaceholderCalledWithStringValueContainingPlaceholder() {
            DataScopeProcessor processor = new DataScopeProcessor(DataScopeContextProvider.empty());

            assertThat(processor.hasPlaceholder(new StringValue("#{currentUserId}"))).isTrue();
        }

        @Test
        @DisplayName("should return false when hasPlaceholder called with StringValue not containing placeholder")
        void shouldReturnFalse_whenHasPlaceholderCalledWithStringValueNotContainingPlaceholder() {
            DataScopeProcessor processor = new DataScopeProcessor(DataScopeContextProvider.empty());

            assertThat(processor.hasPlaceholder(new StringValue("just_text"))).isFalse();
        }

        @Test
        @DisplayName("should return false when hasPlaceholder called with null")
        void shouldReturnFalse_whenHasPlaceholderCalledWithNull() {
            DataScopeProcessor processor = new DataScopeProcessor(DataScopeContextProvider.empty());

            assertThat(processor.hasPlaceholder(null)).isFalse();
        }

        @Test
        @DisplayName("should return false when hasPlaceholder called with non StringValue expression")
        void shouldReturnFalse_whenHasPlaceholderCalledWithNonStringValueExpression() {
            DataScopeProcessor processor = new DataScopeProcessor(DataScopeContextProvider.empty());

            assertThat(processor.hasPlaceholder(new LongValue(123))).isFalse();
        }
    }

    // ==================== provideUserContext ====================

    @Nested
    @DisplayName("provideUserContext")
    class ProvideUserContext {

        @Test
        @DisplayName("should return context from provider when provideUserContext called")
        void shouldReturnContextFromProvider_whenProvideUserContextCalled() {
            DataScopeUserContext expected = DataScopeUserContext.builder()
                .userId(123L)
                .build();
            DataScopeProcessor processor = new DataScopeProcessor(() -> expected);

            DataScopeUserContext result = processor.provideUserContext();

            assertThat(result.getUserId()).isEqualTo(123L);
        }

        @Test
        @DisplayName("should return empty context when provider returns null")
        void shouldReturnEmptyContext_whenProviderReturnsNull() {
            DataScopeProcessor processor = new DataScopeProcessor(() -> null);

            DataScopeUserContext result = processor.provideUserContext();

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isNull();
            assertThat(result.isAllDataPermission()).isFalse();
        }
    }
}
