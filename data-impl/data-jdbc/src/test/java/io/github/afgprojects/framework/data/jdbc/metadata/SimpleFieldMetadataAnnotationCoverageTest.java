package io.github.afgprojects.framework.data.jdbc.metadata;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SimpleFieldMetadata 注解检测覆盖率测试
 * <p>
 * 覆盖以下场景：
 * - Jakarta Persistence @Id 注解检测
 * - Jakarta Persistence @EmbeddedId 注解检测
 * - Spring Data @Id 注解检测
 */
@DisplayName("SimpleFieldMetadata 注解检测覆盖率测试")
class SimpleFieldMetadataAnnotationCoverageTest {

    @Nested
    @DisplayName("Jakarta Persistence @Id 注解测试")
    class JakartaPersistenceIdTests {

        @Test
        @DisplayName("Jakarta @Id 注解应被识别为主键")
        void shouldRecognizeJakartaIdAnnotation() throws Exception {
            // Given
            Field idField = JakartaIdEntity.class.getDeclaredField("id");

            // When
            SimpleFieldMetadata metadata = new SimpleFieldMetadata(idField);

            // Then
            assertThat(metadata.isId()).isTrue();
            assertThat(metadata.isGenerated()).isTrue();
        }

        @Test
        @DisplayName("无 @Id 注解的字段应不被识别为主键")
        void shouldNotRecognizeNonIdField() throws Exception {
            // Given
            Field nameField = JakartaIdEntity.class.getDeclaredField("name");

            // When
            SimpleFieldMetadata metadata = new SimpleFieldMetadata(nameField);

            // Then
            assertThat(metadata.isId()).isFalse();
        }
    }

    @Nested
    @DisplayName("Jakarta Persistence @EmbeddedId 注解测试")
    class JakartaPersistenceEmbeddedIdTests {

        @Test
        @DisplayName("@EmbeddedId 注解应被识别为主键")
        void shouldRecognizeEmbeddedIdAnnotation() throws Exception {
            // Given
            Field idField = EmbeddedIdEntity.class.getDeclaredField("id");

            // When
            SimpleFieldMetadata metadata = new SimpleFieldMetadata(idField);

            // Then
            assertThat(metadata.isId()).isTrue();
        }
    }

    @Nested
    @DisplayName("Spring Data @Id 注解测试")
    class SpringDataIdTests {

        @Test
        @DisplayName("Spring Data @Id 注解应被识别为主键")
        void shouldRecognizeSpringDataIdAnnotation() throws Exception {
            // Given
            Field idField = SpringDataIdEntity.class.getDeclaredField("id");

            // When
            SimpleFieldMetadata metadata = new SimpleFieldMetadata(idField);

            // Then
            assertThat(metadata.isId()).isTrue();
        }
    }

    @Nested
    @DisplayName("字段名为 id 后备方案测试")
    class FallbackToIdNameTests {

        @Test
        @DisplayName("无注解但名为 id 的字段应被识别为主键")
        void shouldFallbackToIdName() throws Exception {
            // Given
            Field idField = NoAnnotationEntity.class.getDeclaredField("id");

            // When
            SimpleFieldMetadata metadata = new SimpleFieldMetadata(idField);

            // Then
            assertThat(metadata.isId()).isTrue();
        }
    }

    @Nested
    @DisplayName("MyBatis-Plus @TableId 注解测试")
    class MyBatisPlusTableIdTests {

        @Test
        @DisplayName("MyBatis-Plus @TableId 注解应被识别为主键")
        void shouldRecognizeMyBatisPlusTableIdAnnotation() throws Exception {
            // Given
            Field idField = MyBatisPlusTableIdEntity.class.getDeclaredField("id");

            // When
            SimpleFieldMetadata metadata = new SimpleFieldMetadata(idField);

            // Then
            assertThat(metadata.isId()).isTrue();
            assertThat(metadata.isGenerated()).isTrue();
        }
    }

    // ==================== 测试实体类 ====================

    static class JakartaIdEntity {
        @Id
        private Long id;
        private String name;
    }

    static class CompositeKey {
        private Long part1;
        private Long part2;
    }

    static class EmbeddedIdEntity {
        @EmbeddedId
        private CompositeKey id;
    }

    static class SpringDataIdEntity {
        @org.springframework.data.annotation.Id
        private Long id;
    }

    static class NoAnnotationEntity {
        private Long id;  // 无注解，仅靠字段名
    }

    static class MyBatisPlusTableIdEntity {
        @com.baomidou.mybatisplus.annotation.TableId
        private Long id;
    }
}
