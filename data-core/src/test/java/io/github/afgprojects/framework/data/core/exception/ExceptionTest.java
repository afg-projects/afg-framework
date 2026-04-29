package io.github.afgprojects.framework.data.core.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exception 包测试
 */
@DisplayName("Exception 包测试")
class ExceptionTest {

    // ==================== OptimisticLockException 测试 ====================

    @Nested
    @DisplayName("OptimisticLockException 测试")
    class OptimisticLockExceptionTest {

        @Test
        @DisplayName("应使用基本构造方法创建异常")
        void shouldCreateWithBasicConstructor() {
            OptimisticLockException ex = new OptimisticLockException("User", 123L, 5L);

            assertThat(ex.getEntityClassName()).isEqualTo("User");
            assertThat(ex.getEntityId()).isEqualTo(123L);
            assertThat(ex.getExpectedVersion()).isEqualTo(5L);
            assertThat(ex.getMessage()).contains("Optimistic lock conflict");
            assertThat(ex.getMessage()).contains("entity=User");
            assertThat(ex.getMessage()).contains("id=123");
            assertThat(ex.getMessage()).contains("expectedVersion=5");
        }

        @Test
        @DisplayName("应使用自定义消息构造方法创建异常")
        void shouldCreateWithCustomMessage() {
            OptimisticLockException ex = new OptimisticLockException(
                    "Custom error message",
                    "Order",
                    "ORD-001",
                    10L
            );

            assertThat(ex.getMessage()).isEqualTo("Custom error message");
            assertThat(ex.getEntityClassName()).isEqualTo("Order");
            assertThat(ex.getEntityId()).isEqualTo("ORD-001");
            assertThat(ex.getExpectedVersion()).isEqualTo(10L);
        }

        @Test
        @DisplayName("应继承 RuntimeException")
        void shouldExtendRuntimeException() {
            OptimisticLockException ex = new OptimisticLockException("Test", 1L, 1L);

            assertThat(ex).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("应支持不同类型的实体ID")
        void shouldSupportDifferentEntityIdTypes() {
            // Long 类型 ID
            OptimisticLockException ex1 = new OptimisticLockException("Entity1", 12345L, 1L);
            assertThat(ex1.getEntityId()).isInstanceOf(Long.class);

            // String 类型 ID
            OptimisticLockException ex2 = new OptimisticLockException("Entity2", "string-id", 2L);
            assertThat(ex2.getEntityId()).isInstanceOf(String.class);

            // Integer 类型 ID
            OptimisticLockException ex3 = new OptimisticLockException("Entity3", 100, 3L);
            assertThat(ex3.getEntityId()).isInstanceOf(Integer.class);
        }

        @Test
        @DisplayName("应正确格式化消息")
        void shouldFormatMessageCorrectly() {
            OptimisticLockException ex = new OptimisticLockException("Product", 999L, 42L);

            String message = ex.getMessage();

            assertThat(message).isEqualTo(
                    "Optimistic lock conflict: entity=Product, id=999, expectedVersion=42"
            );
        }

        @Test
        @DisplayName("应支持异常链")
        void shouldSupportCauseChain() {
            RuntimeException cause = new RuntimeException("Original cause");

            OptimisticLockException ex = new OptimisticLockException("Test", 1L, 1L);
            ex.initCause(cause);

            assertThat(ex.getCause()).isEqualTo(cause);
        }
    }
}