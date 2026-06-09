package io.github.afgprojects.framework.data.core.exception;

import io.github.afgprojects.framework.data.core.transaction.TransactionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataAccessException 体系单元测试
 * <p>
 * 验证异常构造、消息格式和属性获取。
 */
class ExceptionTest {

    // ========== EntityNotFoundException ==========

    @Nested
    @DisplayName("EntityNotFoundException")
    class EntityNotFoundExceptionTest {

        @Test
        @DisplayName("should create exception with entity class and id")
        void shouldCreateException_withEntityClassAndId() {
            EntityNotFoundException ex = new EntityNotFoundException(User.class, 1L);

            assertThat(ex.getMessage()).contains("User").contains("1");
            assertThat(ex.getEntityClassName()).isEqualTo("User");
            assertThat(ex.getEntityId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should create exception with entity class name and id")
        void shouldCreateException_withEntityClassNameAndId() {
            EntityNotFoundException ex = new EntityNotFoundException("Role", 42L);

            assertThat(ex.getMessage()).contains("Role").contains("42");
            assertThat(ex.getEntityClassName()).isEqualTo("Role");
            assertThat(ex.getEntityId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("should create exception with custom message")
        void shouldCreateException_withCustomMessage() {
            EntityNotFoundException ex = new EntityNotFoundException("Custom message", "User", 1L);

            assertThat(ex.getMessage()).isEqualTo("Custom message");
            assertThat(ex.getEntityClassName()).isEqualTo("User");
            assertThat(ex.getEntityId()).isEqualTo(1L);
        }
    }

    // ========== EntityMappingException ==========

    @Nested
    @DisplayName("EntityMappingException")
    class EntityMappingExceptionTest {

        @Test
        @DisplayName("should create exception with message only")
        void shouldCreateException_withMessageOnly() {
            EntityMappingException ex = new EntityMappingException("Mapping failed");

            assertThat(ex.getMessage()).isEqualTo("Mapping failed");
            assertThat(ex.getFieldName()).isNull();
            assertThat(ex.getEntityClassName()).isNull();
        }

        @Test
        @DisplayName("should create exception with entity class, field name and message")
        void shouldCreateException_withEntityClassFieldNameAndMessage() {
            EntityMappingException ex = new EntityMappingException(User.class, "name", "Type mismatch");

            assertThat(ex.getMessage()).contains("name").contains("User").contains("Type mismatch");
            assertThat(ex.getFieldName()).isEqualTo("name");
            assertThat(ex.getEntityClassName()).isEqualTo("User");
        }

        @Test
        @DisplayName("should create exception with message, field name and cause")
        void shouldCreateException_withMessageFieldNameAndCause() {
            RuntimeException cause = new RuntimeException("root cause");
            EntityMappingException ex = new EntityMappingException("Mapping failed", "email", cause);

            assertThat(ex.getMessage()).isEqualTo("Mapping failed");
            assertThat(ex.getFieldName()).isEqualTo("email");
            assertThat(ex.getCause()).isSameAs(cause);
        }

        @Test
        @DisplayName("should create exception with entity class, field name, message and cause")
        void shouldCreateException_withEntityClassFieldNameMessageAndCause() {
            RuntimeException cause = new RuntimeException("root cause");
            EntityMappingException ex = new EntityMappingException(User.class, "age", "Invalid type", cause);

            assertThat(ex.getFieldName()).isEqualTo("age");
            assertThat(ex.getCause()).isSameAs(cause);
        }
    }

    // ========== MetadataLoadException ==========

    @Nested
    @DisplayName("MetadataLoadException")
    class MetadataLoadExceptionTest {

        @Test
        @DisplayName("should create exception with message only")
        void shouldCreateException_withMessageOnly() {
            MetadataLoadException ex = new MetadataLoadException("Load failed");

            assertThat(ex.getMessage()).isEqualTo("Load failed");
            assertThat(ex.getEntityClassName()).isNull();
        }

        @Test
        @DisplayName("should create exception with message and entity class name")
        void shouldCreateException_withMessageAndEntityClassName() {
            MetadataLoadException ex = new MetadataLoadException("Load failed", "User");

            assertThat(ex.getMessage()).isEqualTo("Load failed");
            assertThat(ex.getEntityClassName()).isEqualTo("User");
        }

        @Test
        @DisplayName("should create exception with message and cause")
        void shouldCreateException_withMessageAndCause() {
            RuntimeException cause = new RuntimeException("root cause");
            MetadataLoadException ex = new MetadataLoadException("Load failed", cause);

            assertThat(ex.getMessage()).isEqualTo("Load failed");
            assertThat(ex.getCause()).isSameAs(cause);
        }

        @Test
        @DisplayName("should create exception with message, entity class name and cause")
        void shouldCreateException_withMessageEntityClassNameAndCause() {
            RuntimeException cause = new RuntimeException("root cause");
            MetadataLoadException ex = new MetadataLoadException("Load failed", "User", cause);

            assertThat(ex.getEntityClassName()).isEqualTo("User");
            assertThat(ex.getCause()).isSameAs(cause);
        }
    }

    // ========== OptimisticLockException ==========

    @Nested
    @DisplayName("OptimisticLockException")
    class OptimisticLockExceptionTest {

        @Test
        @DisplayName("should create exception with entity class name, id and version")
        void shouldCreateException_withEntityClassNameIdAndVersion() {
            OptimisticLockException ex = new OptimisticLockException("User", 1L, 3);

            assertThat(ex.getMessage()).contains("User").contains("1").contains("3");
            assertThat(ex.getEntityClassName()).isEqualTo("User");
            assertThat(ex.getEntityId()).isEqualTo(1L);
            assertThat(ex.getExpectedVersion()).isEqualTo(3);
        }

        @Test
        @DisplayName("should create exception with custom message")
        void shouldCreateException_withCustomMessage() {
            OptimisticLockException ex = new OptimisticLockException("Custom message", "Role", 42L, 5);

            assertThat(ex.getMessage()).isEqualTo("Custom message");
            assertThat(ex.getEntityClassName()).isEqualTo("Role");
            assertThat(ex.getEntityId()).isEqualTo(42L);
            assertThat(ex.getExpectedVersion()).isEqualTo(5);
        }
    }

    // ========== TransactionException ==========

    @Nested
    @DisplayName("TransactionException")
    class TransactionExceptionTest {

        @Test
        @DisplayName("should create exception with message only")
        void shouldCreateException_withMessageOnly() {
            TransactionException ex = new TransactionException("TX failed");

            assertThat(ex.getMessage()).isEqualTo("TX failed");
        }

        @Test
        @DisplayName("should create exception with message and cause")
        void shouldCreateException_withMessageAndCause() {
            RuntimeException cause = new RuntimeException("root cause");
            TransactionException ex = new TransactionException("TX failed", cause);

            assertThat(ex.getMessage()).isEqualTo("TX failed");
            assertThat(ex.getCause()).isSameAs(cause);
        }
    }

    // ========== Helper ==========

    private static class User {
        // test entity class
    }
}