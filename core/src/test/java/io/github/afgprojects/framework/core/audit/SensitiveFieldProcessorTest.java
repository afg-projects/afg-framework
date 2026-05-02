package io.github.afgprojects.framework.core.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * SensitiveFieldProcessor 测试
 */
@DisplayName("SensitiveFieldProcessor 测试")
class SensitiveFieldProcessorTest {

    private AuditLogProperties properties;
    private SensitiveFieldProcessor processor;

    @BeforeEach
    void setUp() {
        properties = new AuditLogProperties();
        processor = new SensitiveFieldProcessor(properties);
    }

    @Nested
    @DisplayName("buildSensitiveFields 测试")
    class BuildSensitiveFieldsTests {

        @Test
        @DisplayName("应该合并注解和全局配置的敏感字段")
        void shouldMergeSensitiveFields() {
            properties.setSensitiveFields(new String[]{"globalField"});

            Audited annotation = createAuditedAnnotation(new String[]{"annotationField"});

            Set<String> result = processor.buildSensitiveFields(annotation);

            assertThat(result).contains("globalfield", "annotationfield");
        }

        @Test
        @DisplayName("应该标准化字段名")
        void shouldNormalizeFieldNames() {
            Audited annotation = createAuditedAnnotation(new String[]{"User_Name", "PASS_WORD"});

            Set<String> result = processor.buildSensitiveFields(annotation);

            assertThat(result).contains("username", "password");
        }
    }

    @Nested
    @DisplayName("isSensitive 测试")
    class IsSensitiveTests {

        @Test
        @DisplayName("应该识别敏感字段")
        void shouldIdentifySensitiveField() {
            Set<String> sensitiveFields = Set.of("password", "email");

            assertThat(processor.isSensitive("password", sensitiveFields)).isTrue();
            assertThat(processor.isSensitive("email", sensitiveFields)).isTrue();
        }

        @Test
        @DisplayName("应该忽略大小写和下划线")
        void shouldIgnoreCaseAndUnderscore() {
            Set<String> sensitiveFields = Set.of("password");

            assertThat(processor.isSensitive("PASSWORD", sensitiveFields)).isTrue();
            assertThat(processor.isSensitive("pass_word", sensitiveFields)).isTrue();
            assertThat(processor.isSensitive("PassWord", sensitiveFields)).isTrue();
        }

        @Test
        @DisplayName("非敏感字段应该返回 false")
        void shouldReturnFalseForNonSensitiveField() {
            Set<String> sensitiveFields = Set.of("password");

            assertThat(processor.isSensitive("username", sensitiveFields)).isFalse();
        }
    }

    @Nested
    @DisplayName("normalizeFieldName 测试")
    class NormalizeFieldNameTests {

        @Test
        @DisplayName("应该转换为小写")
        void shouldConvertToLowerCase() {
            String result = processor.normalizeFieldName("PASSWORD");

            assertThat(result).isEqualTo("password");
        }

        @Test
        @DisplayName("应该移除下划线")
        void shouldRemoveUnderscores() {
            String result = processor.normalizeFieldName("user_name");

            assertThat(result).isEqualTo("username");
        }

        @Test
        @DisplayName("应该同时转换小写和移除下划线")
        void shouldConvertToLowerCaseAndRemoveUnderscores() {
            String result = processor.normalizeFieldName("USER_NAME");

            assertThat(result).isEqualTo("username");
        }
    }

    private Audited createAuditedAnnotation(String[] sensitiveFields) {
        return new Audited() {
            @Override
            public String operation() { return ""; }
            @Override
            public String module() { return ""; }
            @Override
            public String[] sensitiveFields() { return sensitiveFields; }
            @Override
            public boolean recordArgs() { return true; }
            @Override
            public boolean recordResult() { return true; }
            @Override
            public String target() { return ""; }
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() { return Audited.class; }
        };
    }
}
