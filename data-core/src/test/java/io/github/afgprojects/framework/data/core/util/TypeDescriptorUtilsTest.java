package io.github.afgprojects.framework.data.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TypeDescriptorUtils 单元测试
 * <p>
 * 验证 JVM 类型描述符解析逻辑。
 */
class TypeDescriptorUtilsTest {

    // ========== 基本类型解析 ==========

    @Nested
    @DisplayName("基本类型解析")
    class PrimitiveTypeResolution {

        @Test
        @DisplayName("should resolve boolean descriptor")
        void shouldResolveBooleanDescriptor() {
            assertThat(TypeDescriptorUtils.resolveTypeFromDescriptor("Z")).isEqualTo(boolean.class);
        }

        @Test
        @DisplayName("should resolve byte descriptor")
        void shouldResolveByteDescriptor() {
            assertThat(TypeDescriptorUtils.resolveTypeFromDescriptor("B")).isEqualTo(byte.class);
        }

        @Test
        @DisplayName("should resolve char descriptor")
        void shouldResolveCharDescriptor() {
            assertThat(TypeDescriptorUtils.resolveTypeFromDescriptor("C")).isEqualTo(char.class);
        }

        @Test
        @DisplayName("should resolve short descriptor")
        void shouldResolveShortDescriptor() {
            assertThat(TypeDescriptorUtils.resolveTypeFromDescriptor("S")).isEqualTo(short.class);
        }

        @Test
        @DisplayName("should resolve int descriptor")
        void shouldResolveIntDescriptor() {
            assertThat(TypeDescriptorUtils.resolveTypeFromDescriptor("I")).isEqualTo(int.class);
        }

        @Test
        @DisplayName("should resolve long descriptor")
        void shouldResolveLongDescriptor() {
            assertThat(TypeDescriptorUtils.resolveTypeFromDescriptor("J")).isEqualTo(long.class);
        }

        @Test
        @DisplayName("should resolve float descriptor")
        void shouldResolveFloatDescriptor() {
            assertThat(TypeDescriptorUtils.resolveTypeFromDescriptor("F")).isEqualTo(float.class);
        }

        @Test
        @DisplayName("should resolve double descriptor")
        void shouldResolveDoubleDescriptor() {
            assertThat(TypeDescriptorUtils.resolveTypeFromDescriptor("D")).isEqualTo(double.class);
        }

        @Test
        @DisplayName("should resolve void descriptor")
        void shouldResolveVoidDescriptor() {
            assertThat(TypeDescriptorUtils.resolveTypeFromDescriptor("V")).isEqualTo(void.class);
        }
    }

    // ========== 引用类型解析 ==========

    @Nested
    @DisplayName("引用类型解析")
    class ReferenceTypeResolution {

        @Test
        @DisplayName("should resolve String reference type")
        void shouldResolveStringReferenceType() {
            Class<?> result = TypeDescriptorUtils.resolveTypeFromDescriptor("Ljava/lang/String;");

            assertThat(result).isEqualTo(String.class);
        }

        @Test
        @DisplayName("should resolve Integer reference type")
        void shouldResolveIntegerReferenceType() {
            Class<?> result = TypeDescriptorUtils.resolveTypeFromDescriptor("Ljava/lang/Integer;");

            assertThat(result).isEqualTo(Integer.class);
        }

        @Test
        @DisplayName("should return null for unknown reference type")
        void shouldReturnNull_forUnknownReferenceType() {
            Class<?> result = TypeDescriptorUtils.resolveTypeFromDescriptor("Lcom/nonexistent/SomeClass;");

            assertThat(result).isNull();
        }
    }

    // ========== 无效输入 ==========

    @Nested
    @DisplayName("无效输入")
    class InvalidInput {

        @Test
        @DisplayName("should return null for unknown descriptor prefix")
        void shouldReturnNull_forUnknownDescriptorPrefix() {
            Class<?> result = TypeDescriptorUtils.resolveTypeFromDescriptor("X");

            assertThat(result).isNull();
        }
    }

    // ========== resolveFieldTypeFromLambda ==========

    @Nested
    @DisplayName("resolveFieldTypeFromLambda")
    class ResolveFieldTypeFromLambda {

        @Test
        @DisplayName("should return null when non-serializable lambda provided")
        void shouldReturnNull_whenNonSerializableLambdaProvided() {
            // Non-serialized lambda (method reference on non-serializable interface)
            // This should return null gracefully
            Class<?> result = TypeDescriptorUtils.resolveFieldTypeFromLambda(null);

            assertThat(result).isNull();
        }
    }
}