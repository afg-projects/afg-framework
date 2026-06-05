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
package io.github.afgprojects.framework.data.core.mapper;

import io.github.afgprojects.framework.data.core.mapper.handlers.BigDecimalTypeHandler;
import io.github.afgprojects.framework.data.core.mapper.handlers.BooleanNumberTypeHandler;
import io.github.afgprojects.framework.data.core.mapper.handlers.DateTimeTypeHandler;
import io.github.afgprojects.framework.data.core.mapper.handlers.EnumTypeHandler;
import io.github.afgprojects.framework.data.core.mapper.handlers.InstantTypeHandler;
import io.github.afgprojects.framework.data.core.mapper.handlers.LocalDateTypeHandler;
import io.github.afgprojects.framework.data.core.mapper.handlers.NumberTypeHandler;
import io.github.afgprojects.framework.data.core.mapper.handlers.StringTypeHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

/**
 * TypeHandlerRegistry 单元测试
 * <p>
 * 测试类型处理器的注册、查找和转换功能。
 */
class TypeHandlerRegistryTest {

    // ==================== 注册操作 ====================

    @Nested
    @DisplayName("注册操作")
    class RegisterOperations {

        @Test
        @DisplayName("should register handler when call register")
        void shouldRegisterHandler_whenCallRegister() {
            TypeHandlerRegistry registry = new TypeHandlerRegistry();
            NumberTypeHandler handler = new NumberTypeHandler();

            registry.register(handler);

            Object result = registry.convert(100L, Long.class);
            assertThat(result).isEqualTo(100L);
        }

        @Test
        @DisplayName("should sort by priority desc when register multiple")
        void shouldSortByPriorityDesc_whenRegisterMultiple() {
            TypeHandlerRegistry registry = new TypeHandlerRegistry();

            // NumberTypeHandler priority=0, BooleanNumberTypeHandler priority=10
            registry.register(new BooleanNumberTypeHandler()); // priority 10
            registry.register(new NumberTypeHandler());        // priority 0

            // BooleanNumberTypeHandler 支持 Boolean.class，优先级更高（数值更小）
            assertThat(registry.convert(1, Boolean.class)).isEqualTo(true);
        }

        @Test
        @DisplayName("should unregister handler when call unregister")
        void shouldUnregisterHandler_whenCallUnregister() {
            TypeHandlerRegistry registry = new TypeHandlerRegistry();
            registry.register(new NumberTypeHandler());

            registry.unregister(Number.class);

            // 注销后应该没有处理器处理 Number 类型的精确匹配
            Object result = registry.convert(100L, Long.class);
            assertThat(result).isEqualTo(100L); // 精确匹配失败，返回原值（因为 Long instanceof Long）
        }
    }

    // ==================== 转换操作 ====================

    @Nested
    @DisplayName("转换操作")
    class ConvertOperations {

        @Test
        @DisplayName("should return null when convert null value")
        void shouldReturnNull_whenConvertNullValue() {
            TypeHandlerRegistry registry = new TypeHandlerRegistry();

            assertThat(registry.convert(null, Long.class)).isNull();
        }

        @Test
        @DisplayName("should return same value when target type is instance")
        void shouldReturnSameValue_whenTargetTypeIsInstance() {
            TypeHandlerRegistry registry = new TypeHandlerRegistry();
            String value = "test-string";

            assertThat(registry.convert(value, String.class)).isSameAs(value);
        }

        @Test
        @DisplayName("should convert exact match when handler registered")
        void shouldConvertExactMatch_whenHandlerRegistered() {
            TypeHandlerRegistry registry = new TypeHandlerRegistry();
            registry.register(new NumberTypeHandler());

            // NumberTypeHandler 处理 Number 类型，Long 是 Number 的子类
            Object result = registry.convert(100, Long.class);
            assertThat(result).isEqualTo(100L);
        }

        @Test
        @DisplayName("should fallback to compatible match when no exact handler")
        void shouldFallbackToCompatibleMatch_whenNoExactHandler() {
            TypeHandlerRegistry registry = new TypeHandlerRegistry();
            registry.register(new NumberTypeHandler());

            // NumberTypeHandler 处理 Number 类型，Integer 是 Number 的子类
            Object result = registry.convert(100L, Integer.class);
            assertThat(result).isEqualTo(100);
        }

        @Test
        @DisplayName("should return value as is when no handler found")
        void shouldReturnValueAsIs_whenNoHandlerFound() {
            TypeHandlerRegistry registry = new TypeHandlerRegistry();

            // 没有注册任何处理器，且类型不匹配
            Object result = registry.convert("test", StringBuilder.class);
            assertThat(result).isEqualTo("test");
        }

        @Test
        @DisplayName("should ignore exception in compatible match when handler throws")
        void shouldIgnoreExceptionInCompatibleMatch_whenHandlerThrows() {
            TypeHandlerRegistry registry = new TypeHandlerRegistry();
            registry.register(new NumberTypeHandler());

            // NumberTypeHandler 无法将 String 转为 Number，应该返回原值
            Object result = registry.convert("not-a-number", Long.class);
            assertThat(result).isEqualTo("not-a-number");
        }
    }

    // ==================== 默认注册表 ====================

    @Nested
    @DisplayName("默认注册表")
    class DefaultRegistry {

        @Test
        @DisplayName("should register all default handlers when defaultRegistry")
        void shouldRegisterAllDefaultHandlers_whenDefaultRegistry() {
            TypeHandlerRegistry registry = TypeHandlerRegistry.defaultRegistry();

            // 验证各种类型都能正确转换
            assertThat(registry.convert(100L, Long.class)).isEqualTo(100L);
            assertThat(registry.convert(1, Boolean.class)).isEqualTo(true);
            assertThat(registry.convert(0, Boolean.class)).isEqualTo(false);
            assertThat(registry.convert(new Timestamp(System.currentTimeMillis()), Instant.class)).isInstanceOf(Instant.class);
            assertThat(registry.convert(new Date(), LocalDate.class)).isInstanceOf(LocalDate.class);
            assertThat(registry.convert("test", String.class)).isEqualTo("test");
        }

        @Test
        @DisplayName("should convert boolean from number when BooleanNumberTypeHandler")
        void shouldConvertBooleanFromNumber_whenBooleanNumberTypeHandler() {
            TypeHandlerRegistry registry = TypeHandlerRegistry.defaultRegistry();

            assertThat(registry.convert(1, Boolean.class)).isEqualTo(true);
            assertThat(registry.convert(0, Boolean.class)).isEqualTo(false);
            assertThat(registry.convert(2, Boolean.class)).isEqualTo(true);
        }

        @Test
        @DisplayName("should convert big decimal from number when BigDecimalTypeHandler")
        void shouldConvertBigDecimalFromNumber_whenBigDecimalTypeHandler() {
            TypeHandlerRegistry registry = TypeHandlerRegistry.defaultRegistry();

            Object result = registry.convert(100.5, BigDecimal.class);
            assertThat(result).isInstanceOf(BigDecimal.class);
            assertThat((BigDecimal) result).isEqualByComparingTo("100.5");
        }

        @Test
        @DisplayName("should convert instant from timestamp when InstantTypeHandler")
        void shouldConvertInstantFromTimestamp_whenInstantTypeHandler() {
            TypeHandlerRegistry registry = TypeHandlerRegistry.defaultRegistry();
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());

            Object result = registry.convert(timestamp, Instant.class);
            assertThat(result).isInstanceOf(Instant.class);
            assertThat(((Instant) result).toEpochMilli()).isEqualTo(timestamp.getTime());
        }

        @Test
        @DisplayName("should convert local date from date when LocalDateTypeHandler")
        void shouldConvertLocalDateFromDate_whenLocalDateTypeHandler() {
            TypeHandlerRegistry registry = TypeHandlerRegistry.defaultRegistry();
            Date date = new Date();

            Object result = registry.convert(date, LocalDate.class);
            assertThat(result).isInstanceOf(LocalDate.class);
        }

        @Test
        @DisplayName("should convert enum from string when EnumTypeHandler")
        void shouldConvertEnumFromString_whenEnumTypeHandler() {
            TypeHandlerRegistry registry = TypeHandlerRegistry.defaultRegistry();

            Object result = registry.convert("VALUE_A", TestEnum.class);
            assertThat(result).isEqualTo(TestEnum.VALUE_A);
        }

        @Test
        @DisplayName("should convert enum from ordinal when EnumTypeHandler")
        void shouldConvertEnumFromOrdinal_whenEnumTypeHandler() {
            TypeHandlerRegistry registry = TypeHandlerRegistry.defaultRegistry();

            Object result = registry.convert(1, TestEnum.class);
            assertThat(result).isEqualTo(TestEnum.VALUE_B);
        }
    }

    // ==================== 边界情况 ====================

    @Nested
    @DisplayName("边界情况")
    class EdgeCases {

        @Test
        @DisplayName("should handle multiple handlers for same type")
        void shouldHandleMultipleHandlersForSameType() {
            TypeHandlerRegistry registry = new TypeHandlerRegistry();
            registry.register(new NumberTypeHandler());      // priority 0
            registry.register(new BigDecimalTypeHandler());  // priority 5

            // 高优先级（数值小）应该优先
            Object result = registry.convert(100, BigDecimal.class);
            assertThat(result).isInstanceOf(BigDecimal.class);
        }

        @Test
        @DisplayName("should handle primitive and wrapper compatibility")
        void shouldHandlePrimitiveAndWrapperCompatibility() {
            TypeHandlerRegistry registry = TypeHandlerRegistry.defaultRegistry();

            // 原始类型和包装类型应该兼容
            assertThat(registry.convert(100L, long.class)).isEqualTo(100L);
            assertThat(registry.convert(100, int.class)).isEqualTo(100);
        }
    }

    // 测试用枚举
    enum TestEnum {
        VALUE_A,
        VALUE_B,
        VALUE_C
    }
}